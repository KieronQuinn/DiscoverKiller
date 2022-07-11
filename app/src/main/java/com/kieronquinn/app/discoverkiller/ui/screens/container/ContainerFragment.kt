package com.kieronquinn.app.discoverkiller.ui.screens.container

import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.navigation.ContainerNavigation
import com.kieronquinn.app.discoverkiller.databinding.FragmentContainerBinding
import com.kieronquinn.app.discoverkiller.ui.base.BaseContainerFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ContainerFragment: BaseContainerFragment<FragmentContainerBinding>(FragmentContainerBinding::inflate) {

    override val viewModel by viewModel<ContainerViewModel>()
    override val navigation by inject<ContainerNavigation>()

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    override val appBar
        get() = binding.containerAppBar
    override val toolbar
        get() = binding.containerToolbar
    override val collapsingToolbar
        get() = binding.containerCollapsingToolbar
    override val fragment
        get() = binding.navHostFragment

    override val bottomNavigation: BottomNavigationView?
        get() = null

}