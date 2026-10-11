package com.gearloop.app.screens.account

import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.Demo
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Grade
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.Phase
import com.gearloop.app.data.countdown
import com.gearloop.app.data.dayTime
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.Booking
import com.gearloop.app.data.repo.ClosetItem
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.vm.EvidenceTrustViewModel
import com.gearloop.app.ui.vm.ExchangeSupplyViewModel
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.MoneyViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.temporal.ChronoUnit

/* ------------------------------------------------------------------ S-31 */

/**
 * S-31 · Read the renter's bookings by state; Update — cancel an upcoming one;
 * Delete — take a finished one off the history, with Undo.
 *
 * The card's primary action changes with the booking's state, because what a
 * renter can do next depends on where the rental is.
 */
@Composable
fun S31MyRentals(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val evidence: EvidenceTrustViewModel = gearViewModel()
    val discovery: FitDiscoveryViewModel = gearViewModel()
    val active by vm.active.collectAsStateWithLifecycle()
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    val past by vm.past.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var cancelling by rememberSaveable { mutableStateOf<String?>(null) }

    GearScreen(
        appBar = { AppBar(title = "Rentals") },
        tabBar = { MainTabBar(nav, MainTab.RENTALS) },
    ) {
        VGap(Space.s)
        SegmentedControl(
            listOf("Active (${active.size})", "Upcoming (${upcoming.size})", "Past"),
            tab,
        ) { tab = it }
        VGap(Space.m)

        when (tab) {
            0 -> {
                if (active.isEmpty()) {
                    EmptyState(
                        icon = Lucide.Package,
                        headline = "Nothing out right now",
                        body = "A rental appears here once you both sign at pickup.",
                        actionLabel = if (upcoming.isNotEmpty()) "See upcoming" else "Find gear",
                        onAction = { if (upcoming.isNotEmpty()) tab = 1 else nav.tab("s04") },
                    )
                }
                active.forEach { booking ->
                    ActiveCard(
                        booking,
                        onReturn = {
                            evidence.select(booking.ref)
                            evidence.startCapture(Phase.RETURN)
                            nav.to("s15")
                        },
                        onOpen = { evidence.select(booking.ref); nav.to(if (booking.status == BookingStatus.RETURNED) "s18" else "s17") },
                    )
                    VGap(Space.listGap)
                }
            }
            1 -> {
                if (upcoming.isEmpty()) {
                    EmptyState(
                        icon = Lucide.CalendarDays,
                        headline = "No upcoming rentals",
                        body = "Bookings and requests you've made appear here until pickup.",
                        actionLabel = "Find gear",
                        onAction = { nav.tab("s04") },
                    )
                }
                upcoming.forEach { booking ->
                    val requested = booking.status == BookingStatus.REQUESTED
                    val days = ChronoUnit.DAYS.between(AppClock.today, booking.start)
                    BookingCard(
                        booking.item,
                        booking.dates,
                        when {
                            requested -> "Awaiting ${booking.owner.name.substringBefore(' ')}"
                            days <= 0L -> "Pickup today"
                            days == 1L -> "Pickup tomorrow"
                            else -> "Pickup in $days days"
                        },
                        if (requested) GearColor.Warning700 else GearColor.Marine500,
                        if (requested) "Withdraw request" else "View pickup details",
                        {
                            if (requested) cancelling = booking.ref
                            else { vm.selectBooking(booking.ref); nav.to("s13") }
                        },
                        reference = booking.ref,
                        onClick = { vm.selectBooking(booking.ref); nav.to("s12") },
                    )
                    VGap(Space.listGap)
                }
            }
            else -> {
                if (past.isEmpty()) {
                    EmptyState(
                        icon = Lucide.History,
                        headline = "No past rentals",
                        body = "Finished, cancelled and declined bookings are kept here.",
                    )
                }
                past.forEach { booking ->
                    val toRate = booking.status == BookingStatus.COMPLETED && booking.rating == null
                    BookingCard(
                        booking.item,
                        booking.dates,
                        when {
                            booking.status != BookingStatus.COMPLETED -> booking.status.label
                            booking.rating != null -> "Rated ★${booking.rating}"
                            else -> "Rate it"
                        },
                        when {
                            booking.status != BookingStatus.COMPLETED -> GearColor.Chalk600
                            toRate -> GearColor.Warning700
                            else -> GearColor.Chalk600
                        },
                        if (toRate) "Leave a review" else "Book again",
                        {
                            discovery.select(booking.item)
                            nav.to(if (toRate) "s42" else "s07")
                        },
                        reference = booking.ref,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        GearLink(
                            "Remove from history", { vm.hideFromHistory(booking.ref) },
                            color = GearText.tertiary, icon = Lucide.Trash2,
                        )
                    }
                    VGap(Space.s)
                }
            }
        }
        VGap(Space.section)
    }

    cancelling?.let { ref ->
        GearDialog(
            title = "Withdraw this request?",
            onDismiss = { cancelling = null },
            confirmLabel = "Keep it",
            onConfirm = { cancelling = null },
            destructiveLabel = "Withdraw",
            onDestructive = { cancelling = null; vm.cancel(ref) },
        ) {
            Text(
                "The payment for $ref is refunded in full.",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
    }
}

@Composable
private fun ActiveCard(booking: Booking, onReturn: () -> Unit, onOpen: () -> Unit) {
    val returned = booking.status == BookingStatus.RETURNED
    GearCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)) {
        Column {
            Row(Modifier.padding(Space.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                GearImage(booking.item.hero, booking.item.title, Modifier.size(72.dp), GearShape.sm)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    StatusBadge(booking.status.label, if (returned) GearColor.Warning700 else GearColor.Success500)
                    VGap(6.dp)
                    Text(booking.item.shortTitle, style = GearType.headingS, color = GearText.primary)
                    VGap(4.dp)
                    Text(booking.ref, style = GearType.dataS, color = GearText.tertiary)
                }
            }
            Column(Modifier.padding(horizontal = Space.cardPadding)) {
                if (returned) {
                    Text(
                        "Back with ${booking.owner.name.substringBefore(' ')}, who has 24 h to check it.",
                        style = GearType.bodyS,
                        color = GearText.secondary,
                    )
                } else {
                    val left = ChronoUnit.MILLIS.between(AppClock.now(), booking.returnAt)
                    Countdown(countdown(left), if (left >= 0) "Until return" else "Overdue")
                    VGap(Space.s)
                    Text(
                        "Return by ${dayTime(booking.returnAt)} · ${booking.owner.area.substringBefore(',')}",
                        style = GearType.dataM,
                        color = GearText.secondary,
                    )
                }
            }
            Row(Modifier.padding(Space.cardPadding)) {
                if (returned) {
                    GearButton("See the return check", onOpen, Modifier.fillMaxWidth(), size = ButtonSize.M)
                } else {
                    GearButton("Start return", onReturn, Modifier.fillMaxWidth(), size = ButtonSize.M)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ S-36 */

private val closetSports = Demo.sports.map { it.first } + "Snowboarding"

/** The word for one of a sport's gear, for "your kayak hasn't moved". */
private fun noun(item: ClosetItem): String = when (item.sport) {
    "Kayaking" -> "kayak"
    "Cricket" -> "bat"
    "Climbing" -> "rack"
    "Cycling" -> "bike"
    "Surfing" -> "board"
    "SUP" -> "SUP"
    "Snowboarding" -> "snowboard"
    "Camping" -> "tent"
    else -> item.shortTitle
}

/**
 * S-36 · Read the renter's own kit; Create an item; Update one; Delete one with
 * Undo; and start a listing from an idle item ("List it"), which is how gear
 * that is sitting in a cupboard reaches the supply side (FR-11).
 */
@Composable
fun S36GearCloset(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val owned by vm.closet.collectAsStateWithLifecycle()
    val listings by vm.listings.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableLongStateOf(-1L) }
    val editing = owned.firstOrNull { it.id == editingId }
    val idle = owned.filter { !it.listed }.maxByOrNull { it.idleDays }

    GearScreen(
        appBar = {
            AppBar(
                title = "Gear closet",
                onBack = { nav.back() },
                actions = { GearIconButton(Lucide.Plus, "Add gear", { adding = true }, size = 44.dp) },
            )
        },
    ) {
        VGap(Space.s)
        Text(
            "${owned.count { it.listed }} listed · ${owned.count { !it.listed }} sitting idle",
            style = GearType.dataM,
            color = GearText.secondary,
        )

        if (idle != null) {
            VGap(Space.m)
            // What it could earn is read off what similar gear rents for nearby —
            // gear that is actually for rent, not listed for swap only.
            val dayRate = listings
                .filter { it.sport == idle.sport && it.owner.handle != vm.me && it.status == ListingStatus.PUBLISHED }
                .map { it.pricePerDay }.average()
                .takeIf { !it.isNaN() }?.toInt() ?: Demo.SUGGESTED_PRICE
            InsightCard(
                headline = "Your ${noun(idle)} hasn't moved in ${idle.idleDays} days",
                body = "It could earn about ${lkr(dayRate * 2 / 100 * 100)} a weekend.",
                action = "List it",
                onAction = { vm.startListing(idle.id) { nav.to("s25") } },
            )
        }

        VGap(Space.m)
        if (owned.isEmpty()) {
            EmptyState(
                icon = Lucide.Boxes,
                headline = "Your closet is empty",
                body = "Add what you own to rent it out or swap it.",
                actionLabel = "Add gear",
                onAction = { adding = true },
            )
        }
        owned.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                pair.forEach { item ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(GearShape.md)
                            .clickable(role = Role.Button, onClickLabel = "Edit ${item.shortTitle}") { editingId = item.id },
                    ) {
                        Column(Modifier.alpha(if (item.listed) 1f else 0.6f)) {
                            GearImage(
                                item.photo, item.title,
                                Modifier.fillMaxWidth().height(120.dp), GearShape.md,
                            )
                            VGap(Space.s)
                            Text(
                                item.shortTitle,
                                style = GearType.labelM,
                                color = GearText.primary,
                                maxLines = 2,
                            )
                            VGap(3.dp)
                            // Compact, like every other grid card. The full tag
                            // does not fit half a phone and was ellipsising the
                            // item code — "BC-1…" — which is the one part of a
                            // tag that has to be readable to be worth printing.
                            GearTag(
                                text = item.tagLineCompact,
                                tone = if (item.listed) TagTone.HI_VIS else TagTone.MUTED,
                            )
                        }
                        if (!item.listed) {
                            Text(
                                "NOT LISTED",
                                style = GearType.labelXs,
                                color = GearText.onDark,
                                modifier = Modifier
                                    .padding(Space.s)
                                    .clip(GearShape.xs)
                                    .background(GearColor.Chalk700)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            VGap(Space.m)
        }
        VGap(Space.section)
    }

    if (adding) {
        ClosetItemDialog(
            title = "Add to your closet",
            initial = null,
            onDismiss = { adding = false },
            vm = vm,
            onSave = { name, sport, size, grade, photo ->
                vm.addClosetItem(name, sport, size, grade, photo)
                adding = false
            },
        )
    }
    if (editing != null) {
        ClosetItemDialog(
            title = editing.shortTitle,
            initial = editing,
            onDismiss = { editingId = -1L },
            vm = vm,
            onSave = { name, _, size, grade, photo ->
                vm.updateClosetItem(editing.id, name, size, grade, photo)
                editingId = -1L
            },
            onDelete = {
                vm.deleteClosetItem(editing.id, editing.shortTitle)
                editingId = -1L
            },
            onList = if (!editing.listed) ({
                editingId = -1L
                vm.startListing(editing.id) { nav.to("s25") }
            }) else null,
        )
    }
}

@Composable
private fun ClosetItemDialog(
    title: String,
    initial: ClosetItem?,
    vm: ExchangeSupplyViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Grade, String?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onList: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var sport by rememberSaveable { mutableStateOf(initial?.sport ?: "Surfing") }
    var size by rememberSaveable { mutableStateOf(initial?.sizeLabel ?: "") }
    var grade by rememberSaveable { mutableIntStateOf(initial?.grade?.ordinal ?: 1) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberPhotoPicker(
        title = "Photo of your gear",
        onTaken = { file -> scope.launch { photo = vm.adoptPhoto(file) } },
        onChosen = { uri -> scope.launch { photo = vm.importPhoto(uri) } },
    )

    GearDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Add" else "Save",
        onConfirm = { onSave(name, sport, size, Grade.entries[grade], photo) },
        confirmEnabled = name.trim().length >= 3,
        destructiveLabel = if (onDelete != null) "Remove" else null,
        onDestructive = onDelete,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shown = photo ?: initial?.photo
            if (shown != null) {
                GearImage(shown, "Photo", Modifier.size(64.dp), GearShape.sm)
                Spacer(Modifier.width(Space.s))
            }
            GearButton(if (shown != null) "Change photo" else "Add a photo", { picker.open() }, type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Camera)
        }
        VGap(Space.s)
        GearTextField(name, label = "What is it?", placeholder = "e.g. Rip Curl 4/3 wetsuit", onValueChange = { name = it })
        VGap(Space.s)
        if (initial == null) {
            Text("Sport", style = GearType.labelM, color = GearText.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                closetSports.forEach { s -> Chip(s, selected = sport == s, onClick = { sport = s }) }
            }
            VGap(Space.s)
        }
        GearTextField(size, label = "Size", placeholder = "e.g. M, 158, 6'6\"", onValueChange = { size = it })
        VGap(Space.s)
        Text("Condition", style = GearType.labelM, color = GearText.secondary)
        VGap(6.dp)
        SegmentedControl(Grade.entries.map { "Grade ${it.letter}" }, grade) { grade = it }
        VGap(6.dp)
        Text(Grade.entries[grade].summary, style = GearType.bodyS, color = GearText.tertiary)
        if (onList != null) {
            VGap(Space.m)
            GearButton("List it for rent", onList, Modifier.fillMaxWidth(), type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Tag)
        }
    }
}
