package com.android.settings.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.applock.SubSettingsActivity

class SubSettingLauncher(private val context: Context) {
    private var destination: String? = null
    private var titleText: CharSequence? = null
    private var args: Bundle? = null

    fun setDestination(destination: String): SubSettingLauncher {
        this.destination = destination
        return this
    }

    fun setTitleText(title: CharSequence): SubSettingLauncher {
        this.titleText = title
        return this
    }

    fun setSourceMetricsCategory(@Suppress("UNUSED_PARAMETER") category: Int): SubSettingLauncher =
        this

    fun setArguments(args: Bundle): SubSettingLauncher {
        this.args = args
        return this
    }

    fun launch() {
        val dest = destination ?: return
        val intent = Intent(context, SubSettingsActivity::class.java).apply {
            putExtra(SubSettingsActivity.EXTRA_DESTINATION, dest)
            putExtra(SubSettingsActivity.EXTRA_TITLE, titleText?.toString())
            putExtra(SubSettingsActivity.EXTRA_ARGS, args)
        }
        context.startActivity(intent)
    }
}
