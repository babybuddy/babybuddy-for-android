package eu.pkgsoftware.babybuddywidgets

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.core.text.toSpanned
import androidx.navigation.Navigation.findNavController
import com.squareup.phrase.Phrase
import eu.pkgsoftware.babybuddywidgets.databinding.QrCodeLoginFragmentBinding
import eu.pkgsoftware.babybuddywidgets.login.InvalidQRCodeException
import eu.pkgsoftware.babybuddywidgets.login.LoginData
import eu.pkgsoftware.babybuddywidgets.login.QRCode


const val CLEAR_DELAY_MS = 2000

class QRCodeLoginFragment : BaseFragment() {
    var qrCode: QRCode? = null
    lateinit var handler: Handler
    lateinit var binding: QrCodeLoginFragmentBinding

    var lastClearCodeTime = System.currentTimeMillis()

    val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initQRReader()
        } else {
            navigateBack()
        }
    }

    var heldLoginData: LoginData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = QrCodeLoginFragmentBinding.inflate(inflater)
        binding.foundLoginCodeGroup.visibility = View.GONE
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
                if (selectValidQRCode(code)) {
                    binding.status.setText(R.string.login_qrcode_status_valid_code)
                } else {
                    binding.status.setText(R.string.login_qrcode_status_invalid_code)
                }
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
            return false
        }

        if (heldLoginData == null) {
            heldLoginData = loginData
            binding.status.visibility = View.GONE
            binding.foundLoginCodeFeedback.setText(
                Phrase.from(this.mainActivity, R.string.login_qrcode_feedback_template)
                    .put("url", loginData.url)
                    .format().to
            )
            binding.foundLoginCodeGroup.visibility = View.VISIBLE
        }

        return true
    }

    private fun navigateBack() {
        findNavController(requireView()).navigateUp()
    }

    companion object {
        @JvmStatic
        fun newInstance() = QRCodeLoginFragment()
    }
}