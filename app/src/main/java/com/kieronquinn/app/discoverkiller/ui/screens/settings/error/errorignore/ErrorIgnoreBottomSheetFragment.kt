package com.kieronquinn.app.discoverkiller.ui.screens.settings.error.errorignore

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.FragmentBottomSheetErrorIgnoreBinding
import com.kieronquinn.app.discoverkiller.ui.base.BaseBottomSheetFragment
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class ErrorIgnoreBottomSheetFragment: BaseBottomSheetFragment<FragmentBottomSheetErrorIgnoreBinding>(FragmentBottomSheetErrorIgnoreBinding::inflate) {

    private val viewModel by viewModel<ErrorIgnoreBottomSheetViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets(view)
        setupMonet()
        setupCheckbox()
        setupButtons()
    }

    private fun setupInsets(view: View){
        ViewCompat.setOnApplyWindowInsetsListener(view){ _, insets ->
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val extraPadding = resources.getDimension(R.dimen.padding_16).toInt()
            view.updatePadding(left = navigationInsets.left, right = navigationInsets.right, bottom = navigationInsets.bottom + extraPadding)
            insets
        }
    }

    private fun setupMonet() = with(binding) {
        errorIgnoreCheckbox.applyMonet()
        errorIgnoreContinue.setTextColor(monet.getAccentColor(requireContext()))
        errorIgnoreCancel.setTextColor(monet.getAccentColor(requireContext()))
    }

    private fun setupButtons(){
        binding.errorIgnoreCancel.setOnClickListener {
            dismiss()
        }
        binding.errorIgnoreContinue.setOnClickListener {
            viewModel.onContinueClicked(it.context)
        }
        lifecycleScope.launchWhenResumed {
            viewModel.continueChecked.collect {
                binding.errorIgnoreContinue.isClickable = it
                binding.errorIgnoreContinue.alpha = if(it) 1.0f else 0.5f
            }
        }
    }

    private fun setupCheckbox() = with(binding.errorIgnoreCheckbox) {
        setOnClickListener {
            viewModel.onCheckboxClicked()
        }
        lifecycleScope.launchWhenResumed {
            viewModel.continueChecked.collect {
                isChecked = it
            }
        }
    }

}