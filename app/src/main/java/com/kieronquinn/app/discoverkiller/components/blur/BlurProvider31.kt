package com.kieronquinn.app.discoverkiller.components.blur

import android.content.res.Resources
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.kieronquinn.app.discoverkiller.R

@RequiresApi(Build.VERSION_CODES.S)
class BlurProvider31(resources: Resources): BlurProvider() {

    override val minBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.min_window_blur_radius).toFloat()
    }

    override val maxBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.max_window_blur_radius).toFloat()
    }

    override fun applyDialogBlur(dialogWindow: Window, appWindow: Window, ratio: Float) {
        applyBlurToWindow(dialogWindow, ratio)
    }

    override fun applyBlurToWindow(window: Window, ratio: Float) {
        val radius = blurRadiusOfRatio(ratio)
        Log.d("BP", "setBackgroundBlurRadius $radius")
        window.attributes.blurBehindRadius = radius
    }

}