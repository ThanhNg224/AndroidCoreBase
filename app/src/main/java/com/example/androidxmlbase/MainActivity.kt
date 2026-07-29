package com.example.androidxmlbase

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.androidxmlbase.databinding.ActivityAppshellMainBinding
import com.example.androidxmlbase.feature.settings.presentation.ui.SettingsActivity
import com.thanhng224.androidxmlbase.core.navigation.ActivityDestination
import com.thanhng224.androidxmlbase.core.navigation.ActivityNavigator
import com.thanhng224.androidxmlbase.core.ui.base.BaseActivity
import com.thanhng224.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityAppshellMainBinding>() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !themeManager.isThemeApplied.value }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityAppshellMainBinding = ActivityAppshellMainBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        // BaseActivity already pads the root by the navigation-bar inset, which lifts the whole nav
        // card clear of the bars. Material's BottomNavigationView installs its own listener that
        // pads itself by that same inset, which would leave a dead strip inside the card -- so
        // replace that listener with one that consumes the insets and pads nothing.
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { _, _ ->
            WindowInsetsCompat.CONSUMED
        }

        val navController =
            (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment)
                .navController
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.topAppBar.title = destination.label
        }

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionSettings -> {
                    activityNavigator.navigate(this, ActivityDestination(SettingsActivity::class))
                    true
                }

                else -> false
            }
        }
    }
}
