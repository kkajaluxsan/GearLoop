package com.gearloop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material's colour slots, mapped onto the GearLoop palette.
 *
 * Almost nothing in the app draws from these — every component reaches for a
 * GearLoop token directly. They exist so a stray Material component can never
 * introduce purple, and so the platform's own surfaces (text selection handles,
 * the ripple, autofill) land somewhere deliberate.
 */
private fun schemeFor(p: GearPalette) = if (p.isDark) {
    darkColorScheme(
        primary = p.hiVis500,
        onPrimary = p.textOnHiVis,
        primaryContainer = p.hiVis200,
        onPrimaryContainer = p.textPrimary,

        secondary = p.pine500,
        onSecondary = p.chalk000,
        secondaryContainer = p.pine700,
        onSecondaryContainer = p.textPrimary,

        tertiary = p.marine500,
        onTertiary = p.chalk000,
        tertiaryContainer = p.marine100,
        onTertiaryContainer = p.marine700,

        background = p.surfaceApp,
        onBackground = p.textPrimary,
        surface = p.surfaceCard,
        onSurface = p.textPrimary,
        surfaceVariant = p.surfaceSunken,
        onSurfaceVariant = p.textSecondary,

        outline = p.surfaceBorder,
        outlineVariant = p.surfaceBorderStrong,

        error = p.danger500,
        onError = p.chalk000,
        errorContainer = p.danger100,
        onErrorContainer = p.danger500,

        scrim = p.viewport,
    )
} else {
    lightColorScheme(
        primary = p.hiVis500,
        onPrimary = p.textOnHiVis,
        primaryContainer = p.hiVis200,
        onPrimaryContainer = p.pine900,

        secondary = p.pine800,
        onSecondary = p.chalk000,
        secondaryContainer = p.pine700,
        onSecondaryContainer = p.chalk000,

        tertiary = p.marine500,
        onTertiary = p.chalk000,
        tertiaryContainer = p.marine100,
        onTertiaryContainer = p.marine700,

        background = p.surfaceApp,
        onBackground = p.textPrimary,
        surface = p.surfaceCard,
        onSurface = p.textPrimary,
        surfaceVariant = p.surfaceSunken,
        onSurfaceVariant = p.textSecondary,

        outline = p.surfaceBorder,
        outlineVariant = p.surfaceBorderStrong,

        error = p.danger500,
        onError = p.chalk000,
        errorContainer = p.danger100,
        onErrorContainer = p.danger500,

        scrim = p.pine900,
    )
}

/**
 * Material's type slots are mapped onto the GearLoop scale so any stray Material
 * component inherits the right family instead of falling back to Roboto.
 */
private val GearTypography = Typography(
    displayLarge = GearType.displayXl,
    displayMedium = GearType.displayL,
    displaySmall = GearType.headingXl,
    headlineLarge = GearType.headingXl,
    headlineMedium = GearType.headingL,
    headlineSmall = GearType.headingM,
    titleLarge = GearType.headingL,
    titleMedium = GearType.headingM,
    titleSmall = GearType.headingS,
    bodyLarge = GearType.bodyL,
    bodyMedium = GearType.bodyM,
    bodySmall = GearType.bodyS,
    labelLarge = GearType.labelL,
    labelMedium = GearType.labelM,
    labelSmall = GearType.labelXs,
)

/**
 * Whether the app is drawing itself as a wireframe.
 *
 * [WirePalette] takes the colour out on its own, but a photograph is not a colour
 * — it goes on being a photograph however grey the page around it gets. This is
 * the signal `GearImage` reads to draw a slot instead. Nothing else needs it:
 * every other surface in the app is already a token.
 */
val LocalWireframe = staticCompositionLocalOf { false }

/**
 * GearLoop ships both themes.
 *
 * The two palettes are instances of the same shape, so a component asks for
 * `GearSurface.card` and gets whichever card colour is correct where it is
 * standing. The one thing that does not follow the theme is hi-vis: it marks
 * action and safety, and a life jacket is the same colour at night.
 *
 * [wireframe] swaps in a third instance of that same shape and renders every
 * screen as the structural drawing it came from — no screen, and no component,
 * knows it happened. It overrides [darkTheme]: a wireframe is a drawing on
 * paper, and paper has one colour.
 *
 * [blockText] greeks the copy: every string is drawn as a bar from cap height to
 * baseline instead of as words. It is the other half of the wireframe convention,
 * and it is kept as a separate flag because the two answer different questions —
 * one takes the colour out, the other takes the reading out. See [LocalBlockText].
 */
@Composable
fun GearLoopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    wireframe: Boolean = false,
    blockText: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = when {
        wireframe -> WirePalette
        darkTheme -> DarkPalette
        else -> LightPalette
    }

    // Shapes are applied per component from the Radius tokens rather than through
    // Material's shape slots, so nothing is left to inherit a default corner.
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalWireframe provides wireframe,
        LocalBlockText provides blockText,
    ) {
        MaterialTheme(
            colorScheme = schemeFor(palette),
            typography = GearTypography,
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides GearType.bodyM,
                LocalContentColor provides GearText.body,
                content = content,
            )
        }
    }
}
