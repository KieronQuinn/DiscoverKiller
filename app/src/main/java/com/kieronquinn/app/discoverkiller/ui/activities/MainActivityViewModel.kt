package com.kieronquinn.app.discoverkiller.ui.activities

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.repositories.RootRepository
import com.kieronquinn.app.discoverkiller.repositories.XposedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

abstract class MainActivityViewModel: ViewModel() {

    abstract val startDestination: StateFlow<Int?>

}

class MainActivityViewModelImpl(
    private val rootRepository: RootRepository,
    private val xposedRepository: XposedRepository
): MainActivityViewModel() {

    companion object {
        private const val SPLASH_TIMEOUT = 1000L
    }

    private val splashTimeout = flow {
        delay(SPLASH_TIMEOUT)
        emit(Unit)
    }

    private val isRooted = flow {
        emit(rootRepository.isRooted())
    }

    private val isModuleHooked = flow {
        emit(xposedRepository.isModuleHooked())
    }

    override val startDestination = combine(
        splashTimeout,
        isRooted,
        isModuleHooked
    ) { _, rooted, hooked ->
        when {
            !rooted -> R.id.errorNoRootFragment
            !hooked -> R.id.errorNoXposedFragment
            else -> R.id.containerFragment
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

}