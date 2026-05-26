package com.android.applock

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.applock.AppLockSettingsFragment
import com.android.settings.applock.AppLockSettingsGate
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class MainActivity : CollapsingToolbarBaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private var settingsContentShown = false

    private val settingsAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showSettingsContent()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsContentShown = savedInstanceState?.getBoolean(STATE_CONTENT_SHOWN) == true
        if (settingsContentShown) {
            showSettingsContent()
            return
        }
        if (AppLockSettingsGate.requiresInAppAuth(this)) {
            settingsAuthLauncher.launch(AppLockSettingsGate.createAuthIntent(this))
            return
        }
        showSettingsContent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_CONTENT_SHOWN, settingsContentShown)
    }

    private fun showSettingsContent() {
        if (settingsContentShown) return
        settingsContentShown = true
        if (supportFragmentManager.findFragmentById(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
            ) != null) {
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                AppLockSettingsFragment(),
            )
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

    companion object {
        private const val STATE_CONTENT_SHOWN = "settings_content_shown"
    }
}
