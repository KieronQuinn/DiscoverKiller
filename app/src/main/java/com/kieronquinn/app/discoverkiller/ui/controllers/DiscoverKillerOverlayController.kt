package com.kieronquinn.app.discoverkiller.ui.controllers

import android.app.Service
import android.content.Context
import com.google.android.libraries.gsa.overlay.controllers.OverlayController
import com.google.android.libraries.gsa.overlay.overlay.ConfigurationOverlayController
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.model.RemoteSettingsHolder
import com.kieronquinn.app.discoverkiller.ui.screens.overlay.applauncher.AppLauncherOverlay
import com.kieronquinn.app.discoverkiller.ui.screens.overlay.snapshot.SnapshotOverlay
import com.kieronquinn.app.discoverkiller.utils.OverlayContext

class DiscoverKillerOverlayController(service: Service, private val settings: RemoteSettingsHolder): ConfigurationOverlayController(service) {

    override fun getOverlay(context: Context): OverlayController {
        val overlayContext = OverlayContext(context)
        return when(settings.overlayMode){
            Settings.OverlayMode.SNAPSHOT -> SnapshotOverlay(overlayContext, settings)
            Settings.OverlayMode.APP -> AppLauncherOverlay(overlayContext, settings)
        }
    }

}