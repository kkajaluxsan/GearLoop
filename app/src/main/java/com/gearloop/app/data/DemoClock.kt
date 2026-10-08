package com.gearloop.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The app's sense of "now".
 *
 * Every date in the seeded dataset is written against Wednesday 5 August 2026 —
 * the Torq is picked up on Saturday the 8th, Thusara's request expires this
 * afternoon, Ayesha's return is already overdue. Reading the device clock would
 * put all of that two months in the past and turn every countdown negative.
 *
 * So the world starts at 09:14 on the 5th when the database is seeded, and from
 * then on runs at real speed: a booking made now is stamped a few minutes after
 * 09:14, a request's countdown really does tick down. Resetting the demo moves
 * the anchor, which is what puts a usability session back at the start.
 *
 * Production would read the system clock here and nothing else would change,
 * because every screen and every repository asks this object rather than
 * calling `LocalDateTime.now()` itself.
 */
object DemoClock {

    /** The moment the seeded dataset describes. */
    val WORLD_START: LocalDateTime = LocalDateTime.of(2026, 8, 5, 9, 14)

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Real wall-clock millis at which the world was at [WORLD_START]. */
    @Volatile
    var anchorRealMillis: Long = System.currentTimeMillis()

    /** Replaced in tests, so a test can stand still or jump forward. */
    @Volatile
    var realMillis: () -> Long = System::currentTimeMillis

    fun now(): LocalDateTime = WORLD_START.plusNanos((realMillis() - anchorRealMillis) * 1_000_000)

    fun nowMillis(): Long = toMillis(now())

    val today: LocalDate get() = now().toLocalDate()

    fun toMillis(time: LocalDateTime): Long = time.atZone(zone).toInstant().toEpochMilli()

    fun fromMillis(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    /** Start the world again from [WORLD_START], as of this instant. */
    fun restart(): Long {
        anchorRealMillis = realMillis()
        return anchorRealMillis
    }
}
