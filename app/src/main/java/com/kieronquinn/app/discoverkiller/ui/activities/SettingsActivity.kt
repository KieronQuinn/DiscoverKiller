package com.kieronquinn.app.discoverkiller.ui.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import com.topjohnwu.superuser.Shell

//Seems to be a lint bug
@SuppressLint("MissingSuperCall")
class SettingsActivity: MonetCompatActivity() {

    init {
        // Set settings before the main shell can be created
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    override val applyBackgroundColorToMenu = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Only need to use Splash compat on < S
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_settings)
    }

}