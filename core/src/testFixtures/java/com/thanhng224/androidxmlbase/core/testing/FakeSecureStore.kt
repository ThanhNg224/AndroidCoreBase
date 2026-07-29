package com.thanhng224.androidxmlbase.core.testing

import com.thanhng224.androidxmlbase.core.storage.secure.SecureStore
import com.thanhng224.androidxmlbase.core.storage.secure.SecureStoreKey

/** In-memory [SecureStore] with no Keystore or disk involvement. */
public class FakeSecureStore(
    initial: Map<String, String> = emptyMap(),
) : SecureStore {
    /** Backing values keyed by [SecureStoreKey.name]; seed or assert on it directly. */
    public val stored: MutableMap<String, String> = initial.toMutableMap()

    override suspend fun getString(key: SecureStoreKey): String? = stored[key.name]

    override suspend fun putString(
        key: SecureStoreKey,
        value: String,
    ) {
        stored[key.name] = value
    }

    override suspend fun remove(key: SecureStoreKey) {
        stored.remove(key.name)
    }

    override suspend fun clear() {
        stored.clear()
    }
}
