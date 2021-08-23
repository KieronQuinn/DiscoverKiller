package com.kieronquinn.app.discoverkiller.ui.screens.settings.error.errorignore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.processphoenix.ProcessPhoenix
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class ErrorIgnoreBottomSheetViewModel: ViewModel() {

    abstract val continueChecked: Flow<Boolean>

    abstract fun onCheckboxClicked()
    abstract fun onContinueClicked(context: Context)

}

class ErrorIgnoreBottomSheetViewModelImpl(private val settings: Settings): ErrorIgnoreBottomSheetViewModel() {

    private val _continueChecked = MutableStateFlow(false)
    override val continueChecked = _continueChecked.asStateFlow()

    override fun onCheckboxClicked() {
        viewModelScope.launch {
            _continueChecked.emit(!_continueChecked.value)
        }
    }

    override fun onContinueClicked(context: Context) {
        viewModelScope.launch {
            settings.ignoreXposedWarnings = true
            ProcessPhoenix.triggerRebirth(context)
        }
    }

}