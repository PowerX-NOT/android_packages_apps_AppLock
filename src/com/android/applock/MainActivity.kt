package com.android.applock

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.applock.AppLockSettingsFragment
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class MainActivity : CollapsingToolbarBaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    AppLockSettingsFragment(),
                )
                .commit()
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        val destination = pref.fragment ?: return false
        SubSettingLauncher(this)
            .setDestination(destination)
            .setTitleText(pref.title ?: "")
            .setArguments(pref.extras ?: Bundle())
            .launch()
        return true
    }
}
