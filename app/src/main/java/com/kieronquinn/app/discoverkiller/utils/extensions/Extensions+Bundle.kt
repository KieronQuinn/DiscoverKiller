package com.kieronquinn.app.discoverkiller.utils.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.xposed.Xposed

private const val EXTRA_DISCOVER_KILLER_TOKEN = "discover_killer_token"

fun Bundle.putDiscoverKillerToken(context: Context) {
    val pendingIntent =
        PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
    putParcelable(EXTRA_DISCOVER_KILLER_TOKEN, pendingIntent)
}

fun Bundle.hasDiscoverKillerToken(): Boolean {
    return getParcelable<PendingIntent>(EXTRA_DISCOVER_KILLER_TOKEN)?.let {
        it.creatorPackage == BuildConfig.APPLICATION_ID
    } == true
}

fun Bundle.hasGSAToken(): Boolean {
    return getParcelable<PendingIntent>(EXTRA_DISCOVER_KILLER_TOKEN)?.let {
        it.creatorPackage == Xposed.GOOGLE_APP_PACKAGE_NAME
    } == true
}