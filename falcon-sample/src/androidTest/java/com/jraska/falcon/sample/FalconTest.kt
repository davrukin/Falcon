package com.jraska.falcon.sample

import android.graphics.Bitmap
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import com.jraska.falcon.Falcon
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import com.jraska.falcon.sample.asserts.BitmapAssert.assertThatBitmap
import com.jraska.falcon.sample.asserts.BitmapFileAssert.assertThatFile
import org.assertj.core.api.Assertions.assertThat

@RunWith(AndroidJUnit4::class)
class FalconTest {

    //endregion

    //region Fields

    @get:Rule
    var activityRule = ActivityTestRule(SampleActivity::class.java)

    private var screenshotFile: File? = null

    //endregion

    //region Setup Methods

    @After
    fun after() {
        if (screenshotFile != null) {
            assertThat(screenshotFile!!.delete()).isTrue()
        }
    }

    //endregion

    //region Test methods

    @Test
    fun takesScreenshotToFile() {
        val activity = activityRule.activity
        val newFile = activity.screenshotFile
        screenshotFile = newFile

        //check that file does not exist yet
        assertThat(newFile).doesNotExist()

        Falcon.takeScreenshot(activity, newFile, Bitmap.CompressFormat.JPEG)

        assertThatFile(newFile).isBitmap
    }

    @Test
    fun takesScreenshotToBitmap() {
        val bitmap = Falcon.takeScreenshotBitmap(activityRule.activity)

        //assertThat<Bitmap>(bitmap).isNotNull()
        assertThatBitmap(bitmap).isNotNull

        assertThatBitmap(bitmap).hasWidthGreaterThan(SMALLEST_SCREEN_EVER)
        assertThatBitmap(bitmap).hasHeightGreaterThan(SMALLEST_SCREEN_EVER)
    }

    @Test
    fun takesCorrectScreenshotSize() {
        val activity = activityRule.activity
        val bitmap = Falcon.takeScreenshotBitmap(activity)

        val decorView = activity.window.decorView
        assertThatBitmap(bitmap).hasWidth(decorView.width)
        assertThatBitmap(bitmap).hasHeight(decorView.height)
    }

    @Test
    fun takesToastIntoScreenshot() {
        val activity = activityRule.activity
        val beforeToastBitmap = Falcon.takeScreenshotBitmap(activity)
        onView(withId(R.id.show_toast)).perform(click())

        val withToastBitmap = Falcon.takeScreenshotBitmap(activity)

        assertThatBitmap(withToastBitmap).isDifferentThan(beforeToastBitmap)
    }

    @Test
    fun takesDialogIntoScreenshot() {
        val activity = activityRule.activity
        val beforeToastBitmap = Falcon.takeScreenshotBitmap(activity)
        onView(withId(R.id.show_dialog)).perform(click())

        val withToastBitmap = Falcon.takeScreenshotBitmap(activity)

        assertThatBitmap(withToastBitmap).isDarkerThan(beforeToastBitmap)
    }

    @Test
    fun takesPopupIntoScreenshot() {
        val activity = activityRule.activity
        val beforePopupBitmap = Falcon.takeScreenshotBitmap(activity)
        onView(withId(R.id.show_popup)).perform(click())

        val withPopupBitmap = Falcon.takeScreenshotBitmap(activity)

        assertThatBitmap(withPopupBitmap).isDifferentThan(beforePopupBitmap)
    }

    companion object {
        //region Constants

        private const val SMALLEST_SCREEN_EVER = 100
    }

    //endregion
}
