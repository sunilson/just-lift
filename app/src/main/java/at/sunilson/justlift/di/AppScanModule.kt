package at.sunilson.justlift.di

import at.sunilson.justlift.features.workout.data.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DatabaseModule::class])
@ComponentScan("at.sunilson.justlift")
class AppScanModule
