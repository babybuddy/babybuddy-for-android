package eu.pkgsoftware.babybuddywidgets.login

import androidx.navigation.Navigation
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
        val view = fragment.view
        if (view == null) {
            return false
        }
        val navController = findNavController(view)
        if (menuItem.getItemId() == R.id.logoutMenuButton) {
            fragment.mainActivity.logout()
            navController.navigate(R.id.logoutOperation)
        }
        if (menuItem.getItemId() == R.id.showHelpMenuButton) {
            navController.navigate(R.id.global_showHelp)
        }
        if (menuItem.getItemId() == R.id.aboutPageMenuItem) {
            navController.navigate(R.id.global_aboutFragment)
        }
        if (menuItem.getItemId() == R.id.exportDebugLogsMenuItem) {
            navController.navigate(R.id.action_global_debugLogDisplay)
        }
        if (menuItem.getItemId() == R.id.contactDeveloperMenuItem) {
            navController.navigate(R.id.action_global_contactDeveloperFragment)
        }
        return false
    }
}