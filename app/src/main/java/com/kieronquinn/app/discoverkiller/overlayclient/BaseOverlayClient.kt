package com.kieronquinn.app.discoverkiller.overlayclient

import android.os.IBinder
import android.view.WindowManager
import com.google.android.libraries.launcherclient.ILauncherOverlay

abstract class BaseOverlayClient: ILauncherOverlay.Stub() {

    var isVisible = false

    abstract fun reattach()
    abstract fun detach()

    override fun setActivityState(flags: Int) {
        isVisible = when(flags){
            1 -> true
            else -> false
        }
    }

}