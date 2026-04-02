package at.sunilson.justlift.features.workout.presentation

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.WorkoutSettingsRepository
import at.sunilson.justlift.features.workout.data.WorkoutHistoryRepository
import at.sunilson.justlift.features.user.data.UserRepository
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryDao
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryUiModel
import at.sunilson.justlift.features.workout.presentation.history.toDomain
import at.sunilson.justlift.platform.AppSoundPlayer
import at.sunilson.justlift.platform.PlatformNotifier
import at.sunilson.justlift.platform.formatDateShort
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koin.android.annotation.KoinViewModel

/**
 * Android-specific ViewModel subclass that adds Paging support for history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AndroidWorkoutViewModel(
    vitruvianDeviceManager: VitruvianDeviceManager,
    soundPlayer: AppSoundPlayer,
    workoutSettingsRepository: WorkoutSettingsRepository,
    workoutHistoryRepository: WorkoutHistoryRepository,
    userRepository: UserRepository,
    platformNotifier: PlatformNotifier,
    private val workoutHistoryDao: WorkoutHistoryDao
) : WorkoutViewModel(
    vitruvianDeviceManager = vitruvianDeviceManager,
    soundPlayer = soundPlayer,
    workoutSettingsRepository = workoutSettingsRepository,
    workoutHistoryRepository = workoutHistoryRepository,
    userRepository = userRepository,
    platformNotifier = platformNotifier
) {
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

                val afterDate = formatDateShort(afterEntry.timestampMillis)
                if (beforeEntry == null) return@insertSeparators WorkoutHistoryUiModel.Header(afterDate)

                val beforeDate = formatDateShort(beforeEntry.timestampMillis)
                if (beforeDate != afterDate) {
                    WorkoutHistoryUiModel.Header(afterDate)
                } else {
                    null
                }
            }
    }.cachedIn(viewModelScope)
}
