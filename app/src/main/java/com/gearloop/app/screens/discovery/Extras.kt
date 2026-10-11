package com.gearloop.app.screens.discovery

import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.R
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.LocalSearchFilters
import com.gearloop.app.data.SearchFilters
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
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
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel

/* ------------------------------------------------------------------ S-29 */

/**
 * S-29 · Read the listings the current filters allow, as price pins; Update a
 * listing's saved state from its card; Create an alert for this area.
 *
 * Tapping a pin selects it; tapping the selected pin opens the listing.
 */
@Composable
fun S29SearchMap(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val all by vm.searchable.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val filters = LocalSearchFilters.current
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val found = filters.apply(all).sortedBy { it.distanceKm }
    val pins = found.take(MAX_PINS)
    val open: (GearItem) -> Unit = { vm.select(it); nav.to("s07") }
    val area = profile?.person?.area?.ifBlank { null } ?: "Jaffna"

    Box(Modifier.fillMaxSize().background(GearColor.Chalk100)) {
        MapCanvas(Modifier.fillMaxSize(), showRadius = false)

        // Each pin sits at its real distance from you, on a bearing that is
        // fixed per listing, so the same item is always in the same place.
        BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = 150.dp)) {
            val reach = (found.maxOfOrNull { it.distanceKm } ?: 1.0).coerceAtLeast(2.0)
            val radius = minOf(maxWidth, maxHeight) * 0.42f
            pins.forEachIndexed { i, item ->
                val bearing = ((item.id.hashCode() and 0x7fffffff) % 360) * Math.PI / 180
                val r = (item.distanceKm / reach).toFloat().coerceIn(0.12f, 1f)
                PricePin(
                    item, selected == i,
                    Modifier.align(Alignment.Center).offset(
                        x = radius * (r * kotlin.math.cos(bearing)).toFloat(),
                        y = radius * (r * kotlin.math.sin(bearing)).toFloat(),
                    ),
                ) { if (selected == i) open(item) else selected = i }
            }
            if (found.size > pins.size) {
                ClusterPin(found.size - pins.size, Modifier.align(Alignment.TopEnd).padding(top = 140.dp, end = Space.screen))
            }
        }

        Row(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = Space.screen, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            GearIconButton(Lucide.ChevronLeft, "Back", { nav.back() }, size = 52.dp)
            Box(Modifier.weight(1f)) {
                SearchBar(
                    placeholder = "Search this area",
                    value = filters.query.ifBlank { "${found.size} near $area" },
                    onClick = { nav.to("s05") },
                )
            }
            GearIconButton(
                Lucide.BellRing, "Alert me about new gear in this area",
                { vm.createAlert("ALERT", "New gear near $area · ${filters.describe()}") },
                size = 52.dp,
            )
        }

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .horizontalScroll(rememberSavedScrollState())
                .padding(horizontal = Space.screen, vertical = Space.m),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            if (found.isEmpty()) {
                GearCard(Modifier.width(300.dp)) {
                    Text("Nothing matches your filters here.", style = GearType.bodyM, color = GearText.secondary, modifier = Modifier.padding(Space.cardPadding))
                }
            }
            (pins.getOrNull(selected)?.let { listOf(it) + (found - it) } ?: found).take(8).forEach { item ->
                Box(Modifier.width(300.dp)) {
                    ListingCard(item, { open(item) }, layout = ListingLayout.LIST, onToggleSave = { vm.toggleSaved(item) })
                }
            }
        }
    }
}

private const val MAX_PINS = 12

@Composable
private fun PricePin(item: GearItem, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(GearShape.full)
            .background(if (selected) GearColor.Pine800 else GearColor.Chalk000)
            .border(Border.medium, GearColor.Pine800, GearShape.full)
            .clickable(role = Role.Button, onClickLabel = if (selected) "Open ${item.shortTitle}" else "Select ${item.shortTitle}", onClick = onClick)
            .padding(horizontal = if (selected) 14.dp else 11.dp, vertical = if (selected) 9.dp else 7.dp),
    ) {
        Text(
            lkr(item.pricePerDay),
            style = GearType.dataM,
            color = if (selected) GearText.onDark else GearText.primary,
        )
    }
}

@Composable
private fun ClusterPin(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(GearShape.full)
            .background(GearColor.Pine800)
            .border(3.dp, GearSurface.overImage, GearShape.full),
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", style = GearType.dataM, color = GearText.onDark)
    }
}

/* ------------------------------------------------------------------ S-30 */

/**
 * S-30 · Read the trip; Update its party and sports; Create the kit list from
 * the chosen sports; add or take out the small extras (Update); Delete a rental
 * from the kit, with Undo.
 */
@Composable
fun S30TripPlanner(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val tripState by vm.trip.collectAsStateWithLifecycle()
    var choosingPlace by rememberSaveable { mutableStateOf(false) }
    val trip = tripState ?: return
    val first = trip.rent.firstOrNull()?.listing

    GearScreen(
        contentPadding = 0.dp,
        footer = {
            StickyFooter(priceValue = lkr(trip.total), priceCaption = "${trip.itemCount} items · ${trip.days} days") {
                GearButton(
                    // Each rental is its own booking, so this opens the first one.
                    // "Book the kit" promised a single checkout for all of them.
                    "Start booking",
                    { if (first != null) { vm.select(first); nav.to("s07") } },
                    size = ButtonSize.M,
                    enabled = first != null,
                )
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            GearImage(placePhoto(trip.destination), trip.destination, Modifier.fillMaxSize())
            PhotoGradient(Modifier.fillMaxSize())
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Space.sm, vertical = Space.s),
            ) {
                GearIconButton(Lucide.ChevronLeft, "Back", { nav.back() }, tone = IconButtonTone.ON_IMAGE, iconSize = IconSize.l)
            }
            Text(
                "What are you doing?",
                style = GearType.displayL,
                color = GearText.onDark,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = Space.screen, bottom = 28.dp),
            )
        }

        Column(Modifier.padding(horizontal = Space.screen).offset(y = (-16).dp)) {
            GearCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.cardPadding)) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Change the destination") { choosingPlace = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) { TripField("Destination", trip.destination, Lucide.MapPin) }
                        GearIcon(Lucide.Pencil, null, size = IconSize.s, tint = GearColor.Chalk500)
                    }
                    GearDivider(Modifier.padding(vertical = Space.s))
                    TripField("Dates", trip.dates, Lucide.Calendar)
                    VGap(Space.s)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        GearIconButton(Lucide.ChevronLeft, "Start a day earlier", { vm.setTripDates(trip.start.minusDays(1), trip.days) }, size = 44.dp, iconSize = IconSize.s)
                        GearIconButton(Lucide.ChevronRight, "Start a day later", { vm.setTripDates(trip.start.plusDays(1), trip.days) }, size = 44.dp, iconSize = IconSize.s)
                        Spacer(Modifier.weight(1f))
                        Text("${trip.days} ${if (trip.days == 1) "day" else "days"}", style = GearType.dataM, color = GearText.primary)
                        Stepper(
                            value = "",
                            onMinus = { vm.setTripDates(trip.start, trip.days - 1) },
                            onPlus = { vm.setTripDates(trip.start, trip.days + 1) },
                            minusEnabled = trip.days > 1,
                            plusEnabled = trip.days < 21,
                        )
                    }
                    GearDivider(Modifier.padding(vertical = Space.s))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GearIcon(Lucide.Users, null, size = IconSize.m, tint = GearColor.Chalk500)
                        Spacer(Modifier.width(Space.s))
                        Text("Party", style = GearType.bodyS, color = GearText.tertiary, modifier = Modifier.width(96.dp))
                        Text(
                            "${trip.adults} ${if (trip.adults == 1) "adult" else "adults"}",
                            style = GearType.dataM,
                            color = GearText.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Stepper(
                            value = "",
                            onMinus = { vm.setTripAdults(trip.adults - 1) },
                            onPlus = { vm.setTripAdults(trip.adults + 1) },
                            minusEnabled = trip.adults > 1,
                            plusEnabled = trip.adults < 8,
                        )
                    }
                    VGap(Space.sm)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        com.gearloop.app.data.Demo.sports.map { it.first }.forEach { sport ->
                            Chip(sport, selected = sport in trip.sports, onClick = { vm.toggleTripSport(sport) })
                        }
                    }
                    VGap(Space.sm)
                    GearButton(
                        "Build my kit list",
                        { vm.buildKit(trip) },
                        Modifier.fillMaxWidth(),
                        size = ButtonSize.M,
                    )
                }
            }

            VGap(Space.l)
            if (trip.own.isNotEmpty()) {
                Eyebrow("You already own")
                VGap(Space.s)
            }
            trip.own.forEach { owned ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GearIcon(Lucide.Check, null, size = IconSize.m, tint = GearColor.Success500)
                    Spacer(Modifier.width(Space.s))
                    Text(owned.label, style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.weight(1f))
                    if (owned.note.contains("Swap", ignoreCase = true)) {
                        GearLink(owned.note, { nav.to("s19") })
                    } else {
                        Text(owned.note, style = GearType.bodyS, color = GearText.tertiary)
                    }
                }
            }

            VGap(Space.m)
            Eyebrow("Rent locally")
            VGap(Space.s)
            if (trip.rent.isEmpty()) {
                Text(
                    "Nothing to rent yet. Pick your sports and build the kit list.",
                    style = GearType.bodyS,
                    color = GearText.tertiary,
                )
                VGap(Space.s)
            }
            trip.rent.forEach { row ->
                val item = row.listing ?: return@forEach
                Box {
                    ListingCard(item, { vm.select(item); nav.to("s07") }, layout = ListingLayout.LIST, onToggleSave = { vm.toggleSaved(item) })
                    // Bottom-end, not top-end: the row already puts its save
                    // heart in the top-right corner, and this chip was sitting
                    // on top of it with the heart showing through underneath.
                    Text(
                        row.note.uppercase(),
                        style = GearType.labelXs,
                        color = GearColor.Pine900,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Space.s)
                            .clip(GearShape.xs)
                            .background(GearColor.HiVis500)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GearLink("Remove from kit", { vm.removeTripItem(row.id, item.shortTitle) }, color = GearText.tertiary)
                }
                VGap(Space.s)
            }

            VGap(Space.s)
            Eyebrow("Worth adding")
            VGap(Space.s)
            trip.extras.forEach { extra ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.sm)
                        .border(
                            if (extra.added) Border.medium else Border.hair,
                            if (extra.added) GearColor.Pine800 else GearSurface.border,
                            GearShape.sm,
                        )
                        .toggleable(value = extra.added, role = Role.Checkbox) {
                            vm.setTripExtraAdded(extra.id, extra.label, it)
                        }
                        .padding(Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(extra.label, style = GearType.bodyM, color = GearText.body, modifier = Modifier.weight(1f))
                    Text(lkr(extra.price), style = GearType.dataM, color = GearText.primary)
                    Spacer(Modifier.width(Space.s))
                    GearIcon(
                        if (extra.added) Lucide.CircleCheck else Lucide.Plus,
                        if (extra.added) "In the kit" else "Add",
                        size = IconSize.m,
                        tint = if (extra.added) GearColor.Success500 else GearText.primary,
                    )
                }
                VGap(Space.s)
            }

            VGap(Space.m)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(GearShape.md)
                    .background(GearColor.Pine800)
                    .padding(Space.cardPadding),
            ) {
                Text("${trip.itemCount} items · ${trip.days} days", style = GearType.bodyS, color = GearColor.Chalk300)
                VGap(6.dp)
                Text(lkr(trip.total), style = GearType.dataXl, color = GearText.onDark)
                VGap(6.dp)
                Text(
                    if (trip.bundleDiscount > 0) "Bundle discount −${lkr(trip.bundleDiscount)}"
                    else "Rent three or more for 10% off",
                    style = GearType.dataM,
                    color = GearColor.HiVis500,
                )
            }
            VGap(Space.section)
        }
    }

    if (choosingPlace) {
        DestinationDialog(trip.destination, onPick = { vm.setTripDestination(it); choosingPlace = false }, onDismiss = { choosingPlace = false })
    }
}

/** Where a trip can be planned to, and the photograph each one shows. */
private val places = listOf("Arugam Bay", "Casuarina Beach", "Jaffna", "Point Pedro", "Delft Island", "Trincomalee", "Knuckles Range", "Ella")

private fun placePhoto(destination: String): Int = when (destination) {
    "Casuarina Beach", "Point Pedro", "Delft Island", "Trincomalee" -> R.drawable.place_casuarina
    "Jaffna" -> R.drawable.place_jaffna
    else -> R.drawable.place_arugam_bay
}

@Composable
private fun DestinationDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf(current) }
    GearDialog(
        title = "Where are you going?",
        onDismiss = onDismiss,
        confirmLabel = "Use this",
        onConfirm = { onPick(typed) },
        confirmEnabled = typed.isNotBlank(),
    ) {
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            places.forEach { p -> Chip(p, selected = typed == p, onClick = { typed = p }) }
        }
        VGap(Space.s)
        GearTextField(typed, label = "Or type a place", onValueChange = { typed = it })
    }
}

@Composable
private fun TripField(label: String, value: String, icon: com.gearloop.app.ui.icons.LucideIcon) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GearIcon(icon, null, size = IconSize.m, tint = GearColor.Chalk500)
        Spacer(Modifier.width(Space.s))
        Text(label, style = GearType.bodyS, color = GearText.tertiary, modifier = Modifier.width(96.dp))
        Text(value, style = GearType.dataM, color = GearText.primary, modifier = Modifier.weight(1f))
    }
}

/* ------------------------------------------------------------------ S-39 */

/** One way out of an empty search, with the number of results it really buys. */
private data class Recovery(
    val title: String,
    val body: String?,
    val icon: com.gearloop.app.ui.icons.LucideIcon,
    val apply: (SearchFilters) -> Unit,
)

private val recoveries = listOf(
    Recovery("Any board length", null, Lucide.Ruler) { it.lengths.clear() },
    Recovery("Any condition grade", "Every grade is checked at handover.", Lucide.Tag) { it.grades.clear() },
    Recovery("Any price", null, Lucide.Banknote) { it.priceLower = 0f; it.priceUpper = 1f },
    Recovery("Include near fits", "Workable for your size, if not perfect.", Lucide.Ruler) { it.fitOnly = false },
    Recovery("Not only instant book", "Owners usually answer within a few hours.", Lucide.Zap) { it.instantOnly = false },
    Recovery("Pickup as well as delivery", null, Lucide.Truck) { it.delivery = false },
    Recovery("Any sport", null, Lucide.LayoutGrid) { it.sports.clear() },
    Recovery("Within 25 km", null, Lucide.MapPin) { it.distanceIndex = SearchFilters.DISTANCES.lastIndex },
)

/**
 * The relaxation ladder, made visual.
 *
 * S-39 · Read the saved alerts and posts; Create an alert or a "wanted" post
 * from the current filters; Delete either. Each recovery card is a real
 * loosening of the live filters, with the count it actually produces.
 */
@Composable
fun S39NoResults(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val all by vm.searchable.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val filters = LocalSearchFilters.current
    var posting by rememberSaveable { mutableStateOf(false) }
    var wanted by rememberSaveable { mutableStateOf("") }

    val now = filters.apply(all).size
    val options = recoveries
        .map { r -> r to filters.copy().also(r.apply).apply(all).size }
        .filter { (_, n) -> n > now }
        .take(3)

    GearScreen(
        appBar = { AppBar(title = "No matches", onBack = { nav.back() }) },
        tabBar = { MainTabBar(nav, MainTab.EXPLORE) },
    ) {
        EmptyState(
            icon = Lucide.Search,
            headline = when {
                now == 0 && filters.activeCount > 0 -> "Nothing matches all ${filters.activeCount} filters"
                now == 0 -> "Nothing listed nearby yet"
                else -> "Not quite what you wanted?"
            },
            body = filters.describe() + if (now > 0) " · $now ${if (now == 1) "result" else "results"} now" else "",
        )

        if (options.isNotEmpty()) {
            Eyebrow("Loosen one thing")
            VGap(Space.s)
            options.forEach { (r, n) ->
                RecoveryCard(r.title, r.body, n, r.icon) {
                    r.apply(filters)
                    nav.to("s05")
                }
                VGap(Space.s)
            }
        }

        VGap(Space.l)
        GearButton(
            "Post what you're looking for", { wanted = filters.describe(); posting = true }, Modifier.fillMaxWidth(),
            type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Send,
        )
        VGap(Space.s)
        GearButton(
            "Alert me when one appears", { vm.createAlert("ALERT", filters.describe()) }, Modifier.fillMaxWidth(),
            type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Bell,
        )

        if (alerts.isNotEmpty()) {
            VGap(Space.l)
            Eyebrow("Your alerts and posts")
            VGap(Space.s)
            alerts.forEach { alert ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GearIcon(
                        if (alert.kind == "ALERT") Lucide.Bell else Lucide.Send, null,
                        size = IconSize.m, tint = GearText.primary,
                    )
                    Spacer(Modifier.width(Space.s))
                    Column(Modifier.weight(1f)) {
                        Text(if (alert.kind == "ALERT") "Alert" else "Wanted post", style = GearType.labelM, color = GearText.primary)
                        Text(alert.summary, style = GearType.bodyS, color = GearText.secondary)
                    }
                    GearIconButton(
                        Lucide.Trash2, "Remove ${if (alert.kind == "ALERT") "alert" else "post"}",
                        { vm.deleteAlert(alert.id, alert.kind, alert.summary) },
                        size = 44.dp,
                    )
                }
                GearDivider()
            }
        }
        VGap(Space.section)
    }

    if (posting) {
        GearDialog(
            title = "What are you looking for?",
            onDismiss = { posting = false },
            confirmLabel = "Post it",
            onConfirm = { vm.createAlert("WANTED", wanted.trim()); posting = false },
            confirmEnabled = wanted.isNotBlank(),
        ) {
            Text(
                "Owners near you see this and can offer gear that isn't listed yet.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.s)
            GearTextField(wanted, label = "Your post", onValueChange = { wanted = it }, singleLine = false)
        }
    }
}

@Composable
private fun RecoveryCard(
    title: String,
    body: String?,
    results: Int,
    icon: com.gearloop.app.ui.icons.LucideIcon,
    onClick: () -> Unit,
) {
    GearCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(Space.cardPadding), verticalAlignment = Alignment.CenterVertically) {
            GearIcon(icon, null, size = IconSize.l, tint = GearText.primary)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = GearType.headingS, color = GearText.primary)
                if (body != null) {
                    VGap(3.dp)
                    Text(body, style = GearType.bodyS, color = GearText.secondary)
                }
            }
            Spacer(Modifier.width(Space.s))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$results", style = GearType.dataL, color = GearText.primary)
                Text("RESULTS", style = GearType.labelXs, color = GearText.tertiary)
            }
        }
    }
}

/* ------------------------------------------------------------------ S-42 */

/**
 * S-42 · Read the listing's reviews and its owner's histogram; Create a review
 * when the renter has a finished rental of it to review; Delete their own.
 */
@Composable
fun S42ListingReviews(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val allReviews by vm.reviews.collectAsStateWithLifecycle()
    val past by vm.myBookings.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var writing by rememberSaveable { mutableStateOf(false) }
    var stars by rememberSaveable { mutableIntStateOf(5) }
    var body by rememberSaveable { mutableStateOf("") }
    val item = selected ?: return
    val owner = people[item.owner.handle] ?: item.owner

    // Counts, not percentages. The bars used to be fractions typed by hand and
    // the counts beside them were those fractions multiplied back out and
    // truncated, so 0.86 · 0.11 · 0.03 of 37 reviews printed as 31 + 4 + 1 = 36.
    val distribution = vm.distributionOf(owner.handle)
    // Of this listing's owner only: an owner's review of a renter is filed
    // against the same listing (S-24 "Write it"), and it used to land here (D-27).
    val reviews = allReviews.filter { it.listingId == item.id && it.subject.handle == item.owner.handle }
    val shown = when (filter) {
        1 -> reviews.filter { it.entry.stars == 5 }
        2 -> reviews.filter { it.entry.photos.isNotEmpty() }
        3 -> reviews.take(3)
        else -> reviews
    }
    // A finished rental of this listing that has not been reviewed yet.
    val toReview = past.firstOrNull {
        it.item.id == item.id && it.status == BookingStatus.COMPLETED && it.rating == null
    }

    GearScreen(
        appBar = { AppBar(title = "Reviews", subtitle = item.shortTitle.uppercase(), onBack = { nav.back() }) },
    ) {
        VGap(Space.m)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (owner.reviews > 0) "★ ${owner.ratingText}" else "★ –", style = GearType.dataXl, color = GearText.primary)
            Spacer(Modifier.width(Space.s))
            Text(
                "${owner.reviews} reviews of ${owner.name.substringBefore(' ')}",
                style = GearType.bodyM,
                color = GearText.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        if (toReview != null) {
            VGap(Space.m)
            Banner(
                title = "How was it?",
                text = "You rented this ${toReview.dates}.",
                kind = BannerKind.INFO,
                action = "Write a review",
                onAction = { writing = true },
            )
        }

        if (distribution != null) {
            VGap(Space.m)
            (5 downTo 1).forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$s★", style = GearType.dataM, color = GearText.tertiary, modifier = Modifier.width(32.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(GearShape.full)
                            .background(GearColor.Chalk200),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(distribution.fractionAt(s))
                                .height(6.dp)
                                .clip(GearShape.full)
                                .background(GearColor.Pine800)
                        )
                    }
                    Spacer(Modifier.width(Space.s))
                    Text(
                        "${distribution.at(s)}",
                        style = GearType.dataS,
                        color = GearText.tertiary,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }

        }

        VGap(Space.l)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf("All", "5★", "With photos", "Recent").forEachIndexed { i, label ->
                Chip(label, selected = filter == i, onClick = { filter = i })
            }
        }

        VGap(Space.m)
        if (shown.isEmpty()) {
            Text("No reviews to show here yet.", style = GearType.bodyS, color = GearText.tertiary)
        }
        // The reviews that exist, once each. The list used to be padded out by
        // appending the first two again, so Ayesha's five-star review appeared
        // twice on the same screen, word for word.
        shown.forEach { review ->
            ReviewCard(review.entry)
            if (review.authorHandle == vm.me) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GearLink("Delete my review", { vm.deleteReview(review.id) }, color = GearText.tertiary)
                }
            }
            VGap(Space.sm)
        }
        VGap(Space.section)
    }

    if (writing && toReview != null) {
        GearDialog(
            title = "Rate ${owner.name.substringBefore(' ')}",
            onDismiss = { writing = false },
            confirmLabel = "Post review",
            onConfirm = {
                vm.addReview(item.id, stars, body, toReview.ref)
                writing = false
                body = ""
            },
            confirmEnabled = body.trim().length >= 10,
        ) {
            StarPicker(stars, { stars = it })
            VGap(Space.s)
            GearTextField(
                body,
                label = "Your review",
                placeholder = "What was it like? At least a sentence.",
                onValueChange = { body = it },
                singleLine = false,
            )
        }
    }
}
