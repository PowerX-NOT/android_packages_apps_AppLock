/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.app.HiddenAppsManager
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.android.applock.R

/** Dialog to pick hide mode for an app. */
object HideAppModeDialog {

    private val MODES = intArrayOf(
        HiddenAppsManager.HIDE_COMPLETE,
        HiddenAppsManager.HIDE_LAUNCHER,
        HiddenAppsManager.HIDE_NONE,
    )

    fun show(
        context: Context,
        appLabel: String,
        currentMode: Int,
        onModeSelected: (Int) -> Unit,
    ) {
        val titles = arrayOf(
            context.getString(R.string.hide_apps_mode_complete_title),
            context.getString(R.string.hide_apps_mode_launcher_title),
            context.getString(R.string.hide_apps_mode_none_title),
        )
        val summaries = arrayOf(
            context.getString(R.string.hide_apps_mode_complete_summary),
            context.getString(R.string.hide_apps_mode_launcher_summary),
            context.getString(R.string.hide_apps_mode_none_title),
        )
        val items = titles.indices.map { i ->
            "${titles[i]}\n${summaries[i]}"
        }.toTypedArray()

        var checked = MODES.indexOf(currentMode).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.hide_apps_dialog_title, appLabel))
            .setSingleChoiceItems(items, checked) { _, which ->
                checked = which
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                onModeSelected(MODES[checked])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
