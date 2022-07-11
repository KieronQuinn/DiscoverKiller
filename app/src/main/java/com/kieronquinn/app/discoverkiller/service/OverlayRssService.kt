package com.kieronquinn.app.discoverkiller.service

import android.content.ComponentName
import android.content.Context
import com.google.android.gsa.overlay.controllers.OverlayController
import com.google.android.gsa.overlay.controllers.OverlaysController
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.ui.controllers.ConfigurationOverlayController
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.rss.RssOverlay
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.unset.UnsetOverlay

class OverlayRssService: BaseOverlayService() {

    companion object {
        val COMPONENT = ComponentName(
            BuildConfig.APPLICATION_ID, OverlayRssService::class.java.name
        )
    }

    override fun createOverlayController(): OverlaysController {
        return RssOverlayController()
    }

    private inner class RssOverlayController: ConfigurationOverlayController(this) {
        override fun getOverlay(context: Context): OverlayController {
            return RssOverlay(context)
        }
    }

}