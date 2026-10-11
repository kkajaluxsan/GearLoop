package com.gearloop.app.screens.owner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.Demo
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Grade
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.Routes
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.Border
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Metrics
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.theme.Text
import com.gearloop.app.ui.vm.ExchangeSupplyViewModel
import com.gearloop.app.ui.vm.gearViewModel

/** Opened with nothing in progress, the listing flow starts a new draft. */
@Composable
private fun rememberListing(vm: ExchangeSupplyViewModel): GearItem? {
    val draft by vm.draftListing.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (vm.draftListing.value == null) vm.startListing {} }
    return draft
}

/** A small count as a word, for running prose — "seven angles", not "7 angles". */
private fun spelled(n: Int): String = when (n) {
    in 1..10 -> listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")[n - 1]
    else -> "$n"
}

/** The noun for a sport's gear, for "Show us the board". */
private fun gearWord(sport: String): String = when (sport) {
    "Surfing", "SUP", "Snowboarding" -> "board"
    "Cycling" -> "bike"
    "Kayaking" -> "kayak"
    "Camping" -> "kit"
    else -> "${sport.lowercase()} gear"
}

/* ------------------------------------------------------------------ photos and details */

/**
 * What it is and what it looks like: the title, the size on the tag, the sport,
 * the condition grade, the owner's own description, and a photo for each of the
 * seven angles a renter checks against at handover — taken with the camera or
 * chosen from the gallery. The first photo is the hero.
 */
@Composable
fun S25CreateListingPhotos(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val item = rememberListing(vm)
    var editingSlot by rememberSaveable { mutableIntStateOf(-1) }
    var pickingSlot by rememberSaveable { mutableIntStateOf(-1) }
    // Edited as text and written once the screen is left behind — on Continue or
    // Save — rather than on every key, in one write (D-15).
    var title by rememberSaveable(item?.id) { mutableStateOf(item?.title?.takeUnless { it.startsWith("New ") } ?: "") }
    var size by rememberSaveable(item?.id) { mutableStateOf(item?.sizeLabel?.takeIf { it != "—" } ?: "") }
    var sport by rememberSaveable(item?.id) { mutableStateOf(item?.sport ?: "Surfing") }
    var grade by rememberSaveable(item?.id) { mutableIntStateOf(item?.grade?.ordinal ?: 1) }
    var description by rememberSaveable(item?.id) { mutableStateOf(item?.description ?: "") }
    val picker = rememberPhotoPicker(
        title = "Listing photo",
        onTaken = { file -> if (pickingSlot >= 0) vm.photoTaken(pickingSlot, file) },
        onChosen = { uri -> if (pickingSlot >= 0) vm.photoChosen(pickingSlot, uri) },
    )

    if (item == null) {
        GearScreen(appBar = { AppBar(onBack = { nav.back() }) }) {}
        return
    }
    val angles = com.gearloop.app.data.CaptureGuide.anglesFor(sport)
    val commit = {
        vm.setDetails(title.ifBlank { item.title }, size.ifBlank { item.sizeLabel }, sport, Grade.entries[grade], description)
    }
    val draft = item.status == ListingStatus.DRAFT
    val photoCount = item.photos.size.coerceAtMost(angles.size)
    // Said beside the button, as the waiver does, rather than left to a greyed-out Continue.
    val missing = buildList {
        if (title.trim().length < 3) add("add a title")
        if (photoCount == 0) add("add the first photo")
    }

    GearScreen(
        appBar = {
            AppBar(
                title = if (draft) "New listing" else "Edit listing",
                subtitle = "${item.owner.name.uppercase()} · OWNER",
                onBack = { commit(); nav.back() },
            )
        },
        footer = {
            StickyFooter {
                Column(Modifier.fillMaxWidth()) {
                    if (missing.isNotEmpty()) {
                        Text("To continue: " + missing.joinToString(" · "), style = GearType.bodyS, color = GearText.secondary)
                        VGap(Space.s)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        GearButton(
                            "Save draft", { commit(); vm.saveDraft() }, Modifier.weight(1f),
                            type = ButtonType.GHOST, size = ButtonSize.M,
                            enabled = draft,
                        )
                        GearButton(
                            "Continue", { commit(); nav.to("s26") }, Modifier.weight(1.4f), size = ButtonSize.M,
                            enabled = missing.isEmpty(),
                        )
                    }
                }
            }
        },
    ) {
        ProgressStepper(step = 1, total = 2)
        VGap(Space.l)
        Text("Show us the ${gearWord(sport)}", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.s)
        val count = spelled(angles.size)
        Text(
            "The $count angles a renter checks at handover. $photoCount of ${angles.size} done.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.m)
        GearTextField(title, label = "Title", placeholder = "e.g. Torq Mod Fish 6'6\" Softtop", onValueChange = { title = it })
        VGap(Space.s)
        GearTextField(size, label = "Size on the tag", placeholder = "e.g. 6'6\", M 52, EU 42, 2P", onValueChange = { size = it })

        VGap(Space.m)
        Eyebrow("Sport")
        VGap(Space.s)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Demo.sports.map { it.first }.forEach { s -> Chip(s, selected = sport == s, onClick = { sport = s }) }
        }
        VGap(Space.m)
        Eyebrow("Condition")
        VGap(Space.s)
        SegmentedControl(Grade.entries.map { "Grade ${it.letter}" }, grade) { grade = it }
        VGap(6.dp)
        Text(Grade.entries[grade].summary, style = GearType.bodyS, color = GearText.tertiary)
        VGap(Space.m)
        GearTextField(
            description, label = "Description",
            placeholder = "What it's good for, what comes with it, anything a renter should know",
            onValueChange = { description = it.take(500) }, singleLine = false,
        )

        VGap(Space.l)
        Eyebrow("Photos")
        VGap(Space.s)
        angles.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                pair.forEachIndexed { col, label ->
                    val slot = row * 2 + col
                    PhotoSlot(
                        label = label,
                        photo = item.photos.getOrNull(slot),
                        isHero = slot == 0,
                        // Slots fill in order, so the next empty one is the only one that can be tapped.
                        enabled = slot <= item.photos.size,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (slot < item.photos.size) editingSlot = slot
                        else { pickingSlot = slot; picker.open() }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Banner(text = "Shoot in daylight against a plain background.", kind = BannerKind.INFO)
        if (draft) {
            VGap(Space.m)
            GearLink("Delete this draft", { vm.discardListing { nav.back() } }, color = GearText.tertiary, icon = Lucide.Trash2)
        }
        VGap(Space.section)
    }

    if (editingSlot >= 0) {
        val label = angles.getOrElse(editingSlot) { "photo" }
        GearDialog(
            title = "$label photo",
            onDismiss = { editingSlot = -1 },
            confirmLabel = "Replace",
            onConfirm = { pickingSlot = editingSlot; editingSlot = -1; picker.open() },
            destructiveLabel = "Remove",
            onDestructive = { vm.removePhoto(editingSlot, label.lowercase()); editingSlot = -1 },
        ) {
            item.photos.getOrNull(editingSlot)?.let {
                GearImage(it, label, Modifier.fillMaxWidth().height(180.dp), GearShape.md)
            }
        }
    }
}

@Composable
private fun PhotoSlot(
    label: String,
    photo: String?,
    isHero: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clip(GearShape.sm)
                .then(
                    if (photo == null) Modifier.border(Border.medium, if (enabled) GearColor.Chalk300 else GearColor.Chalk200, GearShape.sm)
                    else Modifier
                )
                .background(if (photo == null) GearColor.Chalk50 else Color.Transparent)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = if (photo == null) "Add the $label photo" else "Replace or remove the $label photo",
                    onClick = onClick,
                )
                .semantics { contentDescription = "$label photo slot" },
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                GearImage(photo, label, Modifier.fillMaxSize(), GearShape.sm)
                if (isHero) {
                    Text(
                        "HERO",
                        style = GearType.labelXs,
                        color = GearColor.Pine900,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(GearShape.xs)
                            .background(GearColor.HiVis500)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            } else {
                GearIcon(
                    if (enabled) Lucide.Camera else Lucide.Lock, null,
                    size = IconSize.l, tint = if (enabled) GearColor.Chalk400 else GearColor.Chalk300,
                )
            }
        }
        VGap(5.dp)
        Text(label.uppercase(), style = GearType.labelXs, color = GearText.tertiary)
    }
}

/* ------------------------------------------------------------------ price and terms */

/**
 * The daily rate, set against what similar gear actually rents for nearby;
 * the deposit; instant book and delivery; the duration discounts. Publishing
 * puts it in search, in the owner's calendar and, from a closet item, marks
 * the closet item as listed.
 */
@Composable
fun S26CreateListingPricing(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val item = rememberListing(vm)
    if (item == null) {
        GearScreen(appBar = { AppBar(onBack = { nav.back() }) }) {}
        return
    }
    val draft = item.status == ListingStatus.DRAFT
    val price = item.pricePerDay
    val market = vm.marketFor(item.sport, item.id)

    GearScreen(
        appBar = {
            AppBar(
                title = if (draft) "New listing" else item.shortTitle,
                subtitle = "${item.owner.name.uppercase()} · OWNER",
                onBack = { nav.back() },
            )
        },
        footer = {
            StickyFooter(priceValue = lkr(price), priceCaption = "a day") {
                GearButton(
                    if (draft) "Publish listing" else "Save changes",
                    { vm.publish { if (draft) nav.reset(Routes.MY_LISTINGS) else nav.back() } },
                    size = ButtonSize.M,
                    enabled = item.photos.isNotEmpty(),
                )
            }
        },
    ) {
        ProgressStepper(step = 2, total = 2)
        VGap(Space.l)
        Text("Set your price", style = GearType.headingXl, color = GearText.primary)

        VGap(Space.m)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.md)
                .background(GearColor.HiVis100)
                .padding(Space.cardPadding),
        ) {
            Text("SUGGESTED", style = GearType.labelXs, color = GearColor.Pine900)
            VGap(4.dp)
            Text(lkr(market.suggested), style = GearType.dataXl, color = GearColor.Pine900)
            VGap(Space.s)
            Text(
                if (market.count > 0) "Middle of ${market.count} nearby, which rent for ${lkr(market.low)}–${grouped(market.high)} a day"
                else "Nothing like it is listed nearby yet",
                style = GearType.bodyS,
                color = GearColor.Pine900.copy(alpha = 0.75f),
            )
        }

        VGap(Space.m)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Stepper(value = "", onMinus = { vm.setPrice(price - 100) }, onPlus = { vm.setPrice(price + 100) }, minusEnabled = price > 100)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("LKR ", style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.padding(bottom = 5.dp))
                    Text(grouped(price), style = GearType.dataXl, color = GearText.primary)
                    Text(" / day", style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
        }
        if (price != market.suggested) {
            VGap(Space.s)
            GearLink("Use the suggested ${lkr(market.suggested)}", { vm.setPrice(market.suggested) })
        }

        if (market.count > 1) {
            VGap(Space.l)
            Eyebrow("Where you sit")
            VGap(Space.s)
            MarketBand(price, market.low, market.median, market.high)
        }

        VGap(Space.l)
        Eyebrow("Duration discounts")
        VGap(Space.s)
        Demo.durationDiscounts.forEachIndexed { i, (label, pct, _) ->
            // The row is the switch, so it is announced as "3+ days, 10% off, on" (D-11).
            val on = item.discounts.getOrElse(i) { false }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Metrics.tapTarget)
                    .toggleable(value = on, role = Role.Switch, onValueChange = { vm.setDiscount(i, it) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = GearType.labelL, color = GearText.primary, modifier = Modifier.width(88.dp))
                Text(pct, style = GearType.dataM, color = GearColor.Success500)
                Spacer(Modifier.weight(1f))
                Toggle(on, onCheckedChange = null)
            }
        }

        VGap(Space.l)
        Eyebrow("Booking")
        VGap(Space.s)
        ToggleRow("Instant book", item.instantBook, { vm.setInstantBook(it) }, helper = "Renters book without waiting for you to accept")
        VGap(Space.m)
        ToggleRow("I can deliver it", item.delivery, { vm.setDelivery(it) }, helper = "Within 5 km of ${item.owner.area.ifBlank { "you" }}, for ${lkr(600)}")

        VGap(Space.l)
        Eyebrow("Deposit")
        VGap(Space.s)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Column(Modifier.weight(1f)) {
                Text(lkr(item.deposit), style = GearType.dataL, color = GearText.primary)
                Text("Held, not charged. Released after return.", style = GearType.bodyS, color = GearText.tertiary)
            }
            Stepper(value = "", onMinus = { vm.setDeposit(item.deposit - 1_000) }, onPlus = { vm.setDeposit(item.deposit + 1_000) }, minusEnabled = item.deposit >= 1_000)
        }
        VGap(Space.s)
        ToggleRow("Waive the deposit for renters with Plus protection", item.waiveDepositWithPlus, { vm.setWaiveDeposit(it) })

        VGap(Space.m)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .background(GearColor.Chalk50)
                .padding(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIcon(Lucide.TrendingUp, null, size = IconSize.m, tint = GearColor.Success500)
            Spacer(Modifier.width(Space.s))
            val month = price * 8 - Pricing.serviceFeeOn(price * 8)
            Text(
                "Rented 8 days a month, you'd earn about ${lkr(month)}",
                style = GearType.bodyS,
                color = GearText.body,
            )
        }
        VGap(Space.section)
    }
}

@Composable
private fun MarketBand(price: Int, low: Int, median: Int, high: Int) {
    val span = (high - low).coerceAtLeast(1).toFloat()
    val fraction = ((price - low) / span).coerceIn(0f, 1f)
    val medianAt = ((median - low) / span).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(30.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(GearShape.full)
                    .background(GearColor.Chalk200)
            )
            val tickColor = GearColor.Chalk500
            val markerFill = GearColor.HiVis500
            val markerEdge = GearColor.Pine800
            Canvas(Modifier.fillMaxSize()) {
                listOf(0f, medianAt, 1f).forEach { f ->
                    val x = size.width * f
                    drawLine(tickColor, Offset(x, size.height / 2 - 9.dp.toPx()), Offset(x, size.height / 2 + 9.dp.toPx()), 1.5.dp.toPx())
                }
                val x = (size.width * fraction).coerceIn(6.dp.toPx(), size.width - 6.dp.toPx())
                drawCircle(markerFill, 9.dp.toPx(), Offset(x, size.height / 2))
                drawCircle(markerEdge, 9.dp.toPx(), Offset(x, size.height / 2), style = Stroke(width = 2.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Low ${grouped(low)}", style = GearType.dataS, color = GearText.tertiary)
            Spacer(Modifier.weight(1f))
            Text("Middle ${grouped(median)}", style = GearType.dataS, color = GearText.tertiary)
            Spacer(Modifier.weight(1f))
            Text("High ${grouped(high)}", style = GearType.dataS, color = GearText.tertiary)
        }
    }
}

