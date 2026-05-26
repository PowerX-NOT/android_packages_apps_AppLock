/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.app.HiddenAppsManager
import android.content.Context

/** Access to the system {@link HiddenAppsManager} service. */
object HiddenAppsBinder {

    @Volatile
    private var cached: HiddenAppsManager? = null

    fun getOrNull(context: Context): HiddenAppsManager? {
        cached?.let { return it }
        val mgr = context.getSystemService(HiddenAppsManager::class.java) ?: return null
        cached = mgr
        return mgr
    }

    fun clearCache() {
        cached = null
    }
}
