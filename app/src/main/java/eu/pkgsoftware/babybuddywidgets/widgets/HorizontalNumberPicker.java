package eu.pkgsoftware.babybuddywidgets.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import eu.pkgsoftware.babybuddywidgets.R;
import eu.pkgsoftware.babybuddywidgets.Tools;

public class HorizontalNumberPicker extends View {
    public interface ValueGenerator {
        long minValue();
        long maxValue();
        String getValue(long index);
    }

    public static class StringListValues implements ValueGenerator {
        private String[] values;

        public StringListValues(String[] values) {
            this.values = values;
        }

        @Override
        public long minValue() {
            return 0;
        }

        @Override
        public long maxValue() {
            return values.length;
        }

        @Override
        public String getValue(long index) {
            return values[(int) index];
        }
    }

    public HorizontalNumberPicker(Context context) {
        super(context);
        textSize = Tools.dpToPx(getContext(), 64);
    }

    public HorizontalNumberPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
            attrs, R.styleable.HorizontalNumberPicker, 0, 0
        );
        textSize = (int) a.getDimension(R.styleable.HorizontalNumberPicker_textSize, 64);
        a.recycle();
    }

    public HorizontalNumberPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HorizontalNumberPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private int textSize;

    private final Paint BLACK_STROKE = new Paint();
    private final Paint BLACK_FILL = new Paint();
    private final Paint VALUE_PAINT = new Paint();
    private final Paint BG_VALUE_PAINT = new Paint();
    private final Path trianglePath = new Path();

    {
        BLACK_STROKE.setColor(Color.parseColor("#000000"));
        BLACK_STROKE.setStyle(Paint.Style.STROKE);
        BLACK_STROKE.setStrokeCap(Paint.Cap.SQUARE);

        BLACK_FILL.setColor(Color.parseColor("#000000"));
        BLACK_FILL.setStyle(Paint.Style.FILL);
        BLACK_FILL.setStrokeCap(Paint.Cap.SQUARE);

        VALUE_PAINT.setColor(Color.parseColor("#202020"));

        BG_VALUE_PAINT.setColor(Color.parseColor("#202020"));
        BG_VALUE_PAINT.setAlpha(128);

        trianglePath.reset();
        trianglePath.moveTo(0.0f, 0.0f);
        trianglePath.lineTo(1.0f, 1.0f);
        trianglePath.lineTo(-1.0f, 1.0f);
        trianglePath.lineTo(0.0f, 0.0f);
    }

    private final Rect textBounds = new Rect();
    private ValueGenerator values;
    private long valueIndex;

    {
        String[] v = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        values = new StringListValues(v);

        valueIndex = values.minValue();
    }

    private void drawText(Canvas canvas, float x, float y, String text, Paint p) {
        p.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, -textBounds.width() / 2.0f + x, p.getTextSize() / 2.0f + y, p);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        BLACK_STROKE.setStrokeWidth(Tools.dpToPx(getContext(), 1));
        VALUE_PAINT.setTextSize(textSize);
        BG_VALUE_PAINT.setTextSize(textSize);

        int width = getWidth();
        int height = getHeight();

        canvas.translate(width / 2.0f, height / 2.0f);

        // Render middle selector
        {

            float yOffset = textSize * 1.5f / 2.0f;
            float xOffset = yOffset * 2;
            //canvas.drawLine(-xOffset, -yOffset, -xOffset, yOffset, BLACK_STROKE);
            //canvas.drawLine(xOffset, -yOffset, xOffset, yOffset, BLACK_STROKE);
            canvas.drawLine(-xOffset, -yOffset, xOffset, -yOffset, BLACK_STROKE);
            canvas.drawLine(-xOffset, yOffset, xOffset, yOffset, BLACK_STROKE);

            canvas.save();
            canvas.save();

            canvas.translate(0, yOffset);
            canvas.scale(textSize / 2.0f,textSize / 2.0f);
            canvas.drawPath(trianglePath, BLACK_FILL);
            canvas.restore();

            canvas.translate(0, -yOffset);
            canvas.scale(textSize / 2.0f,-textSize / 2.0f);
            canvas.drawPath(trianglePath, BLACK_FILL);
            canvas.restore();
        }


        float xElementSeparation = 3 * textSize;
        int xElementCount = (int) Math.ceil((float) getWidth() / (float) xElementSeparation) + 1;
        int onesidedCount = Math.max(1, xElementCount / 2);

        canvas.clipRect(-width / 2.0f, -height / 2.0f, width / 2.0f, height / 2.0f);

        for (long oi = -onesidedCount; oi < onesidedCount + 1; oi++) {
            long i = valueIndex + oi;

            if ((i < values.minValue()) || (i > values.maxValue())) {
                continue;
            }

            Paint paint = oi == 0 ? VALUE_PAINT : BG_VALUE_PAINT;
            drawText(canvas, i * xElementSeparation, 0, "" + values.getValue(i), paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSpecV = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecV = MeasureSpec.getSize(heightMeasureSpec);

        int preferredMinWidth = textSize * 4 + getPaddingLeft() + getPaddingRight();
        int preferredMinHeight = textSize * 3 + getPaddingTop() + getPaddingBottom();

        int width = preferredMinWidth;
        int height = preferredMinHeight;

        if (widthSpecMode == MeasureSpec.EXACTLY) {
            width = widthSpecV;
        } else {
            width = Math.max(width, widthSpecV);
        }
        if (heightSpecMode == MeasureSpec.EXACTLY) {
            height = heightSpecV;
        } else {
            height = Math.max(height, heightSpecV);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }
}
