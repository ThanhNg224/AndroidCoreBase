package com.thanhng224.androidxmlbase.core.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.thanhng224.androidxmlbase.core.R
import com.thanhng224.androidxmlbase.core.databinding.DialogPromptBinding
import com.thanhng224.androidxmlbase.core.ui.base.BaseDialogFragment
import com.thanhng224.androidxmlbase.core.ui.base.DialogAnimation
import com.thanhng224.androidxmlbase.core.ui.base.setOnDebouncedClickListener

internal enum class PromptType {
    SUCCESS,
    ERROR,
    INFO,
}

/** Reusable status dialog (Success, Error, Info) with customizable actions. */
internal class PromptDialogFragment : BaseDialogFragment<DialogPromptBinding>() {
    override val dialogAnimation: DialogAnimation = DialogAnimation.SCALE

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): DialogPromptBinding = DialogPromptBinding.inflate(inflater, container, false)

    var onPrimary: (() -> Unit)? = null
    var onSecondary: (() -> Unit)? = null

    // Legacy properties
    var onRetry: (() -> Unit)?
        get() = onPrimary
        set(value) {
            onPrimary = value
        }
    var onClose: (() -> Unit)?
        get() = onSecondary
        set(value) {
            onSecondary = value
        }

    override fun onBindingReady(savedInstanceState: Bundle?) {
        val args = requireArguments()

        binding.tvPromptMessage.text = args.getString(ARG_MESSAGE)

        val technicalCode = args.getString(ARG_TECHNICAL_CODE)
        binding.tvPromptCode.text = technicalCode
        binding.tvPromptCode.visibility = if (technicalCode.isNullOrEmpty()) View.GONE else View.VISIBLE

        val type = args.getString(ARG_TYPE)?.let { PromptType.valueOf(it) } ?: PromptType.ERROR
        val iconRes =
            when (type) {
                PromptType.SUCCESS -> R.drawable.ic_prompt_success
                PromptType.ERROR -> R.drawable.ic_prompt_error
                PromptType.INFO -> R.drawable.ic_prompt_info
            }
        binding.ivPromptIcon.setImageResource(iconRes)

        val primaryTextRes = args.getInt(ARG_PRIMARY_TEXT_RES, R.string.core_error_dialog_retry)
        binding.tvPrimaryText.setText(primaryTextRes)
        binding.btnPrimary.setOnDebouncedClickListener {
            emitResult(EVENT_PRIMARY)
            onPrimary?.invoke()
            dismiss()
        }

        val secondaryTextRes = args.getInt(ARG_SECONDARY_TEXT_RES, 0).takeIf { it != 0 }
        if (secondaryTextRes != null) {
            binding.tvSecondaryText.setText(secondaryTextRes)
            binding.btnSecondary.visibility = View.VISIBLE
            binding.btnSecondary.setOnDebouncedClickListener {
                emitResult(EVENT_SECONDARY)
                onSecondary?.invoke()
                dismiss()
            }
        } else {
            binding.btnSecondary.visibility = View.GONE
        }
    }

    private fun emitResult(event: String) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putString(EVENT_KEY, event)
            },
        )
    }

    companion object {
        private const val ARG_MESSAGE = "message"
        private const val ARG_TECHNICAL_CODE = "technical_code"
        private const val ARG_TYPE = "type"
        private const val ARG_PRIMARY_TEXT_RES = "primary_text_res"
        private const val ARG_SECONDARY_TEXT_RES = "secondary_text_res"

        const val RESULT_KEY =
            "com.thanhng224.androidxmlbase.core.ui.components.PromptDialogFragment.result"

        const val EVENT_KEY = "event"
        const val EVENT_PRIMARY = "primary"
        const val EVENT_SECONDARY = "secondary"

        // Legacy aliases
        const val EVENT_RETRY = EVENT_PRIMARY
        const val EVENT_CLOSE = EVENT_SECONDARY

        fun newInstance(
            message: String,
            technicalCode: String? = null,
            type: PromptType = PromptType.ERROR,
            @StringRes primaryButtonTextResId: Int = R.string.core_error_dialog_retry,
            @StringRes secondaryButtonTextResId: Int? = R.string.core_error_dialog_close,
        ): PromptDialogFragment =
            PromptDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_MESSAGE, message)
                        putString(ARG_TECHNICAL_CODE, technicalCode)
                        putString(ARG_TYPE, type.name)
                        putInt(ARG_PRIMARY_TEXT_RES, primaryButtonTextResId)
                        putInt(ARG_SECONDARY_TEXT_RES, secondaryButtonTextResId ?: 0)
                    }
            }
    }
}
