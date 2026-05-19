/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.app.AppLockManager
import android.content.Context
import android.os.Binder
import android.os.UserHandle
import android.provider.Settings

/** Reads and writes App Lock Secure settings on the system user. */
object AppLockSettingsSecure {

    fun getLockBehavior(context: Context): Int = withClearCallingIdentity {
        Settings.Secure.getIntForUser(
            context.contentResolver,
            AppLockManager.SETTING_LOCK_BEHAVIOR,
            AppLockManager.LOCK_BEHAVIOR_ON_LEAVE,
            UserHandle.USER_SYSTEM,
        )
    }

    fun setLockBehavior(context: Context, behavior: Int) {
        withClearCallingIdentity {
            Settings.Secure.putIntForUser(
                context.contentResolver,
                AppLockManager.SETTING_LOCK_BEHAVIOR,
                behavior,
                UserHandle.USER_SYSTEM,
            )
        }
    }

    fun getLockTimeoutSeconds(context: Context): Int = withClearCallingIdentity {
        Settings.Secure.getIntForUser(
            context.contentResolver,
            AppLockManager.SETTING_LOCK_TIMEOUT,
            AppLockManager.DEFAULT_LOCK_TIMEOUT,
            UserHandle.USER_SYSTEM,
        )
    }

    fun setLockTimeoutSeconds(context: Context, timeoutSeconds: Int) {
        withClearCallingIdentity {
            Settings.Secure.putIntForUser(
                context.contentResolver,
                AppLockManager.SETTING_LOCK_TIMEOUT,
                timeoutSeconds,
                UserHandle.USER_SYSTEM,
            )
        }
    }

    private inline fun <T> withClearCallingIdentity(block: () -> T): T {
        val token = Binder.clearCallingIdentity()
        return try {
            block()
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }
}
