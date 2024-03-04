package eu.pkgsoftware.babybuddywidgets.networking

import android.app.ProgressDialog
import android.content.DialogInterface
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.ConnectingDialogInterface

class CoordinatedDisconnectDialog(val fragment: Fragment, val credStore: CredStore) {
    private val dialog = ProgressDialog(fragment.requireContext())
    private var uniqueCounter = 1
    private val progressTrackers = mutableMapOf<String, Long>()

    init {
        dialog.setMessage(fragment.resources.getString(R.string.disconnect_dialog_progress_message))
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.setCancelable(false)
        dialog.setButton(
            ProgressDialog.BUTTON_NEGATIVE,
            fragment.resources.getString(R.string.disconnect_dialog_logout)
        ) { dialogInterface: DialogInterface, i: Int ->
            credStore.clearLoginData()
            findNavController(fragment.requireView()).navigate(R.id.logoutOperation)
        }
    }

    val timeout: Long
        get() {
            if (progressTrackers.isEmpty()) {
                return 0L
            }
            return progressTrackers.values.max()
        }

    private inner class ConnectingDialogInterfaceImpl : ConnectingDialogInterface {
        val key = "interface-${uniqueCounter}"

        init {
            uniqueCounter++
        }

        override fun interruptLoading(): Boolean {
            return false
        }

        override fun showConnecting(currentTimeout: Long, error: Exception?) {
            progressTrackers[key] = currentTimeout
            updateDialog()
        }

        override fun hideConnecting() {
            progressTrackers.remove(key)
            updateDialog()
        }
    }

    fun getInterface(): ConnectingDialogInterface {
        return ConnectingDialogInterfaceImpl()
    }

    private fun updateDialog() {
        if (timeout > 0) {
            dialog.show()
        } else {
            dialog.hide()
        }
    }
}