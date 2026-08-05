package com.gearloop.app.nav

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The theme switch, reachable from the cover.
 *
 * The app follows the system by default, which is the right behaviour. But a
 * demonstration should never depend on someone finding the device settings
 * mid-sentence, so the cover carries an override.
 */
@Immutable
data class ThemeToggle(
    val isDark: Boolean = false,
    val toggle: () -> Unit = {},
)

val LocalThemeToggle = staticCompositionLocalOf { ThemeToggle() }
