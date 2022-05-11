package eu.pkgsoftware.babybuddywidgets

import android.app.Activity
import android.content.Context
import android.view.View

class TutorialAccess(private val activity: Activity) {
    private val tutorialArrow: View;

    init {
        tutorialArrow = activity.findViewById(R.id.tutorial_arrow);

        hideTutorial();
    }

    public fun hideTutorial() {
        tutorialArrow.visibility = View.GONE;
    }
}