package at.sunilson.justlift.di

import at.sunilson.justlift.features.user.data.UserRepository
import at.sunilson.justlift.features.user.data.UserRepositoryImpl
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManagerImpl
import at.sunilson.justlift.features.workout.data.WorkoutHistoryRepository
import at.sunilson.justlift.features.workout.data.WorkoutHistoryRepositoryImpl
import at.sunilson.justlift.features.workout.data.WorkoutSettingsRepository
import at.sunilson.justlift.features.workout.data.WorkoutSettingsRepositoryImpl
import at.sunilson.justlift.features.workout.data.database.IosAppDatabase
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryDao
import at.sunilson.justlift.features.workout.data.database.createIosDatabase
import at.sunilson.justlift.features.workout.presentation.WorkoutViewModel
import at.sunilson.justlift.platform.AppSoundPlayer
import at.sunilson.justlift.platform.IosNotifier
import at.sunilson.justlift.platform.PlatformNotifier
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val iosModule = module {
    singleOf(::AppSoundPlayer)
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    singleOf(::IosNotifier) bind PlatformNotifier::class

    single<IosAppDatabase> { createIosDatabase() }
    single<WorkoutHistoryDao> { get<IosAppDatabase>().workoutHistoryDao() }

    single<WorkoutSettingsRepository> { WorkoutSettingsRepositoryImpl() }
    single<VitruvianDeviceManager> { VitruvianDeviceManagerImpl(get()) }
    single<WorkoutHistoryRepository> { WorkoutHistoryRepositoryImpl(get()) }

    factory {
        WorkoutViewModel(
            vitruvianDeviceManager = get(),
            soundPlayer = get(),
            workoutSettingsRepository = get(),
            workoutHistoryRepository = get(),
            userRepository = get(),
            platformNotifier = get()
        )
    }
}
