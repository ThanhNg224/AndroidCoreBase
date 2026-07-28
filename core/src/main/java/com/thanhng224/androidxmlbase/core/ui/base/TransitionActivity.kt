package com.thanhng224.androidxmlbase.core.ui.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.thanhng224.androidxmlbase.core.R
import com.thanhng224.androidxmlbase.core.ui.transition.TransitionAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Opaque full-screen host for a single [TransitionAction]: shows a loading animation (its
 * colors are static, authored directly into `transition_loading_pulse.json` — see that file if the palette
 * needs to change), runs the action registered under the caller-supplied action key, then
 * finishes.
 * One Activity, one manifest entry — a new transition use case only needs a new
 * [TransitionAction] implementation registered into the Hilt multibinding map, not a new
 * Activity subclass or manifest entry.
 *
 * Extends [AppCompatActivity] (not the lighter `ComponentActivity`): matches this project's
 * convention of every other real screen using `AppCompatActivity` (via `BaseActivity`), and,
 * as a side benefit, makes an isolated single-Activity instrumented test of this class correct
 * without depending on another Activity being alive alongside it. In real usage this Activity is
 * always launched on top of an already-alive `AppCompatActivity` (e.g. `MainActivity`), so
 * `ComponentActivity` would have worked fine there too — this isn't required for production
 * correctness, it's just harmless and more consistent.
 */
@AndroidEntryPoint
class TransitionActivity : AppCompatActivity() {
    @Inject
    lateinit var actions: Map<String, @JvmSuppressWildcards TransitionAction>

    private var actionHasCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transition)

        if (savedInstanceState?.getBoolean(STATE_ACTION_COMPLETED) == true) {
            actionHasCompleted = true
            finishAfterSettleDelay()
        } else {
            animateEntrance()
            runActionAfterTransitionIsOpaque()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_ACTION_COMPLETED, actionHasCompleted)
        super.onSaveInstanceState(outState)
    }

    private fun animateEntrance() {
        val content = findViewById<View>(R.id.transitionContent)
        content.alpha = ENTRANCE_START_ALPHA
        content.scaleX = ENTRANCE_START_SCALE
        content.scaleY = ENTRANCE_START_SCALE
        content
            .animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ENTRANCE_ANIM_DURATION_MS)
            .start()
    }

    private fun runActionAfterTransitionIsOpaque() {
        lifecycleScope.launch {
            delay(TRANSITION_ENTER_DURATION_MS.milliseconds)
            val actionKey = requireNotNull(intent.getStringExtra(EXTRA_ACTION_KEY)) { "Missing $EXTRA_ACTION_KEY extra" }
            actions.getValue(actionKey).perform(intent.extras ?: Bundle.EMPTY)
            actionHasCompleted = true
            finishAfterSettleDelay()
        }
    }

    private fun finishAfterSettleDelay() {
        lifecycleScope.launch {
            delay(TRANSITION_SETTLE_DURATION_MS.milliseconds)
            finish()
        }
    }

    companion object {
        private const val EXTRA_ACTION_KEY = "extra_action_key"
        private const val STATE_ACTION_COMPLETED = "state_action_completed"
        private const val TRANSITION_ENTER_DURATION_MS = 320L
        private const val TRANSITION_SETTLE_DURATION_MS = 80L
        private const val ENTRANCE_ANIM_DURATION_MS = 280L
        private const val ENTRANCE_START_ALPHA = 0f
        private const val ENTRANCE_START_SCALE = 0.9f

        fun createIntent(
            context: Context,
            actionKey: String,
            extras: Bundle = Bundle.EMPTY,
        ): Intent =
            Intent(context, TransitionActivity::class.java)
                .putExtra(EXTRA_ACTION_KEY, actionKey)
                .putExtras(extras)
    }
}
