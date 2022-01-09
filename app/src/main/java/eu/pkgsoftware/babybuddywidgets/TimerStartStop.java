package eu.pkgsoftware.babybuddywidgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link TimerStartStopConfigureActivity TimerStartStopConfigureActivity}
 */
public class TimerStartStop extends AppWidgetProvider {
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        CharSequence widgetText = TimerStartStopConfigureActivity.loadTitlePref(context, appWidgetId);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_start_stop);
        views.setTextViewText(R.id.timerText, "1:01:00");
        views.setViewVisibility(R.id.stopButton, View.GONE);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            TimerStartStopConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}