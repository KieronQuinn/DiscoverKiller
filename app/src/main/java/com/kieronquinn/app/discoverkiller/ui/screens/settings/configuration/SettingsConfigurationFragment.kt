package com.kieronquinn.app.discoverkiller.ui.screens.settings.configuration

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.databinding.FragmentSettingsConfigurationBinding
import com.kieronquinn.app.discoverkiller.model.ConfigurationItem
import com.kieronquinn.app.discoverkiller.model.SettingsItem
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsViewModel
import com.kieronquinn.app.discoverkiller.utils.extensions.onApplyInsets
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.setFragmentResultListener
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.ui.activities.SettingsBackgroundPickerActivity
import com.kieronquinn.app.discoverkiller.ui.base.ProvidesOverflow
import com.kieronquinn.app.discoverkiller.ui.screens.settings.apppicker.SettingsAppPickerFragment
import com.kieronquinn.app.discoverkiller.utils.TransitionUtils
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.monetcompat.extensions.views.enableStretchOverscroll
import kotlinx.coroutines.launch


class SettingsConfigurationFragment :
    BoundFragment<FragmentSettingsConfigurationBinding>(FragmentSettingsConfigurationBinding::inflate), ProvidesOverflow {

    private val settings by inject<Settings>()
    private val settingsViewModel by sharedViewModel<SettingsViewModel>()
    private val viewModel by viewModel<SettingsConfigurationViewModel>()
    private val overlayBackgroundChange = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it == null || it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        (it.data?.getSerializableExtra(SettingsBackgroundPickerActivity.KEY_SELECTED_SPLASH_TYPE) as? RemoteSplashLoader.SplashScreenType)?.let { splash ->
            viewModel.onOverlayBackgroundSelected(settingsViewModel, splash)
        }
    }

    private val settingsItemsSnapshot by lazy {
        listOf(
            SettingsItem.Switch (
                R.drawable.ic_snapshot_auto_reload,
                requireContext().getString(R.string.configuration_snapshot_auto_reload),
                requireContext().getString(R.string.configuration_snapshot_auto_reload_desc),
                settings::autoReloadSnapshot
            ),
            SettingsItem.Switch (
                R.drawable.ic_monet,
                requireContext().getString(R.string.configuration_snapshot_monet),
                requireContext().getString(R.string.configuration_snapshot_monet_desc),
                settings::useMonet
            ),
            SettingsItem.Action (
                R.drawable.ic_monet_color_picker,
                requireContext().getString(R.string.configuration_shared_monet_colors),
                requireContext().getString(R.string.configuration_shared_monet_colors_desc),
                viewModel::onMonetColorPickerClicked
            )
        )
    }

    private var settingsItemsApp: List<SettingsItem>? = null
        get() {
            return field ?: getSettingsItemsAppList()
        }

    private fun getSettingsItemsAppList(): List<SettingsItem> {
        return listOf(
            SettingsItem.Info(
                getString(R.string.configuration_info_app_launch)
            ),
            SettingsItem.Action (
                R.drawable.ic_app_pick_app,
                requireContext().getString(R.string.configuration_app_package),
                viewModel.getChooseAppSubtitle(requireContext()),
                viewModel::onChooseAppClicked
            ),
            SettingsItem.Switch (
                R.drawable.ic_app_new_task,
                requireContext().getString(R.string.configuration_app_new_task),
                requireContext().getString(R.string.configuration_app_new_task_desc),
                settings::overlayAppNewTask
            ),
            SettingsItem.Action (
                R.drawable.ic_monet,
                requireContext().getString(R.string.configuration_app_monet),
                requireContext().getString(R.string.configuration_app_monet_desc),
                {
                    viewModel.onOverlayBackgroundClicked(requireContext(), overlayBackgroundChange)
                }
            ) { settings.overlayApp.isNotEmpty() },
            SettingsItem.Action (
                R.drawable.ic_monet_color_picker,
                requireContext().getString(R.string.configuration_shared_monet_colors),
                requireContext().getString(R.string.configuration_shared_monet_colors_desc),
                viewModel::onMonetColorPickerClicked
            )
        )
    }

    private val configurationAdapter by lazy {
        SettingsConfigurationAdapter(requireContext(), lifecycleScope, remoteSplashLoader, settings, viewModel, listOf(ConfigurationItem.Picker), this::onOverlayChanged, this::reloadFromChange)
    }

    private val fabHeight by lazy {
        resources.getDimension(R.dimen.fab_height).toInt()
    }

    private val fabPadding by lazy {
        resources.getDimension(R.dimen.padding_16).toInt()
    }

    private val remoteSplashLoader by inject<RemoteSplashLoader>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        enterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        returnTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
        reenterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupConfigurationSettings()
        setupBackgroundChangeListener()
        setupMainSwitch()
        setupStatusNav()
        setupFragmentResult()
        lifecycleScope.launch {
            val splashOptions = remoteSplashLoader.getRemoteSplashScreenOptions(requireContext(), BuildConfig.APPLICATION_ID)
        }
    }

    private fun setupRecyclerView() = with(binding.recyclerView) {
        onApplyInsets { _, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            updatePadding(bottom = bottom + fabHeight + fabPadding + fabPadding)
        }
        enableStretchOverscroll()
        layoutManager = LinearLayoutManager(context)
        adapter = configurationAdapter
    }

    private fun setupStatusNav(){
        val window = requireActivity().window
        window.decorView.post {
            WindowInsetsControllerCompat(window, window.decorView).run {
                isAppearanceLightNavigationBars = !requireContext().isDarkMode
                isAppearanceLightStatusBars = !requireContext().isDarkMode
            }
        }
    }

    private fun setupMainSwitch(){
        binding.mainSwitch.setOnClickListener {
            viewModel.toggleOverlayEnabled(settingsViewModel::reloadOverlay)
        }
        binding.switchBackground.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        lifecycleScope.launchWhenResumed {
            viewModel.overlayEnabled.collect {
                binding.mainSwitch.isChecked = it
            }
        }
    }

    private fun setupConfigurationSettings(){
        lifecycleScope.launchWhenResumed {
            viewModel.overlay.debounce(200).collect {
                configurationAdapter.items = getConfigurationItems(it)
                configurationAdapter.notifyItemChanged(1)
            }
        }
    }

    private fun setupBackgroundChangeListener(){
        lifecycleScope.launchWhenResumed {
            viewModel.overlayBackgroundChanged.collect {
                configurationAdapter.notifyItemChanged(0)
            }
        }
    }

    private fun getConfigurationItems(overlayMode: Settings.OverlayMode): List<ConfigurationItem> {
        val secondaryItem = when(overlayMode){
            Settings.OverlayMode.SNAPSHOT -> ConfigurationItem.SnapshotSettings(settingsItemsSnapshot)
            Settings.OverlayMode.APP -> ConfigurationItem.AppSettings(settingsItemsApp!!)
        }
        return listOfNotNull(ConfigurationItem.Picker, secondaryItem, ConfigurationItem.About)
    }

    private fun onOverlayChanged(position: Int){
        viewModel.setOverlay(settingsViewModel::reloadOverlay, Settings.positionToOverlayMode(position))
    }

    private fun setupFragmentResult(){
        setFragmentResultListener(SettingsAppPickerFragment.REQUEST_APP){ _, bundle ->
            binding.recyclerView.scrollToPosition(0)
            val packageName = bundle.getString(SettingsAppPickerFragment.KEY_APP_PACKAGE) ?: return@setFragmentResultListener
            viewModel.setSelectedApp(packageName)
            settingsViewModel.reloadOverlay()
            settingsItemsApp = getSettingsItemsAppList()
            configurationAdapter.notifyItemChanged(0)
            configurationAdapter.notifyItemChanged(1)
        }
    }

    private fun reloadFromChange(){
        settingsViewModel.reloadOverlay()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.menu_main_libraries -> viewModel.onLibrariesClicked(requireContext())
            R.id.menu_main_restart_overlay -> settingsViewModel.reloadOverlay()
            R.id.menu_main_dump_overlay_logs -> viewModel.onDumpLogsClicked()
        }
        return true
    }

}