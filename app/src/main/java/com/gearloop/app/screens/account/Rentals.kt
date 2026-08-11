package com.gearloop.app.screens.account

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.lkr
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
/* ------------------------------------------------------------------ S-31 */

@Composable
fun S31MyRentals(nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    GearScreen(
        appBar = { AppBar(title = "Rentals") },
        tabBar = { MainTabBar(nav, MainTab.RENTALS) },
    ) {
        VGap(Space.s)
        SegmentedControl(listOf("Active", "Upcoming", "Past"), tab) { tab = it }
        VGap(Space.m)

        when (tab) {
            0 -> {
                // The card's primary action changes with booking state — that is
                // the systems-thinking bit, so each state gets a different verb.
                GearCard(Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.padding(Space.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                            GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(72.dp), GearShape.sm)
                            Spacer(Modifier.width(Space.sm))
                            Column(Modifier.weight(1f)) {
                                StatusBadge("In progress", GearColor.Success500)
                                VGap(6.dp)
                                Text(Demo.torq.shortTitle, style = GearType.headingS, color = GearText.primary)
                                VGap(4.dp)
                                Text(Demo.BOOKING_REF, style = GearType.dataS, color = GearText.tertiary)
                            }
                        }
                        Column(Modifier.padding(horizontal = Space.cardPadding)) {
                            Countdown("2d 04h 12m", "Until return")
                            VGap(Space.s)
                            Text("Return by Mon 10 Aug, 17:00 · Nallur", style = GearType.dataM, color = GearText.secondary)
                        }
                        Row(Modifier.padding(Space.cardPadding)) {
                            GearButton("Start return", { nav.to("s18") }, Modifier.fillMaxWidth(), size = ButtonSize.M)
                        }
                    }
                }
                VGap(Space.sm)
                GearButton(
                    "Open the rental", { nav.to("s17") }, Modifier.fillMaxWidth(),
                    type = ButtonType.GHOST, size = ButtonSize.M,
                )
            }
            1 -> {
                BookingCard(
                    Demo.nspSup, "12 – 14 Aug", "Pickup in 3 days", GearColor.Marine500,
                    "View pickup details", { nav.to("s13") }, reference = "GL-6B1P2",
                )
                VGap(Space.listGap)
                BookingCard(
                    Demo.quechuaTent, "22 – 25 Aug", "Confirmed", GearColor.Success500,
                    "View booking", { nav.to("s13") }, reference = "GL-9R2K4",
                )
            }
            else -> {
                BookingCard(
                    Demo.trekMarlin, "18 – 20 Jul", "Rated ★5", GearColor.Chalk600,
                    "Book again", { nav.to("s07") }, reference = "GL-5X8T1",
                )
                VGap(Space.listGap)
                BookingCard(
                    Demo.ssBat, "2 – 3 Jul", "Rate it", GearColor.Warning700,
                    "Leave a review", { nav.to("s42") }, reference = "GL-4M0N7",
                )
                VGap(Space.listGap)
                BookingCard(
                    Demo.kayak, "14 – 16 Jun", "Rated ★4", GearColor.Chalk600,
                    "Book again", { nav.to("s07") }, reference = "GL-2H6Q3",
                )
            }
        }
        VGap(Space.section)
    }
}

/* ------------------------------------------------------------------ S-36 */

@Composable
fun S36GearCloset(nav: Nav) {
    val owned = listOf(
        Demo.burton to true,
        Demo.helmet to true,
        Demo.kayak to false,
        Demo.wetsuit to true,
        Demo.ssBat to false,
        Demo.climbingRack to false,
    )

    GearScreen(
        appBar = {
            AppBar(
                title = "Gear closet",
                onBack = { nav.back() },
                actions = { GearIconButton(Lucide.Plus, "Add gear", { nav.to("s25") }, size = 40.dp) },
            )
        },
    ) {
        VGap(Space.s)
        Text(
            "${owned.count { it.second }} listed · ${owned.count { !it.second }} sitting idle",
            style = GearType.dataM,
            color = GearText.secondary,
        )

        VGap(Space.m)
        InsightCard(
            headline = "Your kayak hasn't moved in 94 days",
            body = "14 people searched for kayaks near you last week. It could earn " +
                "${lkr(4_500)} a weekend.",
            action = "List it",
            onAction = { nav.to("s26") },
        )

        VGap(Space.m)
        owned.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                pair.forEach { (item, listed) ->
                    Box(Modifier.weight(1f)) {
                        Column(Modifier.alpha(if (listed) 1f else 0.6f)) {
                            GearImage(
                                item.hero, item.title,
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
                                tone = if (listed) TagTone.HI_VIS else TagTone.MUTED,
                            )
                        }
                        if (!listed) {
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
}

