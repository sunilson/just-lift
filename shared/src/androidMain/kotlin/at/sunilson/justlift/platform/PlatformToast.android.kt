package at.sunilson.justlift.platform

import android.content.Context
import android.widget.Toast
import org.koin.core.annotation.Single

@Single(binds = [PlatformNotifier::class])
class AndroidNotifier(private val context: Context) : PlatformNotifier {
    override fun showShortMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
