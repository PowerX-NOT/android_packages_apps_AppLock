package com.android.applock

import android.database.Cursor
import android.database.MatrixCursor
import android.provider.SearchIndexableResource
import android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME
import android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID
import android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION
import android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS
import android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE
import android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK
import android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID
import android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS
import android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS
import android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS
import android.provider.SearchIndexablesProvider

/** Exposes App Lock to Settings global search. */
class SettingsSearchIndexablesProvider : SearchIndexablesProvider() {

    private val resources = listOf(
        SearchIndexableResource(
            1,
            R.xml.app_lock_settings,
            MainActivity::class.java.name,
            R.drawable.ic_settings_applock,
        ),
    )

    override fun onCreate(): Boolean = true

    override fun queryXmlResources(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(INDEXABLES_XML_RES_COLUMNS)
        resources.forEach { resource ->
            val row = arrayOfNulls<Any>(INDEXABLES_XML_RES_COLUMNS.size)
            row[COLUMN_INDEX_XML_RES_RANK] = resource.rank
            row[COLUMN_INDEX_XML_RES_RESID] = resource.xmlResId
            row[COLUMN_INDEX_XML_RES_CLASS_NAME] = null
            row[COLUMN_INDEX_XML_RES_ICON_RESID] = resource.iconResId
            row[COLUMN_INDEX_XML_RES_INTENT_ACTION] = "com.android.settings.action.IA_SETTINGS"
            row[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] = "com.android.applock"
            row[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = resource.className
            cursor.addRow(row)
        }
        return cursor
    }

    override fun queryRawData(projection: Array<out String>?): Cursor =
        MatrixCursor(INDEXABLES_RAW_COLUMNS)

    override fun queryNonIndexableKeys(projection: Array<out String>?): Cursor =
        MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS)
}
