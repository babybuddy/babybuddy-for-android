package eu.pkgsoftware.babybuddywidgets.widgets

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.Tools

class InfoBubble(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatTextView(context, attrs, defStyleAttr) {

    var animation: Animator? = null

    constructor(context: Context) : this(context, null, 0) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    init {
        val drawable = ResourcesCompat.getDrawable(
            context.resources, R.drawable.pill_outline, context.theme
        )
        drawable?.let {
            var color = Color.WHITE
            attrs?.let {
                val sAttrs = context.theme.obtainStyledAttributes(
                    attrs, R.styleable.InfoBubble, defStyleAttr, 0
                )
                color = sAttrs.getColor(R.styleable.InfoBubble_pillColor, color)
            }
            it.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_OVER)
        }

        background = drawable
        textAlignment = TEXT_ALIGNMENT_CENTER

        val padding = Tools.dpToPx(context, 12.0f)
        this.setPadding(padding, padding / 2, padding, padding / 2)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        handler.post {
            if (animation == null) {
                visibility = View.GONE
            }
        }
    }

    fun flashMessage(id: Int, timeoutMs: Long) {
        flashMessage(resources.getString(id), timeoutMs)
    }

    fun flashMessage(msg: String, timeoutMs: Long) {
        animation?.cancel()

        this@InfoBubble.visibility = View.VISIBLE
        alpha = 0.0f
        text = msg

        val vertMove = Tools.dpToPx(context, 20f).toFloat();

        animation = AnimatorSet().also {
            val beforeVertOffset = ObjectAnimator.ofFloat(
                this, "translationY", vertMove, 0f
            ).apply { duration = 200 }
            val afterVertOffset = ObjectAnimator.ofFloat(
                this, "translationY", 0f, -vertMove
            ).apply { duration = 200 }
            val beforeAlpha = ObjectAnimator.ofFloat(
                this, "alpha", 0f, 1f
            ).apply { duration = 200 }
            val afterAlpha = ObjectAnimator.ofFloat(
                this, "alpha", 1f, 0f
            ).apply { duration = 200 }

            it.play(beforeAlpha)
                .with(beforeVertOffset)
            it.play(afterAlpha).after(timeoutMs + 200)
            it.play(afterVertOffset).after(timeoutMs + 200)
            it.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    this@InfoBubble.visibility = View.GONE
                    this@InfoBubble.animation = null
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            it.start()
        }
    }
}