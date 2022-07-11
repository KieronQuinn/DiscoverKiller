package com.kieronquinn.app.discoverkiller.utils.extensions

import android.os.RemoteException
import com.google.android.libraries.launcherclient.ILauncherOverlay

fun ILauncherOverlay.ping(): Boolean {
    return try {
        unusedMethod()
        true
    }catch (e: RemoteException){
        false
    }
}