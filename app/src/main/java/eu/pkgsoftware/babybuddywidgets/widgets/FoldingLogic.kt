package eu.pkgsoftware.babybuddywidgets.widgets

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.transition.Scene
import androidx.transition.TransitionManager
import androidx.transition.ChangeBounds

class FoldingLogic(val container: ViewGroup, val foldingArrow: View?) {
    var animationDuration = 200L
    var folded: Boolean = true
        set(value) {
            field = value
            updateFolding()
        }

    private val unfoldedScene: Scene
    private val foldedScene: Scene
    private var arrowAnimation: Animator? = null

    init {
        unfoldedScene = Scene(container)
        unfoldedScene.setEnterAction {
            var first = true
            for (child in container.children) {
                child.visibility = if (first) View.GONE else View.VISIBLE
                first = false
            }
        }
        foldedScene = Scene(container)
        foldedScene.setEnterAction {
            var first = true
            for (child in container.children) {
                child.visibility = if (first) View.VISIBLE else View.GONE
                first = false
            }
        }

        updateFolding()
    }

    fun updateFolding() {
        val transition = ChangeBounds()
        transition.duration = animationDuration

        TransitionManager.endTransitions(container)
        if (folded) {
            TransitionManager.go(foldedScene, transition)
        } else {
            TransitionManager.go(unfoldedScene, transition)
        }

        foldingArrow?.let { arrow ->
            arrowAnimation?.cancel()

            arrowAnimation = AnimatorSet().apply {
                if (folded) {
                    play(ObjectAnimator.ofFloat(arrow, View.ROTATION, 90f, 0f))
                } else {
                    play(ObjectAnimator.ofFloat(arrow, View.ROTATION, 0f, 90f))
                }
                duration = animationDuration
                start()
            }
        }
    }
}