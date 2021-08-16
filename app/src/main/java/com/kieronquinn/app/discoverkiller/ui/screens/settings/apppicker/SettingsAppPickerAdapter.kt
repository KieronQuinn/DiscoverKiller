package com.kieronquinn.app.discoverkiller.ui.screens.settings.apppicker

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.discoverkiller.databinding.ItemAppBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemSnackbarPaddingBinding
import com.kieronquinn.app.discoverkiller.model.AppPickerItem
import com.kieronquinn.app.discoverkiller.model.AppPickerItemType
import com.kieronquinn.app.discoverkiller.utils.AppIconRequestHandler
import com.squareup.picasso.Picasso

class SettingsAppPickerAdapter(
    context: Context,
    private val items: MutableList<AppPickerItem>,
    private val onAppClicked: (AppPickerItem.App) -> Unit
): RecyclerView.Adapter<SettingsAppPickerAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private val picasso by lazy {
        Picasso.get()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return item.itemType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(AppPickerItemType.values().find { it.ordinal == viewType }!!){
            AppPickerItemType.APP -> ViewHolder.ItemAppViewHolder(ItemAppBinding.inflate(layoutInflater, parent, false))
            AppPickerItemType.SNACKBAR_PADDING -> ViewHolder.ItemSnackbarPaddingViewHolder(ItemSnackbarPaddingBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[holder.adapterPosition]
        when(holder){
            is ViewHolder.ItemAppViewHolder -> setupApp(holder.binding, item as AppPickerItem.App)
        }
    }

    private fun setupApp(binding: ItemAppBinding, item: AppPickerItem.App){
        with(binding){
            title.text = item.label
            val uri = Uri.parse("${AppIconRequestHandler.SCHEME_PNAME}:${item.packageName}")
            picasso.load(uri).into(icon)
            root.setOnClickListener {
                onAppClicked.invoke(item)
            }
        }
    }

    fun setItems(items: List<AppPickerItem>){
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return when(val item = items[position]){
            is AppPickerItem.App -> item.packageName.hashCode().toLong()
            is AppPickerItem.SnackbarPadding -> "SnackbarPadding".hashCode().toLong()
        }
    }

    fun addSnackbarPadding(){
        if(!items.contains(AppPickerItem.SnackbarPadding)){
            items.add(AppPickerItem.SnackbarPadding)
            notifyItemInserted(items.size - 1)
        }
    }

    fun removeSnackbarPadding(){
        if(items.contains(AppPickerItem.SnackbarPadding)){
            items.remove(AppPickerItem.SnackbarPadding)
            notifyItemRemoved(items.size)
        }
    }

    sealed class ViewHolder(open val view: View): RecyclerView.ViewHolder(view) {
        data class ItemAppViewHolder(val binding: ItemAppBinding): ViewHolder(binding.root)
        data class ItemSnackbarPaddingViewHolder(val binding: ItemSnackbarPaddingBinding): ViewHolder(binding.root)
    }

}