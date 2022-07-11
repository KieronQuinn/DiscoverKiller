package com.kieronquinn.app.discoverkiller.utils.extensions

import android.content.*
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.IllegalArgumentException

val Context.isDarkMode: Boolean
    get() {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

fun Context.unbindSafely(serviceConnection: ServiceConnection): Boolean {
    return try {
        unbindService(serviceConnection)
        true
    }catch (e: IllegalArgumentException){
        false
    }
}

fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun <T> Context.contentResolverAsTFlow(uri: Uri, block: () -> T): Flow<T> = callbackFlow {
    val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            trySend(block())
        }
    }
    trySend(block())
    contentResolver.safeRegisterContentObserver(uri, true, observer)
    awaitClose {
        contentResolver.unregisterContentObserver(observer)
    }
}

fun Context.broadcastReceiverAsFlow(vararg actions: String) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    actions.forEach {
        registerReceiver(receiver, IntentFilter(it))
    }
    awaitClose {
        unregisterReceiver(receiver)
    }
}

fun Context.contentReceiverAsFlow(uri: Uri) = contentResolverAsTFlow(uri) {}