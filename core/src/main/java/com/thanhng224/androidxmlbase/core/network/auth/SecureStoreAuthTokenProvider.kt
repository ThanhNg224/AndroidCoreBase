package com.thanhng224.androidxmlbase.core.network.auth

import com.thanhng224.androidxmlbase.core.storage.secure.SecureStore
import com.thanhng224.androidxmlbase.core.storage.secure.SecureStoreKeys
import javax.inject.Inject

internal class SecureStoreAuthTokenProvider
    @Inject
    internal constructor(
        private val secureStore: SecureStore,
    ) : AuthTokenProvider {
        override suspend fun getToken(): String? = secureStore.getString(SecureStoreKeys.AUTH_TOKEN)
    }
