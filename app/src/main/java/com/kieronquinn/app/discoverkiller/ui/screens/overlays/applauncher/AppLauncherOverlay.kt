package com.kieronquinn.app.discoverkiller.ui.screens.overlays.applauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoaderImpl
import com.kieronquinn.app.discoverkiller.databinding.OverlayAppLauncherBinding
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.BaseOverlay
import com.kieronquinn.app.discoverkiller.utils.extensions.isAppInstalled
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.removeStatusNavBackgroundOnPreDraw
import com.kieronquinn.monetcompat.core.MonetCompat
import kotlinx.coroutines.delay
import org.koin.core.component.inject

class AppLauncherOverlay(private val context: Context): BaseOverlay<OverlayAppLauncherBinding>(context, OverlayAppLauncherBinding::inflate) {

    companion object {
        private val BACKGROUND_COLOR_DISCOVER_LIGHT = Color.parseColor("#F8F9FA")
        private val BACKGROUND_COLOR_DISCOVER_DARK = Color.parseColor("#1F1F20")
    }

    private val settings by inject<SettingsRepository>()

    private val overlayComponent by lazy {
        val overlayApp = settings.overlayApp.getSync()
        if(overlayApp.isEmpty()) return@lazy null
        ComponentName.unflattenFromString(overlayApp)!!
    }

    private val overlayPackage by lazy {
        overlayComponent?.packageName
    }

    private val appLaunchIntent by lazy {
        val overlayApp = overlayPackage ?: return@lazy null
        val newTask = settings.overlayAppNewTask.getSync()
        if(!context.packageManager.isAppInstalled(overlayApp)) return@lazy null
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = overlayComponent
            if(newTask){
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        if(context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()){
            intent
        }else null
    }

    private val splashLoader by lazy {
        RemoteSplashLoaderImpl()
    }

    private val discoverDefaultBackgroundColor by lazy {
        if(isDarkMode) BACKGROUND_COLOR_DISCOVER_DARK
        else BACKGROUND_COLOR_DISCOVER_LIGHT
    }

    private val defaultAccentColor by lazy {
        Color.parseColor("#03A9F4")
    }

    private val monet by lazy {
        setupMonet()
        MonetCompat.setup(context).apply {
            defaultBackgroundColor = discoverDefaultBackgroundColor
            defaultAccentColor = this@AppLauncherOverlay.defaultAccentColor
            updateMonetColors()
        }
    }

    private val splashScreenType by lazy {
        val overlayApp = overlayPackage ?: return@lazy null
        if(overlayApp.isEmpty()) return@lazy null
        val selectedBackground = settings.overlayBackground.getSync()
        if(selectedBackground == RemoteSplashLoader.SplashScreenType.DEFAULT) {
            splashLoader.getDefaultSplashForPackage(context, overlayApp).type
        }else{
            selectedBackground
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        binding.root.removeAllViews()
        lifecycleScope.launchWhenResumed {
            if(splashScreenType == RemoteSplashLoader.SplashScreenType.MONET_BACKGROUND) {
                monet.awaitMonetReady()
            }
            val view = if(appLaunchIntent != null) {
               splashLoader.inflateSplashScreen(
                    binding.root.context,
                    settings.overlayBackground.getSync(),
                    overlayPackage ?: "",
                    binding.root
                )
            }else{
                splashLoader.inflateSplashScreen(
                    binding.root.context,
                    RemoteSplashLoader.SplashScreenType.DEFAULT,
                    BuildConfig.APPLICATION_ID,
                    binding.root
                )
            }
            binding.root.addView(view)
        }
    }

    override fun onHomeOrBackPressed(i: Int) {
        super.onHomeOrBackPressed(i)
        if(i == 0){
            window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        }
    }

    private var isWaitingForOpen = false
    override fun onPanelOpen() {
        super.onPanelOpen()
        appLaunchIntent?.let {
            isWaitingForOpen = true
            startActivity(it)
        } ?: run {
            val text = if(settings.overlayApp.getSync().isEmpty()){
                remoteContext.getString(R.string.app_launch_not_set)
            }else{
                remoteContext.getString(R.string.app_launch_failed)
            }
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if(isWaitingForOpen) {
            isWaitingForOpen = false
            closeAfterDelay()
        }
    }

    private fun setupMonet(){
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor.getSync()
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

    @Synchronized
    private fun closeAfterDelay(){
        lifecycleScope.launchWhenCreated {
            //Give long enough for the activity to open before animating away
            delay(1000)
            closePanel()
        }
    }

}