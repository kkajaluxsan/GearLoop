package com.gearloop.app.screens.handover

import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.icons.LucideIcon
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space

/* ------------------------------------------------------------------ S-16 */

@Composable
fun S16SignRelease(nav: Nav) {
    val pulse = rememberInfiniteTransition(label = "await")
    val alpha by pulse.animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "a",
    )

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton("Sign and release board", { nav.to("s17") }, Modifier.fillMaxWidth())
            }
        },
    ) {
        VGap(Space.s)
        Text(
            "Both of you agree this is the condition",
            style = GearType.headingXl,
            color = GearText.primary,
        )
        VGap(Space.m)

        val photos = Demo.captureAngles.mapNotNull { it.photo } +
            Demo.torqConditionPhotos + listOf(Demo.torq.photos[3])
        photos.take(9).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                row.forEach {
                    GearImage(it, "Handover photo", Modifier.weight(1f).height(104.dp), GearShape.sm)
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("What's going with it")
        VGap(Space.s)
        Demo.torqIncluded.forEach {
            IconRow(Lucide.Check, it, Modifier.padding(vertical = 4.dp), tint = GearColor.Success500)
        }
        IconRow(
            Lucide.X, "Wax — not included",
            Modifier.padding(vertical = 4.dp),
            tint = GearColor.Chalk400,
            textColor = GearText.tertiary,
            strikethrough = true,
        )

        VGap(Space.m)
        Eyebrow("Existing damage recorded")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                GearImage(Demo.torqConditionPhotos[0], "Damage", Modifier.size(52.dp), GearShape.sm)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text("Tail · pressure ding", style = GearType.labelL, color = GearText.primary)
                    Text("Cosmetic · recorded at pickup", style = GearType.bodyS, color = GearText.tertiary)
                }
                StatusBadge("Cosmetic", GearColor.Warning700)
            }
        }

        VGap(Space.l)
        SignatureBlock(Demo.nirmal, signed = true, alpha = 1f)
        VGap(Space.sm)
        SignatureBlock(Demo.kavitha, signed = false, alpha = alpha)

        VGap(Space.m)
        Banner(
            text = "This record is what settles any claim later. It can't be edited after both signatures.",
            kind = BannerKind.SAFETY,
        )
        VGap(Space.section)
    }
}

@Composable
private fun SignatureBlock(
    person: com.gearloop.app.data.Person,
    signed: Boolean,
    alpha: Float,
) {
    GearCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(person, size = 32.dp)
                Spacer(Modifier.width(Space.s))
                Text(person.name, style = GearType.labelL, color = GearText.primary)
                Spacer(Modifier.weight(1f))
                if (signed) {
                    GearIcon(Lucide.CircleCheck, null, size = IconSize.m, tint = GearColor.Success500)
                } else {
                    Text(
                        "AWAITING",
                        style = GearType.labelXs,
                        color = GearColor.Warning700,
                        modifier = Modifier.alpha(alpha),
                    )
                }
            }
            VGap(Space.sm)
            if (signed) {
                Text(
                    person.name,
                    style = GearType.headingL.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                    color = GearColor.Pine900,
                )
            } else {
                Spacer(Modifier.height(28.dp))
            }
            GearDivider(color = GearColor.Chalk300)
            VGap(5.dp)
            Text(
                if (signed) "Signed 08 Aug 2026, 09:03" else "Not signed yet",
                style = GearType.dataS,
                color = GearText.tertiary,
            )
        }
    }
}

/* ------------------------------------------------------------------ S-17 */

@Composable
fun S17ActiveRental(nav: Nav) {
    var checklistOpen by rememberSaveable { mutableStateOf(false) }

    GearScreen(
        tabBar = { MainTabBar(nav, MainTab.RENTALS) },
        contentPadding = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(GearColor.Pine800)
                .padding(horizontal = Space.screen)
                .padding(top = Space.section + Space.l, bottom = Space.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(72.dp), GearShape.sm)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(Demo.torq.shortTitle, style = GearType.headingS, color = GearText.onDark)
                    VGap(4.dp)
                    Text(Demo.BOOKING_REF, style = GearType.dataM, color = GearColor.Chalk300)
                }
            }
            VGap(Space.m)
            Text("2d 04h 12m", style = GearType.dataXl, color = GearText.onDark)
            VGap(4.dp)
            Text("UNTIL RETURN", style = GearType.labelXs, color = GearColor.HiVis500)
        }

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.m)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GearIcon(Lucide.Clock, null, size = IconSize.s, tint = GearColor.Chalk500)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Mon 10 Aug, 17:00 · Nallur",
                    style = GearType.dataM,
                    color = GearText.primary,
                )
            }

            VGap(Space.m)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                QuickAction(Lucide.CalendarClock, "Extend", Modifier.weight(1f)) { }
                QuickAction(Lucide.TriangleAlert, "Report issue", Modifier.weight(1f)) { nav.to("s34") }
                QuickAction(Lucide.MessageSquare, "Message", Modifier.weight(1f)) { nav.to("s33") }
            }

            VGap(Space.l)
            GearCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.cardPadding)) {
                    Eyebrow("Looking after it")
                    VGap(Space.s)
                    Demo.careNotes.forEach {
                        IconRow(
                            Lucide.Droplet, it,
                            Modifier.padding(vertical = 4.dp),
                            tint = GearColor.Marine500,
                            style = GearType.bodyS,
                        )
                    }
                    VGap(Space.s)
                    Text("From Kavitha", style = GearType.bodyS, color = GearText.tertiary)
                }
            }

            VGap(Space.sm)
            GearCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { checklistOpen = !checklistOpen }
                            .padding(Space.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Return checklist",
                            style = GearType.headingS,
                            color = GearText.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text("4 items", style = GearType.dataM, color = GearText.tertiary)
                        Spacer(Modifier.width(Space.s))
                        GearIcon(
                            if (checklistOpen) Lucide.ChevronUp else Lucide.ChevronDown,
                            null, size = IconSize.m, tint = GearColor.Chalk500,
                        )
                    }
                    if (checklistOpen) {
                        Column(Modifier.padding(start = Space.cardPadding, end = Space.cardPadding, bottom = Space.cardPadding)) {
                            listOf(
                                "Rinse the board and fins with fresh water",
                                "All 3 fins in the bag",
                                "Leash attached",
                                "Re-photograph the same 7 angles",
                            ).forEach {
                                IconRow(
                                    Lucide.Circle, it,
                                    Modifier.padding(vertical = 4.dp),
                                    tint = GearColor.Chalk400,
                                    style = GearType.bodyS,
                                )
                            }
                        }
                    }
                }
            }

            VGap(Space.m)
            GearTag(Demo.torq.tagLine, size = TagSize.FULL)

            VGap(Space.m)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(GearShape.sm)
                    .background(GearColor.HiVis100)
                    .padding(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearIcon(Lucide.ShieldCheck, null, size = IconSize.m, tint = GearColor.Pine900)
                Spacer(Modifier.width(Space.s))
                Text(
                    "No deposit held · Plus protection active",
                    style = GearType.bodyS,
                    color = GearColor.Pine900,
                )
            }

            VGap(Space.m)
            GearButton("Start return", { nav.to("s18") }, Modifier.fillMaxWidth())
            VGap(Space.section)
        }
    }
}

@Composable
private fun QuickAction(
    icon: LucideIcon,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(88.dp)
            .clip(GearShape.md)
            .border(Border.hair, GearSurface.border, GearShape.md)
            .background(GearSurface.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Space.s),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GearIcon(icon, null, size = IconSize.l, tint = GearColor.Pine800)
        VGap(6.dp)
        Text(label, style = GearType.labelM, color = GearText.primary, textAlign = TextAlign.Center)
    }
}

/* ------------------------------------------------------------------ S-18 */

@Composable
fun S18ReturnSettlement(nav: Nav) {
    GearScreen(
        appBar = { AppBar(title = "Return check", onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        Eyebrow("Pickup vs return")
        VGap(6.dp)
        Text(
            "The same seven angles, shot at handover and again on return. " +
                "Kavitha reviews rather than hunts.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.sm)

        Row(Modifier.fillMaxWidth()) {
            Text("AT PICKUP", style = GearType.labelXs, color = GearText.tertiary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(54.dp))
            Text("ON RETURN", style = GearType.labelXs, color = GearText.tertiary, modifier = Modifier.weight(1f))
        }
        VGap(6.dp)

        Demo.returnComparison.forEach { (label, photo, changed) ->
            ComparisonRow(label, photo, changed) { if (changed) nav.to("s18b") }
            VGap(Space.s)
        }

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(Space.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress = 0.28f, size = 52.dp, strokeWidth = 5.dp, color = GearColor.Warning500) {
                    Text("6h", style = GearType.dataS, color = GearText.primary)
                }
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text("Kavitha is checking", style = GearType.headingS, color = GearText.primary)
                    VGap(2.dp)
                    Text("24 h window · 18 h left", style = GearType.bodyS, color = GearText.secondary)
                }
            }
        }

        VGap(Space.m)
        Eyebrow("Settlement")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                SettlementRow(Lucide.CircleCheck, "Returned on time", "10 Aug, 16:41 · 19 min early", GearColor.Success500)
                VGap(Space.sm)
                SettlementRow(Lucide.CircleCheck, "No new damage reported", "Tail ding was recorded at pickup", GearColor.Success500)
                VGap(Space.sm)
                GearDivider()
                VGap(Space.sm)
                PriceRow(com.gearloop.app.data.PriceLine("Deposit", "No deposit held", com.gearloop.app.data.PriceLine.Kind.MUTED))
                VGap(Space.s)
                PriceRow(com.gearloop.app.data.PriceLine("Total paid", lkr(Demo.BOOKING_TOTAL), com.gearloop.app.data.PriceLine.Kind.TOTAL))
            }
        }

        VGap(Space.m)
        GearButton("Rate Kavitha", { nav.to("s42") }, Modifier.fillMaxWidth(), icon = Lucide.Star)
        VGap(Space.s)
        GearButton(
            "Download receipt", { }, Modifier.fillMaxWidth(),
            type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Download,
        )

        VGap(Space.m)
        Banner(
            text = "Your review and hers stay hidden until you've both submitted.",
            kind = BannerKind.INFO,
        )
        VGap(Space.section)
    }
}

@Composable
private fun ComparisonRow(label: String, photo: Int, changed: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.sm)
            .then(
                if (changed) Modifier.border(Border.heavy, GearColor.Warning500, GearShape.sm) else Modifier
            )
            .clickable(enabled = changed, role = Role.Button, onClick = onClick)
            .padding(if (changed) 5.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearImage(photo, "$label at pickup", Modifier.weight(1f).height(96.dp), GearShape.sm)
        Column(
            Modifier.width(54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label.uppercase(), style = GearType.labelXs, color = GearText.tertiary, textAlign = TextAlign.Center)
            if (changed) {
                VGap(4.dp)
                Text(
                    "CHANGED",
                    style = GearType.labelXs,
                    color = GearText.onDark,
                    modifier = Modifier
                        .clip(GearShape.xs)
                        .background(GearColor.Warning500)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        GearImage(photo, "$label on return", Modifier.weight(1f).height(96.dp), GearShape.sm)
    }
}

@Composable
private fun SettlementRow(
    icon: LucideIcon,
    title: String,
    detail: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        GearIcon(icon, null, size = IconSize.m, tint = color)
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.primary)
            VGap(2.dp)
            Text(detail, style = GearType.dataM, color = GearText.secondary)
        }
    }
}

/* ----------------------------------------------------------------- S-18b */

@Composable
fun S18bEvidenceDiff(nav: Nav) {
    GearScreen(
        appBar = { AppBar(title = "Tail · pickup vs return", type = AppBarType.MODAL, onBack = { nav.back() }) },
        background = GearSurface.card,
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton(
                        "It's the same", { nav.back() }, Modifier.weight(1f),
                        type = ButtonType.SECONDARY, size = ButtonSize.M,
                    )
                    GearButton("Raise a claim", { nav.to("s34") }, Modifier.weight(1f), size = ButtonSize.M)
                }
            }
        },
    ) {
        VGap(Space.m)
        Column {
            Eyebrow("At pickup · 08 Aug 08:57")
            VGap(6.dp)
            GearImage(
                Demo.returnComparison[3].second,
                "Tail at pickup",
                Modifier.fillMaxWidth().height(200.dp),
                GearShape.md,
            )
        }
        VGap(Space.m)
        Column {
            Eyebrow("On return · 10 Aug 16:41")
            VGap(6.dp)
            Box {
                GearImage(
                    Demo.returnComparison[3].second,
                    "Tail on return",
                    Modifier.fillMaxWidth().height(200.dp),
                    GearShape.md,
                )
                // The diff highlight — a ring over the region that changed.
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(96.dp)
                        .clip(GearShape.full)
                        .border(3.dp, GearColor.Danger500, GearShape.full)
                        .background(GearColor.Danger500.copy(alpha = 0.12f))
                )
            }
        }
        VGap(Space.m)
        Banner(
            title = "Difference detected on the tail",
            text = "A 3 cm mark appears in the return photo that isn't in the pickup photo. " +
                "Both photos are geotagged to Nallur and timestamped.",
            kind = BannerKind.WARNING,
        )
        VGap(Space.m)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            MetaChip("SAME ANGLE")
            MetaChip("SAME LIGHTING")
            MetaChip("9.7134°N 80.0246°E")
        }
        VGap(Space.section)
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text,
        style = GearType.dataS,
        color = GearText.tertiary,
        modifier = Modifier
            .clip(GearShape.xs)
            .background(GearColor.Chalk100)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}
