package com.thanhng224.androidxmlbase.core.network.auth

public interface AuthTokenRefresher {
    public suspend fun refresh(refreshToken: String?): String?
}
