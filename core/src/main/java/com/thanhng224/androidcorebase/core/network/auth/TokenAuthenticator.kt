package com.thanhng224.androidcorebase.core.network.auth

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.Optional
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class TokenAuthenticator
    @Inject
    internal constructor(
        private val authSession: AuthSession,
        private val tokenRefresher: Optional<Provider<AuthTokenRefresher>>,
    ) : Authenticator {
        private val refreshMutex = Mutex()

        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (responseCount(response) > MAX_RETRIES) return null

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
            val refresher = tokenRefresher.orElse(null)?.get() ?: return null
            val newToken = refresher.refresh(authSession.getRefreshToken()) ?: return null
            authSession.setTokens(newToken)
            return newToken
        }

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

        private companion object {
            const val MAX_RETRIES = 2
        }
    }
