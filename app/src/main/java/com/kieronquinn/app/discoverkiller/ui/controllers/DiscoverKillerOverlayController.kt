package com.kieronquinn.app.discoverkiller.ui.controllers

import android.app.Service
import android.content.Context
import com.google.android.libraries.gsa.overlay.controllers.OverlayController
import com.google.android.libraries.gsa.overlay.overlay.ConfigurationOverlayController
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.model.RemoteSettingsHolder
import com.kieronquinn.app.discoverkiller.ui.screens.overlay.applauncher.AppLauncherOverlay
import com.kieronquinn.app.discoverkiller.ui.screens.overlay.snapshot.SnapshotOverlay

class DiscoverKillerOverlayController(service: Service, private val settings: RemoteSettingsHolder): ConfigurationOverlayController(service) {

    override fun getOverlay(context: Context): OverlayController {
        return when(settings.overlayMode){
            Settings.OverlayMode.SNAPSHOT -> SnapshotOverlay(context, settings)
            Settings.OverlayMode.APP -> AppLauncherOverlay(context, settings)
        }
    }

}