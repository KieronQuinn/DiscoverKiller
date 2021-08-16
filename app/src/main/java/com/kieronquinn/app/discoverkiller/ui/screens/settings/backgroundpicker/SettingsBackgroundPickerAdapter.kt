package com.kieronquinn.app.discoverkiller.ui.screens.settings.backgroundpicker

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import java.io.Serializable

class SettingsBackgroundPickerAdapter(
    fragment: Fragment,
    var items: List<RemoteSplashLoader.SplashScreen>
): FragmentStateAdapter(fragment) {

    override fun getItemCount() = items.size

    override fun createFragment(position: Int): Fragment {
        return SettingsBackgroundPickerPageFragment().apply {
            arguments = bundleOf(
                SettingsBackgroundPickerPageFragment.KEY_SPLASH_TYPE to items[position].type as Serializable
            )
        }
    }
}