package com.thanhng224.androidxmlbase.core.ui.base

import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.thanhng224.androidxmlbase.core.R
import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import com.thanhng224.androidxmlbase.core.ui.components.FullScreenLoaderView
import com.thanhng224.androidxmlbase.core.ui.components.PromptDialogFragment
import com.thanhng224.androidxmlbase.core.ui.text.resolve

/**
 * Renders [result] onto [contentRoot] (full-screen loader) and [dialogHost] (error prompt),
 * invoking [onSuccess] on load. Shared by [BaseActivity] and [BaseFragment] so the loading/error
 * UI stays identical regardless of which host a screen is built on.
 */
internal fun <T> renderResultState(
    result: ResultState<T>,
    contentRoot: ViewGroup,
    dialogHost: FragmentManager,
    onSuccess: (T) -> Unit,
) {
    when (result) {
        is ResultState.Loading -> showFullScreenLoader(contentRoot)
        is ResultState.Success -> {
            hideFullScreenLoader(contentRoot)
            onSuccess(result.data)
        }
        is ResultState.Error -> {
            hideFullScreenLoader(contentRoot)
            showErrorPrompt(dialogHost, result.message.resolve(contentRoot.context))
        }
    }
}

private fun showFullScreenLoader(root: ViewGroup) {
    var loader = root.findViewById<FullScreenLoaderView>(R.id.core_full_screen_loader)
    if (loader == null) {
        loader =
            FullScreenLoaderView(root.context).apply {
                id = R.id.core_full_screen_loader
            }
        root.addView(
            loader,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }
    loader.show()
}

private fun hideFullScreenLoader(root: ViewGroup) {
    root.findViewById<FullScreenLoaderView>(R.id.core_full_screen_loader)?.hide()
}

private fun showErrorPrompt(
    fragmentManager: FragmentManager,
    message: String,
) {
    PromptDialogFragment
        .newInstance(message = message)
        .show(fragmentManager, "error_prompt_dialog")
}
