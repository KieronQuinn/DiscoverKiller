package com.kieronquinn.app.discoverkiller.ui.screens.container

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.ContainerNavigation
import com.kieronquinn.app.discoverkiller.ui.base.BaseContainerViewModel
import kotlinx.coroutines.launch

abstract class ContainerViewModel: ViewModel(), BaseContainerViewModel {
}

class ContainerViewModelImpl(
    private val navigation: ContainerNavigation
): ContainerViewModel() {

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    //No parent
    override fun onParentBackPressed(): Boolean = false

}