# Exercise Recognition: Analysis & Improvement Plan

## Current System Analysis

### How Recognition Works

1. **Workout ends** -> `WorkoutViewModel` calls `ExerciseRecognitionService.recognizeExercise()`
2. The service loads **all** `ExerciseEntity` rows for the user from the `exercises` table
3. It builds a **MovementFingerprint** for the current workout and for each stored exercise
4. Fingerprints contain: position range (ROM), avg position range, peak force positions, movement ratio (concentric/eccentric), and avg velocity — all metrics that should be **independent of weight, reps, and difficulty**
5. Exercises are **grouped by name** and their metrics averaged across all difficulty levels
6. Filtered by cable type (SINGLE/DUAL must match)
7. A GPT-5-mini prompt sends both the current workout's fingerprint and all candidate fingerprints, asking the model to pick the best match
8. Model must return exactly one of the candidate exercise names

### How Corrections Feed Back

There are three paths that update the fingerprint reference table:

| Action | Updates `exercises` table? | Method |
|--------|---------------------------|--------|
| AI auto-recognizes | **No** | Saves to `workout_history` only |
| User confirms AI guess | **Yes** | `averageWith()` EMA |
| User corrects/assigns name | **Yes** | `averageWith()` if exercise exists, `toExerciseEntity()` if new |

The `averageWith()` function uses **Exponential Moving Average (EMA)** with weight **0.3** for position and timing metrics:

```
new_fingerprint = old_value * 0.7 + new_value * 0.3
```

### The Per-Difficulty Storage Model

Fingerprints are stored with a unique index on `(userId, name, difficulty)`. This means a user who does "Bench Press" on HARD, HARDER, and HARDEST has **3 separate fingerprint rows** for "Bench Press". During recognition, all 3 are loaded, grouped by name, and their metrics are **plain-averaged** into a single candidate fingerprint.

---

## Root Problem: Why Early-Session Recognition Fails

### The Core Issue: Single Fingerprint Per Difficulty

Each exercise stores **exactly one fingerprint per difficulty level** in the `exercises` table. This fingerprint is a running EMA that gets updated every time the user confirms or corrects. The problem:

**A single EMA fingerprint has no concept of natural variance.**

When a user does Bench Press 50 times across many sessions, the fingerprint converges to the *average* of all those sessions. But any individual workout has natural variation — slightly different ROM, slightly different tempo, slightly different peak force position. The fingerprint has no memory of *how much* variation is normal for this exercise.

### Why Corrections in the Current Session "Fix" Recognition

When the user corrects exercise #1 in a session, `averageWith()` fires with weight 0.3. This **shifts the fingerprint 30% toward the current session's data**. Now exercise #2 (same type) is being compared against a fingerprint that was just pulled closer to today's workout characteristics. Of course it matches better.

This is the exact behavior you described: first recognitions are wrong, but after a correction or two, it suddenly works. The system is essentially **overfitting to the current session's data distribution** with every correction.

### The 0.3 EMA Weight Is Too Aggressive

With a 0.3 weight, the fingerprint's "memory" of past data decays fast:

| Corrections ago | Remaining influence |
|-----------------|-------------------|
| Current | 30% |
| 1 correction ago | 21% (0.7 * 0.3) |
| 2 corrections ago | 14.7% |
| 5 corrections ago | 5% |
| 10 corrections ago | 0.8% |

After ~5 corrections, the fingerprint is almost entirely shaped by the last 5 workouts. Any natural day-to-day variation in how a user performs an exercise gets lost quickly.

### Cross-Difficulty Averaging Hides Important Variance

When building candidate fingerprints, `buildExerciseFingerprints()` does a **simple average** across all difficulty levels. If the user has done an exercise on 3 difficulties and only 1 has been recently corrected (thus shifted toward today's data), that one freshly-shifted fingerprint gets diluted by the other 2 stale ones.

This creates an inconsistent experience: correction on one difficulty level barely improves recognition at another difficulty level, and vice versa.

---

## Improvement Plan

### Goal

Corrections should **slowly and gradually** improve recognition over many sessions rather than causing a dramatic within-session shift. The system should represent natural workout variance rather than a single point estimate.

---

### Phase 1: Store Historical Fingerprints Instead of a Single EMA

**Problem solved:** The single-EMA approach discards all historical information. We can't reason about variance, detect outliers, or weight recent vs old data intelligently.

**Change:** Store every confirmed/corrected workout as a separate fingerprint sample rather than averaging them into one row.

#### New table: `exercise_fingerprints`

```sql
CREATE TABLE exercise_fingerprints (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId INTEGER NOT NULL,
    exerciseName TEXT NOT NULL,
    difficulty TEXT NOT NULL,
    timestampMillis INTEGER NOT NULL,
    -- all position/timing metrics from current ExerciseEntity
    positionRangeLeft REAL,
    positionRangeRight REAL,
    avgPositionRangeLeft REAL,
    avgPositionRangeRight REAL,
    peakForcePositionUp REAL,
    peakForcePositionDown REAL,
    movementRatio REAL,
    avgVelocityUp REAL,
    avgVelocityDown REAL,
    -- source metadata
    wasCorrection INTEGER DEFAULT 0  -- 1 if user manually corrected this
);
```

#### Migration from `exercises` table

- Keep the existing `exercises` table as-is for backward compatibility during migration
- Seed `exercise_fingerprints` with one row per existing `ExerciseEntity`
- Going forward, every confirm/correction creates a new row instead of calling `averageWith()`

#### Building candidate fingerprints from history

Instead of one EMA value, compute the candidate fingerprint from the **last N samples** (e.g., N=20) with **time-decayed weighting**:

```kotlin
fun buildCandidateFingerprint(samples: List<FingerprintSample>): MovementFingerprint {
    // Use last 20 samples max, weighted by recency
    val recent = samples.sortedByDescending { it.timestampMillis }.take(20)

    // Half-life of ~10 sessions: older data still contributes but less
    val halfLifeSessions = 10.0
    val weights = recent.mapIndexed { index, _ ->
        2.0.pow(-index.toDouble() / halfLifeSessions)
    }
    val totalWeight = weights.sum()

    // Weighted average for each metric
    val posRangeLeft = recent.zip(weights).sumOf { (s, w) -> s.positionRangeLeft * w } / totalWeight
    // ... same for all metrics
}
```

**Why this is better:**
- Natural variance is preserved — the system "knows" that Bench Press ROM varies between 0.42 and 0.48
- No single correction can shift the fingerprint by 30% — it's just one of N samples
- Old sessions still contribute, preventing catastrophic forgetting
- Corrections accumulate gradually: 1 correction is a small signal, 10 corrections over weeks are a strong one

---

### Phase 2: Use Variance for Confidence-Aware Matching

**Problem solved:** The AI currently has no concept of how "spread out" an exercise's metrics are. A metric that's very consistent (low variance) should matter more than one that's noisy.

**Change:** Include per-metric standard deviation in the prompt.

#### Updated prompt format

```
Bench Press:
- Position Range (Left): 0.450 (std: 0.015)
- Position Range (Right): 0.430 (std: 0.020)
- Peak Force Position (Up): 0.650 (std: 0.080)
...
```

Add to matching rules:
```
When comparing metrics, give MORE weight to metrics with LOW standard deviation (std).
A metric with std 0.01 is very consistent and should be a strong differentiator.
A metric with std 0.10 is highly variable and should be used only for tie-breaking.
```

**Why this is better:**
- If an exercise's ROM is always 0.45 +/- 0.01, a workout with ROM 0.60 is clearly NOT that exercise
- If an exercise's velocity varies wildly (std 0.08), the AI won't be confused by velocity differences
- More data = lower std = better recognition — naturally rewards exercises with more samples

---

### Phase 3: Decouple Correction Weight from Confirmation Weight

**Problem solved:** Currently, confirming an AI guess and manually correcting a wrong guess have the same effect on the fingerprint. But a correction carries much stronger signal.

**Change:** Mark each fingerprint sample with its source:

| Source | Meaning |
|--------|---------|
| `CONFIRMED` | AI was right, user tapped checkmark — this sample is "typical" |
| `CORRECTED` | AI was wrong, user manually assigned — this sample expands what the exercise "looks like" |
| `INITIAL` | First time user creates this exercise — baseline |

When building candidate fingerprints, **don't weight corrections more heavily** — instead, simply include them as samples. Over time, the natural distribution of samples defines the exercise. A correction doesn't warp the fingerprint; it adds one data point to the pool.

If a user does 20 workouts of Bench Press and corrects 3 of them, those 3 corrections represent 15% of the data — appropriately influential without dominating.

---

### Phase 4: Remove the Per-Difficulty Split in Fingerprint Building

**Problem solved:** The current system stores separate fingerprints per difficulty and then averages them. This is unnecessary complexity since the fingerprint metrics (ROM, peak force position, velocity) are already designed to be difficulty-independent.

**Change:**
- Store samples with their difficulty as metadata (for future analysis), but **don't split by difficulty** when building candidate fingerprints
- All samples for "Bench Press" regardless of difficulty are pooled into one candidate
- This gives each exercise more samples to work with, improving the statistical robustness

**Important nuance (per your note):** The fingerprint metrics are intentionally difficulty-independent (ROM, normalized positions, movement ratios). They should already be similar across difficulties. Pooling them together gives each exercise a richer history to draw from, and the variance computation will naturally capture any difficulty-related spread.

---

### Phase 5: Cap Sample Count and Add Staleness Decay

**Problem solved:** Over months/years, a user might accumulate hundreds of samples per exercise. Old samples from when the user was a beginner may not represent their current form.

**Change:**
- Keep the **last 30 samples** per exercise (prunable via a periodic cleanup)
- Apply time-based decay when computing the weighted average:
  - Samples from the last 2 weeks: full weight (1.0)
  - Samples from 2-8 weeks ago: weight 0.7
  - Samples older than 8 weeks: weight 0.4
- This naturally handles form drift over time while keeping the system responsive to long-term trends

---

### Phase 6: Add a Confidence Score to Recognition Results

**Problem solved:** Currently the AI returns a name or nothing. There's no concept of "I'm 90% sure" vs "it's a coin flip between two exercises."

**Change:** Ask the model to return a JSON response with confidence:

```json
{"exercise": "Bench Press", "confidence": 0.85, "runner_up": "Incline Press"}
```

Use the confidence to:
- **High confidence (>0.8):** Show the recognized name with a subtle confirm button
- **Medium confidence (0.5-0.8):** Show the recognized name with a more prominent "Is this right?" prompt
- **Low confidence (<0.5):** Show "Which exercise was this?" with the top 2-3 suggestions

This sets better user expectations and makes the correction flow feel intentional rather than like the AI is always guessing wrong.

---

## Implementation Priority

| Phase | Effort | Impact | Priority |
|-------|--------|--------|----------|
| **Phase 1: Historical fingerprints** | Medium (DB migration, new table, updated queries) | **High** — fixes the core "session overfitting" problem | **P0** |
| **Phase 3: Source tagging** | Low (add column, update insert logic) | Medium — cleaner data model for future improvements | **P0** (do with Phase 1) |
| **Phase 4: Pool across difficulties** | Low (remove groupBy difficulty in fingerprint building) | Medium — more samples per exercise, better stats | **P1** |
| **Phase 2: Variance in prompt** | Low (compute std, update prompt) | **High** — makes the AI much smarter about which metrics matter | **P1** |
| **Phase 5: Staleness decay** | Low (add weight function) | Medium — prevents long-term drift | **P2** |
| **Phase 6: Confidence score** | Medium (prompt change, JSON parsing, UI changes) | Medium — better UX, sets expectations | **P2** |

---

## Summary

The core insight is: **a single EMA fingerprint with 0.3 weight is a lossy, volatile representation.** Each correction overwrites 30% of the exercise's identity, causing the system to chase whatever the current session looks like. By storing individual samples and computing statistics over them, corrections become *additive evidence* rather than *destructive overwrites*. The system learns gradually, represents natural variance, and won't oscillate between sessions.
