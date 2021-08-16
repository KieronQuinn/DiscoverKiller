package com.kieronquinn.app.discoverkiller.ui.screens.settings.colorpicker

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.discoverkiller.databinding.ItemColorPickerBinding

class ColorPickerAdapter(context: Context, private val selectedColor: Int?, private val colors: List<Int>, private val onColorPicked: (Int) -> Unit): RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getItemCount() = colors.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemColorPickerBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        with(holder.binding){
            itemColorPickerBackground.backgroundTintList = ColorStateList.valueOf(color)
            itemColorPickerCheck.isVisible = color == selectedColor
            itemColorPickerBackground.setOnClickListener {
                onColorPicked.invoke(color)
            }
        }
    }

    data class ViewHolder(val binding: ItemColorPickerBinding): RecyclerView.ViewHolder(binding.root)

}