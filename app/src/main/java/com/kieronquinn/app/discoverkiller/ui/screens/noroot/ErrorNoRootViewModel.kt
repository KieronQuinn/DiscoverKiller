package com.kieronquinn.app.discoverkiller.ui.screens.noroot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.RootNavigation
import kotlinx.coroutines.launch

abstract class ErrorNoRootViewModel: ViewModel() {

    abstract fun onRetryClicked()

}

class ErrorNoRootViewModelImpl(
    private val navigation: RootNavigation
): ErrorNoRootViewModel() {

    override fun onRetryClicked() {
        viewModelScope.launch {
            navigation.phoenix()
        }
    }

}