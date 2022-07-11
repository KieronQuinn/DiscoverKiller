package com.kieronquinn.app.discoverkiller.model.rss

data class RssItem(
    val url: String,
    val imageUrl: String?,
    val title: String,
    val description: String?
)
