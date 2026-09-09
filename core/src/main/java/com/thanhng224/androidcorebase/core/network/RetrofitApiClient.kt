package com.thanhng224.androidcorebase.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

internal class RetrofitApiClient internal constructor() : ApiClient {
    override suspend fun <T> execute(call: suspend () -> Response<T>): ApiResult<T> =
        try {
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let { ApiResult.Success(it) }
                    ?: ApiResult.Failure(ApiFailure.EmptyBody)
            } else {
                ApiResult.Failure(ApiFailure.Http(response.code(), response.message()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ApiResult.Failure(ApiFailure.Network(e))
        } catch (e: Exception) {
            ApiResult.Failure(ApiFailure.Serialization(e))
        }
}
