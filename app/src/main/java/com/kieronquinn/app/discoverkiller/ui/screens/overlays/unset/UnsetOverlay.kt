package com.kieronquinn.app.discoverkiller.ui.screens.overlays.unset

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.BaseOverlay
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.databinding.OverlayUnsetBinding
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.activities.MainActivity
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.removeStatusNavBackgroundOnPreDraw
import com.kieronquinn.monetcompat.core.MonetCompat
import org.koin.core.component.inject

class UnsetOverlay(context: Context): BaseOverlay<OverlayUnsetBinding>(context, OverlayUnsetBinding::inflate) {

    companion object {
        private const val KEY_BACKGROUND_ALPHA_PROGRESS = "background_alpha_progress"
        private val BACKGROUND_COLOR_DISCOVER_LIGHT = Color.parseColor("#F8F9FA")
        private val BACKGROUND_COLOR_DISCOVER_DARK = Color.parseColor("#1F1F20")
    }

    private val isDarkMode by lazy {
        context.isDarkMode
    }

    private val discoverDefaultBackgroundColor by lazy {
        if(isDarkMode) BACKGROUND_COLOR_DISCOVER_DARK
        else BACKGROUND_COLOR_DISCOVER_LIGHT
    }

    private val settings by inject<SettingsRepository>()

    private val monet by lazy {
        setupMonet()
        MonetCompat.setup(context).apply {
            defaultBackgroundColor = discoverDefaultBackgroundColor
            updateMonetColors()
        }
    }

    private fun setupMonet(){
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor.getSync()
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

    private var backgroundAlphaProgress = 0f

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        backgroundAlphaProgress = bundle?.getFloat(KEY_BACKGROUND_ALPHA_PROGRESS, 0f) ?: 0f
        val background = monet.getBackgroundColor(binding.root.context, isDarkMode)
        val text = monet.getBackgroundColor(binding.root.context, !isDarkMode)
        binding.root.setBackgroundColor(ColorUtils.setAlphaComponent(background, 128))
        binding.overlayUnsetText.setTextColor(text)
        binding.root.setOnClickListener {
            startActivity(
                Intent(packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
                }
            )
        }
        window?.decorView?.removeStatusNavBackgroundOnPreDraw()
    }

    override fun onResume() {
        super.onResume()
        updateProgressViews(backgroundAlphaProgress, true)
        window?.decorView?.post {
            lifecycleScope.launchWhenResumed {
                //Fix case where *two* status bar backgrounds sometimes appear
                window.decorView.removeStatusNavBackgroundOnPreDraw()
            }
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putFloat(KEY_BACKGROUND_ALPHA_PROGRESS, backgroundAlphaProgress)
    }

    override fun onDragProgress(progress: Float) {
        super.onDragProgress(progress)
        updateProgressViews(progress)
    }

    private fun updateProgressViews(progress: Float, force: Boolean = false){
        if(backgroundAlphaProgress == progress && !force) return
        backgroundAlphaProgress = progress
        binding.root.alpha = progress
    }

    override fun onHomeOrBackPressed(i: Int) {
        super.onHomeOrBackPressed(i)
        if(i == 0){
            window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        }
    }

}