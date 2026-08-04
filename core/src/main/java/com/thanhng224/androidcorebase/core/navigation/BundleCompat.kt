package com.thanhng224.androidcorebase.core.navigation

import android.os.Bundle

internal object BundleCompat {
    fun copyOf(bundle: Bundle): Bundle = Bundle(bundle)
}
