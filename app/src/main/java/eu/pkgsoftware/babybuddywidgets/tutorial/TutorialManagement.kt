package eu.pkgsoftware.babybuddywidgets.tutorial

import android.graphics.PointF
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.CredStore

interface Trackable {
    fun getPosition(): PointF
}

class TutorialEntry(
    val id: String,
    val fragmentClass: Class<*>,
    val text: String,
    val trackable: Trackable
) {
    val fullId = "frag:${fragmentClass.simpleName}-id:${id}"
}

val MAX_PRESENTATIONS = 2

class TutorialManagement(val credStore: CredStore, val tutorialAccess: TutorialAccess) {
    private val tutorialEntries = sortedMapOf<String, TutorialEntry>()
    private var selectedFragment: BaseFragment? = null
    private var currentlyShowing: TutorialEntry? = null

    private val suggestedItem: TutorialEntry?
        get() {
            val frag = selectedFragment ?: return null
            val fragClass = frag.javaClass

            for (e in tutorialEntries.values) {
                if (e.fragmentClass != fragClass) {
                    continue
                }
                if (credStore.getTutorialParameter(e.fullId) < MAX_PRESENTATIONS) {
                    return e
                }
            }

            return null
        }

    fun addItem(entry: TutorialEntry) {
        tutorialEntries[entry.id] = entry
        showArrow()
    }

    fun selectActiveFragment(fragment: BaseFragment) {
        selectedFragment?.let {
            if (fragment.javaClass == it.javaClass) {
                return
            }
            deselectActiveFragment(it)
        }

        selectedFragment = fragment
        showArrow()
    }

    fun deselectActiveFragment(fragment: BaseFragment) {
        selectedFragment?.let {
            if (fragment.javaClass == it.javaClass) {
                deactivateArrow()
                selectedFragment = null
            }
        }
    }

    private fun showArrow() {
        val suggestedItem = suggestedItem
        if (suggestedItem != currentlyShowing) {
            deactivateArrow()

            currentlyShowing = suggestedItem
            suggestedItem?.let {
                tutorialAccess.manuallyDismissedCallback

                val p = it.trackable.getPosition()
                tutorialAccess.tutorialMessage(p.x, p.y, it.text)
            }
        }
    }

    private fun deactivateArrow() {
        currentlyShowing?.let {
            tutorialAccess.hideTutorial(false)
            currentlyShowing = null
        }
    }
}