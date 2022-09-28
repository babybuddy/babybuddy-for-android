package eu.pkgsoftware.babybuddywidgets

import androidx.appcompat.app.AppCompatActivity
import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import android.os.Bundle
import android.view.View
import eu.pkgsoftware.babybuddywidgets.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    var credStore: CredStore? = null
        get() {
            if (field == null) {
                field = CredStore(applicationContext)
            }
            return field
        }
        private set

    var client: BabyBuddyClient? = null
        get() {
            if (field == null) {
                field = BabyBuddyClient(mainLooper, credStore)
            }
            return field
        }
        private set

    @JvmField
    var children = arrayOf<Child>()
    @JvmField
    var selectedTimer: BabyBuddyClient.Timer? = null

    fun setTitle(title: String?) {
        supportActionBar!!.title = title
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
}