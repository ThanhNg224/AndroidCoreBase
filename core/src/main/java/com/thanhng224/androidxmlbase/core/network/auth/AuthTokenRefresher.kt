package com.thanhng224.androidxmlbase.core.network.auth

interface AuthTokenRefresher {
    suspend fun refresh(refreshToken: String?): String?
}
