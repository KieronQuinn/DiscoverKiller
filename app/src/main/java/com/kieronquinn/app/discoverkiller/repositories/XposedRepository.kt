package com.kieronquinn.app.discoverkiller.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.kieronquinn.app.discoverkiller.service.IDiscoverKillerClient
import com.kieronquinn.app.discoverkiller.utils.extensions.putDiscoverKillerToken
import com.kieronquinn.app.discoverkiller.utils.extensions.suspendCoroutineWithTimeout
import com.kieronquinn.app.discoverkiller.xposed.Xposed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

interface XposedRepository {

    suspend fun isModuleHooked(): Boolean

}

class XposedRepositoryImpl(private val context: Context): XposedRepository {

    companion object {
        private const val TIMEOUT = 2500L
        private val SERVICE_INTENT = Intent("com.google.android.apps.gsa.publicsearch.IPublicSearchService").apply {
            `package` = Xposed.GOOGLE_APP_PACKAGE_NAME
            component = ComponentName(
                Xposed.GOOGLE_APP_PACKAGE_NAME,
                "com.google.android.apps.gsa.publicsearch.PublicSearchService"
            )
        }
    }

    private val serviceLock = Mutex()

    override suspend fun isModuleHooked(): Boolean {
        return runWithService {
            try {
                IDiscoverKillerClient.Stub.asInterface(it).areHooksWorking()
            }catch (e: Exception){
                false
            }
        } ?: false
    }

    private suspend fun <T> runWithService(block: (IBinder) -> T): T? = serviceLock.withLock {
        runWithServiceLocked(block)
    }

    private suspend fun <T> runWithServiceLocked(
        block: (IBinder) -> T
    ): T? = suspendCoroutineWithTimeout(TIMEOUT) {
        val serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                it.resume(block(binder))
                context.unbindService(this)
            }

            override fun onServiceDisconnected(component: ComponentName) {

            }
        }
        context.bindService(SERVICE_INTENT.apply {
            putDiscoverKillerToken(context)
        }, serviceConnection, Context.BIND_AUTO_CREATE)
    }

}