package eu.pkgsoftware.babybuddywidgets.widgets

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import androidx.transition.ChangeBounds
import android.transition.Transition
import android.view.View
import androidx.transition.Scene
import androidx.transition.TransitionManager
import eu.pkgsoftware.babybuddywidgets.databinding.FoldingTextBinding

class FoldingText(activity: Activity, shortText: String, longText: String) {
    val binding: FoldingTextBinding

    val view: View get() = binding.root
    var folded = true
    val foldedScene: Scene
    val unfoldedScene: Scene

    var arrowAnimation: Animator? = null

    init {
        binding = FoldingTextBinding.inflate(activity.layoutInflater)
        binding.shortText.text = shortText
        binding.longText.text = longText

        unfoldedScene = Scene(binding.textContainer)
        unfoldedScene.setEnterAction {
            binding.shortText.visibility = View.GONE
            binding.longText.visibility = View.VISIBLE
        }
        foldedScene = Scene(binding.textContainer)
        foldedScene.setEnterAction {
            binding.shortText.visibility = View.VISIBLE
            binding.longText.visibility = View.GONE
        }

        updateFolding()

        binding.root.setOnClickListener { onClick() }
    }

    private fun updateFolding() {
        binding.shortText.visibility = if (!folded) View.GONE else View.VISIBLE
        binding.longText.visibility = if (folded) View.GONE else View.VISIBLE

        arrowAnimation?.cancel()
        arrowAnimation = AnimatorSet().apply {
            if (folded) {
                play(ObjectAnimator.ofFloat(binding.foldingArrow, View.ROTATION, 90f, 0f))
            } else {
                play(ObjectAnimator.ofFloat(binding.foldingArrow, View.ROTATION, 0f, 90f))
            }
            duration = 200
            start()
        }

        val transition = ChangeBounds()
        transition.duration = 200

        TransitionManager.endTransitions(binding.textContainer)
        if (folded) {
            TransitionManager.go(foldedScene, transition)
        } else {
            TransitionManager.go(unfoldedScene, transition)
        }
    }

    private fun onClick() {
        folded = !folded
        updateFolding()
    }
}
