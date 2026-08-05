package com.gearloop.app.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a Lucide icon at a given stroke weight.
 *
 * The brief pins the icon set at 1.75px stroke on a 24 grid. Because the source
 * geometry is kept as path data, the same definition can be re-rendered heavier
 * for the active tab-bar state instead of pulling in a second, filled icon set.
 */
private val vectorCache = HashMap<String, ImageVector>()

private fun buildVector(icon: LucideIcon, strokeWidth: Float, filled: Boolean): ImageVector {
    val key = "${icon.id}:$strokeWidth:$filled"
    vectorCache[key]?.let { return it }

    val builder = ImageVector.Builder(
        name = icon.id,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    icon.paths.forEach { d ->
        val nodes = PathParser().parsePathString(d).toNodes()
        if (filled) {
            builder.addPath(pathData = nodes, fill = SolidColor(Color.Black))
        } else {
            builder.addPath(
                pathData = nodes,
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }
    return builder.build().also { vectorCache[key] = it }
}

/** The only icon sizes the design system allows. */
object IconSize {
    val s: Dp = 16.dp
    val m: Dp = 20.dp
    val l: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
}

@Composable
fun GearIcon(
    icon: LucideIcon,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = IconSize.m,
    tint: Color = LocalContentColor.current,
    strokeWidth: Float = 1.75f,
    filled: Boolean = false,
) {
    Image(
        painter = rememberVectorPainter(buildVector(icon, strokeWidth, filled)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
