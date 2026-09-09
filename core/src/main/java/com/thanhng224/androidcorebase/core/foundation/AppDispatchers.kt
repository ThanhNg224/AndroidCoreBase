package com.thanhng224.androidcorebase.core.foundation

import kotlinx.coroutines.CoroutineDispatcher

public interface AppDispatchers {
    public val main: CoroutineDispatcher
    public val io: CoroutineDispatcher
    public val default: CoroutineDispatcher
}
