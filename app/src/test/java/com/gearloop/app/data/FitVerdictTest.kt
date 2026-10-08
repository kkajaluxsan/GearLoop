package com.gearloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verdicts derived from the renter's live profile (FR-01 to FR-03).
 *
 * The prototype stamped each listing with a verdict typed into the dataset. In
 * Milestone 03 the profile is editable and stored, so the stamp is worked out:
 * boards on float, bikes on frame band, everything else keeps the verdict it
 * was listed with. With no measurements the answer is "add your height", never
 * a guess.
 */
class FitVerdictTest {

    private val board = Demo.torq.copy(volumeL = 42, fit = Fit.GOOD)
    private val bike = Demo.trekMarlin.copy(frameBand = "M", fit = Fit.PERFECT)

    @Test
    fun `no measurements means no verdict, for every kind of gear (FR-03)`() {
        listOf(board, bike, Demo.ssBat).forEach { item ->
            assertEquals(Fit.UNKNOWN, FitLogic.verdict(item, null, null))
            assertEquals(Fit.UNKNOWN, FitLogic.verdict(item, 174, null))
        }
    }

    @Test
    fun `missing measurements are reported before anything else (D-10)`() {
        val unknownBoard = board.copy(fit = Fit.UNKNOWN)
        assertEquals("Add your height and weight to get a verdict", FitLogic.reason(unknownBoard, null, null))
        assertEquals("Add your height and weight to get a verdict", FitLogic.reason(board, 174, null))
    }

    @Test
    fun `a board is judged on its volume against the rider's weight`() {
        assertEquals(Fit.PERFECT, FitLogic.verdict(board, 174, 68)) // 38–46 L ideal
        assertEquals(Fit.POOR, FitLogic.verdict(board, 190, 110)) // 39 % short of the ideal float
        assertEquals(Fit.BLOCKED, FitLogic.verdict(board, 190, 140)) // more than 45 % short
        assertEquals("42 L for your 68 kg at intermediate level", FitLogic.reason(board, 174, 68))
    }

    @Test
    fun `a bike in the rider's own band keeps its listed verdict`() {
        assertEquals("M", FitLogic.riderFrameBand(174))
        assertEquals(Fit.PERFECT, FitLogic.verdict(bike, 174, 68))
        assertEquals("M frame for your 174 cm (M band)", FitLogic.reason(bike, 174, 68))
    }

    @Test
    fun `each band away from the rider costs the bike a step`() {
        assertEquals(Fit.WORKABLE, FitLogic.verdict(bike, 165, 60)) // S rider
        assertEquals(Fit.POOR, FitLogic.verdict(bike, 158, 55)) // XS rider
        assertEquals(Fit.POOR, FitLogic.verdict(bike, 186, 85)) // XL rider
    }

    @Test
    fun `gear whose size does not matter stays unknown and says so`() {
        val tent = Demo.allListings.first { it.id == "tent" }.copy(fit = Fit.UNKNOWN)
        assertEquals(Fit.UNKNOWN, FitLogic.verdict(tent, 174, 68))
        assertEquals("Size doesn't decide this one", FitLogic.reason(tent, 174, 68))
    }

    @Test
    fun `other gear keeps the verdict it was listed with`() {
        val helmet = Demo.helmet.copy(fit = Fit.GOOD)
        assertEquals(Fit.GOOD, FitLogic.verdict(helmet, 174, 68))
        assertTrue(FitLogic.reason(helmet, 174, 68).contains("174 cm, 68 kg"))
    }

    @Test
    fun `a poor fit is information, never a gate`() {
        // The verdict is a value, not a permission: nothing in FitLogic refuses a booking.
        val worst = FitLogic.verdict(board, 200, 140)
        assertTrue(worst == Fit.POOR || worst == Fit.BLOCKED)
    }
}
