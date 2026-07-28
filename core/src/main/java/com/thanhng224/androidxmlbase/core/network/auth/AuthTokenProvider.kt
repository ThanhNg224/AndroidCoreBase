package com.thanhng224.androidxmlbase.core.network.auth

interface AuthTokenProvider {
    suspend fun getToken(): String?
}

class NoOpAuthTokenProvider : AuthTokenProvider {
    override suspend fun getToken(): String? = null
}
