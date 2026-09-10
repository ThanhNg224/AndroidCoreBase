package com.example.androidcorebase.startup

import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import com.thanhng224.androidcorebase.core.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Executors

private class FakeThemeManager(
    private val currentThemeFlow: Flow<AppTheme>,
) : ThemeManager {
    var lastApplied: AppTheme? = null
        private set

    /** Name of the thread [applyTheme] last ran on, so tests can prove the main-thread hop. */
    var lastAppliedOnThread: String? = null
        private set

    override val currentTheme: Flow<AppTheme> = currentThemeFlow
    override val isThemeApplied: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun getTheme(): AppTheme = AppTheme.SYSTEM

    override suspend fun setTheme(theme: AppTheme) {
        lastApplied = theme
    }

    override fun applyTheme(theme: AppTheme) {
        lastApplied = theme
        lastAppliedOnThread = Thread.currentThread().name
    }
}

class AppStartupCoordinatorTest {
    /**
     * Regression: the coordinator used to call `applyTheme` straight from the application scope's
     * default dispatcher. `AppCompatDelegate.setDefaultNightMode` recreates live Activities and
     * throws `IllegalStateException: Must be called from main thread` off the main thread, but only
     * once an Activity exists -- so this passed on fast devices and crashed the process on slower
     * ones (caught by the CI emulator, not by any physical device).
     */
    @Test
    fun `applies the theme on the main dispatcher`() {
        val mainExecutor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, MAIN_THREAD_NAME) }
        Dispatchers.setMain(mainExecutor.asCoroutineDispatcher())
        try {
            val themeManager = FakeThemeManager(flowOf(AppTheme.DARK))
            runBlocking(Dispatchers.Default) {
                AppStartupCoordinator(themeManager).initialize()
            }
            assertEquals(AppTheme.DARK, themeManager.lastApplied)
            // Coroutine debug mode appends " @coroutine#N" to the thread name.
            assertTrue(
                "expected the apply to run on $MAIN_THREAD_NAME, was ${themeManager.lastAppliedOnThread}",
                themeManager.lastAppliedOnThread.orEmpty().startsWith(MAIN_THREAD_NAME),
            )
        } finally {
            Dispatchers.resetMain()
            mainExecutor.shutdown()
        }
    }

    private companion object {
        const val MAIN_THREAD_NAME = "test-main-dispatcher"
    }

    @Before
    fun setUp() {
        // The coordinator hops to Dispatchers.Main to apply the theme, so every test needs one.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
