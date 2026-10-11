package com.gearloop.app.data

/**
 * Number formatting. Kept here so no screen ever hand-writes a rupee or a
 * measurement string.
 *
 * Every number in the app that a user could act on passes through one of these,
 * which is what stops the same value appearing two different ways on two
 * different screens.
 */

/** The real minus sign, U+2212. A hyphen is a word-joiner and sets too short. */
const val MINUS = "−"

fun grouped(value: Int): String {
    val s = kotlin.math.abs(value).toString()
    val sb = StringBuilder()
    for ((i, ch) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(ch)
    }
    return if (value < 0) "$MINUS$sb" else sb.toString()
}

/** `LKR 2,800` */
fun lkr(value: Int): String = "LKR ${grouped(value)}"

/** `−840` — always carries its sign, so a credit and a debit can't be confused. */
fun signed(value: Int): String = if (value < 0) "$MINUS${grouped(-value)}" else "+${grouped(value)}"

/**
 * `2.4 km` — one decimal place, always.
 *
 * Below 100 m the decimal stops meaning anything and "0.0 km away" reads like
 * missing data, so the short range gets words instead of a number. Anything at
 * or over 10 km rounds to whole kilometres, because nobody navigates by the
 * tenth at that distance.
 */
fun km(value: Double, suffix: String = ""): String {
    val tail = if (suffix.isEmpty()) "" else " $suffix"
    return when {
        value < 0.1 -> "Nearby$tail"
        value < 10.0 -> "${(Math.round(value * 10) / 10.0)} km$tail"
        else -> "${Math.round(value)} km$tail"
    }
}

/** `2.4 km away` — the form used wherever the distance is to someone else's gear. */
fun kmAway(value: Double): String = km(value, "away")

/**
 * `9.7134°N, 80.0246°E` — four decimals is about 11 m, close enough to say
 * where a photo was taken. A hemisphere letter, never a sign: "−122°E" is a
 * contradiction a reader has to stop and resolve.
 */
fun coordinates(latitude: Double, longitude: Double): String = String.format(
    java.util.Locale.ENGLISH, "%.4f°%s, %.4f°%s",
    kotlin.math.abs(latitude), if (latitude >= 0) "N" else "S",
    kotlin.math.abs(longitude), if (longitude >= 0) "E" else "W",
)

// ------------------------------------------------------------------ dates
//
// Milestone 03 made dates data rather than copy, so they are formatted here once
// for the same reason money is: two screens quoting one booking must not print
// its dates two different ways.

// English month and day names, not Locale.UK: the UK data abbreviates September
// to "Sept", one letter longer than every other month, which broke the mono
// date columns and the "30 Aug – 2 Sep" form documented below (D-17).
private val uk = java.util.Locale.ENGLISH
private val fmtDayMonth = java.time.format.DateTimeFormatter.ofPattern("d MMM", uk)
private val fmtDay = java.time.format.DateTimeFormatter.ofPattern("d", uk)
private val fmtWeekday = java.time.format.DateTimeFormatter.ofPattern("EEE", uk)
private val fmtYear = java.time.format.DateTimeFormatter.ofPattern("yyyy", uk)
private val fmtTime = java.time.format.DateTimeFormatter.ofPattern("HH:mm", uk)
private val fmtMonthYear = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", uk)

/** `8 Aug` */
fun dayMonth(date: java.time.LocalDate): String = date.format(fmtDayMonth)

/** `8 – 10 Aug`, or `30 Aug – 2 Sep` across a month end. */
fun dateRange(start: java.time.LocalDate, end: java.time.LocalDate): String =
    if (start.month == end.month) "${start.format(fmtDay)} – ${end.format(fmtDayMonth)}"
    else "${start.format(fmtDayMonth)} – ${end.format(fmtDayMonth)}"

/** `Sat 8 – Mon 10 Aug 2026` */
fun dateRangeLong(start: java.time.LocalDate, end: java.time.LocalDate): String {
    val head = "${start.format(fmtWeekday)} ${start.format(fmtDay)}"
    val tail = "${end.format(fmtWeekday)} ${end.format(fmtDayMonth)} ${end.format(fmtYear)}"
    return if (start.month == end.month) "$head – $tail"
    else "$head ${start.format(java.time.format.DateTimeFormatter.ofPattern("MMM", uk))} – $tail"
}

/** `August 2026` */
fun monthYear(month: java.time.YearMonth): String = month.format(fmtMonthYear)

/** `Sat 8 Aug, 09:00` */
fun dayTime(time: java.time.LocalDateTime): String =
    "${time.format(fmtWeekday)} ${time.format(fmtDayMonth)}, ${time.format(fmtTime)}"

/** `5 Aug 2026, 09:14` — the form a signature or a ruling is stamped with. */
fun stamp(time: java.time.LocalDateTime): String =
    "${time.format(fmtDayMonth)} ${time.format(fmtYear)}, ${time.format(fmtTime)}"

/** `08 AUG 08:57` — capture metadata, which is set in an uppercase mono style. */
fun metaStamp(time: java.time.LocalDateTime): String =
    time.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm", uk)).uppercase(uk)

/** `09:42` */
fun clockTime(time: java.time.LocalDateTime): String = time.format(fmtTime)

/** `Tue 09:42` — how the message thread labels each message. */
fun chatTime(time: java.time.LocalDateTime): String = "${time.format(fmtWeekday)} ${time.format(fmtTime)}"

/**
 * `09:42` today, then `Yesterday`, then the weekday, then the date — the order
 * a notification list ages its rows in.
 */
fun relativeTime(time: java.time.LocalDateTime, now: java.time.LocalDateTime): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(time.toLocalDate(), now.toLocalDate())
    return when {
        days <= 0L -> time.format(fmtTime)
        days == 1L -> "Yesterday"
        days < 7L -> time.format(fmtWeekday)
        else -> time.format(fmtDayMonth)
    }
}

/**
 * `2d 04h 12m`, `3h 12m` or `12m` — a countdown at the precision that matters at
 * that distance. Anything already past reads as zero rather than going negative.
 */
fun countdown(millis: Long): String {
    val totalMin = (millis.coerceAtLeast(0) / 60_000)
    val d = totalMin / (60 * 24)
    val h = (totalMin / 60) % 24
    val m = totalMin % 60
    return when {
        d > 0 -> "${d}d ${"%02d".format(h)}h ${"%02d".format(m)}m"
        h > 0 -> "${h}h ${"%02d".format(m)}m"
        else -> "${m}m"
    }
}

/** `5 days`, `3 h`, `12 min` — a gap said the way a person would, in its largest unit. */
fun roughDuration(millis: Long): String {
    val min = kotlin.math.abs(millis) / 60_000
    val days = min / (60 * 24)
    return when {
        days > 0 -> "$days day${if (days == 1L) "" else "s"}"
        min >= 60 -> "${min / 60} h"
        else -> "$min min"
    }
}

/** `02:14:06` — the big pickup clock, hours running past 24 if they have to. */
fun stopwatch(millis: Long): String {
    val s = millis.coerceAtLeast(0) / 1000
    return "%02d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60)
}
