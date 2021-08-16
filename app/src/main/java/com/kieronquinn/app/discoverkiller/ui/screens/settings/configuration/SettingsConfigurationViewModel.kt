package com.kieronquinn.app.discoverkiller.ui.screens.settings.configuration

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.navigation.Navigation
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.ui.activities.SettingsBackgroundPickerActivity
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsViewModel
import com.kieronquinn.app.discoverkiller.utils.Links
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class SettingsConfigurationViewModel : ViewModel() {

    abstract val overlayEnabled: Flow<Boolean>
    abstract val overlay: Flow<Settings.OverlayMode>
    abstract val overlayBackgroundChanged: Flow<Unit>

    abstract fun setOverlay(reloadCall: () -> Unit, overlay: Settings.OverlayMode)
    abstract fun toggleOverlayEnabled(reloadCall: () -> Unit, reload: Boolean = true)

    abstract fun onChooseAppClicked()
    abstract fun getChooseAppSubtitle(context: Context): CharSequence
    abstract fun setSelectedApp(packageName: String)

    abstract fun onMonetColorPickerClicked()

    abstract fun onOverlayBackgroundClicked(
        context: Context,
        launcher: ActivityResultLauncher<Intent>
    )

    abstract fun onOverlayBackgroundSelected(
        settingsViewModel: SettingsViewModel,
        splashScreenType: RemoteSplashLoader.SplashScreenType
    )

    abstract fun onGitHubClicked()
    abstract fun onDonateClicked()
    abstract fun onTwitterClicked()
    abstract fun onXDAClicked()
    abstract fun onLibrariesClicked(context: Context)

}

class SettingsConfigurationViewModelImpl(
    private val settings: Settings,
    private val navigation: Navigation
) : SettingsConfigurationViewModel() {

    private val _overlayEnabled = MutableStateFlow(settings.overlayEnabled)
    override val overlayEnabled = _overlayEnabled.asStateFlow()

    private val _overlay = MutableStateFlow(settings.overlayMode)
    override val overlay = _overlay.asStateFlow()

    private val _overlayBackgroundChanged = MutableSharedFlow<Unit>()
    override val overlayBackgroundChanged = _overlayBackgroundChanged.asSharedFlow()

    override fun setOverlay(reloadCall: () -> Unit, overlay: Settings.OverlayMode) {
        viewModelScope.launch {
            settings.overlayMode = overlay
            _overlay.emit(overlay)
            //Enable overlay if it is disabled
            if (!overlayEnabled.value) {
                toggleOverlayEnabled(reloadCall, false)
            }
            reloadCall.invoke()
        }
    }

    override fun toggleOverlayEnabled(reloadCall: () -> Unit, reload: Boolean) {
        viewModelScope.launch {
            val enabled = !settings.overlayEnabled
            settings.overlayEnabled = enabled
            _overlayEnabled.emit(enabled)
            if (reload) {
                reloadCall.invoke()
            }
        }
    }

    override fun onChooseAppClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsConfigurationFragmentDirections.actionSettingsConfigurationFragmentToSettingsAppPickerFragment())
        }
    }

    override fun onOverlayBackgroundClicked(
        context: Context,
        launcher: ActivityResultLauncher<Intent>
    ) {
        launcher.launch(
            Intent(context, SettingsBackgroundPickerActivity::class.java),
            ActivityOptionsCompat.makeCustomAnimation(
                context,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        )
    }

    override fun onOverlayBackgroundSelected(
        settingsViewModel: SettingsViewModel,
        splashScreenType: RemoteSplashLoader.SplashScreenType
    ) {
        viewModelScope.launch {
            settings.overlayBackground = splashScreenType
            _overlayBackgroundChanged.emit(Unit)
            settingsViewModel.reloadOverlay()
        }
    }

    override fun getChooseAppSubtitle(context: Context): CharSequence {
        return if (settings.overlayApp.isNotEmpty()) {
            val selectedApp = try {
                context.packageManager.getApplicationInfo(settings.overlayApp, 0)
                    .loadLabel(context.packageManager)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            if (selectedApp != null) {
                context.getString(R.string.configuration_app_package_desc_selected, selectedApp)
            } else {
                context.getString(R.string.configuration_app_package_desc)
            }
        } else {
            context.getString(R.string.configuration_app_package_desc)
        }
    }

    override fun onMonetColorPickerClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsConfigurationFragmentDirections.actionSettingsConfigurationFragmentToColorPickerBottomSheetFragment())
        }
    }

    override fun setSelectedApp(packageName: String) {
        settings.overlayApp = packageName
    }

    override fun onGitHubClicked() {
        viewModelScope.launch {
            navigation.navigate(Links.createLinkIntent(Links.LINK_GITHUB))
        }
    }

    override fun onDonateClicked() {
        viewModelScope.launch {
            navigation.navigate(Links.createLinkIntent(Links.LINK_DONATE))
        }
    }

    override fun onTwitterClicked() {
        viewModelScope.launch {
            navigation.navigate(Links.createLinkIntent(Links.LINK_TWITTER))
        }
    }

    override fun onXDAClicked() {
        viewModelScope.launch {
            navigation.navigate(Links.createLinkIntent(Links.LINK_XDA))
        }
    }

    override fun onLibrariesClicked(context: Context) {
        viewModelScope.launch {
            OssLicensesMenuActivity.setActivityTitle(context.getString(R.string.libraries))
            navigation.navigate(Intent(context, OssLicensesMenuActivity::class.java))
        }
    }

}