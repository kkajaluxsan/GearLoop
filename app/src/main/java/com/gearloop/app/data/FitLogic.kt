package com.gearloop.app.data

import kotlin.math.roundToInt

/**
 * Fit derivation.
 *
 * This is the product's argument in about forty lines: two numbers a person
 * already knows about themselves turn into the sizes that actually matter for
 * the gear. The panel on S-02 recalculates from these functions as the values
 * change, so the demo is showing real derivation rather than a static mock-up.
 *
 * The formulas are the rules of thumb shops use, not medical advice — the point
 * is that the app takes the question seriously enough to answer it.
 */
object FitLogic {

    /** `188` → `6'2"` */
    fun cmToFeetInches(cm: Int): String {
        val totalInches = (cm / 2.54).roundToInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return "$feet'$inches\""
    }

    /**
     * Board volume in litres. An intermediate surfer rides roughly 0.56–0.68 L
     * per kilo of body weight; beginners want more float, experts less.
     */
    fun boardVolume(weightKg: Int): IntRange {
        val low = (weightKg * 0.56).roundToInt()
        val high = (weightKg * 0.68).roundToInt()
        return low..high
    }

    /** Board length scales off height rather than weight. */
    fun boardLength(heightCm: Int): Pair<String, String> {
        val low = (heightCm * 1.08).roundToInt()
        val high = (heightCm * 1.167).roundToInt()
        return cmToFeetInches(low) to cmToFeetInches(high)
    }

    fun surfboardSummary(heightCm: Int, weightKg: Int): String {
        val (low, high) = boardLength(heightCm)
        val vol = boardVolume(weightKg)
        return "$low – $high · ${vol.first}–${vol.last} L"
    }

    /** All-mountain alpine ski: chin to nose, so height less 5–10 cm. */
    fun skiSummary(heightCm: Int): String = "${heightCm - 10} – ${heightCm - 5} cm"

    /** Frame size bands, with the seat-tube measurement riders actually quote. */
    fun bikeFrame(heightCm: Int): String = when {
        heightCm < 160 -> "XS (46–48)"
        heightCm < 168 -> "S (49–51)"
        heightCm < 176 -> "M (52–54)"
        heightCm < 184 -> "L (55–57)"
        else -> "XL (58–60)"
    }

    /** The three rows on the derived-size panel, recomputed on every change. */
    fun derived(heightCm: Int, weightKg: Int): List<Pair<String, String>> = listOf(
        "SURFBOARD" to surfboardSummary(heightCm, weightKg),
        "ALPINE SKI" to skiSummary(heightCm),
        "BIKE FRAME" to bikeFrame(heightCm),
    )

    /**
     * Verdict for a specific board, given the rider. Drives the FitStamp.
     *
     * Volume is the honest signal for a surfboard — a board with far too little
     * float for your weight is not a preference problem, you simply will not
     * catch anything on it.
     */
    fun verdictForVolume(boardVolume: Int, weightKg: Int): Fit {
        val ideal = boardVolume(weightKg)
        val mid = (ideal.first + ideal.last) / 2f
        val drift = kotlin.math.abs(boardVolume - mid) / mid
        return when {
            boardVolume in ideal -> Fit.PERFECT
            drift <= 0.15f -> Fit.GOOD
            drift <= 0.30f -> Fit.WORKABLE
            drift <= 0.45f -> Fit.POOR
            else -> Fit.BLOCKED
        }
    }

    /** Supporting line under the stamp — says *why*, which is the whole point. */
    fun fitExplanation(boardVolume: Int, weightKg: Int, level: String = "intermediate"): String =
        "$boardVolume L for your $weightKg kg at $level level"
}
