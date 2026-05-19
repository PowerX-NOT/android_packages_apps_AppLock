/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.Context
import android.view.View
import androidx.preference.PreferenceViewHolder
import com.android.applock.R
import com.android.settingslib.widget.SelectorWithWidgetPreference

/** Checkbox row for one lockable app; checkbox is aligned to the end of the row. */
class AppLockAppPreference(context: Context) : SelectorWithWidgetPreference(context, true) {

    init {
        layoutResource = R.layout.app_lock_preference_app_row
        setExtraWidgetOnClickListener(null)
    }

    override fun onClick() {
        // SelectorWithWidgetPreference.onClick() does not call CheckBoxPreference.onClick().
        val newChecked = !isChecked
        if (callChangeListener(newChecked)) {
            isChecked = newChecked
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val widgetFrame = holder.findViewById(android.R.id.widget_frame)

        if (!isEnabled) {
            holder.itemView.isClickable = false
            holder.itemView.setOnClickListener(null)
            widgetFrame?.isClickable = false
            widgetFrame?.setOnClickListener(null)
            return
        }

        val toggleClickListener = View.OnClickListener { performClick() }

        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
        holder.itemView.setOnClickListener(toggleClickListener)

        widgetFrame?.isClickable = true
        widgetFrame?.isFocusable = true
        widgetFrame?.setOnClickListener(toggleClickListener)
    }
}
