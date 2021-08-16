package com.kieronquinn.app.discoverkiller.ui.screens.settings.apppicker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.model.AppPickerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

abstract class SettingsAppPickerViewModel : ViewModel() {

    abstract val loadState: Flow<LoadState>
    abstract val showSearchClearButton: Flow<Boolean>

    abstract fun setSearchTerm(searchTerm: String)
    abstract fun getSearchTerm(): String

    sealed class LoadState {
        object Loading : LoadState()
        data class Loaded(val apps: List<AppPickerItem.App>) : LoadState()
    }

}

class SettingsAppPickerViewModelImpl(
    context: Context
) : SettingsAppPickerViewModel() {

    private val packageManager by lazy {
        context.packageManager
    }

    private val allApps = MutableSharedFlow<List<InstalledApp>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        viewModelScope.launch(Dispatchers.IO) {
            emit(packageManager.getInstalledApplications(0).mapNotNull {
                InstalledApp(it.packageName, it.loadLabel(packageManager))
            }.filter {
                packageManager.getLaunchIntentForPackage(it.packageName) != null
            })
        }
    }

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    override val loadState = _loadState.asStateFlow().apply {
        viewModelScope.launch {
            delay(5)
            collect {
                loadApps()
            }
        }
    }

    private val searchTerm = MutableStateFlow("")

    override val showSearchClearButton: Flow<Boolean> = searchTerm.map { it.isNotEmpty() }

    private suspend fun loadApps() {
        _loadState.emit(LoadState.Loading)
        val apps = combine(
            allApps,
            searchTerm
        ) { apps, search ->
            withContext(Dispatchers.IO) {
                apps.map {
                    AppPickerItem.App(it.packageName, it.label)
                }
            }.sortedBy { it.label.toString().lowercase(Locale.getDefault()) }.run {
                if (search.isNotEmpty()) filter {
                    it.label.toString().lowercase(Locale.getDefault()).contains(
                        search.lowercase(
                            Locale.getDefault()
                        )
                    )
                } else this
            }
        }.first()
        _loadState.emit(LoadState.Loaded(apps))
    }

    override fun setSearchTerm(searchTerm: String) {
        viewModelScope.launch {
            this@SettingsAppPickerViewModelImpl.searchTerm.emit(searchTerm)
            loadApps()
        }
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    private data class InstalledApp(val packageName: String, val label: CharSequence)

}