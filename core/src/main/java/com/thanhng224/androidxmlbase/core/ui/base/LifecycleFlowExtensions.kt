package com.thanhng224.androidxmlbase.core.ui.base

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Collects [this] on [lifecycleOwner]'s scope, restarting whenever it re-enters [Lifecycle.State.STARTED].
 * Shared by every `core/ui/base` host (Activity/Fragment/BottomSheet) so the collection rules stay identical.
 */
internal fun <T> Flow<T>.collectOnStartedBy(
    lifecycleOwner: LifecycleOwner,
    action: suspend (T) -> Unit,
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect(action)
        }
    }
}
