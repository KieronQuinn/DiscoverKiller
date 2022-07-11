package com.kieronquinn.app.discoverkiller.utils.extensions

import android.os.RemoteException
import com.google.android.mediahome.launcheroverlay.aidl.ILauncherOverlay

fun ILauncherOverlay.ping(): Boolean {
    return try {
        hasOverlayContent()
        true
    }catch (e: RemoteException){
        false
    }
}