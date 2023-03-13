package eu.pkgsoftware.babybuddywidgets

import android.graphics.Rect
import android.view.View
import android.widget.ScrollView

class VisibilityCheck(val scrollView: ScrollView) {
    fun checkPartiallyVisible(view: View): Boolean {
        return view.getLocalVisibleRect(Rect())
    }

    fun checkFullyVisible(view: View): Boolean {
        val r = Rect()
        if (!view.getLocalVisibleRect(r)) {
            return false
        }
        return r.width() * r.height() == view.width * view.height;
    }
}