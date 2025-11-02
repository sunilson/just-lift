package at.sunilson.justlift.features.workout.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
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

// TODO Store workout parameters and load them on start to persist data across app restarts
@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val vitruvianDeviceManager: VitruvianDeviceManager
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

    init {
        observeConnectedPeripheralState()
        observeAvailablePeripherals()
        observeWorkoutState()
        observeMachineState()
    }

    fun onDeviceSelected(device: Peripheral) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(loading = true) }
                _connectedPeripheral.value?.disconnect()
                device.connect()
                _connectedPeripheral.value = device
            } catch (error: Exception) {
                Log.e("WorkoutViewModel", "Error connecting to device", error)
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onStartWorkoutClicked() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(loading = true) }
                vitruvianDeviceManager.startWorkout(
                    device = _connectedPeripheral.value ?: return@launch,
                    difficulty = VitruvianDeviceManager.EchoDifficulty.HARD,
                    eccentricPercentage = state.value.eccentricSliderValue.toDouble(),
                    maxReps = null
                )
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Error starting workout", e)
            } finally {
                _state.update { it.copy(loading = false) }
            }
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
            } catch (e: Exception) {
                // Handle disconnection error if needed
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onEccentricSliderValueChange(value: Float) {
        _state.update { it.copy(eccentricSliderValue = value) }
    }

    fun onRepetitionsSliderValueChange(value: Float) {
        _state.update { it.copy(repetitionsSliderValue = value.toInt()) }
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

    private fun observeWorkoutState() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest {
                    if (it != null) vitruvianDeviceManager.getWorkoutStateFlow(it) else flowOf(
                        null
                    )
                }
                .collect { workoutState -> _state.update { state -> state.copy(workoutState = workoutState) } }
        }
    }

    private fun observeMachineState() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { if (it != null) vitruvianDeviceManager.getMachineStateFlow(it) else flowOf(null) }
                .collect { machineState ->
                    // Always expose machine state
                    _state.update { s -> s.copy(machineState = machineState) }

                    val device = _connectedPeripheral.value ?: return@collect
                    val workoutActive = state.value.workoutState != null

                    if (machineState == null) {
                        resetAutoStart()
                        return@collect
                    }

                    // If workout already active, no auto start; clear countdown
                    if (workoutActive) {
                        resetAutoStart()
                        return@collect
                    }

                    val positionLeft = machineState.positionCableLeft
                    val positionRight = machineState.positionCableRight
                    val liftedLeft = positionLeft >= LIFTED_POS_THRESHOLD
                    val liftedRight = positionRight >= LIFTED_POS_THRESHOLD
                    val anyLifted = liftedLeft || liftedRight

                    if (!anyLifted) {
                        // Reset when both at bottom
                        resetAutoStart()
                        return@collect
                    }

                    val now = System.currentTimeMillis()

                    // Initialize baselines at first lift
                    if (startHoldSinceMillis == null) {
                        startHoldSinceMillis = now
                        baselineLeft = positionLeft
                        baselineRight = positionRight
                        // Start a lightweight ticker to ensure UI countdown updates smoothly
                        autoStartTickerJob?.cancel()
                        autoStartTickerJob = viewModelScope.launch {
                            while (startHoldSinceMillis != null && state.value.workoutState == null) {
                                val start = startHoldSinceMillis ?: break
                                val remMs = AUTO_START_HOLD_MS - (System.currentTimeMillis() - start)
                                val secondsLeftTicker = if (remMs > 0) kotlin.math.ceil(remMs / 1000.0).toInt() else 0
                                _state.update { it.copy(autoStartInSeconds = secondsLeftTicker) }
                                delay(250)
                            }
                        }
                    }

                    // Check if held near baseline (within epsilon)
                    val withinLeft = baselineLeft?.let { abs(positionLeft - it) <= HOLD_EPSILON } ?: true
                    val withinRight = baselineRight?.let { abs(positionRight - it) <= HOLD_EPSILON } ?: true

                    // We consider hold valid if each lifted cable stays within epsilon; non-lifted cable ignored
                    val holdValid = ((!liftedLeft || withinLeft) && (!liftedRight || withinRight))

                    if (!holdValid) {
                        // Movement detected -> restart timer/baseline
                        startHoldSinceMillis = now
                        baselineLeft = positionLeft
                        baselineRight = positionRight
                        _state.update { it.copy(autoStartInSeconds = AUTO_START_SECONDS) }
                        return@collect
                    }

                    val elapsed = now - (startHoldSinceMillis ?: now)
                    val remainingMs = AUTO_START_HOLD_MS - elapsed
                    val secondsLeft = if (remainingMs > 0) ceil(remainingMs / 1000.0).toInt() else 0
                    _state.update { it.copy(autoStartInSeconds = secondsLeft) }

                    if (elapsed >= AUTO_START_HOLD_MS) {
                        // Debounce successive triggers
                        if (now - lastAutoStartAt > AUTO_START_DEBOUNCE_MS) {
                            lastAutoStartAt = now
                            viewModelScope.launch {
                                try {
                                    vitruvianDeviceManager.startWorkout(
                                        device = device,
                                        difficulty = VitruvianDeviceManager.EchoDifficulty.HARD,
                                        eccentricPercentage = state.value.eccentricSliderValue.toDouble(),
                                        maxReps = null,
                                        stopAtLastTopRep = false
                                    )
                                } catch (e: Exception) {
                                    Log.e("WorkoutViewModel", "Auto start failed", e)
                                } finally {
                                    resetAutoStart()
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun resetAutoStart() {
        startHoldSinceMillis = null
        baselineLeft = null
        baselineRight = null
        autoStartTickerJob?.cancel()
        autoStartTickerJob = null
        _state.update { it.copy(autoStartInSeconds = null) }
    }

    data class State(
        val loading: Boolean = false,
        val connectedPeripheral: Peripheral? = null,
        val connectedPeripheralState: com.juul.kable.State = com.juul.kable.State.Disconnected(),
        val availablePeripherals: ImmutableList<Peripheral> = persistentListOf(),
        val workoutState: VitruvianDeviceManager.WorkoutState? = null,
        val machineState: VitruvianDeviceManager.MachineState? = null,
        val eccentricSliderValue: Float = 1.0f,
        val repetitionsSliderValue: Int = 8,
        val autoStartInSeconds: Int? = null
    )

    companion object {
        private const val AUTO_START_SECONDS: Int = 3
        private const val AUTO_START_HOLD_MS: Long = AUTO_START_SECONDS * 1000L
        private const val LIFTED_POS_THRESHOLD: Double = 0.05 // 5%

        // Allow small fluctuations while holding
        private const val HOLD_EPSILON: Double = 0.01 // +/-1%

        // Prevent repeated auto-starts in quick succession
        private const val AUTO_START_DEBOUNCE_MS: Long = 5_000L
    }
}
