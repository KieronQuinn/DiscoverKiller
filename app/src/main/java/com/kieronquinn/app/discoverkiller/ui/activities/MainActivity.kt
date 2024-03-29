package com.kieronquinn.app.discoverkiller.ui.activities

import android.os.Bundle
import android.view.View
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.notifications.requestNotificationPermissionIfNeeded
import com.kieronquinn.app.discoverkiller.utils.extensions.delayPreDrawUntilFlow
import com.kieronquinn.app.discoverkiller.work.UpdateWorker
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : MonetCompatActivity() {

    companion object {
        const val EXTRA_SKIP_SPLASH = "SKIP_SPLASH"
    }

    override val applyBackgroundColorToWindow = true

    private val viewModel by viewModel<MainActivityViewModel>()

    private val splashDelay by lazy {
        viewModel.startDestination.filterNotNull().map { true }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if(!intent.getBooleanExtra(EXTRA_SKIP_SPLASH, false)) {
            findViewById<View>(android.R.id.content).delayPreDrawUntilFlow(splashDelay, lifecycle)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        UpdateWorker.queueWorker(this)
        requestNotificationPermissionIfNeeded()
        lifecycleScope.launchWhenCreated {
            monet.awaitMonetReady()
            setContentView(R.layout.activity_main)
        }
    }

}