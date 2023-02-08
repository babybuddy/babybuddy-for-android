package eu.pkgsoftware.babybuddywidgets

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.animation.addListener

class TutorialAccess(private val activity: Activity) {
    private val tutorialArrow: View
    private val tutorialText: TextView
    private var animations = arrayOf<Animator>()

    init {
        tutorialArrow = activity.findViewById(R.id.tutorial_arrow)
        tutorialText = activity.findViewById(R.id.tutorial_text)

        hideTutorial()
    }

    private class AnimStartListener(val startAnim: Animator) : Animator.AnimatorListener {
        override fun onAnimationStart(p0: Animator?) {
        }

        override fun onAnimationEnd(p0: Animator?) {
            startAnim.start();
        }

        override fun onAnimationCancel(p0: Animator?) {
        }

        override fun onAnimationRepeat(p0: Animator?) {
        }
    }

    private fun stopAnimations() {
        for (a in animations) {
            a.cancel()
        }
        animations = arrayOf()
        tutorialArrow.clearAnimation()
    }

    private fun startArrowAnimation() {
        val arrowY = tutorialArrow.y.toFloat()
        val offset = Tools.dpToPx(tutorialArrow.context, 5.0f).toFloat();
        val a1 =
            ObjectAnimator.ofFloat(tutorialArrow, "translationY", -offset + arrowY, offset + arrowY)
                .apply {
                    duration = 200;
                }
        val a2 =
            ObjectAnimator.ofFloat(tutorialArrow, "translationY", offset + arrowY, -offset + arrowY)
                .apply {
                    duration = 200;
                }
        a1.addListener(AnimStartListener(a2))
        a2.addListener(AnimStartListener(a1))
        a1.start()

    }

    fun tutorialMessage(view: View, message: String) {
        if (view.visibility != View.VISIBLE) {
            println("Tutorial: Cannot show tutorial on hidden view")
            hideTutorial()
            return
        }

        val rect = Rect()
        view.getGlobalVisibleRect(rect)

        tutorialMessage(rect.centerX().toFloat(), rect.bottom.toFloat(), message)
    }

    fun tutorialMessage(_arrowX: Float, _arrowY: Float, message: String) {
        var arrowX: Float = _arrowX - tutorialArrow.width / 2
        var arrowY: Float = _arrowY - tutorialArrow.height / 2


        val rootView = tutorialArrow.rootView
        val globalRect = Rect()
        rootView.getGlobalVisibleRect(globalRect)

        arrowX = arrowX - globalRect.left
        arrowY = arrowY - globalRect.top

        tutorialArrow.x = arrowX.toFloat()
        tutorialArrow.y = arrowY.toFloat()

        tutorialText.text = message
        tutorialText.y = arrowY + tutorialArrow.height.toFloat()
        tutorialText.x = arrowX - tutorialText.width / 2f

        tutorialText.visibility = View.VISIBLE
        tutorialArrow.visibility = View.VISIBLE

        stopAnimations()
        startArrowAnimation()
    }


    fun hideTutorial() {
        tutorialArrow.visibility = View.INVISIBLE
        tutorialText.visibility = View.INVISIBLE
    }
}