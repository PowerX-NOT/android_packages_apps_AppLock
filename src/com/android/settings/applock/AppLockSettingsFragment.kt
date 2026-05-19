/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.applock.R
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.dashboard.DashboardFragment

/** Root App Lock settings screen (Settings dashboard entry). */
class AppLockSettingsFragment : DashboardFragment() {

    private lateinit var credentialsLauncher: AppLockCredentialsLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        credentialsLauncher = AppLockCredentialsLauncher(this) {
            refreshCredentialsSummary()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        bindEnableSwitch()
        bindCredentialsPreference()
    }

    override fun onResume() {
        super.onResume()
        bindEnableSwitch()
        refreshLockedAppsSummary()
        refreshRelockSummary()
        refreshCredentialsSummary()
    }

    private fun bindEnableSwitch() {
        val switch = findPreference<SwitchPreferenceCompat>(KEY_ENABLE) ?: return
        val manager = AppLockBinder.getOrNull(requireContext())

        if (manager == null) {
            switch.isEnabled = false
            switch.isChecked = false
            switch.summary = getString(R.string.app_lock_service_unavailable)
            updateDependentPreferences(false)
            return
        }

        switch.isEnabled = true
        switch.summary = getString(R.string.app_lock_enable_summary)
        switch.isChecked = manager.isEnabled
        switch.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled && !AppLockCredentialsHelper.isSetup(requireContext())) {
                Toast.makeText(
                    requireContext(),
                    R.string.app_lock_enable_no_password,
                    Toast.LENGTH_LONG,
                ).show()
            }
            manager.setEnabled(enabled)
            updateDependentPreferences(enabled)
            true
        }
        updateDependentPreferences(manager.isEnabled)
        refreshLockedAppsSummary()
        refreshRelockSummary()
        refreshCredentialsSummary()
    }

    private fun bindCredentialsPreference() {
        findPreference<Preference>(KEY_CREDENTIALS)?.setOnPreferenceClickListener {
            credentialsLauncher.launch()
            true
        }
    }

    private fun updateDependentPreferences(appLockEnabled: Boolean) {
        findPreference<Preference>(KEY_LOCKED_APPS)?.isEnabled = appLockEnabled
        if (!appLockEnabled) {
            findPreference<Preference>(KEY_LOCKED_APPS)?.summary =
                getString(R.string.app_lock_enable_first)
        }
        findPreference<Preference>(KEY_CREDENTIALS)?.isEnabled = appLockEnabled
        if (!appLockEnabled) {
            findPreference<Preference>(KEY_CREDENTIALS)?.summary =
                getString(R.string.app_lock_credentials_summary)
        }
        val relock = findPreference<Preference>(KEY_RELOCK)
        relock?.isEnabled = appLockEnabled
        if (!appLockEnabled) {
            relock?.summary = getString(R.string.app_lock_relock_summary)
        }
    }

    private fun refreshRelockSummary() {
        val relock = findPreference<Preference>(KEY_RELOCK) ?: return
        if (!relock.isEnabled) return
        val behavior = AppLockSettingsSecure.getLockBehavior(requireContext())
        relock.summary = getString(AppLockRelockFragment.summaryForBehavior(behavior))
    }

    private fun refreshCredentialsSummary() {
        val credentials = findPreference<Preference>(KEY_CREDENTIALS) ?: return
        if (!credentials.isEnabled) return
        val context = requireContext()
        if (!AppLockCredentialsHelper.isSetup(context)) {
            credentials.summary = getString(R.string.app_lock_credentials_not_set)
            return
        }
        val typeName = AppLockCredentialsHelper.securityTypeName(context)
            ?: getString(R.string.app_lock_credentials_summary)
        credentials.summary = if (AppLockCredentialsHelper.isBiometricEnabled(context)) {
            getString(R.string.app_lock_credentials_set_with_biometric, typeName)
        } else {
            getString(R.string.app_lock_credentials_set, typeName)
        }
    }

    private fun refreshLockedAppsSummary() {
        val lockedApps = findPreference<Preference>(KEY_LOCKED_APPS) ?: return
        if (!lockedApps.isEnabled) return
        val manager = AppLockBinder.getOrNull(requireContext()) ?: return
        val count = manager.lockedPackages.size
        lockedApps.summary = if (count == 0) {
            getString(R.string.app_lock_locked_apps_summary)
        } else {
            getString(R.string.app_lock_locked_apps_summary_count, count)
        }
    }

    override fun getMetricsCategory() = MetricsProto.MetricsEvent.EVOLVER

    override fun getPreferenceScreenResId() = R.xml.app_lock_settings

    override fun getLogTag() = "AppLockSettings"

    companion object {
        private const val KEY_ENABLE = "app_lock_enable"
        private const val KEY_LOCKED_APPS = "app_lock_locked_apps"
        private const val KEY_CREDENTIALS = "app_lock_credentials"
        private const val KEY_RELOCK = "app_lock_relock"
    }
}
