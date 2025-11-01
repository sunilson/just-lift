package at.sunilson.justlift

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Just Lift",
    ) {
        App()
    }
}