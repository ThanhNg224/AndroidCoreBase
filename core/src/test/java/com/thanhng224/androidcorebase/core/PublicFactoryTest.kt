package com.thanhng224.androidcorebase.core

import com.thanhng224.androidcorebase.core.foundation.AppDispatchers
import com.thanhng224.androidcorebase.core.network.ApiClient
import com.thanhng224.androidcorebase.core.network.NetworkClientFactory
import com.thanhng224.androidcorebase.core.network.auth.AuthSession
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidcorebase.core.network.transfer.FileTransferClient
import com.thanhng224.androidcorebase.core.storage.secure.DbPassphraseProvider
import com.thanhng224.androidcorebase.core.testing.FakeSecureStore
import com.thanhng224.androidcorebase.core.testing.FakeSettingsStore
import com.thanhng224.androidcorebase.core.ui.theme.ThemeManager
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Proves every published contract is constructible from its public factory/constructor alone,
 * with no dependency-injection framework involved -- the whole point of removing Hilt from :core.
 */
class PublicFactoryTest {
    @Test
    fun `public factories create contracts without dependency injection`() {
        val dispatchers: AppDispatchers = AppDispatchers.default()
        val apiClient: ApiClient = NetworkClientFactory.createApiClient()
        val transfer: FileTransferClient =
            NetworkClientFactory.createFileTransferClient(OkHttpClient(), dispatchers)

        assertNotNull(apiClient)
        assertNotNull(transfer)
    }

    @Test
    fun `theme manager is constructible from a plain SettingsStore`() {
        val themeManager: ThemeManager = ThemeManager.create(FakeSettingsStore())

        assertNotNull(themeManager)
    }

    @Test
    fun `auth session and token provider are constructible from a plain SecureStore`() {
        val authSession = AuthSession(FakeSecureStore())
        val tokenProvider: AuthTokenProvider = NetworkClientFactory.createAuthTokenProvider(authSession)
        val authenticator: Authenticator = NetworkClientFactory.createAuthenticator(authSession)

        assertNotNull(tokenProvider)
        assertNotNull(authenticator)
    }

    @Test
    fun `db passphrase provider is constructible from a plain SecureStore`() {
        val provider = DbPassphraseProvider(FakeSecureStore())

        assertNotNull(provider)
    }
}
