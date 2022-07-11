package com.kieronquinn.app.discoverkiller.ui.base

interface BaseContainerViewModel {
    fun onBackPressed()
    fun onParentBackPressed(): Boolean
}