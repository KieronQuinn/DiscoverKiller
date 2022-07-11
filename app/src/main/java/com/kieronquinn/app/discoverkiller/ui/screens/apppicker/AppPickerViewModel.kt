package com.kieronquinn.app.discoverkiller.ui.screens.apppicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.ContainerNavigation
import com.kieronquinn.app.discoverkiller.repositories.AppRepository
import com.kieronquinn.app.discoverkiller.repositories.AppRepository.App
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository.Overlay
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AppPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val searchShowClear: StateFlow<Boolean>
    abstract val searchText: StateFlow<CharSequence>

    abstract fun onAppClicked(app: App)
    abstract fun setSearchText(text: CharSequence)

    sealed class State {
        object Loading: State()
        data class Loaded(val apps: List<App>): State()
    }

}

class AppPickerViewModelImpl(
    appRepository: AppRepository,
    private val navigation: ContainerNavigation,
    private val settings: SettingsRepository
): AppPickerViewModel() {

    private val _searchText = MutableStateFlow("")
    override val searchText = _searchText.asStateFlow()

    private val apps = flow {
        emit(appRepository.getLaunchableApps())
    }

    override val state = combine(apps, searchText) { list, text ->
        list.filter {
            it.label.toString().lowercase().contains(text.lowercase())
        }
    }.map { State.Loaded(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val searchShowClear = searchText.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onAppClicked(app: App) {
        viewModelScope.launch {
            settings.overlayApp.set(app.componentName.flattenToString())
            navigation.navigateBack()
        }
    }

    override fun setSearchText(text: CharSequence) {
        viewModelScope.launch {
            _searchText.emit(text.toString())
        }
    }

}