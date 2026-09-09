package com.example.androidcorebase.startup

import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import com.thanhng224.androidcorebase.core.ui.theme.ThemeManager
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber
import java.io.IOException

private class FakeThemeManager(
    private val currentThemeFlow: Flow<AppTheme>,
) : ThemeManager {
    var lastApplied: AppTheme? = null
        private set

    override val currentTheme: Flow<AppTheme> = currentThemeFlow
    override val isThemeApplied: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun getTheme(): AppTheme = AppTheme.SYSTEM

    override suspend fun setTheme(theme: AppTheme) {
        lastApplied = theme
    }

    override fun applyTheme(theme: AppTheme) {
        lastApplied = theme
    }
}

class AppStartupCoordinatorTest {
    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `IO failure applies the system default theme and marks readiness`() =
        runTest {
            val themeManager = FakeThemeManager(flow { throw IOException("disk read failed") })
            val coordinator = AppStartupCoordinator(themeManager)

            coordinator.initialize()

            assertEquals(AppTheme.SYSTEM, themeManager.lastApplied)
            assertTrue(coordinator.isReady.value)
        }

    @Test
    fun `a theme read that never completes cannot hold readiness past the startup timeout`() =
        runTest {
            val themeManager = FakeThemeManager(flow { awaitCancellation() })
            val coordinator = AppStartupCoordinator(themeManager)

            coordinator.initialize()

            assertTrue(coordinator.isReady.value)
        }

    @Test
    fun `IO failure never logs the underlying exception message`() =
        runTest {
            val loggedMessages = mutableListOf<String>()
            Timber.plant(
                object : Timber.Tree() {
                    override fun log(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?,
                    ) {
                        loggedMessages.add(message)
                    }
                },
            )
            val secretDetail = "super-secret-disk-path"
            val themeManager = FakeThemeManager(flow { throw IOException(secretDetail) })
            val coordinator = AppStartupCoordinator(themeManager)

            coordinator.initialize()

            assertTrue(loggedMessages.none { it.contains(secretDetail) })
        }
}
