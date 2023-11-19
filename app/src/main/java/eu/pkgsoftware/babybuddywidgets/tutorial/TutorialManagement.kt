package eu.pkgsoftware.babybuddywidgets.tutorial

import android.app.Activity
import android.graphics.PointF
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.CredStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface Trackable {
    fun getPosition(): PointF
}

open class TutorialEntry(
    val id: String,
    val fragmentClass: Class<*>,
    val text: String,
    val trackable: Trackable
) {
    open val fullId = "frag:${fragmentClass.simpleName}-id:${id}"
    open val maxPresentations = 1
}

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
                if (credStore.getTutorialParameter(e.fullId) < e.maxPresentations) {
                    return e
                }
            }

            return null
        }

    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

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
                tutorialAccess.manuallyDismissedCallback = DismissedCallback {
                    currentlyShowing?.let {
                        val c = credStore.getTutorialParameter(it.fullId)
                        credStore.setTutorialParameter(it.fullId, c + 1)
                    }
                }

                updateJob = scope.launch {
                    var p = it.trackable.getPosition()
                    tutorialAccess.tutorialMessage(p.x, p.y, it.text)
                    while (true) {
                        delay(100)
                        p = it.trackable.getPosition()
                        tutorialAccess.moveArrow(p.x, p.y)
                    }
                }
            }
        }
    }

    private fun deactivateArrow() {
        currentlyShowing?.let {
            tutorialAccess.hideTutorial(false)
            currentlyShowing = null
        }
        updateJob?.cancel("deactivateArrow")
        updateJob = null
    }
}