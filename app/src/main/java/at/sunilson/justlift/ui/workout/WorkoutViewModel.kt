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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val vitruvianDeviceManager: VitruvianDeviceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<Peripheral?>(null)
    val connectedDevice: StateFlow<Peripheral?> = _connectedDevice.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val connectedDeviceState: Flow<State> = connectedDevice.flatMapLatest { it?.state ?: flowOf(State.Disconnected()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State.Disconnected())

    val workoutState: Flow<VitruvianDeviceManager.WorkoutState?> = connectedDevice.flatMapLatest { device ->
        if (device != null) {
            vitruvianDeviceManager.getWorkoutStateFlow(device)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableDevices: Flow<ImmutableList<Peripheral>> = connectedDevice.flatMapLatest { it?.state ?: flowOf(State.Disconnected()) }
        .flatMapLatest { state ->
            if (state is State.Disconnected) {
                vitruvianDeviceManager.getScannedDevicesFlow()
                    .map { it.toImmutableList() }
            } else {
                flowOf(persistentListOf())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    fun onDeviceSelected(device: Peripheral) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _connectedDevice.value?.disconnect()
                device.connect()
                _connectedDevice.value = device
            } catch (error: Exception) {
                Log.e("WorkoutViewModel", "Error connecting to device", error)
            } finally {
                _loading.value = false
            }
        }
    }

    fun onStartWorkoutClicked() {
        viewModelScope.launch {
            try {
                _loading.value = true
                vitruvianDeviceManager.startJustLiftEchoWorkout(
                    device = _connectedDevice.value ?: return@launch,
                    difficulty = VitruvianDeviceManager.EchoDifficulty.HARD,
                    eccentricPercentage = uiState.value.eccentricSliderValue.toDouble(),
                    maxReps = null
                )
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Error starting workout", e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun onStopWorkoutClicked() {
        viewModelScope.launch {
            try {
                _loading.value = true
                vitruvianDeviceManager.stopWorkout(_connectedDevice.value ?: return@launch)
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Error stopping workout", e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _connectedDevice.value?.disconnect()
                _connectedDevice.value = null
            } catch (e: Exception) {
                // Handle disconnection error if needed
            } finally {
                _loading.value = false
            }
        }
    }

    fun onEccentricSliderValueChange(value: Float) {
        _uiState.update { it.copy(eccentricSliderValue = value) }
    }

    fun onRepetitionsSliderValueChange(value: Float) {
        _uiState.update { it.copy(repetitionsSliderValue = value.toInt()) }
    }

    data class WorkoutUiState(
        val eccentricSliderValue: Float = 1.0f,
        val repetitionsSliderValue: Int = 8
    )
}
