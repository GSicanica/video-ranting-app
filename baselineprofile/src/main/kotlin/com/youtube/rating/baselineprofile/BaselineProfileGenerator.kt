package com.youtube.rating.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a basic startup baseline profile for the target package.
 *
 * Run via Android Studio "Generate Baseline Profile" or Gradle:
 * `./gradlew :androidApp:generateReleaseBaselineProfile`
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw IllegalStateException("targetAppId not passed as instrumentation runner arg"),
            includeInStartupProfile = true
        ) {
            pressHome()
            startActivityAndWait()

            // Warm up first frame
            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000)
            device.waitForIdle()

            // Exercise home feed scroll to capture more hot paths
            device.findObject(By.scrollable(true))?.run {
                fling(Direction.DOWN)
                fling(Direction.UP)
            }
        }
    }
}
