package com.thanhng224.androidcorebase.core.network.auth

import com.thanhng224.androidcorebase.core.foundation.SecureStore
import com.thanhng224.androidcorebase.core.foundation.SecureStoreKey
import com.thanhng224.androidcorebase.core.foundation.SecureStoreKeys
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class AuthSession
    public constructor(
        secureStore: SecureStore,
    ) {
        private val accessTokenSlot = TokenSlot(secureStore, SecureStoreKeys.AUTH_TOKEN)
        private val refreshTokenSlot = TokenSlot(secureStore, SecureStoreKeys.REFRESH_TOKEN)

        /** Cached snapshot of the access token; never touches storage. Null until [getAccessToken] first loads it. */
        public fun peekAccessToken(): String? = accessTokenSlot.peek()

        public suspend fun getAccessToken(): String? = accessTokenSlot.get()

        public suspend fun getRefreshToken(): String? = refreshTokenSlot.get()

        public suspend fun setTokens(
            accessToken: String,
            refreshToken: String? = null,
        ) {
            accessTokenSlot.set(accessToken)
            refreshToken?.let { refreshTokenSlot.set(it) }
        }

        public suspend fun clear() {
            accessTokenSlot.clear()
            refreshTokenSlot.clear()
        }

        /**
         * A single cached secure-store value. The first [get] call per process loads [key] from
         * [secureStore] under [mutex] and caches it; every later call -- concurrent or not --
         * reads the [Volatile] snapshot without touching storage again. [set] persists before
         * updating the snapshot, so a reader never observes a cached value that failed to persist.
         */
        private class TokenSlot(
            private val secureStore: SecureStore,
            private val key: SecureStoreKey,
        ) {
            private sealed interface Cached {
                data object NotLoaded : Cached

                data class Loaded(
                    val value: String?,
                ) : Cached
            }

            @Volatile
            private var cached: Cached = Cached.NotLoaded
            private val mutex = Mutex()

            fun peek(): String? = (cached as? Cached.Loaded)?.value

            suspend fun get(): String? {
                (cached as? Cached.Loaded)?.let { return it.value }
                return mutex.withLock {
                    (cached as? Cached.Loaded)?.let { return@withLock it.value }
                    val loaded = secureStore.getString(key)
                    cached = Cached.Loaded(loaded)
                    loaded
                }
            }

            suspend fun set(value: String) {
                mutex.withLock {
                    secureStore.putString(key, value)
                    cached = Cached.Loaded(value)
                }
            }

            suspend fun clear() {
                mutex.withLock {
                    secureStore.remove(key)
                    cached = Cached.Loaded(null)
                }
            }
        }
    }
