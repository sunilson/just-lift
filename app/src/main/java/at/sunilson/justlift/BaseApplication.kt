package at.sunilson.justlift

import android.app.Application
import at.sunilson.justlift.di.AppScanModule
import at.sunilson.justlift.platform.AppSoundPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module
import timber.log.Timber

class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@BaseApplication)
            modules(
                AppScanModule().module,
                module { single { AppSoundPlayer(get()) } }
            )
        }
    }

}
