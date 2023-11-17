package eu.pkgsoftware.babybuddywidgets.login

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.Navigation.findNavController
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.tutorial.DismissedCallback
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.databinding.LoginFragmentBinding
import eu.pkgsoftware.babybuddywidgets.utils.AsyncPromise
import eu.pkgsoftware.babybuddywidgets.utils.AsyncPromiseFailure
import eu.pkgsoftware.babybuddywidgets.utils.CancelParallel
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.utils.RunOnceAfterLayoutUpdate
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL

class LoginFragment : BaseFragment() {
    private lateinit var binding: LoginFragmentBinding
    private lateinit var loginButton: Button
    private lateinit var addressEdit: EditText
    private lateinit var loginNameEdit: EditText
    private lateinit var loginPasswordEdit: EditText

    private val menu = LoggedOutMenu(this)
    private val cancelParallelLogin = CancelParallel()

    private fun updateLoginButton() {
        loginButton.isEnabled =
            addressEdit.text.length > 0 && loginNameEdit.text.length > 0 && loginPasswordEdit.text.length > 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LoginFragmentBinding.inflate(inflater, container, false)
        val view: View = binding.root
        loginButton = view.findViewById(R.id.loginButton)
        addressEdit = view.findViewById(R.id.serverAddressEdit)
        loginNameEdit = view.findViewById(R.id.loginNameEdit)
        loginPasswordEdit = view.findViewById(R.id.passwordEdit)
        val tw: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                updateLoginButton()
            }
        }
        val credStore = mainActivity.credStore
        var serverUrl = credStore.serverUrl
        if (serverUrl == null) {
            serverUrl = ""
            if (isTestlab) {
                serverUrl = "https://babybuddy-test.pkgsoftware.eu/"
            }
        }
        addressEdit.setText(serverUrl)
        addressEdit.addTextChangedListener(tw)
        loginNameEdit.addTextChangedListener(tw)
        loginPasswordEdit.addTextChangedListener(tw)
        loginButton.setOnClickListener(View.OnClickListener { view1: View? -> uiStartLogin() })
        binding.passwordEdit.setText("")
        binding.loginNameEdit.setText("")
        binding.passwordEdit.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                uiStartLogin()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.loginInfoText.movementMethod = LinkMovementMethod.getInstance()
        binding.qrCode.setOnClickListener { view1: View? ->
            val controller = findNavController(requireView())
            controller.navigate(R.id.action_LoginFragment_to_QRCodeLoginFragment)
        }
        binding.qrCode.isEnabled = false
        updateLoginButton()
        val mainLayout = mainActivity.findViewById<View>(R.id.main_layout)
        mainLayout.requestLayout()
        RunOnceAfterLayoutUpdate(mainLayout, Runnable {
            val r = Rect()
            val hintPresentedCount = credStore.getTutorialParameter("help_hint")
            if (hintPresentedCount >= 2) {
                return@Runnable
            }
            val mainAct = mainActivity
                ?: return@Runnable   // Hotfix -  can happen when the app restarts from resume-state if the fragment deactivates.
            val toolbar = mainAct.findViewById<View>(R.id.app_toolbar)
            toolbar.getGlobalVisibleRect(r)
            val tutorialAccess = mainActivity.tutorialAccess
            tutorialAccess.tutorialMessage(
                (
                        r.right - dpToPx(20f)).toFloat(),
                r.top.toFloat(),
                getString(R.string.tutorial_help_1)
            )
            tutorialAccess.manuallyDismissedCallback = DismissedCallback {
                credStore.setTutorialParameter(
                    "help_hint",
                    hintPresentedCount + 1
                )
            }
        })
        return binding.root
    }

    private fun uiStartLogin() {
        hideKeyboard()
        Utils(mainActivity).httpCleaner(
            addressEdit.text.toString(),
            object : Promise<String, Any> {
                override fun succeeded(s: String) {
                    addressEdit!!.setText(s)
                    performLogin()
                }

                override fun failed(o: Any) {}
            }
        )
    }

    @get:SuppressLint("SetTextI18n")
    private val isTestlab: Boolean
        /**
         * Check if running in a testlab-context
         */
        private get() {
            var testlab = false
            val cr = mainActivity.contentResolver
            if (cr != null) {
                val testLabSetting = Settings.System.getString(cr, "firebase.test.lab")
                if ("true" == testLabSetting) {
                    testlab = true
                }
            }
            return testlab
        }

    private fun showProgress() {
        showProgress(getString(R.string.logging_in_message))
    }

    override fun onResume() {
        super.onResume()
        mainActivity.setTitle("Login to Baby Buddy")
        mainActivity.addMenuProvider(menu)
        val b = arguments
        if (b != null) {
            if (b.getBoolean("noCameraAccess", false)) {
                binding.errorBubble.flashMessage(
                    R.string.login_qrcode_camera_access_was_disabled_message,
                    5000
                )
            }
        }
        if (mainActivity.credStore.appToken != null) {
            progressDialog.hide()
            moveToLoggedIn()
        } else {
            val qrCode = QRCode(this, null, true)
            qrCode.cameraOnInitialized = Runnable {
                binding.qrCode.isEnabled = qrCode.hasCamera
                if (qrCode.hasCamera) {
                    binding.qrCodeInfoText.setText(R.string.login_qrcode_info_text)
                } else {
                    binding.qrCodeInfoText.setText(R.string.login_qrcode_info_text_no_camera)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mainActivity.removeMenuProvider(menu)
    }

    private suspend fun grabToken(asyncGrabber: AsyncGrabAppToken): String {
        asyncGrabber.login(loginNameEdit.text.toString(), loginPasswordEdit.text.toString())
        var result: String? = null
        result = asyncGrabber.fromProfilePage()
        if (result == null) {
            result = asyncGrabber.parseFromSettingsPage()
        }
        if (result == null) {
            throw java.lang.Exception("Token not found")
        } else {
            return result
        }
    }

    private fun performLogin() {
        mainActivity.scope.launch {
            showProgress()
            cancelParallelLogin.cancelParallel {
                val credStore = mainActivity.credStore
                credStore.storeServerUrl(addressEdit.text.toString())
                var token: String? = null
                token = try {
                    grabToken(AsyncGrabAppToken(URL(addressEdit.text.toString())))
                } catch (e: IOException) {
                    showError(true, "Login failed", e.message)
                    return@cancelParallel
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError(true, "Login failed", "Internal error message: " + e.message)
                    return@cancelParallel
                }
                credStore.storeAppToken(token)

                try {
                    AsyncPromise.call<Any, String> {
                        Utils(mainActivity).testLoginToken(it)
                    }
                }
                catch (e: AsyncPromiseFailure) {
                    credStore.storeAppToken(null)
                    showError(true, "Login failed", e.value.toString())
                    return@cancelParallel
                }
                moveToLoggedIn()
            }
        }.invokeOnCompletion {
            progressDialog.hide()
        }
    }

    private fun moveToLoggedIn() {
        val controller = findNavController(requireView())
        controller.navigate(R.id.action_LoginFragment_to_loggedInFragment2)
    }
}