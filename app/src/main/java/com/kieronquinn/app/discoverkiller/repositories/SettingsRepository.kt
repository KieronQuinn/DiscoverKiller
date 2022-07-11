package com.kieronquinn.app.discoverkiller.repositories

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader.SplashScreenType
import com.kieronquinn.app.discoverkiller.model.settings.OverlaySettings
import com.kieronquinn.app.discoverkiller.providers.OverlaySettingsProvider
import com.kieronquinn.app.discoverkiller.repositories.BaseSettingsRepository.DiscoverKillerSetting
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayMode
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayType
import com.kieronquinn.app.discoverkiller.service.OverlayAppLauncherService
import com.kieronquinn.app.discoverkiller.service.OverlayUnsetService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface SettingsRepository {

    val enabled: DiscoverKillerSetting<Boolean>
    val overlayMode: DiscoverKillerSetting<OverlayMode>

    //The wallpaper colour to use (< Android 12)
    val monetColor: DiscoverKillerSetting<Int>
    val useMonet: DiscoverKillerSetting<Boolean>

    val overlayType: DiscoverKillerSetting<OverlayType>
    val overlayComponent: DiscoverKillerSetting<String>
    val overlayApp: DiscoverKillerSetting<String>
    val overlayAppNewTask: DiscoverKillerSetting<Boolean>
    val originalHandlesSearch: DiscoverKillerSetting<Boolean>

    val overlayBackground: DiscoverKillerSetting<SplashScreenType>

    val entertainmentSpaceRestart: DiscoverKillerSetting<Boolean>

    val rssUrl: DiscoverKillerSetting<String>
    val rssTitle: DiscoverKillerSetting<String>
    val rssLogoUrl: DiscoverKillerSetting<String>

    fun getOverlaySettings(): OverlaySettings

    enum class OverlayType {
        NOW, MEDIA
    }

    enum class OverlayMode {
        OVERLAY, APP
    }

}

class SettingsRepositoryImpl(
    private val context: Context,
    private val overlayRepository: OverlayRepository
): BaseSettingsRepositoryImpl(), SettingsRepository {

    companion object {
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val DEFAULT_OVERLAY_ENABLED = true

        private const val KEY_OVERLAY_MODE = "overlay_mode"
        private val DEFAULT_OVERLAY_MODE = OverlayMode.OVERLAY

        private const val KEY_OVERLAY_TYPE = "overlay_type"
        private val DEFAULT_OVERLAY_TYPE = OverlayType.NOW

        private const val KEY_OVERLAY_COMPONENT = "overlay_component"
        private val DEFAULT_OVERLAY_COMPONENT = ComponentName(
            BuildConfig.APPLICATION_ID, OverlayUnsetService::class.java.name
        ).flattenToString()

        private const val KEY_OVERLAY_APP = "overlay_app_component"
        private const val DEFAULT_OVERLAY_APP = ""

        private const val KEY_OVERLAY_APP_NEW_TASK = "overlay_app_new_task"
        private const val DEFAULT_OVERLAY_APP_NEW_TASK = true

        private const val KEY_ORIGINAL_HANDLES_SEARCH = "original_handles_search"
        private const val DEFAULT_ORIGINAL_HANDLES_SEARCH = false

        private const val KEY_ENTERTAINMENT_SPACE_RESTART = "entertainment_space_restart"
        private const val DEFAULT_ENTERTAINMENT_SPACE_RESTART = false

        private const val KEY_OVERLAY_BACKGROUND = "overlay_background"
        private val DEFAULT_OVERLAY_BACKGROUND = SplashScreenType.DEFAULT

        private const val KEY_USE_MONET = "use_monet"
        private const val DEFAULT_USE_MONET = true

        private const val KEY_MONET_COLOR = "monet_color"

        private const val KEY_RSS_URL = "rss_url"
        private const val DEFAULT_RSS_URL = ""

        private const val KEY_RSS_TITLE = "rss_title"
        private const val DEFAULT_RSS_TITLE = ""

        private const val KEY_RSS_LOGO_URL = "rss_logo_url"
        private const val DEFAULT_RSS_LOGO_URL = ""
    }

    private val onOverlaySettingsChanged = MutableSharedFlow<String>()

    override val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)
    }

    override val enabled = boolean(
        KEY_OVERLAY_ENABLED, DEFAULT_OVERLAY_ENABLED, onOverlaySettingsChanged
    )

    override val overlayMode = enum(
        KEY_OVERLAY_MODE, DEFAULT_OVERLAY_MODE, onOverlaySettingsChanged
    )

    override val monetColor = color(
        KEY_MONET_COLOR, Integer.MAX_VALUE, onOverlaySettingsChanged
    )

    override val useMonet = boolean(
        KEY_USE_MONET, DEFAULT_USE_MONET, onOverlaySettingsChanged
    )

    override val originalHandlesSearch = boolean(
        KEY_ORIGINAL_HANDLES_SEARCH, DEFAULT_ORIGINAL_HANDLES_SEARCH, onOverlaySettingsChanged
    )

    override val overlayType = enum(
        KEY_OVERLAY_TYPE, DEFAULT_OVERLAY_TYPE, onOverlaySettingsChanged
    )

    override val overlayComponent = string(
        KEY_OVERLAY_COMPONENT, DEFAULT_OVERLAY_COMPONENT, onOverlaySettingsChanged
    )

    override val overlayApp = string(
        KEY_OVERLAY_APP, DEFAULT_OVERLAY_APP, onOverlaySettingsChanged
    )

    override val overlayAppNewTask = boolean(
        KEY_OVERLAY_APP_NEW_TASK, DEFAULT_OVERLAY_APP_NEW_TASK, onOverlaySettingsChanged
    )

    override val overlayBackground = enum(
        KEY_OVERLAY_BACKGROUND, DEFAULT_OVERLAY_BACKGROUND, onOverlaySettingsChanged
    )

    override val entertainmentSpaceRestart = boolean(
        KEY_ENTERTAINMENT_SPACE_RESTART, DEFAULT_ENTERTAINMENT_SPACE_RESTART, onOverlaySettingsChanged
    )

    override val rssUrl = string(KEY_RSS_URL, DEFAULT_RSS_URL)
    override val rssTitle = string(KEY_RSS_TITLE, DEFAULT_RSS_TITLE)
    override val rssLogoUrl = string(KEY_RSS_LOGO_URL, DEFAULT_RSS_LOGO_URL)

    override fun getOverlaySettings(): OverlaySettings {
        var type = overlayType.getSync()
        var component = overlayComponent.getSync()
        var original = originalHandlesSearch.getSync()
        if(overlayMode.getSync() == OverlayMode.APP){
            type = OverlayType.NOW
            component = OverlayAppLauncherService.COMPONENT.flattenToString()
            //App Overlay has no handling so always use the original
            original = true
        }
        //Some overlays never handle search themselves, eg. the RSS overlay
        if(overlayRepository.shouldAlwaysUseOriginalHandler(component)){
            original = true
        }
        //Media Overlays can't handle search themselves
        if(type == OverlayType.MEDIA){
            original = true
        }
        return OverlaySettings(
            enabled.getSync(),
            type,
            component,
            original,
            entertainmentSpaceRestart.getSync()
        )
    }

    private fun setupOverlayChanged() = GlobalScope.launch {
        onOverlaySettingsChanged.collect {
            OverlaySettingsProvider.notifyChange(context)
            overlayRepository.terminateLauncher()
        }
    }

    init {
        setupOverlayChanged()
    }

}