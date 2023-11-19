package eu.pkgsoftware.babybuddywidgets.tutorial

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.Navigation
import eu.pkgsoftware.babybuddywidgets.MainActivity
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.Tools

fun interface DismissedCallback {
    fun manuallyDismissed()
}

class TutorialAccess(private val activity: MainActivity) {
    var manuallyDismissedCallback: DismissedCallback? = null

    private val tutorialArrow: View
    private val tutorialText: TextView
    private var postInitDone = false
    private var runningAnimators = arrayOf<Animator>()

    var hideOnInput = true

    init {
        tutorialArrow = activity.findViewById(R.id.tutorial_arrow)
        tutorialText = activity.findViewById(R.id.tutorial_text)

        tutorialArrow.setOnClickListener {
            handleInput()
        }
        tutorialText.setOnClickListener {
            handleInput()
        }

        hideTutorial()
        startArrowAnimation()

        activity.inputEventListeners.add {
            if (it is KeyEvent) {
                handleInput()
            }
        }
    }

    fun handleInput() {
        if (hideOnInput) {
            val oldDismissedCallback = manuallyDismissedCallback
            manuallyDismissedCallback = null
            hideTutorial(false)
            oldDismissedCallback?.manuallyDismissed()
        }
    }

    private fun postInit() {
        if (postInitDone) return
        val fragView =
            activity.findViewById<FragmentContainerView>(R.id.nav_host_fragment_content_main)
        val nav = Navigation.findNavController(fragView)
        nav.addOnDestinationChangedListener() { controller, destination, arguments ->
            hideTutorial()
        }
        postInitDone = true
    }

    private fun startArrowAnimation() {
        val oldAnimators = runningAnimators
        runningAnimators = arrayOf()
        oldAnimators.forEach {
            it.cancel()
        }

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
        a1.doOnEnd {
            if (runningAnimators.contains(a2)) {
                a2.start()
            }
        }
        a2.doOnEnd {
            if (runningAnimators.contains(a1)) {
                a1.start()
            }
        }
        a1.setAutoCancel(true)
        a2.setAutoCancel(true)
        a1.start()
        runningAnimators = arrayOf(a1, a2)
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

        tutorialArrow.visibility = View.VISIBLE
        tutorialText.visibility = View.VISIBLE
        tutorialText.text = message

        moveArrow(_arrowX, _arrowY)
        startArrowAnimation()
    }

    fun moveArrow(_arrowX: Float, _arrowY: Float) {
        var arrowX: Float = _arrowX
        var arrowY: Float = _arrowY

        val rootView = tutorialArrow.rootView
        val globalRect = Rect()
        rootView.getGlobalVisibleRect(globalRect)

        arrowX -= globalRect.left
        arrowY -= globalRect.top

        tutorialArrow.x = arrowX - tutorialArrow.width / 2
        tutorialArrow.y = arrowY

        tutorialText.y = arrowY + tutorialArrow.height.toFloat()
        tutorialText.x = arrowX - tutorialText.width / 2f

        println("TUT TEXT: ${tutorialArrow.x},${tutorialArrow.y}")

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
    }

    fun hideTutorial(immediate: Boolean = true) {
        if (immediate) {
            tutorialArrow.visibility = View.INVISIBLE
            tutorialText.visibility = View.INVISIBLE
            arrayOf(tutorialArrow, tutorialText).forEach {
                ObjectAnimator.ofFloat(it, "alpha", 1f).apply {
                    setAutoCancel(true)
                    start()
                }
            }
        } else {
            arrayOf(tutorialArrow, tutorialText).forEach {
                ObjectAnimator.ofFloat(it, "alpha", 1f, 0f).apply {
                    duration = 250
                    setAutoCancel(true)
                    repeatCount = 0
                    doOnEnd {
                        hideTutorial(true)
                    }
                    start()
                }
            }
        }
    }
}
