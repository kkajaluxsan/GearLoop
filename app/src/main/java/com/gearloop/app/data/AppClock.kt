package com.gearloop.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The app's sense of "now": the phone's own clock.
 *
 * Every screen and every repository asks this object rather than calling
 * `LocalDateTime.now()` itself, so a test can stand the clock still or move it
 * forward by replacing [realMillis], and the sample marketplace the database
 * starts with is written relative to the moment it was created rather than to
 * a fixed date.
 */
object AppClock {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Replaced in tests, so a test can stand still or jump forward. */
    @Volatile
    var realMillis: () -> Long = System::currentTimeMillis

    fun now(): LocalDateTime = fromMillis(realMillis())

    fun nowMillis(): Long = realMillis()

    val today: LocalDate get() = now().toLocalDate()

    fun toMillis(time: LocalDateTime): Long = time.atZone(zone).toInstant().toEpochMilli()

    fun fromMillis(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
}
