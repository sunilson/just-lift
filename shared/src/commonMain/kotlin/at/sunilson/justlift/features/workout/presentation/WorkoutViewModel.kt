package at.sunilson.justlift.features.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.sunilson.justlift.features.workout.data.DifficultySettings
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.WorkoutSettings
import at.sunilson.justlift.features.workout.data.WorkoutSettingsRepository
import at.sunilson.justlift.features.workout.data.WorkoutHistoryRepository
import at.sunilson.justlift.features.workout.data.ModeParameters
import at.sunilson.justlift.features.workout.data.SavedDevice
import at.sunilson.justlift.features.user.data.UserRepository
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryEntry
import at.sunilson.justlift.features.workout.presentation.history.ExerciseTrend
import at.sunilson.justlift.features.workout.presentation.history.TrendTimeframe
import at.sunilson.justlift.features.workout.presentation.history.toDomain
import at.sunilson.justlift.features.workout.presentation.history.toEntity
import at.sunilson.justlift.features.workout.presentation.history.estimatedUpwardOneRepMax
import at.sunilson.justlift.features.workout.presentation.history.estimatedDownwardOneRepMax
import at.sunilson.justlift.platform.AppSoundPlayer
import at.sunilson.justlift.platform.PlatformNotifier
import at.sunilson.justlift.platform.currentTimeMillis
import at.sunilson.justlift.platform.platformLog
import at.sunilson.justlift.platform.timeframeStartMillis
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalApi::class, kotlin.uuid.ExperimentalUuidApi::class)
open class WorkoutViewModel(
    private val vitruvianDeviceManager: VitruvianDeviceManager,
    private val soundPlayer: AppSoundPlayer,
    private val workoutSettingsRepository: WorkoutSettingsRepository,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val userRepository: UserRepository,
    private val platformNotifier: PlatformNotifier
) : ViewModel() {

    val currentUserId = userRepository.currentUserId.stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    val twoUserMode = userRepository.twoUserMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val setsPerUser = userRepository.setsPerUser.stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    private var setsCompletedForCurrentUser: Int = 0

    private val _state = MutableStateFlow(WorkoutScreenState())
    val state: StateFlow<WorkoutScreenState> = _state.asStateFlow()

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
        observeConnectedPeripheral()
        observeConnectedPeripheralState()
        observeAvailablePeripherals()
        observeWorkoutState()
        observeMachineState()
        observeSavedDevice()
        observeAutoConnect()
        observeCurrentUser()
        observeSetsPerUser()
    }

    private fun observeSetsPerUser() {
        viewModelScope.launch {
            setsPerUser.collect { value ->
                _state.update { it.copy(setsPerUser = value) }
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            currentUserId.collect { userId ->
                runCatching { workoutSettingsRepository.get(userId) }
                    .onSuccess { settings ->
                        val diff = runCatching {
                            workoutSettingsRepository.getDifficultySettings(userId, settings.echoDifficulty)
                        }.getOrDefault(DifficultySettings())
                        _state.update { s ->
                            s.copy(
                                echoDifficulty = settings.echoDifficulty,
                                repetitionsSliderValue = diff.repetitions,
                                eccentricSliderValue = settings.eccentricPercentage,
                                useTts = settings.useTts,
                                fixedWeightMode = settings.fixedWeightMode,
                                fixedWeightKg = settings.fixedWeightKg,
                                difficultySheetSelection = settings.echoDifficulty
                            )
                        }
                    }

                val latest = workoutHistoryRepository.getLatest(userId)?.toDomain()
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
                try {
                    vitruvianDeviceManager.stopWorkout(device)
                } catch (_: Exception) {
                }
                _connectedPeripheral.value = device

                runCatching { workoutSettingsRepository.setLastDevice(device.identifier.toString(), device.name) }
            } catch (error: Exception) {
                platformLog("WorkoutViewModel", "Error connecting to device: ${error.message}")
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
                platformLog("WorkoutViewModel", "Error stopping workout: ${e.message}")
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
        val newReps = value.toInt()
        _state.update { it.copy(repetitionsSliderValue = newReps) }
        val userId = currentUserId.value
        val difficulty = state.value.echoDifficulty
        viewModelScope.launch {
            val current = runCatching {
                workoutSettingsRepository.getDifficultySettings(userId, difficulty)
            }.getOrDefault(DifficultySettings())
            workoutSettingsRepository.saveDifficultySettings(
                userId,
                difficulty,
                current.copy(repetitions = newReps)
            )
        }
    }

    fun onUseTtsChange(useTts: Boolean) {
        _state.update { it.copy(useTts = useTts) }
        persistCurrentSettings()
    }

    fun onFixedWeightModeChange(enabled: Boolean) {
        _state.update { it.copy(fixedWeightMode = enabled) }
        persistCurrentSettings()
    }

    fun onFixedWeightKgChange(value: Float) {
        _state.update { it.copy(fixedWeightKg = value.coerceIn(5f, 100f)) }
        persistCurrentSettings()
    }

    fun onEchoDifficultyChange(difficulty: VitruvianDeviceManager.EchoDifficulty) {
        _state.update { it.copy(echoDifficulty = difficulty, difficultySheetSelection = difficulty) }
        persistCurrentSettings()
        viewModelScope.launch {
            val diff = runCatching {
                workoutSettingsRepository.getDifficultySettings(currentUserId.value, difficulty)
            }.getOrDefault(DifficultySettings())
            _state.update { it.copy(repetitionsSliderValue = diff.repetitions) }
        }
    }

    private fun persistCurrentSettings() {
        val s = state.value
        viewModelScope.launch {
            workoutSettingsRepository.save(
                currentUserId.value,
                WorkoutSettings(
                    echoDifficulty = s.echoDifficulty,
                    eccentricPercentage = s.eccentricSliderValue,
                    useTts = s.useTts,
                    fixedWeightMode = s.fixedWeightMode,
                    fixedWeightKg = s.fixedWeightKg
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
                        manuallyDisconnected = false
                    }
                }
        }

        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { it?.state ?: flowOf(com.juul.kable.State.Disconnected()) }
                .flatMapLatest { st ->
                    if (st is com.juul.kable.State.Disconnected) vitruvianDeviceManager.getScannedDevicesFlow() else flowOf(emptyList())
                }
                .collect { scanned ->
                    val target = state.value.savedDevice ?: return@collect
                    if (autoConnectInProgress || manuallyDisconnected) return@collect
                    val match = scanned.firstOrNull { it.identifier.toString() == target.id }
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
                    val workoutJustFinished = prev != null && workoutState == null
                    val isNonWarmup = workoutJustFinished && prev!!.difficulty != VitruvianDeviceManager.EchoDifficulty.WARMUP

                    val setUserId = if (isNonWarmup) currentUserId.value else 0

                    _state.update { state ->
                        val updatedPrevious = when {
                            workoutJustFinished -> {
                                if (isNonWarmup) prev else null
                            }
                            else -> state.previousWorkoutState
                        }

                        val updatedExerciseName = when {
                            workoutJustFinished -> null
                            else -> state.previousWorkoutExerciseName
                        }

                        val updatedPreviousEntry = when {
                            workoutJustFinished -> null
                            else -> state.previousWorkoutEntry
                        }

                        val updatedPauseStart = when {
                            workoutJustFinished -> currentTimeMillis()
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

                    if (isNonWarmup) {
                        viewModelScope.launch {
                            val entry = WorkoutHistoryEntry(
                                workoutState = prev!!,
                                timestampMillis = currentTimeMillis()
                            )
                            val id = workoutHistoryRepository.insert(entry.toEntity(setUserId))
                            val finalEntry = entry.copy(id = id)
                            _state.update {
                                it.copy(
                                    previousWorkoutExerciseName = null,
                                    previousWorkoutEntry = finalEntry
                                )
                            }
                        }

                        if (twoUserMode.value) {
                            setsCompletedForCurrentUser += 1
                            if (setsCompletedForCurrentUser >= setsPerUser.value) {
                                val nextUser = if (setUserId == 1) 2 else 1
                                setsCompletedForCurrentUser = 0
                                soundPlayer.playUserSwitch(nextUser, state.value.useTts)
                                userRepository.switchToUser(nextUser)
                                platformNotifier.showShortMessage("Switched to User $nextUser")
                            }
                        }
                    }
                }
        }
    }

    private fun observeMachineState() {
        viewModelScope.launch {
            this@WorkoutViewModel._connectedPeripheral
                .flatMapLatest { if (it != null) vitruvianDeviceManager.getMachineStateFlow(it) else flowOf(null) }
                .collect { machineState ->
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

                    val now = currentTimeMillis()

                    if (startHoldSinceMillis == null) {
                        startHoldSinceMillis = now
                        baselineLeft = positionLeft
                        baselineRight = positionRight

                        _connectedPeripheral.value?.let { device ->
                            viewModelScope.launch {
                                try {
                                    vitruvianDeviceManager.prepareForWorkout(device)
                                } catch (_: Throwable) {
                                }
                            }
                        }

                        autoStartTickerJob?.cancel()
                        autoStartTickerJob = viewModelScope.launch {
                            countdownSoundStarted = false
                            while (startHoldSinceMillis != null && state.value.workoutState == null) {
                                val start = startHoldSinceMillis ?: break
                                val elapsedMs = currentTimeMillis() - start
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
                                    val nowCall = currentTimeMillis()
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
                        startHoldSinceMillis = now
                        baselineLeft = positionLeft
                        baselineRight = positionRight
                        countdownSoundStarted = false
                        _state.update { it.copy(autoStartInSeconds = null) }

                        soundPlayer.stopAutoStartCountDown()

                        autoStartTickerJob?.cancel()

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
                                val elapsedMs = currentTimeMillis() - (startHoldSinceMillis ?: break)
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
                                    val nowCall = currentTimeMillis()
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
            _state.update { it.copy(loading = true, pauseStartTimestamp = null) }
            val device = _connectedPeripheral.value ?: return
            val s = state.value
            if (s.fixedWeightMode) {
                vitruvianDeviceManager.startFixedWeightWorkout(
                    userId = currentUserId.value,
                    device = device,
                    weightPerCableKg = s.fixedWeightKg,
                    eccentricPercentage = (s.eccentricSliderValue / 100.0).toDouble(),
                    maxReps = s.repetitionsSliderValue
                )
            } else {
                vitruvianDeviceManager.startWorkout(
                    userId = currentUserId.value,
                    device = device,
                    difficulty = s.echoDifficulty,
                    eccentricPercentage = (s.eccentricSliderValue / 100.0).toDouble(),
                    maxReps = s.repetitionsSliderValue
                )
            }
        } catch (e: Exception) {
            platformLog("WorkoutViewModel", "Error starting workout: ${e.message}")
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
            val history = workoutHistoryRepository.getAllHistory(userId)
            val timeframe = state.value.selectedTrendTimeframe

            val now = currentTimeMillis()
            val limit = when (timeframe) {
                TrendTimeframe.ONE_WEEK -> timeframeStartMillis(now, weeks = 1)
                TrendTimeframe.TWO_WEEKS -> timeframeStartMillis(now, weeks = 2)
                TrendTimeframe.ONE_MONTH -> timeframeStartMillis(now, months = 1)
                TrendTimeframe.THREE_MONTHS -> timeframeStartMillis(now, months = 3)
                TrendTimeframe.ONE_YEAR -> timeframeStartMillis(now, years = 1)
            }

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

    fun onOpenDifficultySettings() {
        val currentDifficulty = state.value.echoDifficulty
        viewModelScope.launch {
            val params = runCatching {
                workoutSettingsRepository.getModeParameters(currentUserId.value, currentDifficulty)
            }.getOrElse {
                ModeParameters(gain = 1.0f, capKg = 50.0f)
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
                workoutSettingsRepository.getModeParameters(currentUserId.value, difficulty)
            }.getOrElse {
                ModeParameters(gain = 1.0f, capKg = 50.0f)
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
            setsCompletedForCurrentUser = 0
            userRepository.switchToUser(nextUser)
            platformNotifier.showShortMessage("Switched to User $nextUser")
        }
    }

    fun onTwoUserModeChange(enabled: Boolean) {
        viewModelScope.launch {
            setsCompletedForCurrentUser = 0
            userRepository.setTwoUserMode(enabled)
        }
    }

    fun onSetsPerUserChange(value: Int) {
        viewModelScope.launch {
            setsCompletedForCurrentUser = 0
            userRepository.setSetsPerUser(value)
        }
    }

    fun onEditExerciseName(entry: WorkoutHistoryEntry) {
        viewModelScope.launch {
            val exercises = workoutHistoryRepository.getAllExerciseNames()
            _state.update { it.copy(showExerciseSelection = entry, exerciseNameSuggestions = exercises) }
        }
    }

    fun onExerciseSelected(name: String) {
        val entry = state.value.showExerciseSelection ?: return
        viewModelScope.launch {
            if (name != entry.exerciseName) {
                workoutHistoryRepository.updateExerciseName(entry.id, name)
            }

            _state.update { s ->
                val updatedPreviousEntry = if (s.previousWorkoutEntry?.id == entry.id) {
                    s.previousWorkoutEntry.copy(exerciseName = name)
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

    fun onDismissExerciseSelection() {
        _state.update { it.copy(showExerciseSelection = null) }
    }

    fun onOpenExerciseNameEditor() {
        viewModelScope.launch {
            val names = workoutHistoryRepository.getAllExerciseNames()
            _state.update { it.copy(showExerciseNameEditor = true, allExerciseNames = names) }
        }
    }

    fun onDismissExerciseNameEditor() {
        _state.update { it.copy(showExerciseNameEditor = false) }
    }

    fun onRenameExercise(oldName: String, newName: String) {
        viewModelScope.launch {
            workoutHistoryRepository.renameExercise(oldName, newName)
            workoutHistoryRepository.renameExerciseInHistory(oldName, newName)
            val names = workoutHistoryRepository.getAllExerciseNames()
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
            workoutHistoryRepository.deleteExercise(name)
            workoutHistoryRepository.renameExerciseInHistory(name, alternativeName)
            val names = workoutHistoryRepository.getAllExerciseNames()
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
        private const val AUTO_START_TOTAL_HOLD_MS: Long = 4000L
        private const val AUTO_START_COUNTDOWN_MS: Long = 3000L
        private const val AUTO_START_PRECOUNT_MS: Long = AUTO_START_TOTAL_HOLD_MS - AUTO_START_COUNTDOWN_MS
        private const val LIFTED_POS_THRESHOLD: Double = 0.1
        private const val HOLD_EPSILON: Double = 0.025
        private const val AUTO_START_DEBOUNCE_MS: Long = 5_000L
    }
}
