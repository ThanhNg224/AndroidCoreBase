package com.thanhng224.androidxmlbase.core.storage.secure

@JvmInline
public value class SecureStoreKey(
    public val name: String,
)

public interface SecureStore {
    public suspend fun getString(key: SecureStoreKey): String?

    public suspend fun putString(
        key: SecureStoreKey,
        value: String,
    )

    public suspend fun remove(key: SecureStoreKey)

    public suspend fun clear()
}

public object SecureStoreKeys {
    public val AUTH_TOKEN: SecureStoreKey = SecureStoreKey("auth_token")
    public val REFRESH_TOKEN: SecureStoreKey = SecureStoreKey("refresh_token")
}
