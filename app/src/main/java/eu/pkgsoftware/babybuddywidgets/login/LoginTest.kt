package eu.pkgsoftware.babybuddywidgets.login

import eu.pkgsoftware.babybuddywidgets.BaseFragment.Promise
import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.MainActivity
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.RequestCallback

class LoginTest(val mainActivity: MainActivity) {
    fun test(promise: Promise<Any, String>) {
        val credStore = CredStore(mainActivity.applicationContext)
        if (credStore.appToken != null) {
            val client = mainActivity.client
            client.listChildren(object : RequestCallback<Array<Child?>?> {
                override fun error(error: Exception) {
                    promise.failed(error.message)
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