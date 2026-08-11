package com.gearloop.app.screens.owner

import com.gearloop.app.ui.theme.GearSurface
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.gearloop.app.ui.theme.Border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
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
/* ------------------------------------------------------------------ S-25 */

private data class Slot(val label: String, val photo: Int?, val quality: String?, val warn: Boolean = false)

/**
 * A small count as a word, for running prose.
 *
 * The angle count is derived from the capture set rather than typed, and a bare
 * numeral mid-sentence ("7 angles. The same 7 a renter…") reads like a spec
 * sheet. Numbers a user acts on are set in mono; this one is a sentence.
 */
private fun spelled(n: Int): String = when (n) {
    in 1..10 -> listOf(
        "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    )[n - 1]
    else -> "$n"
}

@Composable
fun S25CreateListingPhotos(nav: Nav) {
    // The angles the *handover* asks for, in the same order and under the same
    // names. This screen used to list six of its own — "Rails" and "Fins" where
    // capture wants "Rail L", "Rail R" and "Fin box" — while its own subtitle
    // promised they were "the same six a renter checks against".
    val shot = mapOf(
        "Deck" to (Demo.torq.photos[0] to "Good"),
        "Bottom" to (Demo.torq.photos[4] to "Good"),
        "Nose" to (Demo.torq.photos[1] to "Dark — retake?"),
    )
    val slots = Demo.captureAngles.map { angle ->
        val taken = shot[angle.label]
        Slot(angle.label, taken?.first, taken?.second, warn = taken?.second == "Dark — retake?")
    }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton(
                        "Save draft", { }, Modifier.weight(1f),
                        type = ButtonType.GHOST, size = ButtonSize.M,
                    )
                    GearButton("Continue", { nav.to("s26") }, Modifier.weight(1.4f), size = ButtonSize.M)
                }
            }
        },
    ) {
        ProgressStepper(step = 2, total = 8)
        VGap(Space.l)
        Text("Show us the board", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.s)
        val count = spelled(Demo.captureAngles.size)
        Text(
            "${count.replaceFirstChar { it.uppercase() }} angles. " +
                "The same $count a renter checks against at handover.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.m)

        slots.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                pair.forEachIndexed { i, slot ->
                    PhotoSlot(slot, isHero = slot === slots.first(), modifier = Modifier.weight(1f))
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
        VGap(Space.section)
    }
}

@Composable
private fun PhotoSlot(slot: Slot, isHero: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clip(GearShape.sm)
                .then(
                    if (slot.photo == null) {
                        Modifier.border(Border.medium, GearColor.Chalk300, GearShape.sm)
                    } else Modifier
                )
                .background(if (slot.photo == null) GearColor.Chalk50 else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (slot.photo != null) {
                GearImage(slot.photo, slot.label, Modifier.fillMaxSize(), GearShape.sm)
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
                        GearIcon(
                            if (slot.warn) Lucide.TriangleAlert else Lucide.Check,
                            null, size = 12.dp,
                            tint = if (slot.warn) GearColor.Warning500 else GearColor.Success500,
                        )
                        Text(
                            slot.quality.orEmpty(),
                            style = GearType.labelXs,
                            color = if (slot.warn) GearColor.Warning700 else GearColor.Success500,
                        )
                    }
                }
            } else {
                // Just the affordance. The angle is named in the caption under
                // every tile, filled or empty, so printing it inside an empty
                // one as well said the same word twice, 12 dp apart.
                GearIcon(Lucide.Camera, "Add the ${slot.label} photo", size = IconSize.l, tint = GearColor.Chalk400)
            }
        }
        VGap(5.dp)
        Text(slot.label.uppercase(), style = GearType.labelXs, color = GearText.tertiary)
    }
}

/* ------------------------------------------------------------------ S-26 */

@Composable
fun S26CreateListingPricing(nav: Nav) {
    var price by rememberSaveable { mutableIntStateOf(Demo.SUGGESTED_PRICE) }
    val discounts = rememberSavedStateList { Demo.durationDiscounts.map { it.third } }
    var waiveDeposit by rememberSaveable { mutableIntStateOf(0) }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton("Continue", { nav.to("s24") }, Modifier.fillMaxWidth())
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
                onMinus = { price = (price - 100).coerceAtLeast(200) },
                onPlus = { price = price + 100 },
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("LKR ", style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.padding(bottom = 5.dp))
                    Text(grouped(price), style = GearType.dataXl, color = GearText.primary)
                    Text(" / day", style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
        }

        VGap(Space.l)
        Eyebrow("Where you sit")
        VGap(Space.s)
        MarketBand(price)

        VGap(Space.l)
        Eyebrow("Duration discounts")
        VGap(Space.s)
        Demo.durationDiscounts.forEachIndexed { i, (label, pct, _) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = GearType.labelL, color = GearText.primary, modifier = Modifier.width(88.dp))
                Text(pct, style = GearType.dataM, color = GearColor.Success500)
                Spacer(Modifier.weight(1f))
                Toggle(discounts[i], { discounts[i] = it })
            }
        }

        VGap(Space.l)
        Eyebrow("Seasonal rates")
        VGap(Space.s)
        SeasonCurve(Modifier.fillMaxWidth().height(120.dp))
        VGap(Space.s)
        GearButton("Edit curve", { }, type = ButtonType.GHOST, size = ButtonSize.S)

        VGap(Space.l)
        Eyebrow("Deposit")
        VGap(Space.s)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(lkr(25_000), style = GearType.dataL, color = GearText.primary)
                Text("50% of the board's current value", style = GearType.bodyS, color = GearText.tertiary)
            }
        }
        VGap(Space.s)
        ToggleRow(
            "Waive deposit for renters with Plus protection",
            waiveDeposit == 1,
            { waiveDeposit = if (it) 1 else 0 },
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

