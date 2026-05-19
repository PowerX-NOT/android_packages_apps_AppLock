/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.Context
import android.content.Intent
import android.os.Binder
import com.android.applock.R
import com.android.applock.auth.SetupCredentialsActivity
import com.android.applock.auth.security.AppLockSecurityManager

/** Privacy password state for App Lock ({@code applock_*} Secure keys). */
object AppLockCredentialsHelper {

    fun isSetup(context: Context): Boolean = withClearCallingIdentity {
        AppLockSecurityManager(context).isSetup()
    }

    fun isBiometricEnabled(context: Context): Boolean = withClearCallingIdentity {
        AppLockSecurityManager(context).isBiometricEnabled()
    }

    fun securityTypeName(context: Context): String? = withClearCallingIdentity {
        when (AppLockSecurityManager(context).getSecurityType()) {
            com.android.applock.auth.security.SecurityType.PIN ->
                context.getString(R.string.app_lock_credentials_type_pin)
            com.android.applock.auth.security.SecurityType.PASSWORD ->
                context.getString(R.string.app_lock_credentials_type_password)
            com.android.applock.auth.security.SecurityType.PATTERN ->
                context.getString(R.string.app_lock_credentials_type_pattern)
            else -> null
        }
    }

    fun createSetupIntent(context: Context): Intent =
        Intent(context, SetupCredentialsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private inline fun <T> withClearCallingIdentity(block: () -> T): T {
        val token = Binder.clearCallingIdentity()
        return try {
            block()
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }
}
