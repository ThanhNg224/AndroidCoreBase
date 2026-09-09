package com.example.androidcorebase

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Published library manifests are passive (see `core` and `core:ui-compose`); the starter app is
 * the one that must declare the permissions its own network usage needs.
 */
@RunWith(AndroidJUnit4::class)
class ManifestOwnershipTest {
    @Test
    fun installedApp_declaresInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )

        val declaredPermissions = packageInfo.requestedPermissions.orEmpty().toList()

        assertTrue(
            "Expected android.permission.INTERNET among $declaredPermissions",
            declaredPermissions.contains(android.Manifest.permission.INTERNET),
        )
    }
}
