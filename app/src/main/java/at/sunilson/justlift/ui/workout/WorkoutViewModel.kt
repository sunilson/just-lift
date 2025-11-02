package at.sunilson.justlift.ui.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.sunilson.justlift.bluetooth.VitruvianDeviceManager
import com.juul.kable.Peripheral
import com.juul.kable.State
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

// TODO Store workout parameters and load them on start to persist data across app restarts
@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val vitruvianDeviceManager: VitruvianDeviceManager
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _connectedPeripheral = MutableStateFlow<Peripheral?>(null)

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
                    if (state is State.Disconnected) {
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
                .flatMapLatest { if (it != null) vitruvianDeviceManager.getWorkoutStateFlow(it) else flowOf(null) }
                .collect { workoutState -> _state.update { state -> state.copy(workoutState = workoutState) } }
        }
    }

    private fun observeMachineState() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { if (it != null) vitruvianDeviceManager.getMachineStateFlow(it) else flowOf(null) }
                .collect { machineState -> _state.update { state -> state.copy(machineState = machineState) } }
        }
    }

    data class State(
        val loading: Boolean = false,
        val connectedPeripheral: Peripheral? = null,
        val connectedPeripheralState: com.juul.kable.State = com.juul.kable.State.Disconnected(),
        val availablePeripherals: ImmutableList<Peripheral> = persistentListOf(),
        val workoutState: VitruvianDeviceManager.WorkoutState? = null,
        val machineState: VitruvianDeviceManager.MachineState? = null,
        val eccentricSliderValue: Float = 1.0f,
        val repetitionsSliderValue: Int = 8
    )
}
