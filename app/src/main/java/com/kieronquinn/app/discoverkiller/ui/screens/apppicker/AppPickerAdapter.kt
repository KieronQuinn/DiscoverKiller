package com.kieronquinn.app.discoverkiller.ui.screens.apppicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.databinding.ItemPickerBinding
import com.kieronquinn.app.discoverkiller.repositories.AppRepository.App
import com.kieronquinn.app.discoverkiller.ui.screens.apppicker.AppPickerAdapter.ViewHolder
import com.kieronquinn.app.discoverkiller.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.discoverkiller.utils.extensions.onClicked
import com.kieronquinn.app.discoverkiller.utils.picasso.PackageItemInfoRequestHandler
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppPickerAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<App>,
    val onAppClicked: (App) -> Unit
): LifecycleAwareRecyclerView.Adapter<ViewHolder>(recyclerView), KoinComponent {

    private val picasso by inject<Picasso>()

    private val layoutInflater = LayoutInflater.from(recyclerView.context)

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPickerBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val app = items[position]
        pickerLabel.text = app.label
        pickerPackage.text = app.componentName.packageName
        pickerPackage.isVisible = app.shouldShowPackageName
        picasso.load(PackageItemInfoRequestHandler.getUriFor(app.componentName)).into(pickerIcon)
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onAppClicked(app)
            }
        }
        Unit
    }

    data class ViewHolder(val binding: ItemPickerBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}