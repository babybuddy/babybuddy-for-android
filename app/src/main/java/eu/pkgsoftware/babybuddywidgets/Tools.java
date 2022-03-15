package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;

public class Tools {
    public static int dpToPx(Context context, float dp) {
        return (int) (context.getResources().getDisplayMetrics().density * dp + 0.5f);
    }
}
