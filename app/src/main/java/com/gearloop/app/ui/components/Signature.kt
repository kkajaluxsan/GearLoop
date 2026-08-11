package com.gearloop.app.ui.components

import com.gearloop.app.ui.theme.color
import androidx.compose.runtime.ReadOnlyComposable
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.gearloop.app.ui.preview.PreviewFrame
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.GearPreview
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Fit
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The three signature components — design brief §4.2.
 *
 * Everything else on screen stays quiet so these land.
 */

enum class TagSize { COMPACT, FULL }
enum class TagTone { HI_VIS, MUTED, DARK }

/**
 * ⭐ The Gear Tag — the app's identity mark.
 *
 * Rental gear in the real world carries a tag zip-tied to it: item code, size,
 * condition grade, punched in monospace on hi-vis card stock. This is that tag.
 * It replaces the generic "specs" row every marketplace app has, and it says
 * *this is a real, individual, tracked object* rather than a catalogue SKU.
 */
@Composable
fun GearTag(
    text: String,
    modifier: Modifier = Modifier,
    size: TagSize = TagSize.COMPACT,
    tone: TagTone = TagTone.HI_VIS,
) {
    val height = if (size == TagSize.FULL) 30.dp else 26.dp
    val fill = when (tone) {
        TagTone.HI_VIS -> GearColor.HiVis500
        TagTone.MUTED -> GearColor.Chalk200
        TagTone.DARK -> GearColor.Pine800
    }
    val ink = when (tone) {
        TagTone.DARK -> GearColor.HiVis500
        TagTone.MUTED -> GearColor.Chalk600
        else -> GearText.onHiVis
    }

    Row(
        modifier
            .height(height)
            .clip(GearShape.xs)
            .background(fill)
            .padding(start = 8.dp, end = if (size == TagSize.FULL) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The punched hole. 5px, pine/900 at 40%, 8px in from the left edge.
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    if (tone == TagTone.DARK) GearColor.HiVis500.copy(alpha = 0.5f)
                    else GearColor.Pine900.copy(alpha = 0.4f)
                )
        )
        Box(
            Modifier
                .padding(horizontal = 7.dp)
                .width(1.dp)
                .height(height - 12.dp)
                .background(ink.copy(alpha = 0.22f))
        )
        // A tag is a physical object — it never wraps onto a second line.
        Text(
            text = text.uppercase(),
            style = GearType.dataS,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Read as a tag rather than as a run of glyphs: the interpunct
            // separators mean nothing out loud, and an ellipsised tag would
            // otherwise be announced truncated.
            modifier = Modifier.semantics {
                contentDescription = "Gear tag: " + text.split(" · ").joinToString(", ")
            },
        )
    }
}

/**
 * ⭐ The Fit Stamp — the personalised fit verdict.
 *
 * Drawn as a rubber stamp: 2px border in the fit colour, transparent fill,
 * knocked 3° off true. Fit is a safety property here, not a preference, so it
 * gets its own colour ramp rather than borrowing the status one.
 */
@Composable
fun FitStamp(
    fit: Fit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Box(
        modifier
            .rotate(-3f)
            .height(24.dp)
            .clip(GearShape.xs)
            .border(Border.heavy, fit.color, GearShape.xs)
            .padding(horizontal = 10.dp)
            // Fit is a safety property. The verdict is carried by colour, and a
            // custom label ("174 CM · 68 KG") replaces the wording entirely — so
            // the stamp says what it means rather than reading out its caption.
            .semantics {
                contentDescription = "Fit verdict: ${fit.label.lowercase()}"
                if (label != null) stateDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        // A stamp is one piece of rubber. Squeezed into a column narrower than
        // its verdict it used to wrap — "PERFECT FIT" came out as "PE / RE" with
        // the second line clipped by the fixed height — so it refuses to wrap
        // and pushes the layout instead. Same reasoning as the tag above.
        Text(
            text = label ?: fit.label,
            style = GearType.dataS,
            color = fit.color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

enum class FairnessZone(val word: String) {
    FAIR("FAIR"),
    WARN("UNBALANCED"),
    UNFAIR("UNFAIR");

    companion object {
        /** ≤10% is fair, ≤25% is a warning, beyond that it isn't a trade. */
        fun of(percent: Int): FairnessZone = when {
            abs(percent) <= 10 -> FAIR
            abs(percent) <= 25 -> WARN
            else -> UNFAIR
        }
    }
}

/** The beam and the bar — a graphic, so the accent step. */
val FairnessZone.color: Color
    @Composable @ReadOnlyComposable
    get() = when (this) {
        FairnessZone.FAIR -> GearColor.Success500
        FairnessZone.WARN -> GearColor.Warning500
        FairnessZone.UNFAIR -> GearColor.Danger500
    }

/**
 * The verdict written out — "18% UNBALANCED". Text, so it takes the 700 step;
 * the amber accent that reads well as a beam is too light to read as words.
 */
val FairnessZone.ink: Color
    @Composable @ReadOnlyComposable
    get() = when (this) {
        FairnessZone.FAIR -> GearColor.Success700
        FairnessZone.WARN -> GearColor.Warning700
        FairnessZone.UNFAIR -> GearColor.Danger700
    }

/**
 * ⭐ The Fairness Meter — the swap screen's balance beam.
 *
 * An actual beam on a pivot, not a progress bar. It physically tilts with the
 * value difference between two people's gear, and levels out as cash is added
 * to balance the trade. This is the one moment of motion in the app.
 *
 * @param percent how far out the trade is, signed. Positive = the other side is
 *   worth more, so the right pan drops.
 */
@Composable
fun FairnessMeter(
    percent: Int,
    leftValue: Int,
    rightValue: Int,
    modifier: Modifier = Modifier,
    leftLabel: String = "YOU GIVE",
    rightLabel: String = "YOU GET",
    deltaAmount: Int? = null,
    animate: Boolean = true,
    /**
     * The `LKR 19,000 · 31% UNFAIR` line under the beam. Off where the screen
     * already writes the verdict out in full — S-21 stated the same gap three
     * times in three colours, two of which disagreed about how bad it was.
     */
    showReadout: Boolean = true,
) {
    val zone = FairnessZone.of(percent)
    // ±12° at the extremes; 25% out is already at full tilt.
    val targetTilt = (percent.coerceIn(-40, 40) / 40f) * 12f
    val tilt by animateFloatAsState(
        targetValue = if (animate) targetTilt else targetTilt,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "beam-tilt",
    )
    val beamColor by animateColorAsState(zone.color, tween(400), label = "beam-colour")

    // A DrawScope is not a composable scope, so the canvas below cannot ask the
    // theme for a colour. Read them here, where it can.
    val hangerColor = GearColor.Chalk400
    val pivotColor = GearColor.Pine800
    val groundColor = GearColor.Chalk300

    // The whole verdict is in the tilt of a beam and the colour it turns —
    // nothing a screen reader can see. The meter is announced as one thing, in
    // the same words the design uses, rather than as a pile of stray labels.
    val spoken = buildString {
        append("Trade balance: ${zone.word.lowercase()}. ")
        append(leftLabel.lowercase().replaceFirstChar { it.uppercase() })
        append(" ${lkr(leftValue)}, ")
        append(rightLabel.lowercase())
        append(" ${lkr(rightValue)}.")
        if (deltaAmount != null && deltaAmount != 0) {
            append(" ${abs(percent)} percent out, ${lkr(abs(deltaAmount))}.")
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The frame has to be tall enough for the pan on the low side of a fully
        // tilted beam: pivot + drop + hanger + pan height.
        BoxWithConstraints(Modifier.fillMaxWidth().height(172.dp)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            // Keep the beam short enough that both hanging pans stay on screen —
            // a pan is 104 wide, so it needs 52 either side of the beam end.
            val panHalf = with(density) { 54.dp.toPx() }
            val beamHalf = with(density) { 140.dp.toPx() }.coerceAtMost(widthPx / 2 - panHalf)
            val pivotX = widthPx / 2f
            val pivotY = with(density) { 66.dp.toPx() }
            val rad = Math.toRadians(tilt.toDouble())
            val dx = (beamHalf * cos(rad)).toFloat()
            val dy = (beamHalf * sin(rad)).toFloat()
            val hangPx = with(density) { 18.dp.toPx() }

            Canvas(Modifier.fillMaxSize()) {
                val leftEnd = Offset(pivotX - dx, pivotY - dy)
                val rightEnd = Offset(pivotX + dx, pivotY + dy)

                // The beam, drawn thick with round caps so it reads as a bar.
                drawLine(
                    color = beamColor,
                    start = leftEnd,
                    end = rightEnd,
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Hanger lines down to each pan.
                listOf(leftEnd, rightEnd).forEach { end ->
                    drawLine(
                        color = hangerColor,
                        start = end,
                        end = Offset(end.x, end.y + hangPx),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }

                // Pivot triangle.
                val half = 13.dp.toPx()
                val tri = Path().apply {
                    moveTo(pivotX, pivotY - 2.dp.toPx())
                    lineTo(pivotX - half, pivotY + 30.dp.toPx())
                    lineTo(pivotX + half, pivotY + 30.dp.toPx())
                    close()
                }
                drawPath(tri, pivotColor)

                // Ground line under the pivot.
                drawLine(
                    color = groundColor,
                    start = Offset(pivotX - 34.dp.toPx(), pivotY + 31.dp.toPx()),
                    end = Offset(pivotX + 34.dp.toPx(), pivotY + 31.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // A faint level reference, so "balanced" is visible as a target.
                drawLine(
                    color = groundColor,
                    start = Offset(pivotX - beamHalf, pivotY),
                    end = Offset(pivotX + beamHalf, pivotY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
                )
            }

            // Pans. Positioned against the beam ends rather than drawn in the
            // canvas, so the values stay real text.
            Pan(
                label = leftLabel,
                value = leftValue,
                modifier = Modifier.offset {
                    IntOffset(
                        (pivotX - dx - with(density) { 52.dp.toPx() }).roundToInt(),
                        (pivotY - dy + hangPx).roundToInt(),
                    )
                },
            )
            Pan(
                label = rightLabel,
                value = rightValue,
                modifier = Modifier.offset {
                    IntOffset(
                        (pivotX + dx - with(density) { 52.dp.toPx() }).roundToInt(),
                        (pivotY + dy + hangPx).roundToInt(),
                    )
                },
            )
        }

        if (showReadout) {
            Spacer8()
            Text(
                text = buildString {
                    if (deltaAmount != null) append("${lkr(abs(deltaAmount))} · ")
                    append("${abs(percent)}% ${zone.word}")
                },
                style = GearType.dataM,
                color = zone.ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Pan(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(104.dp)
            .clip(GearShape.xs)
            .background(GearColor.Chalk50)
            .border(Border.hair, GearColor.Chalk200, GearShape.xs)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = GearType.labelXs, color = GearText.tertiary)
        Text(grouped(value), style = GearType.dataL, color = GearText.primary)
    }
}

@Composable
private fun Spacer8() {
    Box(Modifier.height(Space.s))
}

/**
 * The compact form of the meter used on swap cards — same colour logic, laid
 * out as a bar with a centre datum so a list of trades can be scanned quickly.
 */
@Composable
fun FairnessBar(
    percent: Int,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val zone = FairnessZone.of(percent)
    val fraction = (percent.coerceIn(-50, 50) / 100f)

    // Read outside the draw scope — a DrawScope cannot ask the theme anything.
    val trackColor = GearColor.Chalk200
    val fillColor = zone.color
    val datumColor = GearColor.Pine800

    Box(modifier.fillMaxWidth().height(height + 10.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(height + 10.dp)) {
            val midY = size.height / 2
            val midX = size.width / 2
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, midY - height.toPx() / 2),
                size = androidx.compose.ui.geometry.Size(size.width, height.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height.toPx()),
            )
            val span = size.width * fraction
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(if (span >= 0) midX else midX + span, midY - height.toPx() / 2),
                size = androidx.compose.ui.geometry.Size(abs(span), height.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height.toPx()),
            )
            // Centre datum — the balanced point.
            drawLine(
                color = datumColor,
                start = Offset(midX, midY - height.toPx() / 2 - 4.dp.toPx()),
                end = Offset(midX, midY + height.toPx() / 2 + 4.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Diagonal hatching — the buffer state on calendar cells. Worth calling out in
 * the viva: it is what makes turnaround time visible instead of implied.
 */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHatch(
    color: Color,
    spacingPx: Float,
    strokeWidthPx: Float,
) {
    val diagonal = size.width + size.height
    var x = -size.height
    while (x < diagonal) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x + size.height, size.height),
            strokeWidth = strokeWidthPx,
        )
        x += spacingPx
    }
}

/** Rotating helper kept for the stamp effect on dark surfaces. */
@Composable
fun StampFrame(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .rotate(-3f)
            .border(Border.heavy, color, GearShape.xs)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/* ------------------------------------------------------------- previews */

/**
 * The tag in all three tones and both sizes. It is the app's identity mark, so
 * this is the preview to check first after any change to the type scale.
 */
@GearTypePreview
@Composable
private fun GearTagPreview() = PreviewFrame {
    GearTag("6'6\" · GRADE B · TQ-4471", size = TagSize.FULL)
    GearTag("6'6\" · GRADE B")
    GearTag("6'6\" · GRADE B", tone = TagTone.MUTED)
    GearTag("6'6\" · GRADE B", tone = TagTone.DARK)
}

/** The full fit ramp. Fit is a safety property, so every verdict has to read. */
@GearTypePreview
@Composable
private fun FitStampRampPreview() = PreviewFrame {
    Fit.entries.forEach { FitStamp(it) }
    FitStamp(Fit.PERFECT, label = "174 CM · 68 KG")
}

/** The beam at rest, tilted, and past the point where it stops being a trade. */
@GearPreview
@Composable
private fun FairnessMeterFairPreview() = PreviewFrame {
    FairnessMeter(percent = 4, leftValue = 52_000, rightValue = 54_000, deltaAmount = 2_000)
}

@GearPreview
@Composable
private fun FairnessMeterUnbalancedPreview() = PreviewFrame {
    FairnessMeter(percent = 18, leftValue = 52_000, rightValue = 61_500, deltaAmount = 9_500)
}

@GearPreview
@Composable
private fun FairnessMeterUnfairPreview() = PreviewFrame {
    FairnessMeter(percent = -38, leftValue = 52_000, rightValue = 32_000, deltaAmount = -20_000)
}

@GearPreview
@Composable
private fun FairnessBarPreview() = PreviewFrame {
    FairnessBar(percent = 4)
    FairnessBar(percent = 18)
    FairnessBar(percent = -38)
}
