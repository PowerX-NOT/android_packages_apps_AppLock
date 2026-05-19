package com.android.settings.dashboard

import android.os.Bundle
import com.android.settingslib.widget.SettingsBasePreferenceFragment

open class DashboardFragment : SettingsBasePreferenceFragment() {
    open fun getMetricsCategory(): Int = 0
    open fun getPreferenceScreenResId(): Int = 0
    open fun getLogTag(): String = "DashboardFragment"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val resId = getPreferenceScreenResId()
        if (resId != 0) {
            setPreferencesFromResource(resId, rootKey)
        }
    }

    override fun onResume() {
        super.onResume()
        val t = preferenceScreen?.title
        if (t != null && t.isNotEmpty()) {
            activity?.title = t
        }
    }
}
