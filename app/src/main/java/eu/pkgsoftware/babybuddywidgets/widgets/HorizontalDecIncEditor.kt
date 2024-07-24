package eu.pkgsoftware.babybuddywidgets.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import eu.pkgsoftware.babybuddywidgets.databinding.HorizontalDecIncEditorBinding
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class HorizontalDecIncEditor : LinearLayout {
    var value: Double?
        get() {
            return binding.numberEditor.text.toString().toDoubleOrNull()
        }
        set(value) {
            val v = value.let {
                if ((it == null) || (it < 0.0)) {
                    if (allowNull) {
                        return@let null
                    } else {
                        return@let 0.0
                    }
                }
                it
            }
            if (v == null) {
                binding.numberEditor.setText("")
            } else {
                binding.numberEditor.setText(v.toString())
            }
        }

    var allowNull: Boolean = true
        set(value) {
            field = value
            if (!value && this.value == null) {
                this.value = 0.0
            }
        }

    val binding = HorizontalDecIncEditorBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private val incrementValue: Int
        get() = max(1, (10.0).pow(floor(log10(abs(value?.toDouble() ?: 1.0)))).toInt())

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        value = null

        binding.decButton.setOnClickListener {
            value = (value ?: 0.0) - incrementValue
        }
        binding.incButton.setOnClickListener {
            value = (value ?: 0.0) + incrementValue
        }
    }
}