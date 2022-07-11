package com.kieronquinn.app.discoverkiller.ui.screens.settings

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.model.settings.BaseSettingsItem
import com.kieronquinn.app.discoverkiller.model.settings.GenericSettingsItem
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayMode
import com.kieronquinn.app.discoverkiller.repositories.UpdateRepository
import com.kieronquinn.app.discoverkiller.ui.base.ProvidesOverflow
import com.kieronquinn.app.discoverkiller.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.SettingsViewModel.SettingsSettingsItem
import com.kieronquinn.app.discoverkiller.ui.screens.settings.SettingsViewModel.State
import com.kieronquinn.app.discoverkiller.utils.extensions.setTypeface
import com.kieronquinn.monetcompat.extensions.applyMonet
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment: BaseSettingsFragment(), ProvidesOverflow {

    override val addAdditionalPadding = true

    override val adapter by lazy {
        SettingsAdapter(binding.settingsBaseRecyclerView, remoteSplashLoader, emptyList())
    }

    private val updateSnackbar by lazy {
        Snackbar.make(requireView(), R.string.snackbar_update, Snackbar.LENGTH_INDEFINITE).apply {
            applyMonet()
            setTypeface(ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium))
            setAction(R.string.snackbar_update_button){
                viewModel.onUpdateClicked(requireContext())
            }
        }
    }

    private val viewModel by viewModel<SettingsViewModel>()
    private val remoteSplashLoader by inject<RemoteSplashLoader>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupUpdateSnackbar()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(loadItems(state), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val switch = GenericSettingsItem.Switch(
            state.isEnabled,
            getString(R.string.settings_main_switch),
            viewModel::onMainChanged
        )
        val about = SettingsSettingsItem.About(
            viewModel::onContributorsClicked,
            viewModel::onDonateClicked,
            viewModel::onGitHubClicked,
            viewModel::onTwitterClicked,
            viewModel::onXdaClicked,
            viewModel::onLibrariesClicked
        )
        if(!state.isEnabled) return listOf(switch, about)
        val header = SettingsSettingsItem.Header(
            state.overlayMode,
            state.overlayComponent,
            state.overlayAppPackage,
            state.overlayBackground,
            viewModel::onOverlayModeClicked
        )
        val overlay = if(state.overlayMode == OverlayMode.OVERLAY){
            GenericSettingsItem.Setting(
                getString(R.string.settings_overlay_picker),
                state.customOverlayName,
                R.drawable.ic_overlay,
                viewModel::onCustomOverlayClicked
            )
        }else null
        val useOriginal = if(state.overlayMode == OverlayMode.OVERLAY) {
            GenericSettingsItem.SwitchSetting(
                state.useOriginal,
                getString(R.string.settings_original_handler),
                getString(R.string.settings_original_handler_content),
                R.drawable.ic_search,
                onChanged = viewModel::onUseOriginalChanged
            )
        }else null
        val app = if(state.overlayMode == OverlayMode.APP) {
            GenericSettingsItem.Setting(
                getString(R.string.settings_app_picker),
                state.overlayAppLabel,
                R.drawable.ic_overlay,
                viewModel::onChooseAppClicked
            )
        }else null
        val appNewTask = if(state.overlayMode == OverlayMode.APP) {
            GenericSettingsItem.SwitchSetting(
                state.appNewTask,
                getString(R.string.settings_app_new_task),
                getString(R.string.settings_app_new_task_content),
                R.drawable.ic_app_new_task,
                onChanged = viewModel::onAppNewTaskChanged
            )
        }else null
        val restart = if(state.overlaySupportsRestart && state.overlayMode == OverlayMode.OVERLAY) {
            GenericSettingsItem.SwitchSetting(
                state.restartEnabled,
                getString(R.string.settings_restart),
                getText(R.string.settings_restart_content),
                R.drawable.ic_overlay_restart,
                onChanged = viewModel::onRestartChanged
            )
        }else null
        val monet = if(state.shouldShowMonet) {
            GenericSettingsItem.SwitchSetting(
                state.monetEnabled,
                getString(R.string.settings_monet),
                getString(R.string.settings_monet_content),
                R.drawable.ic_monet,
                onChanged = viewModel::onMonetChanged
            )
        }else null
        val splash = if(state.overlayMode == OverlayMode.APP) {
            GenericSettingsItem.Setting(
                getString(R.string.configuration_app_monet),
                getString(R.string.configuration_app_monet_desc),
                R.drawable.ic_splash_picker,
                viewModel::onSplashClicked
            )
        }else null
        return listOfNotNull(switch, header, overlay, useOriginal, app, appNewTask, splash, restart, monet, about)
    }

    private fun setupUpdateSnackbar() {
        handleUpdateSnackbar(viewModel.updateState.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.updateState.collect {
                handleUpdateSnackbar(it)
            }
        }
    }

    private fun handleUpdateSnackbar(updateState: UpdateRepository.UpdateState?) {
        if (updateState is UpdateRepository.UpdateState.UpdateAvailable) {
            updateSnackbar.show()
        } else {
            updateSnackbar.dismiss()
        }
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_main, menu)
        //Only show wallpaper colour picker option on compat versions
        menu.findItem(R.id.menu_wallpaper_colour_picker).isVisible =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.menu_restart_overlay -> viewModel.onRestartOverlayClicked(requireContext())
            R.id.menu_wallpaper_colour_picker -> viewModel.onWallpaperColourPickerClicked()
        }
        return true
    }

}