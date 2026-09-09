package com.thanhng224.androidcorebase.core.foundation

import com.thanhng224.androidcorebase.core.architecture.DefaultAppDispatchers
import kotlinx.coroutines.CoroutineDispatcher

public interface AppDispatchers {
    public val main: CoroutineDispatcher
    public val io: CoroutineDispatcher
    public val default: CoroutineDispatcher

    public companion object {
        public fun default(): AppDispatchers = DefaultAppDispatchers()
    }
}
