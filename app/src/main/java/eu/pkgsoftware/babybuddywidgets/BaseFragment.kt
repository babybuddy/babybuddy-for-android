package eu.pkgsoftware.babybuddywidgets

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import eu.pkgsoftware.babybuddywidgets.Tools.dpToPx
import eu.pkgsoftware.babybuddywidgets.networking.CoordinatedDisconnectDialog
import eu.pkgsoftware.babybuddywidgets.tutorial.Trackable
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialEntry
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialManagement
import java.util.Locale

interface DialogCallback {
    fun call(b: Boolean)
}

abstract class BaseFragment : Fragment() {
    private var dialog: AlertDialog? = null
    private val noopDialogCallback = object : DialogCallback {
        override fun call(b: Boolean) {
        }
    }

    lateinit var disconnectDialog: CoordinatedDisconnectDialog
    lateinit var progressDialog: ProgressDialog

    fun showError(override: Boolean, title: Int, errorMessage: String?): AlertDialog? {
        return showError(
            override,
            getString(title),
            errorMessage,
            noopDialogCallback
        )
    }

    fun showError(override: Boolean, title: String?, errorMessage: Int): AlertDialog? {
        return showError(
            override,
            title,
            getString(errorMessage),
            noopDialogCallback)
    }

    fun showError(override: Boolean, title: Int, errorMessage: Int): AlertDialog? {
        return showError(
            override,
            getString(title),
            getString(errorMessage),
            noopDialogCallback)
    }

    @JvmOverloads
    fun showError(
        override: Boolean,
        title: String?,
        errorMessage: String?,
        callback: DialogCallback = noopDialogCallback
    ): AlertDialog? {
        if (override) {
            hideError()
        } else {
            if (dialog != null && dialog!!.isShowing) {
                return dialog
            }
        }
        if (dialog != null) {
            dialog!!.dismiss()
        }
        dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(errorMessage)
            .setPositiveButton(R.string.dialog_ok) { dialogInterface: DialogInterface?, i: Int ->
                hideError()
                callback.call(true)
            }
            .show()
        return dialog
    }

    fun showQuestion(
        override: Boolean,
        title: String?,
        question: String?,
        positiveMessage: String?,
        negativeMessage: String?,
        callback: DialogCallback
    ): AlertDialog? {
        if (override) {
            hideError()
        } else {
            if (dialog != null && dialog!!.isShowing) {
                return dialog
            }
        }
        if (dialog != null) {
            dialog!!.dismiss()
        }
        dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(question)
            .setCancelable(false)
            .setPositiveButton(positiveMessage) { dialogInterface: DialogInterface?, i: Int ->
                hideError()
                callback.call(true)
            }
            .setNegativeButton(negativeMessage) { dialogInterface: DialogInterface?, i: Int ->
                hideError()
                callback.call(false)
            }
            .show()
        return dialog
    }

    fun hideError() {
        if (dialog != null) {
            dialog!!.cancel()
            dialog = null
        }
    }

    fun runAfterDialog(r: Runnable) {
        if (dialog != null) {
            dialog!!.setOnDismissListener { dialog: DialogInterface? -> r.run() }
        } else {
            r.run()
        }
    }

    fun hideKeyboard() {
        // Modified from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-programmatically
        val activity: Activity? = activity
        val imm = activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.findViewById<View>(android.R.id.content)
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        val tMan = mainActivity!!.tutorialManagement
        setupTutorialMessages(tMan)
        tMan.selectActiveFragment(this)
    }

    override fun onPause() {
        super.onPause()
        mainActivity!!.tutorialManagement.deselectActiveFragment(this)
        hideError()
    }

    protected open fun setupTutorialMessages(m: TutorialManagement) {}
    fun makeTutorialEntry(id: String, text: String, trackable: Trackable): TutorialEntry {
        return TutorialEntry(
            id,
            javaClass,
            text,
            trackable
        )
    }

    fun makeTutorialEntry(text: String, trackable: Trackable): TutorialEntry {
        return TutorialEntry(
            text.lowercase(Locale.getDefault()).replace(" ".toRegex(), "_")
                .substring(0, 12) + "_" + text.hashCode(),
            javaClass,
            text,
            trackable
        )
    }

    fun makeTutorialEntry(@StringRes textRes: Int, trackable: Trackable): TutorialEntry {
        val resName = resources.getResourceEntryName(textRes)
        val text = getString(textRes)
        return TutorialEntry(
            resName,
            javaClass,
            text,
            trackable
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressDialog = ProgressDialog(context)
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog!!.hide()
        disconnectDialog = CoordinatedDisconnectDialog(
            this, mainActivity!!.credStore
        )
    }

    fun showProgress(message: String?) {
        progressDialog!!.setCancelable(false)
        progressDialog!!.setMessage(message)
        progressDialog!!.show()
    }

    fun showProgress(message: String?, cancelButtonText: String?, cancelButton: DialogCallback) {
        progressDialog!!.setMessage(message)
        progressDialog!!.setCancelable(true)
        progressDialog!!.setButton(
            ProgressDialog.BUTTON_NEGATIVE,
            cancelButtonText
        ) { dialogInterface: DialogInterface?, i: Int -> cancelButton.call(false) }
        progressDialog!!.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog!!.dismiss()
        if (dialog != null) {
            dialog!!.dismiss()
        }
    }

    val mainActivity: MainActivity
        get() = requireActivity() as MainActivity

    fun showUrlInBrowser(url: String?) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun dpToPx(dp: Float): Int {
        return dpToPx(context!!, dp)
    }
}