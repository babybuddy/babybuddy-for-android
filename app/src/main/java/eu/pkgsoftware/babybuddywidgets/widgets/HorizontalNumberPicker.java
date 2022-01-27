package eu.pkgsoftware.babybuddywidgets.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class HorizontalNumberPicker extends View {
    public HorizontalNumberPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private static final Paint BLACK = new Paint();

    static {
        BLACK.setColor(Color.parseColor("#000000"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        canvas.drawRect(
            new Rect(0, 0, 100, 100), BLACK
        );
        canvas.drawText("AAAAAAAAAAAAAAAA", 100, 100, BLACK);
    }
}
