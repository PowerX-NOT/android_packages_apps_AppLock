/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.app.Activity
import android.app.HiddenAppsManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.applock.R
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.dashboard.DashboardFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hide Apps settings: per-app hide mode and notification policy. */
class HideAppsSettingsFragment : DashboardFragment() {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var pendingPackage: String? = null
    private var isHideAppsVerified = false

    private val changeAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            pendingPackage = null
            pendingAllowNotifications = null
            return@registerForActivityResult
        }
        val allow = pendingAllowNotifications
        if (allow != null) {
            pendingAllowNotifications = null
            isHideAppsVerified = true
            HiddenAppsBinder.getOrNull(requireContext())
                ?.setAllowNotificationsFromHiddenApps(allow)
            bindNotificationSwitch()
            return@registerForActivityResult
        }
        val pkg = pendingPackage
        pendingPackage = null
        if (pkg != null) {
            isHideAppsVerified = true
            showModeDialog(pkg)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        bindNotificationSwitch()
        loadAppList()
    }

    override fun onResume() {
        super.onResume()
        bindNotificationSwitch()
        loadAppList()
    }

    private fun bindNotificationSwitch() {
        val switch = findPreference<SwitchPreferenceCompat>(KEY_ALLOW_NOTIFICATIONS) ?: return
        val manager = HiddenAppsBinder.getOrNull(requireContext())
        if (manager == null) {
            switch.isEnabled = false
            switch.summary = getString(R.string.hide_apps_service_unavailable)
            return
        }
        switch.isEnabled = true
        switch.isChecked = manager.isAllowNotificationsFromHiddenApps
        switch.setOnPreferenceChangeListener { _, newValue ->
            if (!AppLockCredentialsHelper.isSetup(requireContext()) || isHideAppsVerified) {
                manager.setAllowNotificationsFromHiddenApps(newValue as Boolean)
                return@setOnPreferenceChangeListener true
            }
            pendingPackage = null
            pendingAllowNotifications = newValue as Boolean
            changeAuthLauncher.launch(AppLockSettingsGate.createAuthIntent(requireContext()))
            false
        }
    }

    private var pendingAllowNotifications: Boolean? = null

    private fun showModeDialog(packageName: String) {
        val manager = HiddenAppsBinder.getOrNull(requireContext()) ?: return
        val pm = requireContext().packageManager
        val label = try {
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
        HideAppModeDialog.show(requireContext(), label, manager.getHiddenMode(packageName)) { mode ->
            manager.setHiddenMode(packageName, mode)
            loadAppList()
        }
    }

    private fun loadAppList() {
        val manager = HiddenAppsBinder.getOrNull(requireContext()) ?: return
        val category = findPreference<PreferenceCategory>(KEY_LIST) ?: return

        val context = requireContext()
        mainScope.launch {
            val pm = context.packageManager
            val rows = withContext(Dispatchers.Default) {
                buildRows(context, pm, manager)
            }

            category.removeAll()
            if (rows.isEmpty()) {
                category.addPreference(
                    Preference(requireContext()).apply {
                        key = "hide_apps_empty"
                        title = getString(R.string.hide_apps_empty)
                        isEnabled = false
                    },
                )
                return@launch
            }

            for (row in rows) {
                category.addPreference(
                    Preference(requireContext()).apply {
                        key = "hide_apps_pkg_${row.packageName}"
                        title = row.label
                        icon = row.icon
                        summary = row.statusSummary
                        setOnPreferenceClickListener {
                            if (!AppLockCredentialsHelper.isSetup(requireContext())) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.app_lock_enable_no_password,
                                    Toast.LENGTH_LONG,
                                ).show()
                                return@setOnPreferenceClickListener true
                            }
                            if (isHideAppsVerified) {
                                showModeDialog(row.packageName)
                                return@setOnPreferenceClickListener true
                            }
                            pendingPackage = row.packageName
                            changeAuthLauncher.launch(
                                AppLockSettingsGate.createAuthIntent(requireContext()),
                            )
                            true
                        }
                    },
                )
            }
        }
    }

    private data class AppRow(
        val packageName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable,
        val statusSummary: String,
    )

    private fun buildRows(
        context: android.content.Context,
        pm: PackageManager,
        manager: HiddenAppsManager,
    ): List<AppRow> {
        val rows = ArrayList<AppRow>()
        for (pkg in manager.hideablePackages) {
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val mode = manager.getHiddenMode(pkg)
                val summary = when (mode) {
                    HiddenAppsManager.HIDE_COMPLETE ->
                        context.getString(R.string.hide_apps_status_complete)
                    HiddenAppsManager.HIDE_LAUNCHER ->
                        context.getString(R.string.hide_apps_status_launcher)
                    else -> context.getString(R.string.hide_apps_status_none)
                }
                rows.add(
                    AppRow(
                        packageName = pkg,
                        label = info.loadLabel(pm).toString(),
                        icon = info.loadIcon(pm),
                        statusSummary = summary,
                    ),
                )
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        rows.sortBy { it.label.lowercase() }
        return rows
    }

    override fun getMetricsCategory() = MetricsProto.MetricsEvent.SECURITY

    override fun getPreferenceScreenResId() = R.xml.hide_apps_settings

    override fun getLogTag() = "HideAppsSettings"

    companion object {
        private const val KEY_ALLOW_NOTIFICATIONS = "hide_apps_allow_notifications"
        private const val KEY_LIST = "hide_apps_list_category"
    }
}
