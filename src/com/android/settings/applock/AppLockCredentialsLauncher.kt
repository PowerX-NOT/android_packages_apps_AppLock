/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/** Registers the in-package privacy-password setup flow. */
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
        launcher.launch(AppLockCredentialsHelper.createSetupIntent(fragment.requireContext()))
    }
}
