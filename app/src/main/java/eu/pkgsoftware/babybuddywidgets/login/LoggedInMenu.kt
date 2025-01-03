package eu.pkgsoftware.babybuddywidgets.login

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.navigation.Navigation.findNavController
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject

class LoggedInMenu(val fragment: BaseFragment) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.loggedin_menu, menu)
        if (!GlobalDebugObject.ENABLED) {
            menu.findItem(R.id.exportDebugLogsMenuItem).isVisible = false
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val view = fragment.view ?: return false
        val navController = findNavController(view)
        if (menuItem.itemId == R.id.logoutMenuButton) {
            fragment.mainActivity.logout()
            navController.navigate(R.id.logoutOperation)
        }
        if (menuItem.itemId == R.id.showHelpMenuMenuItem) {
            navController.navigate(R.id.global_showHelp)
        }
        if (menuItem.itemId == R.id.aboutPageMenuItem) {
            navController.navigate(R.id.global_aboutFragment)
        }
        if (menuItem.itemId == R.id.exportDebugLogsMenuItem) {
            navController.navigate(R.id.action_global_debugLogDisplay)
        }
        if (menuItem.itemId == R.id.contactDeveloperMenuItem) {
            navController.navigate(R.id.action_global_contactDeveloperFragment)
        }
        if (menuItem.itemId == R.id.settingsMenuMenuItem) {
            navController.navigate(R.id.action_global_settingsFragment)
        }
        return false
    }
}