package com.kieronquinn.app.discoverkiller.utils.extensions

import android.os.RemoteException
import com.kieronquinn.app.discoverkiller.service.IDiscoverKiller

fun IDiscoverKiller.safePing(): Boolean {
    return try {
        ping()
        true
    }catch (e: RemoteException){
        //Dead
        false
    }
}