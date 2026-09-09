package com.thanhng224.androidcorebase.core.network.auth

public interface AuthTokenProvider {
    /** Cached snapshot. Null when nothing is cached yet; never touches storage. */
    public fun peekToken(): String?

    /** Loads from storage when the snapshot is cold. */
    public suspend fun getToken(): String?
}

public class NoOpAuthTokenProvider : AuthTokenProvider {
    override fun peekToken(): String? = null

    override suspend fun getToken(): String? = null
}
