package com.kieronquinn.app.discoverkiller.ui.base.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.FragmentSettingsBaseBinding
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.utils.extensions.applyBottomPadding
import com.kieronquinn.monetcompat.extensions.views.applyMonet

abstract class BaseSettingsFragment: BoundFragment<FragmentSettingsBaseBinding>(FragmentSettingsBaseBinding::inflate) {

    open val addAdditionalPadding = false

    abstract val adapter: BaseSettingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupLoading()
    }

    private fun setupRecyclerView() = with(binding.settingsBaseRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@BaseSettingsFragment.adapter
        applyBottomPadding(resources.getDimension(R.dimen.padding_16))
        if(addAdditionalPadding){
            updatePadding(top = resources.getDimension(R.dimen.padding_8).toInt())
        }
    }

    private fun setupLoading() = with(binding.settingsBaseLoadingProgress) {
        applyMonet()
    }

}