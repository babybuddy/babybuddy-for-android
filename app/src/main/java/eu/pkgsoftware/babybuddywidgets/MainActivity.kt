package eu.pkgsoftware.babybuddywidgets

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import eu.pkgsoftware.babybuddywidgets.databinding.ActivityMainBinding
import java.lang.Exception
import java.util.*

interface StoreFunction<X> : BabyBuddyClient.RequestCallback<X> {
    fun store(timer: BabyBuddyClient.Timer, callback: BabyBuddyClient.RequestCallback<X>)
    fun name(): String
    fun timerStopped()
    fun cancel()
}


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    internal var internalCredStore: CredStore? = null
    val credStore: CredStore
        get() {
            internalCredStore.let {
                if (it == null) {
                    val newCredStore = CredStore(applicationContext)
                    internalCredStore = newCredStore
                    return newCredStore
                } else {
                    return it
                }
            }
        }

    internal var internalClient: BabyBuddyClient? = null
    val client: BabyBuddyClient
        get() {
            internalClient.let {
                if (it == null) {
                    val newClient = BabyBuddyClient(
                        mainLooper,
                        credStore
                    )
                    internalClient = newClient
                    return newClient
                } else {
                    return it
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(
            layoutInflater
        ).let {
            setContentView(it.root)
            setSupportActionBar(it.toolbar)
            it.toolbar.setNavigationOnClickListener { view: View? -> }
            it.toolbar.navigationIcon = null
            it
        }
    }

    @JvmField
    var children = arrayOf<Child>()
    @JvmField
    var selectedTimer: BabyBuddyClient.Timer? = null

    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    inner class StoreActivity<X>(val timer: BabyBuddyClient.Timer, val storeInterface: StoreFunction<X>) {
        val localCallbacks = object : BabyBuddyClient.RequestCallback<X> {
            override fun error(error: Exception?) {
                // Check if there is an overlapping entry
                // - if overlap is found, offer option to resolve
                // - if no overlap is found, fail for good

                val endDate: Date = timer.computeCurrentServerEndTime(client)
                val listCommandCallback = object : BabyBuddyClient.RequestCallback<String> {
                    override fun error(error: Exception?) {
                        storeInterface.error(error);
                    }

                    override fun response(response: String) {
                        AlertDialog.Builder(this@MainActivity)
                            // !STRINGCOLLECT! Remove strings
                            .setTitle(R.string.conflicting_activity_title)
                            .setMessage(R.string.conflicting_activity_text)
                            .setCancelable(false)
                            .setPositiveButton(R.string.conflicting_activity_modify_option) {
                                    dialogInterface: DialogInterface, i: Int ->
                            }
                            .setNeutralButton(R.string.conflicting_activity_cancel_option) {
                                    dialogInterface: DialogInterface, i: Int ->
                                storeInterface.cancel()
                            }
                            .setNegativeButton(R.string.conflicting_activity_stop_timer_option) {
                                    dialogInterface: DialogInterface, i: Int ->
                                val callback = object : BabyBuddyClient.RequestCallback<Boolean> {
                                    override fun error(error: Exception?) {
                                        storeInterface.error(error)
                                    }

                                    override fun response(response: Boolean?) {
                                        storeInterface.timerStopped()
                                    }
                                }

                                client.setTimerActive(timer.id, false, callback);
                            }
                            .show();

                    }
                }
                client.listGeneric(
                    storeInterface.name(),
                    BabyBuddyClient.Filters()
                        .add("start_max", timer.start)
                        .add("end_min", endDate)
                        .add("limit", 10),
                    listCommandCallback
                )
            }

            override fun response(response: X?) {
                storeInterface.response(response)
            }
        }

        init {
            storeInterface.store(timer, this.localCallbacks);
        }
    }

    fun storeActivity(timer: BabyBuddyClient.Timer, storeInterface: StoreFunction<R>) {
    }
}