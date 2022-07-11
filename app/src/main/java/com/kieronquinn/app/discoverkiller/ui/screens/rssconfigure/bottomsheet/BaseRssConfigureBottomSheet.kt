package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.FragmentBottomSheetRssConfigureBinding
import com.kieronquinn.app.discoverkiller.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.discoverkiller.utils.extensions.onChanged
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect

abstract class BaseRssConfigureBottomSheet : BaseBottomSheetFragment<FragmentBottomSheetRssConfigureBinding>(
    FragmentBottomSheetRssConfigureBinding::inflate
) {

    abstract val viewModel: BaseRssConfigureBottomSheetViewModel

    abstract val title: Int
    abstract val hint: Int?

    open val positiveText: Int = android.R.string.ok
    open val negativeText: Int = android.R.string.cancel
    open val neutralText: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rssConfigureTitle.setText(title)
        setupMonet()
        setupButtons()
        setupInput()
        ViewCompat.setOnApplyWindowInsetsListener(view){ _, insets ->
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val extraPadding = resources.getDimension(R.dimen.padding_16).toInt()
            view.updatePadding(left = navigationInsets.left, right = navigationInsets.right, bottom = navigationInsets.bottom + extraPadding)
            insets
        }
    }

    private fun setupButtons() {
        binding.rssConfigurePositive.setOnClickListener { viewModel.onPositiveClicked() }
        binding.rssConfigureNegative.setOnClickListener { viewModel.onNegativeClicked() }
        binding.rssConfigureNeutral.setOnClickListener { viewModel.onNeutralClicked() }
        binding.rssConfigurePositive.setText(positiveText)
        binding.rssConfigureNegative.setText(negativeText)
        binding.rssConfigureNeutral.text = neutralText?.let { getString(it) }
        binding.rssConfigureNeutral.isVisible = neutralText != null
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.rssConfigurePositive.setTextColor(accent)
        binding.rssConfigureNegative.setTextColor(accent)
        binding.rssConfigureNeutral.setTextColor(accent)
    }

    private fun setupInput() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.rssConfigureEdit.onChanged().collect {
                viewModel.onTextChanged(it?.toString() ?: "")
            }
        }
        binding.rssConfigureEdit.setText(viewModel.value.value)
        binding.rssConfigureInput.hint = hint?.let { getString(it) }
        binding.rssConfigureInput.applyMonet()
        binding.rssConfigureEdit.applyMonet()
    }

}