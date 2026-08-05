package com.gearloop.app.ui.components

import com.gearloop.app.ui.theme.LocalPalette
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import com.gearloop.app.ui.preview.PreviewFrame
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.GearPreview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.gearloop.app.ui.theme.Metrics
import com.gearloop.app.ui.theme.Space

/** Navigation chrome — design brief §4.5. */

enum class AppBarType { DEFAULT, TRANSPARENT, MODAL }

@Composable
fun AppBar(
    title: String? = null,
    modifier: Modifier = Modifier,
    type: AppBarType = AppBarType.DEFAULT,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: (@Composable RowScopeActions.() -> Unit)? = null,
) {
    val onImage = type == AppBarType.TRANSPARENT
    val background = when (type) {
        AppBarType.DEFAULT -> GearColor.Chalk100
        AppBarType.MODAL -> GearSurface.card
        AppBarType.TRANSPARENT -> Color.Transparent
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding()
            .height(Metrics.appBar)
            .padding(horizontal = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            GearIconButton(
                icon = if (type == AppBarType.MODAL) Lucide.X else Lucide.ChevronLeft,
                contentDescription = if (type == AppBarType.MODAL) "Close" else "Back",
                onClick = onBack,
                tone = if (onImage) IconButtonTone.ON_IMAGE else IconButtonTone.ON_LIGHT,
                size = 40.dp,
                iconSize = IconSize.l,
            )
        } else {
            Spacer(Modifier.width(Space.s))
        }
        Column(
            Modifier.weight(1f).padding(horizontal = Space.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (title != null) {
                Text(
                    title,
                    style = GearType.headingM,
                    color = GearText.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(subtitle, style = GearType.dataS, color = GearText.tertiary)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions?.invoke(RowScopeActions)
            if (actions == null && onBack != null) Spacer(Modifier.width(40.dp))
        }
    }
}

/** Marker scope so app-bar actions read as a distinct slot at call sites. */
object RowScopeActions

/**
 * The sticky footer that carries the commitment on every step of a flow.
 * Elevated because it floats over scrolling content — one of the few places the
 * design system permits a shadow.
 */
@Composable
fun StickyFooter(
    modifier: Modifier = Modifier,
    priceValue: String? = null,
    priceCaption: String? = null,
    content: @Composable RowScopeFooter.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(Elevation.sticky, spotColor = GearColor.Pine900, ambientColor = GearColor.Pine900)
            .background(GearSurface.card),
    ) {
        GearDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen, vertical = Space.sm)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            if (priceValue != null) {
                Column(Modifier.weight(1f)) {
                    Text(priceValue, style = GearType.dataXl, color = GearText.primary)
                    if (priceCaption != null) {
                        Text(priceCaption, style = GearType.bodyS, color = GearText.tertiary)
                    }
                }
            }
            RowScopeFooter.content()
        }
    }
}

object RowScopeFooter

data class TabItem(
    val label: String,
    val icon: LucideIcon,
    val badge: Boolean = false,
)

/**
 * Tab bar.
 *
 * The brief asks for a filled icon in the active state. Lucide ships no filled
 * variants, so weight carries the state instead — the active icon is drawn at a
 * heavier stroke in pine/800 while inactive sits at 1.75 in chalk/500. Same
 * signal, one icon set, no mixing.
 */
@Composable
fun TabBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(GearSurface.card)
    ) {
        GearDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .height(Metrics.tabBar)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == selectedIndex
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // The active state is carried by stroke weight and colour,
                        // neither of which reaches a screen reader. `selected` is
                        // what actually announces "tab 2 of 5, selected".
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            onClick = { onSelect(i) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        // Decorative: the label sits directly underneath, and
                        // describing both makes the reader say it twice.
                        GearIcon(
                            icon = tab.icon,
                            contentDescription = null,
                            size = IconSize.l,
                            tint = if (active) GearColor.Pine800 else GearColor.Chalk500,
                            strokeWidth = if (active) 2.5f else 1.75f,
                        )
                        if (tab.badge) {
                            BadgeDot(
                                Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 1.dp),
                                size = 7.dp,
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label.uppercase(),
                        style = GearType.labelXs,
                        color = if (active) GearColor.Pine800 else GearColor.Chalk500,
                    )
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** `STEP 3 OF 6` plus the segment track. */
@Composable
fun ProgressStepper(
    step: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(GearShape.full)
                        .background(
                            when {
                                i < step - 1 -> GearColor.HiVis500
                                i == step - 1 -> GearColor.Pine800
                                else -> GearColor.Chalk300
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(Space.s))
        Eyebrow("Step $step of $total")
    }
}

/**
 * The standard screen shell: background, optional app bar, scrolling body, and
 * an optional sticky footer or tab bar pinned to the bottom.
 */
@Composable
fun GearScreen(
    modifier: Modifier = Modifier,
    background: Color = GearSurface.app,
    appBar: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    tabBar: (@Composable () -> Unit)? = null,
    scrollable: Boolean = true,
    contentPadding: Dp = Space.screen,
    content: @Composable ColumnScope.() -> Unit,
) {
    SystemBarIcons(darkSurface = false)
    // Hoisted rather than called inside the `then` branch: a remember that only
    // runs on one side of a condition is a slot-table hazard.
    val scroll = rememberSavedScrollState()
    Column(modifier.fillMaxSize().background(background)) {
        appBar?.invoke()
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)
                .then(if (contentPadding > 0.dp) Modifier.padding(horizontal = contentPadding) else Modifier),
            content = content,
        )
        footer?.invoke()
        tabBar?.invoke()
        if (footer == null && tabBar == null) {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/**
 * The screen shell for the long lists.
 *
 * Same chrome as [GearScreen], but the body only composes the rows that are on
 * screen. That matters here because a row carries a photograph: the eager shell
 * decodes every picture in the list the moment the screen opens, and a search
 * result page is eight of them. Screens that are a fixed set of sections should
 * stay on [GearScreen] — this is for the ones that scroll through content.
 */
@Composable
fun GearLazyScreen(
    modifier: Modifier = Modifier,
    background: Color = GearSurface.app,
    appBar: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    tabBar: (@Composable () -> Unit)? = null,
    contentPadding: Dp = Space.screen,
    content: LazyListScope.() -> Unit,
) {
    SystemBarIcons(darkSurface = false)
    Column(modifier.fillMaxSize().background(background)) {
        appBar?.invoke()
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = contentPadding),
            content = content,
        )
        footer?.invoke()
        tabBar?.invoke()
        if (footer == null && tabBar == null) {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Screen shell for the dark screens — the handover scan and capture.
 *
 * Deliberately not scrollable: these are camera screens, so the viewport takes
 * the space left over between a fixed header and fixed controls.
 */
@Composable
fun DarkScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SystemBarIcons(darkSurface = true)
    Column(
        modifier
            .fillMaxSize()
            .background(GearSurface.darkest)
            .statusBarsPadding()
            .navigationBarsPadding(),
        content = content,
    )
}

/** Circular progress ring — trust score, owner review window. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = GearColor.Chalk200,
    color: Color = GearColor.Pine800,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            val inset = strokeWidth.toPx() / 2
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - strokeWidth.toPx(),
                this.size.height - strokeWidth.toPx(),
            )
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = arcSize,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
                size = arcSize,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            )
        }
        content()
    }
}

/**
 * Keeps the system status-bar icons legible.
 *
 * The app is a light design with three deliberately dark screens, so the icon
 * appearance has to follow the surface underneath rather than being set once at
 * launch. Every screen shell declares which it is.
 */
@Composable
fun SystemBarIcons(darkSurface: Boolean) {
    val view = LocalView.current
    // In the dark theme every surface is dark, so a screen that only declares
    // itself light would still get light icons and lose them against the bar.
    val onDark = darkSurface || LocalPalette.current.isDark
    if (view.isInEditMode) return
    SideEffect {
        // The view's context is not guaranteed to be the Activity — it can be a
        // ContextWrapper — so walk the chain rather than casting and hoping.
        val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !onDark
            isAppearanceLightNavigationBars = !onDark
        }
    }
}

/** Grabber for bottom sheets — 36×4 in chalk/300. */
@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(vertical = 10.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(GearColor.Chalk300)
        )
    }
}

/* ------------------------------------------------------------- previews */

@GearTypePreview
@Composable
private fun AppBarPreview() = PreviewFrame(padded = false) {
    AppBar(title = "Surfboards", onBack = {})
    AppBar(title = "Torq Mod Fish", subtitle = "TQ-4471", onBack = {})
    AppBar(title = "Filters", onBack = {}, type = AppBarType.MODAL)
}

/**
 * Active state is carried by stroke weight, since Lucide ships no filled
 * variants. This is the preview that proves the two weights are distinguishable.
 */
@GearPreview
@Composable
private fun TabBarPreview() = PreviewFrame(padded = false) {
    val tabs = listOf(
        TabItem("Explore", Lucide.Compass),
        TabItem("Swap", Lucide.ArrowRightLeft, badge = true),
        TabItem("Rentals", Lucide.Package, badge = true),
        TabItem("Messages", Lucide.MessageSquare),
        TabItem("Profile", Lucide.User),
    )
    TabBar(tabs, selectedIndex = 0, onSelect = {})
    TabBar(tabs, selectedIndex = 2, onSelect = {})
}

@GearTypePreview
@Composable
private fun StickyFooterPreview() = PreviewFrame(padded = false) {
    StickyFooter(priceValue = "LKR 9,240", priceCaption = "3 days, all in") {
        GearButton("Book instantly", {})
    }
    StickyFooter {
        GearButton("I accept and continue", {}, Modifier.fillMaxWidth(), enabled = false)
    }
}

@GearPreview
@Composable
private fun ProgressStepperPreview() = PreviewFrame {
    ProgressStepper(step = 1, total = 5)
    ProgressStepper(step = 3, total = 5)
    ProgressStepper(step = 5, total = 5)
}

@GearPreview
@Composable
private fun ProgressRingPreview() = PreviewFrame {
    ProgressRing(progress = 0.82f, size = 120.dp) {
        Text("82", style = GearType.dataXl, color = GearText.primary)
    }
}
