package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.RssNavigation
import com.kieronquinn.app.discoverkiller.repositories.BaseSettingsRepository
import com.kieronquinn.app.discoverkiller.repositories.BaseSettingsRepository.DiscoverKillerSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class BaseRssConfigureBottomSheetViewModel: ViewModel() {

    abstract val setting: DiscoverKillerSetting<String>

    abstract val value: MutableStateFlow<String>

    abstract fun onTextChanged(value: String)
    abstract fun onPositiveClicked()
    abstract fun onNegativeClicked()

    open fun onNeutralClicked() {
        //No-op by default
    }

}

abstract class BaseRssConfigureBottomSheetViewModelImpl(
    private val navigation: RssNavigation
): BaseRssConfigureBottomSheetViewModel() {

    override val value by lazy {
        MutableStateFlow(setting.getSync())
    }

    override fun onTextChanged(value: String) {
        viewModelScope.launch {
            this@BaseRssConfigureBottomSheetViewModelImpl.value.emit(value)
        }
    }

    override fun onPositiveClicked() {
        viewModelScope.launch {
            setting.set(value.value.trim())
            navigation.navigateBack()
        }
    }

    override fun onNegativeClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}