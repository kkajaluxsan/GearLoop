package com.gearloop.app.screens.discovery

import android.content.Intent
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
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.Availability
import com.gearloop.app.data.FitLogic
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.dateRange
import com.gearloop.app.data.kmAway
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.MoneyRepository
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
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------ S-07 */

/**
 * The money screen.
 *
 * Everything a person needs to trust an object they have never seen, in the
 * order they need it: what it is, whether it fits them, what it costs, what
 * condition it is actually in, who has it, and what happens if it goes wrong.
 *
 * Milestone 03 · Read the listing that was opened — any listing, not only the
 * Torq — with its live fit verdict and its own condition record; Update its
 * saved state; Create the booking draft that the next four screens edit.
 */
@Composable
fun S07ListingDetail(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val item = selected ?: return

    // A booking already under way shows its own dates; otherwise the default
    // the draft would start with, so the figure here is the one S-08 opens on.
    val span = draft?.takeIf { it.item.id == item.id }?.span ?: vm.defaultSpan(item)
    val days = Availability.daysIn(span)
    val quote = Pricing.quote(item, days, com.gearloop.app.data.Protection.PLUS, couponEligible = false)

    val scroll = rememberSavedScrollState()
    val scope = rememberCoroutineScope()
    // Layout position, not screen state: re-measured on every frame it changes.
    var conditionY by remember { mutableIntStateOf(0) }
    // Opening a similar listing from the bottom of this one starts at its top.
    LaunchedEffect(item.id) { if (scroll.value > 0) scroll.scrollTo(0) }

    val context = LocalContext.current
    val share = {
        val text = "${item.title} on GearLoop · ${lkr(item.pricePerDay)} a day · ${item.code}"
        context.startActivity(
            Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share listing"),
        )
    }
    val ownerFirst = item.owner.name.substringBefore(' ')
    val mine = item.owner.handle == vm.me

    Column(Modifier.fillMaxSize().background(GearSurface.app)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll),
        ) {
            HeroCarousel(
                item = item,
                onSave = { vm.toggleSaved(item) },
                onShare = share,
                onConditionPhotos = { scope.launch { scroll.animateScrollTo(conditionY) } },
                nav = nav,
            )

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
                // The reason in the rider's own units (FR-03), worked out from the
                // profile S-02 saved — so it changes when the profile does.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FitStamp(item.fit)
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        FitLogic.reason(item, profile?.heightCm, profile?.weightKg),
                        style = GearType.bodyS,
                        color = GearText.secondary,
                        modifier = Modifier.weight(1f),
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
                if (!item.available && item.nextFree != null) {
                    VGap(Space.s)
                    Text(
                        "Out on a rental today · ${item.nextFree}",
                        style = GearType.dataS,
                        color = GearText.secondary,
                    )
                }

                if (item.description.isNotBlank()) {
                    VGap(Space.m)
                    Text(item.description, style = GearType.bodyM, color = GearText.body)
                }

                VGap(Space.l)
                GearDivider()
                VGap(Space.l)

                // --- dates -------------------------------------------------
                SelectRow(
                    label = "Your dates",
                    value = "${dateRange(span.start, span.end)} · $days ${if (days == 1) "day" else "days"}",
                    onClick = { vm.startBooking(item) { nav.to("s08") } },
                    supporting = if (quote.durationDiscount > 0) {
                        "$days-day discount applied · save ${lkr(quote.durationDiscount)}"
                    } else {
                        "Book 3 days or more for 10% off"
                    },
                    leadingIcon = Lucide.Calendar,
                )

                Section("What's included")
                item.included.chunked(2).forEach { pair ->
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
                item.excluded.forEach {
                    IconRow(
                        icon = Lucide.X,
                        text = it,
                        tint = GearColor.Chalk400,
                        textColor = GearText.tertiary,
                        strikethrough = true,
                    )
                }
                item.extras.forEach { extra ->
                    VGap(Space.s)
                    IconRow(
                        icon = Lucide.Plus,
                        text = "${extra.name} · ${lkr(extra.perDay)} a day, added at checkout",
                        tint = GearColor.Marine500,
                    )
                }

                if (item.specs.isNotEmpty()) {
                    Section("Specifications")
                    item.specs.forEach { spec ->
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
                    if (item.specs.any { it.fitRelevant }) {
                        VGap(6.dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(GearColor.HiVis500))
                            Spacer(Modifier.width(6.dp))
                            Text("Affects how it fits you", style = GearType.bodyS, color = GearText.tertiary)
                        }
                    }
                }

                Column(Modifier.onGloballyPositioned { conditionY = it.positionInParent().y.toInt() }) {
                    Section("Condition")
                    ConditionBlock(item)
                }

                Section("Handover")
                HandoverBlock(item)

                Section("Protection & deposit")
                Banner(
                    text = "${lkr(item.deposit)} deposit, held on your card, not charged. Plus or Pro protection replaces it.",
                    kind = BannerKind.SAFETY,
                    action = "Compare protection plans",
                    onAction = { vm.startBooking(item) { nav.to("s09") } },
                )

                VGap(Space.l)
                GearDivider()
                VGap(Space.l)
                OwnerStrip(item.owner, onClick = { vm.showProfile(item.owner.handle); nav.to("s37") })
                VGap(Space.sm)
                if (!mine) {
                    GearButton(
                        "Message $ownerFirst",
                        { vm.openThread(item.owner.handle); nav.to("s33") },
                        Modifier.fillMaxWidth(),
                        type = ButtonType.SECONDARY,
                        size = ButtonSize.M,
                        icon = Lucide.MessageSquare,
                    )
                }

                Section("Reviews")
                // Reviews of this listing's owner. An owner's review of a renter is
                // filed against the same listing, and it was showing up here (D-27).
                ReviewsBlock(item, reviews.filter { it.listingId == item.id && it.subject.handle == item.owner.handle }.map { it.entry }, nav)

                Section("House rules & cancellation")
                houseRules(item).forEach { (title, body) ->
                    Column(Modifier.padding(bottom = Space.sm)) {
                        Text(title, style = GearType.labelL, color = GearText.primary)
                        VGap(2.dp)
                        Text(body, style = GearType.bodyS, color = GearText.secondary)
                    }
                }
            }

            val similar = vm.similarTo(item)
            if (similar.isNotEmpty()) {
                Section2("Similar gear")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberSavedScrollState())
                        .padding(horizontal = Space.screen),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    similar.forEach {
                        ListingCard(
                            it,
                            { vm.select(it) },
                            layout = ListingLayout.GRID,
                            onToggleSave = { vm.toggleSaved(it) },
                        )
                    }
                }
            }
            VGap(Space.section)
        }

        StickyFooter(
            priceValue = lkr(Pricing.listingFooter(item, days)),
            // Not "all in": no extras have been picked and no coupon applied on
            // this screen, so the checkout total is a different — lower —
            // number. Saying "all in" here and then charging something else is
            // the exact thing the price breakdown on S-11 exists to avoid.
            priceCaption = "$days ${if (days == 1) "day" else "days"}, before extras",
        ) {
            if (mine) {
                GearButton("Manage listing", { nav.to(com.gearloop.app.nav.Routes.MY_LISTINGS) }, size = ButtonSize.M, icon = Lucide.Pencil)
            } else {
                GearButton(
                    if (item.instantBook) "Book instantly" else "Request to book",
                    { vm.startBooking(item) { nav.to("s08") } },
                    size = ButtonSize.M,
                    icon = if (item.instantBook) Lucide.Zap else Lucide.Send,
                )
            }
        }
    }
}

/** The rules a renter agrees to: the cancellation policy, and what the gear needs. */
private fun houseRules(item: GearItem): List<Pair<String, String>> = buildList {
    add("Cancellation" to MoneyRepository.CANCELLATION_RULE)
    if (item.sport in setOf("Surfing", "SUP", "Kayaking")) {
        add("Rinse with fresh water" to "There's a tap at the pickup point.")
    }
    add("No commercial use" to "Personal use only.")
}

@Composable
private fun HeroCarousel(
    item: GearItem,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onConditionPhotos: () -> Unit,
    nav: Nav,
) {
    val photos = item.photos
    val pager = rememberPagerState(pageCount = { photos.size })
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            GearImage(photos[page], "${item.shortTitle} photo ${page + 1}", Modifier.fillMaxSize(), dimmed = !item.available)
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
            GearIconButton(Lucide.Share2, "Share", onShare, tone = IconButtonTone.ON_IMAGE)
            Spacer(Modifier.width(6.dp))
            GearIconButton(
                icon = Lucide.Heart,
                contentDescription = if (item.saved) "Saved" else "Save",
                onClick = onSave,
                tone = IconButtonTone.ON_IMAGE,
                tint = if (item.saved) GearColor.HiVis600 else GearColor.Pine800,
                filled = item.saved,
            )
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = Space.screen, bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            // The count and the dots have to agree. This said "of 9" beside five
            // dots, because the number was typed and the dots were counted.
            Pill("${pager.currentPage + 1} / ${photos.size}")
            Pill(
                text = "Condition photos",
                fill = GearSurface.overImage.copy(alpha = 0.92f),
                textColor = GearColor.Pine900,
                modifier = Modifier.clickable(role = Role.Button, onClick = onConditionPhotos),
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
private fun ConditionBlock(item: GearItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(56.dp)
                .clip(GearShape.sm)
                .border(Border.heavy, GearColor.Pine800, GearShape.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.grade.letter, style = GearType.displayXl, color = GearColor.Pine900)
        }
        Spacer(Modifier.width(Space.sm))
        Text(
            item.grade.summary,
            style = GearType.bodyM,
            color = GearText.body,
            modifier = Modifier.weight(1f),
        )
    }
    VGap(Space.m)
    Eyebrow("Condition photos · ${item.conditionDate}")
    VGap(Space.s)
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        item.conditionPhotos.take(3).forEach {
            GearImage(it, "Condition photo", Modifier.weight(1f).height(88.dp), GearShape.sm)
        }
        repeat(3 - item.conditionPhotos.take(3).size) { Spacer(Modifier.weight(1f)) }
    }
    VGap(Space.s)
    Text(item.conditionNote, style = GearType.dataM, color = GearText.tertiary)
}

@Composable
private fun HandoverBlock(item: GearItem) {
    // A radius, not a pin — the exact address is not public before booking.
    MapThumb(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        caption = "Exact address shared once booked",
    )
    VGap(Space.sm)
    val area = item.owner.area.substringBefore(',')
    buildList {
        add(Triple("Self pickup", "$area · meet the owner", "Free"))
        if (item.delivery) add(Triple("Delivery", "Within 5 km of $area", lkr(600)))
        if (item.owner.area.contains("Nallur")) add(Triple("GearPoint Nallur", "Collect any time 08:00–20:00", lkr(200)))
    }.forEach { (name, detail, price) ->
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
private fun ReviewsBlock(item: GearItem, reviews: List<com.gearloop.app.data.ReviewEntry>, nav: Nav) {
    val owner = item.owner
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (owner.reviews > 0) {
            Text("★ ${owner.ratingText}", style = GearType.dataL, color = GearText.primary)
            Spacer(Modifier.width(Space.s))
            Text("${owner.reviews} reviews of ${owner.name.substringBefore(' ')}", style = GearType.bodyM, color = GearText.secondary)
        } else {
            Text("${owner.name.substringBefore(' ')} has no reviews yet", style = GearType.bodyM, color = GearText.secondary)
        }
    }
    VGap(Space.sm)
    if (reviews.isEmpty() && owner.reviews > 0) {
        Text("No written reviews for this item yet.", style = GearType.bodyS, color = GearText.tertiary)
        VGap(Space.sm)
    }
    reviews.take(2).forEach {
        ReviewCard(it)
        VGap(Space.sm)
    }
    GearButton(
        if (reviews.size > 2) "See all ${reviews.size} reviews" else "Reviews and ratings",
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
