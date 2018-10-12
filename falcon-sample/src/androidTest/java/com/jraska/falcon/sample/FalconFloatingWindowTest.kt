package com.jraska.falcon.sample

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.jraska.falcon.Falcon
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import com.jraska.falcon.sample.Assumptions.assumeNoCI
import com.jraska.falcon.sample.asserts.BitmapAssert.assertThatBitmap

@RunWith(AndroidJUnit4::class)
class FalconFloatingWindowTest {
    //region Fields

    @get:Rule
    var activityRule = ActivityTestRule(FloatingViewActivity::class.java)

    //endregion

    //region Test methods

    @Test
    @Throws(InterruptedException::class)
    fun takesCorrectScreenshotSize() {
        waitForActivitiesToFinish()

        val activity = activityRule.activity

        val bitmap = Falcon.takeScreenshotBitmap(activity)

        val decorView = activity.window.decorView
        assertThatBitmap(bitmap).hasWidth(decorView.width)
        assertThatBitmap(bitmap).hasHeight(decorView.height)
    }

    @Test
    fun takesToastOutOfWindowIntoScreenshot() {
        assumeNoCI()

        val activity = activityRule.activity
        onView(withId(R.id.show_toast)).perform(click())

        val bitmap = Falcon.takeScreenshotBitmap(activity)

        val decorView = activity.window.decorView
        assertThatBitmap(bitmap).hasWidthGreaterThan(decorView.height)
    }

    //endregion

    //region Methods

    @Throws(InterruptedException::class)
    private fun waitForActivitiesToFinish() {
        // TODO: 26/07/16 Use idling resource to wait for all other activities to finish
        // Currently previous tests influence this test - Activities did not finish properly
        // and their windows are still attached and taken to screenshot.
        // Since they are bigger then floating window it ruins the test
        // Feel free to submit pull request with solution
        Thread.sleep(3000)
    }

    //endregion
}
