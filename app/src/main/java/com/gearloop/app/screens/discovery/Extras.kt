package com.gearloop.app.screens.discovery

import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gearloop.app.R
import com.gearloop.app.data.Demo
import com.gearloop.app.data.GearItem
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

/* ------------------------------------------------------------------ S-29 */

@Composable
fun S29SearchMap(nav: Nav) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val pins = listOf(Demo.torq, Demo.nspSup, Demo.wetsuit, Demo.kayak)

    Box(Modifier.fillMaxSize().background(GearColor.Chalk100)) {
        MapCanvas(Modifier.fillMaxSize(), showRadius = false)

        // Price pins. Selected pin inverts and scales.
        PricePin(pins[0], selected == 0, Modifier.align(Alignment.Center).offset(x = (-30).dp, y = (-40).dp)) { selected = 0 }
        PricePin(pins[1], selected == 1, Modifier.align(Alignment.Center).offset(x = 70.dp, y = 20.dp)) { selected = 1 }
        PricePin(pins[2], selected == 2, Modifier.align(Alignment.Center).offset(x = (-90).dp, y = 60.dp)) { selected = 2 }
        ClusterPin(7, Modifier.align(Alignment.Center).offset(x = 40.dp, y = (-120).dp))

        Row(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = Space.screen, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Box(Modifier.weight(1f)) {
                SearchBar(
                    placeholder = "Search this area",
                    value = "Gear · ${Demo.nirmal.area}",
                    onClick = { nav.back() },
                )
            }
            GearIconButton(Lucide.Pin, "Draw area", { }, size = 52.dp)
        }

        GearIconButton(
            icon = Lucide.List,
            contentDescription = "List view",
            onClick = { nav.back() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 76.dp, end = Space.screen),
            size = 48.dp,
        )

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .horizontalScroll(rememberSavedScrollState())
                .padding(horizontal = Space.screen, vertical = Space.m),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            pins.forEach { item ->
                Box(Modifier.width(300.dp)) {
                    ListingCard(item, { nav.to("s07") }, layout = ListingLayout.LIST)
                }
            }
        }
    }
}

@Composable
private fun PricePin(item: GearItem, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(GearShape.full)
            .background(if (selected) GearColor.Pine800 else GearColor.Chalk000)
            .border(Border.medium, GearColor.Pine800, GearShape.full)
            .clickable(role = Role.Button, onClick = onClick)
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

@Composable
fun S30TripPlanner(nav: Nav) {
    GearScreen(
        contentPadding = 0.dp,
        footer = {
            StickyFooter(priceValue = lkr(Demo.TRIP_TOTAL), priceCaption = "7 items · 4 days") {
                GearButton("Book the kit", { nav.to("s11") }, size = ButtonSize.M)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            GearImage(R.drawable.place_arugam_bay, "Arugam Bay", Modifier.fillMaxSize())
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
                    TripField("Destination", Demo.TRIP_DESTINATION, Lucide.MapPin)
                    GearDivider(Modifier.padding(vertical = Space.s))
                    TripField("Dates", Demo.TRIP_DATES, Lucide.Calendar)
                    GearDivider(Modifier.padding(vertical = Space.s))
                    TripField("Party", Demo.TRIP_PARTY, Lucide.Users)
                    VGap(Space.sm)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        Chip("Surfing", selected = true)
                        Chip("Camping", selected = true)
                        Chip("Trekking")
                    }
                    VGap(Space.sm)
                    GearButton("Build my kit list", { }, Modifier.fillMaxWidth(), size = ButtonSize.M)
                }
            }

            VGap(Space.l)
            Eyebrow("You already own")
            VGap(Space.s)
            Demo.tripAlreadyOwn.forEach { (name, action) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GearIcon(Lucide.Check, null, size = IconSize.m, tint = GearColor.Success500)
                    Spacer(Modifier.width(Space.s))
                    Text(name, style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.weight(1f))
                    GearLink(action, { })
                }
            }

            VGap(Space.m)
            Eyebrow("Rent locally")
            VGap(Space.s)
            Demo.tripRentLocally.forEach { (item, who) ->
                Box {
                    ListingCard(item, { nav.to("s07") }, layout = ListingLayout.LIST)
                    // Bottom-end, not top-end: the row already puts its save
                    // heart in the top-right corner, and this chip was sitting
                    // on top of it with the heart showing through underneath.
                    Text(
                        who.uppercase(),
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
                VGap(Space.listGap)
            }

            VGap(Space.s)
            Eyebrow("Worth adding")
            VGap(Space.s)
            Demo.tripWorthAdding.forEach { (name, price) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.sm)
                        .border(Border.hair, GearSurface.border, GearShape.sm)
                        .padding(Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, style = GearType.bodyM, color = GearText.body, modifier = Modifier.weight(1f))
                    Text(price, style = GearType.dataM, color = GearText.primary)
                    Spacer(Modifier.width(Space.s))
                    GearIcon(Lucide.Plus, "Add", size = IconSize.m, tint = GearColor.Pine800)
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
                Text("7 items · 4 days", style = GearType.bodyS, color = GearColor.Chalk300)
                VGap(6.dp)
                Text(lkr(Demo.TRIP_TOTAL), style = GearType.dataXl, color = GearText.onDark)
                VGap(6.dp)
                Text(
                    "Bundle discount −${lkr(Demo.TRIP_DISCOUNT)}",
                    style = GearType.dataM,
                    color = GearColor.HiVis500,
                )
            }
            VGap(Space.section)
        }
    }
}

@Composable
private fun TripField(label: String, value: String, icon: com.gearloop.app.ui.icons.LucideIcon) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GearIcon(icon, null, size = IconSize.m, tint = GearColor.Chalk500)
        Spacer(Modifier.width(Space.s))
        Text(label, style = GearType.bodyS, color = GearText.tertiary, modifier = Modifier.width(96.dp))
        Text(value, style = GearType.dataM, color = GearText.primary, modifier = Modifier.weight(1f))
        GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
    }
}

/* ------------------------------------------------------------------ S-39 */

/**
 * The relaxation ladder, made visual.
 *
 * A search that finds nothing is not a dead end — each card loosens exactly one
 * constraint and says how many results that buys.
 */
@Composable
fun S39NoResults(nav: Nav) {
    GearScreen(
        appBar = { AppBar(title = "No matches", onBack = { nav.back() }) },
        tabBar = { MainTabBar(nav, MainTab.EXPLORE) },
    ) {
        EmptyState(
            icon = Lucide.Search,
            headline = "Nothing matches all five filters",
            body = "A 6'6\" Grade A board under LKR 2,000, within 2 km, free 8–10 Aug.",
        )

        Eyebrow("Loosen one thing")
        VGap(Space.s)
        RecoveryCard("Available 12 – 14 Aug instead", "Same board, same price, four days later.", 3, Lucide.CalendarDays) { nav.to("s05") }
        VGap(Space.s)
        RecoveryCard("Within 25 km", "Includes Gurunagar and Point Pedro.", 7, Lucide.MapPin) { nav.to("s05") }
        VGap(Space.s)
        RecoveryCard("Try a funboard", "Similar float, more forgiving at your level.", 5, Lucide.Waves) { nav.to("s05") }

        VGap(Space.l)
        GearButton(
            "Post what you're looking for", { }, Modifier.fillMaxWidth(),
            type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Send,
        )
        VGap(Space.s)
        GearButton(
            "Alert me when one appears", { }, Modifier.fillMaxWidth(),
            type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Bell,
        )
        VGap(Space.section)
    }
}

@Composable
private fun RecoveryCard(
    title: String,
    body: String,
    results: Int,
    icon: com.gearloop.app.ui.icons.LucideIcon,
    onClick: () -> Unit,
) {
    GearCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(Space.cardPadding), verticalAlignment = Alignment.CenterVertically) {
            GearIcon(icon, null, size = IconSize.l, tint = GearColor.Pine800)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = GearType.headingS, color = GearText.primary)
                VGap(3.dp)
                Text(body, style = GearType.bodyS, color = GearText.secondary)
            }
            Spacer(Modifier.width(Space.s))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$results", style = GearType.dataL, color = GearColor.Pine900)
                Text("RESULTS", style = GearType.labelXs, color = GearText.tertiary)
            }
        }
    }
}

/* ------------------------------------------------------------------ S-42 */

@Composable
fun S42ListingReviews(nav: Nav) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    // Counts, not percentages. The bars used to be fractions typed by hand and
    // the counts beside them were those fractions multiplied back out and
    // truncated, so 0.86 · 0.11 · 0.03 of 37 reviews printed as 31 + 4 + 1 = 36.
    val distribution = Demo.starDistribution

    GearScreen(
        appBar = { AppBar(title = "Reviews", onBack = { nav.back() }) },
    ) {
        VGap(Space.m)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("★ ${Demo.kavitha.ratingText}", style = GearType.dataXl, color = GearText.primary)
            Spacer(Modifier.width(Space.s))
            Text(
                "${Demo.kavitha.reviews} reviews",
                style = GearType.bodyM,
                color = GearText.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        VGap(Space.m)
        (5 downTo 1).forEach { stars ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$stars★", style = GearType.dataM, color = GearText.tertiary, modifier = Modifier.width(32.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(GearShape.full)
                        .background(GearColor.Chalk200),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(distribution.fractionAt(stars))
                            .height(6.dp)
                            .clip(GearShape.full)
                            .background(GearColor.Pine800)
                    )
                }
                Spacer(Modifier.width(Space.s))
                Text(
                    "${distribution.at(stars)}",
                    style = GearType.dataS,
                    color = GearText.tertiary,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.End,
                )
            }
        }

        VGap(Space.m)
        Eyebrow("By category")
        VGap(Space.s)
        Demo.subRatings.forEach { (label, value) ->
            RatingBar(label, value / 5f, Modifier.padding(vertical = 4.dp), value = String.format("%.2f", value))
        }

        VGap(Space.l)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf("All", "5★", "With photos", "Recent").forEachIndexed { i, label ->
                Chip(label, selected = filter == i, onClick = { filter = i })
            }
        }

        VGap(Space.m)
        // The reviews that exist, once each. The list used to be padded out by
        // appending the first two again, so Ayesha's five-star review appeared
        // twice on the same screen, word for word.
        Demo.torqReviews.forEach {
            ReviewCard(it)
            VGap(Space.sm)
        }
        VGap(Space.section)
    }
}
