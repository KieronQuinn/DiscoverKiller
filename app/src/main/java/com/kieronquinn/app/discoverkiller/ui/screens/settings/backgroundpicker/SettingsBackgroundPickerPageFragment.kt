package com.kieronquinn.app.discoverkiller.ui.screens.settings.backgroundpicker

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.databinding.FragmentSettingsBackgroundPickerPageBinding
import com.kieronquinn.app.discoverkiller.ui.base.BackAvailable
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsBackgroundPickerPageFragment: BoundFragment<FragmentSettingsBackgroundPickerPageBinding>(FragmentSettingsBackgroundPickerPageBinding::inflate), BackAvailable {

    private val splashLoader by inject<RemoteSplashLoader>()
    private val settings by inject<Settings>()
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
            splashLoader.inflateSplashScreen(requireContext(), splashType, settings.overlayApp).run {
                binding.settingsBackgroundPickerPageSplashContainer.addView(this)
            }
        }
    }

}