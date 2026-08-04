package com.thanhng224.androidcorebase.core.network.auth

public interface AuthTokenRefresher {
    public suspend fun refresh(refreshToken: String?): String?
}
