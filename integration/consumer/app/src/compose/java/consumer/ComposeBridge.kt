package consumer

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import com.thanhng224.androidcorebase.core.ui.base.setThemedContent

/** Compose build (`-PincludeCompose=true`): exercises `:ui-compose`'s public `setThemedContent`. */
internal object ComposeBridge {
    fun attach(
        activity: Activity,
        container: FrameLayout,
    ) {
        val composeView = ComposeView(activity)
        composeView.setThemedContent {
            Text(text = "Compose OK", style = MaterialTheme.typography.bodyMedium)
        }
        container.addView(composeView)
    }
}
