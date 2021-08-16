package com.kieronquinn.app.discoverkiller.ui.screens.settings.backgroundpicker

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.components.splash.RemoteSplashLoader
import com.kieronquinn.app.discoverkiller.databinding.FragmentSettingsBackgroundPickerBinding
import com.kieronquinn.app.discoverkiller.ui.activities.SettingsBackgroundPickerActivity
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.utils.extensions.onApplyInsets
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import java.io.Serializable

class SettingsBackgroundPickerFragment: BoundFragment<FragmentSettingsBackgroundPickerBinding>(FragmentSettingsBackgroundPickerBinding::inflate) {

    companion object {
        private const val KEY_CURRENT_POS = "current_pos"
    }

    internal val viewModel by inject<SettingsBackgroundPickerViewModel>()
    private val settings by inject<Settings>()

    private val backgroundColor by lazy {
        monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(requireContext())
    }

    private val bottomPadding by lazy {
        resources.getDimension(R.dimen.padding_16).toInt()
    }

    private val adapter by lazy {
        SettingsBackgroundPickerAdapter(this@SettingsBackgroundPickerFragment, emptyList())
    }

    private val pageChangeCallback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            adapter.items.getOrNull(position)?.let {
                setupSheetWithItem(it.type)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentPos = savedInstanceState?.getInt(KEY_CURRENT_POS)
        binding.root.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        setupToolbar()
        setupViewPager()
        setupBottomSheet()
        setupClickListener()
        loadSplashScreens(currentPos)
    }

    private fun setupClickListener(){
        lifecycleScope.launchWhenResumed {
            viewModel.clickEvent.collect {
                toggleControls()
            }
        }
    }

    private fun setupViewPager(){
        with(binding.settingsBackgroundPickerViewpager) {
            adapter = this@SettingsBackgroundPickerFragment.adapter
            offscreenPageLimit = 1
            registerOnPageChangeCallback(pageChangeCallback)
            (getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    private fun toggleControls(){
        binding.settingsBackgroundPickerHideable.isVisible = !binding.settingsBackgroundPickerHideable.isVisible
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadSplashScreens(currentPos: Int?) = lifecycleScope.launchWhenResumed {
        viewModel.getSplashScreenOptions(requireContext(), settings.overlayApp).collect {
            val options = it.first
            val default = it.second
            adapter.items = options
            adapter.notifyDataSetChanged()
            if(options.size > 1){
                Toast.makeText(requireContext(), getString(R.string.configuration_app_monet_toast), Toast.LENGTH_LONG).show()
            }
            val currentOption = settings.overlayBackground.run {
                if(this == RemoteSplashLoader.SplashScreenType.DEFAULT){
                    default
                }else this
            }
            val appliedPos = options.indexOfFirst { splash -> splash.type == currentOption }
            if(appliedPos != -1 || currentPos != null) {
                binding.settingsBackgroundPickerViewpager.currentItem = currentPos ?: appliedPos
            }
        }
    }

    private fun setupBottomSheet(){
        with(binding.settingsBackgroundPickerSheet){
            backgroundTintList = ColorStateList.valueOf(backgroundColor)
            background.alpha = 191
            onApplyInsets { view, insets ->
                val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                view.updatePadding(bottom = bottomPadding + bottomInset)
            }
        }
        with(binding.settingsBackgroundPickerApply){
            val accent = monet.getAccentColor(requireContext())
            setTextColor(accent)
            strokeColor = ColorStateList.valueOf(accent)
            setOnClickListener {
                onApplyClicked(binding.settingsBackgroundPickerViewpager.currentItem)
            }
        }
    }

    private fun setupToolbar(){
        with(binding.toolbar){
            title = getString(R.string.configuration_app_monet)
            navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_back)
            setNavigationOnClickListener {
                requireActivity().finish()
            }
            background = ColorDrawable(ColorUtils.setAlphaComponent(backgroundColor, 191))
            onApplyInsets { view, insets ->
                val requiredInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                view.updatePadding(top = requiredInsets.top, left = requiredInsets.left, right = requiredInsets.right)
            }
            setTitleTextAppearance(requireContext(), R.style.ToolbarTitle)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val position = binding.settingsBackgroundPickerViewpager.currentItem
        if(position != 0) {
            outState.putInt(KEY_CURRENT_POS, binding.settingsBackgroundPickerViewpager.currentItem)
        }
    }

    private fun onApplyClicked(selectedPage: Int){
        requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(SettingsBackgroundPickerActivity.KEY_SELECTED_SPLASH_TYPE, adapter.items[selectedPage].type as Serializable))
        requireActivity().finish()
    }

    private fun setupSheetWithItem(type: RemoteSplashLoader.SplashScreenType){
        binding.settingsBackgroundPickerTitle.text = getString(type.titleRes)
        binding.settingsBackgroundPickerInfo.text = getString(type.descRes)
    }

}