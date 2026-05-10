package at.sunilson.justlift.features.workout.presentation

import at.sunilson.justlift.features.workout.data.SavedDevice
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.presentation.history.ExerciseTrend
import at.sunilson.justlift.features.workout.presentation.history.TrendTimeframe
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryEntry
import com.juul.kable.Peripheral
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class WorkoutScreenState(
    val loading: Boolean = false,
    val connectedPeripheral: Peripheral? = null,
    val connectedPeripheralState: com.juul.kable.State = com.juul.kable.State.Disconnected(),
    val availablePeripherals: ImmutableList<Peripheral> = persistentListOf(),
    val workoutState: VitruvianDeviceManager.WorkoutState? = null,
    val previousWorkoutState: VitruvianDeviceManager.WorkoutState? = null,
    val previousWorkoutExerciseName: String? = null,
    val previousWorkoutEntry: WorkoutHistoryEntry? = null,
    val pauseStartTimestamp: Long? = null,
    val machineState: VitruvianDeviceManager.MachineState? = null,
    val echoDifficulty: VitruvianDeviceManager.EchoDifficulty = VitruvianDeviceManager.EchoDifficulty.WARMUP,
    val eccentricSliderValue: Float = 100.0f,
    val repetitionsSliderValue: Int = 8,
    val useTts: Boolean = false,
    val fixedWeightMode: Boolean = false,
    val fixedWeightKg: Float = 20f,
    val autoStartInSeconds: Int? = null,
    val savedDevice: SavedDevice? = null,
    val isAutoConnecting: Boolean = false,
    val showHistory: Boolean = false,
    val showDifficultySheet: Boolean = false,
    val difficultySheetSelection: VitruvianDeviceManager.EchoDifficulty = echoDifficulty,
    val difficultySheetGain: Float = 1.0f,
    val difficultySheetCap: Float = 50.0f,
    val showExerciseSelection: WorkoutHistoryEntry? = null,
    val exerciseNameSuggestions: List<String> = emptyList(),
    val showTendencies: Boolean = false,
    val showTendenciesInfo: Boolean = false,
    val tendencies: List<ExerciseTrend> = emptyList(),
    val selectedTrendTimeframe: TrendTimeframe = TrendTimeframe.ONE_WEEK,
    val showExerciseNameEditor: Boolean = false,
    val allExerciseNames: List<String> = emptyList(),
    val setsPerUser: Int = 1
)
