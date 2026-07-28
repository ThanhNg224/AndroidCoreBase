package com.thanhng224.androidxmlbase.core.navigation

import android.app.Activity
import android.os.Bundle
import kotlin.reflect.KClass

data class ActivityDestination(
    val activityClass: KClass<out Activity>,
    val extras: Bundle? = null,
    val options: NavigationOptions = NavigationOptions(),
)
