package com.kieronquinn.app.discoverkiller.repositories

import android.content.Context
import com.kieronquinn.app.discoverkiller.model.rss.RssItem
import com.prof.rssparser.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.time.Duration

interface RssRepository {

    suspend fun clearCache()
    suspend fun getRssItems(): List<RssItem>?

}

class RssRepositoryImpl(
    context: Context,
    private val settings: SettingsRepository
): RssRepository {

    private val parser = Parser.Builder()
        .context(context)
        .charset(Charset.defaultCharset())
        .cacheExpirationMillis(Duration.ofHours(1).toMillis())
        .build()

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        parser.flushCache(settings.rssUrl.get())
    }

    override suspend fun getRssItems() = withContext(Dispatchers.IO) {
        try {
            parser.getChannel(settings.rssUrl.get()).articles.mapNotNull {
                val articleUrl = it.link ?: it.sourceUrl ?: return@mapNotNull null
                val imageUrl = if (it.image.isNullOrBlank()) null else it.image
                val title = it.title ?: return@mapNotNull null
                val content = it.description ?: it.content
                RssItem(articleUrl, imageUrl, title, content)
            }
        }catch (e: Exception){
            return@withContext null
        }
    }

}