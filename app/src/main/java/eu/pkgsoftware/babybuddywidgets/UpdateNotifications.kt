package eu.pkgsoftware.babybuddywidgets

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

class UpdateNotifications() {
    companion object {
        fun versionLessThanEqualTest(a: String, b: String): Boolean {
            val aSplits = a.split(".")
            val bSplits = b.split(".")

            var i = 0
            while ((i < aSplits.size) && (i < bSplits.size)) {
                val av = aSplits[i]
                val bv = bSplits[i]

                val avi = av.toIntOrNull()
                val bvi = bv.toIntOrNull()
                if ((avi != null) && (bvi != null)) {
                    if (avi > bvi) {
                        return false
                    }
                } else {
                    if (av > bv) {
                        return false
                    }
                }

                i++
            }
            return true
        }

        fun showUpdateNotice(baseFragment: BaseFragment) {
            val credStore = baseFragment.mainActivity.credStore
            if (credStore.isStoredVersionOutdated) {
                val testVersion =
                    baseFragment.resources.getString(R.string.upgrade_version_less_than_test)
                if (!versionLessThanEqualTest(credStore.CURRENT_VERSION, testVersion)) {
                    credStore.updateStoredVersion()
                    return
                }

                baseFragment.runAfterDialog {
                    credStore.updateStoredVersion()
                    AlertDialog.Builder(baseFragment.requireContext())
                        .setTitle(R.string.upgrade_latest_info_title)
                        .setMessage(R.string.upgrade_latest_info_note)
                        .setPositiveButton(R.string.dialog_ok) { dialog, which ->
                            dialog.dismiss()
                        }
                        .create().show()
                }
            }
        }
    }
}