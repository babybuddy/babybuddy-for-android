package eu.pkgsoftware.babybuddywidgets

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

class TutorialAccess(private val activity: Activity) {
    private val tutorialArrow: View;

    init {
        tutorialArrow = activity.findViewById(R.id.tutorial_arrow);

        hideTutorial();
    }

    public fun tutorialMessage(view: View, messsage: String) {
        if (view.visibility != View.VISIBLE) {
            println("Tutorial: Cannot show tutorial on hidden view");
            hideTutorial();
            return;
        }

        val rect = Rect();
        view.getGlobalVisibleRect(rect);

        var arrowX = rect.centerX();
        var arrowY = rect.bottom;

        arrowX = arrowX - tutorialArrow.width / 2;
        arrowY = arrowY - tutorialArrow.height / 2;


        val rootView = tutorialArrow.rootView;
        val globalRect = Rect()
        rootView.getGlobalVisibleRect(globalRect);

        arrowX = arrowX - globalRect.left;
        arrowY = arrowY - globalRect.top;

        tutorialArrow.x = arrowX.toFloat();
        tutorialArrow.y = arrowY.toFloat();

        tutorialArrow.visibility = View.VISIBLE;

        println("Tutorial: data ${tutorialArrow.width} x ${tutorialArrow.height}");
    }

    public fun hideTutorial() {
        tutorialArrow.visibility = View.INVISIBLE;
    }
}