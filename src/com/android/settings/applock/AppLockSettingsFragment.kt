/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applock

import com.android.applock.R
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.dashboard.DashboardFragment

/** Root App Lock settings screen (Settings dashboard entry). */
class AppLockSettingsFragment : DashboardFragment() {

    override fun getMetricsCategory() = MetricsProto.MetricsEvent.EVOLVER

    override fun getPreferenceScreenResId() = R.xml.app_lock_settings

    override fun getLogTag() = "AppLockSettings"
}
