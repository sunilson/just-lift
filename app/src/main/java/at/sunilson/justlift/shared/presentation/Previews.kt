package at.sunilson.justlift.shared.presentation

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview("LightPhonePortrait", device = "spec:width=411dp,height=891dp,orientation=portrait")
@Preview("DarkPhonePortrait", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL, device = "spec:width=411dp,height=891dp,orientation=portrait")
//@Preview("LightTabletLandscape", device = "spec:width=800dp,height=1280dp,orientation=landscape")
// @Preview("DarkTabletLandscape", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL, device = "spec:width=800dp,height=1280dp,orientation=landscape")
annotation class PreviewLightDarkDevices

@Composable
fun ScreenPreview(content: @Composable () -> Unit) {
    JustLiftTheme {
        Scaffold {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(it)
            ) {
                content()
            }
        }
    }
}
