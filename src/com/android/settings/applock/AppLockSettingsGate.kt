/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.Context
import android.content.Intent
import android.os.Process
import com.android.applock.R
import com.android.applock.auth.AuthenticateActivity

/**
 * Privacy verification before opening App Lock settings.
 *
 * When the global toggle is on, the system service locks [PACKAGE_NAME] like other apps.
 * When the toggle is off but a privacy password exists, settings still require one
 * in-app authentication per open.
 */
object AppLockSettingsGate {

    const val PACKAGE_NAME = "com.android.applock"
    const val EXTRA_SETTINGS_ENTRY = "settings_entry"
    const val EXTRA_HIDDEN_DRAWER = "hidden_drawer"

    /** In-app auth when toggle is off and a privacy password is configured. */
    fun requiresInAppAuth(context: Context): Boolean {
        if (!AppLockCredentialsHelper.isSetup(context)) return false
        val manager = AppLockBinder.getOrNull(context) ?: return true
        return !manager.isEnabled
    }

    fun createAuthIntent(context: Context): Intent =
        Intent(context, AuthenticateActivity::class.java).apply {
            action = AuthenticateActivity.ACTION_AUTHENTICATE
            putExtra(AuthenticateActivity.EXTRA_LOCKED_PACKAGE, PACKAGE_NAME)
            putExtra(
                AuthenticateActivity.EXTRA_APP_LABEL,
                context.getString(R.string.app_lock_title),
            )
            putExtra(AuthenticateActivity.EXTRA_USER_ID, Process.myUserHandle().identifier)
            putExtra(EXTRA_SETTINGS_ENTRY, true)
        }
}
