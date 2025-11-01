package at.sunilson.justlift.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("at.sunilson.justlift.**")
class AppScanModule