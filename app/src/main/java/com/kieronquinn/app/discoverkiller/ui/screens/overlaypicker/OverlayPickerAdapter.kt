package com.kieronquinn.app.discoverkiller.ui.screens.overlaypicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.databinding.ItemPickerBinding
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository.Overlay
import com.kieronquinn.app.discoverkiller.ui.screens.overlaypicker.OverlayPickerAdapter.ViewHolder
import com.kieronquinn.app.discoverkiller.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.discoverkiller.utils.extensions.onClicked
import com.kieronquinn.app.discoverkiller.utils.picasso.PackageItemInfoRequestHandler
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OverlayPickerAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<Overlay>,
    val onOverlayClicked: (Overlay) -> Unit
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
        val overlay = items[position]
        pickerLabel.text = overlay.label
        pickerPackage.setText(overlay.overlayType.nameRes)
        picasso.load(PackageItemInfoRequestHandler.getUriFor(overlay.overlayComponent))
            .into(pickerIcon)
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onOverlayClicked(overlay)
            }
        }
        Unit
    }

    data class ViewHolder(val binding: ItemPickerBinding):
        LifecycleAwareRecyclerView.ViewHolder(binding.root)

}