/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.app.AppLockManager
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import com.android.applock.R
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.dashboard.DashboardFragment
/** Choose when unlocked apps should require authentication again. */
class AppLockRelockFragment : DashboardFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        bindBehaviorOptions()
        bindTimeoutPreference()
    }

    override fun onResume() {
        super.onResume()
        refreshBehaviorSelection()
        refreshTimeoutPreference()
    }

    private fun bindBehaviorOptions() {
        val category = findPreference<PreferenceCategory>(KEY_BEHAVIOR) ?: return
        val current = AppLockSettingsSecure.getLockBehavior(requireContext())
        category.removeAll()
        for (option in BEHAVIOR_OPTIONS) {
            category.addPreference(
                AppLockRelockBehaviorPreference(requireContext()).apply {
                    key = "app_lock_relock_behavior_${option.value}"
                    title = getString(option.titleRes)
                    summary = getString(option.summaryRes)
                    isChecked = current == option.value
                    setOnPreferenceChangeListener { _, newValue ->
                        val checked = newValue as Boolean
                        if (!checked) return@setOnPreferenceChangeListener false
                        AppLockSettingsSecure.setLockBehavior(requireContext(), option.value)
                        updateBehaviorChecks(category, option.value)
                        updateTimeoutVisibility(option.value)
                        true
                    }
                },
            )
        }
        updateTimeoutVisibility(current)
    }

    private fun updateBehaviorChecks(category: PreferenceCategory, selected: Int) {
        for (option in BEHAVIOR_OPTIONS) {
            val pref = category.findPreference<AppLockRelockBehaviorPreference>(
                "app_lock_relock_behavior_${option.value}",
            ) ?: continue
            pref.isChecked = option.value == selected
        }
    }

    private fun bindTimeoutPreference() {
        val timeout = findPreference<ListPreference>(KEY_TIMEOUT) ?: return
        timeout.setOnPreferenceChangeListener { _, newValue ->
            val chosen = (newValue as String).toIntOrNull() ?: return@setOnPreferenceChangeListener false
            AppLockSettingsSecure.setLockTimeoutSeconds(requireContext(), chosen)
            timeout.summary = timeout.entry
            true
        }
        refreshTimeoutPreference()
    }

    private fun refreshBehaviorSelection() {
        val category = findPreference<PreferenceCategory>(KEY_BEHAVIOR) ?: return
        val current = AppLockSettingsSecure.getLockBehavior(requireContext())
        updateBehaviorChecks(category, current)
        updateTimeoutVisibility(current)
    }

    private fun refreshTimeoutPreference() {
        val timeout = findPreference<ListPreference>(KEY_TIMEOUT) ?: return
        val behavior = AppLockSettingsSecure.getLockBehavior(requireContext())
        val seconds = AppLockSettingsSecure.getLockTimeoutSeconds(requireContext())
        timeout.value = seconds.toString()
        timeout.summary = timeout.entry ?: getString(R.string.app_lock_relock_timeout_summary)
        timeout.isVisible = behavior == AppLockManager.LOCK_BEHAVIOR_TIMEOUT
    }

    private fun updateTimeoutVisibility(behavior: Int) {
        findPreference<ListPreference>(KEY_TIMEOUT)?.isVisible =
            behavior == AppLockManager.LOCK_BEHAVIOR_TIMEOUT
    }

    override fun getMetricsCategory() = MetricsProto.MetricsEvent.SECURITY

    override fun getPreferenceScreenResId() = R.xml.app_lock_relock

    override fun getLogTag() = "AppLockRelock"

    private data class BehaviorOption(
        val value: Int,
        val titleRes: Int,
        val summaryRes: Int,
    )

    companion object {
        private const val KEY_BEHAVIOR = "app_lock_relock_behavior"
        private const val KEY_TIMEOUT = "app_lock_relock_timeout"

        private val BEHAVIOR_OPTIONS = listOf(
            BehaviorOption(
                AppLockManager.LOCK_BEHAVIOR_ON_LEAVE,
                R.string.app_lock_relock_on_leave_title,
                R.string.app_lock_relock_on_leave_summary,
            ),
            BehaviorOption(
                AppLockManager.LOCK_BEHAVIOR_TIMEOUT,
                R.string.app_lock_relock_timeout_behavior_title,
                R.string.app_lock_relock_timeout_behavior_summary,
            ),
            BehaviorOption(
                AppLockManager.LOCK_BEHAVIOR_ON_SCREEN_OFF,
                R.string.app_lock_relock_screen_off_title,
                R.string.app_lock_relock_screen_off_summary,
            ),
            BehaviorOption(
                AppLockManager.LOCK_BEHAVIOR_ON_KILL,
                R.string.app_lock_relock_on_kill_title,
                R.string.app_lock_relock_on_kill_summary,
            ),
        )

        fun summaryForBehavior(behavior: Int): Int = when (behavior) {
            AppLockManager.LOCK_BEHAVIOR_TIMEOUT ->
                R.string.app_lock_relock_summary_timeout
            AppLockManager.LOCK_BEHAVIOR_ON_SCREEN_OFF ->
                R.string.app_lock_relock_summary_screen_off
            AppLockManager.LOCK_BEHAVIOR_ON_KILL ->
                R.string.app_lock_relock_summary_on_kill
            else -> R.string.app_lock_relock_summary_on_leave
        }
    }
}
