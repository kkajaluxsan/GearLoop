package com.gearloop.app.screens.booking

import com.gearloop.app.ui.theme.Border
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.DayState
import com.gearloop.app.data.dateRange
import com.gearloop.app.data.monthYear
import com.gearloop.app.data.repo.DayCell
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.vm.MoneyViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.YearMonth

/* ------------------------------------------------------------------ S-08 */

/**
 * S-08 · Read the listing's real availability — other bookings, owner blocks and
 * the buffer day after each — and Update the draft's dates with each tap.
 *
 * The first tap starts a range and the second closes it, if every day between
 * is free; a booked, blocked or past day ignores the tap. Both calendars, this
 * one and the owner's on S-27, are drawn by the same rules, so a day cannot be
 * free here and held there (FR-16, FR-17).
 */
@Composable
fun S08DatePicker(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val draftState by vm.draft.collectAsStateWithLifecycle()
    // Opened directly (from the screen index, say): start the draft here.
    LaunchedEffect(Unit) { if (vm.draft.value == null) vm.ensureDraft() }
    val draft = draftState

    Column(Modifier.fillMaxSize().background(GearColor.Pine900.copy(alpha = 0.45f))) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { nav.back() }
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(GearShape.sheet)
                .background(GearSurface.card),
        ) {
            SheetGrabber()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.screen, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Choose your dates", style = GearType.headingL, color = GearText.primary)
                    if (draft != null) {
                        Text(draft.item.shortTitle, style = GearType.bodyS, color = GearText.tertiary)
                    }
                }
                GearIconButton(
                    com.gearloop.app.ui.icons.Lucide.X, "Close", { nav.back() }, size = 40.dp,
                )
            }
            GearDivider()

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberSavedScrollState())
                    .padding(horizontal = Space.screen),
            ) {
                if (draft != null) {
                    vm.monthsFor(draft).forEachIndexed { i, (month, cells) ->
                        VGap(if (i == 0) Space.m else Space.l)
                        MonthGrid(month, cells, onTap = { vm.tapDay(it) })
                    }

                    VGap(Space.l)
                    CalendarLegend()

                    VGap(Space.l)
                    val lines = draft.quote.dateLines()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(GearShape.md)
                            .background(GearColor.Chalk50)
                            .padding(Space.cardPadding),
                    ) {
                        lines.forEachIndexed { i, line ->
                            PriceRow(line)
                            if (i != lines.lastIndex) VGap(Space.s)
                        }
                    }
                }
                VGap(Space.section)
            }

            StickyFooter {
                GearButton(
                    if (draft == null) "Confirm dates"
                    else "Confirm ${dateRange(draft.start, draft.end)} · ${draft.days} ${if (draft.days == 1) "day" else "days"}",
                    { nav.to("s09") },
                    Modifier.fillMaxWidth(),
                    enabled = draft != null,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    cells: List<DayCell>,
    onTap: (java.time.LocalDate) -> Unit,
) {
    // Monday is column 0. August 2026 starts on a Saturday, which is what puts
    // Sat 8 – Mon 10 Aug on the second row.
    val firstWeekday = month.atDay(1).dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    Column(Modifier.fillMaxWidth()) {
        Text(monthYear(month), style = GearType.headingM, color = GearText.primary)
        VGap(Space.s)
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    style = GearType.labelXs,
                    color = GearText.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        VGap(Space.xs)

        val total = firstWeekday + days
        val rows = (total + 6) / 7
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val day = r * 7 + c - firstWeekday + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val cell = cells[day - 1]
                            val tappable = cell.state in setOf(DayState.AVAILABLE, DayState.TODAY, DayState.SELECTED, DayState.IN_RANGE)
                            DateRangeCell(
                                day = day.toString(),
                                state = cell.state,
                                onClick = if (tappable) ({ onTap(cell.date) }) else null,
                                size = 42.dp,
                                modifier = Modifier.semantics {
                                    contentDescription = "${cell.date.dayOfMonth} ${month.month.name.lowercase().replaceFirstChar { it.uppercase() }}, ${stateWord(cell.state)}"
                                },
                            )
                        } else {
                            Spacer(Modifier.size(42.dp))
                        }
                    }
                }
            }
        }
    }
}

/** What a cell means, said out loud — colour and hatching don't reach a screen reader. */
private fun stateWord(state: DayState): String = when (state) {
    DayState.AVAILABLE -> "available"
    DayState.SELECTED -> "selected"
    DayState.IN_RANGE -> "in your dates"
    DayState.BOOKED -> "booked"
    DayState.BUFFER -> "turnaround day, not bookable"
    DayState.BLOCKED -> "blocked by the owner"
    DayState.TODAY -> "today, available"
    DayState.DISABLED -> "past"
}

@Composable
private fun CalendarLegend() {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        LegendItem("Available") {
            SwatchBox(GearSurface.card, bordered = true)
        }
        LegendItem("Selected") { SwatchBox(GearColor.Pine800) }
        LegendItem("In range") { SwatchBox(GearColor.HiVis200) }
        // Booked and selected are the same fill; the hi-vis bar is the whole
        // difference, so the swatch carries it too.
        LegendItem("Booked") { SwatchBox(GearColor.Pine800, underline = GearColor.HiVis500) }
        LegendItem("Blocked") { SwatchBox(GearColor.Chalk200) }
        LegendItem("Buffer") {
            val hatchColor = GearColor.Chalk300
            Box(
                Modifier
                    .size(16.dp)
                    .clip(GearShape.xs)
                    .border(Border.hair, GearSurface.border, GearShape.xs),
            ) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    drawHatch(hatchColor, spacingPx = 5.dp.toPx(), strokeWidthPx = 1.5.dp.toPx())
                }
            }
        }
    }
    VGap(Space.s)
    Text(
        "Buffer is a day for the owner to check the gear. Tap a first and a last day.",
        style = GearType.bodyS,
        color = GearText.secondary,
    )
}
