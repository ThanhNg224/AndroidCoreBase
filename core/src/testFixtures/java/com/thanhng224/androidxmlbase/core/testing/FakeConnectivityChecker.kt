package com.thanhng224.androidxmlbase.core.testing

import com.thanhng224.androidxmlbase.core.network.connectivity.ConnectivityChecker

/** [ConnectivityChecker] whose answer the test controls via [connected]. */
class FakeConnectivityChecker(
    var connected: Boolean = true,
) : ConnectivityChecker {
    override fun isConnected(): Boolean = connected
}
