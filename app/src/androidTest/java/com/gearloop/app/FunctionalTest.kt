package com.gearloop.app

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.repo.DemoData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.File
import java.io.FileOutputStream

/**
 * What every functional test starts from.
 *
 * Each test reseeds the database — the same reset a usability session starts
 * with — opens the app on the screen under test, drives it through its
 * semantics tree as a user would (tap, type, scroll), and then checks the
 * result twice: on screen, and in the Room database itself, so a test cannot
 * pass on a screen that only looks right.
 */
abstract class FunctionalTest {

    private companion object {
        val FRAMES = listOf("torq_2", "torq_5", "cond_3", "cond_1", "torq_4", "torq_4", "cond_2")
    }

    @Suppress("DEPRECATION") // the v2 rule queues coroutines instead of running them at once; these tests rely on the latter
    @get:Rule
    val compose = createEmptyComposeRule()

    protected val app: GearLoopApplication get() = ApplicationProvider.getApplicationContext()
    protected val c: AppContainer get() = app.container

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun resetData() {
        // The cases were written for Wed 5 Aug 2026, 09:14, and the sample
        // marketplace is generated relative to "now", so the clock is stood at
        // that moment (and runs on from it) before the reseed: "8 – 10 Aug",
        // a hold on 14–16 Aug, a countdown, all land where the cases expect.
        val started = System.currentTimeMillis()
        val anchor = com.gearloop.app.data.AppClock.toMillis(java.time.LocalDateTime.of(2026, 8, 5, 9, 14))
        com.gearloop.app.data.AppClock.realMillis = { anchor + (System.currentTimeMillis() - started) }
        runBlocking { c.demo.reset() }
        // The capture screens ask for the camera and location as they open; a
        // system prompt over the app would take the screen away from the test.
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            .forEach { runCatching { ui.grantRuntimePermission(app.packageName, it) } }
        // Short toasts, so a confirmation never sits over the next control tapped.
        c.toaster.displayMs = 250
        c.toaster.undoDisplayMs = 400
    }

    @After
    fun closeApp() {
        scenario?.close()
        com.gearloop.app.data.AppClock.realMillis = System::currentTimeMillis
        c.toaster.displayMs = 3_000
        c.toaster.undoDisplayMs = 5_000
    }

    /** Fast-forward to a prepared state with the same calls the screens make. */
    protected fun play(scenario: DemoData.Scenario) = runBlocking { c.demo.play(scenario) }

    /**
     * A precondition, not a step under test: book [listingId] through the calls
     * S-07, S-10 and S-11 make, and return the new reference.
     */
    protected fun book(listingId: String = "torq"): String = runBlocking {
        c.money.startBooking(listingId)
        c.money.signWaiver(com.gearloop.app.data.Demo.nirmal.name)
        when (val result = c.money.confirmDraft()) {
            is com.gearloop.app.data.repo.MoneyRepository.ConfirmResult.Booked -> result.ref
            else -> error("$listingId should be bookable on its default dates, got $result")
        }
    }

    /** The seven angles for [ref]'s gear, as the capture screen lists them. */
    protected fun anglesOf(ref: String): List<String> = runBlocking {
        val booking = requireNotNull(c.db.money().booking(ref))
        val listing = requireNotNull(c.db.discovery().listing(booking.listingId))
        com.gearloop.app.data.CaptureGuide.anglesFor(listing.sport)
    }

    /**
     * Passing frames for [angles] of [ref] in [phase], as the shutter makes
     * them, from the bundled condition photographs: a test can't point a camera.
     */
    protected fun shoot(ref: String, phase: com.gearloop.app.data.Phase, angles: List<String> = anglesOf(ref)) = runBlocking {
        c.evidence.startCapture(phase)
        angles.forEachIndexed { i, angle -> c.evidence.capture(ref, phase, angle, FRAMES[i % FRAMES.size]) }
    }

    /**
     * How many listings S-05 shows with the filters [configure] sets, worked out
     * with the same filter the screen applies, so a test follows the
     * marketplace rather than pinning a count the seed happened to give.
     */
    protected fun resultCount(configure: (com.gearloop.app.data.SearchFilters) -> Unit = {}): Int {
        until { c.discovery.searchable.value.isNotEmpty() }
        return com.gearloop.app.data.SearchFilters().also(configure).apply(c.discovery.searchable.value).size
    }

    /** `7 results`, `1 result`, as S-05 prints a count. */
    protected fun resultsLabel(n: Int): String = if (n == 1) "1 result" else "$n results"

    /** Open the app on [route], as `adb shell am start --es screen <route>` does. */
    protected fun launch(route: String) {
        val intent = Intent(app, MainActivity::class.java).putExtra("screen", route)
        scenario = ActivityScenario.launch(intent)
        compose.waitForIdle()
    }

    /** Close the app and open it again on [route] — proves a value survived the restart. */
    protected fun relaunch(route: String) {
        scenario?.close()
        launch(route)
    }

    /** Read the database directly. */
    protected fun <T> db(block: suspend GearDatabase.() -> T): T = runBlocking { c.db.block() }

    /** Poll until [condition] holds — writes land asynchronously. */
    protected fun until(timeoutMs: Long = 6_000, condition: () -> Boolean) =
        compose.waitUntil(timeoutMs) { runCatching(condition).getOrDefault(false) }

    protected fun text(value: String, substring: Boolean = false): SemanticsNodeInteraction =
        compose.onNodeWithText(value, substring = substring)

    protected fun desc(value: String, substring: Boolean = false): SemanticsNodeInteraction =
        compose.onNodeWithContentDescription(value, substring = substring)

    protected fun exists(value: String, substring: Boolean = false): Boolean =
        compose.onAllNodesWithText(value, substring = substring).fetchSemanticsNodes().isNotEmpty()

    protected fun descExists(value: String, substring: Boolean = false): Boolean =
        compose.onAllNodesWithContentDescription(value, substring = substring).fetchSemanticsNodes().isNotEmpty()

    /** Bring a node on screen if it is in a scrolling column, then tap it. */
    protected fun SemanticsNodeInteraction.tap(): SemanticsNodeInteraction {
        runCatching { performScrollTo() }
        performClick()
        compose.waitForIdle()
        return this
    }

    protected fun tapText(value: String, substring: Boolean = false) = text(value, substring).tap()

    protected fun waitForText(value: String, substring: Boolean = false, timeoutMs: Long = 6_000) {
        try {
            until(timeoutMs) { exists(value, substring) }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Never saw \"$value\". On screen: ${onScreen().joinToString(" | ")}", e)
        }
    }

    /** Every string on screen, for a failure message that says what was there instead. */
    protected fun onScreen(): List<String> = runCatching {
        compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { node -> node.config[androidx.compose.ui.semantics.SemanticsProperties.Text].map { it.text } }
            .distinct()
            .take(80)
    }.getOrDefault(emptyList())

    /** Move the clock to [at], from where it runs on in real time. */
    protected fun clockAt(at: java.time.LocalDateTime) {
        val started = System.currentTimeMillis()
        val anchor = com.gearloop.app.data.AppClock.toMillis(at)
        com.gearloop.app.data.AppClock.realMillis = { anchor + (System.currentTimeMillis() - started) }
    }

    /** Sign in as [handle], as the account switcher does, before a screen is launched. */
    protected fun signInAs(handle: String) = runBlocking { c.session.signIn(handle) }

    /**
     * A full-screen capture, status bar included, for the report. Saved in the
     * app's private files and pulled off the device after the run.
     */
    protected fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dir = File(app.filesDir, "screens").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
