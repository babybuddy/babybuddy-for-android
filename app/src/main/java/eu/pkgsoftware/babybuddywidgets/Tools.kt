package eu.pkgsoftware.babybuddywidgets

import android.content.Context

object Tools {
    @JvmStatic
    fun dpToPx(context: Context, dp: Float): Int {
        return (context.resources.displayMetrics.density * dp + 0.5f).toInt()
    }
}