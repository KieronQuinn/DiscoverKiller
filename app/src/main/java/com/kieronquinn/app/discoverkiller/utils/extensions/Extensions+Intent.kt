package com.kieronquinn.app.discoverkiller.utils.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.xposed.Xposed


fun Intent.putDiscoverKillerToken(context: Context){
    putExtras(Bundle().apply { putDiscoverKillerToken(context) })
}

fun Intent.hasDiscoverKillerToken(): Boolean {
    return extras?.hasDiscoverKillerToken() ?: false
}

fun Intent.hasGSAToken(): Boolean {
    return extras?.hasGSAToken() ?: false
}