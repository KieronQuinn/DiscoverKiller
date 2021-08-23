package com.kieronquinn.app.discoverkiller.ui.screens.settings.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.xposed.XposedSelfHook
import com.kieronquinn.app.discoverkiller.ui.screens.settings.error.hookfail.ErrorXposedHookFailFragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.error.noxposed.ErrorNoXposedFragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.main.SettingsMainFragment
import com.kieronquinn.app.discoverkiller.utils.extensions.isAppInstalled

class SettingsContainerPagerAdapter(fragment: Fragment, private val settings: Settings): FragmentStateAdapter(fragment) {

    private val isXposedInstalled by lazy {
        fragment.requireContext().packageManager.run {
            XposedSelfHook.XPOSED_APPS.any { isAppInstalled(it) }
        }
    }

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position){
            1 -> {
                when {
                    !settings.ignoreXposedWarnings && !isXposedInstalled -> ErrorNoXposedFragment()
                    !settings.ignoreXposedWarnings && !XposedSelfHook.isXposedHooked() -> ErrorXposedHookFailFragment()
                    else -> SettingsMainFragment()
                }
            }
            else -> Fragment()
        }
    }

}