package com.kieronquinn.app.discoverkiller.ui.screens.overlays.rss

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.model.rss.RssItem
import com.kieronquinn.app.discoverkiller.repositories.RssRepository
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.activities.RssConfigureActivity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RssViewModel: ViewModel(), KoinComponent {

    private val rssRepository by inject<RssRepository>()
    private val settings by inject<SettingsRepository>()
    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    private val rssUrl = settings.rssUrl.asFlow()
    private val rssTitle = settings.rssTitle.asFlow()
    private val rssLogoUrl = settings.rssLogoUrl.asFlow()

    val state = combine(
        reloadBus,
        rssUrl,
        rssTitle,
        rssLogoUrl
    ) { _, url, _, _ ->
        if(url.isEmpty()){
            return@combine State.Error(State.ErrorType.UNSET)
        }
        val items = rssRepository.getRssItems()
        when {
            items == null -> State.Error(State.ErrorType.FAILED)
            items.isEmpty() -> State.Error(State.ErrorType.EMPTY)
            else -> State.Loaded(items)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun onRssItemClicked(context: Context, rssItem: RssItem) {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(rssItem.url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun reloadItems(clearCache: Boolean) {
        viewModelScope.launch {
            if(clearCache) rssRepository.clearCache()
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    fun onConfigureClicked(context: Context) {
        context.startActivity(Intent(context, RssConfigureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<RssItem>): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class Error(val errorType: ErrorType): State()

        enum class ErrorType(@DrawableRes val icon: Int, @StringRes val title: Int) {
            UNSET(R.drawable.ic_rss_error_unset, R.string.overlay_rss_error_unset),
            EMPTY(R.drawable.ic_rss_error_generic, R.string.overlay_rss_error_empty),
            FAILED(R.drawable.ic_rss_error_generic, R.string.overlay_rss_error_failed)
        }
    }

}