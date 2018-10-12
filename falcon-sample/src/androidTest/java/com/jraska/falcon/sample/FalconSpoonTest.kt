package com.jraska.falcon.sample

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import com.jraska.falcon.FalconSpoonRule

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.support.test.rule.GrantPermissionRule.grant
import com.jraska.falcon.sample.asserts.BitmapFileAssert.assertThatFile
import org.assertj.core.api.Assertions.assertThat

/**
 * Shows usage of [FalconSpoonRule] compat screenshots
 */
@RunWith(AndroidJUnit4::class)
class FalconSpoonTest {

    //region Fields

    @get:Rule
    var activityRule = ActivityTestRule(SampleActivity::class.java)

    @get:Rule
    val falconSpoonRule = FalconSpoonRule()

    @get:Rule
    val permissionRule = grant(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)!!

    private var screenshotFile: File? = null

    //endregion

    //region Setup Methods

    @After
    @Throws(Exception::class)
    fun after() {
        if (screenshotFile != null) {
            assertThat(screenshotFile!!.delete()).isTrue()
        }
    }

    //endregion

    //region Test methods

    @Test
    @Throws(Exception::class)
    fun takesScreenshotToFile() {
        val tag = "ExampleScreenshot"
        screenshotFile = falconSpoonRule.screenshot(activityRule.activity, tag)

        assertThat(screenshotFile!!.length()).isGreaterThan(0L)
        assertThatFile(screenshotFile).isBitmap
    }

    //endregion
}
