package com.kieronquinn.app.discoverkiller.utils.extensions

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.components.xposed.apps.GoogleApp

fun BroadcastReceiver(onReceive: (Context, Intent?) -> Unit) = object: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent?) {
        onReceive.invoke(context, intent)
    }
}

private const val SECURE_BROADCAST_RECEIVER_EXTRA_PENDING_INTENT = "verification_intent"
private val SECURE_BROADCAST_PACKAGE_WHITELIST = arrayOf(GoogleApp.PACKAGE_NAME, BuildConfig.APPLICATION_ID)

/**
 *  A BroadcastReceiver that must contain a PendingIntent created by the Google App or Discover Killer to work.
 *  This should be used in conjunction with [Context.sendSecureBroadcast]
 */
fun SecureBroadcastReceiver(onReceive: (Context, Intent?) -> Unit) = BroadcastReceiver { context, intent ->
    val pendingIntent = intent?.getParcelableExtra<PendingIntent>(SECURE_BROADCAST_RECEIVER_EXTRA_PENDING_INTENT) ?: return@BroadcastReceiver
    if(!SECURE_BROADCAST_PACKAGE_WHITELIST.contains(pendingIntent.creatorPackage)) return@BroadcastReceiver
    onReceive.invoke(context, intent)
}

/**
 *  Sends a Broadcast to a given intent, with the added PendingIntent with the creatorPackage of this app
 *  This should be used in conjunction with [SecureBroadcastReceiver]
 */
fun Context.sendSecureBroadcast(intent: Intent){
    sendBroadcast(intent.apply {
        putExtra(SECURE_BROADCAST_RECEIVER_EXTRA_PENDING_INTENT, PendingIntent.getBroadcast(this@sendSecureBroadcast, intent.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE))
    })
}