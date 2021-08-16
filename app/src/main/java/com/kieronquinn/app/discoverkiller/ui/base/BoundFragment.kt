package com.kieronquinn.app.discoverkiller.ui.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.discoverkiller.components.navigation.Navigation
import com.kieronquinn.monetcompat.app.MonetFragment
import org.koin.android.ext.android.inject

//Seems to be a lint bug
@SuppressLint("MissingSuperCall")
abstract class BoundFragment<T : ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T) : MonetFragment() {

    private var _binding: T? = null
    internal val binding
        get() = _binding ?: throw NullPointerException("Binding cannot be accessed before onCreateView or after onDestroyView")

    internal val navigation by inject<Navigation>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}