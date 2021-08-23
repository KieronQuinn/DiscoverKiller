package com.kieronquinn.app.discoverkiller.ui.screens.settings.error.hookfail

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.jakewharton.processphoenix.ProcessPhoenix
import com.kieronquinn.app.discoverkiller.databinding.FragmentErrorHookingBinding
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.error.errorignore.ErrorIgnoreBottomSheetFragment
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor

class ErrorXposedHookFailFragment: BoundFragment<FragmentErrorHookingBinding>(FragmentErrorHookingBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStatusNav()
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        with(binding.errorHookingRetry){
            setup()
            setOnClickListener {
                ProcessPhoenix.triggerRebirth(requireContext())
            }
        }
        with(binding.errorHookingIgnore){
            setup()
            setOnClickListener {
                ErrorIgnoreBottomSheetFragment().show(childFragmentManager, "bs_ignore")
            }
        }
    }

    private fun setupStatusNav(){
        val window = requireActivity().window
        window.decorView.post {
            WindowInsetsControllerCompat(window, window.decorView).run {
                isAppearanceLightNavigationBars = !requireContext().isDarkMode
                isAppearanceLightStatusBars = !requireContext().isDarkMode
            }
        }
    }

    private fun MaterialButton.setup(){
        val accentColor = monet.getAccentColor(requireContext())
        strokeColor = ColorStateList.valueOf(accentColor)
        setTextColor(accentColor)
        overrideRippleColor(accentColor)
    }

}