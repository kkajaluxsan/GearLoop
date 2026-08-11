package com.gearloop.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * GearLoop colour tokens (design brief §3.1).
 *
 * Five ramps, each with one job:
 *  - Pine    structure, dark surfaces, primary text
 *  - Chalk   neutrals — cool grey-green, deliberately not warm cream
 *  - HiVis   primary action and safety information only, never decorative
 *  - Marine  informational, links, the swap rail's identity colour
 *  - Fit     its own scale, because fit is a distinct system from status
 *
 * Every token below resolves against whichever [GearPalette] is in scope, which
 * is how one call site renders correctly in both themes. That is also why these
 * are `@Composable` getters rather than plain vals: a colour is no longer a
 * constant, it is a question about where you are.
 *
 * The consequence is that a colour cannot be read outside composition. Where
 * something genuinely needs one — the `Fit` and `TrustTier` enums, the fairness
 * zones — the value is a `@Composable` extension property instead; see
 * `TokenColors.kt`. Nothing else in the app read a colour outside a composable,
 * which is what made adding a second theme a three-file job rather than a
 * thirty-two-file one.
 */
val LocalPalette = staticCompositionLocalOf { LightPalette }

object GearColor {

    // Pine — structure
    val Pine900: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pine900
    val Pine800: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pine800
    val Pine700: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pine700
    val Pine600: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pine600
    val Pine500: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pine500

    // Chalk — neutrals
    val Chalk000: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk000
    val Chalk50: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk50
    val Chalk100: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk100
    val Chalk200: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk200
    val Chalk300: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk300
    val Chalk400: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk400
    val Chalk500: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk500
    val Chalk600: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk600
    val Chalk700: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chalk700

    // Hi-Vis — action + safety. The same five steps in both themes.
    val HiVis600: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hiVis600
    val HiVis500: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hiVis500
    val HiVis400: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hiVis400
    val HiVis200: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hiVis200
    val HiVis100: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hiVis100

    // Marine — informational
    val Marine700: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.marine700
    val Marine500: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.marine500
    val Marine300: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.marine300
    val Marine100: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.marine100

    /**
     * Semantic, in three steps. `100` fills, `500` accents, `700` is the ink
     * written on the `100`. Use `700` for text on a tint — a `500` accent is
     * chosen to read as a colour, not to read as words on top of itself.
     */
    val Success700: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.success700
    val Success500: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.success500
    val Success100: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.success100
    val Warning700: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warning700
    val Warning500: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warning500
    val Warning100: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warning100
    val Danger700: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.danger700
    val Danger600: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.danger600
    val Danger500: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.danger500
    val Danger100: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.danger100

    /**
     * Trust tiers. A metal scale of its own — deliberately outside the five
     * ramps, because a tier badge should not read as a status or a fit verdict.
     */
    val TierBronze: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tierBronze
    val TierSilver: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tierSilver
    val TierGold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tierGold

    // The metal as a word rather than as a ring. See the note in Palette.kt.
    val TierBronzeInk: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tierBronzeInk
    val TierSilverInk: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tierSilverInk
    val TierGoldInk: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tierGoldInk

    /**
     * Camera viewport shades. Darker than pine/900 so the hi-vis guides and
     * brackets read as an overlay on a live feed rather than as UI on a surface.
     */
    val Viewport: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.viewport
    val ViewportChip: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.viewportChip

    // Fit ramp
    val FitPerfect: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.fitPerfect
    val FitGood: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.fitGood
    val FitWorkable: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.fitWorkable
    val FitPoor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.fitPoor
    val FitBlocked: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.fitBlocked
}

/** Semantic aliases, so screens read in intent rather than in ramp steps. */
object GearSurface {
    val app: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceApp
    val card: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceCard
    val sunken: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceSunken
    val dark: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceDark
    val darkest: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceDarkest
    val border: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceBorder
    val borderStrong: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceBorderStrong

    /**
     * A light chip sitting on a photograph or a camera feed — the save heart, the
     * pager dots, the shutter ring, the quality badge on a photo slot.
     *
     * The same colour in both themes, because the picture underneath is the same
     * picture. Use [GearText.onOverImage] for anything written on it.
     */
    val overImage: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.overImage
}

object GearText {
    val primary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textPrimary
    val body: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textBody
    val secondary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textSecondary
    val tertiary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textTertiary
    val placeholder: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.textPlaceholder
    val onDark: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textOnDark
    val onDarkMuted: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.textOnDarkMuted

    /**
     * Text on hi-vis is always pine/900. Contrast 11.2:1. Never white — and
     * never theme-dependent, because hi-vis itself never moves.
     */
    val onHiVis: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textOnHiVis

    /** Ink on [GearSurface.overImage]. Constant, for the same reason. */
    val onOverImage: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.onOverImage

    /** The success/danger pair as drawn on [GearSurface.overImage]. */
    val successOnImage: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.successOnImage
    val dangerOnImage: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.dangerOnImage
}
