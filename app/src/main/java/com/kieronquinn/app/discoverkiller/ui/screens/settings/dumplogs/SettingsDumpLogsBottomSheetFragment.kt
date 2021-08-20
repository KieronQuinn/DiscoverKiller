package com.kieronquinn.app.discoverkiller.ui.screens.settings.dumplogs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.FragmentBottomSheetDumpLogsBinding
import com.kieronquinn.app.discoverkiller.ui.base.BaseBottomSheetFragment
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsDumpLogsBottomSheetFragment: BaseBottomSheetFragment<FragmentBottomSheetDumpLogsBinding>(FragmentBottomSheetDumpLogsBinding::inflate) {

    private val viewModel by viewModel<SettingsDumpLogsBottomSheetViewModel>()

    private val locationPicker = registerForActivityResult(ActivityResultContracts.CreateDocument()){
        if(it != null){
            viewModel.onLocationSelected(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view){ _, insets ->
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val extraPadding = resources.getDimension(R.dimen.padding_16).toInt()
            view.updatePadding(left = navigationInsets.left, right = navigationInsets.right, bottom = navigationInsets.bottom + extraPadding)
            insets
        }
        setupMonet()
        setupButtons()
        setupStateListener()
    }

    private fun setupButtons(){
        with(binding){
            dumpLogsContinue.setOnClickListener {
                viewModel.onContinueClicked()
            }
            dumpLogsCancel.setOnClickListener {
                viewModel.onCancelClicked()
            }
        }
    }

    private fun setupMonet(){
        with(binding) {
            binding.dumpLogsProgress.applyMonet()
            dumpLogsContinue.setTextColor(monet.getAccentColor(requireContext()))
            dumpLogsCancel.setTextColor(monet.getAccentColor(requireContext()))
        }
    }

    private fun setupStateListener(){
        lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: SettingsDumpLogsBottomSheetViewModel.State) {
        when(state){
            is SettingsDumpLogsBottomSheetViewModel.State.ChooseLocation -> {
                viewModel.chooseLocation(locationPicker)
            }
            is SettingsDumpLogsBottomSheetViewModel.State.Idle -> {
                binding.dumpLogsContent.isInvisible = false
                binding.dumpLogsProgress.isVisible = false
                binding.dumpLogsCancel.isVisible = true
                binding.dumpLogsContinue.isVisible = true
            }
            is SettingsDumpLogsBottomSheetViewModel.State.Dump -> {
                binding.dumpLogsContent.isInvisible = true
                binding.dumpLogsProgress.isVisible = true
                binding.dumpLogsCancel.isVisible = false
                binding.dumpLogsContinue.isVisible = false
            }
            is SettingsDumpLogsBottomSheetViewModel.State.Finished -> {
                Toast.makeText(requireContext(), getString(state.result.toastMessage), Toast.LENGTH_LONG).show()
                dismiss()
            }
        }
    }

}