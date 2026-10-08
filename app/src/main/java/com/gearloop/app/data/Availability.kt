package com.gearloop.app.data

import java.time.LocalDate

/**
 * Which days a unit of gear can be booked on, worked out from the bookings and
 * blocks that actually exist.
 *
 * FR-16 asks that a date shown as bookable is a date that is held, and FR-17
 * that buffer days are first-class objects on both sides. Both calendars — the
 * renter's on S-08 and the owner's on S-27 — call [stateOf], so a day cannot be
 * free on one and booked on the other.
 *
 * A booking leaves [BUFFER_DAYS] behind it: the owner's turnaround time to rinse
 * and check the gear before it goes out again.
 */
object Availability {

    const val BUFFER_DAYS = 1L

    /** A stretch of days held by a booking, ends inclusive. */
    data class Span(val start: LocalDate, val end: LocalDate) {
        operator fun contains(day: LocalDate): Boolean = !day.isBefore(start) && !day.isAfter(end)
        val bufferDay: LocalDate get() = end.plusDays(BUFFER_DAYS)
    }

    fun stateOf(
        day: LocalDate,
        today: LocalDate,
        booked: List<Span>,
        blocked: Set<LocalDate>,
        selection: Span? = null,
    ): DayState = when {
        day.isBefore(today) -> DayState.DISABLED
        selection != null && (day == selection.start || day == selection.end) -> DayState.SELECTED
        selection != null && day in selection -> DayState.IN_RANGE
        booked.any { day in it } -> DayState.BOOKED
        day in blocked -> DayState.BLOCKED
        booked.any { it.bufferDay == day } -> DayState.BUFFER
        selection != null && selection.bufferDay == day -> DayState.BUFFER
        day == today -> DayState.TODAY
        else -> DayState.AVAILABLE
    }

    /** Whether [day] can start or end a new booking at all. */
    fun isFree(day: LocalDate, today: LocalDate, booked: List<Span>, blocked: Set<LocalDate>): Boolean =
        !day.isBefore(today) &&
            day !in blocked &&
            booked.none { day in it || it.bufferDay == day }

    /**
     * Whether a whole new booking fits: every day free, and the day after it free
     * too, because this booking will need its own buffer before the next one.
     */
    fun isRangeFree(span: Span, today: LocalDate, booked: List<Span>, blocked: Set<LocalDate>): Boolean {
        var d = span.start
        while (!d.isAfter(span.end)) {
            if (!isFree(d, today, booked, blocked)) return false
            d = d.plusDays(1)
        }
        return booked.none { span.bufferDay in it }
    }

    /**
     * The next booking a tap produces. The first tap starts a new range; the
     * second, if it is later and the days between are free, closes it; anything
     * else starts again from the day tapped.
     */
    fun extend(
        current: Span?,
        tapped: LocalDate,
        today: LocalDate,
        booked: List<Span>,
        blocked: Set<LocalDate>,
    ): Span? {
        if (!isFree(tapped, today, booked, blocked)) return current
        if (current == null || current.start != current.end || tapped.isBefore(current.start)) {
            return Span(tapped, tapped)
        }
        val candidate = Span(current.start, tapped)
        return if (isRangeFree(candidate, today, booked, blocked)) candidate else Span(tapped, tapped)
    }

    /** The first run of [days] free days on or after [from], for a default selection. */
    fun firstFreeRun(
        from: LocalDate,
        days: Int,
        today: LocalDate,
        booked: List<Span>,
        blocked: Set<LocalDate>,
        searchDays: Int = 120,
    ): Span? {
        var start = if (from.isBefore(today)) today else from
        repeat(searchDays) {
            val span = Span(start, start.plusDays(days - 1L))
            if (isRangeFree(span, today, booked, blocked)) return span
            start = start.plusDays(1)
        }
        return null
    }

    /** The first day after [today] on which a new booking could start, for "NEXT FREE". */
    fun nextFreeDay(today: LocalDate, booked: List<Span>, blocked: Set<LocalDate>): LocalDate? {
        var d = today
        repeat(180) {
            if (isFree(d, today, booked, blocked)) return d
            d = d.plusDays(1)
        }
        return null
    }

    fun daysIn(span: Span): Int = (span.end.toEpochDay() - span.start.toEpochDay() + 1).toInt()
}
