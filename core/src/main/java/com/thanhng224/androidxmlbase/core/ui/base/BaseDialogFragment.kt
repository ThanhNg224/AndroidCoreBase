package com.thanhng224.androidxmlbase.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.thanhng224.androidxmlbase.core.R

public enum class DialogAnimation {
    NONE,
    FADE,
    SLIDE,
    SCALE,
}

/**
 * Base class for all customized DialogFragments. Handles standard margins,
 * tablet width limits, corner rounding, and entry/exit animation styling.
 */
public abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed before onViewCreated() completed" }

    protected abstract fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): VB

    /** Chosen entrance/exit transition animation style. Override this to customize. */
    protected open val dialogAnimation: DialogAnimation = DialogAnimation.SCALE

    /** Background drawable. Override to use custom shapes. */
    protected open val backgroundDrawableRes: Int = R.drawable.core_bg_dialog_surface

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bindingOrNull = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        onBindingReady(savedInstanceState)
    }

    /** Subclasses do their wiring here instead of overriding onViewCreated */
    protected abstract fun onBindingReady(savedInstanceState: Bundle?)

    override fun onStart() {
        super.onStart()
        setupDialogWindow()
    }

    private fun setupDialogWindow() {
        dialog?.window?.let { window ->
            window.setBackgroundDrawableResource(backgroundDrawableRes)

            // Cap width at max width with margin
            val screenWidth = resources.displayMetrics.widthPixels
            val margin = resources.getDimensionPixelSize(R.dimen.core_dialog_screen_margin) * 2
            val maxWidth = resources.getDimensionPixelSize(R.dimen.core_dialog_max_width)
            val calculatedWidth = (screenWidth - margin).coerceAtMost(maxWidth)

            window.setLayout(calculatedWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

            val animStyle =
                when (dialogAnimation) {
                    DialogAnimation.NONE -> 0
                    DialogAnimation.FADE -> R.style.Animation_AndroidXmlBase_Dialog_Fade
                    DialogAnimation.SLIDE -> R.style.Animation_AndroidXmlBase_Dialog_Slide
                    DialogAnimation.SCALE -> R.style.Animation_AndroidXmlBase_Dialog_Scale
                }
            if (animStyle != 0) {
                window.setWindowAnimations(animStyle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingOrNull = null
    }
}
