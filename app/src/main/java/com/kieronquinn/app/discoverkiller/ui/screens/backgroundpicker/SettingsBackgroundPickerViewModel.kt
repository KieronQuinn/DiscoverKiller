package com.kieronquinn.app.discoverkiller.ui.screens.backgroundpicker

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader.SplashScreenType
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class SettingsBackgroundPickerViewModel: ViewModel() {

    abstract val clickEvent: Flow<Unit>

    /**
     *  Get the available splash screen options and the default splash to use when DEFAULT is set
     */
    abstract fun getSplashScreenOptions(context: Context, packageName: String): Flow<Pair<List<RemoteSplashLoader.SplashScreen>, SplashScreenType>>
    abstract fun onPageClicked()

    abstract fun onSplashTypeSelected(splashScreenType: SplashScreenType)

}

class SettingsBackgroundPickerViewModelImpl(
    private val splashLoader: RemoteSplashLoader,
    private val settingsRepository: SettingsRepository
): SettingsBackgroundPickerViewModel() {

    private val _clickEvent = MutableSharedFlow<Unit>()
    override val clickEvent = _clickEvent.asSharedFlow()

    override fun getSplashScreenOptions(
        context: Context,
        packageName: String
    ): Flow<Pair<List<RemoteSplashLoader.SplashScreen>, SplashScreenType>> = flow {
        val options = splashLoader.getRemoteSplashScreenOptions(context, packageName)
        val default = splashLoader.getDefaultSplashForPackage(context, packageName).type
        emit(Pair(options, default))
    }

    override fun onPageClicked() {
        viewModelScope.launch {
            _clickEvent.emit(Unit)
        }
    }

    override fun onSplashTypeSelected(splashScreenType: SplashScreenType) {
        viewModelScope.launch {
            settingsRepository.overlayBackground.set(splashScreenType)
        }
    }

}