package com.kieronquinn.app.discoverkiller.ui.screens.settings.container

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.libraries.launcherclient.LauncherClient
import com.kieronquinn.app.discoverkiller.components.xposed.XposedSelfHook
import com.kieronquinn.app.discoverkiller.databinding.FragmentSettingsContainerBinding
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SettingsContainerFragment :
    BoundFragment<FragmentSettingsContainerBinding>(FragmentSettingsContainerBinding::inflate) {

    private val settingsViewModel by sharedViewModel<SettingsViewModel>()

    private val launcherClient by lazy {
        LauncherClient(requireActivity(), settingsViewModel, true)
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.settingsContainerViewpager.isUserInputEnabled = position == 0
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            if (position == 0) {
                launcherClient.updateMove(1f - positionOffset)
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            when (state) {
                ViewPager2.SCROLL_STATE_IDLE -> {
                    launcherClient.endMove()
                    binding.settingsContainerViewpager.currentItem = 1
                }
                ViewPager2.SCROLL_STATE_DRAGGING, ViewPager2.SCROLL_STATE_SETTLING -> {
                    launcherClient.startMove()
                }
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launcherClient.onAttachedToWindow()
        setupWithXposedDelayIfNeeded()
    }

    private fun setupWithXposedDelayIfNeeded() = lifecycleScope.launchWhenResumed {
        //Give time for Xposed hooks to start if needed
        if(!XposedSelfHook.isXposedHooked()){
            withContext(Dispatchers.IO) {
                delay(2000)
            }
            setup()
        }else{
            setup()
        }
    }

    private fun setup(){
        setupViewPager()
        setupShowListener()
        setupReconnectListener()
    }

    private fun setupViewPager() {
        with(binding.settingsContainerViewpager) {
            adapter = SettingsContainerPagerAdapter(this@SettingsContainerFragment)
            setCurrentItem(1, false)
            registerOnPageChangeCallback(pageChangeCallback)
            isUserInputEnabled = false
        }
    }

    private fun setupShowListener(){
        lifecycleScope.launchWhenResumed {
            settingsViewModel.showOverlayBus.collect {
                binding.settingsContainerViewpager.currentItem = 0
            }
        }
    }

    private fun setupReconnectListener(){
        lifecycleScope.launchWhenResumed {
            settingsViewModel.reconnectOverlayBus.collect {
                launcherClient.reconnect()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        launcherClient.onDetachedFromWindow()
    }

    override fun onResume() {
        super.onResume()
        launcherClient.onResume()
    }

    override fun onPause() {
        super.onPause()
        launcherClient.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        launcherClient.onDestroy()
    }

}