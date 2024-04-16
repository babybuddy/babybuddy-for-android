package eu.pkgsoftware.babybuddywidgets.timers.utils;

import java.text.DecimalFormat;

import eu.pkgsoftware.babybuddywidgets.widgets.HorizontalNumberPicker;

public class AmountValuesGenerator implements HorizontalNumberPicker.ValueGenerator {
    public static final DecimalFormat FORMAT_VALUE = new DecimalFormat("#.#");

    public long minValue() {
        return -1L;
    }

    public long maxValue() {
        return 1 + 5 * 9;
    } // 5 orders of magnitude

    private long calcBaseValue(long index) {
        return (long) Math.round(Math.pow(10, (double) Math.max(0, (index / 9))));
    }

    public String getValue(long index) {
        if (index < 0) {
            return "None";
        } else {
            return FORMAT_VALUE.format(getRawValue(index, 0f));
        }
    }

    public Double getRawValue(long index, float offset) {
        if (index + offset < -0.001) {
            return null;
        } else {
            if (offset < 0) {
                index--;
                offset += 1.0f;
                if (index < 0) {
                    index = 0;
                    offset = 0.0f;
                }
            }

            if (index == 0) {
                return (double) offset;
            }
            return (double) calcBaseValue(index - 1) * (((index - 1) % 9 + 1) + offset);
        }
    }

    public long getValueIndex(Double value) {
        if (value == null) {
            return -1L;
        } else {
            long exp = (long) Math.max(0, Math.floor(Math.log10(value)));
            long base10 = Math.round(Math.pow(10, exp));
            double relativeRest = value / base10;

            long index = (long) Math.floor(relativeRest);
            double offset = relativeRest - index;
            if (offset >= 0.5) {
                offset -= 1.0;
                index += 1;
            }
            index += 9 * exp;

            return Math.max(minValue(), Math.min(maxValue(), index));
        }
    }

    public double getValueOffset(Double value) {
        if (value == null) {
            return 0.0d;
        } else {
            long exp = (long) Math.max(0, Math.floor(Math.log10(value)));
            long base10 = Math.round(Math.pow(10, exp));
            double relativeRest = value / base10;

            long index = (long) Math.floor(relativeRest);
            double offset = relativeRest - index;
            if (offset >= 0.5) {
                offset -= 1.0;
                index += 1;
            }
            index += 9 * exp;

            return Math.max(-0.5, Math.min(0.5, offset));
        }
    }
}
