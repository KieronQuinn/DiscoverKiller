package com.kieronquinn.app.discoverkiller.components.settings

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.koin.android.ext.android.inject

class RemoteSettingsProvider : ContentProvider() {

    private val settings by lazy {
        SettingsImpl(context!!)
    }

    companion object {
        const val AUTHORITY = "com.kieronquinn.app.discoverkiller.provider.remotesettings"
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        init {
            uriMatcher.addURI(AUTHORITY, "get", 1)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val cursor = MatrixCursor(arrayOf("json"))
        when (uriMatcher.match(uri)) {
            1 -> {
                cursor.newRow().add("json", settings.toRemoteSettings().toJson())
                return cursor
            }
        }
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

}