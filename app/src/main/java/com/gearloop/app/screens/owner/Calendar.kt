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
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.DayState
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.countdown
import com.gearloop.app.data.dateRange
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.Booking
import com.gearloop.app.data.repo.CalendarUnit
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
import com.gearloop.app.ui.vm.ExchangeSupplyViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/* ------------------------------------------------------------------ S-27 */

/**
 * S-27 · Read every unit's fortnight — bookings, requests, blocks and the buffer
 * after each booking — from the same rules the renter's calendar uses (FR-16,
 * FR-17); Create a block on a free day and Delete one, in block mode; Update a
 * booking onto a free unit to clear a conflict.
 */
@Composable
fun S27MasterCalendar(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val units by vm.units.collectAsStateWithLifecycle()
    val conflicts by vm.conflicts.collectAsStateWithLifecycle()
    // Observed so the timeline redraws when a block or a booking changes.
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var startDay by rememberSaveable { mutableLongStateOf(AppClock.today.toEpochDay()) }
    var blocking by rememberSaveable { mutableStateOf(false) }
    val from = LocalDate.ofEpochDay(startDay)
    val days = if (mode == 0) 14 else 7
    val to = from.plusDays(days - 1L)

    GearScreen(
        appBar = {
            AppBar(
                title = "Calendar",
                subtitle = account?.person?.name?.let { "${it.uppercase()} · OWNER" },
                onBack = { nav.back() },
                actions = { GearIconButton(Lucide.Inbox, "Requests", { nav.to("s28") }, size = 44.dp) },
            )
        },
        contentPadding = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.s)
            SegmentedControl(listOf("Fortnight", "Week"), mode) { mode = it }
            VGap(Space.sm)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GearIconButton(
                    Lucide.ChevronLeft, "Previous", { startDay -= days },
                    size = 44.dp, iconSize = IconSize.s,
                )
                Text(
                    dateRange(from, to) + " ${to.year}",
                    style = GearType.headingS,
                    color = GearText.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                GearIconButton(
                    Lucide.ChevronRight, "Next", { startDay += days },
                    size = 44.dp, iconSize = IconSize.s,
                )
            }
        }

        VGap(Space.s)
        if (units.isEmpty()) {
            Column(Modifier.padding(horizontal = Space.screen)) {
                EmptyState(icon = Lucide.CalendarDays, headline = "Nothing listed yet", body = "Each unit you list gets a row here.")
            }
        }
        UnitTimeline(units, from, days, blocking, vm, bookings, blocks)

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.m)
            CalendarStateLegend()

            conflicts.forEach { c ->
                VGap(Space.m)
                val hoursLate = ChronoUnit.HOURS.between(c.late.returnAt, AppClock.now()).coerceAtLeast(1)
                val untilNext = ChronoUnit.MILLIS.between(AppClock.now(), c.next.pickupAt)
                AccentCard(GearColor.Danger500, Modifier.fillMaxWidth()) {
                    Text("Booking conflict", style = GearType.headingS, color = GearText.primary)
                    VGap(4.dp)
                    Text(
                        "${c.late.unitLabel} is $hoursLate h overdue — ${c.next.renter.name.substringBefore(' ')}'s booking starts in ${countdown(untilNext)}.",
                        style = GearType.bodyS,
                        color = GearText.secondary,
                    )
                    VGap(Space.sm)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        if (c.freeUnit != null) {
                            GearButton(
                                "Move to unit #${c.freeUnit}", { vm.moveToUnit(c.next.ref, c.freeUnit) },
                                type = ButtonType.SECONDARY, size = ButtonSize.S,
                            )
                        }
                        GearButton(
                            "Message ${c.late.renter.name.substringBefore(' ')}",
                            { vm.openThread(c.late.renter.handle); nav.to("s33") },
                            type = ButtonType.GHOST, size = ButtonSize.S,
                        )
                    }
                }
            }

            VGap(Space.l)
            Eyebrow("Occupancy this ${if (mode == 0) "fortnight" else "week"}")
            VGap(Space.s)
            GearCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.cardPadding)) {
                    units.forEach { u ->
                        val pct = vm.occupancyFor(u, from)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(u.label, style = GearType.bodyS, color = GearText.primary, modifier = Modifier.width(130.dp), maxLines = 1)
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

            VGap(Space.m)
            if (blocking) {
                Banner(
                    text = "Tap a free day to block it, or a blocked day to free it.",
                    kind = BannerKind.INFO,
                )
                VGap(Space.s)
            }
            GearFab(
                if (blocking) Lucide.Check else Lucide.CalendarX,
                if (blocking) "Done blocking" else "Block dates",
                { blocking = !blocking },
                label = if (blocking) "Done" else "Block dates",
            )
            VGap(Space.section)
        }
    }
}

/** Rows are inventory units, columns are days. Cells reuse the calendar states. */
@Composable
private fun UnitTimeline(
    units: List<CalendarUnit>,
    from: LocalDate,
    days: Int,
    blocking: Boolean,
    vm: ExchangeSupplyViewModel,
    bookings: List<Booking>,
    blocks: List<com.gearloop.app.data.db.CalendarBlockEntity>,
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState())) {
        Column {
            // header row
            Row {
                Spacer(Modifier.width(Space.screen))
                Spacer(Modifier.width(120.dp))
                repeat(days) { d ->
                    val day = from.plusDays(d.toLong())
                    Text(
                        "${day.dayOfMonth}",
                        style = GearType.labelXs,
                        color = if (day == AppClock.today) GearText.primary else GearText.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(if (blocking) 44.dp else 26.dp),
                    )
                }
            }
            VGap(4.dp)
            units.forEach { u ->
                // Computed from the lists passed in, so a new block or booking redraws the
                // row. An unused "version" parameter did not: Compose skips on unused
                // parameters, and a blocked day stayed drawn as free (D-16).
                val cells = vm.cellsFor(u, from, bookings, blocks).take(days)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(Space.screen))
                    Text(
                        u.label,
                        style = GearType.bodyS,
                        color = GearText.primary,
                        maxLines = 1,
                        modifier = Modifier.width(120.dp),
                    )
                    cells.forEach { cell ->
                        Box(
                            Modifier
                                .width(if (blocking) 44.dp else 26.dp)
                                .height(if (blocking) 44.dp else 40.dp)
                                .padding(1.dp)
                                .then(
                                    if (blocking && cell.state in setOf(DayState.AVAILABLE, DayState.BLOCKED)) {
                                        Modifier.clickable(
                                            role = Role.Button,
                                            onClickLabel = if (cell.state == DayState.BLOCKED) "Free this day" else "Block this day",
                                        ) { vm.toggleBlock(u, cell.date) }
                                    } else Modifier
                                )
                                .semantics { contentDescription = "${u.label}, ${dayMonth(cell.date)}, ${cell.state.name.lowercase().replace('_', ' ')}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            DateRangeCell(
                                day = "",
                                state = cell.state,
                                size = 24.dp,
                            )
                        }
                    }
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
        LegendItem("Booked") { SwatchBox(GearColor.Pine800, underline = GearColor.HiVis500) }
        LegendItem("Requested") { SwatchBox(GearColor.HiVis200) }
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

/**
 * S-28 · Read the owner's requests — each with the renter's live fit verdict,
 * trust tier and trust breakdown attached (FR-23) — and their upcoming and past
 * bookings; Update a request to accepted or declined. The renter is told either
 * way, and an accepted request appears on S-27 as booked.
 */
@Composable
fun S28RequestsInbox(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val requests by vm.requests.collectAsStateWithLifecycle()
    val upcoming by vm.ownerUpcoming.collectAsStateWithLifecycle()
    val past by vm.ownerPast.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(requests.firstOrNull()?.ref) }
    var declining by rememberSaveable { mutableStateOf<String?>(null) }
    val account by vm.account.collectAsStateWithLifecycle()

    GearScreen(
        appBar = { AppBar(title = "Requests", subtitle = account?.person?.name?.let { "${it.uppercase()} · OWNER" }, onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        SegmentedControl(
            listOf("Requests (${requests.size})", "Upcoming", "Past"),
            tab,
        ) { tab = it }
        VGap(Space.m)

        when (tab) {
            0 -> {
                if (requests.isEmpty()) {
                    EmptyState(icon = Lucide.Inbox, headline = "No requests waiting", body = "New requests appear here.")
                }
                requests.forEach { request ->
                    RequestCard(
                        request = request,
                        stats = vm.trustStats(request.renter.handle),
                        expanded = expanded == request.ref,
                        onToggle = { expanded = if (expanded == request.ref) null else request.ref },
                        onAccept = { vm.acceptRequest(request.ref, request.renter.name.substringBefore(' ')) },
                        onDecline = { declining = request.ref },
                        onMessage = { vm.openThread(request.renter.handle); nav.to("s33") },
                    )
                    VGap(Space.listGap)
                }
            }
            1 -> {
                if (upcoming.isEmpty()) EmptyState(icon = Lucide.CalendarDays, headline = "Nothing booked yet", body = "Accepted requests and instant bookings appear here.")
                upcoming.forEach { b ->
                    if (b.status == BookingStatus.RETURNED) {
                        BookingCard(
                            b.item, "${b.dates} · ${b.renter.name}", "Back · check it", GearColor.Warning700,
                            "Check the return", { vm.selectBooking(b.ref); nav.to("s18b") }, reference = b.ref,
                        )
                    } else BookingCard(
                        b.item, "${b.dates} · ${b.renter.name}",
                        if (b.status == BookingStatus.ACTIVE) "Out now" else "Confirmed",
                        if (b.status == BookingStatus.ACTIVE) GearColor.Warning700 else GearColor.Success500,
                        "See it on the calendar", { nav.to("s27") }, reference = b.ref,
                    )
                    VGap(Space.listGap)
                }
            }
            else -> {
                if (past.isEmpty()) EmptyState(icon = Lucide.History, headline = "No history yet", body = "Finished, declined and cancelled bookings are kept here.")
                past.forEach { b ->
                    BookingCard(
                        b.item, "${b.dates} · ${b.renter.name}", b.status.label, GearColor.Chalk600,
                        "Message ${b.renter.name.substringBefore(' ')}",
                        { vm.openThread(b.renter.handle); nav.to("s33") }, reference = b.ref,
                    )
                    VGap(Space.listGap)
                }
            }
        }
        VGap(Space.section)
    }

    declining?.let { ref ->
        val request = requests.firstOrNull { it.ref == ref }
        GearDialog(
            title = "Decline this request?",
            onDismiss = { declining = null },
            confirmLabel = "Keep it",
            onConfirm = { declining = null },
            destructiveLabel = "Decline request",
            onDestructive = {
                declining = null
                vm.declineRequest(ref, request?.renter?.name?.substringBefore(' ') ?: "The renter")
            },
        ) {
            Text(
                "${request?.renter?.name ?: "The renter"} is refunded in full and the dates free up straight away.",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
    }
}

@Composable
private fun RequestCard(
    request: Booking,
    stats: Triple<Int, Int, Int>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onMessage: () -> Unit,
) {
    val renter = request.renter
    val item = request.item
    val left = request.respondBy?.let { ChronoUnit.MILLIS.between(AppClock.now(), it) } ?: 0
    GearCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.cardPadding)) {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Trust breakdown", onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
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
                    // Per renter, read from the renter's record.
                    listOf(
                        "Completed rentals" to "${stats.first}",
                        "On-time returns" to "${stats.second}%",
                        "Verification" to "${stats.third} / 6",
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
                    Text(request.unitLabel, style = GearType.labelL, color = GearText.primary, maxLines = 1)
                    VGap(3.dp)
                    GearTag(item.tagLine)
                }
            }

            VGap(Space.sm)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(request.dates, style = GearType.dataM, color = GearText.primary)
                Spacer(Modifier.weight(1f))
                // What the owner earns for exactly these nights.
                Text(lkr(request.ownerPayout), style = GearType.dataL, color = GearText.primary)
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
                Text(
                    if (left > 0) "Respond within ${countdown(left)}" else "Reply overdue",
                    style = GearType.dataM,
                    color = GearColor.Warning700,
                )
            }

            VGap(Space.sm)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                GearButton("Decline", onDecline, type = ButtonType.GHOST, size = ButtonSize.S)
                // It opens the conversation; "Counter" promised a counter-offer
                // that a booking request has no way to make.
                GearButton("Message", onMessage, type = ButtonType.SECONDARY, size = ButtonSize.S, icon = Lucide.MessageSquare)
                Spacer(Modifier.weight(1f))
                GearButton("Accept", onAccept, size = ButtonSize.S)
            }
        }
    }
}
