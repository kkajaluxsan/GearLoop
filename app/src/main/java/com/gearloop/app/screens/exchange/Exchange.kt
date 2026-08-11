package com.gearloop.app.screens.exchange

import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Person
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/* ------------------------------------------------------------------ S-19 */

@Composable
fun S19SwapMatchFeed(nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Lazy: each match card carries both sides of the trade as photographs.
    GearLazyScreen(
        appBar = {
            AppBar(
                title = "Swap",
                actions = { GearIconButton(Lucide.CircleHelp, "About swapping", { }, size = 40.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.SWAP) },
    ) {
        item(key = "header") {
            VGap(Space.s)
            SegmentedControl(listOf("Matches", "Browse", "My swaps"), tab) { tab = it }

            VGap(Space.m)
            Banner(
                text = "We found 6 trades using your Burton Custom 158.",
                kind = BannerKind.INFO,
            )
            VGap(Space.m)
        }

        items(Demo.swapMatches, key = { it.give.id + it.get.id }) { offer ->
            if (offer.threeWay) {
                ThreeWayCard(offer.counterparty) { nav.to("s23") }
            } else {
                SwapCard(offer, onPropose = { nav.to("s20") })
            }
            VGap(Space.listGap)
        }

        item(key = "footer") { VGap(Space.section) }
    }
}

@Composable
private fun ThreeWayCard(with: Person, onClick: () -> Unit) {
    GearCard(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        borderColor = GearColor.Marine500,
        borderWidth = Border.medium,
    ) {
        Column(Modifier.padding(Space.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GearIcon(Lucide.Workflow, null, size = IconSize.s, tint = GearColor.Marine500)
                Text("3-WAY TRADE", style = GearType.labelXs, color = GearColor.Marine500)
            }
            VGap(Space.sm)
            LoopDiagram(Modifier.fillMaxWidth().height(190.dp), compact = true)
            VGap(Space.sm)
            Text(
                "None of you can trade directly. Together, all three of you get what you want.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.sm)
            GearButton(
                "See the loop", onClick, Modifier.fillMaxWidth(),
                type = ButtonType.SECONDARY, size = ButtonSize.M,
            )
        }
    }
}

/* ------------------------------------------------------------------ S-20 */

@Composable
fun S20BuildProposal(nav: Nav) {
    var mine by rememberSaveable { mutableIntStateOf(0) }
    var theirs by rememberSaveable { mutableIntStateOf(0) }

    val myGear = listOf(Demo.burton to Demo.BURTON_FMV, Demo.helmet to 6_500, Demo.wetsuit to 9_000)
    val theirGear = listOf(
        Demo.santaCruz to Demo.SANTACRUZ_FMV,
        Demo.climbingRack to 29_000,
        Demo.kayak to 44_000,
    )

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(GearColor.Chalk50)
                        .padding(horizontal = Space.screen, vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("You: ${grouped(myGear[mine].second)}", style = GearType.dataM, color = GearText.primary)
                    Spacer(Modifier.weight(1f))
                    GearIcon(Lucide.ArrowRightLeft, null, size = IconSize.s, tint = GearColor.Marine500)
                    Spacer(Modifier.weight(1f))
                    Text("Them: ${grouped(theirGear[theirs].second)}", style = GearType.dataM, color = GearText.primary)
                }
                StickyFooter {
                    GearButton("Check the balance", { nav.to("s21") }, Modifier.fillMaxWidth())
                }
            }
        },
    ) {
        ProgressStepper(step = 1, total = 3)
        VGap(Space.l)
        Text("What are you offering?", style = GearType.headingXl, color = GearText.primary)

        VGap(Space.l)
        Eyebrow("Your gear")
        VGap(Space.s)
        myGear.forEachIndexed { i, (item, fmv) ->
            SwapPickRow(item, fmv, i == mine) { mine = i }
            VGap(Space.s)
        }
        GearButton(
            "Add something not listed yet", { }, Modifier.fillMaxWidth(),
            type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Plus,
        )

        VGap(Space.l)
        GearDivider()
        VGap(Space.l)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("You want")
            Spacer(Modifier.width(Space.s))
            Text("from ${Demo.dinesh.name}", style = GearType.bodyS, color = GearText.tertiary)
        }
        VGap(Space.s)
        theirGear.forEachIndexed { i, (item, fmv) ->
            SwapPickRow(item, fmv, i == theirs) { theirs = i }
            VGap(Space.s)
        }
        VGap(Space.section)
    }
}

@Composable
private fun SwapPickRow(item: GearItem, fmv: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.md)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) GearColor.Pine800 else GearSurface.border,
                GearShape.md,
            )
            .background(GearSurface.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearRadio(selected)
        Spacer(Modifier.width(Space.s))
        GearImage(item.hero, item.title, Modifier.size(56.dp), GearShape.sm)
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(item.shortTitle, style = GearType.labelL, color = GearText.primary, maxLines = 1)
            VGap(3.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FMV ${grouped(fmv)}", style = GearType.dataM, color = GearText.secondary)
                Spacer(Modifier.width(Space.s))
                StatusBadge("Grade ${item.grade.letter}", GearColor.Chalk600)
            }
        }
    }
}

/* ------------------------------------------------------------------ S-21 */

/**
 * The signature screen.
 *
 * The slider adds cash to the lighter side of the trade, and the beam above it
 * levels out as it moves. That single interaction is what makes the fairness
 * idea legible in about two seconds.
 */
@Composable
fun S21FairnessBoot(nav: Nav) {
    val gap = Demo.SANTACRUZ_FMV - Demo.BURTON_FMV
    var boot by rememberSaveable { mutableFloatStateOf(0f) }
    val bootAmount = (boot * 25_000).roundToInt() / 500 * 500
    val remaining = gap - bootAmount
    // Relative to the more valuable side, the way the readout is phrased.
    val percent = (remaining * 100f / Demo.SANTACRUZ_FMV).roundToInt()

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton("Send proposal", { nav.to("s22") }, Modifier.fillMaxWidth())
            }
        },
    ) {
        VGap(Space.s)
        Text("Is this a fair trade?", style = GearType.headingXl, color = GearText.primary)

        VGap(Space.m)
        FairnessMeter(
            percent = percent,
            leftValue = Demo.BURTON_FMV + bootAmount,
            rightValue = Demo.SANTACRUZ_FMV,
            deltaAmount = remaining,
        )

        VGap(Space.s)
        // The meter above already prints "LKR 19,000 · 31% UNFAIR" in the colour
        // of the zone it is in. This used to repeat both numbers in a second
        // headline and then a third time in the sentence — and the headline was
        // hardcoded amber, so a trade the meter had called red was called amber
        // directly underneath it. One reading, then one plain-English sentence.
        Text(
            text = if (remaining > 0) "Their gear is worth $percent% more"
            else "Both sides are within 10% of each other",
            style = GearType.bodyM,
            color = GearText.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        VGap(Space.l)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            ValuationColumn(
                item = Demo.burton,
                rows = Demo.burtonValuation,
                confidence = "HIGH CONFIDENCE",
                confidenceColor = GearColor.Success500,
                modifier = Modifier.weight(1f),
            )
            ValuationColumn(
                item = Demo.santaCruz,
                rows = Demo.santaCruzValuation,
                // Reads as the same kind of statement as the one beside it.
                // "HIGH CONFIDENCE" next to a bare "MEDIUM" invited the question
                // "medium what?".
                confidence = "MEDIUM CONFIDENCE",
                confidenceColor = GearColor.Warning700,
                modifier = Modifier.weight(1f),
            )
        }

        VGap(Space.l)
        GearDivider()
        VGap(Space.l)

        Text("Add cash to balance it", style = GearType.headingM, color = GearText.primary)
        VGap(Space.s)
        GearSlider(
            value = boot,
            onValueChange = { boot = it },
            valueLabel = lkr(bootAmount),
        )
        VGap(Space.s)
        Text(
            if (bootAmount > 0) "You pay Dinesh ${lkr(bootAmount)}"
            else "Straight swap — no cash either way",
            style = GearType.bodyM,
            color = GearText.primary,
        )
        VGap(6.dp)
        Text(
            "Escrow holds it until both of you confirm the trade is complete.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )

        VGap(Space.m)
        GearButton(
            "Request an independent valuation", { }, Modifier.fillMaxWidth(),
            type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Scale,
        )
        VGap(Space.section)
    }
}

@Composable
private fun ValuationColumn(
    item: GearItem,
    rows: List<Pair<String, String>>,
    confidence: String,
    confidenceColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    GearCard(modifier) {
        Column(Modifier.padding(Space.sm)) {
            GearImage(item.hero, item.title, Modifier.fillMaxWidth().height(72.dp), GearShape.sm)
            VGap(Space.s)
            Text(item.shortTitle, style = GearType.labelM, color = GearText.primary, maxLines = 2)
            VGap(Space.s)
            rows.forEachIndexed { i, (label, value) ->
                val last = i == rows.lastIndex
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        label,
                        style = GearType.bodyS,
                        color = if (last) GearText.primary else GearText.tertiary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        value,
                        style = GearType.dataM,
                        color = if (last) GearText.primary else GearText.secondary,
                    )
                }
                if (last) return@forEachIndexed
                Text("↓", style = GearType.bodyS, color = GearColor.Chalk300)
            }
            VGap(Space.s)
            Text(
                confidence,
                style = GearType.labelXs,
                color = confidenceColor,
                modifier = Modifier
                    .clip(GearShape.xs)
                    .background(confidenceColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ S-22 */

@Composable
fun S22ProposalDetail(nav: Nav) {
    GearScreen(
        appBar = {
            AppBar(
                title = "Swap · ${Demo.SWAP_REF}",
                onBack = { nav.back() },
                actions = {
                    Text(
                        "Expires in 5d 6h",
                        style = GearType.dataS,
                        color = GearColor.Warning700,
                        modifier = Modifier
                            .clip(GearShape.xs)
                            .background(GearColor.Warning100)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                },
            )
        },
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton(
                        "Counter", { }, Modifier.weight(1f),
                        type = ButtonType.SECONDARY, size = ButtonSize.M,
                    )
                    GearButton("Accept", { nav.to("s23") }, Modifier.weight(1f), size = ButtonSize.M)
                }
            }
        },
    ) {
        VGap(Space.m)
        SwapItemCard(Demo.burton, Demo.BURTON_FMV, "YOU GIVE", Demo.nirmal)
        Box(Modifier.fillMaxWidth().padding(vertical = Space.s), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(GearColor.Marine100),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(Lucide.ArrowRightLeft, null, size = IconSize.m, tint = GearColor.Marine500)
            }
        }
        SwapItemCard(Demo.santaCruz, Demo.SANTACRUZ_FMV, "YOU GET", Demo.dinesh)

        VGap(Space.l)
        FairnessMeter(
            percent = 0,
            leftValue = Demo.BURTON_FMV + Demo.BOOT_AMOUNT,
            rightValue = Demo.SANTACRUZ_FMV,
            deltaAmount = 0,
        )
        VGap(Space.s)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .background(GearColor.Success100)
                .padding(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIcon(Lucide.HandCoins, null, size = IconSize.m, tint = GearColor.Success500)
            Spacer(Modifier.width(Space.s))
            Text(
                "${lkr(Demo.BOOT_AMOUNT)} from you, held in escrow",
                style = GearType.bodyM,
                color = GearColor.Success500,
            )
        }

        VGap(Space.l)
        Eyebrow("Terms")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Demo.swapTerms.forEachIndexed { i, (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(label, style = GearType.bodyS, color = GearText.secondary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            value,
                            style = GearType.dataM,
                            color = GearText.primary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                    if (i != Demo.swapTerms.lastIndex) GearDivider()
                }
            }
        }

        VGap(Space.l)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Negotiation")
            Spacer(Modifier.weight(1f))
            Text("ROUND 2 OF 5", style = GearType.dataS, color = GearText.tertiary)
        }
        VGap(Space.s)
        Demo.swapThread.forEach { msg ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (msg.fromMe) Arrangement.End else Arrangement.Start,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth(0.82f)
                        .clip(GearShape.md)
                        .then(
                            if (msg.fromMe) Modifier.background(GearColor.Pine800)
                            else Modifier
                                .border(Border.hair, GearSurface.border, GearShape.md)
                                .background(GearSurface.card)
                        )
                        .padding(Space.sm),
                ) {
                    Text(
                        msg.body,
                        style = GearType.bodyM,
                        color = if (msg.fromMe) GearText.onDark else GearText.body,
                    )
                    VGap(4.dp)
                    Text(
                        msg.time,
                        style = GearType.dataS,
                        color = if (msg.fromMe) GearColor.Chalk400 else GearText.tertiary,
                    )
                }
            }
            VGap(Space.s)
        }
        VGap(Space.section)
    }
}

@Composable
private fun SwapItemCard(item: GearItem, fmv: Int, label: String, owner: Person) {
    GearCard(Modifier.fillMaxWidth()) {
        Column {
            GearImage(item.hero, item.title, Modifier.fillMaxWidth().height(150.dp), GearShape.imageTop)
            Column(Modifier.padding(Space.cardPadding)) {
                Eyebrow(label)
                VGap(6.dp)
                Text(item.title, style = GearType.headingM, color = GearText.primary)
                VGap(Space.s)
                GearTag(item.tagLine)
                VGap(Space.sm)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("FMV ${grouped(fmv)}", style = GearType.dataL, color = GearText.primary)
                    Spacer(Modifier.weight(1f))
                    Avatar(owner, size = 28.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(owner.name, style = GearType.bodyS, color = GearText.secondary)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ S-23 */

@Composable
fun S23CyclePreview(nav: Nav) {
    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton("Accept my part", { nav.back() }, Modifier.fillMaxWidth())
            }
        },
    ) {
        VGap(Space.s)
        Text("A three-way trade", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.s)
        Text(
            "None of you could trade with each other directly. In a loop, all three of you " +
                "get what you wanted.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )

        VGap(Space.m)
        LoopDiagram(Modifier.fillMaxWidth().height(300.dp))

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("You give")
                        VGap(3.dp)
                        Text(Demo.burton.shortTitle, style = GearType.labelL, color = GearText.primary)
                    }
                    GearIcon(Lucide.ArrowRight, null, size = IconSize.m, tint = GearColor.Marine500)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Eyebrow("You get")
                        VGap(3.dp)
                        Text(
                            Demo.santaCruz.shortTitle,
                            style = GearType.labelL,
                            color = GearText.primary,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }

        VGap(Space.l)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Acceptance")
            Spacer(Modifier.weight(1f))
            // "2 / 3", not "2 OF 3": the app counts this way on S-15 and S-28,
            // and a capital O set in mono beside two digits reads as a zero.
            Text("2 / 3", style = GearType.dataL, color = GearText.primary)
        }
        VGap(Space.s)
        AcceptRow(Demo.nirmal, "accepted", true)
        AcceptRow(Demo.dinesh, "accepted", true)
        AcceptRow(Demo.ayesha, "pending", false)

        VGap(Space.m)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.md)
                .background(GearColor.Chalk50)
                .padding(Space.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("71:12:44", style = GearType.dataXl, color = GearText.primary)
            VGap(4.dp)
            Text("ALL THREE MUST ACCEPT", style = GearType.labelXs, color = GearText.tertiary)
        }

        VGap(Space.m)
        Banner(
            text = "Nothing moves unless everyone accepts. If anyone declines or time runs out, " +
                "the whole trade is cancelled and no gear or money changes hands.",
            kind = BannerKind.INFO,
        )

        VGap(Space.m)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .border(Border.hair, GearSurface.border, GearShape.sm)
                .padding(Space.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your delta", style = GearType.bodyS, color = GearText.tertiary)
                Text("+2%", style = GearType.dataL, color = GearColor.Success500)
            }
            Column(Modifier.weight(1f)) {
                Text("Loop total", style = GearType.bodyS, color = GearText.tertiary)
                Text("−4%", style = GearType.dataL, color = GearColor.Success500)
            }
        }
        VGap(Space.section)
    }
}

@Composable
private fun AcceptRow(person: Person, state: String, done: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(person, size = 36.dp)
        Spacer(Modifier.width(Space.sm))
        Text(person.name, style = GearType.labelL, color = GearText.primary, modifier = Modifier.weight(1f))
        GearIcon(
            if (done) Lucide.CircleCheck else Lucide.Clock,
            null,
            size = IconSize.m,
            tint = if (done) GearColor.Success500 else GearColor.Warning500,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            state,
            style = GearType.bodyS,
            color = if (done) GearColor.Success500 else GearColor.Warning700,
        )
    }
}

/**
 * The loop diagram — three people, three curved arrows, one direction of travel.
 * Drawn rather than composed so the arrows can actually curve between nodes.
 */
@Composable
fun LoopDiagram(modifier: Modifier = Modifier, compact: Boolean = false) {
    val people = listOf(Demo.nirmal, Demo.dinesh, Demo.ayesha)
    val gives = listOf(
        Demo.burton.shortTitle to Demo.BURTON_FMV,
        Demo.santaCruz.shortTitle to Demo.SANTACRUZ_FMV,
        Demo.kayak.shortTitle to 44_000,
    )

    // The canvas and the avatars have to agree on where a node *is*. They used
    // to be positioned two different ways — the circles by trigonometry, the
    // labels by Box alignment — so the hi-vis ring was struck through the name
    // under it and an arrowhead landed in the middle of "gives Burton". The
    // geometry is settled once here and both read it.
    val avatarSize = if (compact) 40.dp else 48.dp
    val nodeWidth = if (compact) 96.dp else 116.dp

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val cx = wPx / 2f
        val radius = minOf(wPx, hPx) * 0.30f
        // Derived, not a fraction of the height: the top node's avatar has to
        // clear the top edge, and at the compact size a fraction that worked for
        // the 300dp diagram cropped it.
        val cy = radius + with(density) { (avatarSize / 2 + 4.dp).toPx() }
        // Nodes at 12, 4 and 8 o'clock.
        val centres = listOf(-90.0, 30.0, 150.0).map {
            Offset(
                cx + (radius * cos(Math.toRadians(it))).toFloat(),
                cy + (radius * sin(Math.toRadians(it))).toFloat(),
            )
        }
        // How far an arc has to stop short of a node to clear the ring round it.
        val nodeClearPx = with(density) { (avatarSize / 2 + 8.dp).toPx() }

        // The loop is drawn, so its two colours are read before the draw starts.
        val arcColor = GearColor.Marine500
        val youRing = GearColor.HiVis500
        Canvas(Modifier.fillMaxSize()) {
            val pts = centres

            pts.forEachIndexed { i, from ->
                val to = pts[(i + 1) % 3]
                // Pull the control point outward so the arc bows away from centre.
                val mid = Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
                val ctrl = Offset(mid.x + (mid.x - cx) * 0.42f, mid.y + (mid.y - cy) * 0.42f)

                fun pointAt(t: Float): Offset {
                    val u = 1 - t
                    return Offset(
                        u * u * from.x + 2 * u * t * ctrl.x + t * t * to.x,
                        u * u * from.y + 2 * u * t * ctrl.y + t * t * to.y,
                    )
                }

                // Trim each end back to where the arc actually leaves the ring,
                // rather than to a fixed fraction that only happened to look
                // right at one diagram size.
                fun trimFrom(anchor: Offset, ascending: Boolean): Float {
                    var t = if (ascending) 0f else 1f
                    val step = if (ascending) 0.01f else -0.01f
                    repeat(60) {
                        val p = pointAt(t)
                        val dxa = p.x - anchor.x
                        val dya = p.y - anchor.y
                        if (dxa * dxa + dya * dya >= nodeClearPx * nodeClearPx) return t
                        t += step
                    }
                    return t
                }

                val trimStart = trimFrom(from, ascending = true)
                val trimEnd = trimFrom(to, ascending = false)

                val path = Path().apply {
                    val s = pointAt(trimStart)
                    moveTo(s.x, s.y)
                    var t = trimStart
                    while (t <= trimEnd) {
                        val p = pointAt(t)
                        lineTo(p.x, p.y)
                        t += 0.02f
                    }
                }
                drawPath(
                    path,
                    arcColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )

                // Arrowhead at the end of the trimmed arc.
                val tip = pointAt(trimEnd)
                val before = pointAt(trimEnd - 0.05f)
                val dx = tip.x - before.x
                val dy = tip.y - before.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                val ux = dx / len
                val uy = dy / len
                val head = 9.dp.toPx()
                val arrow = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(tip.x - ux * head - uy * head * 0.55f, tip.y - uy * head + ux * head * 0.55f)
                    lineTo(tip.x - ux * head + uy * head * 0.55f, tip.y - uy * head - ux * head * 0.55f)
                    close()
                }
                drawPath(arrow, arcColor)
            }

            // Your node carries a hi-vis ring, just outside the avatar.
            drawCircle(
                youRing,
                (avatarSize / 2).toPx() + 5.dp.toPx(),
                centres[0],
                style = Stroke(width = 3.dp.toPx()),
            )
        }

        // Avatars and labels, laid over the canvas on the same coordinates.
        val labelStyle = if (compact) GearType.labelXs else GearType.labelM
        centres.forEachIndexed { i, centre ->
            // 0 is Nirmal at the top, then Dinesh and Ayesha clockwise — the
            // order the arcs are drawn in.
            val person = people[i]
            val gives = gives[i]
            LoopNode(
                person = person,
                gives = gives,
                compact = compact,
                labelStyle = labelStyle,
                avatarSize = avatarSize,
                modifier = Modifier
                    .width(nodeWidth)
                    .offset {
                        IntOffset(
                            (centre.x - with(density) { nodeWidth.toPx() } / 2).roundToInt(),
                            (centre.y - with(density) { avatarSize.toPx() } / 2).roundToInt(),
                        )
                    },
            )
        }
    }
}

@Composable
private fun LoopNode(
    person: Person,
    gives: Pair<String, Int>,
    compact: Boolean,
    labelStyle: androidx.compose.ui.text.TextStyle,
    avatarSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(person, size = avatarSize, showTierRing = true)
        // Clears the hi-vis "you" ring, which is drawn 5dp outside the avatar.
        VGap(9.dp)
        Text(
            person.name.split(" ").first(),
            style = labelStyle,
            color = GearText.primary,
            textAlign = TextAlign.Center,
        )
        if (!compact) {
            Text(
                "gives ${gives.first}",
                style = GearType.bodyS,
                color = GearText.tertiary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(grouped(gives.second), style = GearType.dataS, color = GearText.secondary)
        }
    }
}
