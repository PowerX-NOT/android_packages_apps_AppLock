/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.app.AppLockManager
import android.content.Context

/** Access to the system {@link AppLockManager} service. */
object AppLockBinder {

    @Volatile
    private var cached: AppLockManager? = null

    fun getOrNull(context: Context): AppLockManager? {
        cached?.let { return it }
        val mgr = context.getSystemService(AppLockManager::class.java) ?: return null
        cached = mgr
        return mgr
    }

    fun clearCache() {
        cached = null
    }
}
