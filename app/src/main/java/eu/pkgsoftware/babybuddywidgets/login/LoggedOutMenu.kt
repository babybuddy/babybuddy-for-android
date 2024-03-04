package eu.pkgsoftware.babybuddywidgets.login

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import eu.pkgsoftware.babybuddywidgets.R

class LoggedOutMenu(val fragment: Fragment) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.loggedout_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.getItemId() == R.id.aboutPageMenuItem) {
            Navigation.findNavController(fragment.requireView()).navigate(R.id.global_aboutFragment)
        }
        if (menuItem.getItemId() == R.id.showHelpMenuButton) {
            Navigation.findNavController(fragment.requireView()).navigate(R.id.global_showHelp)
        }
        if (menuItem.getItemId() == R.id.contactDeveloperMenuItem) {
            Navigation.findNavController(fragment.requireView()).navigate(R.id.action_global_contactDeveloperFragment)
        }
        return false
    }
}