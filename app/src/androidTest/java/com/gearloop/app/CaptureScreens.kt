package com.gearloop.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Phase
import com.gearloop.app.data.repo.DemoData
import com.gearloop.app.data.repo.Seed
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not a test case: the screenshot set for the report.
 *
 * Opens every screen in the state a reader needs to see it in (a booking for
 * S-12, a live rental for S-17, an open claim for S-34) and saves a capture
 * with nothing transient on it. It is skipped unless asked for, so it never
 * counts among TC-01 to TC-68:
 *
 *     gradlew connectedDebugAndroidTest
 *       -Pandroid.testInstrumentationRunnerArguments.class=com.gearloop.app.CaptureScreens
 *       -Pandroid.testInstrumentationRunnerArguments.capture=true
 *       -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
 */
@RunWith(AndroidJUnit4::class)
class CaptureScreens : FunctionalTest() {

    private val ref = Demo.BOOKING_REF

    private fun at(route: String) {
        relaunch(route)
        Thread.sleep(1_500) // photographs decode off the main thread
        shot("app_$route")
    }

    private fun reset() = runBlocking { c.demo.reset() }

    private fun capture(phase: Phase, angles: List<String> = c.evidence.angles) = runBlocking {
        c.evidence.startCapture(phase)
        angles.forEach { c.evidence.capture(ref, phase, it) }
    }

    @Test
    fun captureEveryScreen() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("capture") == "true")

        // Discovery, checkout and the screens that read the seed as it is.
        listOf(
            "s01", "s02", "s03", "s04", "s05", "s06", "s07", "s29", "s30", "s39", "s42",
            "s08", "s09", "s10", "s11",
            "s32", "s35", "s37", "s41",
            "s19", "s20", "s21", "s22", "s23", "s24", "s25", "s26", "s27", "s28", "s36",
        ).forEach { reset(); at(it) }

        // After a booking: confirmation, the thread it writes to, the bell.
        reset(); book()
        listOf("s12", "s33", "s38", "s13", "s14").forEach { at(it) }

        // Mid-handover: three angles shot, one mark recorded, then all seven.
        runBlocking { c.evidence.enterCode(ref, Seed.handoverCode(ref)) }
        capture(Phase.PICKUP, c.evidence.angles.take(3))
        at("s15")
        runBlocking {
            c.evidence.addMark(ref, Phase.PICKUP, Demo.PICKUP_MARK.area, Demo.PICKUP_MARK.severity, Demo.PICKUP_MARK.note, 0.5f, 0.86f)
        }
        at("s15b")
        capture(Phase.PICKUP)
        at("s16")

        // A live rental, on the renter's list and on its own screen.
        play(DemoData.Scenario.MID_RENTAL)
        at("s17")
        at("s31")

        // Back with a new mark: the paired comparison and the difference.
        play(DemoData.Scenario.MID_RENTAL)
        capture(Phase.RETURN)
        runBlocking {
            c.evidence.addMark(ref, Phase.RETURN, Demo.RETURN_MARK.area, DamageMark.Severity.FUNCTIONAL, Demo.RETURN_MARK.note, 0.5f, 0.86f)
            c.evidence.finishReturn(ref)
        }
        at("s18")
        at("s18b")

        // The claim, from both sides of the desk.
        play(DemoData.Scenario.CLAIM_OPEN)
        at("s34")
        at("s40")
    }
}
