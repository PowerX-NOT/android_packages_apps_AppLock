/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.Context
import android.view.View
import androidx.preference.PreferenceViewHolder
import com.android.settingslib.widget.SelectorWithWidgetPreference

/** Single-choice row for relock policy (only one option may be selected). */
class AppLockRelockBehaviorPreference(context: Context) :
    SelectorWithWidgetPreference(context, true) {

    init {
        setExtraWidgetOnClickListener(null)
    }

    override fun onClick() {
        // SelectorWithWidgetPreference.onClick() does not call CheckBoxPreference.onClick().
        if (isChecked) return
        if (callChangeListener(true)) {
            isChecked = true
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

        val selectClickListener = View.OnClickListener { performClick() }

        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
        holder.itemView.setOnClickListener(selectClickListener)

        widgetFrame?.isClickable = true
        widgetFrame?.isFocusable = true
        widgetFrame?.setOnClickListener(selectClickListener)
    }
}
