/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import android.content.Context
import com.android.settingslib.widget.SelectorWithWidgetPreference

/** Checkbox row for one lockable app in the locked-apps list. */
class AppLockAppPreference(context: Context) : SelectorWithWidgetPreference(context, true)
