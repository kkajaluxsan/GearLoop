package com.gearloop.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.gearloop.app.data.Fit
import com.gearloop.app.data.TrustTier

/**
 * Colours for the domain types.
 *
 * `Fit` and `TrustTier` live in `data/` and describe the product, not the paint.
 * They used to carry a `Color` in their constructor, which quietly assumed there
 * would only ever be one theme. Now the verdict and the tier stay in the data
 * layer and the colour is asked for at the point of drawing.
 *
 * The call sites are unchanged — `fit.color` and `person.tier.accent` still read
 * the same, they just need composition around them now.
 */

/** The fit ramp. Fit is a safety property, so it gets its own scale. */
val Fit.color: Color
    @Composable @ReadOnlyComposable
    get() = when (this) {
        Fit.PERFECT -> GearColor.FitPerfect
        Fit.GOOD -> GearColor.FitGood
        Fit.WORKABLE -> GearColor.FitWorkable
        Fit.POOR -> GearColor.FitPoor
        Fit.BLOCKED -> GearColor.FitBlocked
        Fit.UNKNOWN -> GearColor.Chalk500
    }

/** The trust metals — deliberately outside the five ramps. */
val TrustTier.accent: Color
    @Composable @ReadOnlyComposable
    get() = when (this) {
        TrustTier.BRONZE -> GearColor.TierBronze
        TrustTier.SILVER -> GearColor.TierSilver
        TrustTier.GOLD -> GearColor.TierGold
    }
