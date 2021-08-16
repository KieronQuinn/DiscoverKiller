package com.kieronquinn.app.discoverkiller.components.blur

import android.view.View
import android.view.Window

class BlurProvider28: BlurProvider() {

    override val minBlurRadius = 0f
    override val maxBlurRadius = 0f

    override fun applyBlurToWindow(window: Window, ratio: Float) {
        //no-op
    }

    override fun applyDialogBlur(dialogWindow: Window, appWindow: Window, ratio: Float) {
        dialogWindow.addDimming()
    }

}