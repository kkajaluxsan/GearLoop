package com.gearloop.app.screens.owner

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.DayState
import com.gearloop.app.data.Demo
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
/* ------------------------------------------------------------------ S-27 */

@Composable
fun S27MasterCalendar(nav: Nav) {
    var mode by rememberSaveable { mutableIntStateOf(0) }

    GearScreen(
        appBar = {
            AppBar(
                title = "Calendar",
                onBack = { nav.back() },
                actions = { GearIconButton(Lucide.Funnel, "Filter", { }, size = 40.dp) },
            )
        },
        contentPadding = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.s)
            SegmentedControl(listOf("Month", "Week"), mode) { mode = it }
            VGap(Space.sm)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GearIconButton(Lucide.ChevronLeft, "Previous", { }, size = 36.dp, iconSize = IconSize.s)
                Text(
                    "8 – 21 August 2026",
                    style = GearType.headingS,
                    color = GearText.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                GearIconButton(Lucide.ChevronRight, "Next", { }, size = 36.dp, iconSize = IconSize.s)
            }
        }

        VGap(Space.s)
        UnitTimeline()

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.m)
            CalendarStateLegend()

            VGap(Space.m)
            AccentCard(GearColor.Danger500, Modifier.fillMaxWidth()) {
                Text("Booking conflict", style = GearType.headingS, color = GearText.primary)
                VGap(4.dp)
                Text(
                    "Trek Marlin #1 is 6 h overdue — Ayesha's booking starts in 2 h.",
                    style = GearType.bodyS,
                    color = GearText.secondary,
                )
                VGap(Space.sm)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton("Move to unit #2", { }, type = ButtonType.SECONDARY, size = ButtonSize.S)
                    GearButton("Message Ayesha", { nav.to("s33") }, type = ButtonType.GHOST, size = ButtonSize.S)
                }
            }
            VGap(Space.l)
            Eyebrow("Occupancy this fortnight")
            VGap(Space.s)
            GearCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.cardPadding)) {
                    Demo.calendarUnits.forEachIndexed { i, unit ->
                        val pct = unitOccupancy[i]
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(unit, style = GearType.bodyS, color = GearText.primary, modifier = Modifier.width(130.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(GearShape.full)
                                    .background(GearColor.Chalk200),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(pct / 100f)
                                        .height(6.dp)
                                        .clip(GearShape.full)
                                        .background(if (pct >= 60) GearColor.Success500 else GearColor.Warning500)
                                )
                            }
                            Spacer(Modifier.width(Space.s))
                            Text("$pct%", style = GearType.dataM, color = GearText.primary, modifier = Modifier.width(44.dp))
                        }
                    }
                }
            }
            VGap(Space.s)
            Text(
                "Trek Marlin #2 sits idle because bookings default to unit #1. " +
                    "Rotating the default would even them out.",
                style = GearType.bodyS,
                color = GearText.tertiary,
            )

            VGap(Space.m)
            GearFab(Lucide.CalendarX, "Block dates", { }, label = "Block dates")
            VGap(Space.section)
        }
    }
}

/** Occupancy per inventory unit, shown on the timeline and summarised below it. */
private val unitOccupancy = listOf(72, 58, 91, 34)

/** Rows are inventory units, columns are days. Cells reuse the calendar states. */
@Composable
private fun UnitTimeline() {
    val days = 14
    val occupancy = unitOccupancy

    fun stateFor(unit: Int, day: Int): DayState = when (unit) {
        0 -> when (day) {
            in 0..2 -> DayState.BOOKED
            3 -> DayState.BUFFER
            in 7..9 -> DayState.BOOKED
            10 -> DayState.BUFFER
            else -> DayState.AVAILABLE
        }
        1 -> when (day) {
            in 4..6 -> DayState.BOOKED
            7 -> DayState.BUFFER
            in 11..13 -> DayState.BLOCKED
            else -> DayState.AVAILABLE
        }
        2 -> when (day) {
            in 0..3 -> DayState.BOOKED
            4 -> DayState.BOOKED       // the overdue one, bleeding into the next
            5 -> DayState.BOOKED
            6 -> DayState.BUFFER
            else -> DayState.AVAILABLE
        }
        else -> when (day) {
            in 4..5 -> DayState.BLOCKED
            in 9..11 -> DayState.BOOKED
            12 -> DayState.BUFFER
            else -> DayState.AVAILABLE
        }
    }

    Row(Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState())) {
        Column {
            // header row
            Row {
                Spacer(Modifier.width(Space.screen))
                Spacer(Modifier.width(120.dp))
                repeat(days) { d ->
                    Text(
                        "${8 + d}",
                        style = GearType.labelXs,
                        color = GearText.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(26.dp),
                    )
                }
            }
            VGap(4.dp)
            Demo.calendarUnits.forEachIndexed { u, unit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(Space.screen))
                    Text(
                        unit,
                        style = GearType.bodyS,
                        color = GearText.primary,
                        maxLines = 1,
                        modifier = Modifier.width(120.dp),
                    )
                    repeat(days) { d ->
                        val conflict = u == 2 && d == 5
                        Box(
                            Modifier
                                .width(26.dp)
                                .height(40.dp)
                                .padding(1.dp)
                                .then(
                                    if (conflict) Modifier.border(Border.heavy, GearColor.Danger500, GearShape.xs)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            DateRangeCell(
                                day = "",
                                state = stateFor(u, d),
                                size = 24.dp,
                            )
                        }
                    }
                    Spacer(Modifier.width(Space.s))
                    Text(
                        "${occupancy[u]}%",
                        style = GearType.dataS,
                        color = GearText.secondary,
                        modifier = Modifier.width(38.dp),
                    )
                    Spacer(Modifier.width(Space.screen))
                }
            }
        }
    }
}

@Composable
private fun CalendarStateLegend() {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        LegendItem("Available") { SwatchBox(GearSurface.card, bordered = true) }
        LegendItem("Booked") { SwatchBox(GearColor.Pine800) }
        LegendItem("Blocked") { SwatchBox(GearColor.Chalk200) }
        LegendItem("Buffer — turnaround") {
            val hatchColor = GearColor.Chalk300
            Box(Modifier.size(16.dp).clip(GearShape.xs).border(Border.hair, GearSurface.border, GearShape.xs)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawHatch(hatchColor, spacingPx = 5.dp.toPx(), strokeWidthPx = 1.5.dp.toPx())
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ S-28 */

@Composable
fun S28RequestsInbox(nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableIntStateOf(0) }

    GearScreen(
        appBar = { AppBar(title = "Requests", onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        SegmentedControl(listOf("Requests (3)", "Upcoming", "Past"), tab) { tab = it }
        VGap(Space.m)

        Demo.requestInbox.forEachIndexed { i, (renter, item, dates) ->
            RequestCard(
                renter = renter,
                item = item,
                dates = dates,
                expanded = expanded == i,
                onToggle = { expanded = if (expanded == i) -1 else i },
                onAccept = { nav.to("s27") },
                onMessage = { nav.to("s33") },
            )
            VGap(Space.listGap)
        }
        VGap(Space.section)
    }
}

@Composable
private fun RequestCard(
    renter: com.gearloop.app.data.Person,
    item: com.gearloop.app.data.GearItem,
    dates: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAccept: () -> Unit,
    onMessage: () -> Unit,
) {
    GearCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.cardPadding)) {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
                Avatar(renter, size = 40.dp, showTierRing = true)
                Spacer(Modifier.width(Space.s))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(renter.name, style = GearType.headingS, color = GearText.primary)
                        TrustBadge(renter)
                    }
                    VGap(2.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarValue(renter.rating, renter.reviews)
                        Text("  ·  Member since ${renter.memberSince}", style = GearType.bodyS, color = GearText.tertiary)
                    }
                }
                GearIcon(
                    if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    null, size = IconSize.m, tint = GearColor.Chalk500,
                )
            }

            if (expanded) {
                VGap(Space.sm)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.sm)
                        .background(GearColor.Chalk50)
                        .padding(Space.sm),
                ) {
                    Eyebrow("Trust breakdown")
                    VGap(Space.s)
                    listOf(
                        "Completed rentals" to "8",
                        "On-time returns" to "100%",
                        "Verification" to "5 / 6",
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(label, style = GearType.bodyS, color = GearText.secondary)
                            Spacer(Modifier.weight(1f))
                            Text(value, style = GearType.dataM, color = GearText.primary)
                        }
                    }
                }
            }

            VGap(Space.sm)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GearImage(item.hero, item.title, Modifier.size(48.dp), GearShape.sm)
                Spacer(Modifier.width(Space.s))
                Column(Modifier.weight(1f)) {
                    Text(item.shortTitle, style = GearType.labelL, color = GearText.primary, maxLines = 1)
                    VGap(3.dp)
                    GearTag(item.tagLine)
                }
            }

            VGap(Space.sm)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dates, style = GearType.dataM, color = GearText.primary)
                Spacer(Modifier.weight(1f))
                Text(lkr(item.pricePerDay * 3), style = GearType.dataL, color = GearText.primary)
            }

            VGap(Space.s)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(GearShape.xs)
                    .background(GearColor.Warning100)
                    .padding(horizontal = Space.s, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearIcon(Lucide.Timer, null, size = IconSize.s, tint = GearColor.Warning500)
                Spacer(Modifier.width(6.dp))
                Text("Respond within 3h 12m", style = GearType.dataM, color = GearColor.Warning700)
            }

            VGap(Space.sm)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                GearButton("Decline", { }, type = ButtonType.GHOST, size = ButtonSize.S)
                GearButton("Counter", onMessage, type = ButtonType.SECONDARY, size = ButtonSize.S)
                Spacer(Modifier.weight(1f))
                GearButton("Accept", onAccept, size = ButtonSize.S)
            }
        }
    }
}
