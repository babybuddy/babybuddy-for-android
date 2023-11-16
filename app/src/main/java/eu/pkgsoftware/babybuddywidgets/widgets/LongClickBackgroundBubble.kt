package eu.pkgsoftware.babybuddywidgets.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import eu.pkgsoftware.babybuddywidgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class LongClickBackgroundBubble(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var animateJob: Job? = null

    @ColorInt
    private var bubbleColor = Color.argb(0x80, 0, 0, 0)
    private var growStart: Long? = null

    var growTimeDeadZone = 0.25f
    var growTime = ViewConfiguration.getLongPressTimeout() / 1000f

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LongClickBackgroundBubble,
            0, 0).apply {

            try {
                bubbleColor = getColor(R.styleable.LongClickBackgroundBubble_color, bubbleColor)
                growTime = getFloat(R.styleable.LongClickBackgroundBubble_time, growTime)
            } finally {
                recycle()
            }
        }
    }

    private val BUBBLE_PAINT = Paint()
    init {
        BUBBLE_PAINT.style = Paint.Style.FILL_AND_STROKE
        BUBBLE_PAINT.strokeCap = Paint.Cap.SQUARE
        BUBBLE_PAINT.color = bubbleColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        growStart?.let { start ->
            var radiusFactor = min(1f, (System.currentTimeMillis() - start) / 1000f / growTime)
            radiusFactor = max(0f, (radiusFactor - growTimeDeadZone) / (1f - growTimeDeadZone))
            val radius = radiusFactor * sqrt((width * width + height * height) / 4.0f)

            canvas.drawCircle(width / 2f, height / 2f, radius, BUBBLE_PAINT)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        animateJob?.cancel()
        animateJob = scope.launch {
            runBubbleAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animateJob?.cancel()
    }

    private suspend fun runBubbleAnimation() {
        while (true) {
            delay(25)
            growStart?.let {
                invalidate()
            }
        }
    }

    fun startGrow() {
        growStart = System.currentTimeMillis()
    }

    fun stopGrow() {
        growStart = null
        invalidate()
    }
}