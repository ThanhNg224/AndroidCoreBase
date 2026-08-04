package com.thanhng224.androidcorebase.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

internal class RetrofitApiClient
    @Inject
    internal constructor() : ApiClient {
        override suspend fun <T> execute(call: suspend () -> Response<T>): ApiResult<T> =
            try {
                val response = call()
                if (response.isSuccessful) {
                    response.body()?.let { ApiResult.Success(it) } ?: ApiResult.EmptyBody
                } else {
                    ApiResult.HttpError(response.code(), response.message())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                ApiResult.NetworkError(e)
            } catch (e: Exception) {
                ApiResult.ParseError(e)
            }
    }
