package eu.pkgsoftware.babybuddywidgets

import android.view.View
import android.view.ViewTreeObserver

class RunOnceAfterLayoutUpdate(val view: View, val callback: Runnable) :
    ViewTreeObserver.OnGlobalLayoutListener {

    init {
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        callback.run()
    }
}