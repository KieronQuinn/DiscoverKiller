package com.kieronquinn.app.discoverkiller.ui.screens.settings.colorpicker

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.settings.Settings
import com.kieronquinn.app.discoverkiller.databinding.FragmentBottomSheetColorPickerBinding
import com.kieronquinn.app.discoverkiller.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ColorPickerBottomSheetFragment : BaseBottomSheetFragment<FragmentBottomSheetColorPickerBinding>(FragmentBottomSheetColorPickerBinding::inflate) {

    private val settings by inject<Settings>()
    private val settingsViewModel by sharedViewModel<SettingsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view){ _, insets ->
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val extraPadding = resources.getDimension(R.dimen.padding_16).toInt()
            view.updatePadding(left = navigationInsets.left, right = navigationInsets.right, bottom = navigationInsets.bottom + extraPadding)
            insets
        }
        lifecycleScope.launchWhenResumed {
            with(binding){
                val availableColors = monet.getAvailableWallpaperColors() ?: emptyList()
                //No available colors = likely using a live wallpaper, show a toast and dismiss
                if(availableColors.isEmpty()){
                    Toast.makeText(requireContext(), getString(R.string.color_picker_unavailable), Toast.LENGTH_LONG).show()
                    dismiss()
                    return@launchWhenResumed
                }
                root.backgroundTintList = ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
                colorPickerList.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                colorPickerList.adapter = ColorPickerAdapter(requireContext(), monet.getSelectedWallpaperColor(), availableColors){
                    onColorPicked(it)
                }
                colorPickerOk.setOnClickListener {
                    dialog?.dismiss()
                }
                colorPickerOk.setTextColor(monet.getAccentColor(requireContext()))
            }
        }
    }

    private fun onColorPicked(color: Int) = lifecycleScope.launchWhenResumed {
        settings.monetColor = color
        settingsViewModel.reloadOverlay()
        //Trigger a manual update
        monet.updateMonetColors()
    }

}