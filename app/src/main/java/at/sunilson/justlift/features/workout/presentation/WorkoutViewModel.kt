package at.sunilson.justlift.features.workout.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.WorkoutSettings
import at.sunilson.justlift.features.workout.data.WorkoutSettingsRepository
import at.sunilson.justlift.features.workout.data.DifficultySettings
import at.sunilson.justlift.features.workout.data.SavedDevice
import at.sunilson.justlift.shared.audio.AppSoundPlayer
import com.juul.kable.Peripheral
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryDao
import at.sunilson.justlift.features.user.data.UserRepository
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryEntry
import at.sunilson.justlift.features.workout.presentation.history.ExerciseTrend
import at.sunilson.justlift.features.workout.presentation.history.TrendTimeframe
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryUiModel
import at.sunilson.justlift.features.workout.presentation.history.toDomain
import at.sunilson.justlift.features.workout.presentation.history.toEntity
import at.sunilson.justlift.features.workout.presentation.history.toExerciseEntity
import at.sunilson.justlift.features.workout.presentation.history.averageWith
import at.sunilson.justlift.features.workout.presentation.history.estimatedUpwardOneRepMax
import at.sunilson.justlift.features.workout.presentation.history.estimatedDownwardOneRepMax
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import android.widget.Toast
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.juul.kable.ExperimentalApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.util.Date
import java.text.DateFormat
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil

import at.sunilson.justlift.features.workout.data.ExerciseRecognitionService
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalApi::class)
@KoinViewModel
class WorkoutViewModel(
    private val vitruvianDeviceManager: VitruvianDeviceManager,
    private val soundPlayer: AppSoundPlayer,
    private val workoutSettingsRepository: WorkoutSettingsRepository,
    private val workoutHistoryDao: WorkoutHistoryDao,
    private val userRepository: UserRepository,
    private val exerciseRecognitionService: ExerciseRecognitionService,
    private val context: Context
) : ViewModel(), DefaultLifecycleObserver {

    private val isForeground = MutableStateFlow(false)

    val currentUserId = userRepository.currentUserId.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val pagedHistory = currentUserId.flatMapLatest { userId ->
        Pager(PagingConfig(pageSize = 20)) {
            workoutHistoryDao.getAllPaged(userId)
        }.flow
    }.map { pagingData ->
        pagingData
            .map { it.toDomain() }
            .map { WorkoutHistoryUiModel.Entry(it) as WorkoutHistoryUiModel }
            .insertSeparators { before, after ->
                val afterEntry = (after as? WorkoutHistoryUiModel.Entry)?.entry
                val beforeEntry = (before as? WorkoutHistoryUiModel.Entry)?.entry

                if (afterEntry == null) return@insertSeparators null

                val afterDate = DateFormat.getDateInstance().format(Date(afterEntry.timestampMillis))
                if (beforeEntry == null) return@insertSeparators WorkoutHistoryUiModel.Header(afterDate)

                val beforeDate = DateFormat.getDateInstance().format(Date(beforeEntry.timestampMillis))
                if (beforeDate != afterDate) {
                    WorkoutHistoryUiModel.Header(afterDate)
                } else {
                    null
                }
            }
    }.cachedIn(viewModelScope)

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
    private var finishSoundPlayed: Boolean = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        observeConnectedPeripheral()
        observeConnectedPeripheralState()
        observeAvailablePeripherals()
        observeWorkoutState()
        observeMachineState()
        observeSavedDevice()
        observeAutoConnect()
        observeCurrentUser()
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground.value = false
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        super.onCleared()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            combine(currentUserId, isForeground) { userId, foreground ->
                userId
            }.collect { userId ->
                runCatching { workoutSettingsRepository.get(userId) }
                    .onSuccess { settings ->
                        _state.update { s ->
                            s.copy(
                                echoDifficulty = settings.echoDifficulty,
                                repetitionsSliderValue = settings.repetitions,
                                eccentricSliderValue = settings.eccentricPercentage,
                                useTts = settings.useTts,
                                difficultySheetSelection = settings.echoDifficulty
                            )
                        }
                    }

                val latest = workoutHistoryDao.getLatest(userId)?.toDomain()
                _state.update { s ->
                    s.copy(
                        previousWorkoutEntry = latest,
                        previousWorkoutState = latest?.workoutState,
                        previousWorkoutExerciseName = latest?.exerciseName
                    )
                }
            }
        }
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
        manuallyDisconnected = true
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

    fun onUseTtsChange(useTts: Boolean) {
        _state.update { it.copy(useTts = useTts) }
        persistCurrentSettings()
    }


    fun onEchoDifficultyChange(difficulty: VitruvianDeviceManager.EchoDifficulty) {
        _state.update { it.copy(echoDifficulty = difficulty, difficultySheetSelection = difficulty) }
        // Persist selected difficulty alongside current global settings
        persistCurrentSettings()
    }

    private fun persistCurrentSettings() {
        val s = state.value
        viewModelScope.launch {
            // Save last used selection for quick restore
            workoutSettingsRepository.save(
                currentUserId.value,
                WorkoutSettings(
                    echoDifficulty = s.echoDifficulty,
                    repetitions = s.repetitionsSliderValue,
                    eccentricPercentage = s.eccentricSliderValue,
                    useTts = s.useTts
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

    private var manuallyDisconnected: Boolean = false
    private var autoConnectInProgress: Boolean = false
    private fun observeAutoConnect() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { it?.state ?: flowOf(com.juul.kable.State.Disconnected()) }
                .collect { connState ->
                    if (connState is com.juul.kable.State.Connected) {
                        _state.update { it.copy(isAutoConnecting = false) }
                        autoConnectInProgress = false
                        // Reset manually disconnected flag when we successfully connect
                        manuallyDisconnected = false
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
                    if (autoConnectInProgress || manuallyDisconnected) return@collect
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
                    val useTts = state.value.useTts
                    when {
                        prev == null && workoutState != null -> {
                            soundPlayer.playStart(useTts)
                            finishSoundPlayed = false
                        }
                        prev != null && workoutState == null -> {
                            if (!finishSoundPlayed) soundPlayer.playDone(useTts)
                        }
                        prev != null && workoutState != null -> {
                            val repsIncreased = workoutState.upwardRepetitionsCompleted > prev.upwardRepetitionsCompleted
                            val calibratingIncreased = workoutState.calibratingRepsCompleted > prev.calibratingRepsCompleted
                            if (repsIncreased) {
                                val maxReps = workoutState.maxReps
                                if (maxReps != null && workoutState.upwardRepetitionsCompleted >= maxReps && !finishSoundPlayed) {
                                    soundPlayer.playDone(useTts)
                                    finishSoundPlayed = true
                                } else {
                                    soundPlayer.playRep(workoutState.upwardRepetitionsCompleted, false, useTts)
                                }
                            } else if (calibratingIncreased && workoutState.calibratingRepsCompleted > 0) {
                                soundPlayer.playRep(workoutState.calibratingRepsCompleted, true, useTts)
                            }
                        }
                    }
                    lastWorkoutState = workoutState
                    _state.update { state ->
                        val updatedPrevious = when {
                            // Transition from active -> paused/stopped: keep the last non-null snapshot
                            prev != null && workoutState == null -> {
                                if (prev.difficulty != VitruvianDeviceManager.EchoDifficulty.WARMUP) {
                                    prev
                                } else {
                                    null
                                }
                            }

                            else -> state.previousWorkoutState
                        }

                        val updatedExerciseName = when {
                            // Clear exercise name on transition
                            prev != null && workoutState == null -> null
                            else -> state.previousWorkoutExerciseName
                        }

                        val updatedPreviousEntry = when {
                            // Clear previous entry on transition
                            prev != null && workoutState == null -> null
                            else -> state.previousWorkoutEntry
                        }

                        // Save to database when a workout just finished
                        if (prev != null && workoutState == null) {
                            if (prev.difficulty != VitruvianDeviceManager.EchoDifficulty.WARMUP) {
                                viewModelScope.launch {
                                    val userId = currentUserId.value
                                    Log.d(
                                        "ExerciseRecognition",
                                        "Workout finished. Starting recognition for user $userId..."
                                    )
                                    val guessedName = if (isForeground.value) {
                                        exerciseRecognitionService.recognizeExercise(userId, prev)
                                    } else {
                                        Log.d(
                                            "ExerciseRecognition",
                                            "App in background, skipping immediate recognition."
                                        )
                                        null
                                    }
                                    Log.d("ExerciseRecognition", "Guessed exercise name: $guessedName")
                                    val entry = WorkoutHistoryEntry(
                                        workoutState = prev,
                                        timestampMillis = System.currentTimeMillis(),
                                        exerciseName = guessedName,
                                        wasAutomaticallyRecognized = guessedName != null
                                    )
                                    val id = workoutHistoryDao.insert(entry.toEntity(userId))
                                    val finalEntry = entry.copy(id = id)
                                    _state.update {
                                        it.copy(
                                            previousWorkoutExerciseName = guessedName,
                                            previousWorkoutEntry = finalEntry
                                        )
                                    }
                                    Log.d(
                                        "ExerciseRecognition",
                                        "Workout entry saved to history with ID $id"
                                    )
                                }
                            } else {
                                Log.d("ExerciseRecognition", "Warmup finished. Skipping history and recognition.")
                            }
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
                            previousWorkoutExerciseName = updatedExerciseName,
                            previousWorkoutEntry = updatedPreviousEntry,
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
                userId = currentUserId.value,
                device = _connectedPeripheral.value ?: return,
                difficulty = state.value.echoDifficulty,
                eccentricPercentage = (state.value.eccentricSliderValue / 100.0).toDouble(),
                maxReps = state.value.repetitionsSliderValue
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
        val previousWorkoutExerciseName: String? = null,
        val previousWorkoutEntry: WorkoutHistoryEntry? = null,
        // Timestamp when pause started (ms since epoch). Null initially and cleared on workout start.
        val pauseStartTimestamp: Long? = null,
        val machineState: VitruvianDeviceManager.MachineState? = null,
        val echoDifficulty: VitruvianDeviceManager.EchoDifficulty = VitruvianDeviceManager.EchoDifficulty.WARMUP,
        val eccentricSliderValue: Float = 100.0f,
        val repetitionsSliderValue: Int = 8,
        val useTts: Boolean = false,
        val autoStartInSeconds: Int? = null,
        val savedDevice: SavedDevice? = null,
        val isAutoConnecting: Boolean = false,
        // UI flag: whether history overlay is visible
        val showHistory: Boolean = false,
        // Difficulty settings editor UI (per-mode machine parameters)
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
        val allExerciseNames: List<String> = emptyList()
    )

    fun onHistoryClicked() {
        _state.update { it.copy(showHistory = true) }
    }

    fun onDismissHistoryClicked() {
        _state.update { it.copy(showHistory = false) }
    }

    fun onShowTendenciesClicked() {
        _state.update { it.copy(showTendencies = true) }
        calculateTrends()
    }

    fun onDismissTendenciesClicked() {
        _state.update { it.copy(showTendencies = false) }
    }

    fun onTrendTimeframeChanged(timeframe: TrendTimeframe) {
        _state.update { it.copy(selectedTrendTimeframe = timeframe) }
        calculateTrends()
    }

    private fun calculateTrends() {
        viewModelScope.launch {
            val userId = currentUserId.value
            val history = workoutHistoryDao.getAllHistory(userId)
            val timeframe = state.value.selectedTrendTimeframe

            val now = ZonedDateTime.now()
            val limit = when (timeframe) {
                TrendTimeframe.ONE_WEEK -> now.minus(1, ChronoUnit.WEEKS)
                TrendTimeframe.TWO_WEEKS -> now.minus(2, ChronoUnit.WEEKS)
                TrendTimeframe.ONE_MONTH -> now.minus(1, ChronoUnit.MONTHS)
                TrendTimeframe.THREE_MONTHS -> now.minus(3, ChronoUnit.MONTHS)
                TrendTimeframe.ONE_YEAR -> now.minus(1, ChronoUnit.YEARS)
            }.toInstant().toEpochMilli()

            val trends = history
                .filter { it.exerciseName != null }
                .groupBy { it.exerciseName!! to it.difficulty }
                .mapNotNull { (key, entries) ->
                    val (name, difficulty) = key
                    val recentEntries = entries.filter { it.timestampMillis > limit }
                    val beforeEntries = entries.filter { it.timestampMillis <= limit }

                    if (recentEntries.isEmpty() || beforeEntries.isEmpty()) return@mapNotNull null

                    val avgUpwardRecent = recentEntries.map { it.estimatedUpwardOneRepMax() }.average()
                    val avgUpwardBefore = beforeEntries.map { it.estimatedUpwardOneRepMax() }.average()

                    val avgDownwardRecent = recentEntries.map { it.estimatedDownwardOneRepMax() }.average()
                    val avgDownwardBefore = beforeEntries.map { it.estimatedDownwardOneRepMax() }.average()

                    ExerciseTrend(
                        exerciseName = "$name ($difficulty)",
                        avgUpwardTrend = avgUpwardRecent - avgUpwardBefore,
                        avgDownwardTrend = avgDownwardRecent - avgDownwardBefore,
                        recentUpwardTrend = avgUpwardRecent,
                        recentDownwardTrend = avgDownwardRecent
                    )
                }
                .sortedByDescending { abs(it.avgUpwardTrend) + abs(it.avgDownwardTrend) }

            _state.update { it.copy(tendencies = trends) }
        }
    }

    fun onShowTendenciesInfoClicked() {
        _state.update { it.copy(showTendenciesInfo = true) }
    }

    fun onDismissTendenciesInfoClicked() {
        _state.update { it.copy(showTendenciesInfo = false) }
    }

    // Difficulty settings bottom sheet events (tuning per-mode machine parameters)
    fun onOpenDifficultySettings() {
        // Initialize sheet selection from current difficulty and load its values
        val currentDifficulty = state.value.echoDifficulty
        viewModelScope.launch {
            val params = runCatching {
                workoutSettingsRepository.getModeParameters(
                    currentUserId.value,
                    currentDifficulty
                )
            }
                .getOrElse {
                    // Fall back to safe defaults if needed
                    at.sunilson.justlift.features.workout.data.ModeParameters(gain = 1.0f, capKg = 50.0f)
                }
            _state.update {
                it.copy(
                    showDifficultySheet = true,
                    difficultySheetSelection = currentDifficulty,
                    difficultySheetGain = params.gain,
                    difficultySheetCap = params.capKg
                )
            }
        }
    }

    fun onDismissDifficultySettings() {
        _state.update { it.copy(showDifficultySheet = false) }
    }

    fun onDifficultySheetSelectDifficulty(difficulty: VitruvianDeviceManager.EchoDifficulty) {
        viewModelScope.launch {
            val params = runCatching {
                workoutSettingsRepository.getModeParameters(
                    currentUserId.value,
                    difficulty
                )
            }
                .getOrElse {
                    at.sunilson.justlift.features.workout.data.ModeParameters(gain = 1.0f, capKg = 50.0f)
                }
            _state.update {
                it.copy(
                    difficultySheetSelection = difficulty,
                    difficultySheetGain = params.gain,
                    difficultySheetCap = params.capKg
                )
            }
        }
    }

    fun onDifficultySheetUpdateGain(value: Float) {
        val selected = state.value.difficultySheetSelection
        viewModelScope.launch {
            val current = workoutSettingsRepository.getModeParameters(currentUserId.value, selected)
            val newParams = current.copy(gain = value.coerceIn(0.5f, 3.333f))
            workoutSettingsRepository.saveModeParameters(currentUserId.value, selected, newParams)
            _state.update { it.copy(difficultySheetGain = newParams.gain) }
        }
    }

    fun onDifficultySheetUpdateCap(value: Float) {
        val selected = state.value.difficultySheetSelection
        viewModelScope.launch {
            val current = workoutSettingsRepository.getModeParameters(currentUserId.value, selected)
            val newParams = current.copy(capKg = value.coerceIn(15f, 50f))
            workoutSettingsRepository.saveModeParameters(currentUserId.value, selected, newParams)
            _state.update { it.copy(difficultySheetCap = newParams.capKg) }
        }
    }

    fun onDifficultySheetResetSelected() {
        val selected = state.value.difficultySheetSelection
        viewModelScope.launch {
            workoutSettingsRepository.resetModeParameters(currentUserId.value, selected)
            val restored = workoutSettingsRepository.getModeParameters(currentUserId.value, selected)
            _state.update {
                it.copy(
                    difficultySheetGain = restored.gain,
                    difficultySheetCap = restored.capKg
                )
            }
        }
    }

    fun onUserSwitchClicked() {
        viewModelScope.launch {
            val nextUser = if (currentUserId.value == 1) 2 else 1
            userRepository.switchToUser(nextUser)
            Toast.makeText(context, "Switched to User $nextUser", Toast.LENGTH_SHORT).show()
        }
    }

    fun onEditExerciseName(entry: WorkoutHistoryEntry) {
        viewModelScope.launch {
            val exercises = workoutHistoryDao.getAllExerciseNames()
            _state.update { it.copy(showExerciseSelection = entry, exerciseNameSuggestions = exercises) }
        }
    }

    fun onExerciseSelected(name: String) {
        val entry = state.value.showExerciseSelection ?: return
        viewModelScope.launch {
            val userId = currentUserId.value
            
            // Only update fingerprint and history if the name was actually changed or set for the first time.
            // This ensures that recognized exercises don't override their own fingerprints 
            // unless the user manually intervenes and changes/confirms the name.
            if (name != entry.exerciseName) {
                // Update history entry
                workoutHistoryDao.updateExerciseName(entry.id, name)
                updateExerciseFingerprint(userId, name, entry.workoutState)
            }

            _state.update { s ->
                val updatedPreviousEntry = if (s.previousWorkoutEntry?.id == entry.id) {
                    s.previousWorkoutEntry.copy(
                        exerciseName = name,
                        wasAutomaticallyRecognized = false
                    )
                } else {
                    s.previousWorkoutEntry
                }
                s.copy(
                    showExerciseSelection = null,
                    previousWorkoutExerciseName = if (s.previousWorkoutEntry?.id == entry.id) name else s.previousWorkoutExerciseName,
                    previousWorkoutEntry = updatedPreviousEntry
                )
            }
        }
    }

    fun onConfirmRecognition(entry: WorkoutHistoryEntry) {
        val name = entry.exerciseName ?: return
        viewModelScope.launch {
            val userId = currentUserId.value
            workoutHistoryDao.setConfirmed(entry.id)
            updateExerciseFingerprint(userId, name, entry.workoutState)
            _state.update { s ->
                val updatedPreviousEntry = if (s.previousWorkoutEntry?.id == entry.id) {
                    s.previousWorkoutEntry.copy(isConfirmed = true)
                } else {
                    s.previousWorkoutEntry
                }
                s.copy(previousWorkoutEntry = updatedPreviousEntry)
            }
        }
    }

    private suspend fun updateExerciseFingerprint(
        userId: Int,
        name: String,
        workoutState: VitruvianDeviceManager.WorkoutState
    ) {
        if (workoutState.difficulty == VitruvianDeviceManager.EchoDifficulty.WARMUP) {
            Log.d("ExerciseRecognition", "updateExerciseFingerprint skipped for WARMUP")
            return
        }

        // Save as exercise fingerprint (average with existing if present)
        val existing = workoutHistoryDao.getExercise(userId, name, workoutState.difficulty.name)
        val exerciseEntity = if (existing != null) {
            existing.averageWith(workoutState)
        } else {
            workoutState.toExerciseEntity(userId, name)
        }
        workoutHistoryDao.insertExercise(exerciseEntity)
    }

    fun onDismissExerciseSelection() {
        _state.update { it.copy(showExerciseSelection = null) }
    }

    fun onOpenExerciseNameEditor() {
        viewModelScope.launch {
            val names = workoutHistoryDao.getAllExerciseNames()
            _state.update { it.copy(showExerciseNameEditor = true, allExerciseNames = names) }
        }
    }

    fun onDismissExerciseNameEditor() {
        _state.update { it.copy(showExerciseNameEditor = false) }
    }

    fun onRenameExercise(oldName: String, newName: String) {
        viewModelScope.launch {
            workoutHistoryDao.renameExercise(oldName, newName)
            workoutHistoryDao.renameExerciseInHistory(oldName, newName)
            // Refresh names
            val names = workoutHistoryDao.getAllExerciseNames()
            _state.update {
                it.copy(
                    allExerciseNames = names,
                    previousWorkoutExerciseName = if (it.previousWorkoutExerciseName == oldName) newName else it.previousWorkoutExerciseName,
                    previousWorkoutEntry = if (it.previousWorkoutEntry?.exerciseName == oldName) {
                        it.previousWorkoutEntry.copy(exerciseName = newName)
                    } else {
                        it.previousWorkoutEntry
                    }
                )
            }
        }
    }

    fun onDeleteExercise(name: String, alternativeName: String?) {
        viewModelScope.launch {
            workoutHistoryDao.deleteExercise(name)
            workoutHistoryDao.renameExerciseInHistory(name, alternativeName)
            // Refresh names
            val names = workoutHistoryDao.getAllExerciseNames()
            _state.update {
                it.copy(
                    allExerciseNames = names,
                    previousWorkoutExerciseName = if (it.previousWorkoutExerciseName == name) alternativeName else it.previousWorkoutExerciseName,
                    previousWorkoutEntry = if (it.previousWorkoutEntry?.exerciseName == name) {
                        it.previousWorkoutEntry.copy(exerciseName = alternativeName)
                    } else {
                        it.previousWorkoutEntry
                    }
                )
            }
        }
    }

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
