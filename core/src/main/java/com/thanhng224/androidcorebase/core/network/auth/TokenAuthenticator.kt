package com.thanhng224.androidcorebase.core.network.auth

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

internal class TokenAuthenticator
    internal constructor(
        private val authSession: AuthSession,
        private val tokenRefresher: (() -> AuthTokenRefresher)? = null,
    ) : Authenticator {
        private val refreshMutex = Mutex()

        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            // A non-null priorResponse means this failure is itself the result of a retry that
            // this authenticator already attempted, so at most one retry ever follows the
            // original 401.
            if (response.priorResponse != null) return null

            val failedAuthHeader = response.request.header("Authorization")

            val nextToken =
                runBlocking {
                    refreshMutex.withLock {
                        val cached = authSession.getAccessToken()
                        if (cached != null && cached != failedAuthHeader) {
                            cached
                        } else {
                            refreshAndPersist()
                        }
                    }
                }

            if (nextToken.isNullOrBlank()) return null

            return response.request
                .newBuilder()
                .header("Authorization", nextToken)
                .build()
        }

        private suspend fun refreshAndPersist(): String? {
            val refresher = tokenRefresher?.invoke() ?: return null
            val newToken = refresher.refresh(authSession.getRefreshToken()) ?: return null
            authSession.setTokens(newToken)
            return newToken
        }
    }
