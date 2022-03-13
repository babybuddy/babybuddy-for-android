package eu.pkgsoftware.babybuddywidgets;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public enum FeedingTypeEnum {
        BREAST_MILK(0, "breast milk"),
        FORMULA(1, "formula"),
        FORTIFIED_BREAST_MILK(2, "fortified breast milk"),
        SOLID_FOOD(3, "solid food");

        public int value;
        public String post_name;
        FeedingTypeEnum(int v, String post_name) {
            value = v;
            this.post_name = post_name;
        }
    };
    public static Map<Integer, FeedingTypeEnum> FeedingTypeEnumValues = new HashMap<>();
    static {
        for (FeedingTypeEnum e : FeedingTypeEnum.values()) {
            FeedingTypeEnumValues.put(e.value, e);
        }
    }

    public enum FeedingMethodEnum {
        BOTTLE(0, "bottle"),
        LEFT_BREAST(1, "left breast"),
        RIGHT_BREAST(2, "right breast"),
        BOTH_BREASTS(3, "both breasts"),
        PARENT_FED(4, "parent fed"),
        SELF_FED(5, "self fed");

        public int value;
        public String post_name;
        FeedingMethodEnum(int v, String post_name) {
            value = v;
            this.post_name = post_name;
        }
    };
    public static Map<Integer, FeedingMethodEnum> FeedingMethodEnumValues = new HashMap<>();
    static {
        for (FeedingMethodEnum e : FeedingMethodEnum.values()) {
            FeedingMethodEnumValues.put(e.value, e);
        }
    }
}
