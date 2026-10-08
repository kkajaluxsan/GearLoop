package com.gearloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The formatters every screen prints dates, money and clocks through. A date
 * written two ways on two screens is the inconsistency the dataset is prone to,
 * so the forms are pinned here.
 */
class FormatTest {

    @Test
    fun `money is grouped, and a debit carries a real minus sign`() {
        assertEquals("LKR 9,240", lkr(9_240))
        assertEquals("25,000", grouped(25_000))
        assertEquals("−840", signed(-840))
        assertEquals("+6,400", signed(6_400))
    }

    @Test
    fun `a range inside one month names the month once`() {
        assertEquals("8 – 10 Aug", dateRange(LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 10)))
    }

    @Test
    fun `a range across a month end names both months`() {
        assertEquals("30 Aug – 2 Sep", dateRange(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 2)))
    }

    @Test
    fun `the long form is the one the design prints on S-11`() {
        assertEquals(Demo.BOOKING_DATES_LONG, dateRangeLong(LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 10)))
    }

    @Test
    fun `countdowns keep the precision that matters at that distance`() {
        val minute = 60_000L
        assertEquals("12m", countdown(12 * minute))
        assertEquals("3h 05m", countdown((3 * 60 + 5) * minute))
        assertEquals("2d 04h 12m", countdown(((2 * 24 + 4) * 60 + 12) * minute))
        assertEquals("0m", countdown(-5 * minute))
    }

    @Test
    fun `the pickup clock runs past a day rather than wrapping`() {
        assertEquals("26:00:05", stopwatch((26 * 3600 + 5) * 1000L))
    }

    @Test
    fun `notification times age from clock time to weekday to date`() {
        val now = LocalDateTime.of(2026, 8, 5, 9, 14)
        assertEquals("08:15", relativeTime(now.minusMinutes(59), now))
        assertEquals("Yesterday", relativeTime(now.minusDays(1), now))
        assertEquals("Mon", relativeTime(LocalDateTime.of(2026, 8, 3, 10, 0), now))
        assertEquals("20 Jul", relativeTime(LocalDateTime.of(2026, 7, 20, 10, 0), now))
    }
}
