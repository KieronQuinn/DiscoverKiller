package com.kieronquinn.app.discoverkiller.ui.screens.backgroundpicker

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.databinding.FragmentSettingsBackgroundPickerPageBinding
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.base.BackAvailable
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import org.koin.android.ext.android.inject
import java.lang.Exception

class SettingsBackgroundPickerPageFragment: BoundFragment<FragmentSettingsBackgroundPickerPageBinding>(FragmentSettingsBackgroundPickerPageBinding::inflate), BackAvailable {

    private val splashLoader by inject<RemoteSplashLoader>()
    private val settings by inject<SettingsRepository>()
    private val viewModel by lazy {
        (requireParentFragment() as SettingsBackgroundPickerFragment).viewModel
    }

    companion object {
        const val KEY_SPLASH_TYPE = "splash_type"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnClickListener {
            viewModel.onPageClicked()
        }
        val splashType = arguments?.getSerializable(KEY_SPLASH_TYPE) as? RemoteSplashLoader.SplashScreenType ?: return
        lifecycleScope.launchWhenResumed {
            val packageName = try {
                ComponentName.unflattenFromString(settings.overlayApp.get())
            }catch (e: Exception){
                null
            }?.packageName ?: BuildConfig.APPLICATION_ID
            splashLoader.inflateSplashScreen(requireContext(), splashType, packageName).run {
                binding.settingsBackgroundPickerPageSplashContainer.addView(this)
            }
        }
    }

}