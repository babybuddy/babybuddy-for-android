package eu.pkgsoftware.babybuddywidgets

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.*;
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule


@RunWith(AndroidJUnit4::class)
class Screengrab {
    companion object {
        @get:ClassRule val localeTestRule = LocaleTestRule()
    }

    @get:Rule var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testTakeScreenshot() {


        Screengrab.screenshot("before_button_click")

        // Your custom onView...
        // onView(withId(R.id.fab)).perform(click())
        // Screengrab.screenshot("after_button_click")
    }
}