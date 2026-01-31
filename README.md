# Just Lift

A native Android companion app for the Vitruvian Trainer (V-Form) that enables "Just Lift Echo" mode without the official app or cloud connectivity. Control your smart weight trainer directly via Bluetooth Low Energy with AI-powered exercise recognition.

## Features

### Core Workout Features
- **Bluetooth Device Connection** - Automatic scanning and connection to Vitruvian "Vee" devices
- **Real-time Workout Tracking** - Live force, position, and rep monitoring for both cables
- **Dual Cable Support** - Independent tracking for left/right cables, supporting single or double-handed exercises
- **Auto-Start Detection** - Hold cables in lifted position for 4 seconds to auto-start workout
- **Auto-Stop Detection** - Automatically ends workout after 3 seconds at rest position with light load
- **Difficulty Modes** - WARMUP, HARD, HARDER, HARDEST with configurable gain and cap settings
- **Eccentric Control** - Adjustable eccentric percentage (0-130%)
- **Max Reps Target** - Optional rep limit with audio notification
- **Text-to-Speech** - Voice feedback for rep counts

### AI Exercise Recognition
- **Automatic Exercise Detection** - Uses GPT-5.1 to identify exercises based on movement "fingerprints"
- **Position-Based Matching** - Primary recognition based on range of motion (ROM), which is stable across weight/volume variations
- **Cable Usage Detection** - Distinguishes single vs dual-cable exercises
- **Fingerprint Learning** - Builds and refines exercise profiles with each confirmed workout
- **Cross-Difficulty Support** - Maintains separate fingerprints per difficulty level

### Data & Analytics
- **Workout History** - Paginated history with volume scoring relative to other sessions
- **Exercise Tendencies** - 1-Rep Max estimates and trend analysis over configurable time periods (1 week to 1 year)
- **Multi-User Support** - Switch between User 1 and User 2 with independent settings and history
- **Persistent Settings** - Difficulty, reps, eccentric %, TTS preferences saved per user

## Architecture

The app follows modern Android architecture patterns:

```
app/src/main/java/at/sunilson/justlift/
├── features/
│   ├── permissions/        # Bluetooth permissions handling
│   ├── user/              # User management
│   └── workout/
│       ├── data/          # BLE communication, AI recognition, database
│       └── presentation/  # UI components, ViewModels
├── di/                    # Koin dependency injection
├── navigation/            # Type-safe navigation routes
└── shared/                # Theme, components, audio
```

### Technology Stack
- **UI**: Jetpack Compose + Material 3
- **State Management**: Kotlin Flows, StateFlow
- **DI**: Koin with KSP annotations
- **Database**: Room with Paging 3
- **Bluetooth**: Kable (JuulLabs)
- **AI/ML**: OpenAI API (GPT-5.1)
- **Settings**: DataStore Preferences

## Getting the App

At the moment only an APK download is available. Publishing to Google Play is too risky due to legal reasons.

You can find the latest release here: https://github.com/sunilson/just-lift/releases

## Building from Source

1. Clone the repository
2. Create `local.properties` with your OpenAI API key:
   ```
   OPENAI_API_KEY=your_key_here
   ```
3. Build with Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

## How It Works

### Bluetooth Communication
The app connects to Vitruvian devices via BLE using the Nordic UART Service (NUS). It reads:
- **Monitor Characteristic** - Force and position data at ~100ms intervals
- **Rep Notify Characteristic** - Rep completion notifications for counting

### Exercise Recognition
The AI recognition system works by:
1. **Collecting workout data** - Position ranges, peak force positions, timing metrics
2. **Normalizing single-cable exercises** - Data swapped to "Left" side for consistency
3. **Building fingerprints** - Movement patterns stored per (user, exercise, difficulty)
4. **Matching via AI** - GPT-5.1 compares current workout to known fingerprints
5. **Updating fingerprints** - Confirmed workouts refine exercise profiles via weighted averaging

### Key Metrics for Recognition
- **Position Range (ROM)** - Most stable identifier across weight/volume variations
- **Peak Force Position** - Where in the ROM the exercise is hardest
- **Timing** - Rep duration, rest duration, velocity
- **Cable Usage** - Single vs dual cable (must match exactly)

## Other Modes

At the moment there are no plans to implement other modes like "Guided Workouts" or "Custom Workouts".

## iOS & Multiplatform

Could be done as well, as all used libraries would support Multiplatform. If enough interest exists, this might be explored.

## Development Guidelines

- The task is not finished before the build succeeds. Always ensure the project builds successfully (`./gradlew assemble`) before considering a task complete.

## TODO

- [ ] Static Analysis
    - [ ] Add detekt
    - [ ] Add ktlint
- [ ] Logging
    - [ ] Persist logs for debugging
    - [ ] Log Bluetooth communication for easier debugging
- [ ] Convert to Multiplatform project
- [ ] Error handling and user feedback improvements
- [ ] Localization

## License

This is an unofficial companion app and is not affiliated with Vitruvian.
