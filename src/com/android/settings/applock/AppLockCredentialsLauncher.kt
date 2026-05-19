/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.applock.R

/** Registers the privacy-password setup flow and refreshes summary on return. */
class AppLockCredentialsLauncher(
    private val fragment: Fragment,
    private val onReturned: () -> Unit,
) {
    private val launcher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        onReturned()
    }

    fun launch() {
        try {
            launcher.launch(AppLockCredentialsHelper.createSetupIntent())
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                fragment.requireContext(),
                R.string.app_lock_credentials_unavailable,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
