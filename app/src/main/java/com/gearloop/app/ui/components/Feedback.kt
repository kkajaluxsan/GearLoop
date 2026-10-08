package com.gearloop.app.ui.components

import androidx.compose.runtime.ReadOnlyComposable
import com.gearloop.app.ui.theme.Border
import com.gearloop.app.ui.preview.PreviewFrame
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.GearPreview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.TimelineNode
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.icons.LucideIcon
import com.gearloop.app.ui.theme.Elevation
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space

/** Feedback and status — design brief §4.5. */

enum class BannerKind { INFO, SUCCESS, WARNING, DANGER, SAFETY }

private data class BannerSkin(val fill: Color, val spine: Color, val ink: Color, val icon: LucideIcon)

@Composable
@ReadOnlyComposable
private fun skin(kind: BannerKind) = when (kind) {
    // fill · spine · ink. The ink is always the 700 step: an accent picked to
    // read as a colour is not dark enough to read as text on its own tint.
    BannerKind.INFO -> BannerSkin(GearColor.Marine100, GearColor.Marine500, GearColor.Marine700, Lucide.Info)
    BannerKind.SUCCESS -> BannerSkin(GearColor.Success100, GearColor.Success500, GearColor.Success700, Lucide.CircleCheck)
    BannerKind.WARNING -> BannerSkin(GearColor.Warning100, GearColor.Warning500, GearColor.Warning700, Lucide.TriangleAlert)
    BannerKind.DANGER -> BannerSkin(GearColor.Danger100, GearColor.Danger500, GearColor.Danger700, Lucide.CircleAlert)
    // Safety uses hi-vis, the same way a life jacket does.
    BannerKind.SAFETY -> BannerSkin(GearColor.HiVis100, GearColor.HiVis600, GearColor.Pine900, Lucide.ShieldCheck)
}

@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    kind: BannerKind = BannerKind.INFO,
    title: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val s = skin(kind)
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(GearShape.sm)
            .background(s.fill),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(s.spine))
        Row(Modifier.padding(Space.m), verticalAlignment = Alignment.Top) {
            GearIcon(s.icon, null, size = IconSize.m, tint = s.spine)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (title != null) {
                    Text(title, style = GearType.headingS, color = s.ink)
                    Spacer(Modifier.height(3.dp))
                }
                Text(text, style = GearType.bodyS, color = s.ink)
                if (action != null && onAction != null) {
                    Spacer(Modifier.height(8.dp))
                    GearLink(action, onAction, color = s.spine, icon = Lucide.ArrowRight)
                }
            }
        }
    }
}

@Composable
fun Toast(
    text: String,
    modifier: Modifier = Modifier,
    undoLabel: String? = null,
    onUndo: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(Elevation.lg, GearShape.sm, spotColor = GearColor.Pine900)
            .clip(GearShape.sm)
            .background(GearColor.Pine800)
            .padding(horizontal = Space.m, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = GearType.bodyM, color = GearText.onDark, modifier = Modifier.weight(1f))
        if (undoLabel != null && onUndo != null) {
            Spacer(Modifier.width(Space.sm))
            GearLink(undoLabel, onUndo, color = GearColor.HiVis500, style = GearType.labelL)
        }
    }
}

/** Booking state history — vertical rail with a node per event. */
@Composable
fun Timeline(
    nodes: List<TimelineNode>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        nodes.forEachIndexed { i, node ->
            Row(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                    TimelineNodeMark(node.state)
                    if (i != nodes.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(if (node.state == TimelineNode.State.CURRENT) 46.dp else 40.dp)
                                .background(GearColor.Chalk200)
                        )
                    }
                }
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f).padding(bottom = if (i == nodes.lastIndex) 0.dp else Space.m)) {
                    Text(node.time.uppercase(), style = GearType.dataS, color = GearText.tertiary)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        node.title,
                        style = GearType.labelM,
                        color = if (node.state == TimelineNode.State.FUTURE) GearText.tertiary else GearText.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineNodeMark(state: TimelineNode.State) {
    val color = when (state) {
        TimelineNode.State.DONE -> GearColor.Success500
        TimelineNode.State.CURRENT -> GearColor.HiVis500
        TimelineNode.State.FUTURE -> GearColor.Chalk300
    }
    Box(Modifier.size(12.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        if (state == TimelineNode.State.CURRENT) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(GearColor.Pine900))
        }
    }
}

/**
 * Empty state.
 *
 * Every empty state in this app names a specific next action. No shrugs.
 */
@Composable
fun EmptyState(
    icon: LucideIcon,
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Space.section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GearIcon(icon, null, size = IconSize.xxl, tint = GearColor.Chalk300, strokeWidth = 1.5f)
        Spacer(Modifier.height(Space.m))
        Text(headline, style = GearType.headingM, color = GearText.primary)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = GearType.bodyM,
            color = GearText.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.m))
            GearButton(actionLabel, onAction, type = ButtonType.PRIMARY, size = ButtonSize.M)
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(Space.s))
            GearButton(secondaryLabel, onSecondary, type = ButtonType.GHOST, size = ButtonSize.M)
        }
    }
}

/** Loading placeholder blocks. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    widthFraction: Float = 1f,
) {
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(GearShape.sm)
            .background(GearColor.Chalk200)
    )
}

@Composable
fun SkeletonListRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        Box(Modifier.size(96.dp).clip(GearShape.sm).background(GearColor.Chalk200))
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(height = 18.dp, widthFraction = 0.85f)
            SkeletonBlock(height = 14.dp, widthFraction = 0.5f)
            SkeletonBlock(height = 14.dp, widthFraction = 0.35f)
        }
    }
}

/** A labelled row of icon + text, used for included items and checklists. */
@Composable
fun IconRow(
    icon: LucideIcon,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = GearColor.Pine800,
    textColor: Color = GearText.body,
    strikethrough: Boolean = false,
    style: androidx.compose.ui.text.TextStyle = GearType.bodyM,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        GearIcon(icon, null, size = IconSize.m, tint = tint)
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = if (strikethrough) {
                style.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            } else style,
            color = textColor,
        )
    }
}

/** Big mono countdown — used on handover, active rental and the swap cycle. */
@Composable
fun Countdown(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = GearText.primary,
    labelColor: Color = GearText.tertiary,
) {
    Column(modifier) {
        Text(value, style = GearType.dataXl, color = valueColor)
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), style = GearType.labelXs, color = labelColor)
    }
}

/** Legend entry for calendars and comparison views. */
@Composable
fun LegendItem(
    label: String,
    modifier: Modifier = Modifier,
    swatch: @Composable () -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatch()
        Text(label, style = GearType.bodyS, color = GearText.secondary)
    }
}

/**
 * A legend swatch.
 *
 * [underline] exists because a legend that doesn't draw what the grid draws is
 * worse than no legend: "selected" and "booked" are both pine/800 cells and are
 * told apart only by the hi-vis bar under a booked one, so the swatch has to
 * carry the bar too. Without it the calendar keys read as one colour twice.
 */
@Composable
fun SwatchBox(
    color: Color,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    underline: Color? = null,
) {
    Box(
        modifier
            .size(16.dp)
            .clip(GearShape.xs)
            .then(if (bordered) Modifier.border(Border.hair, GearSurface.border, GearShape.xs) else Modifier)
            .background(color),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (underline != null) {
            Box(
                Modifier
                    .padding(bottom = 3.dp)
                    .size(width = 8.dp, height = 2.dp)
                    .background(underline)
            )
        }
    }
}

/* ------------------------------------------------------------- previews */

/**
 * All five banner kinds together, because the point of the set is that safety
 * reads differently from information — which is only checkable side by side.
 */
@GearTypePreview
@Composable
private fun BannerKindsPreview() = PreviewFrame {
    Banner("Exact address is shared once the booking is confirmed.", kind = BannerKind.INFO)
    Banner("Your deposit has been released.", kind = BannerKind.SUCCESS)
    Banner("This board is booked from 12 Aug.", kind = BannerKind.WARNING)
    Banner("The owner reported damage on return.", kind = BannerKind.DANGER)
    Banner(
        "Wrong-sized gear isn't just uncomfortable. Helmets and harnesses have to fit.",
        kind = BannerKind.SAFETY,
    )
    Banner(
        "3 people want to swap for your Burton Custom",
        title = "New matches",
        kind = BannerKind.INFO,
        action = "View matches",
        onAction = {},
    )
}

@GearPreview
@Composable
private fun ToastAndCountdownPreview() = PreviewFrame {
    Toast("Listing removed", undoLabel = "Undo", onUndo = {})
    Countdown("02:14:06", "UNTIL RETURN")
}

@GearPreview
@Composable
private fun TimelinePreview() = PreviewFrame {
    Timeline(
        listOf(
            TimelineNode("09:12", "Booking confirmed", TimelineNode.State.DONE),
            TimelineNode("14:30", "Gear handed over", TimelineNode.State.DONE),
            TimelineNode("Now", "Rental active", TimelineNode.State.CURRENT),
            TimelineNode("10 Aug", "Return and settle", TimelineNode.State.FUTURE),
        )
    )
}

@GearTypePreview
@Composable
private fun EmptyStatePreview() = PreviewFrame {
    EmptyState(
        icon = Lucide.Search,
        headline = "Nothing matches all of that",
        body = "Loosen one filter and there are 12 boards within 5 km.",
        actionLabel = "Widen to 10 km",
        onAction = {},
        secondaryLabel = "Clear filters",
        onSecondary = {},
    )
}

@GearPreview
@Composable
private fun SkeletonPreview() = PreviewFrame {
    SkeletonListRow()
    SkeletonListRow()
    SkeletonBlock(Modifier.fillMaxWidth().height(96.dp))
}

@GearPreview
@Composable
private fun IconRowPreview() = PreviewFrame {
    IconRow(Lucide.Check, "Board", tint = GearColor.Success500)
    IconRow(Lucide.Check, "Fins ×3", tint = GearColor.Success500)
    IconRow(Lucide.X, "Wax (not included)", tint = GearColor.Chalk400, textColor = GearText.tertiary)
}

/**
 * A small form that belongs to the screen behind it: writing a review, adding
 * gear to the closet, typing the handover code. A dialog rather than a new
 * route, because the screen behind it is the context — the review is of the
 * rental on screen, the code is for the booking on screen.
 *
 * Elevated, because it floats; the card itself is [GearDialogCard], which is
 * what the preview draws.
 */
@Composable
fun GearDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissLabel: String = "Cancel",
    destructiveLabel: String? = null,
    onDestructive: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GearDialogCard(
            title, onDismiss, confirmLabel, onConfirm, confirmEnabled, dismissLabel,
            destructiveLabel, onDestructive, content,
        )
    }
}

@Composable
fun GearDialogCard(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissLabel: String = "Cancel",
    destructiveLabel: String? = null,
    onDestructive: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(Elevation.lg, GearShape.lg, spotColor = GearColor.Pine900)
            .clip(GearShape.lg)
            .background(GearSurface.card)
            .padding(Space.l),
    ) {
        Text(title, style = GearType.headingL, color = GearText.primary)
        Spacer(Modifier.height(Space.m))
        content()
        Spacer(Modifier.height(Space.l))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (destructiveLabel != null && onDestructive != null) {
                GearButton(destructiveLabel, onDestructive, type = ButtonType.DANGER, size = ButtonSize.M)
            }
            Spacer(Modifier.weight(1f))
            GearButton(dismissLabel, onDismiss, type = ButtonType.GHOST, size = ButtonSize.M)
            GearButton(confirmLabel, onConfirm, size = ButtonSize.M, enabled = confirmEnabled)
        }
    }
}

@GearTypePreview
@Composable
private fun DialogCardPreview() = PreviewFrame {
    GearDialogCard(
        title = "Rate Kavitha",
        onDismiss = {},
        confirmLabel = "Post review",
        onConfirm = {},
        destructiveLabel = "Delete",
        onDestructive = {},
    ) {
        StarPicker(4, {})
        Spacer(Modifier.height(Space.s))
        GearTextField("Board was exactly as described.", label = "Your review", onValueChange = {}, singleLine = false)
    }
}

/**
 * The calendar key, both calendars use.
 *
 * Worth a preview of its own because the failure it guards against is only
 * visible with the entries side by side: "selected" and "booked" are the same
 * pine fill, and the swatch has to reproduce the hi-vis bar that tells a booked
 * cell apart or the legend says one thing twice.
 */
@GearPreview
@Composable
private fun CalendarLegendPreview() = PreviewFrame {
    LegendItem("Available") { SwatchBox(GearSurface.card, bordered = true) }
    LegendItem("Selected") { SwatchBox(GearColor.Pine800) }
    LegendItem("In range") { SwatchBox(GearColor.HiVis200) }
    LegendItem("Booked") { SwatchBox(GearColor.Pine800, underline = GearColor.HiVis500) }
    LegendItem("Blocked") { SwatchBox(GearColor.Chalk200) }
}
