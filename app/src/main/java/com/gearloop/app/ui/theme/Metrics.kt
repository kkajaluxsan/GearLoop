package com.gearloop.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, radius and elevation (design brief §3.3).
 *
 * Radii are deliberately restrained — gear is engineered, not soft. Cards are
 * separated by a 1px border, not a shadow; shadow is reserved for things that
 * genuinely float above the content.
 */
/**
 * The layout scale — what sets the rhythm of a page.
 *
 * These are the values that decide how a screen breathes: gaps between rows,
 * padding inside a card, the margin down the side. Every one of them is used;
 * a token nobody reaches for is a claim the design system doesn't make, so it
 * doesn't live here.
 *
 * Values below 8 that tune the *interior* of a component — the 3 dp under a
 * card title, the 6 dp between an icon and its label — are written as literals
 * at the point of use. They are optical corrections, not layout, and naming
 * them would imply a rhythm that isn't there.
 */
object Space {
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val sm: Dp = 12.dp
    val m: Dp = 16.dp

    /** Screen side margin. */
    val screen: Dp = 20.dp
    val l: Dp = 24.dp

    /** Gap between major sections. */
    val section: Dp = 32.dp

    /** Padding inside a card. */
    val cardPadding: Dp = 16.dp

    /** Gap between rows in a list. */
    val listGap: Dp = 12.dp
}

/**
 * Border widths.
 *
 * A border is load-bearing in this design — it is what separates two cards,
 * where most apps would reach for a shadow. That makes its weight a design
 * decision rather than a number, so it gets tokens.
 */
object Border {
    /** The default: card edges, dividers, input outlines. */
    val hair: Dp = 1.dp

    /** Selected and focused states — visibly heavier without moving the layout. */
    val medium: Dp = 1.5.dp

    /** The fit stamp, and anything else drawn as a stamped mark. */
    val heavy: Dp = 2.dp
}

object Radius {
    /** Gear tag, small chips. */
    val xs: Dp = 4.dp

    /** Buttons, inputs, badges. */
    val sm: Dp = 8.dp

    /** Cards, images. */
    val md: Dp = 10.dp

    /** Sheets, modals. */
    val lg: Dp = 14.dp

    /** Avatars, filter pills, FAB. */
    val full: Dp = 999.dp
}

object GearShape {
    val xs: Shape = RoundedCornerShape(Radius.xs)
    val sm: Shape = RoundedCornerShape(Radius.sm)
    val md: Shape = RoundedCornerShape(Radius.md)
    val lg: Shape = RoundedCornerShape(Radius.lg)
    val full: Shape = RoundedCornerShape(Radius.full)

    /** Bottom sheets — top corners only. */
    val sheet: Shape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg)

    /** Card images that sit flush against the card's top edge. */
    val imageTop: Shape = RoundedCornerShape(topStart = Radius.md, topEnd = Radius.md)
}

/**
 * Elevation is sparse by design. `sm` for a knob, `md` for a floating card,
 * `lg` for a toast, `sticky` for the footer bar that sits over scrolling content.
 */
object Elevation {
    val sm: Dp = 2.dp
    val md: Dp = 12.dp
    val lg: Dp = 32.dp
    val sticky: Dp = 16.dp
}

/**
 * Fixed geometry the brief calls out by number.
 *
 * System bar heights are deliberately absent: they vary by device, and the app
 * reads the real ones through `statusBarsPadding()` / `navigationBarsPadding()`
 * rather than guessing at a constant.
 */
object Metrics {
    val appBar: Dp = 52.dp
    val tabBar: Dp = 49.dp

    /** Minimum tap target. Nothing interactive may be smaller. */
    val tapTarget: Dp = 44.dp
}
