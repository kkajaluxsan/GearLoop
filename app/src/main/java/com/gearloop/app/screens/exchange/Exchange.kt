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
import com.gearloop.app.ui.theme.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.Demo
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Person
import com.gearloop.app.data.ProposalStatus
import com.gearloop.app.data.countdown
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.CycleLeg
import com.gearloop.app.data.repo.Proposal
import com.gearloop.app.data.stopwatch
import com.gearloop.app.nav.Nav
import com.gearloop.app.container
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
import com.gearloop.app.ui.vm.ExchangeSupplyViewModel
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** No proposal in hand: the screens below start from a match on S-19. */
@Composable
private fun NoProposal(nav: Nav) {
    GearScreen(appBar = { AppBar(title = "Swap", onBack = { nav.back() }) }) {
        EmptyState(
            icon = Lucide.ArrowRightLeft,
            headline = "No swap under way",
            body = "Pick a match on the Swap tab and a proposal starts here.",
            actionLabel = "See matches",
            onAction = { nav.tab("s19") },
        )
    }
}

/* ------------------------------------------------------------------ S-19 */

/**
 * S-19 · Read the matches for the renter's own gear, and their proposals under
 * "My swaps"; Create a proposal from a match (or reopen the one already under
 * way); hide a match that isn't for them, with Undo.
 */
@Composable
fun S19SwapMatchFeed(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val discovery: FitDiscoveryViewModel = gearViewModel()
    val matches by vm.matches.collectAsStateWithLifecycle()
    val proposals by vm.proposals.collectAsStateWithLifecycle()
    val listings by vm.listings.collectAsStateWithLifecycle()
    val cycle by vm.cycle.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var about by rememberSaveable { mutableStateOf(false) }
    val me = currentMe()
    // Looks for new trades for whatever the member has listed, quietly.
    LaunchedEffect(me) { vm.refreshMatches() }

    // Lazy: each match card carries both sides of the trade as photographs.
    GearLazyScreen(
        appBar = {
            AppBar(
                title = "Swap",
                actions = { GearIconButton(Lucide.CircleHelp, "About swapping", { about = true }, size = 44.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.SWAP) },
    ) {
        item(key = "header") {
            VGap(Space.s)
            SegmentedControl(listOf("Matches", "Browse", "My swaps (${proposals.size})"), tab) { tab = it }
            VGap(Space.m)
        }

        when (tab) {
            0 -> {
                item(key = "banner") {
                    val gives = matches.map { it.offer.give.shortTitle }.distinct()
                    if (matches.isEmpty()) {
                        EmptyState(
                            icon = Lucide.ArrowRightLeft,
                            headline = if (vm.myGear().isEmpty()) "Nothing to swap yet" else "No trades right now",
                            body = if (vm.myGear().isEmpty()) "List something you own and trades for it appear here."
                            else "Check back as more gear is listed.",
                            actionLabel = if (vm.myGear().isEmpty()) "Go to your closet" else "Look again",
                            onAction = { if (vm.myGear().isEmpty()) nav.to("s36") else vm.refreshMatches(announce = true) },
                        )
                    } else {
                        Banner(
                            text = "We found ${matches.size} ${if (matches.size == 1) "trade" else "trades"} using your " +
                                if (gives.size == 1) "${gives.first()}." else "gear.",
                            kind = BannerKind.INFO,
                            action = "Look again",
                            onAction = { vm.refreshMatches(announce = true) },
                        )
                    }
                    VGap(Space.m)
                }
                items(matches, key = { it.id }) { match ->
                    val offer = match.offer
                    if (offer.threeWay) {
                        ThreeWayCard(cycle?.legs) { nav.to("s23") }
                    } else {
                        SwapCard(
                            offer,
                            onPropose = { vm.propose(match.id) { nav.to("s20") } },
                            proposeLabel = if (match.proposalRef != null) "Continue proposal" else "Propose swap",
                            onHide = { vm.hideMatch(match.id, offer.get.shortTitle) },
                        )
                    }
                    VGap(Space.listGap)
                }
            }
            1 -> {
                items(listings.filter { it.owner.handle != me && it.status == com.gearloop.app.data.ListingStatus.PUBLISHED }, key = { it.id }) { item ->
                    ListingCard(
                        item, { discovery.select(item); nav.to("s07") }, layout = ListingLayout.LIST,
                        onToggleSave = { discovery.toggleSaved(item) },
                    )
                    VGap(Space.listGap)
                }
            }
            else -> {
                if (proposals.isEmpty()) {
                    item(key = "none") {
                        EmptyState(
                            icon = Lucide.ArrowRightLeft,
                            headline = "No proposals yet",
                            body = "Swaps you propose from a match appear here.",
                        )
                    }
                }
                items(proposals, key = { it.ref }) { p ->
                    ProposalRow(p) { vm.selectProposal(p.ref); nav.to("s22") }
                    VGap(Space.s)
                }
            }
        }

        item(key = "footer") { VGap(Space.section) }
    }

    if (about) {
        GearDialog(
            title = "How swapping works",
            onDismiss = { about = false },
            confirmLabel = "Got it",
            onConfirm = { about = false },
            dismissLabel = "Close",
        ) {
            Text(
                "Both items are valued from their new price, age and condition grade. " +
                    "If one is worth more, cash balances it, held in escrow until you both confirm. " +
                    "A three-way loop is offered when no two-way trade works.",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
    }
}

@Composable
private fun ProposalRow(p: Proposal, onClick: () -> Unit) {
    GearCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            GearImage(p.get.hero, p.get.title, Modifier.size(56.dp), GearShape.sm)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text("${p.give.shortTitle} → ${p.get.shortTitle}", style = GearType.labelL, color = GearText.primary, maxLines = 1)
                VGap(3.dp)
                Text(
                    "${p.ref} · ${p.counterparty.name.substringBefore(' ')} · round ${p.round}".uppercase(),
                    style = GearType.dataS, color = GearText.tertiary,
                )
            }
            StatusBadge(
                p.status.label,
                when (p.status) {
                    ProposalStatus.ACCEPTED -> GearColor.Success500
                    ProposalStatus.COUNTERED -> GearColor.Warning700
                    ProposalStatus.DECLINED, ProposalStatus.WITHDRAWN -> GearColor.Chalk600
                    else -> GearColor.Marine500
                },
            )
        }
    }
}

@Composable
private fun ThreeWayCard(legs: List<CycleLeg>?, onClick: () -> Unit) {
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
            LoopDiagram(Modifier.fillMaxWidth().height(190.dp), compact = true, legs = legs)
            VGap(Space.sm)
            Text(
                "No two-way trade works, but a loop of three does.",
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

/** Building a proposal: what is offered (S-20), then the cash that balances it (S-21). */
private const val SWAP_STEPS = 2

/**
 * S-20 · Read the proposal and both sides' gear; Update what is given and what
 * is wanted. The values beside each item are the same fair-market values S-21
 * shows the working for.
 */
@Composable
fun S20BuildProposal(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val proposalState by vm.proposal.collectAsStateWithLifecycle()
    val p = proposalState ?: return NoProposal(nav)
    val myGear = vm.myGear()
    val theirGear = vm.theirGear(p)

    GearScreen(
        appBar = { AppBar(title = p.ref, onBack = { nav.back() }) },
        footer = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(GearColor.Chalk50)
                        .padding(horizontal = Space.screen, vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("You: ${grouped(p.giveValue)}", style = GearType.dataM, color = GearText.primary)
                    Spacer(Modifier.weight(1f))
                    GearIcon(Lucide.ArrowRightLeft, null, size = IconSize.s, tint = GearColor.Marine500)
                    Spacer(Modifier.weight(1f))
                    Text("Them: ${grouped(p.getValue)}", style = GearType.dataM, color = GearText.primary)
                }
                StickyFooter {
                    GearButton("Check the balance", { nav.to("s21") }, Modifier.fillMaxWidth(), enabled = p.open)
                }
            }
        },
    ) {
        // Two steps, this and the balance on S-21. It counted to three, with
        // nothing after the first to say which step was which.
        ProgressStepper(step = 1, total = SWAP_STEPS)
        VGap(Space.l)
        Text("What are you offering?", style = GearType.headingXl, color = GearText.primary)

        VGap(Space.l)
        Eyebrow("Your gear")
        VGap(Space.s)
        myGear.forEach { item ->
            SwapPickRow(item, vm.valueOf(item), item.id == p.give.id) { vm.setGive(item) }
            VGap(Space.s)
        }
        GearButton(
            "Add something not listed yet", { nav.to("s36") }, Modifier.fillMaxWidth(),
            type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Plus,
        )

        VGap(Space.l)
        GearDivider()
        VGap(Space.l)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("You want")
            Spacer(Modifier.width(Space.s))
            Text("from ${p.counterparty.name}", style = GearType.bodyS, color = GearText.tertiary)
        }
        VGap(Space.s)
        theirGear.forEach { item ->
            SwapPickRow(item, vm.valueOf(item), item.id == p.get.id) { vm.setGet(item) }
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
            .clickable(role = Role.RadioButton, onClick = onClick)
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
                // "Worth", not "FMV": the abbreviation meant nothing to anyone
                // who doesn't trade for a living.
                Text("Worth ${lkr(fmv)}", style = GearType.dataM, color = GearText.secondary)
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
 *
 * S-21 · Read the proposal and both valuations; Update the cash that balances
 * it; Update the proposal to sent, or sent back as a counter.
 */
@Composable
fun S21FairnessBoot(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val proposalState by vm.proposal.collectAsStateWithLifecycle()
    val p = proposalState ?: return NoProposal(nav)
    val gap = p.getValue - p.giveValue
    // The slider runs a little past the gap, so the renter can offer more than
    // the arithmetic asks — never below zero.
    val max = maxOf(25_000, gap + 6_000)
    // The drag is held on screen and written once per 500-rupee step, not on
    // every pixel.
    var boot by rememberSaveable(p.ref) { mutableStateOf(p.boot.toFloat() / max) }
    val bootAmount = (boot * max).roundToInt() / 500 * 500
    val remaining = gap - bootAmount
    // Relative to the more valuable side, the way the readout is phrased.
    val percent = (remaining * 100f / maxOf(p.getValue, 1)).roundToInt()
    val proposer = p.proposer.handle == currentMe()
    val them = (if (proposer) p.counterparty else p.proposer).name.substringBefore(' ')

    GearScreen(
        appBar = { AppBar(title = p.ref, onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    when {
                        !proposer -> "Send counter-offer"
                        p.status == ProposalStatus.COUNTERED -> "Send counter-offer"
                        p.status == ProposalStatus.SENT -> "Update proposal"
                        else -> "Send proposal"
                    },
                    { vm.send(bootAmount) { nav.to("s22") } },
                    Modifier.fillMaxWidth(),
                    enabled = p.open,
                )
            }
        },
    ) {
        VGap(Space.s)
        // Step 2 while the proposal is being built; a counter-offer is a reply, not a step.
        if (p.status == ProposalStatus.DRAFT) {
            ProgressStepper(step = 2, total = SWAP_STEPS)
            VGap(Space.l)
        }
        Text("Is this a fair trade?", style = GearType.headingXl, color = GearText.primary)

        VGap(Space.m)
        FairnessMeter(
            percent = percent,
            leftValue = p.giveValue + bootAmount,
            rightValue = p.getValue,
            deltaAmount = remaining,
        )

        VGap(Space.s)
        Text(
            text = when {
                remaining > 0 -> "Their gear is worth $percent% more"
                remaining < 0 -> "Your side is worth ${-percent}% more"
                else -> "Both sides are level"
            },
            style = GearType.bodyM,
            color = GearText.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        VGap(Space.l)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            ValuationColumn(
                item = p.give,
                rows = vm.valuationRows(p.give),
                confidence = "HIGH CONFIDENCE",
                confidenceColor = GearColor.Success500,
                modifier = Modifier.weight(1f),
            )
            ValuationColumn(
                item = p.get,
                rows = vm.valuationRows(p.get),
                // Reads as the same kind of statement as the one beside it.
                confidence = "MEDIUM CONFIDENCE",
                confidenceColor = GearColor.Warning700,
                modifier = Modifier.weight(1f),
            )
        }

        VGap(Space.l)
        GearDivider()
        VGap(Space.l)

        Text(if (proposer) "Add cash to balance it" else "Cash $them adds to balance it", style = GearType.headingM, color = GearText.primary)
        VGap(Space.s)
        GearSlider(
            value = boot,
            onValueChange = { boot = it },
            valueLabel = lkr(bootAmount),
            label = if (proposer) "Cash you add" else "Cash $them adds",
        )
        VGap(Space.s)
        Text(
            when {
                bootAmount == 0 -> "Straight swap — no cash either way"
                proposer -> "You pay $them ${lkr(bootAmount)}"
                else -> "$them pays you ${lkr(bootAmount)}"
            },
            style = GearType.bodyM,
            color = GearText.primary,
        )
        VGap(6.dp)
        Text("Held in escrow until you both confirm.", style = GearType.bodyS, color = GearText.tertiary)

        VGap(Space.m)
        GearButton(
            "Request an independent valuation", { vm.requestValuation() }, Modifier.fillMaxWidth(),
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

/**
 * S-22 · Read the proposal, its terms and the negotiation; Update it — accept
 * the counterparty's terms, or decline; Delete a draft, or withdraw one that was
 * sent. Countering goes back to S-21 to move the cash.
 */
@Composable
fun S22ProposalDetail(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val proposalState by vm.proposal.collectAsStateWithLifecycle()
    val thread by vm.negotiation.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    var withdrawing by rememberSaveable { mutableStateOf(false) }
    val p = proposalState ?: return NoProposal(nav)
    val meHandle = currentMe()
    val proposer = p.proposer.handle == meHandle
    val other = if (proposer) p.counterparty else p.proposer
    val me = people[meHandle] ?: p.proposer
    val them = other.name.substringBefore(' ')
    val myMove = vm.myMove(p)
    val left = ChronoUnit.MILLIS.between(AppClock.now(), p.expiresAt)

    GearScreen(
        appBar = {
            AppBar(
                title = "Swap · ${p.ref}",
                onBack = { nav.back() },
                actions = {
                    if (p.open) {
                        Text(
                            "EXPIRES IN ${countdown(left).uppercase()}",
                            style = GearType.dataS,
                            color = GearColor.Warning700,
                            modifier = Modifier
                                .clip(GearShape.xs)
                                .background(GearColor.Warning100)
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                        )
                    }
                },
            )
        },
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    when {
                        p.open && myMove && p.status != ProposalStatus.DRAFT -> {
                            GearButton("Decline", { vm.decline() }, type = ButtonType.GHOST, size = ButtonSize.M)
                            GearButton("Counter", { nav.to("s21") }, Modifier.weight(1f), type = ButtonType.SECONDARY, size = ButtonSize.M)
                            GearButton("Accept", { vm.accept() }, Modifier.weight(1f), size = ButtonSize.M)
                        }
                        p.status == ProposalStatus.SENT || p.status == ProposalStatus.COUNTERED -> {
                            if (proposer) GearButton("Withdraw", { withdrawing = true }, Modifier.weight(1f), type = ButtonType.SECONDARY, size = ButtonSize.M)
                            GearButton("Message $them", { vm.openThread(other.handle); nav.to("s33") }, Modifier.weight(1f), size = ButtonSize.M)
                        }
                        p.status == ProposalStatus.DRAFT -> {
                            GearButton("Delete draft", { withdrawing = true }, Modifier.weight(1f), type = ButtonType.SECONDARY, size = ButtonSize.M)
                            GearButton("Continue", { nav.to("s21") }, Modifier.weight(1f), size = ButtonSize.M)
                        }
                        p.status == ProposalStatus.ACCEPTED -> GearButton(
                            "Message $them", { vm.openThread(other.handle); nav.to("s33") }, Modifier.fillMaxWidth(), size = ButtonSize.M,
                        )
                        else -> GearButton("Back to matches", { nav.tab("s19") }, Modifier.fillMaxWidth(), size = ButtonSize.M, type = ButtonType.SECONDARY)
                    }
                }
            }
        },
    ) {
        VGap(Space.m)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(
                p.status.label,
                when (p.status) {
                    ProposalStatus.ACCEPTED -> GearColor.Success500
                    ProposalStatus.COUNTERED -> GearColor.Warning700
                    ProposalStatus.DECLINED, ProposalStatus.WITHDRAWN -> GearColor.Chalk600
                    else -> GearColor.Marine500
                },
            )
            Spacer(Modifier.width(Space.s))
            Text(
                when {
                    p.open && myMove && p.status != ProposalStatus.DRAFT -> "$them has answered. Your move."
                    p.status == ProposalStatus.SENT || p.status == ProposalStatus.COUNTERED -> "Waiting for $them."
                    p.status == ProposalStatus.DRAFT -> "Not sent yet."
                    p.status == ProposalStatus.ACCEPTED -> "Agreed. Arrange the handover."
                    else -> "This proposal is closed."
                },
                style = GearType.bodyS,
                color = GearText.secondary,
            )
        }
        VGap(Space.m)
        SwapItemCard(if (proposer) p.give else p.get, if (proposer) p.giveValue else p.getValue, "YOU GIVE", me)
        Box(Modifier.fillMaxWidth().padding(vertical = Space.s), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(GearColor.Marine100),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(Lucide.ArrowRightLeft, null, size = IconSize.m, tint = GearColor.Marine500)
            }
        }
        SwapItemCard(if (proposer) p.get else p.give, if (proposer) p.getValue else p.giveValue, "YOU GET", other)

        VGap(Space.l)
        if (proposer) {
            FairnessMeter(
                percent = (p.remainingGap * 100f / maxOf(p.getValue, 1)).roundToInt(),
                leftValue = p.giveValue + p.boot,
                rightValue = p.getValue,
                deltaAmount = p.remainingGap,
            )
        } else {
            FairnessMeter(
                percent = (-p.remainingGap * 100f / maxOf(p.giveValue + p.boot, 1)).roundToInt(),
                leftValue = p.getValue,
                rightValue = p.giveValue + p.boot,
                deltaAmount = -p.remainingGap,
            )
        }
        if (p.boot > 0) {
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
                    if (proposer) "${lkr(p.boot)} from you, held in escrow" else "${lkr(p.boot)} from $them, held in escrow",
                    style = GearType.bodyM,
                    color = GearColor.Success500,
                )
            }
        }

        VGap(Space.l)
        Eyebrow("Terms")
        VGap(Space.s)
        val terms = listOf(
            "Type" to "Permanent",
            "Inspection window" to "48 hours",
            "Ownership" to "Transfers on completion",
            "Escrow" to if (p.boot > 0) "Holds your ${lkr(p.boot)} until both confirm" else "Nothing to hold",
        )
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                terms.forEachIndexed { i, (label, value) ->
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
                    if (i != terms.lastIndex) GearDivider()
                }
            }
        }

        VGap(Space.l)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Negotiation")
            Spacer(Modifier.weight(1f))
            Text("ROUND ${p.round} OF 5", style = GearType.dataS, color = GearText.tertiary)
        }
        VGap(Space.s)
        if (thread.isEmpty()) {
            Text("Nothing said yet.", style = GearType.bodyS, color = GearText.tertiary)
        }
        thread.forEach { msg ->
            val chat = msg.chat
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (chat.fromMe) Arrangement.End else Arrangement.Start,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth(0.82f)
                        .clip(GearShape.md)
                        .then(
                            if (chat.fromMe) Modifier.background(GearColor.Pine800)
                            else Modifier
                                .border(Border.hair, GearSurface.border, GearShape.md)
                                .background(GearSurface.card)
                        )
                        .padding(Space.sm),
                ) {
                    Text(
                        chat.body,
                        style = GearType.bodyM,
                        color = if (chat.fromMe) GearText.onDark else GearText.body,
                    )
                    VGap(4.dp)
                    Text(
                        chat.time.uppercase(),
                        style = GearType.dataS,
                        color = if (chat.fromMe) GearColor.Chalk400 else GearText.tertiary,
                    )
                }
            }
            VGap(Space.s)
        }
        VGap(Space.section)
    }

    if (withdrawing) {
        GearDialog(
            title = if (p.status == ProposalStatus.DRAFT) "Delete this draft?" else "Withdraw this proposal?",
            onDismiss = { withdrawing = false },
            confirmLabel = "Keep it",
            onConfirm = { withdrawing = false },
            destructiveLabel = if (p.status == ProposalStatus.DRAFT) "Delete" else "Withdraw proposal",
            onDestructive = { withdrawing = false; vm.withdraw { nav.tab("s19") } },
        ) {
            Text(
                if (p.status == ProposalStatus.DRAFT) "Nothing has been sent to $them."
                else "You can propose again from the match.",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
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
                    Text("Worth ${lkr(fmv)}", style = GearType.dataL, color = GearText.primary)
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

/**
 * S-23 · Read the loop and who has accepted; Update the renter's own seat —
 * accept, decline, or take the answer back.
 *
 * Milestone 01's most valuable failure: three of four participants could not
 * say who ends up with what (F-08, FR-13). The screen now leads with the
 * outcome in one sentence, then the trade as three numbered steps, and only
 * then the loop drawing. It also fixes what the old card said: "You get" named
 * the Santa Cruz, which in this loop goes to Ayesha (D-02).
 */
@Composable
fun S23CyclePreview(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val cycleState by vm.cycle.collectAsStateWithLifecycle()
    val cycle = cycleState ?: return NoProposal(nav)
    val legs = cycle.legs
    val me = currentMe()
    val mine = cycle.members.firstOrNull { it.person.handle == me }
    val gets = cycle.receivedBy(me)
    val myAnswer = mine?.accepted
    val delta = if (mine != null && gets != null) ((gets.givesValue - mine.givesValue) * 100f / mine.givesValue).roundToInt() else 0

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    when (myAnswer) {
                        null -> {
                            GearButton("Decline", { vm.setMyAcceptance(false) }, type = ButtonType.SECONDARY, size = ButtonSize.M)
                            GearButton("Accept my part", { vm.setMyAcceptance(true) }, Modifier.weight(1f), size = ButtonSize.M)
                        }
                        else -> GearButton(
                            if (myAnswer) "Take my acceptance back" else "Undo my decline",
                            { vm.setMyAcceptance(null) }, Modifier.fillMaxWidth(),
                            type = ButtonType.SECONDARY, size = ButtonSize.M,
                        )
                    }
                }
            }
        },
    ) {
        VGap(Space.s)
        Text("A three-way trade", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.s)
        // The outcome first, in one sentence, before any of the mechanism.
        if (mine != null && gets != null) {
            Text(
                "You give your ${mine.gives.shortTitle} to ${legs.first { it.giver.handle == me }.receiver.name.substringBefore(' ')} " +
                    "and get ${gets.person.name.substringBefore(' ')}'s ${gets.gives.shortTitle}. No cash changes hands.",
                style = GearType.headingS,
                color = GearText.primary,
            )
        }

        VGap(Space.m)
        Eyebrow("How it moves")
        VGap(Space.s)
        legs.forEachIndexed { i, leg ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = GearType.dataL, color = GearColor.Marine500, modifier = Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${name(leg.giver)} → ${name(leg.receiver)}",
                        style = GearType.labelL,
                        color = GearText.primary,
                    )
                    Text(leg.item.shortTitle, style = GearType.bodyS, color = GearText.secondary)
                }
                Text(grouped(leg.value), style = GearType.dataM, color = GearText.secondary)
            }
            if (i != legs.lastIndex) GearDivider()
        }

        VGap(Space.m)
        LoopDiagram(Modifier.fillMaxWidth().height(300.dp), legs = legs)

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("You give")
                        VGap(3.dp)
                        Text(mine?.gives?.shortTitle ?: "", style = GearType.labelL, color = GearText.primary)
                    }
                    GearIcon(Lucide.ArrowRight, null, size = IconSize.m, tint = GearColor.Marine500)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Eyebrow("You get")
                        VGap(3.dp)
                        Text(
                            gets?.gives?.shortTitle ?: "",
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
            // "2 / 3", not "2 OF 3": a capital O in mono beside digits reads as zero.
            Text("${cycle.acceptedCount} / ${cycle.members.size}", style = GearType.dataL, color = GearText.primary)
        }
        VGap(Space.s)
        cycle.members.forEach { m ->
            AcceptRow(m.person, m.accepted)
        }

        VGap(Space.m)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.md)
                .background(GearColor.Chalk50)
                .padding(Space.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when {
                    cycle.declined -> "Off"
                    cycle.acceptedCount == cycle.members.size -> "Agreed"
                    else -> "Waiting on ${cycle.members.size - cycle.acceptedCount}"
                },
                style = GearType.dataXl,
                color = GearText.primary,
            )
            VGap(4.dp)
            Text(
                when {
                    cycle.declined -> "SOMEONE DECLINED · THE LOOP IS OFF"
                    cycle.acceptedCount == cycle.members.size -> "EVERYONE ACCEPTED · ARRANGE THE HANDOVERS"
                    else -> "ALL THREE MUST ACCEPT"
                },
                style = GearType.labelXs,
                color = GearText.tertiary,
            )
        }

        VGap(Space.m)
        Banner(
            text = "If anyone declines or time runs out, the trade is off and nothing changes hands.",
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
                Text("Your value change", style = GearType.bodyS, color = GearText.tertiary)
                Text(
                    (if (delta >= 0) "+" else "−") + "${kotlin.math.abs(delta)}%",
                    style = GearType.dataL,
                    color = if (delta >= 0) GearColor.Success500 else GearColor.Warning700,
                )
            }
        }
        VGap(Space.section)
    }
}

@Composable
private fun name(p: Person) = if (p.handle == currentMe()) "You" else p.name.substringBefore(' ')

/** The signed-in member's handle, read where a screen needs to say "you". */
@Composable
private fun currentMe(): String =
    androidx.compose.ui.platform.LocalContext.current.container.session.me.collectAsStateWithLifecycle().value

@Composable
private fun AcceptRow(person: Person, accepted: Boolean?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(person, size = 36.dp)
        Spacer(Modifier.width(Space.sm))
        Text(
            if (person.handle == currentMe()) "${person.name} (you)" else person.name,
            style = GearType.labelL, color = GearText.primary, modifier = Modifier.weight(1f),
        )
        GearIcon(
            when (accepted) {
                true -> Lucide.CircleCheck
                false -> Lucide.CircleX
                null -> Lucide.Clock
            },
            null,
            size = IconSize.m,
            tint = when (accepted) {
                true -> GearColor.Success500
                false -> GearColor.Danger500
                null -> GearColor.Warning500
            },
        )
        Spacer(Modifier.width(6.dp))
        Text(
            when (accepted) {
                true -> "accepted"
                false -> "declined"
                null -> "pending"
            },
            style = GearType.bodyS,
            color = when (accepted) {
                true -> GearColor.Success500
                false -> GearColor.Danger700
                null -> GearColor.Warning700
            },
        )
    }
}

/**
 * The loop diagram — three people, three curved arrows, one direction of travel.
 * Drawn rather than composed so the arrows can actually curve between nodes.
 * Each arrow runs from a giver to the person who receives their gear.
 */
@Composable
fun LoopDiagram(modifier: Modifier = Modifier, compact: Boolean = false, legs: List<CycleLeg>? = null) {
    val people = legs?.map { it.giver } ?: listOf(Demo.nirmal, Demo.dinesh, Demo.ayesha)
    val gives = legs?.map { it.item.shortTitle to it.value } ?: listOf(
        Demo.burton.shortTitle to Demo.BURTON_FMV,
        Demo.santaCruz.shortTitle to Demo.SANTACRUZ_FMV,
        Demo.ayeshaKayak.shortTitle to Demo.AYESHA_KAYAK_FMV,
    )

    // The canvas and the avatars have to agree on where a node *is*; the
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
        // clear the top edge at both sizes.
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

                // Trim each end back to where the arc actually leaves the ring.
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
            LoopNode(
                person = people[i],
                gives = gives[i],
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
        val label = if (person.handle == currentMe()) "You" else person.name.split(" ").first()
        Text(
            // labelXs is an uppercase style, so the compact label arrives uppercased.
            if (compact) label.uppercase() else label,
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
