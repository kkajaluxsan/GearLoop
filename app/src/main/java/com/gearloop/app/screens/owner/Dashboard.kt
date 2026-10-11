package com.gearloop.app.screens.owner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.Demo
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.clockTime
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.Attention
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.vm.ExchangeSupplyViewModel
import com.gearloop.app.ui.vm.gearViewModel

/* ------------------------------------------------------------------ S-24 */

/**
 * S-24 · Read the owner's month: earnings, occupancy, what needs doing and
 * what is coming up — all derived from bookings, requests and tasks. Update a
 * task to done ("Block dates" creates the block it names; "Write it" creates
 * the review it asks for), and Create a new listing.
 *
 * This is Kavitha's console. A request Nirmal sends from the renter's side
 * appears here as the most urgent row.
 */
@Composable
fun S24OwnerDashboard(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    val attention by vm.attention.collectAsStateWithLifecycle()
    val coming by vm.comingUp.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val mine by vm.myListings.collectAsStateWithLifecycle()
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val requests by vm.requests.collectAsStateWithLifecycle()
    var reviewing by rememberSaveable { mutableStateOf<String?>(null) }
    var stars by rememberSaveable { mutableIntStateOf(5) }
    var body by rememberSaveable { mutableStateOf("") }

    val act: (Attention) -> Unit = { row ->
        when {
            row.taskId != null && row.item.action == "Block dates" -> vm.completeTask(row.taskId)
            row.taskId == "review" -> { reviewing = row.taskId; body = "" }
            row.route == "s33" && row.bookingRef != null -> {
                vm.bookings.value.firstOrNull { it.ref == row.bookingRef }?.let { vm.openThread(it.renter.handle) }
                nav.to("s33")
            }
            else -> nav.to(row.route)
        }
    }

    GearScreen(
        appBar = {
            AppBar(
                title = "Your gear",
                subtitle = account?.person?.name?.let { "${it.uppercase()} · OWNER" },
                actions = { GearIconButton(Lucide.CalendarDays, "Calendar", { nav.to("s27") }, size = 44.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.PROFILE) },
    ) {
        VGap(Space.s)
        if (mine.isEmpty()) {
            EmptyState(
                icon = Lucide.Store,
                headline = "Lend your gear",
                body = "List something you own and it's in search straight away.",
                actionLabel = "List your first item",
                onAction = { vm.startListing { nav.to("s25") } },
            )
            VGap(Space.section)
            return@GearScreen
        }
        EarningsHero(dashboard.earnings, dashboard.nextPayout, dashboard.weekly)

        VGap(Space.sm)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            StatTile("${dashboard.occupancy}%", "Occupancy", Modifier.weight(1f))
            StatTile("${dashboard.listings}", "Listings", Modifier.weight(1f))
            StatTile("★ ${dashboard.ratingText}", "Rating", Modifier.weight(1f))
        }

        VGap(Space.l)
        Eyebrow("Needs you now")
        VGap(Space.s)
        if (attention.isEmpty()) {
            Text("Nothing waiting on you.", style = GearType.bodyS, color = GearText.tertiary)
        }
        attention.forEach { row ->
            AttentionRow(row.item) { act(row) }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("Coming up")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                if (coming.isEmpty()) {
                    Text("No pickups or returns in the next three days.", style = GearType.bodyS, color = GearText.tertiary)
                }
                coming.forEachIndexed { i, event ->
                    val today = event.at.toLocalDate() == AppClock.today
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { nav.to("s27") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.width(64.dp)) {
                            Text(clockTime(event.at), style = GearType.dataM, color = GearText.primary)
                            Text(
                                if (today) "TODAY" else event.at.dayOfWeek.name.take(3),
                                style = GearType.dataS, color = GearText.tertiary,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(event.label, style = GearType.labelL, color = GearText.primary)
                            Text(event.what, style = GearType.bodyS, color = GearText.tertiary)
                        }
                        GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
                    }
                    if (i != coming.lastIndex) GearDivider()
                }
            }
        }

        // Worked out from the member's own listings: too few photos, or no
        // bookings in a month — each one opens the listing to fix it.
        val monthAgo = AppClock.today.minusDays(30)
        val live = mine.filter { it.status == com.gearloop.app.data.ListingStatus.PUBLISHED }
        val fewPhotos = live.firstOrNull { it.photos.count(com.gearloop.app.data.Photos::isFile) == 0 && it.photos.size < 3 }
        val idle = live.firstOrNull { item -> bookings.none { it.item.id == item.id && !it.start.isBefore(monthAgo) } }
        if (fewPhotos != null || idle != null) {
            VGap(Space.l)
            Eyebrow("Worth doing")
            VGap(Space.s)
            fewPhotos?.let { item ->
                InsightCard(
                    "${item.shortTitle} has ${item.photos.size} photo${if (item.photos.size == 1) "" else "s"}",
                    "Adding the seven condition angles takes about two minutes.",
                    "Add photos",
                    { vm.editListing(item.id) { nav.to("s25") } },
                )
                VGap(Space.s)
            }
            idle?.takeIf { it.id != fewPhotos?.id }?.let { item ->
                val market = vm.marketFor(item.sport, item.id)
                InsightCard(
                    "${item.shortTitle} hasn't been booked in 30 days",
                    if (market.count > 0) "Similar ${item.sport.lowercase()} gear nearby rents for about ${lkr(market.median)} a day. Yours is ${lkr(item.pricePerDay)}."
                    else "Try instant book or a lower daily rate to get the first booking.",
                    "Review pricing",
                    { vm.editListing(item.id) { nav.to("s26") } },
                )
                VGap(Space.s)
            }
        }

        VGap(Space.m)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            GearButton(
                "Your listings (${mine.size})", { nav.to(com.gearloop.app.nav.Routes.MY_LISTINGS) }, Modifier.weight(1f),
                type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Tags,
            )
            // With the count, like the listings beside it, so a waiting request shows from here.
            GearButton(
                if (requests.isEmpty()) "Requests" else "Requests (${requests.size})", { nav.to("s28") }, Modifier.weight(1f),
                type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Inbox,
            )
        }
        VGap(Space.s)
        GearButton(
            "Add a listing", { vm.startListing { nav.to("s25") } }, Modifier.fillMaxWidth(),
            icon = Lucide.Plus,
        )
        VGap(Space.section)
    }

    reviewing?.let { taskId ->
        GearDialog(
            title = "Review Mohamed",
            onDismiss = { reviewing = null },
            confirmLabel = "Post review",
            onConfirm = {
                vm.reviewRenter(taskId, Demo.rizwan.handle, Demo.trekMarlin.id, stars, body)
                reviewing = null
            },
            confirmEnabled = body.trim().length >= 10,
        ) {
            Text("Other owners read this before they accept a request.", style = GearType.bodyS, color = GearText.secondary)
            VGap(Space.s)
            StarPicker(stars, { stars = it })
            VGap(Space.s)
            GearTextField(body, label = "Your review", onValueChange = { body = it }, singleLine = false)
        }
    }
}

@Composable
private fun EarningsHero(earnings: Int, payout: Int, weekly: List<Float>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.md)
            .background(GearColor.Pine800)
            .padding(Space.cardPadding),
    ) {
        Text("THIS MONTH", style = GearType.labelXs, color = GearColor.HiVis500)
        VGap(6.dp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(lkr(earnings), style = GearType.dataXl, color = GearText.onDark)
            Spacer(Modifier.weight(1f))
            Sparkline(
                weekly,
                Modifier.width(96.dp).height(36.dp),
                color = GearColor.HiVis500,
            )
        }
        VGap(Space.sm)
        Text(
            if (payout > 0) "${lkr(payout)} more to come as current rentals are returned" else "Paid into your wallet when each rental is returned",
            style = GearType.bodyS,
            color = GearColor.Chalk300,
        )
    }
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - (v.coerceIn(0f, 1f) * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun AttentionRow(row: NeedsAttention, onAction: () -> Unit) {
    val accent = if (row.severity == NeedsAttention.Severity.URGENT) {
        GearColor.Danger500
    } else {
        GearColor.Warning500
    }
    AccentCard(accent, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.title, style = GearType.headingS, color = GearText.primary)
                VGap(3.dp)
                Text(row.detail, style = GearType.bodyS, color = GearText.secondary)
            }
            Spacer(Modifier.width(Space.s))
            GearButton(row.action, onAction, type = ButtonType.SECONDARY, size = ButtonSize.S)
        }
    }
}
