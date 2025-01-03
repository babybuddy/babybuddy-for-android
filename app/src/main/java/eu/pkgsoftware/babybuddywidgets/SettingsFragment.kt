package eu.pkgsoftware.babybuddywidgets

import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {
    val mainActivity: MainActivity
        get() = activity as MainActivity


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferences?.let { p ->
            if (p.getString("setting_dark_light_mode", "") == "") {
                p.edit().putString("setting_dark_light_mode", "system").commit()
            }

            println("setting_dark_light_mode: ${p.getString("setting_dark_light_mode", "xxxx")}")
        }


        setPreferencesFromResource(R.xml.app_settings, rootKey)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.setTitle("Settings")
        mainActivity.enableBackNavigationButton(true)

    }
}
