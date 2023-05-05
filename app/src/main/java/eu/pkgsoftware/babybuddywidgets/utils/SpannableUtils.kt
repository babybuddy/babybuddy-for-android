package eu.pkgsoftware.babybuddywidgets.utils

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.regex.Pattern

interface ClickHandler {
    fun linkClicked(url: String)
}

class SpannableUtils {
    private class ClickableLinkSpan(
        private val url: String,
        private val clickHandler: ClickHandler
    ) : ClickableSpan() {
        override fun onClick(view: View) {
            clickHandler.linkClicked(url)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = true
            ds.color = Color.BLUE
        }
    }

    companion object {
        val HREF_DETECTOR_PATTERN =
            Pattern.compile("<a .*href=[\"']([^\"']+)[\"'][^>]*>([^<]*)</a>")

        fun filterLinksFromTextFields(root: ViewGroup, clickHandler: ClickHandler) {
            for (i in 0 until root.childCount) {
                val v = root.getChildAt(i) as? TextView ?: continue
                val tv = v
                val orgText = tv.text.toString()
                val matcher = HREF_DETECTOR_PATTERN.matcher(orgText)
                var builder: SpannableStringBuilder? = null
                var prevMatchEnd = 0
                while (matcher.find()) {
                    if (builder == null) {
                        builder = SpannableStringBuilder()
                    }
                    builder.append(orgText.substring(prevMatchEnd, matcher.start()))
                    val linkUrl = matcher.group(1)
                    val linkText = matcher.group(2)
                    val startSpanIndex = builder.length
                    builder.append(linkText)
                    builder.setSpan(
                        ClickableLinkSpan(linkUrl, clickHandler),
                        startSpanIndex,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    prevMatchEnd = matcher.end()
                }
                if (builder != null) {
                    builder.append(orgText.substring(prevMatchEnd))
                    tv.movementMethod = LinkMovementMethod.getInstance()
                    tv.text = builder
                }
            }
        }
    }
}