package com.thanhng224.androidxmlbase.core.network

interface ApiClient {
    suspend fun <T> execute(call: suspend () -> retrofit2.Response<T>): ApiResult<T>
}
