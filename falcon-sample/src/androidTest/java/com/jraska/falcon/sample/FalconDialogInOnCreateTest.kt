package com.jraska.falcon.sample

import android.support.test.espresso.Espresso
import android.support.test.rule.ActivityTestRule
import com.jraska.falcon.Falcon
import org.junit.Rule
import org.junit.Test

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.jraska.falcon.sample.asserts.BitmapAssert.assertThatBitmap

class FalconDialogInOnCreateTest {
    @get:Rule
    var activityRule = ActivityTestRule(DialogOnCreate::class.java)

    // Tests https://github.com/jraska/Falcon/issues/11
    @Test
    fun takesDialogOnCreate() {
        val activity = activityRule.activity
        onView(withText(DialogOnCreate.DIALOG_TITLE)).check(matches(isDisplayed()))

        val withDialog = Falcon.takeScreenshotBitmap(activity)
        Espresso.pressBack()
        onView(withText(DialogOnCreate.DIALOG_TITLE)).check(doesNotExist())

        val afterDialogDismiss = Falcon.takeScreenshotBitmap(activity)

        assertThatBitmap(withDialog).isDarkerThan(afterDialogDismiss)
    }
}
