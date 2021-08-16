package com.kieronquinn.app.discoverkiller.model

import com.google.gson.Gson
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader

data class RemoteSettingsHolder(
    val overlayEnabled: Boolean,
    val overlayMode: Settings.OverlayMode,
    val overlayBackground: RemoteSplashLoader.SplashScreenType,
    val useMonet: Boolean,
    val monetColor: Int?,
    val overlayApp: String,
    val overlayAppNewTask: Boolean,
    val autoReloadSnapshot: Boolean
) {

    companion object {
        fun fromJson(json: String): RemoteSettingsHolder {
            return Gson().fromJson(json, RemoteSettingsHolder::class.java)
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

}
