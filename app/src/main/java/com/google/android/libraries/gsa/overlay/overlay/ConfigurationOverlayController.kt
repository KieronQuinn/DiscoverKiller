package com.google.android.libraries.gsa.overlay.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.IBinder
import com.google.android.libraries.gsa.overlay.controllers.OverlayController
import com.google.android.libraries.gsa.overlay.controllers.OverlaysController

abstract class ConfigurationOverlayController(service: Service) : OverlaysController(service) {

    private val mContext: Context = service

    @Synchronized
    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    @Synchronized
    override fun onUnbind(intent: Intent) {
        super.onUnbind(intent)
    }

    override fun createController(
        configuration: Configuration?,
        i: Int,
        i2: Int
    ): OverlayController {
        var context = mContext
        if (VERSION.SDK_INT >= 17 && configuration != null) {
            context = context.createConfigurationContext(configuration)
        }
        return getOverlay(context)
    }

    abstract fun getOverlay(context: Context): OverlayController

}