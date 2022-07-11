package com.kieronquinn.app.discoverkiller

import android.app.Application
import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.kieronquinn.app.discoverkiller.components.blur.BlurProvider
import com.kieronquinn.app.discoverkiller.components.navigation.*
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoaderImpl
import com.kieronquinn.app.discoverkiller.repositories.*
import com.kieronquinn.app.discoverkiller.ui.activities.MainActivityViewModel
import com.kieronquinn.app.discoverkiller.ui.activities.MainActivityViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.apppicker.AppPickerViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.apppicker.AppPickerViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.backgroundpicker.SettingsBackgroundPickerViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.backgroundpicker.SettingsBackgroundPickerViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.container.ContainerViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.container.ContainerViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.contributors.ContributorsViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.contributors.ContributorsViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.noroot.ErrorNoRootViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.noroot.ErrorNoRootViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.noxposed.ErrorNoXposedViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.noxposed.ErrorNoXposedViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.overlaypicker.OverlayPickerViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.overlaypicker.OverlayPickerViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.RssConfigureViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.RssConfigureViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.logo.RssLogoBottomSheetViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.title.RssTitleBottomSheetViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.url.RssUrlBottomSheetViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.settings.SettingsViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.settings.SettingsViewModelImpl
import com.kieronquinn.app.discoverkiller.ui.screens.update.UpdateViewModel
import com.kieronquinn.app.discoverkiller.ui.screens.update.UpdateViewModelImpl
import com.kieronquinn.app.discoverkiller.utils.picasso.PackageItemInfoRequestHandler
import com.kieronquinn.app.discoverkiller.xposed.Xposed
import com.kieronquinn.monetcompat.core.MonetCompat
import com.squareup.picasso.Picasso
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.android.ext.android.get

class DiscoverKiller: Application() {

    private val repositories = module {
        single<SettingsRepository>(createdAtStart = true) { SettingsRepositoryImpl(get(), get()) }
        single<RootNavigation> { RootNavigationImpl() }
        single<ContainerNavigation> { ContainerNavigationImpl() }
        single<RssNavigation> { RssNavigationImpl() }
        single<OverlayRepository> { OverlayRepositoryImpl(get()) }
        single<RemoteSplashLoader> { RemoteSplashLoaderImpl() }
        single<RssRepository> { RssRepositoryImpl(get(), get()) }
        single<AppRepository> { AppRepositoryImpl(get()) }
        single<UpdateRepository> { UpdateRepositoryImpl(get()) }
        single<RootRepository> { RootRepositoryImpl() }
        single<XposedRepository> { XposedRepositoryImpl(get()) }
        single { BlurProvider.getBlurProvider(resources) }
        single { createPicasso(get()) }
        single { createMarkwon() }
    }

    private val viewModels = module {
        viewModel<MainActivityViewModel> { MainActivityViewModelImpl(get(), get()) }
        viewModel<ContainerViewModel> { ContainerViewModelImpl(get()) }
        viewModel<SettingsViewModel> { SettingsViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<OverlayPickerViewModel> { OverlayPickerViewModelImpl(get(), get(), get()) }
        viewModel<AppPickerViewModel> { AppPickerViewModelImpl(get(), get(), get()) }
        viewModel<RssConfigureViewModel> { RssConfigureViewModelImpl(get(), get()) }
        viewModel<UpdateViewModel> { UpdateViewModelImpl(get(), get(), get()) }
        viewModel<ErrorNoXposedViewModel> { ErrorNoXposedViewModelImpl(get()) }
        viewModel<ErrorNoRootViewModel> { ErrorNoRootViewModelImpl(get()) }
        viewModel<ContributorsViewModel> { ContributorsViewModelImpl(get()) }
        viewModel { RssUrlBottomSheetViewModel(get(), get()) }
        viewModel { RssTitleBottomSheetViewModel(get(), get()) }
        viewModel { RssLogoBottomSheetViewModel(get(), get()) }
        viewModel<SettingsBackgroundPickerViewModel> { SettingsBackgroundPickerViewModelImpl(get(), get()) }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        setupKoin(base)
    }

    override fun onCreate() {
        super.onCreate()
        setupMonet()
    }

    private fun setupKoin(context: Context) {
        startKoin {
            androidContext(context)
            modules(repositories, viewModels)
        }
    }

    private fun createPicasso(context: Context): Picasso {
        return Picasso.Builder(context)
            .addRequestHandler(PackageItemInfoRequestHandler(context))
            .build()
    }

    private fun createMarkwon(): Markwon {
        val typeface = ResourcesCompat.getFont(this, R.font.google_sans_text_medium)
        return Markwon.builder(this).usePlugin(object: AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                typeface?.let {
                    builder.headingTypeface(it)
                    builder.headingBreakHeight(0)
                }
            }
        }).build()
    }

    private fun setupMonet(){
        val settings = get<SettingsRepository>()
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor.getSync()
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

}