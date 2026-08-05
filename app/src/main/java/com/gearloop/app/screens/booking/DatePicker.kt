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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.DayState
import com.gearloop.app.data.Demo
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space

/* ------------------------------------------------------------------ S-08 */

/**
 * August 2026 starts on a Saturday, which is what makes Sat 8 – Mon 10 Aug
 * land correctly across every screen that quotes those dates.
 */
private const val AUG_FIRST_WEEKDAY = 5 // Monday = 0
private const val SEP_FIRST_WEEKDAY = 1

private fun augustState(day: Int): DayState = when (day) {
    in 1..4 -> DayState.DISABLED          // already past
    5 -> DayState.TODAY
    8, 10 -> DayState.SELECTED
    9 -> DayState.IN_RANGE
    11 -> DayState.BUFFER                 // turnaround after this rental
    in 14..16 -> DayState.BOOKED
    17 -> DayState.BUFFER
    24, 25 -> DayState.BLOCKED
    else -> DayState.AVAILABLE
}

private fun septemberState(day: Int): DayState = when (day) {
    in 4..6 -> DayState.BOOKED
    7 -> DayState.BUFFER
    else -> DayState.AVAILABLE
}

@Composable
fun S08DatePicker(nav: Nav) {
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
                Text("Choose your dates", style = GearType.headingL, color = GearText.primary)
                Spacer(Modifier.weight(1f))
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
                VGap(Space.m)
                MonthGrid("August 2026", AUG_FIRST_WEEKDAY, 31, ::augustState)
                VGap(Space.l)
                MonthGrid("September 2026", SEP_FIRST_WEEKDAY, 30, ::septemberState)

                VGap(Space.l)
                CalendarLegend()

                VGap(Space.l)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.md)
                        .background(GearColor.Chalk50)
                        .padding(Space.cardPadding),
                ) {
                    Demo.datePanelLines.forEachIndexed { i, line ->
                        PriceRow(line)
                        if (i != Demo.datePanelLines.lastIndex) VGap(Space.s)
                    }
                }
                VGap(Space.section)
            }

            StickyFooter {
                GearButton("Confirm dates", { nav.to("s09") }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MonthGrid(
    label: String,
    firstWeekday: Int,
    days: Int,
    stateFor: (Int) -> DayState,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = GearType.headingM, color = GearText.primary)
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

        val cells = firstWeekday + days
        val rows = (cells + 6) / 7
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val index = r * 7 + c
                    val day = index - firstWeekday + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            DateRangeCell(
                                day = day.toString(),
                                state = stateFor(day),
                                onClick = { },
                                size = 42.dp,
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
        LegendItem("Booked") { SwatchBox(GearColor.Pine800) }
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
        "Buffer is turnaround time — the owner needs 2 h to rinse and check the board " +
            "before it goes out again.",
        style = GearType.bodyS,
        color = GearText.secondary,
    )
}
