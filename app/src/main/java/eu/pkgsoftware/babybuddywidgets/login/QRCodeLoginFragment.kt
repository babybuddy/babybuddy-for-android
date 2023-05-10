package eu.pkgsoftware.babybuddywidgets.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.clearSpans
import androidx.core.text.set
import androidx.core.text.toSpannable
import androidx.navigation.Navigation.findNavController
import com.squareup.phrase.Phrase
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.databinding.QrCodeLoginFragmentBinding


const val CLEAR_DELAY_MS = 2000

class QRCodeLoginFragment : BaseFragment() {
    var qrCode: QRCode? = null
    lateinit var handler: Handler
    lateinit var binding: QrCodeLoginFragmentBinding

    val menu = LoggedOutMenu(this)

    var lastClearCodeTime = System.currentTimeMillis()

    val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initQRReader()
        } else {
            navigateBack(noCamAccess = true)
        }
    }

    var heldLoginData: LoginData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = QrCodeLoginFragmentBinding.inflate(inflater)
        binding.foundLoginCodeGroup.visibility = View.GONE
        binding.qrcodeCancelButton.setOnClickListener {
            heldLoginData = null
            binding.status.visibility = View.VISIBLE
            binding.foundLoginCodeGroup.visibility = View.GONE
        }
        binding.qrcodeLoginButton.setOnClickListener {
            heldLoginData?.also {
                performLogin(it)
            } ?: {
                binding.qrcodeCancelButton.performClick()
            }
        }
        return binding.root
    }

    private fun clearQRCodeMessage() {
        if (view == null) {
            return
        }
        handler.postDelayed({ clearQRCodeMessage() }, CLEAR_DELAY_MS / 10L)

        if (System.currentTimeMillis() - lastClearCodeTime >= CLEAR_DELAY_MS) {
            binding.status.setText(R.string.login_qrcode_status_no_code)
        }
    }

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initQRReader()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.CAMERA
                )
            ) {
                showQuestion(
                    true,
                    getString(R.string.login_qrcode_camera_access_needed_dialog_title),
                    getString(R.string.login_qrcode_camera_access_needed_dialog_body),
                    getString(R.string.login_qrcode_camera_access_needed_dialog_confirm),
                    getString(R.string.login_qrcode_camera_access_needed_dialog_cancel)
                ) {
                    if (it) {
                        permissionRequest.launch(Manifest.permission.CAMERA)
                    } else {
                        navigateBack()
                    }
                }
            } else {
                permissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler = Handler(mainActivity.mainLooper)
        clearQRCodeMessage()

        hideKeyboard()
        mainActivity.setTitle(getString(R.string.login_qrcode_title))
        mainActivity.enableBackNavigationButton(true)

        requireActivity().addMenuProvider(menu)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menu)
    }

    override fun onStop() {
        super.onStop()
        qrCode?.close()
        qrCode = null
    }

    private fun initQRReader() {
        val qrCode = QRCode(this, binding.preview)
        qrCode.codeDetectedCallback = Runnable {
            if (qrCode.detectedCodes.size > 0) {
                val code = qrCode.detectedCodes[0]
                lastClearCodeTime = System.currentTimeMillis()
                selectValidQRCode(code)
            }
            qrCode.detectedCodes.clear()
        }
        this.qrCode = qrCode
    }

    private fun selectValidQRCode(code: String): Boolean {
        val loginData: LoginData
        try {
            loginData = LoginData.fromQrcodeJSON(code)
        } catch (e: InvalidQRCodeException) {
            binding.status.setText(R.string.login_qrcode_status_invalid_code)
            return false
        }

        if (heldLoginData != null) {
            // Kind of a "trick" to short-cut the update to the text
            binding.status.setText(R.string.login_qrcode_status_no_code)
            return false
        }

        binding.status.setText(R.string.login_qrcode_status_valid_code)

        heldLoginData = loginData

        val linkDescription =
            Phrase.from(this.mainActivity, R.string.login_qrcode_feedback_template)
                .put("url", loginData.url)
                .format().toSpannable()

        val spanList = linkDescription.getSpans(
            0,
            linkDescription.length,
            URLSpan::class.javaObjectType
        ).toList()
        val spanRanges = spanList.map {
            IntRange(linkDescription.getSpanStart(it), linkDescription.getSpanEnd(it))
        }
        linkDescription.clearSpans()

        for (range in spanRanges) {
            linkDescription.set(range, URLSpan(loginData.url))
        }

        binding.foundLoginCodeFeedback.setText(linkDescription)
        binding.foundLoginCodeFeedback.movementMethod = LinkMovementMethod.getInstance()

        binding.status.setText(R.string.login_qrcode_status_no_code)
        binding.status.visibility = View.GONE
        binding.foundLoginCodeGroup.visibility = View.VISIBLE

        return true
    }

    private fun navigateBack(noCamAccess: Boolean? = null) {
        val controller = findNavController(requireView())
        if (noCamAccess == null) {
            controller.navigateUp()
        } else {
            val b = Bundle()
            b.putBoolean("noCameraAccess", noCamAccess)
            controller.navigate(R.id.action_QRCodeLoginFragment_to_LoginFragment_onFailure, b)
        }
    }

    private fun performLogin(loginData: LoginData) {
        Utils(mainActivity).httpCleaner(loginData.url, object : Promise<String, Any> {
            override fun succeeded(s: String?) {
                showProgress(getString(R.string.logging_in_message))

                val credStore = mainActivity.credStore;
                credStore.storeServerUrl(loginData.url)
                credStore.storeAppToken(loginData.token)

                Utils(mainActivity).testLoginToken(object : Promise<Any, String> {
                    override fun succeeded(s: Any?) {
                        progressDialog.hide()
                        moveToLoggedIn()
                    }

                    override fun failed(s: String) {
                        progressDialog.hide()
                        credStore.storeAppToken(null)
                        binding.qrcodeCancelButton.performClick()
                        showError(true, "Login failed", s)
                    }
                })
            }

            override fun failed(f: Any?) {
                binding.qrcodeCancelButton.performClick()
            }
        })
    }

    private fun moveToLoggedIn() {
        val controller = findNavController(requireView())
        controller.navigate(R.id.action_QRCodeLoginFragment_to_loggedInFragment2)
    }
}