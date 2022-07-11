package com.kieronquinn.app.discoverkiller.ui.screens.noroot

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.kieronquinn.app.discoverkiller.databinding.FragmentErrorNoRootBinding
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import org.koin.androidx.viewmodel.ext.android.viewModel

class ErrorNoRootFragment: BoundFragment<FragmentErrorNoRootBinding>(FragmentErrorNoRootBinding::inflate) {

    private val viewModel by viewModel<ErrorNoRootViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        with(binding.errorNoRootRetry){
            setup()
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                onClicked().collect {
                    viewModel.onRetryClicked()
                }
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