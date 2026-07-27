package com.thanhng224.androidxmlbase.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Automatically intercepts 401 HTTP responses to attempt a token refresh cycle.
 */
@Singleton
internal class TokenAuthenticator
    @Inject
    internal constructor(
        private val tokenProvider: Provider<AuthTokenProvider>,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            val authorizationHeader = response.request.header("Authorization")
            val token = runBlocking { tokenProvider.get().getToken() }

            if (token != null && token == authorizationHeader) {
                // Avoid infinite retries if request failed with the latest token
                return null
            }

            // Fetch fresh token
            val freshToken = runBlocking { tokenProvider.get().getToken() }

            if (freshToken.isNullOrBlank()) {
                return null
            }

            return response.request
                .newBuilder()
                .header("Authorization", freshToken)
                .build()
        }
    }
