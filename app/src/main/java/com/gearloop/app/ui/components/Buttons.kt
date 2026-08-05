package com.gearloop.app.ui.components

import androidx.compose.foundation.layout.heightIn
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import com.gearloop.app.ui.preview.PreviewFrame
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.GearPreview
import com.gearloop.app.ui.icons.Lucide
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.LucideIcon
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Metrics
import com.gearloop.app.ui.theme.Radius
import com.gearloop.app.ui.theme.Space

/** Actions — design brief §4.1. */

enum class ButtonType { PRIMARY, SECONDARY, GHOST, DANGER }

enum class ButtonSize(val height: Dp, val padX: Dp) {
    L(52.dp, 24.dp),
    M(44.dp, 20.dp),
    S(36.dp, 14.dp),
}

private data class ButtonSkin(
    val fill: Color,
    val content: Color,
    val border: Color? = null,
    val borderWidth: Dp = Border.medium,
)

@Composable
private fun skinFor(type: ButtonType, pressed: Boolean, enabled: Boolean): ButtonSkin = when {
    !enabled -> ButtonSkin(GearColor.Chalk200, GearColor.Chalk400)
    type == ButtonType.PRIMARY -> ButtonSkin(
        fill = if (pressed) GearColor.HiVis600 else GearColor.HiVis500,
        // Text on hi-vis is always pine/900. Never white.
        content = GearText.onHiVis,
    )
    type == ButtonType.SECONDARY -> ButtonSkin(
        fill = if (pressed) GearColor.Chalk50 else GearColor.Chalk000,
        content = GearColor.Pine800,
        border = GearColor.Pine800,
    )
    type == ButtonType.GHOST -> ButtonSkin(
        fill = if (pressed) GearColor.Chalk200 else Color.Transparent,
        content = GearColor.Pine800,
    )
    else -> ButtonSkin(
        fill = if (pressed) GearColor.Danger600 else GearColor.Danger500,
        content = GearText.onDark,
    )
}

@Composable
fun GearButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.PRIMARY,
    size: ButtonSize = ButtonSize.L,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: LucideIcon? = null,
    trailingIcon: LucideIcon? = null,
    /** Overrides the content colour — used for ghost buttons on dark screens. */
    contentColor: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val skin = skinFor(type, pressed, enabled).let {
        if (contentColor != null && enabled) it.copy(content = contentColor) else it
    }
    val shape: Shape = GearShape.sm
    val padX = if (type == ButtonType.GHOST) 12.dp else size.padX

    Row(
        modifier
            .height(size.height)
            .clip(shape)
            .then(
                if (skin.border != null && enabled) Modifier.border(skin.borderWidth, skin.border, shape)
                else Modifier
            )
            .background(skin.fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            // While loading the label still reads "Confirming", but a screen
            // reader has no way to know the button has stopped accepting taps.
            .semantics { if (loading) stateDescription = "Busy" }
            .padding(horizontal = padX),
        horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            Spinner(color = skin.content, size = 18.dp)
        } else if (icon != null) {
            GearIcon(icon, null, size = IconSize.m, tint = skin.content)
        }
        Text(
            text = label,
            style = if (size == ButtonSize.S) GearType.labelM else GearType.labelL,
            color = skin.content,
        )
        if (trailingIcon != null && !loading) {
            GearIcon(trailingIcon, null, size = IconSize.m, tint = skin.content)
        }
    }
}

/** Indeterminate spinner drawn as an arc — no Material progress ring. */
@Composable
fun Spinner(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, strokeWidth: Dp = 2.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Box(
        modifier
            .size(size)
            .rotate(angle)
            .drawBehind {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                    size = Size(this.size.width - strokeWidth.toPx(), this.size.height - strokeWidth.toPx()),
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2),
                )
            }
    )
}

enum class IconButtonTone { ON_LIGHT, ON_IMAGE, DARK, HI_VIS }

@Composable
fun GearIconButton(
    icon: LucideIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: IconButtonTone = IconButtonTone.ON_LIGHT,
    size: Dp = Metrics.tapTarget,
    iconSize: Dp = IconSize.m,
    tint: Color? = null,
    filled: Boolean = false,
    badge: String? = null,
) {
    val fill = when (tone) {
        IconButtonTone.ON_LIGHT -> GearSurface.card
        // Sits over a photograph, so it needs its own contrast.
        IconButtonTone.ON_IMAGE -> GearSurface.overImage.copy(alpha = 0.92f)
        IconButtonTone.DARK -> GearColor.Pine800
        IconButtonTone.HI_VIS -> GearColor.HiVis500
    }
    val content = tint ?: when (tone) {
        IconButtonTone.DARK -> GearText.onDark
        IconButtonTone.HI_VIS -> GearText.onHiVis
        else -> GearColor.Pine800
    }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (tone == IconButtonTone.ON_LIGHT) Modifier.border(Border.hair, GearSurface.border, CircleShape)
                    else Modifier
                )
                .background(fill)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            GearIcon(icon, contentDescription, size = iconSize, tint = content, filled = filled)
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(GearColor.Danger500)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, style = GearType.dataS, color = GearText.onDark, modifier = Modifier.padding(horizontal = 5.dp))
            }
        }
    }
}

/** A dot badge — no count, just "there is something here". */
@Composable
fun BadgeDot(modifier: Modifier = Modifier, color: Color = GearColor.Danger500, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: LucideIcon? = null,
    count: Int? = null,
    trailingIcon: LucideIcon? = null,
    enabled: Boolean = true,
) {
    val fill = when {
        !enabled -> GearColor.Chalk100
        selected -> GearColor.Pine800
        else -> GearSurface.card
    }
    val content = when {
        !enabled -> GearColor.Chalk400
        selected -> GearText.onDark
        else -> GearColor.Pine800
    }
    // The chip is painted at 34 but has to be tappable at 44, so the target is
    // the box and the pill is what gets drawn inside it. Growing the pill itself
    // would make it a button; leaving the target at 34 would break the rule that
    // nothing interactive is smaller than a fingertip.
    Box(
        modifier
            .heightIn(min = Metrics.tapTarget)
            .then(
                if (onClick != null && enabled) {
                    Modifier.selectable(selected = selected, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
    Row(
        Modifier
            .height(34.dp)
            .clip(GearShape.full)
            .then(if (!selected) Modifier.border(Border.hair, GearSurface.border, GearShape.full) else Modifier)
            .background(fill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) GearIcon(icon, null, size = IconSize.s, tint = content)
        Text(label, style = GearType.labelM, color = content)
        if (count != null) {
            Text(
                text = count.toString(),
                style = GearType.dataS,
                color = if (selected) GearColor.HiVis500 else GearText.tertiary,
            )
        }
        if (trailingIcon != null) GearIcon(trailingIcon, null, size = IconSize.s, tint = content)
    }
    }
}

/** Filter pill that carries a live count badge, used on the search sub-bar. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(GearColor.HiVis500),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), style = GearType.dataS, color = GearText.onHiVis)
    }
}

/** Floating action button — radius/full, elevated because it floats. */
@Composable
fun GearFab(
    icon: LucideIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Row(
        modifier
            .height(52.dp)
            .clip(GearShape.full)
            .background(GearColor.Pine800)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (label == null) 16.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        GearIcon(icon, contentDescription, size = IconSize.l, tint = GearText.onDark)
        if (label != null) Text(label, style = GearType.labelL, color = GearText.onDark)
    }
}

/** Text link — marine by default, because links are informational. */
@Composable
fun GearLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GearColor.Marine500,
    icon: LucideIcon? = null,
    style: androidx.compose.ui.text.TextStyle = GearType.labelM,
) {
    Row(
        modifier
            .clip(GearShape.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = style, color = color)
        if (icon != null) GearIcon(icon, null, size = IconSize.s, tint = color)
    }
}

/** Radius token exposed for the few places that need a raw value. */
val chipRadius = Radius.full

/* ------------------------------------------------------------- previews */

@GearTypePreview
@Composable
private fun ButtonTypesPreview() = PreviewFrame {
    GearButton("Book instantly", {}, type = ButtonType.PRIMARY)
    GearButton("Ask a question", {}, type = ButtonType.SECONDARY)
    GearButton("See all reviews", {}, type = ButtonType.GHOST)
    GearButton("Report damage", {}, type = ButtonType.DANGER)
}

@GearPreview
@Composable
private fun ButtonStatesPreview() = PreviewFrame {
    GearButton("Continue", {}, size = ButtonSize.L)
    GearButton("Continue", {}, size = ButtonSize.M)
    GearButton("Continue", {}, size = ButtonSize.S)
    GearButton("Disabled until you scroll", {}, enabled = false)
    GearButton("Confirming", {}, loading = true)
    GearButton("With an icon", {}, icon = Lucide.Search)
}

@GearPreview
@Composable
private fun IconButtonTonesPreview() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        GearIconButton(Lucide.Heart, "Save", {}, tone = IconButtonTone.ON_LIGHT)
        GearIconButton(Lucide.Share2, "Share", {}, tone = IconButtonTone.DARK)
        GearIconButton(Lucide.Check, "Accept", {}, tone = IconButtonTone.HI_VIS)
        GearIconButton(Lucide.Bell, "Alerts", {}, badge = "2")
    }
}

@GearTypePreview
@Composable
private fun ChipsPreview() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        Chip("Available today", onClick = {})
        Chip("Instant book", selected = true, onClick = {}, icon = Lucide.Zap)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        Chip("Grade A", onClick = {}, count = 12)
        Chip("Fits me", selected = true, onClick = {}, trailingIcon = Lucide.X)
        Chip("Booked out", enabled = false)
    }
}

@GearPreview
@Composable
private fun FabAndLinkPreview() = PreviewFrame {
    GearFab(Lucide.Plus, "List an item", {})
    GearFab(Lucide.Plus, "List an item", {}, label = "List an item")
    GearLink("See exactly what's covered", {}, icon = Lucide.ExternalLink)
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        CountBadge(3)
        BadgeDot()
        Spinner(GearColor.Pine800)
    }
}
