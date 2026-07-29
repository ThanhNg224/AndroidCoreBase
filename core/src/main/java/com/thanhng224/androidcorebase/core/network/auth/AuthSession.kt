package com.thanhng224.androidcorebase.core.network.auth

import com.thanhng224.androidcorebase.core.storage.secure.SecureStore
import com.thanhng224.androidcorebase.core.storage.secure.SecureStoreKeys
import javax.inject.Inject

public class AuthSession
    @Inject
    internal constructor(
        private val secureStore: SecureStore,
    ) {
        public suspend fun getAccessToken(): String? = secureStore.getString(SecureStoreKeys.AUTH_TOKEN)

        public suspend fun getRefreshToken(): String? = secureStore.getString(SecureStoreKeys.REFRESH_TOKEN)

        public suspend fun setTokens(
            accessToken: String,
            refreshToken: String? = null,
        ) {
            secureStore.putString(SecureStoreKeys.AUTH_TOKEN, accessToken)
            refreshToken?.let { secureStore.putString(SecureStoreKeys.REFRESH_TOKEN, it) }
        }

        public suspend fun clear() {
            secureStore.remove(SecureStoreKeys.AUTH_TOKEN)
            secureStore.remove(SecureStoreKeys.REFRESH_TOKEN)
        }
    }
