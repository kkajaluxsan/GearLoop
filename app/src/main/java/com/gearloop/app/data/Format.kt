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
