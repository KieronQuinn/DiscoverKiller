package com.kieronquinn.app.discoverkiller.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.gsa.overlay.controllers.OverlaysController

abstract class BaseOverlayService: Service() {

    abstract fun createOverlayController(): OverlaysController

    private lateinit var overlaysController: OverlaysController

    override fun onCreate() {
        super.onCreate()
        overlaysController = createOverlayController()
    }

    override fun onDestroy() {
        overlaysController.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return overlaysController.onBind(intent, null)
    }

    override fun onUnbind(intent: Intent): Boolean {
        overlaysController.onUnbind(intent)
        return false
    }

}