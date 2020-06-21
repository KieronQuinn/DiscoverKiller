package com.kieronquinn.app.discoverkiller.settings

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.utils.AppIconRequestHandler
import com.squareup.picasso.Picasso
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.applySystemWindowInsetsToMargin
import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val navController = findNavController(R.id.nav_host_fragment)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            title = ""
        }
        val typedValue = TypedValue()
        if (theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
            val windowBackground = typedValue.data
            window.statusBarColor = windowBackground
            window.navigationBarColor = windowBackground
            if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES){
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            toolbar_title.text = destination.label
        }
        home.setOnClickListener {
            onBackPressed()
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navHostFragment?.childFragmentManager?.addOnBackStackChangedListener {
            if (navHostFragment.childFragmentManager.backStackEntryCount == 0) {
                home.visibility = View.GONE
            } else {
                home.visibility = View.VISIBLE
            }
        }
        window.navigationBarColor = Color.TRANSPARENT
        Insetter.setEdgeToEdgeSystemUiFlags(window.decorView, true)
        toolbar.applySystemWindowInsetsToMargin(top = true)
    }

    fun setToolbarElevationEnabled(enabled: Boolean){
        toolbar?.elevation = if(enabled) resources.getDimension(R.dimen.toolbar_elevation) else 0f
    }
}