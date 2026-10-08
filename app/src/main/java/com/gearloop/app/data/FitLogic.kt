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

    // ------------------------------------------------------- Milestone 03
    //
    // The prototype stamped each listing with a verdict typed into the dataset.
    // Now the profile is real and editable, so the verdict is derived wherever
    // there is something to derive it from, and every screen that shows a stamp
    // re-reads it when the profile changes.

    private val frameBands = listOf("XS", "S", "M", "L", "XL")

    /** The band letter a rider of this height falls in — `M` for 174 cm. */
    fun riderFrameBand(heightCm: Int): String = bikeFrame(heightCm).substringBefore(' ')

    /**
     * The verdict for [item] given the rider, or [Fit.UNKNOWN] when the profile
     * has no measurements — FR-03: say "add your height" rather than guess.
     *
     *  - Boards with a known volume are judged on float ([verdictForVolume]).
     *  - Bikes are judged on frame band: same band keeps the listing's own
     *    verdict, one band out is workable, two is poor, three is the wrong size.
     *  - Everything else keeps the verdict the listing was given, because there
     *    is nothing in two numbers that could improve on it.
     *
     * Never used to block a booking. A poor fit is information, not a gate.
     */
    fun verdict(item: GearItem, heightCm: Int?, weightKg: Int?): Fit {
        if (item.fit == Fit.UNKNOWN) return Fit.UNKNOWN
        if (heightCm == null || weightKg == null) return Fit.UNKNOWN
        item.volumeL?.let { return verdictForVolume(it, weightKg) }
        item.frameBand?.let { band ->
            val have = frameBands.indexOf(band)
            val want = frameBands.indexOf(riderFrameBand(heightCm))
            if (have >= 0 && want >= 0) {
                return when (kotlin.math.abs(have - want)) {
                    0 -> item.fit
                    1 -> Fit.WORKABLE
                    2 -> Fit.POOR
                    else -> Fit.BLOCKED
                }
            }
        }
        return item.fit
    }

    /**
     * The sentence beside the stamp on S-07, in the rider's own units.
     *
     * Missing measurements are checked first. The item passed in already carries
     * its live verdict, which is UNKNOWN for *every* item when there are no
     * measurements — checking the verdict first told a renter with no profile
     * that size "doesn't decide" a surfboard, beside a stamp saying "add your
     * height" (D-10, found by TC-03).
     */
    fun reason(item: GearItem, heightCm: Int?, weightKg: Int?): String = when {
        heightCm == null || weightKg == null -> "Add your height and weight to get a verdict"
        item.fit == Fit.UNKNOWN -> "Size doesn't decide this one"
        item.volumeL != null -> fitExplanation(item.volumeL, weightKg)
        item.frameBand != null -> "${item.frameBand} frame for your $heightCm cm (${riderFrameBand(heightCm)} band)"
        else -> "Sized ${item.sizeLabel} · checked against $heightCm cm, $weightKg kg"
    }
}
