package com.kieronquinn.app.discoverkiller.ui.screens.settings.error.noxposed

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import com.jakewharton.processphoenix.ProcessPhoenix
import com.kieronquinn.app.discoverkiller.databinding.FragmentErrorNoXposedBinding
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor

class ErrorNoXposedFragment: BoundFragment<FragmentErrorNoXposedBinding>(FragmentErrorNoXposedBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        with(binding.errorNoXposedRetry){
            val accentColor = monet.getAccentColor(requireContext())
            strokeColor = ColorStateList.valueOf(accentColor)
            setTextColor(accentColor)
            overrideRippleColor(accentColor)
            setOnClickListener {
                ProcessPhoenix.triggerRebirth(requireContext())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().finish()
    }

}