package eu.pkgsoftware.babybuddywidgets

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation.findNavController
import eu.pkgsoftware.babybuddywidgets.databinding.QrCodeLoginFragmentBinding
import eu.pkgsoftware.babybuddywidgets.login.QRCode

class QRCodeLoginFragment : BaseFragment() {
    var qrCode: QRCode? = null

    lateinit var binding: QrCodeLoginFragmentBinding

    val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initQRReader()
        } else {
            navigateBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = QrCodeLoginFragmentBinding.inflate(inflater)
        return binding.root
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
                    "Camera access needed",
                    "In order to us the QR-Code scanner, the app requires access to your " +
                            "device's camera.\n\nAlternatively, you can choose to use the " +
                            "login screen and provide server, username, and password manually " +
                            "instead.",
                    "Give permission",
                    "Cancel"
                ) {
                    if (true) {
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

    private fun initQRReader() {
        qrCode = QRCode(this, binding.preview)
    }

    private fun navigateBack() {
        findNavController(requireView()).navigateUp()
    }

    companion object {
        @JvmStatic
        fun newInstance() = QRCodeLoginFragment()
    }
}