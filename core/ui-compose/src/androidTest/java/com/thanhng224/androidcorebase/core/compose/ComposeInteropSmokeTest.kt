package com.thanhng224.androidcorebase.core.compose

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thanhng224.androidcorebase.core.ui.base.setThemedContent
import com.thanhng224.androidcorebase.core.ui.theme.AndroidCoreBaseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.thanhng224.androidcorebase.core.R as CoreR

/**
 * The risk in this module is not its ~89 lines of glue but the module boundary: the theme bridge
 * reads `:core`'s `core_color_*` resources across an AAR boundary, and `setThemedContent` has to
 * compose inside a plain XML [ComposeView]. Both fail at runtime, not at compile time.
 */
@RunWith(AndroidJUnit4::class)
class ComposeInteropSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val coreColorPrimary: Color
        get() = Color(ContextCompat.getColor(composeRule.activity, CoreR.color.core_color_primary))

    @Test
    fun themeBridgesCoreColorResources() {
        var primary: Color? = null
        composeRule.setContent {
            AndroidCoreBaseTheme { primary = MaterialTheme.colorScheme.primary }
        }
        composeRule.waitForIdle()

        assertEquals(coreColorPrimary, primary)
    }

    @Test
    fun setThemedContentComposesInsideAComposeView() {
        var primary: Color? = null
        composeRule.activity.runOnUiThread {
            val composeView = ComposeView(composeRule.activity)
            composeRule.activity.setContentView(composeView)
            composeView.setThemedContent {
                primary = MaterialTheme.colorScheme.primary
                Text(text = "smoke", modifier = Modifier.testTag("smoke_content"))
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("smoke_content").assertExists()
        assertEquals(coreColorPrimary, primary)
    }
}
