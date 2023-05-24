package eu.pkgsoftware.babybuddywidgets

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.squareup.phrase.Phrase

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
                val testVersion = baseFragment.resources.getString(R.string.upgrade_latest_info_version_less_than_or_equal)
                if (!versionLessThanEqualTest(credStore.CURRENT_VERSION, testVersion)) {
                    credStore.updateStoredVersion()
                    return
                }

                val title = Phrase.from(baseFragment.mainActivity, R.string.upgrade_latest_info_title)
                    .putOptional("version", credStore.CURRENT_VERSION)
                    .format()
                val note = Phrase.from(baseFragment.mainActivity, R.string.upgrade_latest_info_note)
                    .putOptional("version", credStore.CURRENT_VERSION)
                    .format()

                baseFragment.runAfterDialog {
                    credStore.updateStoredVersion()
                    AlertDialog.Builder(baseFragment.requireContext())
                        .setTitle(title)
                        .setMessage(note)
                        .setPositiveButton(R.string.dialog_ok) { dialog, which ->
                            dialog.dismiss()
                        }
                        .create().show()
                }
            }
        }
    }
}