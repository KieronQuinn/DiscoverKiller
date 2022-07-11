package com.kieronquinn.app.discoverkiller.utils.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Created by anandwana001 on
 * 25, January, 2019
 **/

const val OG_TITLE: String = "og:title"
const val OG_DESCRIPTION: String = "og:description"
const val OG_TYPE: String = "og:type"
const val OG_IMAGE: String = "og:image"
const val OG_URL: String = "og:url"
const val OG_SITE_NAME: String = "og:site_name"

/**
 * Main class where all logic happens.
 * This class uses jsoup which gets the url and returned all the parsed data.
 */
class OgTagParser {

    // This is the entry point of the library which gets url and the callback
    fun getContents(urlToParse: String): LinkSourceContent? {
        return execute(urlToParse)
    }

    private fun execute(urlToParse: String): LinkSourceContent? {
        var linkSourceContent: LinkSourceContent?
        runBlocking {
            linkSourceContent = doInBackground(urlToParse)
        }
        return linkSourceContent
    }

    /**
     * Using withContext as we don't need parallel execution.
     * withContext return the result of single task.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun doInBackground(urlToParse: String): LinkSourceContent? =
        withContext(Dispatchers.IO) {
            try {
                val response = Jsoup.connect(urlToParse)
                    .ignoreContentType(true)
                    .userAgent("Mozilla")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .execute()
                val doc = response.parse()
                return@withContext organize_fetched_data(doc)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }

    private fun organize_fetched_data(doc: Document): LinkSourceContent {
        val linkSourceContent = LinkSourceContent()
        val ogTags = doc.select("meta[property^=og:]")
        when {
            ogTags.size > 0 ->
                ogTags.forEachIndexed { index, _ ->
                    val tag = ogTags[index]
                    val property = tag.attr("property")
                    val content = (tag.attr("content"))
                    when (property) {
                        OG_IMAGE -> {
                            linkSourceContent.image = content
                        }
                        OG_DESCRIPTION -> {
                            linkSourceContent.ogDescription = content
                        }
                        OG_URL -> {
                            linkSourceContent.ogUrl = content
                        }
                        OG_TITLE -> {
                            linkSourceContent.ogTitle = content
                        }
                        OG_SITE_NAME -> {
                            linkSourceContent.ogSiteName = content
                        }
                        OG_TYPE -> {
                            linkSourceContent.ogType = content
                        }
                    }
                }
        }
        return linkSourceContent
    }
}

/**
 * Created by anandwana001 on
 * 26, January, 2019
 **/

/**
 * Data class required for parsing.
 */
data class LinkSourceContent(
    var ogTitle: String,
    var ogDescription: String,
    var ogUrl: String,
    var image: String,
    var ogSiteName: String,
    var ogType: String
) {
    constructor() : this("", "", "", "", "", "")
}