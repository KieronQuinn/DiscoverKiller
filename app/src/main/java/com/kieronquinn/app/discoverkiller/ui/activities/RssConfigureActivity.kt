package com.kieronquinn.app.discoverkiller.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.navigation.RssNavigation
import com.kieronquinn.app.discoverkiller.components.navigation.setupWithNavigation
import com.kieronquinn.app.discoverkiller.utils.extensions.delayPreDrawUntilFlow
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import org.koin.android.ext.android.inject

class RssConfigureActivity: MonetCompatActivity() {

    private val navHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_rss) as NavHostFragment
    }

    private val navigation by inject<RssNavigation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Only need to use Splash compat on < S
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_rss_configure)
        setupNavigation()
    }

    private fun setupNavigation() {
        lifecycleScope.launchWhenResumed {
            navHostFragment.setupWithNavigation(navigation)
        }
    }

}