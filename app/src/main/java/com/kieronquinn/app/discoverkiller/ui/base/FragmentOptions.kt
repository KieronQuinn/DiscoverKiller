package com.kieronquinn.app.discoverkiller.ui.base

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

interface BackAvailable
interface LockCollapsed
interface NoToolbar

interface ProvidesBack {
    fun onBackPressed(): Boolean
}

interface ProvidesTitle {
    fun getTitle(): CharSequence?
}

interface ProvidesOverflow {
    fun inflateMenu(menuInflater: MenuInflater, menu: Menu)
    fun onMenuItemSelected(menuItem: MenuItem): Boolean
}