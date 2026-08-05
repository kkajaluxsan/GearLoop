package com.gearloop.app.ui.components

import com.gearloop.app.ui.theme.GearSurface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gearloop.app.ui.theme.GearColor
import kotlin.math.sin

/**
 * A map drawn to the GearLoop palette.
 *
 * Every mapping SDK ships the same blue-and-beige default, and leaving it in a
 * design portfolio reads as "I didn't style this". Land is chalk, water is
 * marine, roads are chalk/300 — so the map sits inside the design system rather
 * than beside it.
 *
 * The geometry is procedural rather than a real tile set. This is a UI
 * prototype: there is no map SDK, no key and no network call.
 */

/**
 * The paint the map needs, gathered before drawing starts.
 *
 * A `DrawScope` is not a composable scope, so it cannot ask the theme for a
 * colour. Collecting them up front also makes the map's palette legible in one
 * place — which is the point of restyling it rather than shipping a default.
 */
private data class MapColors(
    val land: Color,
    val block: Color,
    val water: Color,
    val waterEdge: Color,
    val road: Color,
    val radius: Color,
    val route: Color,
    val pinFrom: Color,
    val pinDot: Color,
)

@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    showRoute: Boolean = false,
    showRadius: Boolean = true,
    seed: Int = 7,
) {
    val colors = MapColors(
        land = GearColor.Chalk100,
        block = GearColor.Chalk50,
        water = GearColor.Marine100,
        waterEdge = GearColor.Marine300.copy(alpha = 0.55f),
        road = GearColor.Chalk300,
        radius = GearColor.Pine600,
        route = GearColor.Pine800,
        pinFrom = GearColor.Marine500,
        pinDot = GearSurface.overImage,
    )
    Canvas(modifier) {
        drawStyledMap(colors, showRoute = showRoute, showRadius = showRadius, seed = seed)
    }
}

private fun DrawScope.drawStyledMap(
    c: MapColors,
    showRoute: Boolean,
    showRadius: Boolean,
    seed: Int,
) {
    val w = size.width
    val h = size.height

    drawRect(c.land)

    // Coastline — Jaffna sits on a lagoon, so water enters from one corner.
    val coast = Path().apply {
        moveTo(w, h * 0.30f)
        cubicTo(w * 0.78f, h * 0.36f, w * 0.72f, h * 0.58f, w * 0.80f, h)
        lineTo(w, h)
        close()
    }
    drawPath(coast, c.water)
    drawPath(coast, c.waterEdge, style = Stroke(width = 1.5.dp.toPx()))

    // Blocks — a loose grid, offset per row so it doesn't read as graph paper.
    val block = 46.dp.toPx()
    var row = 0
    var y = -block * 0.4f
    while (y < h) {
        var x = -block * (0.3f + 0.35f * sin((row + seed).toFloat()))
        while (x < w) {
            val bw = block * (0.62f + 0.22f * sin((x + row * 3 + seed).toDouble()).toFloat())
            val bh = block * 0.56f
            if (x + bw < w * 0.74f || y > h * 0.62f) {
                drawRoundRect(
                    color = c.block,
                    topLeft = Offset(x, y),
                    size = Size(bw.coerceAtLeast(12f), bh),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            x += block
        }
        y += block * 0.86f
        row++
    }

    // Roads.
    val roadStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    listOf(0.22f, 0.52f, 0.80f).forEach { f ->
        drawLine(c.road, Offset(0f, h * f), Offset(w * 0.82f, h * (f + 0.06f)), roadStroke.width, StrokeCap.Round)
    }
    listOf(0.26f, 0.58f).forEach { f ->
        drawLine(c.road, Offset(w * f, 0f), Offset(w * (f + 0.05f), h), roadStroke.width, StrokeCap.Round)
    }
    // One arterial, heavier.
    drawLine(c.road, Offset(w * 0.05f, h), Offset(w * 0.68f, 0f), 5.dp.toPx(), StrokeCap.Round)

    val centre = Offset(w * 0.42f, h * 0.52f)

    if (showRadius) {
        // An approximate radius, not a pin. The exact address is private until
        // the booking is confirmed, and the map has to say that visually.
        val r = minOf(w, h) * 0.28f
        drawCircle(c.radius.copy(alpha = 0.10f), r, centre)
        drawCircle(
            color = c.radius.copy(alpha = 0.55f),
            radius = r,
            center = centre,
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)),
            ),
        )
    }

    if (showRoute) {
        val from = Offset(w * 0.18f, h * 0.78f)
        val to = Offset(w * 0.66f, h * 0.28f)
        val route = Path().apply {
            moveTo(from.x, from.y)
            lineTo(from.x, h * 0.52f)
            lineTo(w * 0.44f, h * 0.52f)
            lineTo(w * 0.44f, to.y)
            lineTo(to.x, to.y)
        }
        drawPath(route, c.route, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
        drawMapPin(from, c.pinFrom, c.pinDot)
        drawMapPin(to, c.route, c.pinDot)
    }
}

/** Teardrop pin, drawn rather than imported so it inherits the palette. */
private fun DrawScope.drawMapPin(at: Offset, color: Color, dot: Color) {
    val r = 9.dp.toPx()
    val pin = Path().apply {
        moveTo(at.x, at.y)
        lineTo(at.x - r * 0.72f, at.y - r * 1.15f)
        lineTo(at.x + r * 0.72f, at.y - r * 1.15f)
        close()
    }
    drawPath(pin, color)
    drawCircle(color, r, Offset(at.x, at.y - r * 1.5f))
    drawCircle(dot, r * 0.38f, Offset(at.x, at.y - r * 1.5f))
}
