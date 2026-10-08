package com.gearloop.app.ui.feedback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A toast has to expire whether or not any screen is showing it (D-18). It used
 * to be timed by the screen, so a toast whose screen closed first came back,
 * with a live Undo, the next time the app opened.
 */
class ToasterTest {

    private val toaster = Toaster(CoroutineScope(SupervisorJob() + Dispatchers.Default)).apply {
        displayMs = 50
        undoDisplayMs = 80
    }

    private fun waitUntil(timeoutMs: Long = 2_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < end) Thread.sleep(10)
    }

    @Test
    fun `a toast expires with no screen showing it`() {
        toaster.show("Saved")
        assertNotNull(toaster.current.value)
        waitUntil { toaster.current.value == null }
        assertNull(toaster.current.value)
    }

    @Test
    fun `an undo toast expires too`() {
        toaster.show("Removed", onUndo = {})
        assertEquals("Undo", toaster.current.value?.undoLabel)
        waitUntil { toaster.current.value == null }
        assertNull(toaster.current.value)
    }

    @Test
    fun `an old timer never dismisses a newer toast`() {
        toaster.displayMs = 300
        toaster.show("First")
        Thread.sleep(150)
        toaster.show("Second")
        Thread.sleep(200) // the first toast's timer has fired by now
        assertEquals("Second", toaster.current.value?.text)
    }
}
