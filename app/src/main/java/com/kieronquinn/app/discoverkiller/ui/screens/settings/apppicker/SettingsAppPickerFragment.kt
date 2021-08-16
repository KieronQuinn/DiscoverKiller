package com.kieronquinn.app.discoverkiller.ui.screens.settings.apppicker

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.discoverkiller.databinding.FragmentAppPickerBinding
import com.kieronquinn.app.discoverkiller.model.AppPickerItem
import com.kieronquinn.app.discoverkiller.ui.base.BackAvailable
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsViewModel
import com.kieronquinn.app.discoverkiller.utils.TransitionUtils
import com.kieronquinn.app.discoverkiller.utils.extensions.applyMonetToFastScroller
import com.kieronquinn.monetcompat.extensions.views.applyMonetRecursively
import com.kieronquinn.monetcompat.extensions.views.enableStretchOverscroll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class SettingsAppPickerFragment :
    BoundFragment<FragmentAppPickerBinding>(FragmentAppPickerBinding::inflate), BackAvailable {

    companion object {
        const val REQUEST_APP = "request_app"
        const val KEY_APP_PACKAGE = "package"
    }

    private val viewModel by viewModel<SettingsAppPickerViewModel>()
    private val settingsViewModel by sharedViewModel<SettingsViewModel>()

    private val adapter by lazy {
        SettingsAppPickerAdapter(
            requireContext(),
            emptyList<AppPickerItem>().toMutableList(),
            this::onPackageEnabledChanged
        )
    }

    private val searchTextWatcher = object : TextWatcher {

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setSearchTerm(s?.toString() ?: return)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable?) {}

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        enterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        returnTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
        reenterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.applyMonetRecursively()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResult(REQUEST_APP, bundleOf(KEY_APP_PACKAGE to null))
        setLoadingState(loading = true, isEmpty = false)
        setupRecyclerView()
        setupViewModel()
        setupSearch()
        setupSnackbarPadding()
    }

    private fun setupRecyclerView() {
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SettingsAppPickerFragment.adapter
            ViewCompat.setOnApplyWindowInsetsListener(this){ view, insets ->
                val requiredInsets = insets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.statusBars())
                updatePadding(left = requiredInsets.left, right = requiredInsets.right, bottom = requiredInsets.bottom)
                insets
            }
            enableStretchOverscroll()
            applyMonetToFastScroller()
        }
    }

    private fun setupViewModel() {
        lifecycleScope.launchWhenResumed {
            launch {
                viewModel.loadState.debounce(50).collect {
                    handleLoadState(it)
                }
            }
        }
    }

    private fun handleLoadState(loadState: SettingsAppPickerViewModel.LoadState) {
        setLoadingState(
            loadState is SettingsAppPickerViewModel.LoadState.Loading,
            (loadState is SettingsAppPickerViewModel.LoadState.Loaded && loadState.apps.isEmpty())
        )
        if (loadState is SettingsAppPickerViewModel.LoadState.Loaded) {
            adapter.setItems(loadState.apps)
        }
    }

    private fun setLoadingState(loading: Boolean, isEmpty: Boolean) {
        binding.recyclerView.isVisible = !loading && !isEmpty
        binding.appPickerLoading.isVisible = loading && !isEmpty
        binding.appPickerEmpty.isVisible = isEmpty
    }

    private fun setupSearch() {
        with(binding.appPickerSearch) {
            val background = monet.getBackgroundColor(requireContext())
            val secondaryBackground = monet.getBackgroundColorSecondary(requireContext()) ?: background
            root.setBackgroundColor(background)
            searchBox.backgroundTintList = ColorStateList.valueOf(secondaryBackground)
            searchBox.text.run {
                clear()
                append(viewModel.getSearchTerm())
            }
            lifecycleScope.launchWhenResumed {
                viewModel.showSearchClearButton.collect {
                    searchClear.isVisible = it
                }
            }
            searchClear.setOnClickListener {
                searchBox.text.clear()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.appPickerSearch.searchBox.addTextChangedListener(searchTextWatcher)
    }

    override fun onPause() {
        super.onPause()
        binding.appPickerSearch.searchBox.removeTextChangedListener(searchTextWatcher)
    }

    private fun onPackageEnabledChanged(appPickerItem: AppPickerItem.App){
        lifecycleScope.launchWhenResumed {
            setFragmentResult(REQUEST_APP, bundleOf(KEY_APP_PACKAGE to appPickerItem.packageName))
            navigation.navigateBack()
        }
    }

    private fun setupSnackbarPadding(){
        lifecycleScope.launchWhenResumed {
            settingsViewModel.snackbarShowing.collect {
                if(it){
                    adapter.addSnackbarPadding()
                }else{
                    adapter.removeSnackbarPadding()
                }
            }
        }
    }

}