/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.app.AppLockManager
import android.content.Context
import android.os.RemoteException
import android.util.Log

/** Reads and writes relock policy via {@link AppLockManager} (system service). */
object AppLockSettingsSecure {

    private const val TAG = "AppLockSettingsSecure"

    fun getLockBehavior(context: Context): Int {
        val manager = AppLockBinder.getOrNull(context) ?: return AppLockManager.LOCK_BEHAVIOR_ON_LEAVE
        return manager.lockBehavior
    }

    fun setLockBehavior(context: Context, behavior: Int) {
        AppLockBinder.getOrNull(context)?.setLockBehavior(behavior)
    }

    fun getLockTimeoutSeconds(context: Context): Int {
        val manager = AppLockBinder.getOrNull(context)
            ?: return AppLockManager.DEFAULT_LOCK_TIMEOUT
        return manager.lockTimeout
    }

    fun setLockTimeoutSeconds(context: Context, timeoutSeconds: Int) {
        AppLockBinder.getOrNull(context)?.setLockTimeout(timeoutSeconds)
    }

    fun isHideNotificationContentEnabled(context: Context): Boolean {
        val manager = AppLockBinder.getOrNull(context) ?: return true
        return try {
            manager.isHideNotificationContentEnabled
        } catch (e: RemoteException) {
            Log.w(TAG, "isHideNotificationContentEnabled failed", e)
            true
        }
    }

    fun setHideNotificationContent(context: Context, hide: Boolean) {
        val manager = AppLockBinder.getOrNull(context) ?: return
        try {
            manager.setHideNotificationContent(hide)
        } catch (e: RemoteException) {
            Log.w(TAG, "setHideNotificationContent failed", e)
        }
    }
}
