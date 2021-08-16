package com.kieronquinn.app.discoverkiller.ui.screens.settings.error.hookfail

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import com.jakewharton.processphoenix.ProcessPhoenix
import com.kieronquinn.app.discoverkiller.databinding.FragmentErrorHookingBinding
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor

class ErrorXposedHookFailFragment: BoundFragment<FragmentErrorHookingBinding>(FragmentErrorHookingBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        with(binding.errorHookingRetry){
            val accentColor = monet.getAccentColor(requireContext())
            strokeColor = ColorStateList.valueOf(accentColor)
            setTextColor(accentColor)
            overrideRippleColor(accentColor)
            setOnClickListener {
                ProcessPhoenix.triggerRebirth(requireContext())
            }
        }
    }

}