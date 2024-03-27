package eu.pkgsoftware.babybuddywidgets.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.Tools
import eu.pkgsoftware.babybuddywidgets.login.Utils

enum class RowAlign(val value: Int) {
    TOP(0),
    CENTER(1),
    BOTTOM(2)
}

class AutoHGrid : ViewGroup {
    internal data class RowData(
        val children: List<View>,
        val width: Int,
        val maxHeight: Int
    )

    var rowAlign: RowAlign = RowAlign.CENTER
    var rowSpacing: Float = 0f

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    fun init(attrs: AttributeSet?, defStyle: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoHGrid, defStyle, 0)
        val rowAlignRaw = typedArray.getInt(R.styleable.AutoHGrid_rowAlign, RowAlign.CENTER.value)

        rowAlign = RowAlign.entries.firstOrNull { it.value == rowAlignRaw } ?: RowAlign.CENTER
        rowSpacing = typedArray.getDimension(R.styleable.AutoHGrid_rowSpacing, 0f)

        typedArray.recycle()
    }

    private fun computeRows(width: Int): List<RowData> {
        val placeableChildren = children.filter { it.visibility != View.GONE }.toList()

        val rows = mutableListOf(mutableListOf<View>())
        val rowWidths = mutableListOf<Int>()

        val spacing = Tools.dpToPx(context, rowSpacing)

        var currentRowWidth = 0
        for (child in placeableChildren) {
            val widthWithSpacing = child.measuredWidth + if (currentRowWidth > 0) spacing else 0
            if (currentRowWidth + widthWithSpacing > width) {
                rows.add(mutableListOf())
                rowWidths.add(currentRowWidth)
                currentRowWidth = 0
            }
            rows.last().add(child)
            currentRowWidth += child.measuredWidth + if (currentRowWidth > 0) spacing else 0
        }
        rowWidths.add(currentRowWidth)

        val result = mutableListOf<RowData>()
        for (i in rows.indices) {
            val row = rows[i]
            if (row.isEmpty()) {
                continue
            }
            result.add(RowData(row, rowWidths[i], row.maxOf { it.measuredHeight }))
        }
        return result
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val placeableChildren = children.filter { it.visibility != View.GONE }.toList()
        for (child in placeableChildren) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }

        val rows = computeRows(width)

        val height = rows.sumOf { it.maxHeight }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val spacing = Tools.dpToPx(context, rowSpacing)

        val rows = computeRows(width)
        var currentY = t
        for (row in rows) {
            var currentX = (width - row.width) / 2
            for (child in row.children) {
                val yOffset = when (rowAlign) {
                    RowAlign.TOP -> 0
                    RowAlign.CENTER -> (row.maxHeight - child.measuredHeight) / 2
                    RowAlign.BOTTOM -> row.maxHeight - child.measuredHeight
                }
                child.layout(
                    currentX,
                    currentY + yOffset,
                    currentX + child.measuredWidth,
                    currentY + child.measuredHeight + yOffset
                )
                currentX += child.measuredWidth + spacing
            }
            currentY += row.maxHeight
        }
    }
}