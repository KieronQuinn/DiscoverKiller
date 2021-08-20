package com.kieronquinn.app.discoverkiller.ui.screens.settings.dumplogs

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.navigation.Navigation
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class SettingsDumpLogsBottomSheetViewModel: ViewModel() {

    abstract val state: Flow<State>

    abstract fun onContinueClicked()
    abstract fun onCancelClicked()
    abstract fun chooseLocation(launcher: ActivityResultLauncher<String>)
    abstract fun onLocationSelected(outputUri: Uri)

    sealed class State {
        object Idle: State()
        object ChooseLocation: State()
        data class Dump(val outputUri: Uri): State()
        data class Finished(val result: Result): State()
    }

    enum class Result(@StringRes val toastMessage: Int) {
        SUCCESS(R.string.bottom_sheet_dump_logs_result_success), FAILED(R.string.bottom_sheet_dump_logs_result_failed)
    }

}

class SettingsDumpLogsBottomSheetViewModelImpl(applicationContext: Context, private val navigation: Navigation): SettingsDumpLogsBottomSheetViewModel() {

    companion object {
        private const val logDumpFilename = "discoverkiller-overlay-logs-%s.txt"
    }

    private val createOutputStream = { outputUri: Uri ->
        val file = DocumentFile.fromSingleUri(applicationContext, outputUri)
        if(file != null && file.canWrite()){
            applicationContext.contentResolver.openOutputStream(outputUri)
        }else null
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    override val state = _state.asStateFlow().apply {
        viewModelScope.launch {
            collect {
                if(it is State.Dump){
                    dumpLogs(it.outputUri)
                }
            }
        }
    }

    override fun onContinueClicked() {
        viewModelScope.launch {
            _state.emit(State.ChooseLocation)
        }
    }

    override fun onCancelClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun chooseLocation(launcher: ActivityResultLauncher<String>) {
        launcher.launch(getLogFilename())
    }

    override fun onLocationSelected(outputUri: Uri) {
        viewModelScope.launch {
            _state.emit(State.Dump(outputUri))
        }
    }

    private fun dumpLogs(outputUri: Uri) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO){
            val shellResult = Shell.sh("logcat -s -d OverlaySController").exec()
            val outputStream = createOutputStream(outputUri) ?: return@withContext Result.FAILED
            outputStream.bufferedWriter().use {
                shellResult.out.forEach { line ->
                    it.appendLine(line)
                }
            }
            Result.SUCCESS
        }
        _state.emit(State.Finished(result))
    }

    private fun getLogFilename(): String {
        return String.format(
            logDumpFilename,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
        )
    }

}