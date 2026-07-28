package com.thanhng224.androidxmlbase.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import javax.inject.Inject

class ActivityNavigator
    @Inject
    constructor() {
        fun navigate(
            context: Context,
            destination: ActivityDestination,
        ) {
            val intent =
                Intent(context, destination.activityClass.java).apply {
                    destination.extras?.let { putExtras(BundleCompat.copyOf(it)) }
                    addFlags(destination.options.toIntentFlags())
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    putExtra(EXTRA_TRANSITION_TYPE, destination.options.transitionType.name)
                }
            context.startActivity(intent)
            if (context is Activity) {
                applyTransition(context, destination.options.transitionType, isOpen = true)
            }
        }

        fun finish(activity: Activity) {
            activity.finish()
        }

        companion object {
            const val EXTRA_TRANSITION_TYPE = "extra_transition_type"

            fun applyTransition(
                activity: Activity,
                type: TransitionType,
                isOpen: Boolean,
            ) {
                val enterAnim: Int
                val exitAnim: Int
                when (type) {
                    TransitionType.NONE -> {
                        enterAnim = 0
                        exitAnim = 0
                    }
                    TransitionType.SLIDE_HORIZONTAL -> {
                        if (isOpen) {
                            enterAnim = com.thanhng224.androidxmlbase.core.R.anim.transition_slide_in_from_right
                            exitAnim = com.thanhng224.androidxmlbase.core.R.anim.transition_slide_out_to_left
                        } else {
                            enterAnim = com.thanhng224.androidxmlbase.core.R.anim.transition_slide_in_from_left
                            exitAnim = com.thanhng224.androidxmlbase.core.R.anim.transition_slide_out_to_right
                        }
                    }
                    TransitionType.FADE -> {
                        enterAnim = com.thanhng224.androidxmlbase.core.R.anim.transition_fade_in
                        exitAnim = com.thanhng224.androidxmlbase.core.R.anim.transition_fade_out
                    }
                    TransitionType.DEFAULT -> return
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val overrideType =
                        if (isOpen) {
                            Activity.OVERRIDE_TRANSITION_OPEN
                        } else {
                            Activity.OVERRIDE_TRANSITION_CLOSE
                        }
                    activity.overrideActivityTransition(overrideType, enterAnim, exitAnim)
                } else {
                    @Suppress("DEPRECATION")
                    activity.overridePendingTransition(enterAnim, exitAnim)
                }
            }
        }
    }
