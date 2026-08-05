package com.gearloop.app.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gearloop.app.R

/**
 * Typography (design brief §3.2).
 *
 * Three families, three jobs:
 *  - Archivo         display and headings. Grotesque with athletic weight.
 *  - Instrument Sans body and UI.
 *  - JetBrains Mono  anything measurable — sizes, grades, codes, prices, countdowns.
 *
 * The rule that keeps the app coherent: if a user could act on the number,
 * it is set in mono.
 */
val Archivo = FontFamily(
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
)

val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
    Font(R.font.instrument_sans_bold, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/**
 * Android adds font padding above and below every line by default, which throws
 * out the measured line heights in the spec. Trimming both edges makes the
 * rendered result match the design.
 */
private val trimmed = PlatformTextStyle(includeFontPadding = false)
private val evenLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    trackingPercent: Float = 0f,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (trackingPercent / 100f).em,
    platformStyle = trimmed,
    lineHeightStyle = evenLines,
)

object GearType {
    // Display — Archivo, tight tracking
    val displayXl = style(Archivo, FontWeight.Bold, 40, 42, -3f)
    val displayL = style(Archivo, FontWeight.Bold, 32, 36, -2.5f)

    // Headings
    val headingXl = style(Archivo, FontWeight.Bold, 26, 30, -2f)
    val headingL = style(Archivo, FontWeight.Bold, 22, 27, -1.5f)
    val headingM = style(Archivo, FontWeight.SemiBold, 18, 23, -1f)
    val headingS = style(InstrumentSans, FontWeight.SemiBold, 16, 21, -0.5f)

    // Body
    val bodyL = style(InstrumentSans, FontWeight.Normal, 17, 26)
    val bodyM = style(InstrumentSans, FontWeight.Normal, 15, 22)
    val bodyS = style(InstrumentSans, FontWeight.Normal, 13, 19)

    // Labels
    val labelL = style(InstrumentSans, FontWeight.Medium, 15, 20)
    val labelM = style(InstrumentSans, FontWeight.SemiBold, 13, 16, 1f)

    /** Always rendered UPPERCASE — used for eyebrows and micro-labels. */
    val labelXs = style(InstrumentSans, FontWeight.Bold, 11, 14, 6f)

    // Data — mono, for everything measurable
    val dataXl = style(JetBrainsMono, FontWeight.Bold, 28, 32, -2f)
    val dataL = style(JetBrainsMono, FontWeight.SemiBold, 20, 24, -1f)
    val dataM = style(JetBrainsMono, FontWeight.Medium, 14, 18)

    /** Always rendered UPPERCASE — gear tags, codes, grades. */
    val dataS = style(JetBrainsMono, FontWeight.Bold, 11, 14, 4f)
}
