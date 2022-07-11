package com.kieronquinn.app.discoverkiller.ui.screens.settings

import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader.OverlayPreview
import com.kieronquinn.app.discoverkiller.databinding.IncludeOverlayPreviewBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemSettingsAboutBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemSettingsHeaderBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemSettingsOverlayTypeBinding
import com.kieronquinn.app.discoverkiller.model.settings.BaseSettingsItem
import com.kieronquinn.app.discoverkiller.model.settings.BaseSettingsItemType
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayMode
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayType
import com.kieronquinn.app.discoverkiller.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.unset.UnsetOverlay
import com.kieronquinn.app.discoverkiller.ui.screens.settings.SettingsViewModel.SettingsSettingsItem
import com.kieronquinn.app.discoverkiller.ui.screens.settings.SettingsViewModel.SettingsSettingsItem.ItemType
import com.kieronquinn.app.discoverkiller.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.discoverkiller.utils.extensions.*
import com.kieronquinn.app.discoverkiller.utils.picasso.PackageItemInfoRequestHandler
import com.kieronquinn.monetcompat.extensions.toArgb
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collect
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    private val splashLoader: RemoteSplashLoader,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items), KoinComponent {

    private val picasso by inject<Picasso>()

    private val chipBackground by lazy {
        ColorStateList.valueOf(monet.getPrimaryColor(recyclerView.context))
    }

    private val googleSansTextMedium by lazy {
        ResourcesCompat.getFont(recyclerView.context, R.font.google_sans_text_medium)
    }

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<ItemType>(viewType) ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        return when(itemType){
            ItemType.HEADER -> SettingsViewHolder.Header(
                ItemSettingsOverlayTypeBinding.inflate(layoutInflater, parent, false)
            )
            ItemType.ABOUT -> SettingsViewHolder.About(
                ItemSettingsAboutBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is SettingsViewHolder.Header -> {
                val item = items[position] as SettingsSettingsItem.Header
                holder.setup(item)
            }
            is SettingsViewHolder.About -> {
                val item = items[position] as SettingsSettingsItem.About
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun SettingsViewHolder.Header.setup(
        header: SettingsSettingsItem.Header
    ) = with(binding) {
        val overlayCustom = header.overlayComponent
        val backgroundColor = monet.getBackgroundColor(root.context)
        val tabBackground = monet.getMonetColors().accent1[600]?.toArgb()
            ?: monet.getAccentColor(root.context, false)
        root.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        )
        itemSettingsOverlayAppContainer.root.backgroundTintList =
            ColorStateList.valueOf(backgroundColor)
        itemSettingsOverlayAppContainer.root.clipToOutline = true
        itemSettingsOverlayCustomContainer.root.backgroundTintList =
            ColorStateList.valueOf(backgroundColor)
        itemSettingsOverlayCustomContainer.root.clipToOutline = true
        itemSettingsOverlayTabs.backgroundTintList = ColorStateList.valueOf(tabBackground)
        itemSettingsOverlayTabs.setSelectedTabIndicatorColor(monet.getAccentColor(root.context))
        itemSettingsOverlayTabs.selectTab(header.overlayMode.ordinal)
        lifecycleScope.launchWhenResumed {
            itemSettingsOverlayTabs.onSelected().collect {
                header.onOverlayModeChanged(OverlayMode.values()[it])
            }
        }
        lifecycleScope.launchWhenResumed {
            itemSettingsOverlayCustom.onClicked().collect {
                itemSettingsOverlayTabs.selectTab(0)
            }
        }
        lifecycleScope.launchWhenResumed {
            itemSettingsOverlayApp.onClicked().collect {
                itemSettingsOverlayTabs.selectTab(1)
            }
        }
        //Set up splash preview
        lifecycle.coroutineScope.launchWhenResumed {
            itemSettingsOverlayAppContainer.overlayPreviewDefault.setImageBitmap(
                renderSplashPreview(header.overlayBackground, header.overlayAppComponent)
            )
            itemSettingsOverlayAppContainer.overlayPreviewDefaultIcon.isVisible = false
        }
        //Set up overlay preview
        lifecycle.coroutineScope.launchWhenResumed {
            itemSettingsOverlayCustomContainer.renderOverlayPreview(overlayCustom)
        }
    }

    private fun SettingsViewHolder.About.setup(about: SettingsSettingsItem.About) = with(binding) {
        val context = root.context
        val content = context.getString(R.string.about_version, BuildConfig.VERSION_NAME)
        itemUpdatesAboutContent.text = content
        root.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        )
        mapOf(
            itemUpdatesAboutContributors to about.onContributorsClicked,
            itemUpdatesAboutDonate to about.onDonateClicked,
            itemUpdatesAboutGithub to about.onGitHubClicked,
            itemUpdatesAboutLibraries to about.onLibrariesClicked,
            itemUpdatesAboutTwitter to about.onTwitterClicked,
            itemUpdatesAboutXda to about.onXdaClicked
        ).forEach { chip ->
            with(chip.key){
                chipBackgroundColor = chipBackground
                typeface = googleSansTextMedium
                lifecycleScope.launchWhenResumed {
                    onClicked().collect {
                        chip.value()
                    }
                }
            }
        }
    }

    private suspend fun ItemSettingsOverlayTypeBinding.renderSplashPreview(
        overlayBackground: RemoteSplashLoader.SplashScreenType,
        overlayAppComponent: String
    ): Bitmap {
        return splashLoader.inflateSplashScreenIntoBitmap(root.context, overlayBackground, overlayAppComponent)
    }

    private suspend fun IncludeOverlayPreviewBinding.renderOverlayPreview(
        overlayComponent: ComponentName
    ) {
        when(val icon = splashLoader.getOverlayPreview(root.context, overlayComponent)){
            is OverlayPreview.Preview -> {
                overlayPreviewDefaultIcon.isVisible = false
                overlayPreviewContainer.isVisible = false
                overlayPreviewFull.isVisible = true
                overlayPreviewFull.setImageResource(icon.resource)
            }
            is OverlayPreview.Icon -> {
                overlayPreviewDefaultIcon.isVisible = true
                overlayPreviewContainer.isVisible = false
                overlayPreviewFull.isVisible = false
                picasso.load(PackageItemInfoRequestHandler.getUriFor(icon.info.toComponent()))
                    .into(overlayPreviewDefaultIcon)
            }
        }
    }

    sealed class SettingsViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemSettingsOverlayTypeBinding): SettingsViewHolder(binding)
        data class About(override val binding: ItemSettingsAboutBinding): SettingsViewHolder(binding)
    }

}