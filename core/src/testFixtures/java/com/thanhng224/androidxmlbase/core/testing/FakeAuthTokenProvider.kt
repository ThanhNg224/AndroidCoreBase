package com.thanhng224.androidxmlbase.core.testing

import com.thanhng224.androidxmlbase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidxmlbase.core.network.auth.AuthTokenRefresher

/** [AuthTokenProvider] returning a token the test controls. */
class FakeAuthTokenProvider(
    var token: String? = null,
) : AuthTokenProvider {
    override suspend fun getToken(): String? = token
}

/** [AuthTokenRefresher] that records how often it ran and returns [newToken]. */
class FakeAuthTokenRefresher(
    private val newToken: String? = null,
) : AuthTokenRefresher {
    var callCount: Int = 0
        private set

    override suspend fun refresh(refreshToken: String?): String? {
        callCount++
        return newToken
    }
}
