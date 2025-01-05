package eu.pkgsoftware.babybuddywidgets

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import eu.pkgsoftware.babybuddywidgets.login.Utils

class SettingsFragment : PreferenceFragmentCompat() {

    val changeListener =
        OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "setting_dark_light_mode") {
                mainActivity.applyLightDarkMode()
            }
        }

    val mainActivity: MainActivity
        get() = activity as MainActivity


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferences?.let { p ->
            if (p.getString("setting_dark_light_mode", "") == "") {
                p.edit().putString("setting_dark_light_mode", "system").commit()
            }
        }

        setPreferencesFromResource(R.xml.app_settings, rootKey)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.setTitle(getString(R.string.action_settings))
        mainActivity.enableBackNavigationButton(true)

        preferenceManager?.sharedPreferences?.let { p ->
            p.registerOnSharedPreferenceChangeListener(changeListener)
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceManager?.sharedPreferences?.let { p ->
            p.unregisterOnSharedPreferenceChangeListener(changeListener)
        }
    }
}
