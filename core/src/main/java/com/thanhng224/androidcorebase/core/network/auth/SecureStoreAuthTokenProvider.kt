package com.thanhng224.androidcorebase.core.network.auth

import javax.inject.Inject

internal class SecureStoreAuthTokenProvider
    @Inject
    internal constructor(
        private val authSession: AuthSession,
    ) : AuthTokenProvider {
        override fun peekToken(): String? = authSession.peekAccessToken()

        override suspend fun getToken(): String? = authSession.getAccessToken()
    }
