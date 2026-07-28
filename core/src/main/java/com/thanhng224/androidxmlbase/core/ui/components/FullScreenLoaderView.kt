package com.thanhng224.androidxmlbase.core.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.thanhng224.androidxmlbase.core.R

/** Full-screen blocking loader shown during async operations (e.g. API calls). */
internal class FullScreenLoaderView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val statusText: TextView

        init {
            LayoutInflater.from(context).inflate(R.layout.view_full_screen_loader, this, true)
            statusText = findViewById(R.id.tvLoaderStatus)
            isClickable = true
            isFocusable = true
            visibility = GONE
        }

        fun show() {
            show(R.string.core_design_system_result_loading)
        }

        fun show(statusText: String) {
            this.statusText.text = statusText
            showInternal()
        }

        fun show(statusResId: Int) {
            this.statusText.text = context.getString(statusResId)
            showInternal()
        }

        private fun showInternal() {
            if (visibility != VISIBLE) {
                alpha = 0f
                visibility = VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(ANIM_DURATION)
                    .setListener(null)
                    .start()
            }
        }

        fun hide() {
            if (isVisible) {
                animate()
                    .alpha(0f)
                    .setDuration(ANIM_DURATION)
                    .setListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                visibility = GONE
                            }
                        },
                    ).start()
            }
        }

        companion object {
            private const val ANIM_DURATION = 200L
        }
    }
