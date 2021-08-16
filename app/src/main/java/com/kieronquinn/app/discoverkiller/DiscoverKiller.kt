package com.kieronquinn.app.discoverkiller

import android.app.Application
import com.kieronquinn.app.discoverkiller.components.blur.BlurProvider
import com.kieronquinn.app.discoverkiller.components.navigation.Navigation
import com.kieronquinn.app.discoverkiller.components.navigation.NavigationImpl
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.settings.SettingsImpl
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoaderImpl
import com.kieronquinn.app.discoverkiller.ui.screens.settings.apppicker.SettingsAppPickerViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.settings.apppicker.SettingsAppPickerViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.settings.backgroundpicker.SettingsBackgroundPickerViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.settings.backgroundpicker.SettingsBackgroundPickerViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.settings.configuration.SettingsConfigurationViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.settings.configuration.SettingsConfigurationViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsViewModelImpl
import com.kieronquinn.app.discoverkiller.utils.AppIconRequestHandler
import com.kieronquinn.monetcompat.core.MonetCompat
import com.squareup.picasso.Picasso
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.lsposed.hiddenapibypass.HiddenApiBypass

class DiscoverKiller : Application() {

    private val components by lazy {
        module {
            single { BlurProvider.getBlurProvider(resources) }
            single<Navigation> { NavigationImpl() }
            single<Settings> { SettingsImpl(this@DiscoverKiller) }
            single<RemoteSplashLoader> { RemoteSplashLoaderImpl() }
        }
    }

    private val viewModels by lazy {
        module {
            viewModel<SettingsViewModel> { SettingsViewModelImpl() }
            viewModel<SettingsConfigurationViewModel> { SettingsConfigurationViewModelImpl(get(), get()) }
            viewModel<SettingsAppPickerViewModel> { SettingsAppPickerViewModelImpl(get()) }
            viewModel<SettingsBackgroundPickerViewModel> { SettingsBackgroundPickerViewModelImpl(get()) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            //Allows blurs on some devices, doesn't really matter if it fails
            HiddenApiBypass.addHiddenApiExemptions("L")
        }
        startKoin {
            androidContext(this@DiscoverKiller)
            modules(components, viewModels)
        }
        Picasso.setSingletonInstance(
            Picasso.Builder(this)
                .addRequestHandler(AppIconRequestHandler(this))
                .build()
        )
        setupMonet()
    }

    private fun setupMonet(){
        val settings = get<Settings>()
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

}