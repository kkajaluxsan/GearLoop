package com.gearloop.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records what the app has to compile on its way to the first frame.
 *
 * GearLoop opens on the cover screen and is judged from that moment, so the
 * profile covers the launch path and the first scroll — the part of the app a
 * cold start actually pays for. Walking a whole flow here would grow the profile
 * without improving the thing being measured.
 */
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun coverScreen() = rule.collect(
        packageName = InstrumentationArgs.targetAppId,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        // The cover is a scrolling control panel; a scroll pulls in the layout
        // and text paths that a static first frame would miss.
        device.waitForIdle()
    }
}

private object InstrumentationArgs {
    val targetAppId: String
        get() = androidx.test.platform.app.InstrumentationRegistry.getArguments()
            .getString("targetAppId")
            ?: error("targetAppId not passed — the plugin normally supplies it")
}
