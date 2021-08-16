package com.kieronquinn.app.discoverkiller.ui.screens.overlay.applauncher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.gsa.overlay.overlay.BaseOverlay
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoaderImpl
import com.kieronquinn.app.discoverkiller.databinding.OverlayAppLauncherBinding
import com.kieronquinn.app.discoverkiller.model.RemoteSettingsHolder
import com.kieronquinn.app.discoverkiller.ui.screens.overlay.snapshot.SnapshotOverlay
import com.kieronquinn.app.discoverkiller.utils.extensions.isAppInstalled
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.monetcompat.core.MonetCompat
import kotlinx.coroutines.delay
import org.koin.android.ext.android.get

class AppLauncherOverlay(private val context: Context, private val settings: RemoteSettingsHolder): BaseOverlay<OverlayAppLauncherBinding>(context, OverlayAppLauncherBinding::inflate) {

    private val appLaunchIntent by lazy {
        val overlayApp = settings.overlayApp
        if(overlayApp.isEmpty() || !context.packageManager.isAppInstalled(overlayApp)) return@lazy null
        context.packageManager.getLaunchIntentForPackage(settings.overlayApp)?.apply {
            if(settings.overlayAppNewTask){
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
    }

    private val splashLoader by lazy {
        RemoteSplashLoaderImpl()
    }

    private val discoverDefaultBackgroundColor by lazy {
        if(isDarkMode) SnapshotOverlay.BACKGROUND_COLOR_DISCOVER_DARK
        else SnapshotOverlay.BACKGROUND_COLOR_DISCOVER_LIGHT
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
        if(settings.overlayApp.isEmpty()) return@lazy null
        val selectedBackground = settings.overlayBackground
        if(selectedBackground == RemoteSplashLoader.SplashScreenType.DEFAULT) {
            splashLoader.getDefaultSplashForPackage(context, settings.overlayApp).type
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
                    settings.overlayBackground,
                    settings.overlayApp,
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
        Log.d("ALO", "onPanelOpen")
        appLaunchIntent?.let {
            isWaitingForOpen = true
            startActivity(it)
        } ?: run {
            val text = if(settings.overlayApp.isEmpty()){
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
            val selectedColor = settings.monetColor
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

    override fun onDragProgress(progress: Float) {
        super.onDragProgress(progress)
        if(splashScreenType == RemoteSplashLoader.SplashScreenType.TRANSPARENT){
            applyBlurToBackground(progress)
        }
    }

}