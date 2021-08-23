package com.kieronquinn.app.discoverkiller.components.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.model.RemoteSettingsHolder
import com.kieronquinn.app.discoverkiller.utils.extensions.toHexString
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Settings {

    abstract var overlayEnabled: Boolean
    abstract var overlayMode: OverlayMode
    abstract var overlayBackground: RemoteSplashLoader.SplashScreenType
    abstract var useMonet: Boolean
    abstract var monetColor: Int?
    abstract var overlayApp: String
    abstract var overlayAppNewTask: Boolean
    abstract var autoReloadSnapshot: Boolean
    abstract var ignoreXposedWarnings: Boolean

    abstract fun toRemoteSettings(): RemoteSettingsHolder

    enum class OverlayMode {
        SNAPSHOT, APP
    }

    companion object {
        fun positionToOverlayMode(position: Int): OverlayMode {
            return OverlayMode.values()[position]
        }
    }

}

class SettingsImpl(context: Context): Settings() {

    companion object {
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val DEFAULT_OVERLAY_ENABLED = true

        private const val KEY_OVERLAY_MODE = "overlay_mode"
        private val DEFAULT_OVERLAY_MODE = OverlayMode.SNAPSHOT

        private const val KEY_OVERLAY_BACKGROUND = "overlay_background"
        private val DEFAULT_OVERLAY_BACKGROUND = RemoteSplashLoader.SplashScreenType.DEFAULT

        private const val KEY_USE_MONET = "use_monet"
        private const val DEFAULT_USE_MONET = true

        private const val KEY_MONET_COLOR = "monet_color"

        private const val KEY_OVERLAY_APP = "overlay_app"
        private const val DEFAULT_OVERLAY_APP = ""

        private const val KEY_OVERLAY_APP_NEW_TASK = "overlay_app_new_task"
        private const val DEFAULT_OVERLAY_APP_NEW_TASK = true

        private const val KEY_AUTO_RELOAD_SNAPSHOT = "auto_reload_snapshot"
        private const val DEFAULT_AUTO_RELOAD_SNAPSHOT = true

        private const val KEY_IGNORE_XPOSED_WARNINGS = "ignore_xposed_warnings"
        private const val DEFAULT_IGNORE_XPOSED_WARNINGS = false

        fun getDefaultRemoteSettings(): RemoteSettingsHolder {
            return RemoteSettingsHolder(DEFAULT_OVERLAY_ENABLED, DEFAULT_OVERLAY_MODE, DEFAULT_OVERLAY_BACKGROUND, DEFAULT_USE_MONET, null, DEFAULT_OVERLAY_APP, DEFAULT_OVERLAY_APP_NEW_TASK, DEFAULT_AUTO_RELOAD_SNAPSHOT)
        }
    }

    override var overlayEnabled by shared(KEY_OVERLAY_ENABLED, DEFAULT_OVERLAY_ENABLED)
    override var overlayMode by sharedEnum(KEY_OVERLAY_MODE, DEFAULT_OVERLAY_MODE)
    override var overlayBackground by sharedEnum(KEY_OVERLAY_BACKGROUND, DEFAULT_OVERLAY_BACKGROUND)
    override var useMonet by shared(KEY_USE_MONET, DEFAULT_USE_MONET)
    override var monetColor by sharedColor(KEY_MONET_COLOR)
    override var overlayApp by shared(KEY_OVERLAY_APP, DEFAULT_OVERLAY_APP)
    override var overlayAppNewTask by shared(KEY_OVERLAY_APP_NEW_TASK, DEFAULT_OVERLAY_APP_NEW_TASK)
    override var autoReloadSnapshot by shared(KEY_AUTO_RELOAD_SNAPSHOT, DEFAULT_AUTO_RELOAD_SNAPSHOT)
    override var ignoreXposedWarnings by shared(KEY_IGNORE_XPOSED_WARNINGS, DEFAULT_IGNORE_XPOSED_WARNINGS)

    override fun toRemoteSettings(): RemoteSettingsHolder {
        return RemoteSettingsHolder(overlayEnabled, overlayMode, overlayBackground, useMonet, monetColor, overlayApp, overlayAppNewTask, autoReloadSnapshot)
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)
    }

    private fun shared(key: String, default: Boolean) = ReadWriteProperty({
        sharedPreferences.getBoolean(key, default)
    }, {
        sharedPreferences.edit().putBoolean(key, it).commit()
    })

    private fun shared(key: String, default: String) = ReadWriteProperty({
        sharedPreferences.getString(key, default) ?: default
    }, {
        sharedPreferences.edit().putString(key, it).commit()
    })

    private fun sharedColor(key: String) = ReadWriteProperty({
        val rawColor = sharedPreferences.getString(key, "") ?: ""
        if(rawColor.isEmpty()) null
        else Color.parseColor(rawColor)
    }, {
        sharedPreferences.edit().putString(key, it?.toHexString() ?: "").commit()
    })

    private inline fun <reified T : Enum<T>> sharedEnum(key: String, default: Enum<T>): ReadWriteProperty<Any?, T> {
        return object: ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return java.lang.Enum.valueOf(T::class.java, sharedPreferences.getString(key, default.name))
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                sharedPreferences.edit().putString(key, value.name).commit()
            }

        }
    }

    private inline fun <T> ReadWriteProperty(crossinline getValue: () -> T, crossinline setValue: (T) -> Unit): ReadWriteProperty<Any?, T> {
        return object: ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return getValue.invoke()
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                setValue.invoke(value)
            }

        }
    }

}