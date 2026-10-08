package com.gearloop.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.gearloop.app.ui.preview.GearPreview
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.PreviewFrame

/**
 * Whether the app is greeking its own copy — every string drawn as a bar rather
 * than as words.
 *
 * A wireframe convention: at this fidelity the reviewer is being asked about
 * structure and hierarchy, and real sentences invite them to read instead of
 * look. [WirePalette] takes the colour out; this takes the letters out.
 *
 * Orthogonal to [LocalWireframe] by design, so either can be used alone — but
 * `--ez blocks true` is normally passed alongside `--ez wire true`.
 */
val LocalBlockText = staticCompositionLocalOf { false }

/**
 * The app's `Text`.
 *
 * Every screen and component imports this in place of `androidx.compose.material3.Text`,
 * which is the whole mechanism: 458 call sites are intercepted by 27 import lines,
 * and not one of them knows it happened. The same argument as [WirePalette] —
 * a presentation variant should not be reachable only by editing screens.
 *
 * ## Why this is not done in the type layer
 *
 * The obvious implementation is a transparent `color` with a background on the
 * [TextStyle] in [GearType]. It does not work: 421 call sites pass `color =`
 * explicitly, and an explicit colour argument wins over `style.color` inside
 * Compose's `Text`. Intercepting the call itself is the only place that sees
 * both the style and the colour the design actually asked for.
 *
 * ## Why the bar takes the ink colour
 *
 * The bar is drawn in the colour the words would have been. That is what keeps
 * it legible everywhere without a token of its own: a bar on the camera viewport
 * comes out light because [GearText.onDark] is light, a bar on a hi-vis button
 * comes out pine because [GearText.onHiVis] is pine, and quiet text greeks to a
 * quieter bar because tertiary is quieter than primary. The palette's hierarchy
 * survives having the letters removed from it.
 *
 * The band runs from cap height to baseline — descenders are dropped, which is
 * what makes a row of bars read as a line of type rather than as a filled box.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    if (!LocalBlockText.current) {
        MaterialText(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
        return
    }

    // The colour the words would have been, resolved the same way Compose resolves
    // it: the argument, then the style, then the ambient content colour.
    val ambient = LocalContentColor.current
    val ink = when {
        color != Color.Unspecified -> color
        style.color != Color.Unspecified -> style.color
        else -> ambient
    }

    // Cap height in pixels, so the bar is a band of type rather than the whole
    // line box. Falls back to a fraction of the measured line when neither the
    // argument nor the style names a size.
    val requested = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize
    val density = LocalDensity.current
    val fontPx = if (requested != TextUnit.Unspecified) {
        with(density) { requested.toPx() }
    } else {
        0f
    }

    // A TextLayoutResult cannot be saved, so `remember` is correct here — the same
    // exemption MutableInteractionSource has. Nothing about a screen's state lives
    // in it; it is thrown away and re-measured on every composition that matters.
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    MaterialText(
        text = text,
        modifier = modifier.drawBehind {
            val measured = layout ?: return@drawBehind
            for (line in 0 until measured.lineCount) {
                val left = measured.getLineLeft(line)
                val width = measured.getLineRight(line) - left
                if (width <= 0f) continue

                val baseline = measured.getLineBaseline(line)
                val cap = if (fontPx > 0f) {
                    fontPx * CAP_RATIO
                } else {
                    (measured.getLineBottom(line) - measured.getLineTop(line)) * FALLBACK_RATIO
                }
                drawRect(ink, Offset(left, baseline - cap), Size(width, cap))
            }
        },
        // The glyphs still lay out — that is what keeps every position identical to
        // the finished screen — they simply are not painted.
        color = Color.Transparent,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = {
            layout = it
            onTextLayout?.invoke(it)
        },
        style = style,
    )
}

/** Cap height as a fraction of font size, across the three families the app ships. */
private const val CAP_RATIO = 0.72f

/** Used only when no size is resolvable, measured against the whole line box. */
private const val FALLBACK_RATIO = 0.62f

// ---- Previews ---------------------------------------------------------------

/**
 * The same block of type twice — as words, then greeked.
 *
 * This is the pair worth looking at before capturing a wireframe set: every bar
 * has to sit exactly where its words sat, or the wireframe is describing a
 * layout the finished screen does not have.
 */
@Composable
private fun TypeSpecimen() {
    Text("Torq Mod Fish", style = GearType.headingL, color = GearText.primary)
    Text("LKR 2,800 / day", style = GearType.dataL, color = GearText.primary)
    Text(
        "A wireframe asks about structure, so the copy is drawn as bars. " +
            "Two lines, to show that wrapping is measured rather than guessed.",
        style = GearType.bodyM,
        color = GearText.body,
    )
    Text("2.4 km away", style = GearType.bodyS, color = GearText.tertiary)
    Text("PERFECT FIT", style = GearType.dataS, color = GearText.primary)
}

@GearPreview
@Composable
private fun BlockTextOffPreview() = PreviewFrame { TypeSpecimen() }

@GearPreview
@Composable
private fun BlockTextOnPreview() = PreviewFrame(blockText = true) { TypeSpecimen() }

/** How the set is actually captured: no colour and no letters. */
@GearTypePreview
@Composable
private fun BlockTextWireframePreview() =
    PreviewFrame(wireframe = true, blockText = true) { TypeSpecimen() }
