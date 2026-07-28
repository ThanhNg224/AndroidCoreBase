package com.example.androidxmlbase.feature.settings.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import com.example.androidxmlbase.R
import com.example.androidxmlbase.databinding.ActivitySettingsBinding
import com.example.androidxmlbase.feature.settings.presentation.state.SettingsUiEffect
import com.example.androidxmlbase.feature.settings.presentation.state.SettingsUiEvent
import com.example.androidxmlbase.feature.settings.presentation.state.SettingsUiState
import com.example.androidxmlbase.feature.settings.presentation.viewmodel.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.ui.base.BaseActivity
import com.thanhng224.androidxmlbase.core.ui.base.TransitionActivity
import com.thanhng224.androidxmlbase.core.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater): ActivitySettingsBinding = ActivitySettingsBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.rowAppearance.setOnClickListener { showThemeDialog(viewModel.state.value.theme) }
        binding.rowLanguage.setOnClickListener { showLanguageDialog(viewModel.state.value.language) }

        viewModel.state.collectOnStarted(::render)
        viewModel.effect.collectOnStarted(::handleEffect)
    }

    private fun render(state: SettingsUiState) {
        binding.tvAppearanceSummary.setText(state.theme.labelResId)
        binding.tvLanguageSummary.setText(state.language?.displayNameResId ?: R.string.settings_language_system)
    }

    private fun showThemeDialog(selectedTheme: AppTheme) {
        val options = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK)
        showSingleChoiceDialog(
            titleResId = R.string.settings_appearance_dialog_title,
            labels = options.map { getString(it.labelResId) }.toTypedArray(),
            checkedItem = options.indexOf(selectedTheme),
        ) { selectedIndex ->
            viewModel.onEvent(SettingsUiEvent.ThemeSelected(options[selectedIndex]))
        }
    }

    private fun showLanguageDialog(selectedLanguage: AppLanguage?) {
        val options = listOf(null, AppLanguage.ENGLISH, AppLanguage.VIETNAMESE)
        showSingleChoiceDialog(
            titleResId = R.string.settings_language_dialog_title,
            labels = options.map { language -> getString(language?.displayNameResId ?: R.string.settings_language_system) }.toTypedArray(),
            checkedItem = options.indexOf(selectedLanguage),
        ) { selectedIndex ->
            viewModel.onEvent(SettingsUiEvent.LanguageSelected(options[selectedIndex]))
        }
    }

    private fun showSingleChoiceDialog(
        titleResId: Int,
        labels: Array<String>,
        checkedItem: Int,
        onSelected: (Int) -> Unit,
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(titleResId)
            .setSingleChoiceItems(labels, checkedItem) { dialog, selectedIndex ->
                onSelected(selectedIndex)
                dialog.dismiss()
            }.setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun handleEffect(effect: SettingsUiEffect) {
        when (effect) {
            is SettingsUiEffect.ApplyLanguage -> startLanguageTransition(effect.language)
        }
    }

    private fun startLanguageTransition(language: AppLanguage?) {
        startActivity(
            TransitionActivity.createIntent(
                context = this,
                actionKey = LanguageTransitionAction.KEY,
                extras =
                    Bundle().apply {
                        putString(LanguageTransitionAction.EXTRA_LANGUAGE_TAG, language?.languageTag.orEmpty())
                    },
            ),
        )
    }

    private val AppTheme.labelResId: Int
        get() =
            when (this) {
                AppTheme.SYSTEM -> R.string.settings_theme_system
                AppTheme.LIGHT -> R.string.settings_theme_light
                AppTheme.DARK -> R.string.settings_theme_dark
            }
}
