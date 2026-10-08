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

    @Suppress("DEPRECATION") // the v2 rule queues coroutines instead of running them at once; these tests rely on the latter
    @get:Rule
    val compose = createEmptyComposeRule()

    protected val app: GearLoopApplication get() = ApplicationProvider.getApplicationContext()
    protected val c: AppContainer get() = app.container

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun resetData() {
        runBlocking { c.demo.reset() }
        // Short toasts, so a confirmation never sits over the next control tapped.
        c.toaster.displayMs = 250
        c.toaster.undoDisplayMs = 400
    }

    @After
    fun closeApp() {
        scenario?.close()
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
        requireNotNull(c.money.confirmDraft()) { "$listingId should be bookable on its default dates" }
    }

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

    protected fun waitForText(value: String, substring: Boolean = false, timeoutMs: Long = 6_000) =
        until(timeoutMs) { exists(value, substring) }

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
