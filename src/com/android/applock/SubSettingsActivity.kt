package com.android.applock

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.applock.AppLockLockedAppsFragment
import com.android.settings.applock.AppLockRelockFragment
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class SubSettingsActivity : CollapsingToolbarBaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return

        val fragmentName = intent.getStringExtra(EXTRA_DESTINATION) ?: return finish()
        val args = intent.getBundleExtra(EXTRA_ARGS)
        title = intent.getStringExtra(EXTRA_TITLE) ?: title

        val fragment = newSubSettingsFragment(fragmentName, args) ?: return finish()
        supportFragmentManager.beginTransaction()
            .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, fragment)
            .commit()
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

    private fun newSubSettingsFragment(className: String?, args: Bundle?): Fragment? {
        return when (className) {
            AppLockLockedAppsFragment::class.java.name -> AppLockLockedAppsFragment()
            AppLockRelockFragment::class.java.name -> AppLockRelockFragment()
            else -> null
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARGS = "args"
    }
}
