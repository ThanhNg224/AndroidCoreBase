package com.example.androidcorebase.feature.settings.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.androidcorebase.R
import com.example.androidcorebase.databinding.FragmentSettingsBinding
import com.example.androidcorebase.feature.settings.presentation.state.PendingSettingsMessage
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiEvent
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiState
import com.example.androidcorebase.feature.settings.presentation.viewmodel.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.ui.base.BaseFragment
import com.thanhng224.androidcorebase.core.ui.text.resolve
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    private val viewModel: SettingsViewModel by viewModels()
    private var lastShownMessageId: Long? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentSettingsBinding = FragmentSettingsBinding.inflate(inflater, container, false)

    override fun onBindingReady(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        binding.rowAppearance.setOnClickListener { showThemeDialog(viewModel.state.value.theme) }
        binding.rowLanguage.setOnClickListener {
            showLanguageDialog(viewModel.state.value.language, viewModel.state.value.supportedLanguages)
        }

        viewModel.state.collectOnStarted(::render)
    }

    private fun render(state: SettingsUiState) {
        binding.tvAppearanceSummary.setText(state.theme.labelResId)
        binding.tvLanguageSummary.setText(state.language?.displayNameResId ?: R.string.settings_language_system)

        val pending = state.pendingMessages.firstOrNull()
        if (pending != null && pending.id != lastShownMessageId) {
            lastShownMessageId = pending.id
            showPendingMessage(pending)
        }
    }

    private fun showPendingMessage(message: PendingSettingsMessage) {
        Snackbar
            .make(binding.root, message.text.resolve(requireContext()), Snackbar.LENGTH_LONG)
            .addCallback(
                object : Snackbar.Callback() {
                    override fun onDismissed(
                        transientBottomBar: Snackbar?,
                        event: Int,
                    ) {
                        viewModel.onMessageHandled(message.id)
                    }
                },
            ).show()
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

    private fun showLanguageDialog(
        selectedLanguage: AppLanguage?,
        supportedLanguages: List<AppLanguage>,
    ) {
        val options = listOf(null) + supportedLanguages
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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleResId)
            .setSingleChoiceItems(labels, checkedItem) { dialog, selectedIndex ->
                onSelected(selectedIndex)
                dialog.dismiss()
            }.setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private val AppTheme.labelResId: Int
        get() =
            when (this) {
                AppTheme.SYSTEM -> R.string.settings_theme_system
                AppTheme.LIGHT -> R.string.settings_theme_light
                AppTheme.DARK -> R.string.settings_theme_dark
            }
}
