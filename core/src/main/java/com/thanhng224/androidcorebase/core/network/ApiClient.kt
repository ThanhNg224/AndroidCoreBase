package com.thanhng224.androidcorebase.core.network

public interface ApiClient {
    public suspend fun <T> execute(call: suspend () -> retrofit2.Response<T>): ApiResult<T>
}
