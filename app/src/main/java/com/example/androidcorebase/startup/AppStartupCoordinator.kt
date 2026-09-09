package com.example.androidcorebase.startup

import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import com.thanhng224.androidcorebase.core.ui.theme.ThemeManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the persisted theme at process startup with a bounded timeout, so a slow or failing
 * settings read can never hold splash readiness indefinitely. [isReady] becomes `true` exactly
 * once [initialize] returns, whether theme application succeeded, failed, or timed out.
 */
@Singleton
class AppStartupCoordinator
    @Inject
    constructor(
        private val themeManager: ThemeManager,
    ) {
        private val mutableIsReady = MutableStateFlow(false)
        val isReady: StateFlow<Boolean> = mutableIsReady.asStateFlow()

        suspend fun initialize() {
            try {
                withTimeoutOrNull(STARTUP_TIMEOUT_MILLIS) {
                    themeManager.applyTheme(themeManager.currentTheme.first())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("Failed to load the persisted theme; applying the system default")
                themeManager.applyTheme(AppTheme.SYSTEM)
            } finally {
                mutableIsReady.value = true
            }
        }

        private companion object {
            const val STARTUP_TIMEOUT_MILLIS = 2_000L
        }
    }
