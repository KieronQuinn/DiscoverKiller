package com.kieronquinn.app.discoverkiller.settings.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.kieronquinn.app.discoverkiller.*
import com.kieronquinn.app.discoverkiller.settings.holders.DiscoverBehaviour
import com.kieronquinn.app.discoverkiller.settings.preferences.Preference
import com.kieronquinn.app.discoverkiller.settings.preferences.RadioButtonPreference
import com.kieronquinn.app.discoverkiller.settings.preferences.SwitchPreference
import com.kieronquinn.app.discoverkiller.utils.Links
import com.kieronquinn.app.discoverkiller.utils.XposedUtils
import dev.chrisbanes.insetter.applySystemGestureInsetsToPadding

class SettingsFragment : PreferenceFragmentCompat() {

    private val scrollListener = object: RecyclerView.OnScrollListener() {
        private var isElevationEnabled = false

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val newState = recyclerView.computeVerticalScrollOffset() > 0
            if(isElevationEnabled != newState) {
                setToolbarElevationEnabled(newState)
                isElevationEnabled = newState
            }
        }
    }

    private val swipePreference by lazy {
        findPreference<SwitchPreference>("option_enable_swipe")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        setToolbarElevationEnabled(false)
        val radioButtons = arrayOf(
            findPreference<RadioButtonPreference>("behaviour_google_updates"),
            findPreference("behaviour_custom_app"),
            findPreference("behaviour_none")
        )
        when(context?.getSelectedBehaviour() ?: DiscoverBehaviour.UPDATES){
            DiscoverBehaviour.UPDATES -> radioButtons.find { it?.key == "behaviour_google_updates" }?.isChecked = true
            DiscoverBehaviour.CUSTOM_APP -> radioButtons.find { it?.key == "behaviour_custom_app" }?.isChecked = true
            DiscoverBehaviour.NONE -> radioButtons.find { it?.key == "behaviour_none" }?.isChecked = true
        }
        radioButtons.forEach {
            when(it?.key){
                "behaviour_custom_app" -> {
                    it.clearButtons()
                    it.clearButtonsList()
                    it.addButton(getPackageLabel(context?.getCustomApp()) ?: getString(R.string.button_select_app), R.drawable.ic_select_app, R.color.colorAccent){
                        getNavController()?.navigate(R.id.action_settingsFragment_to_appsFragment)
                    }
                    it.setOnPreferenceClickListener { preference ->
                        onRadioButtonSelected(preference.key, radioButtons)
                        true
                    }
                }
                else -> {
                    it?.setOnPreferenceClickListener { preference ->
                        onRadioButtonSelected(preference.key, radioButtons)
                        true
                    }
                }
            }
        }
        swipePreference?.run {
            isChecked = context?.getIsSwipeEnabled() ?: false
            isEnabled = context?.getSelectedBehaviour() == DiscoverBehaviour.UPDATES
            setOnPreferenceChangeListener { preference, newValue ->
                context?.setSwipeEnabled(newValue as Boolean)
                true
            }
        }
        findPreference<Preference>("module_status")?.run {
            summary = when{
                XposedUtils.isEdXposedModuleActive() -> getString(R.string.module_status_desc_enabled)
                XposedUtils.isEdXposedInstalled(context) -> getString(R.string.module_status_desc_disabled)
                else -> getString(R.string.module_status_desc_no_xposed)
            }
            icon = when{
                XposedUtils.isEdXposedModuleActive() -> ContextCompat.getDrawable(context, R.drawable.ic_module_check)
                XposedUtils.isEdXposedInstalled(context) -> ContextCompat.getDrawable(context, R.drawable.ic_module_error)
                else -> ContextCompat.getDrawable(context, R.drawable.ic_module_cross)
            }
            if(XposedUtils.isEdXposedInstalled(context)){
                setOnPreferenceClickListener {
                    startActivity(XposedUtils.getEdXposedLaunchIntent(context))
                    true
                }
            }
        }
        setFragmentResultListener(AppsFragment.KEY_SELECTED_APP){ key, bundle ->
            if(key == AppsFragment.KEY_SELECTED_APP){
                val selectedApp = bundle.getString(AppsFragment.KEY_SELECTED_APP) ?: ""
                val packageManager = context?.packageManager
                val applicationInfo = packageManager?.getApplicationInfo(selectedApp, 0)
                radioButtons.find { it?.key == "behaviour_custom_app" }?.getButton(0)?.run {
                    text = applicationInfo?.loadLabel(packageManager)?.toString()?.concat(150f) ?: getString(R.string.button_select_app)
                }
                context?.setCustomApp(selectedApp)
            }
        }
        findPreference<Preference>("about_about")?.apply {
            title = getString(R.string.about, getString(R.string.app_name), BuildConfig.VERSION_NAME)
        }
        findPreference<Preference>("about_libraries")?.apply {
            setOnPreferenceClickListener {
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.libraries))
                true
            }
        }
        context?.let { context ->
            Links.setupPreference(context, preferenceScreen, "about_github", Links.LINK_GITHUB)
            Links.setupPreference(context, preferenceScreen, "about_xda", Links.LINK_XDA)
            Links.setupPreference(context, preferenceScreen, "about_donate", Links.LINK_DONATE)
            Links.setupPreference(context, preferenceScreen, "about_twitter", Links.LINK_TWITTER)
        }
    }

    override fun onResume() {
        super.onResume()
        listView.addOnScrollListener(scrollListener)
        listView.post {
            setToolbarElevationEnabled(listView.computeVerticalScrollOffset() > 0)
            listView.clipToPadding = false
            listView.applySystemGestureInsetsToPadding(bottom = true)
        }
    }

    override fun onPause() {
        super.onPause()
        listView.removeOnScrollListener(scrollListener)
    }

    private fun onRadioButtonSelected(key: String, radioButtons: Array<RadioButtonPreference?>){
        when(key){
            "behaviour_google_updates" -> context?.setSelectedBehaviour(DiscoverBehaviour.UPDATES)
            "behaviour_custom_app" -> context?.setSelectedBehaviour(DiscoverBehaviour.CUSTOM_APP)
            "behaviour_none" -> context?.setSelectedBehaviour(DiscoverBehaviour.NONE)
        }
        radioButtons.forEach {
            it?.isChecked = false
        }
        radioButtons.find { it?.key == key }?.isChecked = true
        swipePreference?.isEnabled = key == "behaviour_google_updates"
    }

    private fun getPackageLabel(packageName: String?): String? {
        val packageManager = context?.packageManager
        packageManager?.let {
            return try {
                it.getApplicationInfo(packageName, 0).loadLabel(it).toString()
            }catch (e: PackageManager.NameNotFoundException){
                null
            }
        }
        return null
    }

}