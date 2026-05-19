/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.UserHandle
import android.provider.Settings
import com.android.applock.R

/** Reads privacy-password state stored by AppLocker ({@code sandbox_* Secure keys}). */
object AppLockCredentialsHelper {

    private const val PACKAGE_APPLOCKER = "com.android.applocker"
    private const val CLASS_SETUP = "com.android.applocker.SetupCredentialsActivity"
    const val ACTION_SETUP_CREDENTIALS = "com.android.applocker.action.SETUP_CREDENTIALS"

    private const val KEY_SECURITY_TYPE = "sandbox_security_type"
    private const val KEY_CREDENTIAL_HASH = "sandbox_credential_hash"
    private const val KEY_BIOMETRIC_ENABLED = "sandbox_biometric_enabled"

    fun isSetup(context: Context): Boolean = withClearCallingIdentity {
        val resolver = context.contentResolver
        val type = Settings.Secure.getString(resolver, KEY_SECURITY_TYPE) ?: return@withClearCallingIdentity false
        val hash = Settings.Secure.getString(resolver, KEY_CREDENTIAL_HASH)
        type != "NONE" && !hash.isNullOrEmpty()
    }

    fun isBiometricEnabled(context: Context): Boolean = withClearCallingIdentity {
        Settings.Secure.getInt(
            context.contentResolver,
            KEY_BIOMETRIC_ENABLED,
            0,
        ) == 1
    }

    fun securityTypeName(context: Context): String? = withClearCallingIdentity {
        when (Settings.Secure.getString(context.contentResolver, KEY_SECURITY_TYPE)) {
            "PIN" -> context.getString(R.string.app_lock_credentials_type_pin)
            "PASSWORD" -> context.getString(R.string.app_lock_credentials_type_password)
            "PATTERN" -> context.getString(R.string.app_lock_credentials_type_pattern)
            else -> null
        }
    }

    fun isAppLockerInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PACKAGE_APPLOCKER, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun canLaunchSetup(context: Context): Boolean {
        if (!isAppLockerInstalled(context)) return false
        val intent = createSetupIntent()
        return context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) != null
    }

    fun createSetupIntent(): Intent =
        Intent(ACTION_SETUP_CREDENTIALS)
            .setClassName(PACKAGE_APPLOCKER, CLASS_SETUP)
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
