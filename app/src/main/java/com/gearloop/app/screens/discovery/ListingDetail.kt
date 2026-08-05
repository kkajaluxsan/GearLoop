package com.gearloop.app.screens.discovery

import com.gearloop.app.ui.theme.Border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.FitLogic
import com.gearloop.app.data.grouped
import com.gearloop.app.data.kmAway
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Nav
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

/* ------------------------------------------------------------------ S-07 */

/**
 * The money screen.
 *
 * Everything a person needs to trust an object they have never seen, in the
 * order they need it: what it is, whether it fits them, what it costs, what
 * condition it is actually in, who has it, and what happens if it goes wrong.
 */
@Composable
fun S07ListingDetail(nav: Nav) {
    val item = Demo.torq
    var saved by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(GearSurface.app)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberSavedScrollState()),
        ) {
            HeroCarousel(item.photos, saved, onSave = { saved = !saved }, nav = nav)

            Column(Modifier.padding(horizontal = Space.screen)) {
                // The tag hangs over the bottom edge of the hero.
                GearTag(item.tagLine, Modifier.offset(y = (-13).dp), size = TagSize.FULL)

                Text(item.title, style = GearType.headingXl, color = GearText.primary)
                VGap(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GearIcon(Lucide.MapPin, null, size = IconSize.s, tint = GearColor.Chalk500)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${item.owner.area} · ${kmAway(item.distanceKm)}",
                        style = GearType.bodyS,
                        color = GearText.secondary,
                    )
                }

                VGap(Space.m)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FitStamp(item.fit)
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        FitLogic.fitExplanation(42, Demo.WEIGHT_KG),
                        style = GearType.bodyS,
                        color = GearText.secondary,
                    )
                }

                VGap(Space.m)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceInline(item.pricePerDay, style = GearType.dataXl)
                    Spacer(Modifier.weight(1f))
                    if (item.instantBook) {
                        Row(
                            Modifier
                                .clip(GearShape.full)
                                .background(GearColor.HiVis200)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            GearIcon(Lucide.Zap, null, size = IconSize.s, tint = GearColor.Pine900)
                            Text("Instant book", style = GearType.labelM, color = GearColor.Pine900)
                        }
                    }
                }

                VGap(Space.l)
                GearDivider()
                VGap(Space.l)

                // --- dates -------------------------------------------------
                SelectRow(
                    label = "Your dates",
                    value = "${Demo.BOOKING_DATES} · ${Demo.BOOKING_DAYS} days",
                    onClick = { nav.to("s08") },
                    supporting = "3-day discount applied · save ${lkr(840)}",
                    leadingIcon = Lucide.Calendar,
                )

                Section("What's included")
                Demo.torqIncluded.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth()) {
                        pair.forEach { label ->
                            IconRow(
                                icon = Lucide.Check,
                                text = label,
                                tint = GearColor.Success500,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    VGap(Space.s)
                }
                Demo.torqExcluded.forEach {
                    IconRow(
                        icon = Lucide.X,
                        text = it,
                        tint = GearColor.Chalk400,
                        textColor = GearText.tertiary,
                        strikethrough = true,
                    )
                }

                Section("Specifications")
                Demo.torqSpecs.forEach { spec ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(spec.label, style = GearType.bodyS, color = GearText.secondary)
                        if (spec.fitRelevant) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(6.dp).clip(CircleShape).background(GearColor.HiVis500))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(spec.value, style = GearType.dataM, color = GearText.primary)
                    }
                }
                VGap(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(GearColor.HiVis500))
                    Spacer(Modifier.width(6.dp))
                    Text("Affects how it fits you", style = GearType.bodyS, color = GearText.tertiary)
                }

                Section("Condition")
                ConditionBlock()

                Section("Handover")
                HandoverBlock()

                Section("Protection & deposit")
                Banner(
                    text = "${lkr(item.deposit)} deposit is a hold on your card, not a charge. " +
                        "Released within 24 h of return.",
                    kind = BannerKind.SAFETY,
                    action = "Compare protection plans",
                    onAction = { nav.to("s09") },
                )

                VGap(Space.l)
                GearDivider()
                VGap(Space.l)
                OwnerStrip(item.owner, onClick = { nav.to("s37") })
                VGap(Space.sm)
                GearButton(
                    "Message Kavitha",
                    { nav.to("s33") },
                    Modifier.fillMaxWidth(),
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.M,
                    icon = Lucide.MessageSquare,
                )

                Section("Reviews")
                ReviewsBlock(nav)

                Section("House rules & cancellation")
                Demo.houseRules.forEach { (title, body) ->
                    Column(Modifier.padding(bottom = Space.sm)) {
                        Text(title, style = GearType.labelL, color = GearText.primary)
                        VGap(2.dp)
                        Text(body, style = GearType.bodyS, color = GearText.secondary)
                    }
                }
            }

            Section2("Similar boards")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberSavedScrollState())
                    .padding(horizontal = Space.screen),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                listOf(Demo.nspSup, Demo.wetsuit, Demo.kayak, Demo.trekMarlin).forEach {
                    ListingCard(it, { }, layout = ListingLayout.GRID)
                }
            }
            VGap(Space.section)
        }

        StickyFooter(
            priceValue = lkr(Demo.LISTING_FOOTER_TOTAL),
            priceCaption = "${Demo.BOOKING_DAYS} days, all in",
        ) {
            GearButton("Book instantly", { nav.to("s08") }, size = ButtonSize.M, icon = Lucide.Zap)
        }
    }
}

@Composable
private fun HeroCarousel(photos: List<Int>, saved: Boolean, onSave: () -> Unit, nav: Nav) {
    val pager = rememberPagerState(pageCount = { photos.size })
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            GearImage(photos[page], "Torq Mod Fish photo ${page + 1}", Modifier.fillMaxSize())
        }

        // Floating transparent app bar over the hero.
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Space.sm, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIconButton(Lucide.ChevronLeft, "Back", { nav.back() }, tone = IconButtonTone.ON_IMAGE, iconSize = IconSize.l)
            Spacer(Modifier.weight(1f))
            GearIconButton(Lucide.Share2, "Share", { }, tone = IconButtonTone.ON_IMAGE)
            Spacer(Modifier.width(6.dp))
            GearIconButton(
                icon = Lucide.Heart,
                contentDescription = if (saved) "Saved" else "Save",
                onClick = onSave,
                tone = IconButtonTone.ON_IMAGE,
                tint = if (saved) GearColor.HiVis600 else GearColor.Pine800,
                filled = saved,
            )
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = Space.screen, bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Pill("${pager.currentPage + 1} / 9")
            Pill(
                text = "Condition photos",
                fill = GearSurface.overImage.copy(alpha = 0.92f),
                textColor = GearColor.Pine900,
                modifier = Modifier.clickable { },
            )
        }

        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = Space.screen, bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(photos.size) { i ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            GearSurface.overImage.copy(alpha = if (i == pager.currentPage) 1f else 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun ConditionBlock() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(56.dp)
                .clip(GearShape.sm)
                .border(Border.heavy, GearColor.Pine800, GearShape.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(Demo.torq.grade.letter, style = GearType.displayXl, color = GearColor.Pine900)
        }
        Spacer(Modifier.width(Space.sm))
        Text(
            Demo.torq.grade.summary,
            style = GearType.bodyM,
            color = GearText.body,
            modifier = Modifier.weight(1f),
        )
    }
    VGap(Space.m)
    Eyebrow("Condition photos · 12 Jul 2026")
    VGap(Space.s)
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        Demo.torqConditionPhotos.forEach {
            GearImage(it, "Condition photo", Modifier.weight(1f).height(88.dp), GearShape.sm)
        }
    }
    VGap(Space.s)
    Text("Used 47 days · last serviced 2 Jul", style = GearType.dataM, color = GearText.tertiary)
}

@Composable
private fun HandoverBlock() {
    // A radius, not a pin — the exact address is not public before booking.
    MapThumb(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        caption = "Exact address shared once booked",
    )
    VGap(Space.sm)
    Demo.handoverMethods.forEach { (name, detail, price) ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = GearType.labelL, color = GearText.primary)
                Text(detail, style = GearType.bodyS, color = GearText.tertiary)
            }
            Text(
                price,
                style = GearType.dataM,
                color = if (price == "Free") GearColor.Success500 else GearText.primary,
            )
        }
    }
}

/**
 * Stand-in for a map view.
 *
 * Deliberately restyled to the palette rather than left as default map blue —
 * a default map tile in a design portfolio is a mark-loser.
 */
@Composable
fun MapThumb(
    modifier: Modifier = Modifier,
    caption: String? = null,
    showRoute: Boolean = false,
) {
    Column {
        Box(
            modifier
                .clip(GearShape.md)
                .background(GearColor.Chalk100)
                .border(Border.hair, GearSurface.border, GearShape.md),
        ) {
            MapCanvas(Modifier.fillMaxSize(), showRoute = showRoute)
        }
        if (caption != null) {
            VGap(6.dp)
            Text(caption, style = GearType.bodyS, color = GearText.tertiary)
        }
    }
}

@Composable
private fun ReviewsBlock(nav: Nav) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("★ ${Demo.kavitha.rating}", style = GearType.dataL, color = GearText.primary)
        Spacer(Modifier.width(Space.s))
        Text("${Demo.kavitha.reviews} reviews", style = GearType.bodyM, color = GearText.secondary)
    }
    VGap(Space.sm)
    Demo.subRatings.forEach { (label, value) ->
        RatingBar(label, value / 5f, Modifier.padding(vertical = 4.dp), value = String.format("%.2f", value))
    }
    VGap(Space.sm)
    Demo.torqReviews.take(2).forEach {
        ReviewCard(it)
        VGap(Space.sm)
    }
    GearButton(
        "See all ${Demo.kavitha.reviews} reviews",
        { nav.to("s42") },
        Modifier.fillMaxWidth(),
        type = ButtonType.GHOST,
        size = ButtonSize.M,
    )
}

@Composable
private fun Section(eyebrow: String) {
    VGap(Space.l)
    GearDivider()
    VGap(Space.l)
    Eyebrow(eyebrow)
    VGap(Space.sm)
}

@Composable
private fun Section2(eyebrow: String) {
    VGap(Space.l)
    Box(Modifier.padding(horizontal = Space.screen)) { Eyebrow(eyebrow) }
    VGap(Space.sm)
}
