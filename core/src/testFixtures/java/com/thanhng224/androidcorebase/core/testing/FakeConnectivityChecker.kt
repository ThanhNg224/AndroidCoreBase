package com.thanhng224.androidcorebase.core.testing

import com.thanhng224.androidcorebase.core.network.connectivity.ConnectivityChecker

/** [ConnectivityChecker] whose answer the test controls via [connected]. */
public class FakeConnectivityChecker(
    public var connected: Boolean = true,
) : ConnectivityChecker {
    override fun isConnected(): Boolean = connected
}
