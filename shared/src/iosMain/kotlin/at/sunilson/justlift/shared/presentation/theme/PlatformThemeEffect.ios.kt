package at.sunilson.justlift.shared.presentation.theme

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformThemeEffect(darkTheme: Boolean) {
    // No-op on iOS - status bar styling handled by iOS platform layer
}
