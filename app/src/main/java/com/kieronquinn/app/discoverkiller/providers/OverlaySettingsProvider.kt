package com.kieronquinn.app.discoverkiller.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.bundleOf
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.model.settings.OverlaySettings
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.utils.extensions.hasGSAToken
import com.kieronquinn.app.discoverkiller.utils.extensions.putDiscoverKillerToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.koin.android.ext.android.inject

class OverlaySettingsProvider: ContentProvider() {

    companion object {
        private const val KEY_SETTINGS = "settings"
        private val URI =
            Uri.parse("content://${BuildConfig.APPLICATION_ID}.overlay_settings")

        fun getCurrentOverlaySettings(context: Context): OverlaySettings {
            val extras = Bundle().apply {
                putDiscoverKillerToken(context)
            }
            val bundle = context.contentResolver.call(URI, "get", null, extras)
            return OverlaySettings.fromJson(bundle!!.getString(KEY_SETTINGS)!!)
        }

        fun getOverlaySettings(context: Context) = callbackFlow {
            val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    trySend(getCurrentOverlaySettings(context))
                }
            }
            trySend(getCurrentOverlaySettings(context))
            context.contentResolver.registerContentObserver(URI, true, observer)
            awaitClose {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }

        fun notifyChange(context: Context) {
            context.contentResolver.notifyChange(URI, null, 0)
        }
    }

    private val settings by inject<SettingsRepository>()

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        if(extras?.hasGSAToken() != true){
            throw SecurityException("Cannot access Overlay Settings outside Xposed module")
        }
        return bundleOf(
            KEY_SETTINGS to settings.getOverlaySettings().toJson()
        )
    }

    override fun getType(p0: Uri): String? {
        return null
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        throw RuntimeException("Unsupported")
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        throw RuntimeException("Unsupported")
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        throw RuntimeException("Unsupported")
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        throw RuntimeException("Unsupported")
    }

}