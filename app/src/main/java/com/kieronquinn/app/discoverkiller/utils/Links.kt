package com.kieronquinn.app.discoverkiller.utils

import android.content.Intent
import android.net.Uri

object Links {
    const val LINK_GITHUB = "https://kieronquinn.co.uk/redirect/DiscoverKiller/github"
    const val LINK_XDA = "https://kieronquinn.co.uk/redirect/DiscoverKiller/xda"
    const val LINK_DONATE = "https://kieronquinn.co.uk/redirect/DiscoverKiller/donate"
    const val LINK_TWITTER = "https://kieronquinn.co.uk/redirect/DiscoverKiller/twitter"

    fun createLinkIntent(link: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(link)
        }
    }
}