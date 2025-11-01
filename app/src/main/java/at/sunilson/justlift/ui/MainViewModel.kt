package at.sunilson.justlift.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(
    private val application: Application
) : ViewModel()