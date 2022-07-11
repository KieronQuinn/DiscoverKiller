package com.kieronquinn.app.discoverkiller.ui.screens.apppicker

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.FragmentAppPickerBinding
import com.kieronquinn.app.discoverkiller.ui.base.BackAvailable
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.ui.screens.apppicker.AppPickerViewModel.State
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.onApplyInsets
import com.kieronquinn.app.discoverkiller.utils.extensions.onChanged
import com.kieronquinn.app.discoverkiller.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AppPickerFragment: BoundFragment<FragmentAppPickerBinding>(FragmentAppPickerBinding::inflate), BackAvailable {

    private val viewModel by viewModel<AppPickerViewModel>()

    private val adapter by lazy {
        AppPickerAdapter(
            binding.appPickerRecyclerview,
            emptyList(),
            viewModel::onAppClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMonet()
        setupState()
        setupSearch()
        setupSearchClear()
        setupInsets()
    }

    private fun setupRecyclerView() = with(binding.appPickerRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@AppPickerFragment.adapter
    }

    private fun setupMonet() {
        val searchBackground = monet.getPrimaryColor(requireContext(), !requireContext().isDarkMode)
        binding.includeSearch.searchBox.backgroundTintList =
            ColorStateList.valueOf(searchBackground)
        binding.appPickerLoadingProgress.applyMonet()
    }

    private fun setupInsets() {
        val standardPadding = resources.getDimension(R.dimen.padding_16).toInt()
        binding.appPickerRecyclerview.onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            view.updatePadding(bottom = standardPadding + bottomInset)
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.appPickerLoading.isVisible = true
                binding.appPickerRecyclerview.isVisible = false
                binding.appPickerEmpty.isVisible = false
            }
            is State.Loaded -> {
                binding.appPickerLoading.isVisible = false
                binding.appPickerEmpty.isVisible = state.apps.isEmpty()
                binding.appPickerRecyclerview.isVisible = state.apps.isNotEmpty()
                adapter.items = state.apps
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupSearch() {
        setSearchText(viewModel.searchText.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            launch {
                binding.includeSearch.searchBox.onChanged().debounce(250L).collect {
                    viewModel.setSearchText(it ?: "")
                }
            }
        }
    }

    private fun setupSearchClear() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        launch {
            viewModel.searchShowClear.collect {
                binding.includeSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.includeSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.includeSearch.searchBox.run {
            this.text?.let {
                it.clear()
                it.append(text)
            } ?: setText(text)
        }
    }

}