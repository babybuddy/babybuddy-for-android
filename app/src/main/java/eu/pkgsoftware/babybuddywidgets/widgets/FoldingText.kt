package eu.pkgsoftware.babybuddywidgets.widgets

import android.app.Activity
import android.view.View
import eu.pkgsoftware.babybuddywidgets.databinding.FoldingTextBinding

class FoldingText(activity: Activity, shortText: String, longText: String) {
    val binding: FoldingTextBinding

    val view: View get() = binding.root

    init {
        binding = FoldingTextBinding.inflate(activity.layoutInflater)
        binding.shortText.text = shortText
        binding.longText.text = longText
        binding.longText.visibility = View.GONE
    }
}