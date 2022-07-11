package com.kieronquinn.app.discoverkiller.ui.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.blur.BlurProvider
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import org.koin.android.ext.android.inject

//Seems to be a lint bug
@SuppressLint("MissingSuperCall")
class SettingsBackgroundPickerActivity: MonetCompatActivity() {

    private val blurProvider by inject<BlurProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        super.onCreate(savedInstanceState)
        setupStatusNav()
        setContentView(R.layout.activity_settings_background_picker)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post {
            blurProvider.applyBlurToWindow(window, 1f)
        }
    }

    private fun setupStatusNav(){
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.post {
            WindowInsetsControllerCompat(window, window.decorView).run {
                isAppearanceLightNavigationBars = !isDarkMode
                isAppearanceLightStatusBars = !isDarkMode
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

}