package com.thanhng224.androidxmlbase.core.storage.secure

@JvmInline
value class SecureStoreKey(
    val name: String,
)

interface SecureStore {
    suspend fun getString(key: SecureStoreKey): String?

    suspend fun putString(
        key: SecureStoreKey,
        value: String,
    )

    suspend fun remove(key: SecureStoreKey)

    suspend fun clear()
}

object SecureStoreKeys {
    val AUTH_TOKEN = SecureStoreKey("auth_token")
    val REFRESH_TOKEN = SecureStoreKey("refresh_token")
}
