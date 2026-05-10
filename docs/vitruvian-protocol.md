# Vitruvian Trainer+ BLE Protocol — Reverse-Engineered Reference

Protocol-level documentation derived from the official Vitruvian Android app
(`com.vitruvian`, decompiled with `jadx`). This document focuses on the BLE
GATT layout, packet formats, and workout behavior — and on **where our
implementation in `VitruvianDeviceManagerImpl.kt` deviates from it**.

All multi-byte integers and floats are **little-endian** unless noted.
`s16/s32` = signed, `u16/u32` = unsigned, `f32` = IEEE-754 single.

---

## 1. Connection & discovery

### Scan filter
Both apps filter scan results by **device name prefix `"Vee"`** (`String.startsWith`).
No service UUID filter, no manufacturer-data filter.

### GATT layout (single Nordic UART service)

| Role                                | UUID                                       | Properties           |
|-------------------------------------|--------------------------------------------|----------------------|
| Service (Nordic UART, NUS)          | `6e400001-b5a3-f393-e0a9-e50e24dcca9e`     | —                    |
| RX — write target                   | `6e400002-b5a3-f393-e0a9-e50e24dcca9e`     | Write w/ Response    |
| Sample (combined cable monitor)     | `90e991a6-c548-44ed-969b-eb541014eae3`     | Read                 |
| Cable Left (independent fallback)   | `bc4344e9-8d63-4c89-8263-951e2d74f744`     | Read                 |
| Cable Right (independent fallback)  | `92ef83d6-8916-4921-8172-a9919bc82566`     | Read                 |
| Reps notify                         | `8308f2a6-0875-4a94-a86f-5c5c5e1b068a`     | Notify (16/24 bytes) |
| Mode notify (run-state)             | `67d0dae0-5bfc-4ea2-acc9-ac784dee7f29`     | Notify (4 bytes)     |
| Heuristic (per-cable rep stats)     | `c7b73007-b245-4503-a1ed-9e4e97eb9802`     | Read (96 bytes)      |
| Version                             | `74e994ac-0e80-4c02-9cd0-76cb31d3959b`     | Read                 |
| WifiState                           | `a7d06ce0-2e84-485f-9c25-3d4ba6fe7319`     | Notify               |
| BleUpdateRequest (firmware)         | `ef0e485a-8749-4314-b1be-01e57cd1712e`     | Notify (5 bytes)     |
| UpdateState                         | `383f7276-49af-4335-9072-f01b0f8acad6`     | Notify (9 bytes)     |
| DiagnosticDetails                   | `5fa538ec-d041-42f6-bbd6-c30d475387b7`     | Notify (20 bytes)    |

> Our impl currently uses only the first three rows + Reps notify. We are
> missing Mode notify, DiagnosticDetails, and the dual-cable fallback path.

### MTU & subscription order
- The official app starts with the default MTU of **23** and dynamically
  switches between two Sample-read strategies (combined vs. dual-cable)
  based on whether the negotiated MTU is large enough to carry the combined
  Sample payload (`MTU − 3 ≥ characteristic.size`).
- On connect, the official app subscribes to **all known notify
  characteristics** (Reps, Mode, UpdateState, BleUpdateRequest, WifiState,
  DiagnosticDetails). It does **not** wait for any handshake or auth packet
  before issuing workout commands.
- Reads `Version` to consult a feature-bitmap (`canTrainerSupportMode`) but
  this is informational; commands work without it.

### Connection state machine
Defined as `Connecting → Connected → Disconnecting → Disconnected(cause)`.
Disconnect causes: `BluetoothOff`, `Cancelled`, `Rejected`, `LinkLoss`,
`ScanFailed(status)`, `Timeout`, `Unknown(status)`. One exception subclass
per cause (`BluetoothDisabledException`, `ConnectionLostException`, etc.).

### Heuristic: detecting set-start mid-stream
The official app maintains a sticky flag: when a Mode notification arrives
with state `SOFTWARE` (running) and the prior `Reps` notification had
`down == 0`, the next `Reps` with `down == 0` is treated as a synthetic
"new set" event. This compensates for the device staying in `SOFTWARE`
across rest periods.

---

## 2. Outbound packets — the command-ID table

All outbound packets are written to the RX characteristic
(`6e400002…`) with **Write-with-Response**. Every packet starts with a
**`u32` little-endian command ID**. There is **no length prefix and no
checksum** — packet length is fixed per command.

| Symbol             | Cmd ID | Length | Class                       | Purpose                              |
|--------------------|--------|--------|-----------------------------|--------------------------------------|
| `BEGIN_WIFI_UPDATE`| `0x01` | 4      | `UpdateBeginPacket`         | Begin Wi-Fi OTA                      |
| `WIFI_UPDATE_RESC` | `0x02` | varies | `UpdateResourcePacket`      | Wi-Fi OTA resource metadata          |
| `ACTIVATION`       | `0x04` | 96     | `ActivationPacket`          | Old School / fixed-weight start      |
| `DEVICE_COLORS`    | `0x11` | 34     | `DeviceColorSchemePacket`   | LED color preset                     |
| `BLE_UPDATE_BEGIN` | `0x1D` | varies | `BleUpdateBeginPacket`      | Begin BLE OTA                        |
| `BLE_UPDATE_CANCEL`| `0x1F` | 4      | `BleUpdateCancelPacket`     | Cancel BLE OTA                       |
| `ISOKINETIC`       | `0x38` | —      | (defined, unused)           |                                      |
| `QUICKSTART`       | `0x39` | —      | (defined, unused)           |                                      |
| `ECHO`             | `0x4E` | 32     | `EchoPacket`                | Echo / variable-resistance start     |
| `REGULAR`          | `0x4F` | 28     | `RegularPacket`             | Regular concentric/eccentric kg mode |
| `STOP`             | `0x50` | 2      | `StopPacket`                | Stop workout (returns Mode→BASELINE) |

Note: Stop is sent as raw bytes `[0x50, 0x00]`, **not** as a 4-byte
command-ID prefix.

---

## 3. EchoPacket — `0x4E`, 32 bytes

The Echo packet drives the Echo / "Just Lift" workout, which is what we
care about most.

| Offset | Size | Type | Field                       | Notes                                     |
|--------|------|------|-----------------------------|-------------------------------------------|
| 0x00   | 4    | u32  | cmdId                       | `0x4E`                                    |
| 0x04   | 1    | u8   | romRepCount                 | ROM/calibration reps; default **3**       |
| 0x05   | 1    | u8   | repCount                    | Target reps; **`0xFF` = unlimited**       |
| 0x06   | 2    | s16  | spotter                     | Always `0` in official UI                 |
| 0x08   | 2    | s16  | eccentricOverloadPct        | Default 100; user-tunable                 |
| 0x0A   | 2    | s16  | referenceMapBlend           | Always `50` in official UI                |
| 0x0C   | 4    | f32  | concentricDelaySeconds      | Always `0.1`                              |
| 0x10   | 4    | f32  | concentric.durationSeconds  | `50.0 / velocity` (see difficulty table)  |
| 0x14   | 4    | f32  | concentric.maxVelocity      | `velocity` (see difficulty table)         |
| 0x18   | 4    | f32  | eccentric.durationSeconds   | Always `0.0`                              |
| 0x1C   | 4    | f32  | eccentric.maxVelocity       | Always `-200.0`                           |

### Difficulty → Echo phase parameters

The official `EchoDifficulty` enum has only **four** values. WARMUP is *not*
a Vitruvian-side difficulty — it's a Just Lift addition.

| Difficulty | velocity | concentric.duration (s) | concentric.maxVelocity |
|------------|----------|-------------------------|------------------------|
| HARD       | 50.0     | 1.000                   | 50.0                   |
| HARDER     | 40.0     | 1.250                   | 40.0                   |
| HARDEST    | 30.0     | ≈1.667                  | 30.0                   |
| EPIC       | 15.0     | ≈3.333                  | 15.0                   |

Eccentric phase is always `(duration=0.0, maxVel=-200.0)` regardless of
difficulty. The eccentric *feel* is tuned exclusively via the
`eccentricOverloadPct` short at offset 0x08.

### What we send is not this

Our `buildEchoControlFrame` writes **gain / cap / floor / negLimit** at
offsets `0x10`, `0x14`, `0x18`, `0x1C` — those fields **don't exist** in
the Echo packet. The device likely falls back to internal defaults for any
field it doesn't recognize, which is why we get *some* working Echo
behavior despite the bytes being wrong. To match the official protocol we
need to write `(50/v, v, 0.0, -200.0)` in those four floats and remove our
gain/cap state from the user-facing settings (or repurpose them to drive
the difficulty enum / eccentric percent).

---

## 4. ActivationPacket — `0x04`, 96 bytes (Old School / fixed weight)

Composition: cmdId (4) + RepConfig (32) + ActivationForceConfig (60).

**RepConfig** (32 B at offset 0x04):
- RepCounts (4 B): `total: u8, baseline: u8 = 3, adaptive: u8 = 3, pad: u8 = 0`
  where `total = clamp(targetReps + 3, 0, 255)`.
- `seedRange: f32`
- Top RepBound (12 B): `threshold: f32 = 5.0, drift: f32 = 0.0,
  inner: (s16,s16), outer: (s16,s16)`
- Bottom RepBound (12 B): same shape.
- Safety RepBand (4 B): `mmPerM: s16, mmMax: s16`.

**ActivationForceConfig** (60 B at offset 0x24):
- Concentric phase: 2× `ActivationRamp{minMmS: s16, maxMmS: s16, ramp: f32}`
- Eccentric phase: 2× `ActivationRamp` with negative ranges.
- Force config (16 B): `forces.lo: f32, forces.hi: f32, softMax: f32, increment: f32`.

For Old School the official ramps match the constants we already use:
`(0, 20, 3.0)`, `(75, 600, 50.0)` (concentric) and
`(-1300, -1200, 100.0)`, `(-260, -110, 0.0)` (eccentric).

**No separate "Start" command exists** — the ActivationPacket itself
starts the set. Our extra `[0x03, 0x00, 0x00, 0x00]` write is unknown and
likely a no-op.

---

## 5. RegularPacket — `0x4F`, 28 bytes

Newer fixed-weight mode with separate concentric/eccentric loads:

| Offset | Size | Type | Field                       |
|--------|------|------|-----------------------------|
| 0x00   | 4    | u32  | cmdId = `0x4F`              |
| 0x04   | 1    | u8   | romRepCount = 3             |
| 0x05   | 1    | u8   | repCount                    |
| 0x06   | 2    | s16  | spotter = 0                 |
| 0x08   | 4    | f32  | concentric (kg)             |
| 0x0C   | 4    | f32  | eccentric (kg)              |
| 0x10   | 4    | f32  | progression                 |
| 0x14   | 4    | f32  | curve.linearC1              |
| 0x18   | 4    | f32  | curve.squareC2 = 0          |

We don't expose this mode yet.

---

## 6. DeviceColorSchemePacket — `0x11`, 34 bytes

Sets LED colors. Composition:
- cmdId (4) + 3× f32 (`0.0, 0.0, 0.4`) + 6× 24-bit RGB triplets.
- The 6 triplets are 3 colors written **twice** in the same order.
- Each ARGB int is encoded high→low without alpha: `(>>16) & 0xFF`,
  `(>>8) & 0xFF`, `& 0xFF`.

Our hard-coded preset matches byte-for-byte.

---

## 7. StopPacket — `[0x50, 0x00]`

Just two bytes. After writing it, the device transitions Mode `SOFTWARE
→ BASELINE`. The official app waits for that Mode notification before
declaring the stop complete.

> **Our impl sends INIT `[0x0A, 0x00, 0x00, 0x00]` to stop.** That
> command does not appear in the official command-ID enum; it's likely
> ignored. We should switch to `[0x50, 0x00]` and observe the Mode
> characteristic to confirm.

---

## 8. Inbound — Sample (`90e991a6…`), 16 or 18 bytes

Layout per cable, read or notification on the Sample characteristic:

| Offset | Size | Type | Scaling | Meaning                          |
|--------|------|------|---------|----------------------------------|
| 0x00   | 2    | s16  | / 10.0  | left.position (mm)               |
| 0x02   | 2    | s16  | / 10.0  | left.velocity (mm/s)             |
| 0x04   | 2    | s16  | / 100.0 | **left.force (kg)**              |
| 0x06   | 2    | s16  | / 10.0  | right.position (mm)              |
| 0x08   | 2    | s16  | / 10.0  | right.velocity (mm/s)            |
| 0x0A   | 2    | s16  | / 100.0 | **right.force (kg)**             |
| 0x0C   | 4    | s32  | —       | device timestamp                 |
| 0x10   | 2    | s16  | —       | sampleStatus (optional, if ≥18B) |

Validity gate: `force ∈ [-1000, 1000]`, `position ∈ [0, 100]` after
scaling. If invalid, the official app re-negotiates a higher MTU and
retries.

### Compared to our parser

Our `parseMonitorLoads` reads forces at **offsets 8 (right) and 14 (left)
as `u16/100`**; the correct offsets are **4 (left) and 10 (right) as
`s16/100`**. Symptoms today:

- Left/right are likely swapped relative to the official app.
- Negative forces (e.g. cable slack) wrap to ~655 kg through the
  unsigned cast.
- Position offsets and divisor are also off (we use 4/10 with `/2000`;
  correct is 0/6 with `/10` to get millimeters, then validity-bounded to
  100 mm).

### Dual-cable fallback (`bc4344e9…` / `92ef83d6…`)

When the negotiated MTU can't carry the combined Sample, the official
app reads two characteristics independently. **Each is 12 bytes: 3 `f32`
in raw units (position, velocity, force).** The phone synthesizes a
Sample using `System.currentTimeMillis()` for the timestamp.

We do not implement this path. With default MTU=23 and the validity
guard, our reads can silently fail bounds checks. Either request
`MTU ≥ 67` explicitly or implement the fallback.

---

## 9. Inbound — Reps (`8308f2a6…`), 16 or 24 bytes

| Offset | Size | Type | Field           |
|--------|------|------|-----------------|
| 0x00   | 4    | s32  | up              |
| 0x04   | 4    | s32  | down            |
| 0x08   | 4    | f32  | rangeTop = 300  |
| 0x0C   | 4    | f32  | rangeBottom = 0 |
| 0x10   | 2    | s16  | repsRomCount    |
| 0x12   | 2    | s16  | repsRomTotal    |
| 0x14   | 2    | s16  | repsSetCount    |
| 0x16   | 2    | s16  | repsSetTotal    |

`up` and `down` are **cumulative monotonic counters**. ROM/set fields
appear when adaptive ROM tracking and set-progress tracking are active.

### Compared to our handler

We treat each notification as a "half-rep" and toggle UP/DOWN by
parity (`halfRepNotifications % 2`). This is fragile:

- Any missed or duplicated notification permanently desyncs parity.
- We discard the explicit counters that would self-correct on every tick.
- We discard `repsRomCount/Total` and `repsSetCount/Total`, which would
  let us show real-time ROM-completion progress and set tracking.

Recommended: track `prevUp`/`prevDown`, treat `up - prevUp > 0` as a
top-of-rep event and `down - prevDown > 0` as a bottom-of-rep event,
ignore parity entirely.

---

## 10. Inbound — Mode (`67d0dae0…`), 4 bytes

`u32` little-endian:

| Value | Mode      | Meaning                              |
|-------|-----------|--------------------------------------|
| 0     | BASELINE  | Idle / no workout                    |
| 1     | SOFTWARE  | Workout running, software-controlled |
| 2     | STATIC    | Legacy static-load mode              |
| 3     | TWO_PHASE | Legacy                               |
| 4     | MASTER    | Legacy                               |

For modern Echo / Activation / Regular workouts only `BASELINE` and
`SOFTWARE` are observed. Subscribing here gives a clean
"workout-started" / "workout-stopped" signal — strictly better than
inferring from rep notifications.

---

## 11. Inbound — Heuristic (`c7b73007…`), 96 bytes

Two `Statistics` (left, right). Each contains two `PhaseStatistics`
(down, up). Each contains 6× `f32`. Total 96 B (2 × 2 × 6 × 4).

Diagnostic / per-cable rep-detection state. Not used in workout flow;
useful for logging.

---

## 12. Calibration / warmup

There is **no separate calibration packet**. Calibration is encoded in
the start packet itself:

- Echo and Regular packets: `romRepCount: u8 = 3` (default).
- Activation packet: `RepCounts.baseline = 3, adaptive = 3`, and
  `total = clamp(targetReps + 3, 0, 255)` — so a "10-rep" set actually
  asks the device for 13 reps (3 ROM + 10 working).

Our 3-rep calibration count and our handling already match. No fix.

---

## 13. Auto-stop / inactivity

The device does **not** emit a "workout complete" event. The official
app stops only when:
- The user sends `StopPacket`, or
- `repCount` reps are reached (the device naturally stops applying
  resistance because `total = base + target` is hit).

There is no inactivity timer or position-threshold auto-stop in the
official app. Our 3-second-bottom-hold and 2.6 kg light-load auto-stops
are Just Lift UX additions, not protocol requirements.

---

## 14. Firmware update (one-line each)

| Class                     | Cmd    | Direction       | Purpose                                    |
|---------------------------|--------|-----------------|--------------------------------------------|
| `UpdateBeginPacket`       | `0x01` | App → Device    | Begin Wi-Fi-pulled OTA                     |
| `UpdateResourcePacket`    | `0x02` | App → Device    | OTA metadata: directory, hash, signature   |
| `BleUpdateBeginPacket`    | `0x1D` | App → Device    | Begin BLE OTA, payload = base64 signature  |
| `BleUpdateCancelPacket`   | `0x1F` | App → Device    | Abort BLE OTA                              |
| `BleUpdateResponsePacket` | —      | App → Device    | `(offset: u32, raw bytes)` chunk reply     |
| `BleUpdateRequest`        | —      | Device → App    | `(offset: u32, index: u8)` chunk request   |
| `UpdateState`             | —      | Device → App    | `(status: u32, error: u32, progress: u8)`  |

We don't touch any of these.

---

## 15. Comparison delta — must-fix list for `VitruvianDeviceManagerImpl.kt`

Ordered by severity. Each item: **what we do → what they do → impact**.

### Correctness bugs (likely affecting current behavior)

1. **Sample force offsets and signedness.** We read `u16` at offsets
   8 and 14. Correct: **`s16` at offsets 4 (left) and 10 (right)**, then
   `/100`. Our left/right are likely swapped, and small negative forces
   wrap to ~655 kg through the unsigned cast.

2. **Sample position offsets and scaling.** We read at offsets 4 and 10
   with `/2000`. Correct: **`s16` at offsets 0 (left) and 6 (right),
   `/10` for millimeters**, validity-bounded to `[0, 100] mm`. Our
   reported positions are off by a large constant factor.

3. **Stop command bytes wrong.** We send INIT `[0x0A, 0x00, 0x00, 0x00]`
   to stop. The correct stop is `StopPacket = [0x50, 0x00]`. Our INIT is
   not in the protocol's command table — likely silently ignored. We
   currently rely on natural decay back to BASELINE.

4. **Echo packet field layout in `0x10..0x1F` is wrong.** We write
   `gain / cap / floor / negLimit` (our own state) at those offsets.
   Official protocol has `concentric.duration / concentric.maxVelocity /
   eccentric.duration / eccentric.maxVelocity`. Difficulty maps to
   velocity ∈ {50, 40, 30, 15} which drives both concentric fields.
   Eccentric is fixed `(0.0, -200.0)`. We need to drop the
   gain/cap/floor model and rebuild the packet as
   `(50/v, v, 0.0, -200.0)`.

5. **Reps parser uses parity instead of explicit counters.** We treat
   each notification as a half-rep. The notification carries
   monotonic `up: i32` and `down: i32` counters — desync-proof. Switch
   to delta-tracking and remove `halfRepNotifications`.

### Missing data / observability

6. **No subscription to Mode (`67d0dae0…`).** Subscribing gives a
   reliable BASELINE↔SOFTWARE signal, removing reliance on rep
   notifications and timeouts to know whether a workout has actually
   started or stopped.

7. **No use of ROM / set tracking from Reps.** `repsRomCount` /
   `repsRomTotal` would let us show a real range-of-motion progress bar
   per rep instead of the raw rep counter.

8. **No DiagnosticDetails subscription.** Useful for surfacing
   device-side fault states to the user.

### Compatibility / robustness

9. **No MTU negotiation / dual-cable fallback.** With default
   MTU=23, the combined Sample read may fail validity. Either request
   `MTU ≥ 67` on connect or implement the fallback that reads the two
   `Cable*` characteristics (12 B each, 3× f32) and synthesizes a
   Sample client-side.

10. **Echo eccentric.maxVelocity = `-100.0` instead of `-200.0`.** Once
    fix #4 is in, also use the official `-200.0` cap; our `-100.0`
    softens the descent.

### Cosmetic / harmless

11. **`EchoDifficulty.WARMUP` is a Just Lift addition.** No matching
    official preset. Acceptable to keep, but it's not a real Vitruvian
    difficulty — internally it should map to one of the four real
    presets (probably HARD with low eccentric overload).

12. **Pre-INIT `[0x0A, 0x00, 0x00, 0x00]`.** Not in the official
    command table. Probably a no-op. Can be removed.

13. **Old School separate Start `[0x03, 0x00, 0x00, 0x00]`.** Not in the
    official command table. The ActivationPacket itself starts the set.
    Can be removed.

14. **Auto-stop on bottom hold / light load.** Not in the official app.
    These are our UX additions for Just Lift. No protocol issue, just
    document them as on-top-of behavior.

15. **Calibration count of 3.** Matches. No fix.

16. **NUS service / RX UUIDs.** Match. No fix.

17. **Scan filter prefix `"Vee"`.** Match. No fix.

---

## 16. Suggested fix order

If we do these in this order, most user-visible behavior issues should
clear up:

1. Fix Sample parsing (#1, #2) — forces and positions in the UI become
   correct.
2. Fix Reps parser to use explicit counters (#5) — rep counting becomes
   self-healing.
3. Fix Echo packet bytes at `0x10..0x1F` (#4) — Echo difficulty actually
   matches the official feel.
4. Switch stop command to `[0x50, 0x00]` (#3) and subscribe to Mode (#6)
   — clean stop semantics.
5. Either negotiate higher MTU or implement dual-Cable fallback (#9).
6. Remove dead writes (#12, #13).
7. Surface ROM-progress in UI from Reps (#7).
