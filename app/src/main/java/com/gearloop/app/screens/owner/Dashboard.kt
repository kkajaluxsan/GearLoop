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
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.clockTime
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.Attention
import com.gearloop.app.data.repo.Seed
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
    var reviewing by rememberSaveable { mutableStateOf<String?>(null) }
    var stars by rememberSaveable { mutableIntStateOf(5) }
    var body by rememberSaveable { mutableStateOf("") }

    val act: (Attention) -> Unit = { row ->
        when {
            row.taskId == "service" -> vm.completeTask("service")
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
                subtitle = "KAVITHA SELVAM · OWNER",
                actions = { GearIconButton(Lucide.CalendarDays, "Calendar", { nav.to("s27") }, size = 44.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.PROFILE) },
    ) {
        VGap(Space.s)
        EarningsHero(dashboard.earnings, dashboard.nextPayout)

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
            Text("Nothing waiting on you. Requests and late returns show up here.", style = GearType.bodyS, color = GearText.tertiary)
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
                    val today = event.at.toLocalDate() == DemoClock.today
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

        VGap(Space.l)
        Eyebrow("Worth doing")
        VGap(Space.s)
        // The two insights edit real listings: the Torq's photos, the kayak's price.
        Demo.ownerInsights.forEach { insight ->
            InsightCard(insight.headline, insight.body, insight.action, {
                if (insight.action == "Add photos") vm.editListing(Demo.torq.id) { nav.to("s25") }
                else vm.editListing(Demo.kayak.id) { nav.to("s26") }
            })
            VGap(Space.s)
        }

        VGap(Space.m)
        GearButton(
            "Add a listing", { vm.startListing(Seed.OWNER) { nav.to("s25") } }, Modifier.fillMaxWidth(),
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
            Text("How was Mohamed as a renter? Other owners read this before they accept a request.", style = GearType.bodyS, color = GearText.secondary)
            VGap(Space.s)
            StarPicker(stars, { stars = it })
            VGap(Space.s)
            GearTextField(body, label = "Your review", onValueChange = { body = it }, singleLine = false)
        }
    }
}

@Composable
private fun EarningsHero(earnings: Int, payout: Int) {
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
                Demo.earningsSpark,
                Modifier.width(96.dp).height(36.dp),
                color = GearColor.HiVis500,
            )
        }
        VGap(Space.sm)
        Text(
            "Next payout ${lkr(payout)} · ${Demo.NEXT_PAYOUT_DATE}",
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
