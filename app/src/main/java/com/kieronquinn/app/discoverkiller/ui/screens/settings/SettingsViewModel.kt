package com.kieronquinn.app.discoverkiller.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.navigation.ContainerNavigation
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.model.settings.BaseSettingsItem
import com.kieronquinn.app.discoverkiller.model.settings.BaseSettingsItemType
import com.kieronquinn.app.discoverkiller.model.update.Release
import com.kieronquinn.app.discoverkiller.model.update.toRelease
import com.kieronquinn.app.discoverkiller.repositories.AppRepository
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayMode
import com.kieronquinn.app.discoverkiller.repositories.UpdateRepository
import com.kieronquinn.app.discoverkiller.repositories.UpdateRepository.UpdateState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class SettingsViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val updateState: StateFlow<UpdateState?>

    abstract fun onMainChanged(enabled: Boolean)
    abstract fun onOverlayModeClicked(overlayMode: OverlayMode)
    abstract fun onCustomOverlayClicked()
    abstract fun onChooseAppClicked()
    abstract fun onAppNewTaskChanged(enabled: Boolean)
    abstract fun onRestartChanged(enabled: Boolean)
    abstract fun onMonetChanged(enabled: Boolean)
    abstract fun onRestartOverlayClicked(context: Context)
    abstract fun onWallpaperColourPickerClicked()
    abstract fun onUseOriginalChanged(enabled: Boolean)
    abstract fun onSplashClicked()
    abstract fun onUpdateClicked(context: Context)

    abstract fun onContributorsClicked()
    abstract fun onDonateClicked()
    abstract fun onGitHubClicked()
    abstract fun onLibrariesClicked()
    abstract fun onTwitterClicked()
    abstract fun onXdaClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val isEnabled: Boolean,
            val overlayMode: OverlayMode,
            val overlayComponent: ComponentName,
            val overlayAppPackage: String,
            val overlayAppLabel: CharSequence,
            val overlayBackground: RemoteSplashLoader.SplashScreenType,
            val customOverlayName: CharSequence,
            val shouldShowMonet: Boolean,
            val monetEnabled: Boolean,
            val overlaySupportsRestart: Boolean,
            val restartEnabled: Boolean,
            val appNewTask: Boolean,
            val useOriginal: Boolean
        ): State()
    }

    sealed class SettingsSettingsItem(type: ItemType): BaseSettingsItem(type) {

        data class Header(
            val overlayMode: OverlayMode,
            val overlayComponent: ComponentName,
            val overlayAppComponent: String,
            val overlayBackground: RemoteSplashLoader.SplashScreenType,
            val onOverlayModeChanged: (OverlayMode) -> Unit,
        ): SettingsSettingsItem(ItemType.HEADER) {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }

        data class About(
            val onContributorsClicked: () -> Unit,
            val onDonateClicked: () -> Unit,
            val onGitHubClicked: () -> Unit,
            val onTwitterClicked: () -> Unit,
            val onXdaClicked: () -> Unit,
            val onLibrariesClicked: () -> Unit
        ): SettingsSettingsItem(ItemType.ABOUT)

        enum class ItemType: BaseSettingsItemType {
            HEADER, ABOUT
        }

    }

}

class SettingsViewModelImpl(
    private val overlayRepository: OverlayRepository,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val navigation: ContainerNavigation,
    updateRepository: UpdateRepository
): SettingsViewModel() {

    companion object {
        private const val LINK_TWITTER = "https://kieronquinn.co.uk/redirect/DiscoverKiller/twitter"
        private const val LINK_GITHUB = "https://kieronquinn.co.uk/redirect/DiscoverKiller/github"
        private const val LINK_XDA = "https://kieronquinn.co.uk/redirect/DiscoverKiller/xda"
        private const val LINK_DONATE = "https://kieronquinn.co.uk/redirect/DiscoverKiller/donate"
    }

    private val enabled = settingsRepository.enabled
    private val overlayMode = settingsRepository.overlayMode
    private val overlayComponent = settingsRepository.overlayComponent
    private val overlayAppComponent = settingsRepository.overlayApp
    private val overlayBackground = settingsRepository.overlayBackground
    private val restartEnabled = settingsRepository.entertainmentSpaceRestart
    private val appNewTask = settingsRepository.overlayAppNewTask
    private val monetEnabled = settingsRepository.useMonet
    private val useOriginal = settingsRepository.originalHandlesSearch

    private val settings = combine(
        useOriginal.asFlow(),
        restartEnabled.asFlow(),
        monetEnabled.asFlow()
    ) { useOriginal, restart, monet ->
        Triple(useOriginal, restart, monet)
    }

    private val appSettings = combine(
        enabled.asFlow(),
        overlayAppComponent.asFlow(),
        appNewTask.asFlow()
    ) { enabled, component, newTask ->
        Triple(enabled, component, newTask)
    }

    override val state = combine(
        overlayMode.asFlow(),
        overlayComponent.asFlow(),
        appSettings,
        overlayBackground.asFlow(),
        settings
    ) { type, component, app, background, settings ->
        val componentName = ComponentName.unflattenFromString(component)!!
        val overlayLabel = overlayRepository.getOverlayName(componentName)
        val monetSupported = overlayRepository.doesOverlaySupportMonet(componentName) ||
                type == OverlayMode.APP
        val restartSupported = overlayRepository.doesOverlaySupportRestart(componentName)
        val appName = appRepository.getActivityName(app.second)
        val newTask = app.third
        val monetEnabled = settings.third
        val restartEnabled = settings.second
        State.Loaded(
            app.first,
            type,
            componentName,
            app.second,
            appName,
            background,
            overlayLabel,
            monetSupported,
            monetEnabled,
            restartSupported,
            restartEnabled,
            newTask,
            settings.first
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val updateState = flow {
        emit(updateRepository.getUpdateState())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override fun onMainChanged(enabled: Boolean) {
        viewModelScope.launch {
            this@SettingsViewModelImpl.enabled.set(enabled)
        }
    }

    override fun onOverlayModeClicked(overlayMode: OverlayMode) {
        viewModelScope.launch {
            settingsRepository.overlayMode.set(overlayMode)
        }
    }

    override fun onCustomOverlayClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToOverlayPickerFragment())
        }
    }

    override fun onChooseAppClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToAppPickerFragment())
        }
    }

    override fun onRestartChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.entertainmentSpaceRestart.set(enabled)
        }
    }

    override fun onMonetChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.useMonet.set(enabled)
        }
    }

    override fun onAppNewTaskChanged(enabled: Boolean) {
        viewModelScope.launch {
            appNewTask.set(enabled)
        }
    }

    override fun onRestartOverlayClicked(context: Context) {
        viewModelScope.launch {
            overlayRepository.terminateLauncher()
            Toast.makeText(
                context.applicationContext, R.string.toast_restart_overlay, Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onWallpaperColourPickerClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToWallpaperColourPickerBottomSheetFragment())
        }
    }

    override fun onSplashClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToSettingsBackgroundPickerActivity())
        }
    }

    override fun onUseOriginalChanged(enabled: Boolean) {
        viewModelScope.launch {
            useOriginal.set(enabled)
        }
    }

    override fun onUpdateClicked(context: Context) {
        val gitHubRelease = (updateState.value as? UpdateState.UpdateAvailable)?.release ?: return
        val release = gitHubRelease.toRelease(
            context.resources.getString(R.string.app_name),
            BuildConfig.TAG_NAME
        ) ?: return
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToUpdateFragment(
                release
            ))
        }
    }

    override fun onContributorsClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToContributorsFragment())
        }
    }

    override fun onDonateClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_DONATE.toIntent())
        }
    }

    override fun onGitHubClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_GITHUB.toIntent())
        }
    }

    override fun onLibrariesClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsFragmentDirections.actionSettingsFragmentToOssLicensesMenuActivity())
        }
    }

    override fun onTwitterClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_TWITTER.toIntent())
        }
    }

    override fun onXdaClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_XDA.toIntent())
        }
    }

    private fun String.toIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(this@toIntent)
        }
    }

}