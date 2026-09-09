package consumer

import android.app.Activity
import android.widget.FrameLayout

/** Main-only build: nothing to attach, Compose is not on this build's classpath at all. */
internal object ComposeBridge {
    fun attach(
        activity: Activity,
        container: FrameLayout,
    ) = Unit
}
