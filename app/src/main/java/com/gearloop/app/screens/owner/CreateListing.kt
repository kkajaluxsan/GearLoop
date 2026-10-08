package com.gearloop.app.screens.owner

import com.gearloop.app.ui.theme.GearSurface
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.gearloop.app.ui.theme.Border
import androidx.compose.foundation.Canvas
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
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import com.gearloop.app.ui.theme.Metrics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.Demo
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.Seed
import com.gearloop.app.nav.Nav
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

/** Opened straight from the screen index, the listing flow starts a draft for Kavitha. */
@Composable
private fun rememberListing(vm: ExchangeSupplyViewModel): GearItem? {
    val draft by vm.draftListing.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (vm.draftListing.value == null) vm.startListing(Seed.OWNER) {} }
    return draft
}

/* ------------------------------------------------------------------ S-25 */

/**
 * A small count as a word, for running prose — "seven angles", not "7 angles".
 */
private fun spelled(n: Int): String = when (n) {
    in 1..10 -> listOf(
        "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    )[n - 1]
    else -> "$n"
}

/**
 * S-25 · Read the listing being made or edited; Create a photo in a slot; Update
 * its title and size; Delete a photo; save the draft. The slots are the angles
 * the handover checks, in the same order and under the same names.
 */
@Composable
fun S25CreateListingPhotos(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val item = rememberListing(vm)
    var editingSlot by rememberSaveable { mutableIntStateOf(-1) }
    // The title and size are edited as text and written once the field is left
    // behind — on Continue or Save — rather than on every key.
    var title by rememberSaveable(item?.id) { mutableStateOf(item?.title ?: "") }
    var size by rememberSaveable(item?.id) { mutableStateOf(item?.sizeLabel?.takeIf { it != "—" } ?: "") }
    val angles = Demo.captureAngles.map { it.label }

    if (item == null) {
        GearScreen(appBar = { AppBar(onBack = { nav.back() }) }) {}
        return
    }
    // One write for both fields: two separate edits raced, and the second put
    // the old title back (D-15).
    val commit = {
        val newTitle = title.takeIf { it.isNotBlank() } ?: item.title
        val newSize = size.takeIf { it.isNotBlank() } ?: item.sizeLabel
        if (newTitle != item.title || newSize != item.sizeLabel) vm.setDetails(newTitle, newSize)
    }
    val draft = item.status == ListingStatus.DRAFT
    val photoCount = item.photos.size.coerceAtMost(angles.size)

    GearScreen(
        appBar = {
            AppBar(
                title = if (draft) "New listing" else "Edit listing",
                subtitle = if (item.owner.handle == Seed.ME) "YOUR CLOSET" else "KAVITHA SELVAM · OWNER",
                onBack = { commit(); nav.back() },
            )
        },
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton(
                        "Save draft", { commit(); vm.saveDraft() }, Modifier.weight(1f),
                        type = ButtonType.GHOST, size = ButtonSize.M,
                        enabled = draft,
                    )
                    GearButton(
                        "Continue", { commit(); nav.to("s26") }, Modifier.weight(1.4f), size = ButtonSize.M,
                        enabled = photoCount > 0 && title.trim().length >= 3,
                    )
                }
            }
        },
    ) {
        ProgressStepper(step = 2, total = 8)
        VGap(Space.l)
        Text(
            "Show us the ${if (item.sport == "Surfing") "board" else item.sport.lowercase() + " gear"}",
            style = GearType.headingXl,
            color = GearText.primary,
        )
        VGap(Space.s)
        val count = spelled(angles.size)
        Text(
            "${count.replaceFirstChar { it.uppercase() }} angles. " +
                "The same $count a renter checks against at handover. $photoCount of ${angles.size} done.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.m)
        GearTextField(title, label = "Title", placeholder = "e.g. Torq Mod Fish 6'6\" Softtop", onValueChange = { title = it })
        VGap(Space.s)
        GearTextField(size, label = "Size on the tag", placeholder = "e.g. 6'6\", M 52, 158", onValueChange = { size = it })
        VGap(Space.m)

        angles.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                pair.forEachIndexed { col, label ->
                    val slot = row * 2 + col
                    PhotoSlot(
                        label = label,
                        photo = item.photos.getOrNull(slot),
                        isHero = slot == 0,
                        // Slots fill in order, so the next empty one is the only
                        // empty one that can be tapped.
                        enabled = slot <= item.photos.size,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (slot < item.photos.size) editingSlot = slot else vm.addPhoto(slot)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Banner(
            text = "Shoot in daylight against a plain wall. Listings with clear photos get booked 2.3× more.",
            kind = BannerKind.INFO,
        )
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
            confirmLabel = "Retake",
            onConfirm = { vm.addPhoto(editingSlot); editingSlot = -1 },
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
    photo: Int?,
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
                    if (photo == null) {
                        Modifier.border(Border.medium, if (enabled) GearColor.Chalk300 else GearColor.Chalk200, GearShape.sm)
                    } else Modifier
                )
                .background(if (photo == null) GearColor.Chalk50 else Color.Transparent)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = if (photo == null) "Add the $label photo" else "Retake or remove the $label photo",
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
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(GearShape.xs)
                        .background(GearSurface.overImage.copy(alpha = 0.94f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GearIcon(Lucide.Check, null, size = 12.dp, tint = GearColor.Success500)
                        Text("GOOD", style = GearType.labelXs, color = GearColor.Success500)
                    }
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

/* ------------------------------------------------------------------ S-26 */

/**
 * S-26 · Read the listing; Update the daily rate, the duration discounts and
 * the deposit waiver; Update a draft to published — at which point it is in
 * search, in the owner's calendar, and, from a closet item, marked as listed.
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
    val mine = item.owner.handle == Seed.ME

    GearScreen(
        appBar = {
            AppBar(
                title = if (draft) "New listing" else item.shortTitle,
                subtitle = if (mine) "YOUR CLOSET" else "KAVITHA SELVAM · OWNER",
                onBack = { nav.back() },
            )
        },
        footer = {
            StickyFooter(priceValue = lkr(price), priceCaption = "a day") {
                GearButton(
                    if (draft) "Publish listing" else "Save changes",
                    { vm.publish { if (draft) nav.to(if (mine) "s36" else "s24") else nav.back() } },
                    size = ButtonSize.M,
                    enabled = item.photos.isNotEmpty(),
                )
            }
        },
    ) {
        ProgressStepper(step = 6, total = 8)
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
            Text(lkr(Demo.SUGGESTED_PRICE), style = GearType.dataXl, color = GearColor.Pine900)
            VGap(Space.s)
            ConfidenceBand()
            VGap(Space.s)
            Text(
                "Based on 11 similar boards booked in Jaffna in the last 90 days",
                style = GearType.bodyS,
                color = GearColor.Pine900.copy(alpha = 0.75f),
            )
        }

        VGap(Space.m)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Stepper(
                value = "",
                onMinus = { vm.setPrice(price - 100) },
                onPlus = { vm.setPrice(price + 100) },
                minusEnabled = price > 200,
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("LKR ", style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.padding(bottom = 5.dp))
                    Text(grouped(price), style = GearType.dataXl, color = GearText.primary)
                    Text(" / day", style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
        }
        if (price != Demo.SUGGESTED_PRICE) {
            VGap(Space.s)
            GearLink("Use the suggested ${lkr(Demo.SUGGESTED_PRICE)}", { vm.setPrice(Demo.SUGGESTED_PRICE) })
        }

        VGap(Space.l)
        Eyebrow("Where you sit")
        VGap(Space.s)
        MarketBand(price)

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
        Eyebrow("Seasonal rates")
        VGap(Space.s)
        SeasonCurve(Modifier.fillMaxWidth().height(120.dp))
        VGap(Space.s)
        Text("Surf season, June to August, is highlighted. Seasonal pricing arrives in the next release.", style = GearType.bodyS, color = GearText.tertiary)

        VGap(Space.l)
        Eyebrow("Deposit")
        VGap(Space.s)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(lkr(item.deposit), style = GearType.dataL, color = GearText.primary)
                Text("50% of the item's current value", style = GearType.bodyS, color = GearText.tertiary)
            }
        }
        VGap(Space.s)
        ToggleRow(
            "Waive deposit for renters with Plus protection",
            item.waiveDepositWithPlus,
            { vm.setWaiveDeposit(it) },
        )

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
            Text(
                "At ${Demo.OCCUPANCY}% occupancy you'd earn about ${lkr(price * 30 * Demo.OCCUPANCY / 100 / 2)} / month",
                style = GearType.bodyS,
                color = GearText.body,
            )
        }
        VGap(Space.section)
    }
}

@Composable
private fun ConfidenceBand() {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(GearShape.full)
                .background(GearColor.Pine900.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .padding(start = 40.dp)
                    .fillMaxWidth(0.45f)
                    .height(8.dp)
                    .clip(GearShape.full)
                    .background(GearColor.Pine800)
            )
        }
        VGap(4.dp)
        Row(Modifier.fillMaxWidth()) {
            Text(lkr(Demo.priceBand.first), style = GearType.dataS, color = GearColor.Pine900)
            Spacer(Modifier.weight(1f))
            Text(lkr(Demo.priceBand.second), style = GearType.dataS, color = GearColor.Pine900)
        }
    }
}

@Composable
private fun MarketBand(price: Int) {
    val min = 1_200f
    val max = 4_600f
    val fraction = ((price - min) / (max - min)).coerceIn(0f, 1f)
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
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val x = size.width * f
                    drawLine(
                        tickColor,
                        Offset(x, size.height / 2 - 9.dp.toPx()),
                        Offset(x, size.height / 2 + 9.dp.toPx()),
                        1.5.dp.toPx(),
                    )
                }
                val x = (size.width * fraction).coerceIn(6.dp.toPx(), size.width - 6.dp.toPx())
                drawCircle(markerFill, 9.dp.toPx(), Offset(x, size.height / 2))
                drawCircle(
                    markerEdge, 9.dp.toPx(), Offset(x, size.height / 2),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Min ${grouped(min.toInt())}", style = GearType.dataS, color = GearText.tertiary)
            Spacer(Modifier.weight(1f))
            Text("Median 2,650", style = GearType.dataS, color = GearText.tertiary)
            Spacer(Modifier.weight(1f))
            Text("Max ${grouped(max.toInt())}", style = GearType.dataS, color = GearText.tertiary)
        }
    }
}

@Composable
private fun SeasonCurve(modifier: Modifier = Modifier) {
    val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val curveColor = GearColor.Pine800
    val seasonBand = GearColor.HiVis200
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val values = Demo.seasonCurve
            val step = size.width / (values.size - 1)
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i * step
                val y = size.height - v * size.height * 0.86f - 4f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, curveColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            // Surf season highlight.
            drawRect(
                seasonBand.copy(alpha = 0.5f),
                topLeft = Offset(step * 5, 0f),
                size = androidx.compose.ui.geometry.Size(step * 3, size.height),
            )
        }
        VGap(4.dp)
        Row(Modifier.fillMaxWidth()) {
            months.forEach {
                Text(
                    it,
                    style = GearType.labelXs,
                    color = GearText.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
