/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceCategory
import com.android.applock.R
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.dashboard.DashboardFragment
import com.android.settingslib.widget.SelectorWithWidgetPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pick which apps require App Lock authentication before opening. */
class AppLockLockedAppsFragment : DashboardFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        loadAppList()
    }

    override fun onResume() {
        super.onResume()
        loadAppList()
    }

    private fun loadAppList() {
        val manager = AppLockBinder.getOrNull(requireContext()) ?: return
        val category = findPreference<PreferenceCategory>(KEY_LIST) ?: return

        lifecycleScope.launch {
            val pm = requireContext().packageManager
            val locked = manager.lockedPackages.toHashSet()
            val rows = withContext(Dispatchers.Default) {
                buildAppRows(pm, manager.lockablePackages, locked)
            }

            category.removeAll()
            if (rows.isEmpty()) {
                val empty = SelectorWithWidgetPreference(requireContext(), true).apply {
                    key = "app_lock_empty"
                    title = getString(R.string.app_lock_locked_apps_empty)
                    isEnabled = false
                    isSelectable = false
                }
                category.addPreference(empty)
                return@launch
            }

            for (row in rows) {
                category.addPreference(createAppPreference(row, manager))
            }
        }
    }

    private fun createAppPreference(
        row: AppRow,
        manager: android.app.AppLockManager,
    ): AppLockAppPreference {
        return AppLockAppPreference(requireContext()).apply {
            key = "app_lock_pkg_${row.packageName}"
            title = row.label
            icon = row.icon
            isChecked = row.locked
            isEnabled = true
            setOnPreferenceChangeListener { _, newValue ->
                val shouldLock = newValue as Boolean
                if (shouldLock) {
                    manager.addLockedApp(row.packageName)
                } else {
                    manager.removeLockedApp(row.packageName)
                }
                true
            }
        }
    }

    override fun getMetricsCategory() = MetricsProto.MetricsEvent.SECURITY

    override fun getPreferenceScreenResId() = R.xml.app_lock_locked_apps

    override fun getLogTag() = "AppLockLockedApps"

    private data class AppRow(
        val packageName: String,
        val label: String,
        val icon: Drawable,
        val locked: Boolean,
    )

    companion object {
        private const val KEY_LIST = "app_lock_locked_apps_list"

        private fun buildAppRows(
            pm: PackageManager,
            lockablePackages: List<String>,
            locked: Set<String>,
        ): List<AppRow> {
            val rows = ArrayList<AppRow>()
            for (pkg in lockablePackages) {
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    rows.add(
                        AppRow(
                            packageName = pkg,
                            label = info.loadLabel(pm).toString(),
                            icon = info.loadIcon(pm),
                            locked = locked.contains(pkg),
                        ),
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                }
            }
            rows.sortBy { it.label.lowercase() }
            return rows
        }
    }
}
