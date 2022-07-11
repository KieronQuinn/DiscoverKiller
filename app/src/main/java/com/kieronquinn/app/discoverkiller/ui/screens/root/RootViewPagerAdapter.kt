package com.kieronquinn.app.discoverkiller.ui.screens.root

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class RootViewPagerAdapter(
    fragment: RootFragment,
    private val overlayFragment: Fragment,
    private val contentFragment: Fragment
): FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> overlayFragment
            1 -> contentFragment
            else -> throw RuntimeException("Illegal position $position")
        }
    }

    override fun getItemCount() = 2

}