package com.gearloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fit derivation — the product's central claim, in about forty lines.
 *
 * S-02 is the screen the viva leads with: two numbers a person already knows
 * turn into the sizes that actually matter. If this is wrong the demo is
 * confidently wrong, which is worse than being obviously broken.
 */
class FitLogicTest {

    @Test
    fun `centimetres convert to feet and inches`() {
        assertEquals("5'9\"", FitLogic.cmToFeetInches(175))
        assertEquals("6'2\"", FitLogic.cmToFeetInches(188))
        assertEquals("5'0\"", FitLogic.cmToFeetInches(152))
    }

    @Test
    fun `board volume brackets the rider's weight`() {
        val volume = FitLogic.boardVolume(68)
        assertEquals(38, volume.first)
        assertEquals(46, volume.last)
        assertTrue("the range must not be inverted", volume.first < volume.last)
    }

    @Test
    fun `a heavier rider is given a bigger board`() {
        val light = FitLogic.boardVolume(60)
        val heavy = FitLogic.boardVolume(90)
        assertTrue(heavy.first > light.first)
        assertTrue(heavy.last > light.last)
    }

    @Test
    fun `bike frame bands are continuous and ascending`() {
        // Every height in a plausible range must produce a frame, and the bands
        // must not overlap or leave a gap someone could fall into.
        val bands = (140..200).map { FitLogic.bikeFrame(it) }
        assertTrue(bands.none { it.isBlank() })
        assertEquals(listOf("XS (46–48)", "S (49–51)", "M (52–54)", "L (55–57)", "XL (58–60)"),
            bands.distinct())
    }

    @Test
    fun `ski length sits below the rider's height`() {
        val summary = FitLogic.skiSummary(174)
        assertEquals("164 – 169 cm", summary)
    }

    @Test
    fun `the derived panel always returns all three rows`() {
        val rows = FitLogic.derived(174, 68)
        assertEquals(3, rows.size)
        assertEquals(listOf("SURFBOARD", "ALPINE SKI", "BIKE FRAME"), rows.map { it.first })
        assertTrue("no row may be blank", rows.none { it.second.isBlank() })
    }

    @Test
    fun `the derived panel changes when either number changes`() {
        // This is the claim S-02 makes out loud: move a stepper and the sizes
        // move. A static panel would pass every other test here.
        val base = FitLogic.derived(174, 68)
        assertTrue("height must affect the panel", FitLogic.derived(190, 68) != base)
        assertTrue("weight must affect the panel", FitLogic.derived(174, 85) != base)
    }

    @Test
    fun `a board inside the ideal range is a perfect fit`() {
        val ideal = FitLogic.boardVolume(68)
        assertEquals(Fit.PERFECT, FitLogic.verdictForVolume(ideal.first, 68))
        assertEquals(Fit.PERFECT, FitLogic.verdictForVolume(ideal.last, 68))
    }

    @Test
    fun `the verdict degrades as the board gets further from ideal`() {
        // The demo relies on the Torq's 42 L reading PERFECT for a 68 kg rider.
        assertEquals(Fit.PERFECT, FitLogic.verdictForVolume(42, 68))

        val order = listOf(Fit.PERFECT, Fit.GOOD, Fit.WORKABLE, Fit.POOR, Fit.BLOCKED)
        val verdicts = listOf(42, 34, 28, 22, 12).map { FitLogic.verdictForVolume(it, 68) }
        val ranks = verdicts.map { order.indexOf(it) }
        assertEquals(
            "moving further from ideal must never improve the verdict",
            ranks.sorted(), ranks,
        )
    }

    @Test
    fun `the explanation names the numbers it was given`() {
        val text = FitLogic.fitExplanation(42, 68)
        assertTrue(text.contains("42"))
        assertTrue(text.contains("68"))
    }
}
