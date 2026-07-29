package com.thanhng224.androidcorebase.core.network.auth

public interface AuthTokenProvider {
    public suspend fun getToken(): String?
}

public class NoOpAuthTokenProvider : AuthTokenProvider {
    override suspend fun getToken(): String? = null
}
