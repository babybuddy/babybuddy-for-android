package eu.pkgsoftware.babybuddywidgets.login

import android.app.AlertDialog
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.MainActivity
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.RequestCallback
import java.util.Locale

class Utils(val mainActivity: MainActivity) {
    fun httpCleaner(address: String, promise: Promise<String, Any>) {
        var cleanedAddress = ("" + address).trim { it <= ' ' }
        if (cleanedAddress.lowercase(Locale.getDefault()).startsWith("http:")) {
            AlertDialog.Builder(mainActivity)
                .setTitle(R.string.login_http_warning_title)
                .setMessage(R.string.login_http_warning_message)
                .setPositiveButton(
                    R.string.login_http_warning_cancel
                ) { dialogInterface, i ->
                    dialogInterface.dismiss()
                    promise.failed(Object())
                }
                .setNegativeButton(
                    R.string.login_http_warning_continue_login
                ) { dialogInterface, i ->
                    dialogInterface.dismiss()
                    promise.succeeded(cleanedAddress)
                }.show()
        } else {
            if (!cleanedAddress.lowercase(Locale.getDefault()).startsWith("https:")) {
                cleanedAddress = "https://" + cleanedAddress;
            }
            promise.succeeded(cleanedAddress)
        }
    }

    fun testLoginToken(promise: Promise<Any, String>) {
        val credStore = CredStore(mainActivity.applicationContext)
        if (credStore.appToken != null) {
            val client = mainActivity.client
            client.listChildren(object : RequestCallback<Array<Child?>?> {
                override fun error(error: Exception) {
                    error.message?.let {
                        promise.failed(it)
                    } ?: {
                        promise.failed("Failed")
                    }
                }

                override fun response(r: Array<Child?>?) {
                    if (r == null || r.any { it == null }) {
                        promise.failed("Invalid children list")
                    } else {
                        mainActivity.children = r.filterNotNull().toTypedArray()
                        promise.succeeded(Any())
                    }
                }
            })
        } else {
            promise.failed("No app token found.")
        }
    }
}