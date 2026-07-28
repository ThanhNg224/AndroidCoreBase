package com.thanhng224.androidxmlbase.core.network.auth

internal interface AuthTokenProvider {
    suspend fun getToken(): String?
}

internal class NoOpAuthTokenProvider : AuthTokenProvider {
    override suspend fun getToken(): String? = null
}
