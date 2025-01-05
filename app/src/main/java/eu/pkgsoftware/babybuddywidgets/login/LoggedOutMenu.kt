package eu.pkgsoftware.babybuddywidgets.login

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import eu.pkgsoftware.babybuddywidgets.R

class LoggedOutMenu(val fragment: Fragment) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.loggedout_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val view = fragment.view ?: return false
        val navController = findNavController(view)
        if (menuItem.itemId == R.id.aboutPageMenuItem) {
            navController.navigate(R.id.global_aboutFragment)
        }
        if (menuItem.itemId == R.id.showHelpMenuButton) {
            navController.navigate(R.id.global_showHelp)
        }
        if (menuItem.itemId == R.id.contactDeveloperMenuItem) {
            navController.navigate(R.id.action_global_contactDeveloperFragment)
        }
        return false
    }
}
