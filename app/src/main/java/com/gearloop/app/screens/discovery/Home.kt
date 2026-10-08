package com.gearloop.app.screens.discovery

import com.gearloop.app.data.grouped
import com.gearloop.app.data.SearchFilters
import com.gearloop.app.data.LocalSearchFilters
import androidx.compose.foundation.layout.FlowRow
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Fit
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel
import com.gearloop.app.ui.theme.Metrics
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/* ------------------------------------------------------------------ S-04 */

/**
 * S-04 · Read the rails, the profile and the unread count; Update a listing's
 * saved state from any card's heart.
 */
@Composable
fun S04Home(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val rails by vm.rails.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val unread by vm.unread.collectAsStateWithLifecycle()
    val matches by vm.matches.collectAsStateWithLifecycle()
    val filters = LocalSearchFilters.current
    // The home chips write to the same model the sheet and the results use, so
    // "Instant book" means one thing in the app rather than three.
    var activeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val open: (GearItem) -> Unit = { vm.select(it); nav.to("s07") }

    fun applyHomeChip(label: String?) {
        activeFilter = label
        filters.instantOnly = label == "Instant book"
        filters.fitOnly = label == "Fits me"
        filters.priceUpper = if (label == "Under LKR 2,000") 0.34f else 1f
    }

    // A chip is lit only while its filter is really on. The three above live in
    // the shared model, so clearing one on S-05 or S-06 puts its chip out here.
    fun isOn(label: String) = activeFilter == label && when (label) {
        "Instant book" -> filters.instantOnly
        "Fits me" -> filters.fitOnly
        "Under LKR 2,000" -> filters.priceUpper < 1f
        else -> true
    }

    // "Available today" and "Swap" have no field in the shared filter: they
    // narrow these rails only. Both used to light up and change nothing (D-21).
    val swappable = matches.map { it.offer.get.id }.toSet()
    fun homeOnly(items: List<GearItem>): List<GearItem> = when {
        isOn("Available today") -> items.filter { it.available }
        isOn("Swap") -> items.filter { it.id in swappable }
        else -> items
    }

    GearScreen(
        appBar = { HomeAppBar(nav, profile?.person?.area?.substringAfterLast(", ") ?: "Jaffna", unread) },
        tabBar = { MainTabBar(nav, MainTab.EXPLORE) },
        contentPadding = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = Space.screen)) {
            SearchBar(
                placeholder = "Search gear, sports, brands",
                onClick = { nav.to("s05") },
                onFilter = { nav.to("s06") },
            )
        }
        VGap(Space.sm)

        // Filter chips — horizontal scroll, first row of the page.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Space.screen),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            items(Demo.homeFilters, key = { it }) { label ->
                Chip(
                    label = label,
                    selected = isOn(label),
                    onClick = { applyHomeChip(if (isOn(label)) null else label) },
                    icon = if (label == "Instant book") Lucide.Zap else null,
                )
            }
        }

        VGap(Space.l)
        Rail(
            eyebrow = "This weekend near you",
            items = homeOnly(filters.apply(rails["weekend"].orEmpty())),
            onOpen = open,
            onSave = vm::toggleSaved,
            onSeeAll = { nav.to("s05") },
        )

        VGap(Space.l)
        Rail(
            eyebrow = "Fits you",
            // Ranked by the live verdict, so changing the profile on S-02 reorders
            // this rail — and anything now the wrong size drops out of it.
            items = homeOnly(filters.apply(rails["fits"].orEmpty()))
                .filter { it.fit in setOf(Fit.PERFECT, Fit.GOOD, Fit.WORKABLE) }
                .sortedBy { it.fit.ordinal },
            onOpen = open,
            onSave = vm::toggleSaved,
            onSeeAll = { nav.to("s05") },
            leading = {
                val p = profile
                if (p?.heightCm != null && p.weightKg != null) {
                    FitStamp(Fit.PERFECT, label = "${p.heightCm} CM · ${p.weightKg} KG")
                } else {
                    FitStamp(Fit.UNKNOWN)
                }
            },
        )

        VGap(Space.l)
        // This is how swap gets discovered from the rental side of the app. The
        // count is the live one, so hiding a match on S-19 changes it here.
        val people = matches.map { it.offer.counterparty.handle }.distinct().size
        if (people > 0) {
            Box(Modifier.padding(horizontal = Space.screen)) {
                Banner(
                    title = if (people == 1) "1 person wants to swap for your Burton Custom"
                    else "$people people want to swap for your Burton Custom",
                    text = if (matches.any { it.offer.threeWay }) "One of the trades is a three-way loop."
                    else "Each one is a straight two-way trade.",
                    kind = BannerKind.INFO,
                    action = "View matches",
                    onAction = { nav.to("s19") },
                )
            }
        }

        VGap(Space.l)
        Rail(
            eyebrow = "New in surfing",
            items = homeOnly(filters.apply(rails["surf"].orEmpty())),
            onOpen = open,
            onSave = vm::toggleSaved,
            onSeeAll = { nav.to("s05") },
        )

        VGap(Space.l)
        Box(Modifier.padding(horizontal = Space.screen)) {
            TripPlannerPromo { nav.to("s30") }
        }
        VGap(Space.section)
    }
}

@Composable
private fun HomeAppBar(nav: Nav, area: String, unread: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(GearColor.Chalk100)
            .statusBarsPadding()
            .padding(horizontal = Space.screen, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .clip(GearShape.full)
                .border(Border.hair, GearSurface.border, GearShape.full)
                .background(GearSurface.card)
                .clickable(role = Role.Button) { nav.to("s29") }
                .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIcon(Lucide.MapPin, null, size = IconSize.s, tint = GearColor.Pine800)
            Spacer(Modifier.width(6.dp))
            Text(area, style = GearType.labelM, color = GearText.primary)
            Spacer(Modifier.width(2.dp))
            GearIcon(Lucide.ChevronDown, null, size = IconSize.s, tint = GearColor.Chalk500)
        }
        Spacer(Modifier.weight(1f))
        GearIconButton(
            icon = Lucide.Bell,
            contentDescription = if (unread > 0) "Notifications, $unread unread" else "Notifications",
            onClick = { nav.to("s38") },
            badge = if (unread > 0) "$unread" else null,
        )
    }
}

@Composable
private fun Rail(
    eyebrow: String,
    items: List<GearItem>,
    onOpen: (GearItem) -> Unit,
    onSave: (GearItem) -> Unit,
    onSeeAll: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Column {
        Box(Modifier.padding(horizontal = Space.screen)) {
            SectionHeader(
                eyebrow = eyebrow,
                leading = leading,
                trailing = { GearLink("See all", onSeeAll) },
            )
        }
        VGap(Space.sm)
        if (items.isEmpty()) {
            // The chips really filter, so a rail can genuinely empty out. Saying
            // so is better than leaving a heading standing over nothing.
            Box(Modifier.padding(horizontal = Space.screen)) {
                Text(
                    "Nothing in this rail matches the filters above.",
                    style = GearType.bodyS,
                    color = GearText.tertiary,
                )
            }
        } else {
            // Lazy: a rail is four photographs wide and only two fit on screen.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Space.screen),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                items(items, key = { it.id }) { item ->
                    ListingCard(
                        item,
                        onClick = { onOpen(item) },
                        layout = ListingLayout.GRID,
                        onToggleSave = { onSave(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TripPlannerPromo(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(GearShape.md)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        GearImage(
            com.gearloop.app.R.drawable.place_arugam_bay,
            "Arugam Bay",
            Modifier.fillMaxSize(),
        )
        PhotoGradient(Modifier.fillMaxSize(), strength = 0.85f)
        Column(Modifier.align(Alignment.BottomStart).padding(Space.m)) {
            Text("PLAN A TRIP", style = GearType.labelXs, color = GearColor.HiVis500)
            Spacer(Modifier.height(6.dp))
            Text(
                "Going somewhere?\nGet the whole kit list.",
                style = GearType.headingL,
                color = GearText.onDark,
            )
            Spacer(Modifier.height(10.dp))
            GearButton("Build my kit", onClick, type = ButtonType.PRIMARY, size = ButtonSize.S)
        }
    }
}

/* ------------------------------------------------------------------ S-05 */

/** How the results are ordered — the "Best match" control, which now works. */
private val sortModes = listOf("Best match", "Lowest price", "Nearest")

/**
 * S-05 · Read the published listings through the shared filter; Update a
 * listing's saved state from its heart.
 */
@Composable
fun S05SearchResults(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val all by vm.searchable.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val filters = LocalSearchFilters.current
    var sort by rememberSaveable { mutableIntStateOf(0) }
    val filtered = filters.apply(all)
    val results = when (sort) {
        1 -> filtered.sortedBy { it.pricePerDay }
        2 -> filtered.sortedBy { it.distanceKm }
        // Best match: free today first, then the closest fit, then distance.
        else -> filtered.sortedWith(compareBy({ !it.available }, { it.fit.ordinal }, { it.distanceKm }))
    }
    val open: (GearItem) -> Unit = { vm.select(it); nav.to("s07") }

    // Lazy: every result carries a photograph, so composing the whole list up
    // front would decode all of them before the first one is on screen.
    GearLazyScreen(
        appBar = {
            AppBar(
                // The list is every listing in Jaffna, filtered — a bike, a
                // tent and a cricket bat among them. Heading it "Surfboards"
                // described a result set the screen has never shown.
                title = "Gear in ${profile?.person?.area ?: Demo.nirmal.area}",
                onBack = { nav.back() },
                actions = {
                    GearIconButton(Lucide.Map, "Map view", { nav.to("s29") }, size = 40.dp)
                },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.EXPLORE) },
        contentPadding = 0.dp,
    ) {
        item(key = "header") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(GearColor.Chalk100)
                    .padding(horizontal = Space.screen, vertical = Space.s),
            ) {
                SearchBar(
                    placeholder = "Search gear, sports, item codes",
                    value = filters.query,
                    onValueChange = { filters.query = it },
                    onFilter = { nav.to("s06") },
                    filterCount = filters.activeCount,
                )
                VGap(Space.sm)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (results.size == 1) "1 result" else "${results.size} results",
                        style = GearType.dataM,
                        color = GearText.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    // Was a `clickable {}` that did nothing and had no role. Each
                    // tap now moves to the next ordering.
                    Row(
                        Modifier
                            .heightIn(min = Metrics.tapTarget)
                            .clickable(role = Role.Button, onClickLabel = "Change the order") {
                                sort = (sort + 1) % sortModes.size
                            }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(sortModes[sort], style = GearType.labelM, color = GearText.primary)
                        GearIcon(Lucide.ArrowUpDown, null, size = IconSize.s, tint = GearColor.Chalk500)
                    }
                }
                // The chips are the filters, not a picture of them: removing one
                // re-runs the search underneath.
                if (filters.activeCount > 0) {
                    VGap(Space.s)
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.s),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        ActiveFilterChips(filters)
                    }
                }
            }
            GearDivider()
            VGap(Space.sm)
        }

        itemsIndexed(results, key = { _, item -> item.id }) { i, item ->
            Column(Modifier.padding(horizontal = Space.screen)) {
                // Every fourth position gets the wide variant, to break the rhythm.
                ListingCard(
                    item,
                    { open(item) },
                    layout = if (i > 0 && i % 4 == 3) ListingLayout.WIDE else ListingLayout.LIST,
                    onToggleSave = { vm.toggleSaved(item) },
                )
                VGap(Space.listGap)
            }
        }

        if (results.isEmpty()) {
            item(key = "empty") {
                // Reached by filtering, not by tapping a button that admits it is
                // a mock-up. The action names the filter it will loosen.
                Column(Modifier.padding(horizontal = Space.screen)) {
                    EmptyState(
                        icon = Lucide.Search,
                        headline = "Nothing matches all of that",
                        body = "Loosen one filter and there is gear here. " +
                            "${all.size} items are listed in total.",
                        actionLabel = "Loosen the tightest filter",
                        onAction = filters::loosen,
                        secondaryLabel = "Clear all filters",
                        onSecondary = filters::reset,
                    )
                }
            }
        }

        item(key = "footer") {
            Column(Modifier.padding(horizontal = Space.screen)) {
                VGap(Space.s)
                GearButton(
                    "Nothing quite right?",
                    { nav.to("s39") },
                    Modifier.fillMaxWidth(),
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.M,
                    icon = Lucide.Search,
                )
                VGap(Space.section)
            }
        }
    }
}

/**
 * The filters currently doing something, each removable.
 *
 * Reads back from the model rather than from a parallel list, so a chip can
 * never disagree with the results underneath it.
 */
@Composable
private fun ActiveFilterChips(filters: SearchFilters) {
    if (filters.query.isNotBlank()) {
        RemovableChip("\"${filters.query}\"") { filters.query = "" }
    }
    if (filters.fitOnly) {
        RemovableChip("Fits me") { filters.fitOnly = false }
    }
    if (filters.instantOnly) {
        RemovableChip("Instant book") { filters.instantOnly = false }
    }
    if (filters.delivery) {
        RemovableChip("Delivery") { filters.delivery = false }
    }
    if (filters.distanceIndex != SearchFilters.DEFAULT_DISTANCE) {
        RemovableChip("Within ${filters.maxDistanceKm} km") {
            filters.distanceIndex = SearchFilters.DEFAULT_DISTANCE
        }
    }
    if (filters.priceLower > 0f || filters.priceUpper < 1f) {
        RemovableChip(
            "${lkr(filters.priceAt(filters.priceLower))}–${grouped(filters.priceAt(filters.priceUpper))}"
        ) {
            filters.priceLower = 0f
            filters.priceUpper = 1f
        }
    }
    filters.lengths.toList().forEach { length ->
        RemovableChip(length) { filters.lengths.remove(length) }
    }
    filters.grades.toList().forEach { grade ->
        RemovableChip(grade) { filters.grades.remove(grade) }
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    Chip(label = label, selected = true, onClick = onRemove, trailingIcon = Lucide.X)
}
