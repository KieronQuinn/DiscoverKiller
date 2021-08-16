package com.kieronquinn.app.discoverkiller.utils.extensions

import androidx.fragment.app.Fragment
import com.kieronquinn.app.discoverkiller.ui.screens.settings.container.SettingsContainerFragment

fun Fragment.expandAppBar(){
    (parentFragment as? SettingsContainerFragment)?.expandAppBar()
}