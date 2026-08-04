package com.thanhng224.androidcorebase.core.network

public data class ApiConfig(
    val baseUrl: String,
    val enableLogging: Boolean = false,
    val connectTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    val readTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    val writeTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
) {
    public companion object {
        public const val DEFAULT_TIMEOUT_SECONDS: Long = 30L
    }
}
