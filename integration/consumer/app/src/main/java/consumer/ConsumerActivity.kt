package consumer

import android.app.Activity
import android.os.Bundle
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.thanhng224.androidcorebase.core.foundation.AppDispatchers
import com.thanhng224.androidcorebase.core.network.ApiClient
import com.thanhng224.androidcorebase.core.network.NetworkClientFactory
import com.thanhng224.androidcorebase.core.storage.settings.SettingsStoreFactory
import consumer.app.databinding.ActivityConsumerBinding

/**
 * Exercises the main-only public surface of the published `AndroidCoreBase` artifact: no Hilt, no
 * Compose, no dependency-injection framework -- plain public constructors and factories, wired into
 * a plain XML/ViewBinding screen. [ComposeBridge.attach] additionally exercises `:ui-compose` when
 * this module is built with `-PincludeCompose=true` (see build.gradle.kts's conditional source set).
 */
class ConsumerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityConsumerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dispatchers: AppDispatchers = AppDispatchers.default()
        val settingsStore =
            SettingsStoreFactory.create(
                PreferenceDataStoreFactory.create(
                    produceFile = { applicationContext.preferencesDataStoreFile("consumer_settings") },
                ),
            )
        val apiClient: ApiClient = NetworkClientFactory.createApiClient()

        binding.tvStatus.text =
            "dispatchers=$dispatchers settingsStore=$settingsStore apiClient=$apiClient"

        ComposeBridge.attach(this, binding.composeContainer)
    }
}
