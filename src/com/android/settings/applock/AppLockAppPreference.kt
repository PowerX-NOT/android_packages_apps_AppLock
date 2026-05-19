/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.Context
import androidx.preference.PreferenceViewHolder
import com.android.applock.R
import com.android.settingslib.widget.SelectorWithWidgetPreference

/** Checkbox row for one lockable app; checkbox is aligned to the end of the row. */
class AppLockAppPreference(context: Context) : SelectorWithWidgetPreference(context, true) {

    init {
        layoutResource = R.layout.app_lock_preference_app_row
        setExtraWidgetOnClickListener(null)
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

        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
        holder.itemView.setOnClickListener { onClick() }

        widgetFrame?.isClickable = true
        widgetFrame?.isFocusable = true
        widgetFrame?.setOnClickListener { onClick() }
    }
}
