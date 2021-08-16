package com.kieronquinn.app.discoverkiller.ui.screens.settings.container

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.launcherclient.LauncherClientCallbacks
import com.kieronquinn.app.discoverkiller.components.xposed.apps.GoogleApp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

abstract class SettingsViewModel: ViewModel(), LauncherClientCallbacks {

    abstract val scroll: Flow<Float>
    abstract val showOverlayBus: Flow<Unit>
    abstract val reconnectOverlayBus: Flow<Unit>
    abstract val overlayLoadState: Flow<OverlayLoadState>
    abstract val snackbarShowing: Flow<Boolean>

    abstract fun showOverlay()
    abstract fun reloadOverlay()
    abstract fun reconnectOverlay()

    enum class OverlayLoadState {
        IDLE, RUNNING, RESTART, WAITING_FOR_RESTART, TIMEOUT
    }

}

class SettingsViewModelImpl : SettingsViewModel(){

    private val _scroll = MutableStateFlow(0f)
    override val scroll = _scroll.asStateFlow()

    private val _overlayLoadState = MutableStateFlow(OverlayLoadState.IDLE).apply {
        viewModelScope.launch {
            launch {
                debounce(200).collect {
                    handleOverlayState(it)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            restartGsa()
            delay(1000)
            reconnectOverlay()
        }
    }

    override val overlayLoadState = _overlayLoadState.asStateFlow()

    private val _showOverlayBus = MutableSharedFlow<Unit>()
    override val showOverlayBus = _showOverlayBus.asSharedFlow()

    private val _reconnectOverlayBus = MutableSharedFlow<Unit>()
    override val reconnectOverlayBus = _reconnectOverlayBus.asSharedFlow()

    override val snackbarShowing = overlayLoadState.map {
        it == OverlayLoadState.WAITING_FOR_RESTART || it == OverlayLoadState.TIMEOUT
    }

    override fun onOverlayScrollChanged(progress: Float) {
        viewModelScope.launch {
            _scroll.emit(progress)
        }
    }

    override fun onServiceStateChanged(overlayAttached: Boolean, hotwordActive: Boolean) {
        Log.d("DrawerOverlayClient", "onServiceStateChanged $overlayAttached")
        viewModelScope.launch {
            if(overlayAttached) {
                _overlayLoadState.emit(OverlayLoadState.RUNNING)
            }
        }
    }

    override fun showOverlay() {
        viewModelScope.launch {
            _showOverlayBus.emit(Unit)
        }
    }

    override fun reloadOverlay() {
        viewModelScope.launch {
            _overlayLoadState.emit(OverlayLoadState.RESTART)
        }
    }

    override fun reconnectOverlay() {
        viewModelScope.launch {
            _reconnectOverlayBus.emit(Unit)
        }
    }

    private var reloadTimeoutJob: Job? = null
    private suspend fun handleOverlayState(state: OverlayLoadState){
        when (state) {
            OverlayLoadState.RESTART -> {
                restartGsa()
                _overlayLoadState.emit(OverlayLoadState.WAITING_FOR_RESTART)
                reloadTimeoutJob = viewModelScope.launch {
                    delay(5000)
                    _overlayLoadState.emit(OverlayLoadState.TIMEOUT)
                }
            }
            OverlayLoadState.RUNNING -> {
                reloadTimeoutJob?.cancel()
                reloadTimeoutJob = null
            }
        }
    }

    private suspend fun restartGsa(){
        withContext(Dispatchers.IO) {
            Shell.su("am force-stop ${GoogleApp.PACKAGE_NAME}").exec()
        }
    }

}