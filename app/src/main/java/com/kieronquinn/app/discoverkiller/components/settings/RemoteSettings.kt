package com.kieronquinn.app.discoverkiller.components.settings

import android.content.Context
import android.net.Uri
import com.kieronquinn.app.discoverkiller.model.RemoteSettingsHolder

abstract class RemoteSettings {

    companion object {

        private var INSTANCE: RemoteSettings? = null

        @Synchronized
        fun getInstance(): RemoteSettings {
            return INSTANCE ?: RemoteSettingsImpl().also {
                INSTANCE = it
            }
        }

    }

    abstract fun getRemoteSettings(context: Context): RemoteSettingsHolder

}

class RemoteSettingsImpl: RemoteSettings() {

    override fun getRemoteSettings(context: Context): RemoteSettingsHolder {
        val uri = Uri.parse("content://${RemoteSettingsProvider.AUTHORITY}/get")
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return if(cursor != null){
            cursor.moveToFirst()
            val json = cursor.getString(0)
            cursor.close()
            return RemoteSettingsHolder.fromJson(json)
        }else{
            SettingsImpl.getDefaultRemoteSettings()
        }
    }

}