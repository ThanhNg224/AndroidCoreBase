package com.thanhng224.androidcorebase.core.storage.secure

import android.content.Context
import androidx.core.util.AtomicFile
import com.thanhng224.androidcorebase.core.foundation.AppDispatchers
import com.thanhng224.androidcorebase.core.foundation.SecureStore
import com.thanhng224.androidcorebase.core.foundation.SecureStoreKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * [SecureStore] implementation whose single small file lives under [Context.noBackupFilesDir] (so
 * it is never included in Auto Backup) and is updated through [AtomicFile], so a failed write
 * preserves the previously committed value. The whole payload is encrypted -- see
 * [EncryptedFileCodec] for the envelope format and key handling.
 */
internal class EncryptedFileSecureStore
    @Inject
    internal constructor(
        @ApplicationContext context: Context,
        private val dispatchers: AppDispatchers,
    ) : SecureStore {
        private val storeFile = File(context.noBackupFilesDir, FILE_NAME)
        private val atomicFile = AtomicFile(storeFile)
        private val mutex = Mutex()

        override suspend fun getString(key: SecureStoreKey): String? =
            withContext(dispatchers.io) {
                mutex.withLock { readAll()[key.name] }
            }

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) {
            withContext(dispatchers.io) {
                mutex.withLock {
                    val updated = readAll().toMutableMap()
                    updated[key.name] = value
                    writeAll(updated)
                }
            }
        }

        override suspend fun remove(key: SecureStoreKey) {
            withContext(dispatchers.io) {
                mutex.withLock {
                    val updated = readAll().toMutableMap()
                    updated.remove(key.name)
                    writeAll(updated)
                }
            }
        }

        override suspend fun clear() {
            withContext(dispatchers.io) {
                mutex.withLock { writeAll(emptyMap()) }
            }
        }

        private fun readAll(): Map<String, String> {
            val bytes =
                try {
                    atomicFile.readFully()
                } catch (_: FileNotFoundException) {
                    return emptyMap()
                }
            return EncryptedFileCodec.decodeOrNull(bytes) ?: emptyMap()
        }

        private fun writeAll(values: Map<String, String>) {
            val output = atomicFile.startWrite()
            var wroteSuccessfully = false
            try {
                output.write(EncryptedFileCodec.encode(values))
                atomicFile.finishWrite(output)
                wroteSuccessfully = true
            } finally {
                if (!wroteSuccessfully) atomicFile.failWrite(output)
            }
        }

        private companion object {
            const val FILE_NAME = "core_secure_store"
        }
    }
