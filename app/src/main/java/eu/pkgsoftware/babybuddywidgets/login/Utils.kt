package eu.pkgsoftware.babybuddywidgets.login

import android.app.AlertDialog
import android.content.DialogInterface
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.MainActivity
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.RequestCallback
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.utils.AsyncPromise
import eu.pkgsoftware.babybuddywidgets.utils.AsyncPromiseFailure
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    suspend fun showBetaWarningForHomeassistant(loginData: LoginData) {
        if (loginData.cookies.containsKey("ingress_session")) {
            suspendCoroutine<Any?> { cont ->
                AlertDialog.Builder(mainActivity)
                    .setTitle(R.string.login_qrcode_homeassistant_beta_warning_title)
                    .setMessage(R.string.login_qrcode_homeassistant_beta_warning_message)
                    .setPositiveButton(R.string.login_qrcode_homeassistant_beta_warning_continue) { dialogInterface, i ->
                        dialogInterface.dismiss()
                        cont.resume(null)
                    }.show()
            }
        }
    }

    suspend fun cleanLoginData(loginData: LoginData): LoginData? {
        return try {
            showBetaWarningForHomeassistant(loginData)
            val cleanedAddress = AsyncPromise.call<String, Any> {
                httpCleaner(loginData.url, it)
            }
            loginData.replace(cleanedAddress, null, null)
        } catch (e: AsyncPromiseFailure) {
            null
        }
    }

    fun testLoginToken(promise: Promise<Any, String>) {
        val credStore = CredStore(mainActivity.applicationContext)
        if (credStore.appToken != null) {
            val client = mainActivity.client
            client.listChildren(object : RequestCallback<Array<Child?>?> {
                override fun error(error: Exception) {
                    val message = error.message
                    if (message == null) {
                        if (error is RequestCodeFailure) {
                            if ((error.code >= 401) || (error.code < 600)) {
                                promise.failed("Authentication failed (http response: ${error.code})")
                                return
                            }
                        }
                        promise.failed("Failed")
                    } else {
                        promise.failed(message)
                    }
                }

                override fun response(r: Array<Child?>?) {
                    if (r == null || r.any { it == null }) {
                        promise.failed("Invalid children list")
                    } else {
                        promise.succeeded(Any())
                    }
                }
            })
        } else {
            promise.failed("No app token found.")
        }
    }

    companion object {
        fun padToLen(s: String, c: Char, length: Int): String {
            val sBuilder = StringBuilder(s)
            while (sBuilder.length < length) {
                sBuilder.insert(0, c)
            }
            return sBuilder.toString()
        }
    }
}