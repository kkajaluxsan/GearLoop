package com.gearloop.app.data

import com.gearloop.app.data.Availability.Span
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The availability rules both calendars draw from (FR-16, FR-17).
 *
 * S-08 and S-27 call the same functions, so a day cannot be free on the
 * renter's calendar and held on the owner's. The cases below use the seeded
 * world: today is Wednesday 5 August 2026, Dinesh holds the Torq from 14 to 16
 * August, and Kavitha has blocked the 24th and 25th for a fin-box repair.
 */
class AvailabilityTest {

    private fun aug(day: Int): LocalDate = LocalDate.of(2026, 8, day)
    private val today = aug(5)
    private val dinesh = listOf(Span(aug(14), aug(16)))
    private val repair = setOf(aug(24), aug(25))

    private fun state(day: Int, selection: Span? = null) =
        Availability.stateOf(aug(day), today, dinesh, repair, selection)

    @Test
    fun `each day gets the state the legend promises`() {
        assertEquals(DayState.DISABLED, state(4))
        assertEquals(DayState.TODAY, state(5))
        assertEquals(DayState.AVAILABLE, state(9))
        assertEquals(DayState.BOOKED, state(15))
        assertEquals(DayState.BUFFER, state(17))
        assertEquals(DayState.BLOCKED, state(24))
    }

    @Test
    fun `a selection draws its ends, its middle and its own buffer day`() {
        val pick = Span(aug(8), aug(10))
        assertEquals(DayState.SELECTED, state(8, pick))
        assertEquals(DayState.IN_RANGE, state(9, pick))
        assertEquals(DayState.SELECTED, state(10, pick))
        assertEquals(DayState.BUFFER, state(11, pick))
    }

    @Test
    fun `a booking cannot end the day before another begins`() {
        // 10–13 Aug would leave no turnaround day before Dinesh's pickup on the 14th.
        assertFalse(Availability.isRangeFree(Span(aug(10), aug(13)), today, dinesh, repair))
        assertTrue(Availability.isRangeFree(Span(aug(10), aug(12)), today, dinesh, repair))
    }

    @Test
    fun `a range across a booking, a buffer or a block is refused`() {
        assertFalse(Availability.isRangeFree(Span(aug(12), aug(18)), today, dinesh, repair))
        assertFalse(Availability.isRangeFree(Span(aug(17), aug(18)), today, dinesh, repair))
        assertFalse(Availability.isRangeFree(Span(aug(22), aug(24)), today, dinesh, repair))
        assertFalse(Availability.isRangeFree(Span(aug(3), aug(6)), today, dinesh, repair))
    }

    @Test
    fun `the first tap starts a range and a later second tap closes it`() {
        val first = Availability.extend(Span(aug(8), aug(10)), aug(18), today, dinesh, repair)
        assertEquals(Span(aug(18), aug(18)), first)
        val second = Availability.extend(first, aug(20), today, dinesh, repair)
        assertEquals(Span(aug(18), aug(20)), second)
    }

    @Test
    fun `an earlier second tap starts again from the day tapped`() {
        val start = Span(aug(20), aug(20))
        assertEquals(Span(aug(18), aug(18)), Availability.extend(start, aug(18), today, dinesh, repair))
    }

    @Test
    fun `a range that would swallow a booking starts again instead`() {
        val start = Span(aug(8), aug(8))
        assertEquals(Span(aug(20), aug(20)), Availability.extend(start, aug(20), today, dinesh, repair))
    }

    @Test
    fun `tapping a booked, buffer, blocked or past day changes nothing`() {
        val pick = Span(aug(8), aug(10))
        listOf(aug(15), aug(17), aug(24), aug(4)).forEach { day ->
            assertEquals(pick, Availability.extend(pick, day, today, dinesh, repair))
        }
    }

    @Test
    fun `the default dates for the Torq are the designed 8 to 10 August`() {
        val saturday = aug(8)
        assertEquals(Span(aug(8), aug(10)), Availability.firstFreeRun(saturday, 3, today, dinesh, repair))
        assertEquals(3, Availability.daysIn(Span(aug(8), aug(10))))
    }

    @Test
    fun `the first free run skips a booking and its buffer`() {
        assertEquals(Span(aug(18), aug(20)), Availability.firstFreeRun(aug(13), 3, today, dinesh, repair))
    }

    @Test
    fun `next free day is today when nothing holds it`() {
        assertEquals(today, Availability.nextFreeDay(today, dinesh, repair))
        val busy = listOf(Span(aug(5), aug(6)))
        assertEquals(aug(8), Availability.nextFreeDay(today, busy, emptySet()))
    }

    @Test
    fun `nothing is free when every day for half a year is blocked`() {
        val everything = (0L until 200L).map { today.plusDays(it) }.toSet()
        assertNull(Availability.nextFreeDay(today, emptyList(), everything))
    }
}
