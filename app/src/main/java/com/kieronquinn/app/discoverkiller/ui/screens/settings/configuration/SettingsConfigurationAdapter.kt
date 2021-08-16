package com.kieronquinn.app.discoverkiller.ui.screens.settings.configuration

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.tabs.TabLayout
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.databinding.ItemConfigurationAboutBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemConfigurationOverlayPickerBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemConfigurationSettingsBinding
import com.kieronquinn.app.discoverkiller.model.ConfigurationItem
import com.kieronquinn.app.discoverkiller.model.ConfigurationItemType
import com.kieronquinn.app.discoverkiller.utils.extensions.isAppInstalled
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.toArgb

class SettingsConfigurationAdapter(
    context: Context,
    private val lifecycle: LifecycleCoroutineScope,
    private val splashLoader: RemoteSplashLoader,
    private val settings: Settings,
    val viewModel: SettingsConfigurationViewModel,
    var items: List<ConfigurationItem>,
    private val onOverlayChanged: (Int) -> Unit,
    private val reloadListener: () -> Unit
): RecyclerView.Adapter<SettingsConfigurationAdapter.ViewHolder>(),
    TabLayout.OnTabSelectedListener {

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].itemType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(ConfigurationItemType.values()[viewType]){
            ConfigurationItemType.PICKER -> ViewHolder.Picker(
                ItemConfigurationOverlayPickerBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            ConfigurationItemType.SNAPSHOT_SETTINGS -> ViewHolder.SnapshotSettings(
                ItemConfigurationSettingsBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            ConfigurationItemType.APP_SETTINGS -> ViewHolder.AppSettings(
                ItemConfigurationSettingsBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            ConfigurationItemType.ABOUT -> ViewHolder.About(
                ItemConfigurationAboutBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when(holder){
            is ViewHolder.Picker -> holder.binding.setupPicker()
            is ViewHolder.SnapshotSettings -> holder.binding.setupSnapshotSettings(item as ConfigurationItem.SnapshotSettings)
            is ViewHolder.AppSettings -> holder.binding.setupAppSettings(item as ConfigurationItem.AppSettings)
            is ViewHolder.About -> holder.binding.setupAbout()
        }
    }

    private fun ItemConfigurationOverlayPickerBinding.setupPicker(){
        val backgroundColor = monet.getBackgroundColor(root.context)
        val tabBackground = monet.getMonetColors().accent1[600]?.toArgb() ?: monet.getAccentColor(root.context, false)
        root.backgroundTintList = ColorStateList.valueOf(monet.getBackgroundColorSecondary(root.context) ?: backgroundColor)
        itemConfigurationOverlaySnapshot.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        itemConfigurationOverlayAppContainer.root.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        itemConfigurationOverlayAppContainer.root.clipToOutline = true
        itemConfigurationOverlayTabs.backgroundTintList = ColorStateList.valueOf(tabBackground)
        itemConfigurationOverlayTabs.setSelectedTabIndicatorColor(monet.getAccentColor(root.context))
        itemConfigurationOverlayTabs.selectTab(settings.overlayMode.ordinal)
        itemConfigurationOverlayTabs.addOnTabSelectedListener(this@SettingsConfigurationAdapter)
        itemConfigurationOverlaySnapshot.setOnClickListener {
            itemConfigurationOverlayTabs.selectTab(0)
        }
        itemConfigurationOverlayApp.setOnClickListener {
            itemConfigurationOverlayTabs.selectTab(1)
        }
        //Set up splash preview
        if(settings.overlayApp.isNotEmpty() && root.context.packageManager.isAppInstalled(settings.overlayApp)) {
            lifecycle.launchWhenResumed {
                itemConfigurationOverlayAppContainer.overlayPreviewAppDefault.setImageBitmap(renderSplashPreview())
                itemConfigurationOverlayAppContainer.overlayPreviewAppDefaultIcon.isVisible = false
            }
        }
    }

    private suspend fun ItemConfigurationOverlayPickerBinding.renderSplashPreview(): Bitmap {
        return splashLoader.inflateSplashScreenIntoBitmap(root.context, settings.overlayBackground, settings.overlayApp)
    }

    private fun ItemConfigurationSettingsBinding.setupSnapshotSettings(snapshotSettings: ConfigurationItem.SnapshotSettings) {
        with(root){
            layoutManager = LinearLayoutManager(context)
            adapter = SettingsConfigurationInnerAdapter(context, snapshotSettings.settingsItems, reloadListener)
        }
    }

    private fun ItemConfigurationSettingsBinding.setupAppSettings(appSettings: ConfigurationItem.AppSettings) {
        with(root){
            layoutManager = LinearLayoutManager(context)
            adapter = SettingsConfigurationInnerAdapter(context, appSettings.settingsItems, reloadListener)
        }
    }

    private fun ItemConfigurationAboutBinding.setupAbout(){
        with(root){
            backgroundTintList = ColorStateList.valueOf(monet.getBackgroundColorSecondary(context) ?: monet.getBackgroundColor(context))
            itemConfigurationAboutSubtitle.text = context.getString(R.string.item_configuration_about_subtitle, BuildConfig.VERSION_NAME)
            val chips = arrayOf(aboutChipGithub, aboutChipDonate, aboutChipTwitter, aboutChipXda)
            chips.forEach {
                it.chipBackgroundColor = ColorStateList.valueOf(monet.getAccentColor(context))
            }
            aboutChipGithub.setOnClickListener {
                viewModel.onGitHubClicked()
            }
            aboutChipDonate.setOnClickListener {
                viewModel.onDonateClicked()
            }
            aboutChipTwitter.setOnClickListener {
                viewModel.onTwitterClicked()
            }
            aboutChipXda.setOnClickListener {
                viewModel.onXDAClicked()
            }
        }
    }

    private fun TabLayout.selectTab(position: Int){
        selectTab(getTabAt(position))
    }

    sealed class ViewHolder(open val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        data class Picker(override val binding: ItemConfigurationOverlayPickerBinding): ViewHolder(binding)
        data class SnapshotSettings(override val binding: ItemConfigurationSettingsBinding): ViewHolder(binding)
        data class AppSettings(override val binding: ItemConfigurationSettingsBinding): ViewHolder(binding)
        data class About(override val binding: ItemConfigurationAboutBinding): ViewHolder(binding)
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        onOverlayChanged.invoke(tab.position)
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {}

    override fun onTabReselected(tab: TabLayout.Tab?) {}

}