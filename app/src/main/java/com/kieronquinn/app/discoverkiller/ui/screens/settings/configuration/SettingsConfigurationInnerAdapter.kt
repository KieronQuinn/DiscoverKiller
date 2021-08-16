package com.kieronquinn.app.discoverkiller.ui.screens.settings.configuration

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemConfigurationSettingsItemBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemConfigurationSettingsItemInfoBinding
import com.kieronquinn.app.discoverkiller.databinding.ItemConfigurationSettingsItemSwitchBinding
import com.kieronquinn.app.discoverkiller.model.SettingsItem
import com.kieronquinn.app.discoverkiller.model.SettingsItemType
import com.kieronquinn.app.discoverkiller.utils.extensions.applyMonetLight
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.views.applyMonet

class SettingsConfigurationInnerAdapter(
    context: Context,
    private val items: List<SettingsItem>,
    private val reloadListener: () -> Unit
) : RecyclerView.Adapter<SettingsConfigurationInnerAdapter.ViewHolder>() {

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].itemType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (SettingsItemType.values()[viewType]) {
            SettingsItemType.SWITCH -> ViewHolder.Setting(
                ItemConfigurationSettingsItemSwitchBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            SettingsItemType.ACTION -> ViewHolder.Action(
                ItemConfigurationSettingsItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            SettingsItemType.INFO -> ViewHolder.Info(
                ItemConfigurationSettingsItemInfoBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ViewHolder.Setting -> holder.binding.setupSetting(item as SettingsItem.Switch)
            is ViewHolder.Action -> holder.binding.setupAction(item as SettingsItem.Action)
            is ViewHolder.Info -> holder.binding.setupInfo(item as SettingsItem.Info)
        }
    }

    private fun ItemConfigurationSettingsItemSwitchBinding.setupSetting(item: SettingsItem.Switch) {
        if (item.visible != null) {
            root.setItemVisibility(item.visible.invoke())
        }
        itemSettingSwitch.setOnCheckedChangeListener(null)
        itemSettingTitle.text = item.title
        itemSettingContent.isVisible = item.subtitle != null
        itemSettingContent.text = item.subtitle
        if (item.icon != null) {
            itemSettingIcon.setImageResource(item.icon)
        }
        itemSettingSwitch.applyMonetLight()
        itemSettingSwitch.isChecked = item.setting.get()
        itemSettingSwitch.setOnCheckedChangeListener { _, checked ->
            item.setting.set(checked)
            if (item.reloadOnChange) {
                reloadListener.invoke()
            }
        }
        root.setOnClickListener {
            itemSettingSwitch.toggle()
        }
    }

    private fun ItemConfigurationSettingsItemBinding.setupAction(item: SettingsItem.Action) {
        if (item.visible != null) {
            root.setItemVisibility(item.visible.invoke())
        }
        itemSettingTitle.text = item.title
        itemSettingContent.isVisible = item.subtitle != null
        itemSettingContent.text = item.subtitle
        if (item.icon != null) {
            itemSettingIcon.setImageResource(item.icon)
        }
        root.setOnClickListener {
            item.action.invoke()
        }
    }

    private fun ItemConfigurationSettingsItemInfoBinding.setupInfo(item: SettingsItem.Info) {
        itemSettingInfoContent.text = item.text
        root.backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(root.context))
    }

    private fun ViewGroup.setItemVisibility(visible: Boolean) {
        if (visible) {
            visibility = View.VISIBLE
            updateLayoutParams<RecyclerView.LayoutParams> {
                height = RecyclerView.LayoutParams.WRAP_CONTENT
            }
        } else {
            visibility = View.GONE
            updateLayoutParams<RecyclerView.LayoutParams> { height = 0 }
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        data class Setting(override val binding: ItemConfigurationSettingsItemSwitchBinding) :
            ViewHolder(binding)

        data class Action(override val binding: ItemConfigurationSettingsItemBinding) :
            ViewHolder(binding)

        data class Info(override val binding: ItemConfigurationSettingsItemInfoBinding) :
            ViewHolder(binding)
    }

}