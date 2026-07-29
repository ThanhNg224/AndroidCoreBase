package com.thanhng224.androidxmlbase.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

public interface ConnectivityChecker {
    public fun isConnected(): Boolean
}

internal class AndroidConnectivityChecker(
    private val context: Context,
) : ConnectivityChecker {
    override fun isConnected(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
