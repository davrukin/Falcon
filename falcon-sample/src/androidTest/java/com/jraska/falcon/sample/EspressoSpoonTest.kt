package com.jraska.falcon.sample

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.jraska.falcon.FalconSpoonRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.util.ArrayList

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.GrantPermissionRule.grant
import com.jraska.falcon.sample.asserts.BitmapFileAssert.assertThatFile
import org.assertj.core.api.Assertions.assertThat

/**
 * Example Espresso test of dialog taken by SpoonCompat
 */
@RunWith(AndroidJUnit4::class)
class EspressoSpoonTest {

    //region Fields

    @get:Rule
    var activityRule = ActivityTestRule(SampleActivity::class.java)

    @get:Rule
    val falconSpoonRule = FalconSpoonRule()

    @get:Rule
    val permissionRule = grant(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)!!

    private val takenScreenshots = ArrayList<File>()

    //endregion

    //region Setup Methods

    @After
    fun after() {
        for (screenshot in takenScreenshots) {
            assertThat(screenshot.delete()).isTrue()
        }
    }

    //endregion

    //region Test Methods

    @Test
    fun dialogTakenInScreenshot() {
        val activity = activityRule.activity

        val screenshotWithoutDialogFile = falconSpoonRule.screenshot(activity, "No_dialog")
        takenScreenshots.add(screenshotWithoutDialogFile)

        onView(withId(R.id.show_dialog)).perform(click())
        onView(withText("Screenshot")).check(matches(isDisplayed()))

        val screenshotWithDialogFile = falconSpoonRule.screenshot(activity, "Dialog_test")
        takenScreenshots.add(screenshotWithDialogFile)

        assertThatFile(screenshotWithDialogFile).isDarkerThan(screenshotWithoutDialogFile)
    }

    //endregion
}
