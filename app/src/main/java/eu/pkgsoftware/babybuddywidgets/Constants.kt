package eu.pkgsoftware.babybuddywidgets

import java.text.SimpleDateFormat
import java.util.Locale

object Constants {
    @JvmField
    val SERVER_DATE_FORMAT = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH)

    enum class FeedingTypeEnum(@JvmField var value: Int, @JvmField var post_name: String) {
        BREAST_MILK(0, "breast milk"),
        FORMULA(1, "formula"),
        FORTIFIED_BREAST_MILK(2,"fortified breast milk"),
        SOLID_FOOD(3, "solid food");

        companion object {
            @JvmStatic
            fun byValue(value: Int): FeedingTypeEnum {
                return FeedingTypeEnum.values().first { it.value == value }
            }

            @JvmStatic
            fun byPostName(name: String): FeedingTypeEnum {
                return FeedingTypeEnum.values().first { it.post_name == name }
            }
        }
    }

    enum class FeedingMethodEnum(@JvmField var value: Int, @JvmField var post_name: String) {
        BOTTLE(0, "bottle"),
        LEFT_BREAST(1, "left breast"),
        RIGHT_BREAST(2, "right breast"),
        BOTH_BREASTS(3, "both breasts"),
        PARENT_FED(4, "parent fed"),
        SELF_FED(5, "self fed");

        companion object {
            @JvmStatic
            fun byValue(value: Int): FeedingMethodEnum {
                return FeedingMethodEnum.values().first { it.value == value }
            }

            @JvmStatic
            fun byPostName(name: String): FeedingMethodEnum {
                return FeedingMethodEnum.values().first { it.post_name == name }
            }
        }
    }

    @JvmField
    var FeedingTypeEnumValues: Map<Int, FeedingTypeEnum>
    init {
        val m = mutableMapOf<Int, FeedingTypeEnum>()
        for (e in FeedingTypeEnum.values()) {
            m.put(e.value, e)
        }
        FeedingTypeEnumValues = m.toMap()
    }

    @JvmField
    var FeedingMethodEnumValues: Map<Int, FeedingMethodEnum>
    init {
        val m = mutableMapOf<Int, FeedingMethodEnum>()
        for (e in FeedingMethodEnum.values()) {
            m.put(e.value, e)
        }
        FeedingMethodEnumValues = m.toMap()
    }

    enum class SolidDiaperColorEnum(var value: Int, var post_name: String) {
        BLACK(0, "black"),
        BROWN(1, "brown"),
        GREEN(2, "green"),
        YELLOW(3, "yellow");

        companion object {
            @JvmStatic
            fun byValue(value: Int): SolidDiaperColorEnum {
                return SolidDiaperColorEnum.entries.first { it.value == value }
            }

            @JvmStatic
            fun byPostName(name: String): SolidDiaperColorEnum {
                return SolidDiaperColorEnum.entries.first { it.post_name == name }
            }
        }
    }
}