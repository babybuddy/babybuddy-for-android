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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class Trackable {
    open val orientation: Direction = Direction.UP
    abstract val position: PointF?
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

    private fun suggestedItem(): TutorialEntry? {
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
        val suggestedItem = suggestedItem()
        if (suggestedItem?.fullId != currentlyShowing?.fullId) {
            deactivateArrow()
            currentlyShowing = suggestedItem
            startArrow()
        } else if (suggestedItem == null) {
            deactivateArrow()
        }
    }

    private fun startArrow() {
        currentlyShowing?.let { track ->
            tutorialAccess.manuallyDismissedCallback = DismissedCallback {
                val c = credStore.getTutorialParameter(track.fullId)
                credStore.setTutorialParameter(track.fullId, c + 1)
                currentlyShowing = null
                showArrow()
            }

            updateJob?.cancel("cancel running job")
            updateJob = scope.launch {
                while (isActive) {
                    delay(100)
                    track.trackable.position?.let { p ->
                        if (tutorialAccess.isHidden) {
                            tutorialAccess.tutorialMessage(
                                p.x,
                                p.y,
                                track.text,
                                dir = track.trackable.orientation,
                            )
                        } else {
                            tutorialAccess.moveArrow(p.x, p.y, dir = track.trackable.orientation)
                        }
                    } ?: tutorialAccess.hideTutorial(true)
                }
            }
        }
    }

    private fun deactivateArrow() {
        currentlyShowing?.let {
            tutorialAccess.hideTutorial(false)
            tutorialAccess.manuallyDismissedCallback = null
            currentlyShowing = null
        }
        updateJob?.cancel("deactivateArrow")
        updateJob = null
    }

    fun updateArrows() {
        currentlyShowing?.let {
            val currentId = it.id
            tutorialAccess.hideTutorial(true)
            currentlyShowing = tutorialEntries.getOrDefault(currentId, null)
            startArrow()
        }
    }
}