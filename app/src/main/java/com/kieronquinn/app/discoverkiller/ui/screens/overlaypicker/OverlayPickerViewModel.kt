package com.kieronquinn.app.discoverkiller.ui.screens.overlaypicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.ContainerNavigation
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository.Overlay
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class OverlayPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val searchShowClear: StateFlow<Boolean>
    abstract val searchText: StateFlow<CharSequence>

    abstract fun onOverlayClicked(overlay: Overlay)
    abstract fun setSearchText(text: CharSequence)

    sealed class State {
        object Loading: State()
        data class Loaded(val overlays: List<Overlay>): State()
    }

}

class OverlayPickerViewModelImpl(
    overlayRepository: OverlayRepository,
    private val navigation: ContainerNavigation,
    private val settings: SettingsRepository
): OverlayPickerViewModel() {

    private val _searchText = MutableStateFlow("")
    override val searchText = _searchText.asStateFlow()

    private val overlays = flow {
        emit(overlayRepository.getOverlays())
    }

    override val state = combine(overlays, searchText) { list, text ->
        list.filter {
            it.label.toString().lowercase().contains(text.lowercase())
        }
    }.map { State.Loaded(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val searchShowClear = searchText.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onOverlayClicked(overlay: Overlay) {
        viewModelScope.launch {
            val overlayType = when(overlay.overlayType){
                OverlayRepository.OverlayType.NOW -> SettingsRepository.OverlayType.NOW
                OverlayRepository.OverlayType.MEDIA -> SettingsRepository.OverlayType.MEDIA
                OverlayRepository.OverlayType.RSS -> SettingsRepository.OverlayType.NOW
            }
            settings.overlayType.set(overlayType)
            settings.overlayComponent.set(overlay.overlayComponent.flattenToString())
            navigation.navigateBack()
        }
    }

    override fun setSearchText(text: CharSequence) {
        viewModelScope.launch {
            _searchText.emit(text.toString())
        }
    }

}