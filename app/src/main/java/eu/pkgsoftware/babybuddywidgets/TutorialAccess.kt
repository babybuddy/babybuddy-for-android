package eu.pkgsoftware.babybuddywidgets

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.Navigation

class TutorialAccess(private val activity: Activity) {
    private val tutorialArrow: View
    private val tutorialText: TextView
    private var animations = arrayOf<Animator>()
    private var postInitDone = false

    init {
        tutorialArrow = activity.findViewById(R.id.tutorial_arrow)
        tutorialText = activity.findViewById(R.id.tutorial_text)

        hideTutorial()
    }

    private fun postInit() {
        if (postInitDone) return
        val fragView =
            activity.findViewById<FragmentContainerView>(R.id.nav_host_fragment_content_main)
        val nav = Navigation.findNavController(fragView)
        nav.addOnDestinationChangedListener() { controller, destionaion, arguments ->
            hideTutorial()
        }
        postInitDone = true
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
        postInit()

        var arrowX: Float = _arrowX - tutorialArrow.width / 2
        var arrowY: Float = _arrowY

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

        tutorialText.doOnNextLayout {
            val width = it.width
            if (tutorialText.x < globalRect.left) {
                tutorialText.x = globalRect.left.toFloat()
            }
            if (tutorialText.x > globalRect.right - width) {
                tutorialText.x = (globalRect.right - width).toFloat()
            }
        }
        tutorialText.requestLayout()

        stopAnimations()
        startArrowAnimation()
    }


    fun hideTutorial() {
        tutorialArrow.visibility = View.INVISIBLE
        tutorialText.visibility = View.INVISIBLE
        stopAnimations()
    }
}
