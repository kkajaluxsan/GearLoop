package com.gearloop.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Contrast, measured rather than eyeballed.
 *
 * The light palette was designed against a brief that quotes its ratios; the
 * dark one was derived from it, and deriving a palette is exactly where contrast
 * quietly breaks — a green that reads well on white can vanish on near-black.
 *
 * Rather than trust the preview pane, every pairing the app actually draws is
 * checked here against WCAG 2.1. Both palettes go through the same list, so
 * neither can drift without a test going red.
 *
 * Thresholds: 4.5:1 for body text, 3:1 for large text (≥18.66sp bold or ≥24sp —
 * which covers the display and heading scales) and for the boundary of a
 * meaningful shape such as a border or an icon.
 */
class ContrastTest {

    // ---------------------------------------------------------------- WCAG

    private fun channel(c: Double): Double =
        if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red.toDouble()) +
            0.7152 * channel(color.green.toDouble()) +
            0.0722 * channel(color.blue.toDouble())

    private fun ratio(fg: Color, bg: Color): Double {
        val a = luminance(fg)
        val b = luminance(bg)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun Color.hex() =
        "#%02X%02X%02X".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

    /**
     * This colour laid over [under] at [alpha].
     *
     * Several chips fill themselves with an accent at low alpha and then write
     * on it. The thing text has to contrast against there is the blend, not
     * either colour on its own, so it is worked out rather than approximated
     * with "near enough white".
     */
    private fun Color.over(under: Color, alpha: Float) = Color(
        red = red * alpha + under.red * (1 - alpha),
        green = green * alpha + under.green * (1 - alpha),
        blue = blue * alpha + under.blue * (1 - alpha),
    )

    private class Failure(val label: String, val got: Double, val want: Double, val detail: String)

    private val failures = mutableListOf<Failure>()

    private fun check(label: String, fg: Color, bg: Color, want: Double) {
        val got = ratio(fg, bg)
        if (got < want) {
            failures += Failure(label, got, want, "${fg.hex()} on ${bg.hex()}")
        }
    }

    private fun report() {
        if (failures.isEmpty()) return
        val lines = failures.joinToString("\n") {
            "  %-52s %.2f:1  (needs %.1f:1)  %s".format(it.label, it.got, it.want, it.detail)
        }
        throw AssertionError("${failures.size} pairing(s) below the WCAG threshold:\n$lines")
    }

    private companion object {
        const val TEXT = 4.5
        const val LARGE = 3.0
        const val SHAPE = 3.0
    }

    // ------------------------------------------------------------ the audit

    /**
     * Everything the app actually draws, as foreground-on-background pairs.
     * Run against both palettes so a token can't be correct in only one theme.
     */
    private fun audit(name: String, p: GearPalette) {
        failures.clear()

        // Body copy on each of the three surfaces a screen is built from.
        listOf(
            "app" to p.surfaceApp,
            "card" to p.surfaceCard,
            "sunken" to p.surfaceSunken,
        ).forEach { (surfaceName, surface) ->
            check("$name · primary text on $surfaceName", p.textPrimary, surface, TEXT)
            check("$name · body text on $surfaceName", p.textBody, surface, TEXT)
            check("$name · secondary text on $surfaceName", p.textSecondary, surface, TEXT)
            // Tertiary carries timestamps and captions — small, so it is body-sized.
            check("$name · tertiary text on $surfaceName", p.textTertiary, surface, TEXT)
        }

        // The hi-vis rule the whole design rests on.
        check("$name · text on hi-vis/500", p.textOnHiVis, p.hiVis500, TEXT)
        check("$name · text on hi-vis/600", p.textOnHiVis, p.hiVis600, TEXT)
        check("$name · text on hi-vis/400", p.textOnHiVis, p.hiVis400, TEXT)

        // Dark chips and the two deliberately dark screens.
        check("$name · onDark on surfaceDark", p.textOnDark, p.surfaceDark, TEXT)
        check("$name · onDark on surfaceDarkest", p.textOnDark, p.surfaceDarkest, TEXT)
        check("$name · onDarkMuted on surfaceDark", p.textOnDarkMuted, p.surfaceDark, TEXT)
        check("$name · hi-vis/500 on surfaceDark", p.hiVis500, p.surfaceDark, LARGE)

        // The camera screens.
        check("$name · onDark on viewport", p.textOnDark, p.viewport, TEXT)
        check("$name · hi-vis/500 on viewport", p.hiVis500, p.viewport, LARGE)
        check("$name · onDark on viewportChip", p.textOnDark, p.viewportChip, TEXT)

        // Chips sitting on a photograph — constant across themes by design.
        check("$name · ink on overImage", p.onOverImage, p.overImage, TEXT)
        check("$name · success on overImage", p.successOnImage, p.overImage, TEXT)
        check("$name · danger on overImage", p.dangerOnImage, p.overImage, TEXT)

        // Banner ink on its own tint. The 700 step is what a Banner writes with;
        // the 500 is the spine beside it, which only has to read as a shape.
        check("$name · success ink on success tint", p.success700, p.success100, TEXT)
        check("$name · warning ink on warning tint", p.warning700, p.warning100, TEXT)
        check("$name · danger ink on danger tint", p.danger700, p.danger100, TEXT)
        check("$name · marine ink on marine tint", p.marine700, p.marine100, TEXT)
        check("$name · text on hi-vis tint (safety banner)", p.textPrimary, p.hiVis100, TEXT)

        check("$name · success spine on success tint", p.success500, p.success100, SHAPE)
        check("$name · warning spine on warning tint", p.warning500, p.warning100, SHAPE)
        check("$name · danger spine on danger tint", p.danger500, p.danger100, SHAPE)

        // Semantic colours written as text — status rows, countdowns, deltas.
        // Checked on the app surface too, not only the card: that is the
        // quieter of the two and the one this originally slipped through on.
        listOf("app" to p.surfaceApp, "card" to p.surfaceCard).forEach { (surfaceName, surface) ->
            check("$name · success text on $surfaceName", p.success700, surface, TEXT)
            check("$name · warning text on $surfaceName", p.warning700, surface, TEXT)
            check("$name · danger text on $surfaceName", p.danger700, surface, TEXT)
            check("$name · marine link on $surfaceName", p.marine500, surface, TEXT)

            // The same colours as an icon, a ring or a 2px border.
            check("$name · success accent on $surfaceName", p.success500, surface, SHAPE)
            check("$name · warning accent on $surfaceName", p.warning500, surface, SHAPE)
            check("$name · danger accent on $surfaceName", p.danger500, surface, SHAPE)
        }

        // The fit ramp. It is a safety signal, so every verdict has to read.
        listOf(
            "perfect" to p.fitPerfect,
            "good" to p.fitGood,
            "workable" to p.fitWorkable,
            "poor" to p.fitPoor,
            "blocked" to p.fitBlocked,
        ).forEach { (verdict, colour) ->
            // The stamp is a 2px border *and* the label inside it, both in the
            // same colour — so it is held to the text bar, not the shape one.
            check("$name · fit $verdict on card", colour, p.surfaceCard, TEXT)
            check("$name · fit $verdict on app", colour, p.surfaceApp, TEXT)
        }

        // Trust tiers, in the two jobs they do.
        //
        // The metal is a ring round an avatar and the tick inside a badge, so it
        // is a shape. The ink is the word GOLD written on that badge, so it is
        // text — and the badge fills itself with the metal at 14 % alpha, which
        // is the quieter of the two backgrounds it has to clear. Both were drawn
        // in the metal before this: silver measured 3.3:1 as a label and gold
        // 2.9:1, and neither was being checked here.
        listOf(
            Triple("bronze", p.tierBronze, p.tierBronzeInk),
            Triple("silver", p.tierSilver, p.tierSilverInk),
            Triple("gold", p.tierGold, p.tierGoldInk),
        ).forEach { (tier, metal, ink) ->
            check("$name · tier $tier ring on card", metal, p.surfaceCard, SHAPE)
            check("$name · tier $tier label on card", ink, p.surfaceCard, TEXT)
            check("$name · tier $tier label on app", ink, p.surfaceApp, TEXT)
            check("$name · tier $tier label on its own tint", ink, metal.over(p.surfaceCard, 0.14f), TEXT)
        }

        // Borders do the work shadows would. WCAG sets no ratio for a separator,
        // and this one is meant to be quiet, so the bar is only "visible at all"
        // — enough to catch a border that has vanished into its background.
        check("$name · border on app", p.surfaceBorder, p.surfaceApp, 1.1)
        check("$name · border on card", p.surfaceBorder, p.surfaceCard, 1.1)
        check("$name · strong border on card", p.surfaceBorderStrong, p.surfaceCard, 1.25)

        // The card has to separate from the page behind it.
        check("$name · card against app background", p.surfaceCard, p.surfaceApp, 1.05)

        // Buttons without a hi-vis fill. A secondary button writes its label and
        // draws its outline in ink on chalk/000 — the brightest surface in light,
        // the deepest in dark — and its pressed state on chalk/50. Ghost and
        // default icon buttons write ink on the surface under them, which the
        // surface checks at the top already hold. All three wrote pine/800 until
        // 1.1.1, and pine/800 is a surface step in the dark palette: 1.66:1 on
        // chalk/000, found in internal testing (QA-08) because nothing here
        // checked a button.
        check("$name · secondary button label on its fill", p.textPrimary, p.chalk000, TEXT)
        check("$name · secondary button label, pressed", p.textPrimary, p.chalk50, TEXT)
        check("$name · secondary button outline on the page", p.textPrimary, p.surfaceApp, SHAPE)

        report()
    }

    @Test
    fun `light palette meets contrast across every pairing the app draws`() {
        audit("light", LightPalette)
    }

    @Test
    fun `dark palette meets contrast across every pairing the app draws`() {
        audit("dark", DarkPalette)
    }

    @Test
    fun `wireframe palette meets contrast across every pairing the app draws`() {
        // A wireframe is still read, so it is held to the same bar rather than
        // excused as a working drawing. This is not theoretical: the fit ramp was
        // first written as an even spread of greys, and three of its five
        // verdicts failed here — the stamp draws its border and its label in one
        // colour, which puts a mid grey on a near-white page at 2.1:1.
        audit("wire", WirePalette)
    }

    @Test
    fun `the wireframe palette has no colour left in it`() {
        // The point of the palette is that nothing in it is a hue. A value
        // pasted across from the light set would still contrast correctly and
        // would still be wrong, so channel equality is checked directly.
        val tokens = listOf(
            "pine900" to WirePalette.pine900, "pine500" to WirePalette.pine500,
            "chalk000" to WirePalette.chalk000, "chalk700" to WirePalette.chalk700,
            "hiVis500" to WirePalette.hiVis500, "hiVis100" to WirePalette.hiVis100,
            "marine500" to WirePalette.marine500, "success500" to WirePalette.success500,
            "warning500" to WirePalette.warning500, "danger500" to WirePalette.danger500,
            "tierGold" to WirePalette.tierGold, "fitPerfect" to WirePalette.fitPerfect,
            "fitBlocked" to WirePalette.fitBlocked, "surfaceApp" to WirePalette.surfaceApp,
            "surfaceCard" to WirePalette.surfaceCard, "textPrimary" to WirePalette.textPrimary,
            "viewport" to WirePalette.viewport, "textOnHiVis" to WirePalette.textOnHiVis,
        )
        val coloured = tokens.filter { (_, c) -> c.red != c.green || c.green != c.blue }
        if (coloured.isNotEmpty()) {
            throw AssertionError(
                "not grey: " + coloured.joinToString { (name, c) -> "$name ${c.hex()}" }
            )
        }
    }

    @Test
    fun `the wireframe fit ramp stays five distinguishable steps`() {
        // Compressing the ramp into the range that passes contrast is only worth
        // doing if the five verdicts remain telling apart. Guards against a later
        // contrast fix that collapses two of them onto the same grey.
        val ramp = listOf(
            "perfect" to WirePalette.fitPerfect,
            "good" to WirePalette.fitGood,
            "workable" to WirePalette.fitWorkable,
            "poor" to WirePalette.fitPoor,
            "blocked" to WirePalette.fitBlocked,
        )
        ramp.zipWithNext().forEach { (a, b) ->
            if (luminance(b.second) <= luminance(a.second)) {
                throw AssertionError("fit ${b.first} is not lighter than fit ${a.first}")
            }
            if (ratio(a.second, b.second) < 1.15) {
                throw AssertionError(
                    "fit %s and %s are too close to tell apart: %.2f:1"
                        .format(a.first, b.first, ratio(a.second, b.second))
                )
            }
        }
    }

    // --------------------------------------------------- structural invariants

    @Test
    fun `hi-vis is identical in both themes`() {
        // Safety information must not depend on a setting. If this ever fails,
        // the contrast figure quoted in the README stops being true.
        listOf(
            "600" to (LightPalette.hiVis600 to DarkPalette.hiVis600),
            "500" to (LightPalette.hiVis500 to DarkPalette.hiVis500),
            "400" to (LightPalette.hiVis400 to DarkPalette.hiVis400),
        ).forEach { (step, pair) ->
            val (light, dark) = pair
            if (light != dark) {
                throw AssertionError("hi-vis/$step moved between themes: ${light.hex()} vs ${dark.hex()}")
            }
        }
        if (LightPalette.textOnHiVis != DarkPalette.textOnHiVis) {
            throw AssertionError("text on hi-vis moved between themes")
        }
    }

    @Test
    fun `the contrast figure quoted for text on hi-vis is accurate`() {
        // The README, CLAUDE.md and the design-system board all print a number
        // for this. It used to say 11.2:1, which was never measured; it is 11.9.
        val measured = ratio(LightPalette.textOnHiVis, LightPalette.hiVis500)
        if (measured < 11.85 || measured > 11.95) {
            throw AssertionError(
                "text on hi-vis measures %.2f:1 — the docs quote 11.9:1".format(measured)
            )
        }
    }

    @Test
    fun `anything drawn over a photograph is identical in both themes`() {
        // A photograph does not get darker when someone turns on dark mode.
        val pairs = listOf(
            "overImage" to (LightPalette.overImage to DarkPalette.overImage),
            "onOverImage" to (LightPalette.onOverImage to DarkPalette.onOverImage),
            "successOnImage" to (LightPalette.successOnImage to DarkPalette.successOnImage),
            "dangerOnImage" to (LightPalette.dangerOnImage to DarkPalette.dangerOnImage),
        )
        pairs.forEach { (name, pair) ->
            val (light, dark) = pair
            if (light != dark) throw AssertionError("$name moved between themes")
        }
    }

    @Test
    fun `the dark palette really is darker`() {
        // Guards against a paste error leaving a light value in the dark set.
        val lightSurfaces = listOf(
            LightPalette.surfaceApp, LightPalette.surfaceCard, LightPalette.surfaceSunken,
        ).map { luminance(it) }
        val darkSurfaces = listOf(
            DarkPalette.surfaceApp, DarkPalette.surfaceCard, DarkPalette.surfaceSunken,
        ).map { luminance(it) }

        if (darkSurfaces.max() >= lightSurfaces.min()) {
            throw AssertionError("a dark surface is lighter than a light one")
        }
        if (luminance(DarkPalette.textPrimary) <= luminance(DarkPalette.surfaceCard)) {
            throw AssertionError("dark-theme text is not lighter than the card it sits on")
        }
    }

    @Test
    fun `surface ramps are ordered, so a sunken panel never out-lifts a card`() {
        listOf("light" to LightPalette, "dark" to DarkPalette).forEach { (name, p) ->
            val app = luminance(p.surfaceApp)
            val card = luminance(p.surfaceCard)
            val sunken = luminance(p.surfaceSunken)
            if (p.isDark) {
                if (card <= app) throw AssertionError("$name: card must sit above the page")
                if (sunken > card) throw AssertionError("$name: sunken must not out-lift the card")
            } else {
                if (card < app) throw AssertionError("$name: card must sit above the page")
                if (sunken > card) throw AssertionError("$name: sunken must not out-lift the card")
            }
        }
    }
}
