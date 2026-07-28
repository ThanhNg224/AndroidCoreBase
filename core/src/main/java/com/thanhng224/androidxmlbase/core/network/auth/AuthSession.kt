package com.thanhng224.androidxmlbase.core.network.auth

import com.thanhng224.androidxmlbase.core.storage.secure.SecureStore
import com.thanhng224.androidxmlbase.core.storage.secure.SecureStoreKeys
import javax.inject.Inject

class AuthSession
    @Inject
    internal constructor(
        private val secureStore: SecureStore,
    ) {
        suspend fun getAccessToken(): String? = secureStore.getString(SecureStoreKeys.AUTH_TOKEN)

        suspend fun getRefreshToken(): String? = secureStore.getString(SecureStoreKeys.REFRESH_TOKEN)

        suspend fun setTokens(
            accessToken: String,
            refreshToken: String? = null,
        ) {
            secureStore.putString(SecureStoreKeys.AUTH_TOKEN, accessToken)
            refreshToken?.let { secureStore.putString(SecureStoreKeys.REFRESH_TOKEN, it) }
        }

        suspend fun clear() {
            secureStore.remove(SecureStoreKeys.AUTH_TOKEN)
            secureStore.remove(SecureStoreKeys.REFRESH_TOKEN)
        }
    }
