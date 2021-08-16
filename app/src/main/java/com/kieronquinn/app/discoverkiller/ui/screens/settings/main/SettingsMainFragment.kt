package com.kieronquinn.app.discoverkiller.ui.screens.settings.main

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.MenuInflater
import android.view.View
import android.view.animation.Animation
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.navigation.Navigation
import com.kieronquinn.app.discoverkiller.databinding.FragmentSettingsMainBinding
import com.kieronquinn.app.discoverkiller.ui.base.AutoExpandOnRotate
import com.kieronquinn.app.discoverkiller.ui.base.BackAvailable
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.ui.base.ProvidesOverflow
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsViewModel
import com.kieronquinn.app.discoverkiller.utils.extensions.onApplyInsets
import com.kieronquinn.app.discoverkiller.utils.extensions.slideIn
import com.kieronquinn.app.discoverkiller.utils.extensions.slideOut
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SettingsMainFragment: BoundFragment<FragmentSettingsMainBinding>(FragmentSettingsMainBinding::inflate) {

    private val settingsViewModel by sharedViewModel<SettingsViewModel>()

    private val fabMargin by lazy {
        resources.getDimension(R.dimen.padding_16).toInt()
    }

    private val appBarTypeface by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    private val navController by lazy {
        navHostFragment.navController
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        setupNavigationListener()
        setupMonet()
        setupFab()
        setupAppBar()
        setupSnackbar()
    }

    private fun setupNavigation(){
        lifecycleScope.launchWhenResumed {
            navigation.navigationBus.collect {
                handleNavigationEvent(it)
            }
        }
    }

    private fun handleNavigationEvent(navigationEvent: Navigation.NavigationEvent){
        when(navigationEvent){
            is Navigation.NavigationEvent.Directions -> navController.navigate(navigationEvent.directions)
            is Navigation.NavigationEvent.Id -> navController.navigate(navigationEvent.id)
            is Navigation.NavigationEvent.Back -> navController.navigateUp()
            is Navigation.NavigationEvent.PopupTo -> navController.popBackStack(navigationEvent.id, navigationEvent.popInclusive)
            is Navigation.NavigationEvent.Intent -> startActivity(navigationEvent.intent)
        }
    }

    private fun setupNavigationListener(){
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            if(!navController.navigateUp()){
                requireActivity().finish()
            }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.label?.isNotBlank() == true) {
                binding.collapsingToolbar.title = destination.label
                binding.appBar.setExpanded(true)
            }
        }
        navHostFragment.childFragmentManager.addOnBackStackChangedListener {
            updateFragmentOptions()
        }
        binding.toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        updateFragmentOptions()
    }

    private fun updateFragmentOptions(){
        val lastFragment = navHostFragment.childFragmentManager.fragments.first()
        binding.toolbar.setBackEnabled(lastFragment is BackAvailable)
        if(lastFragment is AutoExpandOnRotate) {
            binding.appBar.setExpanded(true)
        }
        binding.toolbar.menu.clear()
        if(lastFragment is ProvidesOverflow){
            (lastFragment as ProvidesOverflow).run {
                inflateMenu(MenuInflater(requireContext()), binding.toolbar.menu)
                binding.toolbar.setOnMenuItemClickListener {
                    onMenuItemSelected(it)
                }
            }
        }
    }

    private fun Toolbar.setBackEnabled(enabled: Boolean){
        navigationIcon = if(enabled) ContextCompat.getDrawable(requireContext(), R.drawable.ic_back) else null
    }

    private fun setupMonet(){
        val background = monet.getBackgroundColor(requireContext())
        binding.root.setBackgroundColor(background)
        binding.appBar.setBackgroundColor(background)
        binding.fabPreview.applyMonet()
    }

    private fun setupFab(){
        with(binding.fabPreview){
            setOnClickListener {
                settingsViewModel.showOverlay()
            }
            onApplyInsets { view, insets ->
                val bottomMargin = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    updateMargins(bottom = fabMargin + bottomMargin)
                }
            }
        }
        lifecycleScope.launchWhenResumed {
            settingsViewModel.overlayLoadState.collect {
                handleLoadState(it)
            }
        }
    }

    private fun handleLoadState(state: SettingsViewModel.OverlayLoadState) {
        Log.d("SVM", "handleLoadState $state")
        if(state == SettingsViewModel.OverlayLoadState.WAITING_FOR_RESTART){
            setSnackbarVisibility(true)
            binding.snackbarRoot.snackbarText.text = getString(R.string.snackbar_reloading_overlay)
            binding.snackbarRoot.snackbarProgress.isVisible = true
            reconnectAfterDelay()
        }else{
            setSnackbarVisibility(false)
        }
        if(state == SettingsViewModel.OverlayLoadState.RUNNING){
            setFabVisibility(true)
        }else{
            setFabVisibility(false)
        }
        if(state == SettingsViewModel.OverlayLoadState.TIMEOUT){
            setSnackbarVisibility(true)
            binding.snackbarRoot.snackbarText.text = getString(R.string.snackbar_overlay_timeout)
            binding.snackbarRoot.snackbarProgress.isVisible = false
        }
    }

    private fun reconnectAfterDelay(){
        lifecycleScope.launchWhenResumed {
            //Give time for overlay to be killed & QSB to restart
            delay(1000)
            settingsViewModel.reconnectOverlay()
        }
    }

    private fun setupAppBar(){
        binding.appBar.onApplyInsets { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = topInset)
        }
        binding.collapsingToolbar.setContentScrimColor(monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(requireContext()))
        binding.collapsingToolbar.setCollapsedTitleTypeface(appBarTypeface)
        binding.collapsingToolbar.setExpandedTitleTypeface(appBarTypeface)
    }

    private fun setupSnackbar(){
        binding.snackbarContainer.onApplyInsets { view, insets ->
            val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = bottomInsets)
        }
        val background = monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(requireContext())
        binding.snackbar.backgroundTintList = ColorStateList.valueOf(background)
        binding.snackbarRoot.snackbarProgress.applyMonet()
    }

    private var snackbarAnimation: Animation? = null
    private var snackbarVisible = false
    private fun setSnackbarVisibility(visible: Boolean){
        if(visible == snackbarVisible) return
        snackbarVisible = visible
        snackbarAnimation?.cancel()
        snackbarAnimation = if(visible) binding.snackbarContainer.slideIn {  }
        else binding.snackbarContainer.slideOut {  }
    }

    private var fabVisible = true
    private fun setFabVisibility(visible: Boolean){
        if(fabVisible == visible) return
        fabVisible = visible
        if(visible){
            binding.fabPreview.show()
        }else{
            binding.fabPreview.hide()
        }
    }

}