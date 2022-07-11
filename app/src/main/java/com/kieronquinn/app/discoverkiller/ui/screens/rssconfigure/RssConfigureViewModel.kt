package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.RssNavigation
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class RssConfigureViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    sealed class State {
        object Loading: State()
        data class Loaded(
            val rssUrl: String,
            val rssTitleEnabled: Boolean,
            val rssTitle: String,
            val rssLogoUrl: String
        ): State()
    }

    abstract fun onRssUrlClicked()
    abstract fun onRssTitleClicked()
    abstract fun onRssLogoClicked()

}

class RssConfigureViewModelImpl(
    settings: SettingsRepository,
    private val navigation: RssNavigation
): RssConfigureViewModel() {

    override val state = combine(
        settings.rssUrl.asFlow(),
        settings.rssTitle.asFlow(),
        settings.rssLogoUrl.asFlow()
    ) { url, title, logo ->
        val titleEnabled = logo.isEmpty()
        State.Loaded(url, titleEnabled, title, logo)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onRssUrlClicked() {
        viewModelScope.launch {
            navigation.navigate(RssConfigureFragmentDirections.actionRssConfigureFragmentToRssUrlBottomSheetFragment())
        }
    }

    override fun onRssTitleClicked() {
        viewModelScope.launch {
            navigation.navigate(RssConfigureFragmentDirections.actionRssConfigureFragmentToRssTitleBottomSheetFragment())
        }
    }

    override fun onRssLogoClicked() {
        viewModelScope.launch {
            navigation.navigate(RssConfigureFragmentDirections.actionRssConfigureFragmentToRssLogoBottomSheetFragment())
        }
    }

}