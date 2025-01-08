package eu.pkgsoftware.babybuddywidgets.widgets

import android.app.Activity
import android.view.View
import eu.pkgsoftware.babybuddywidgets.databinding.FoldingTextBinding

class FoldingText(activity: Activity, shortText: String, longText: String) {
    val binding: FoldingTextBinding

    val view: View get() = binding.root

    private val foldingLogic: FoldingLogic

    init {
        binding = FoldingTextBinding.inflate(activity.layoutInflater)
        binding.shortText.text = shortText
        binding.longText.text = longText

        foldingLogic = FoldingLogic(binding.textContainer, binding.foldingArrow)
        binding.root.setOnClickListener { onClick() }
    }

    private fun onClick() {
        foldingLogic.folded = !foldingLogic.folded
    }
}
