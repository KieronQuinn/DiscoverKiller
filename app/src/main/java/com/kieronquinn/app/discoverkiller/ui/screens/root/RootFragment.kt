package com.kieronquinn.app.discoverkiller.ui.screens.root

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.navigation.RootNavigation
import com.kieronquinn.app.discoverkiller.components.navigation.setupWithNavigation
import com.kieronquinn.app.discoverkiller.databinding.FragmentRootBinding
import com.kieronquinn.app.discoverkiller.ui.activities.MainActivityViewModel
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.utils.extensions.firstNotNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class RootFragment: BoundFragment<FragmentRootBinding>(
    FragmentRootBinding::inflate
) {

    private val navigation by inject<RootNavigation>()
    private val activityViewModel by sharedViewModel<MainActivityViewModel>()

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_root) as NavHostFragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            setupStartDestination(
                activityViewModel.startDestination.firstNotNull(), savedInstanceState
            )
        }
    }

    private fun setupStartDestination(id: Int, savedInstanceState: Bundle?) {
        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph_root)
        graph.setStartDestination(id)
        navHostFragment.navController.setGraph(graph, savedInstanceState)
    }

    private fun setupNavigation() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        launch {
            navHostFragment.setupWithNavigation(navigation)
        }
    }

}