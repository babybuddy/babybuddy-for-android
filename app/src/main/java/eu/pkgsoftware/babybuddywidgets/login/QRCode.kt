package eu.pkgsoftware.babybuddywidgets.login

import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.zxingcpp.BarcodeReader
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import java.lang.Exception

class QRCode(
    val fragment: BaseFragment,
    private val previewFrame: PreviewView?,
    private val checkOnly: Boolean = false
) {
    val bcr = BarcodeReader()

    var hasCamera = false
    var cameraReady = false
    var cameraOnInitialized: Runnable? = null

    val detectedCodes = mutableListOf<String>()
    var codeDetectedCallback: Runnable? = null

    private val selector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()
    private val imageCap = ImageAnalysis.Builder()
        .setImageQueueDepth(1)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()
    private val previewCap = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .setTargetRotation(Surface.ROTATION_0)
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
                cameraOnInitialized?.run()
                e.printStackTrace()
                return@Runnable
            }
            if (camProvider.hasCamera(selector)) {
                hasCamera = true
                initCamera()
            } else {
                cameraOnInitialized?.run()
            }
        }, ContextCompat.getMainExecutor(fragment.requireContext()))
    }

    private fun initCamera() {
        cam = camProvider.bindToLifecycle(fragment, selector, imageCap, previewCap)
        cameraReady = true
        cameraOnInitialized?.run()
        if (checkOnly) {
            close()
            return
        }

        previewFrame?.let {
            previewCap.setSurfaceProvider(it.surfaceProvider)
        }

        imageCap.setAnalyzer(
            ContextCompat.getMainExecutor(fragment.requireContext())
        ) { image ->
            bcr.read(image)?.let { qrResult ->
                qrResult.text?.let {
                    if (!detectedCodes.contains(it)) {
                        detectedCodes.add(it)
                        codeDetectedCallback?.run()
                    }
                }
            }
        }

        cameraOnInitialized?.run()
    }

    fun close() {
        if (cameraReady) {
            cameraReady = false
            camProvider.unbindAll()
        }
    }
}