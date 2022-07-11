package com.kieronquinn.app.discoverkiller.service

import android.content.ComponentName
import android.content.Context
import com.google.android.gsa.overlay.controllers.OverlayController
import com.google.android.gsa.overlay.controllers.OverlaysController
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.ui.controllers.ConfigurationOverlayController
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.applauncher.AppLauncherOverlay
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.unset.UnsetOverlay

class OverlayAppLauncherService: BaseOverlayService() {

    companion object {
        val COMPONENT = ComponentName(
            BuildConfig.APPLICATION_ID, OverlayAppLauncherService::class.java.name
        )
    }

    override fun createOverlayController(): OverlaysController {
        return AppLauncherOverlayController()
    }

    private inner class AppLauncherOverlayController: ConfigurationOverlayController(this) {
        override fun getOverlay(context: Context): OverlayController {
            return AppLauncherOverlay(context)
        }
    }

}