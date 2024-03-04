package eu.pkgsoftware.babybuddywidgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import eu.pkgsoftware.babybuddywidgets.databinding.FragmentContactDeveloperBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.Arrays
import java.util.Properties


class ContactDeveloperFragment : BaseFragment() {
    lateinit var binding: FragmentContactDeveloperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactDeveloperBinding.inflate(inflater, container, false)
        binding.contactDeveloperBackButton.setOnClickListener {
            navigateBack()
        }
        binding.contactDeveloperSendButton.setOnClickListener {
            val job = mainActivity.scope.launch(Dispatchers.Main) {
                val sent = sendMessage()
                if (sent) {
                    mainActivity.binding.globalSuccessBubble.flashMessage(
                        getString(R.string.send_message_sent_message)
                    )
                } else {
                    mainActivity.binding.globalErrorBubble.flashMessage(
                        getString(R.string.send_message_failed_message)
                    )
                }
                navigateBack()
            }
        }
        return binding.root
    }

    fun navigateBack() {
        Navigation.findNavController(requireView()).navigateUp()
    }

    suspend fun sendMessage(): Boolean {
        val message = binding.contactDeveloperMessageEditor.text.toString()
        val secret = loadSecret()
        val endpoint = "https://www.pkgsoftware.eu/endpoints/forwardappmessage.php"

        var outerException: Exception? = null
        coroutineScope {
            launch(Dispatchers.IO) {
                try {
                    val url = java.net.URL(endpoint)
                    val con = url.openConnection() as java.net.HttpURLConnection
                    con.requestMethod = "POST"
                    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    con.doOutput = true

                    val requestData = listOf(
                        "Authorization=" + URLEncoder.encode("Token $secret", "UTF-8"),
                        "message=" + URLEncoder.encode(message, "UTF-8"),
                        "app=bbapp",
                    ).joinToString("&")
                    con.outputStream.write(requestData.toByteArray(Charsets.UTF_8))
                    con.outputStream.close()

                    val responseCode = con.responseCode
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw IOException("HTTP error code: $responseCode")
                    }
                } catch (e: IOException) {
                    outerException = e
                }
            }
        }

        return outerException == null
    }

    fun loadSecret(): String {
        val SECRETS_FILE = "secrets.properties"
        val props = Properties()
        val files = requireContext().assets.list("") ?: return "not loaded"
        val hasSecrets = Arrays.asList(*files).contains(SECRETS_FILE)
        if (!hasSecrets) {
            return "not loaded"
        }
        val inStream = requireContext().assets.open("secrets.properties")
        props.load(inStream)
        return props.getProperty("MESSAGE_AUTH", "DEFAULT")
    }
}