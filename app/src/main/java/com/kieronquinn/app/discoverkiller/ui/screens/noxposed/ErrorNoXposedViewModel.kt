package com.kieronquinn.app.discoverkiller.ui.screens.noxposed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.RootNavigation
import kotlinx.coroutines.launch

abstract class ErrorNoXposedViewModel: ViewModel() {

    abstract fun onRetryClicked()

}

class ErrorNoXposedViewModelImpl(
    private val navigation: RootNavigation
): ErrorNoXposedViewModel() {

    override fun onRetryClicked() {
        viewModelScope.launch {
            navigation.phoenix()
        }
    }

}