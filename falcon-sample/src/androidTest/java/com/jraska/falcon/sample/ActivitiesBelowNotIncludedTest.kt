package com.jraska.falcon.sample

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.jraska.falcon.Falcon
import com.jraska.falcon.sample.asserts.BitmapAssert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.longClick
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.greaterThanOrEqualTo

@RunWith(AndroidJUnit4::class)
@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class ActivitiesBelowNotIncludedTest {
    @get:Rule
    var activityRule = ActivityTestRule(SampleActivity::class.java)

    @Before
    fun before() {
        Assume.assumeThat(Build.VERSION.SDK_INT, greaterThanOrEqualTo(Build.VERSION_CODES.ICE_CREAM_SANDWICH))
    }

    @Test
    @Throws(InterruptedException::class)
    fun activityBelowNotIncluded() {

        val originalScreen = Falcon.takeScreenshotBitmap(activityRule.activity)

        val callbacks = GetTopActivityCallbacks()
        activityRule.activity.application.registerActivityLifecycleCallbacks(callbacks)

        // starts new Activity
        onView(withId(R.id.toolbar)).perform(longClick())

        rotateScreen(callbacks.topActivity)
        Thread.sleep(1000) // Lame! Some idling resource should be used but for what to wait???

        val bitmap = Falcon.takeScreenshotBitmap(callbacks.topActivity!!)
        BitmapAssert.assertThatBitmap(bitmap).isRotatedSize(originalScreen)
    }

    private fun rotateScreen(activity: Activity?) {
        val context = InstrumentationRegistry.getTargetContext()
        val orientation = context.resources.configuration.orientation

      if (orientation == Configuration.ORIENTATION_PORTRAIT)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      else
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    internal class GetTopActivityCallbacks : Application.ActivityLifecycleCallbacks {

        internal var topActivity: Activity? = null

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {
            topActivity = activity
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {
            topActivity = activity
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }
}
