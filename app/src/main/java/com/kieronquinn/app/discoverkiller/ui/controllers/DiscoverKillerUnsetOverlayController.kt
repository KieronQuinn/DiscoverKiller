package com.kieronquinn.app.discoverkiller.ui.controllers

import android.app.Service
import android.content.Context
import com.google.android.gsa.overlay.controllers.OverlayController
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.unset.UnsetOverlay

class DiscoverKillerUnsetOverlayController(
    service: Service
): ConfigurationOverlayController(service) {

    override fun getOverlay(context: Context): OverlayController {
        return UnsetOverlay(context)
    }

}