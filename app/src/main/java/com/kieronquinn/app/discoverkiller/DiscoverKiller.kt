package com.kieronquinn.app.discoverkiller

import android.app.Application
import com.kieronquinn.app.discoverkiller.utils.AppIconRequestHandler
import com.squareup.picasso.Picasso

class DiscoverKiller : Application() {

    override fun onCreate() {
        super.onCreate()
        Picasso.setSingletonInstance(
            Picasso.Builder(this)
                .addRequestHandler(AppIconRequestHandler(this))
                .build()
        )
    }

}