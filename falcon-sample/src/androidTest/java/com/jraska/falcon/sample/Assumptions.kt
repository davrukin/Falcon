package com.jraska.falcon.sample

import android.os.Build
import org.junit.Assume

import org.hamcrest.Matchers.`is`

object Assumptions {

    private val isContinuousIntegration: Boolean
        get() = BuildConfig.CI_BUILD

    fun assumeNoCI() {
        Assume.assumeThat("Test should not run on CI server.", isContinuousIntegration, `is`(false))
    }

}
