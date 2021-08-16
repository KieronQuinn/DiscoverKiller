package com.kieronquinn.app.discoverkiller.model

import androidx.annotation.DrawableRes
import kotlin.reflect.KMutableProperty0

sealed class SettingsItem(open val itemType: SettingsItemType, open val visible: (() -> Boolean)? = null){
    data class Switch(@DrawableRes val icon: Int?, val title: CharSequence, val subtitle: CharSequence?, val setting: KMutableProperty0<Boolean>, val reloadOnChange: Boolean = true, override val visible: (() -> Boolean)? = null): SettingsItem(SettingsItemType.SWITCH)
    data class Action(@DrawableRes val icon: Int?, val title: CharSequence, val subtitle: CharSequence?, val action: () -> Unit, override val visible: (() -> Boolean)? = null): SettingsItem(SettingsItemType.ACTION)
    data class Info(val text: CharSequence): SettingsItem(SettingsItemType.INFO)
}

enum class SettingsItemType {
    SWITCH, ACTION, INFO
}
