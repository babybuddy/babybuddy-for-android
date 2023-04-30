package eu.pkgsoftware.babybuddywidgets.login

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zxingcpp.BarcodeReader
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import java.lang.Exception

class QRCode(val fragment: BaseFragment, val previewFrame: PreviewView?) {
    val bcr = BarcodeReader()

    var fullyInitialized = false
    var cameraReady = false

    val detectedCodes = mutableSetOf<String>()

    private val imageCap = ImageAnalysis.Builder()
        .setImageQueueDepth(1)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()
    private val previewCap = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()

    private lateinit var camProvider: ProcessCameraProvider
    private lateinit var cam: Camera

    init {
        bcr.options = BarcodeReader.Options(
            formats = setOf(BarcodeReader.Format.QR_CODE),
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
            tryDownscale = true
        )

        initCameraAccess()
    }

    private fun initCameraAccess() {
        val futureCamProvider = ProcessCameraProvider.getInstance(fragment.requireContext())
        futureCamProvider.addListener(Runnable {
            try {
                camProvider = futureCamProvider.get()
            } catch (e: Exception) {
                fullyInitialized = true
                e.printStackTrace()
                return@Runnable
            }
            initCamera()
        }, ContextCompat.getMainExecutor(fragment.requireContext()))
    }

    private fun initCamera() {
        val selector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        cam = camProvider.bindToLifecycle(fragment, selector, imageCap, previewCap)

        previewFrame?.let {
            previewCap.setSurfaceProvider(it.surfaceProvider)
        }

        imageCap.setAnalyzer(
            ContextCompat.getMainExecutor(fragment.requireContext())
        ) { image ->
            bcr.read(image)?.let { qrResult ->
                qrResult.text?.let {
                    detectedCodes.add(it)
                    System.out.println("AAA Detected: " + it)
                }
            }
        }
    }
}