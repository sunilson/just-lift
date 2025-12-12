package at.sunilson.justlift.features.workout.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.WorkoutSettings
import at.sunilson.justlift.features.workout.data.WorkoutSettingsRepository
import at.sunilson.justlift.features.workout.data.SavedDevice
import at.sunilson.justlift.shared.audio.AppSoundPlayer
import com.juul.kable.Peripheral
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import kotlin.math.abs
import kotlin.math.ceil

@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class, com.juul.kable.ExperimentalApi::class)
class WorkoutViewModel(
    private val vitruvianDeviceManager: VitruvianDeviceManager,
    private val soundPlayer: AppSoundPlayer,
    private val workoutSettingsRepository: WorkoutSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _connectedPeripheral = MutableStateFlow<Peripheral?>(null)

    // Auto start detection tracking
    private var startHoldSinceMillis: Long? = null
    private var baselineLeft: Double? = null
    private var baselineRight: Double? = null
    private var lastAutoStartAt: Long = 0L
    private var autoStartTickerJob: kotlinx.coroutines.Job? = null
    private var countdownSoundStarted: Boolean = false

    init {
        observeConnectedPeripheral()
        observeConnectedPeripheralState()
        observeAvailablePeripherals()
        observeWorkoutState()
        observeMachineState()
        observeSavedDevice()
        observeAutoConnect()
    }

    fun onDeviceSelected(device: Peripheral) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(loading = true) }
                _connectedPeripheral.value?.disconnect()
                device.connect()
                // Always stop any possibly running/stale workout right after connecting
                // to ensure the device and our UI start from a clean state.
                try {
                    vitruvianDeviceManager.stopWorkout(device)
                } catch (_: Exception) {
                    // Best-effort cleanup; ignore if stop fails
                }
                _connectedPeripheral.value = device

                // Persist last device for future auto-connects
                runCatching { workoutSettingsRepository.setLastDevice(device.identifier, device.name) }

                // Restore previously saved defaults once upon a fresh connection
                runCatching { workoutSettingsRepository.get() }
                    .onSuccess { settings ->
                        _state.update { s ->
                            s.copy(
                                echoDifficulty = settings.echoDifficulty,
                                useNoRepLimit = settings.useNoRepLimit,
                                repetitionsSliderValue = settings.repetitions,
                                eccentricSliderValue = settings.eccentricPercentage
                            )
                        }
                    }
            } catch (error: Exception) {
                Log.e("WorkoutViewModel", "Error connecting to device", error)
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onStartWorkoutClicked() {
        viewModelScope.launch {
            tryStartWorkout()
        }
    }

    fun onStopWorkoutClicked() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(loading = true) }
                vitruvianDeviceManager.stopWorkout(_connectedPeripheral.value ?: return@launch)
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Error stopping workout", e)
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(loading = true) }
                _connectedPeripheral.value?.disconnect()
                _connectedPeripheral.value = null
            } catch (_: Exception) {
                // Handle disconnection error if needed
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onClearSavedDeviceClicked() {
        viewModelScope.launch {
            workoutSettingsRepository.clearLastDevice()
        }
    }

    fun onEccentricSliderValueChange(value: Float) {
        _state.update { it.copy(eccentricSliderValue = value) }
        persistCurrentSettings()
    }

    fun onRepetitionsSliderValueChange(value: Float) {
        _state.update { it.copy(repetitionsSliderValue = value.toInt()) }
        persistCurrentSettings()
    }

    fun onUseNoRepLimitChange(useNoRepLimit: Boolean) {
        _state.update { it.copy(useNoRepLimit = useNoRepLimit) }
        persistCurrentSettings()
    }

    fun onEchoDifficultyChange(difficulty: VitruvianDeviceManager.EchoDifficulty) {
        _state.update { it.copy(echoDifficulty = difficulty) }
        persistCurrentSettings()
    }

    private fun persistCurrentSettings() {
        val s = state.value
        viewModelScope.launch {
            workoutSettingsRepository.save(
                WorkoutSettings(
                    echoDifficulty = s.echoDifficulty,
                    useNoRepLimit = s.useNoRepLimit,
                    repetitions = s.repetitionsSliderValue,
                    eccentricPercentage = s.eccentricSliderValue
                )
            )
        }
    }

    private fun observeConnectedPeripheral() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .collect { peripheral -> _state.update { state -> state.copy(connectedPeripheral = peripheral) } }
        }
    }

    private fun observeConnectedPeripheralState() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { it?.state ?: flowOf(com.juul.kable.State.Disconnected()) }
                .collect { _state.update { state -> state.copy(connectedPeripheralState = it) } }
        }
    }

    private fun observeAvailablePeripherals() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { it?.state ?: flowOf(com.juul.kable.State.Disconnected()) }
                .flatMapLatest { state ->
                    if (state is com.juul.kable.State.Disconnected) {
                        vitruvianDeviceManager.getScannedDevicesFlow()
                            .map { it.toImmutableList() }
                    } else {
                        flowOf(persistentListOf())
                    }
                }
                .collect { peripherals -> _state.update { state -> state.copy(availablePeripherals = peripherals) } }
        }
    }

    private fun observeSavedDevice() {
        viewModelScope.launch {
            workoutSettingsRepository.savedDeviceFlow.collect { saved ->
                _state.update { it.copy(savedDevice = saved) }
                if (saved == null) {
                    // Cancel any auto-connect in UI
                    _state.update { it.copy(isAutoConnecting = false) }
                    autoConnectInProgress = false
                }
            }
        }
    }

    private var autoConnectInProgress: Boolean = false
    private fun observeAutoConnect() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { it?.state ?: flowOf(com.juul.kable.State.Disconnected()) }
                .collect { connState ->
                    if (connState is com.juul.kable.State.Connected) {
                        _state.update { it.copy(isAutoConnecting = false) }
                        autoConnectInProgress = false
                    }
                }
        }

        viewModelScope.launch {
            // When disconnected and saved device exists, try to connect to it once seen in scan
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { it?.state ?: flowOf(com.juul.kable.State.Disconnected()) }
                .flatMapLatest { st ->
                    if (st is com.juul.kable.State.Disconnected) vitruvianDeviceManager.getScannedDevicesFlow() else flowOf(emptyList())
                }
                .collect { scanned ->
                    val target = state.value.savedDevice ?: return@collect
                    if (autoConnectInProgress) return@collect
                    val match = scanned.firstOrNull { it.identifier == target.id }
                    if (match != null) {
                        autoConnectInProgress = true
                        _state.update { it.copy(isAutoConnecting = true) }
                        onDeviceSelected(match)
                    }
                }
        }
    }

    private var lastWorkoutState: VitruvianDeviceManager.WorkoutState? = null
    private fun observeWorkoutState() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { if (it != null) vitruvianDeviceManager.getWorkoutStateFlow(it) else flowOf(null) }
                .collect { workoutState ->
                    val prev = lastWorkoutState
                    when {
                        prev == null && workoutState != null -> soundPlayer.playStart()
                        prev != null && workoutState == null -> soundPlayer.playDone()
                        prev != null && workoutState != null -> {
                            val repsIncreased = workoutState.upwardRepetitionsCompleted > prev.upwardRepetitionsCompleted
                            val calibratingIncreased = workoutState.calibratingRepsCompleted > prev.calibratingRepsCompleted
                            if (repsIncreased || (calibratingIncreased && workoutState.calibratingRepsCompleted > 0)) {
                                soundPlayer.playRepRegular()
                            }
                        }
                    }
                    lastWorkoutState = workoutState
                    _state.update { state ->
                        val updatedPrevious = when {
                            // Transition from active -> paused/stopped: keep the last non-null snapshot
                            prev != null && workoutState == null -> prev
                            else -> state.previousWorkoutState
                        }
                        val updatedPauseStart = when {
                            // Set pause start timestamp exactly when we transition to paused (active -> null)
                            prev != null && workoutState == null -> System.currentTimeMillis()
                            // Clear timestamp when a workout starts/resumes (null -> non-null)
                            prev == null && workoutState != null -> null
                            else -> state.pauseStartTimestamp
                        }
                        state.copy(
                            workoutState = workoutState,
                            previousWorkoutState = updatedPrevious,
                            pauseStartTimestamp = updatedPauseStart
                        )
                    }
                }
        }
    }

    private fun observeMachineState() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { if (it != null) vitruvianDeviceManager.getMachineStateFlow(it) else flowOf(null) }
                .collect { machineState ->
                    // Always expose machine state
                    _state.update { s -> s.copy(machineState = machineState) }

                    if (machineState == null || state.value.workoutState != null) {
                        resetAutoStart()
                        return@collect
                    }

                    val positionLeft = machineState.positionCableLeft
                    val positionRight = machineState.positionCableRight
                    val liftedLeft = positionLeft >= LIFTED_POS_THRESHOLD
                    val liftedRight = positionRight >= LIFTED_POS_THRESHOLD

                    if (!liftedLeft && !liftedRight) {
                        resetAutoStart()
                        return@collect
                    }

                    val now = System.currentTimeMillis()

                    // Initialize baselines at first lift
                    if (startHoldSinceMillis == null) {
                        startHoldSinceMillis = now
                        baselineLeft = positionLeft
                        baselineRight = positionRight

                        // Pre-initialize device to reduce start latency (INIT + PRESET)
                        _connectedPeripheral.value?.let { device ->
                            viewModelScope.launch {
                                try {
                                    vitruvianDeviceManager.prepareForWorkout(device)
                                } catch (_: Throwable) {
                                }
                            }
                        }

                        // Start a lightweight ticker to ensure UI countdown updates smoothly
                        autoStartTickerJob?.cancel()
                        autoStartTickerJob = viewModelScope.launch {
                            countdownSoundStarted = false
                            while (startHoldSinceMillis != null && state.value.workoutState == null) {
                                val start = startHoldSinceMillis ?: break
                                val elapsedMs = System.currentTimeMillis() - start
                                val remMs = AUTO_START_TOTAL_HOLD_MS - elapsedMs

                                if (!countdownSoundStarted && elapsedMs >= AUTO_START_PRECOUNT_MS) {
                                    soundPlayer.playAutoStartCountDown()
                                    countdownSoundStarted = true
                                }

                                val secondsLeftTicker: Int? = if (elapsedMs >= AUTO_START_PRECOUNT_MS) {
                                    if (remMs > 0) ceil(remMs / 1000.0).toInt() else 0
                                } else null
                                _state.update { it.copy(autoStartInSeconds = secondsLeftTicker) }

                                if (remMs <= 0) {
                                    // Safety: trigger auto-start directly from ticker to avoid waiting for next monitor tick
                                    val nowCall = System.currentTimeMillis()
                                    if (nowCall - lastAutoStartAt > AUTO_START_DEBOUNCE_MS && state.value.workoutState == null) {
                                        lastAutoStartAt = nowCall
                                        tryStartWorkout()
                                        resetAutoStart()
                                    }
                                    break
                                }

                                delay(100)
                            }
                        }.also { it.invokeOnCompletion { soundPlayer.stopAutoStartCountDown() } }
                    }

                    val withinLeft = baselineLeft?.let { abs(positionLeft - it) <= HOLD_EPSILON } ?: true
                    val withinRight = baselineRight?.let { abs(positionRight - it) <= HOLD_EPSILON } ?: true
                    val holdValid = ((!liftedLeft || withinLeft) && (!liftedRight || withinRight))

                    if (!holdValid) {
                        // Movement detected -> restart timer/baseline and cancel countdown sound
                        startHoldSinceMillis = now
                        baselineLeft = positionLeft
                        baselineRight = positionRight
                        countdownSoundStarted = false
                        _state.update { it.copy(autoStartInSeconds = null) }

                        // Explicitly stop only the countdown sound when the hold is lost
                        soundPlayer.stopAutoStartCountDown()

                        // Restart ticker; countdown sound will start after pre-count window
                        autoStartTickerJob?.cancel()

                        // Pre-initialize again to keep it fresh if throttling allows
                        _connectedPeripheral.value?.let { device ->
                            viewModelScope.launch {
                                try {
                                    vitruvianDeviceManager.prepareForWorkout(device)
                                } catch (_: Throwable) {
                                }
                            }
                        }

                        autoStartTickerJob = viewModelScope.launch {
                            countdownSoundStarted = false
                            while (startHoldSinceMillis != null && state.value.workoutState == null) {
                                val elapsedMs = System.currentTimeMillis() - (startHoldSinceMillis ?: break)
                                val remainingMs = AUTO_START_TOTAL_HOLD_MS - elapsedMs

                                if (!countdownSoundStarted && elapsedMs >= AUTO_START_PRECOUNT_MS) {
                                    soundPlayer.playAutoStartCountDown()
                                    countdownSoundStarted = true
                                }

                                val secondsLeftTicker = if (elapsedMs >= AUTO_START_PRECOUNT_MS) {
                                    if (remainingMs > 0) ceil(remainingMs / 1000.0).toInt() else 0
                                } else null
                                _state.update { it.copy(autoStartInSeconds = secondsLeftTicker) }

                                if (remainingMs <= 0) {
                                    val nowCall = System.currentTimeMillis()
                                    if (nowCall - lastAutoStartAt > AUTO_START_DEBOUNCE_MS && state.value.workoutState == null) {
                                        lastAutoStartAt = nowCall
                                        tryStartWorkout()
                                        resetAutoStart()
                                    }
                                    break
                                }
                                delay(100)
                            }
                        }.also { it.invokeOnCompletion { soundPlayer.stopAutoStartCountDown() } }
                        return@collect
                    }

                    val elapsed = now - (startHoldSinceMillis ?: now)
                    val remainingMs = AUTO_START_TOTAL_HOLD_MS - elapsed
                    val secondsLeft: Int? = if (elapsed >= AUTO_START_PRECOUNT_MS) {
                        if (remainingMs > 0) ceil(remainingMs / 1000.0).toInt() else 0
                    } else null
                    _state.update { it.copy(autoStartInSeconds = secondsLeft) }

                    if (elapsed >= AUTO_START_TOTAL_HOLD_MS && now - lastAutoStartAt > AUTO_START_DEBOUNCE_MS) {
                        lastAutoStartAt = now
                        viewModelScope.launch {
                            tryStartWorkout()
                            resetAutoStart()
                        }
                    }
                }
        }
    }

    private suspend fun tryStartWorkout() {
        try {
            // Clear any pause timestamp as we are starting/resuming a workout
            _state.update { it.copy(loading = true, pauseStartTimestamp = null) }
            vitruvianDeviceManager.startWorkout(
                device = _connectedPeripheral.value ?: return,
                difficulty = state.value.echoDifficulty,
                eccentricPercentage = (state.value.eccentricSliderValue / 100.0).toDouble(),
                maxReps = state.value.repetitionsSliderValue.takeIf { !state.value.useNoRepLimit }
            )
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error starting workout", e)
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    private fun resetAutoStart() {
        startHoldSinceMillis = null
        baselineLeft = null
        baselineRight = null
        countdownSoundStarted = false
        autoStartTickerJob?.cancel()
        autoStartTickerJob = null
        soundPlayer.stopAutoStartCountDown()
        _state.update { it.copy(autoStartInSeconds = null) }
    }

    data class State(
        val loading: Boolean = false,
        val connectedPeripheral: Peripheral? = null,
        val connectedPeripheralState: com.juul.kable.State = com.juul.kable.State.Disconnected(),
        val availablePeripherals: ImmutableList<Peripheral> = persistentListOf(),
        val workoutState: VitruvianDeviceManager.WorkoutState? = null,
        val previousWorkoutState: VitruvianDeviceManager.WorkoutState? = null,
        // Timestamp when pause started (ms since epoch). Null initially and cleared on workout start.
        val pauseStartTimestamp: Long? = null,
        val machineState: VitruvianDeviceManager.MachineState? = null,
        val useNoRepLimit: Boolean = true,
        val echoDifficulty: VitruvianDeviceManager.EchoDifficulty = VitruvianDeviceManager.EchoDifficulty.HARDEST,
        val eccentricSliderValue: Float = 100.0f,
        val repetitionsSliderValue: Int = 8,
        val autoStartInSeconds: Int? = null,
        val savedDevice: SavedDevice? = null,
        val isAutoConnecting: Boolean = false
    )

    companion object {
        // Total hold time before auto-start
        private const val AUTO_START_TOTAL_HOLD_MS: Long = 4000L

        // Length of audible/visible countdown (seconds)
        private const val AUTO_START_COUNTDOWN_MS: Long = 3000L

        // Silent pre-count before countdown starts
        private const val AUTO_START_PRECOUNT_MS: Long = AUTO_START_TOTAL_HOLD_MS - AUTO_START_COUNTDOWN_MS

        private const val LIFTED_POS_THRESHOLD: Double = 0.1

        // Allow small fluctuations while holding
        private const val HOLD_EPSILON: Double = 0.025

        // Prevent repeated auto-starts in quick succession
        private const val AUTO_START_DEBOUNCE_MS: Long = 5_000L
    }
}
