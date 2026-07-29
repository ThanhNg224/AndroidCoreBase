package com.thanhng224.androidxmlbase.core.architecture

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

public interface AppDispatchers {
    public val main: CoroutineDispatcher
    public val io: CoroutineDispatcher
    public val default: CoroutineDispatcher
}

internal class DefaultAppDispatchers
    @Inject
    internal constructor() : AppDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
    }
