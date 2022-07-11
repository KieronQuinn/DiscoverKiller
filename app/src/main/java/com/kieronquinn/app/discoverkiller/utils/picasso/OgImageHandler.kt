package com.kieronquinn.app.discoverkiller.utils.picasso

import android.net.Uri
import com.kieronquinn.app.discoverkiller.utils.rss.OgTagParser
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

class OgImageHandler: RequestHandler() {

    companion object {
        private const val OGIMAGE_URI_PREFIX = "oguriimage:"

        fun createUri(url: String): Uri {
            return Uri.parse("$OGIMAGE_URI_PREFIX$url")
        }
    }

    private val picasso = Picasso.get()

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri.toString().startsWith(OGIMAGE_URI_PREFIX)
    }

    override fun load(request: Request, networkPolicy: Int): Result? {
        val url = request.uri.toString().substring(OGIMAGE_URI_PREFIX.length)
        val tag = OgTagParser().getContents(url) ?: return null
        return Result(picasso.load(tag.image).get(), Picasso.LoadedFrom.NETWORK)
    }


}