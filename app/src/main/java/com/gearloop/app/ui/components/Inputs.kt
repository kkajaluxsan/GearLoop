package com.gearloop.app.ui.components

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import com.gearloop.app.ui.preview.PreviewFrame
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.GearPreview
import com.gearloop.app.data.lkr
import com.gearloop.app.data.Demo
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.DayState
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
import kotlin.math.roundToInt

/** Inputs — design brief §4.4. */

enum class FieldState { DEFAULT, FOCUS, FILLED, ERROR, DISABLED }

@Composable
fun GearTextField(
    value: String,
    label: String? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    state: FieldState = FieldState.DEFAULT,
    error: String? = null,
    prefix: String? = null,
    suffix: String? = null,
    leadingIcon: LucideIcon? = null,
    trailingIcon: LucideIcon? = null,
    onClick: (() -> Unit)? = null,
) {
    val borderColor = when {
        error != null || state == FieldState.ERROR -> GearColor.Danger500
        state == FieldState.FOCUS -> GearColor.Pine800
        state == FieldState.DISABLED -> GearColor.Chalk300
        else -> GearSurface.border
    }
    val fill = if (state == FieldState.DISABLED) GearColor.Chalk100 else GearSurface.card
    val textColor = when {
        state == FieldState.DISABLED -> GearColor.Chalk400
        value.isEmpty() -> GearText.placeholder
        else -> GearText.primary
    }

    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = GearType.labelM, color = GearText.secondary)
            Spacer(Modifier.height(6.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(GearShape.sm)
                .border(Border.medium, borderColor, GearShape.sm)
                .background(fill)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) GearIcon(leadingIcon, null, size = IconSize.m, tint = GearColor.Chalk500)
            if (prefix != null) Text(prefix, style = GearType.dataM, color = GearText.tertiary)
            Text(
                text = value.ifEmpty { placeholder },
                style = GearType.bodyM,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
            if (suffix != null) Text(suffix, style = GearType.dataM, color = GearText.tertiary)
            if (trailingIcon != null) GearIcon(trailingIcon, null, size = IconSize.m, tint = GearColor.Chalk500)
            // A caret, so a focused field reads as focused in a static mock.
            if (state == FieldState.FOCUS) {
                Box(Modifier.width(1.5.dp).height(20.dp).background(GearColor.Pine800))
            }
        }
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GearIcon(Lucide.CircleAlert, null, size = IconSize.s, tint = GearColor.Danger500)
                Text(error, style = GearType.bodyS, color = GearColor.Danger500)
            }
        }
    }
}

/**
 * The search bar, in two modes.
 *
 * Pass [onValueChange] and it becomes a real field that types and filters. Leave
 * it out and it stays a tappable bar that navigates — which is what the home
 * screen wants, since tapping search there opens the results screen rather than
 * searching in place.
 *
 * `BasicTextField` rather than Material's `TextField`: the latter brings its own
 * container, indicator line and label animation, none of which belong to this
 * design system.
 */
@Composable
fun SearchBar(
    placeholder: String,
    modifier: Modifier = Modifier,
    value: String = "",
    onClick: () -> Unit = {},
    onValueChange: ((String) -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    filterCount: Int = 0,
) {
    val editable = onValueChange != null
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(GearShape.full)
            .border(Border.hair, GearSurface.border, GearShape.full)
            .background(GearSurface.card)
            .then(if (editable) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(start = Space.m, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearIcon(Lucide.Search, "Search", size = IconSize.m, tint = GearColor.Chalk500)
        Spacer(Modifier.width(10.dp))
        if (editable) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = GearType.bodyM.copy(color = GearText.primary),
                    cursorBrush = SolidColor(GearColor.Pine800),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) {
                    Text(placeholder, style = GearType.bodyM, color = GearText.placeholder)
                }
            }
            if (value.isNotEmpty()) {
                GearIconButton(
                    icon = Lucide.X,
                    contentDescription = "Clear search",
                    onClick = { onValueChange("") },
                    tone = IconButtonTone.ON_LIGHT,
                    size = 36.dp,
                    iconSize = IconSize.s,
                )
            }
        } else {
            Text(
                text = value.ifEmpty { placeholder },
                style = if (value.isEmpty()) GearType.bodyM else GearType.dataM,
                color = if (value.isEmpty()) GearText.placeholder else GearText.primary,
                modifier = Modifier.weight(1f),
            )
        }
        if (onFilter != null) {
            Box(contentAlignment = Alignment.TopEnd) {
                GearIconButton(
                    icon = Lucide.SlidersHorizontal,
                    contentDescription = "Filters",
                    onClick = onFilter,
                    tone = IconButtonTone.ON_LIGHT,
                    size = 40.dp,
                )
                if (filterCount > 0) CountBadge(filterCount, Modifier.offset(x = 2.dp, y = (-2).dp))
            }
        }
    }
}

@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepperButton(Lucide.Minus, "Decrease", onMinus, minusEnabled)
        Text(
            value,
            style = GearType.dataM,
            color = GearText.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
        StepperButton(Lucide.Plus, "Increase", onPlus, plusEnabled)
    }
}

@Composable
private fun StepperButton(icon: LucideIcon, description: String, onClick: () -> Unit, enabled: Boolean) {
    // 44 rather than the 36 in the spec table — the accessibility checklist in
    // the same brief requires every tap target to clear 44.
    Box(
        Modifier
            .size(Metrics.tapTarget)
            .clip(GearShape.sm)
            .border(Border.medium, if (enabled) GearSurface.border else GearColor.Chalk200, GearShape.sm)
            .background(GearSurface.card)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(icon, description, size = IconSize.m, tint = if (enabled) GearColor.Pine800 else GearColor.Chalk300)
    }
}

@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val knobOffset = if (checked) 23.dp else 3.dp
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(GearShape.full)
            .background(
                when {
                    !enabled -> GearColor.Chalk200
                    checked -> GearColor.Pine800
                    else -> GearColor.Chalk300
                }
            )
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .align(Alignment.CenterStart)
                .size(26.dp)
                .clip(CircleShape)
                .background(GearColor.Chalk000)
        )
    }
}

/** Toggle with its label and helper text, as used throughout the filter sheet. */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.primary)
            if (helper != null) {
                Spacer(Modifier.height(2.dp))
                Text(helper, style = GearType.bodyS, color = GearText.tertiary)
            }
        }
        Spacer(Modifier.width(Space.sm))
        Toggle(checked, onCheckedChange)
    }
}

@Composable
fun GearCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(Metrics.tapTarget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(GearShape.xs)
                .border(Border.medium, if (checked) GearColor.Pine800 else GearColor.Chalk300, GearShape.xs)
                .background(if (checked) GearColor.Pine800 else GearSurface.card),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) GearIcon(Lucide.Check, null, size = IconSize.s, tint = GearColor.HiVis500, strokeWidth = 2.5f)
        }
    }
}

@Composable
fun CheckRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The whole row is the target, so the box inside it is only a picture of
        // one — left addressable it would be a second, identical stop.
        Box(Modifier.clearAndSetSemantics { }) {
            GearCheckbox(checked, onCheckedChange)
        }
        Spacer(Modifier.width(4.dp))
        Text(text, style = GearType.bodyM, color = GearText.body, modifier = Modifier.weight(1f))
    }
}

@Composable
fun GearRadio(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(if (selected) 6.dp else 1.5.dp, if (selected) GearColor.Pine800 else GearColor.Chalk300, CircleShape)
            .background(GearSurface.card)
    )
}

/**
 * Slider with a value bubble.
 *
 * Drawn from scratch rather than using the Material one so the knob can carry
 * the 1.5px pine border the design system specifies, and so the price slider
 * can render a histogram of live listings behind its track.
 */
@Composable
fun GearSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueLabel: String? = null,
    histogram: List<Float>? = null,
    trackHeight: Dp = 4.dp,
) {
    val density = LocalDensity.current
    val knob = 28.dp
    BoxWithConstraints(modifier.fillMaxWidth().height(if (histogram != null) 96.dp else 56.dp)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val knobPx = with(density) { knob.toPx() }
        val travel = widthPx - knobPx
        var raw by remember { mutableFloatStateOf(value) }
        val fraction = value.coerceIn(0f, 1f)

        fun report(xPx: Float) {
            raw = ((xPx - knobPx / 2f) / travel).coerceIn(0f, 1f)
            onValueChange(raw)
        }

        if (histogram != null) {
            // A DrawScope can't read the theme, so the two bar colours are
            // resolved here where it still can.
            val barIn = GearColor.Chalk300
            val barOut = GearColor.Chalk200
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.TopStart)
            ) {
                val slot = size.width / histogram.size
                histogram.forEachIndexed { i, h ->
                    val barH = size.height * h
                    drawRoundRect(
                        color = if ((i + 0.5f) / histogram.size <= fraction) barIn else barOut,
                        topLeft = Offset(i * slot + 1.5f, size.height - barH),
                        size = Size(slot - 3f, barH),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(knob)
                .align(if (histogram != null) Alignment.BottomStart else Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectTapGestures { report(it.x) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> report(change.position.x) }
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(GearShape.full)
                    .background(GearColor.Chalk200)
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(trackHeight)
                    .clip(GearShape.full)
                    .background(GearColor.Pine800)
            )
            Box(
                Modifier
                    .offset { IntOffset((travel * fraction).roundToInt(), 0) }
                    .size(knob)
                    .shadow(Elevation.sm, CircleShape)
                    .clip(CircleShape)
                    .border(Border.medium, GearColor.Pine800, CircleShape)
                    .background(GearColor.Chalk000)
            )
        }

        if (valueLabel != null) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (travel * fraction + knobPx / 2f - with(density) { 46.dp.toPx() }).roundToInt()
                                .coerceIn(0, (widthPx - with(density) { 92.dp.toPx() }).roundToInt()),
                            0,
                        )
                    }
                    .width(92.dp)
                    .clip(GearShape.xs)
                    .background(GearColor.Pine800)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(valueLabel, style = GearType.dataM, color = GearText.onDark)
            }
        }
    }
}

/** Two-handle range slider — price filtering. */
@Composable
fun GearRangeSlider(
    lower: Float,
    upper: Float,
    onRangeChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    histogram: List<Float>? = null,
) {
    val density = LocalDensity.current
    val knob = 28.dp
    BoxWithConstraints(modifier.fillMaxWidth().height(if (histogram != null) 92.dp else 44.dp)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val knobPx = with(density) { knob.toPx() }
        val travel = widthPx - knobPx

        if (histogram != null) {
            val barIn = GearColor.Chalk400
            val barOut = GearColor.Chalk200
            Canvas(Modifier.fillMaxWidth().height(48.dp).align(Alignment.TopStart)) {
                val slot = size.width / histogram.size
                histogram.forEachIndexed { i, h ->
                    val centre = (i + 0.5f) / histogram.size
                    val inRange = centre in lower..upper
                    drawRoundRect(
                        color = if (inRange) barIn else barOut,
                        topLeft = Offset(i * slot + 1.5f, size.height - size.height * h),
                        size = Size(slot - 3f, size.height * h),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(knob)
                .pointerInput(lower, upper) {
                    detectDragGestures { change, _ ->
                        val f = ((change.position.x - knobPx / 2f) / travel).coerceIn(0f, 1f)
                        // Move whichever handle is nearer the finger.
                        if (kotlin.math.abs(f - lower) <= kotlin.math.abs(f - upper)) {
                            onRangeChange(f.coerceAtMost(upper - 0.05f), upper)
                        } else {
                            onRangeChange(lower, f.coerceAtLeast(lower + 0.05f))
                        }
                    }
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(GearShape.full)
                    .background(GearColor.Chalk200)
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((travel * lower + knobPx / 2).roundToInt(), 0) }
                    .width(with(density) { (travel * (upper - lower)).toDp() })
                    .height(4.dp)
                    .background(GearColor.Pine800)
            )
            listOf(lower, upper).forEach { f ->
                Box(
                    Modifier
                        .offset { IntOffset((travel * f).roundToInt(), 0) }
                        .size(knob)
                        .shadow(Elevation.sm, CircleShape)
                        .clip(CircleShape)
                        .border(Border.medium, GearColor.Pine800, CircleShape)
                        .background(GearColor.Chalk000)
                )
            }
        }
    }
}

/**
 * One calendar cell.
 *
 * The state worth pointing at in the viva is BUFFER — diagonal hatching that
 * makes turnaround time visible. The owner needs two hours to rinse and check
 * the board, and the calendar says so rather than silently blocking the day.
 */
@Composable
fun DateRangeCell(
    day: String,
    state: DayState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    size: Dp = 44.dp,
) {
    val fill = when (state) {
        DayState.SELECTED -> GearColor.Pine800
        DayState.IN_RANGE -> GearColor.HiVis200
        DayState.BOOKED -> GearColor.Pine800
        DayState.BLOCKED -> GearColor.Chalk200
        else -> Color.Transparent
    }
    val ink = when (state) {
        DayState.SELECTED, DayState.BOOKED -> GearText.onDark
        DayState.DISABLED -> GearColor.Chalk300
        DayState.BLOCKED -> GearColor.Chalk400
        DayState.BUFFER -> GearColor.Chalk600
        else -> GearText.primary
    }
    // The in-range fill runs edge to edge so a selected week reads as one band.
    val shape = if (state == DayState.IN_RANGE) RectangleShape else GearShape.sm

    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(fill)
            .then(
                if (state == DayState.TODAY) Modifier.border(Border.medium, GearColor.Pine800, shape) else Modifier
            )
            .then(if (onClick != null && state != DayState.DISABLED) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (state == DayState.BUFFER) {
            val hatchColor = GearColor.Chalk300
            Canvas(Modifier.fillMaxSize()) {
                drawHatch(hatchColor, spacingPx = 6.dp.toPx(), strokeWidthPx = 2.dp.toPx())
            }
        }
        Text(
            text = day,
            style = GearType.dataM,
            color = ink,
            textAlign = TextAlign.Center,
        )
        if (state == DayState.BOOKED) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(width = 12.dp, height = 2.dp)
                    .background(GearColor.HiVis500)
            )
        }
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(Metrics.tapTarget)
            .clip(GearShape.sm)
            .background(GearColor.Chalk100)
            .padding(3.dp),
    ) {
        options.forEachIndexed { i, option ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (active) Modifier.shadow(Elevation.sm, GearShape.xs) else Modifier)
                    .clip(GearShape.xs)
                    .background(if (active) GearSurface.card else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = GearType.labelM,
                    color = if (active) GearText.primary else GearText.tertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

/* ------------------------------------------------------------- previews */

@GearTypePreview
@Composable
private fun TextFieldStatesPreview() = PreviewFrame {
    GearTextField("", label = "Full name", placeholder = "As printed on your ID")
    GearTextField("2,800", label = "Price", prefix = "LKR", suffix = "/ day", state = FieldState.FILLED)
    GearTextField(
        "24", label = "Board volume", suffix = "L", state = FieldState.ERROR,
        error = "That's too little float for 68 kg",
    )
    GearTextField("Locked", label = "Item code", state = FieldState.DISABLED)
}

@GearPreview
@Composable
private fun SearchBarPreview() = PreviewFrame {
    SearchBar(placeholder = "Search gear, sports, brands")
    SearchBar(placeholder = "Search", value = "Jaffna · 8–10 Aug", onFilter = {}, filterCount = 2)
}

@GearTypePreview
@Composable
private fun SelectionControlsPreview() = PreviewFrame {
    SegmentedControl(listOf("Matches", "Browse", "My swaps"), 0) {}
    ToggleRow("Only show gear that fits me", true, {}, helper = "Based on 174 cm, 68 kg")
    ToggleRow("Delivery available", false, {})
    CheckRow("I have read the safety briefing", true, {})
    Row(horizontalArrangement = Arrangement.spacedBy(Space.m), verticalAlignment = Alignment.CenterVertically) {
        Toggle(true, {})
        Toggle(false, {})
        GearCheckbox(true, {})
        GearRadio(true)
        GearRadio(false)
    }
    Stepper("174 cm", {}, {})
}

@GearPreview
@Composable
private fun SlidersPreview() = PreviewFrame {
    GearSlider(0.42f, {}, valueLabel = lkr(2_100))
    GearRangeSlider(0.10f, 0.62f, { _, _ -> }, histogram = Demo.priceHistogram)
}

/** All eight calendar states — the set S-27's master calendar is built from. */
@GearPreview
@Composable
private fun DateRangeCellStatesPreview() = PreviewFrame {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        DayState.entries.forEachIndexed { i, state ->
            DateRangeCell("${i + 8}", state)
        }
    }
}
