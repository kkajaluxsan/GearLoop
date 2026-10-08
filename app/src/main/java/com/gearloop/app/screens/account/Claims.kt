package com.gearloop.app.screens.account

import com.gearloop.app.ui.theme.Border
import com.gearloop.app.ui.theme.GearSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.countdown
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
import com.gearloop.app.data.stamp
import com.gearloop.app.data.repo.Claim
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
import com.gearloop.app.ui.vm.MoneyViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.temporal.ChronoUnit

/* ------------------------------------------------------------------ S-34 */

/**
 * S-34 · Read the claim against the renter, with the frames it is argued over;
 * Update it — contest with a statement, add evidence, or accept liability.
 *
 * Once a ruling is issued on S-40 it appears here with the adjudicator's reason
 * (FR-19), and with what the renter actually pays — never more than the most
 * their protection plan said they could lose (FR-05).
 */
@Composable
fun S34DamageClaim(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val claimState by vm.currentClaim.collectAsStateWithLifecycle()
    var contesting by rememberSaveable { mutableStateOf(false) }
    var evidence by rememberSaveable { mutableStateOf(false) }
    var accepting by rememberSaveable { mutableStateOf(false) }
    var statement by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    val claim = claimState
    if (claim == null) {
        GearScreen(appBar = { AppBar(title = "Claims", onBack = { nav.back() }) }) {
            EmptyState(
                icon = Lucide.ShieldCheck,
                headline = "No claims against you",
                body = "If an owner reports new damage after a return, the claim and both sets of photos appear here.",
            )
        }
        return
    }

    val booking = vm.bookingFor(claim)
    val plan = booking?.protection
    val cap = plan?.mostYouCouldLose(booking.item.deposit)
    val open = claim.status in setOf(ClaimStatus.OPEN, ClaimStatus.MORE_EVIDENCE, ClaimStatus.CONTESTED)
    val claimant = claim.claimant.name.substringBefore(' ')
    val (pickupPhoto, returnPhoto) = vm.evidenceFor(claim)

    GearScreen(
        appBar = { AppBar(title = "Claim ${claim.ref}", onBack = { nav.back() }) },
        footer = {
            if (open) {
                // The two answers a renter is most likely to give, and the ones
                // that keep the claim open. Accepting liability is the
                // irreversible one and sits further down the page, in outline.
                StickyFooter {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        GearButton(
                            if (claim.status == ClaimStatus.CONTESTED) "Edit statement" else "Contest this",
                            { statement = claim.renterStatement.orEmpty(); contesting = true },
                            Modifier.weight(1f),
                            type = ButtonType.SECONDARY, size = ButtonSize.M,
                        )
                        GearButton(
                            "Add my evidence", { note = claim.renterEvidence.orEmpty(); evidence = true }, Modifier.weight(1f),
                            size = ButtonSize.M, icon = Lucide.Camera,
                        )
                    }
                }
            }
        },
    ) {
        VGap(Space.m)
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow(if (claim.decided) "Awarded" else "Amount at stake")
                VGap(4.dp)
                Text(lkr(claim.award ?: claim.amount), style = GearType.dataXl, color = GearText.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow(if (open) "Respond within" else "Status")
                VGap(4.dp)
                if (open) {
                    Text(
                        countdown(ChronoUnit.MILLIS.between(DemoClock.now(), claim.respondBy)),
                        style = GearType.dataL,
                        color = GearColor.Warning700,
                    )
                } else {
                    Text(claim.status.label, style = GearType.labelL, color = GearText.primary)
                }
            }
        }

        VGap(Space.m)
        when (claim.status) {
            ClaimStatus.DECIDED -> RulingCard(claim, vm.renterShare(claim, claim.award ?: 0))
            ClaimStatus.ACCEPTED -> Banner(
                title = "You accepted this claim",
                text = "Settled at ${lkr(claim.amount)}. You paid ${lkr(vm.renterShare(claim, claim.amount))}; your protection covered the rest.",
                kind = BannerKind.SUCCESS,
            )
            ClaimStatus.ESCALATED -> Banner(
                title = "With a senior adjudicator",
                text = "The claims desk has escalated this. Nothing changes for you until a ruling is issued.",
                kind = BannerKind.INFO,
            )
            else -> Banner(
                title = "$claimant reported new damage on the ${claim.area.lowercase()}",
                text = if (claim.status == ClaimStatus.MORE_EVIDENCE) {
                    "The claims desk has asked for anything else you have. Add it before ${dayMonth(claim.respondBy.toLocalDate())}."
                } else {
                    "You have until ${dayMonth(claim.respondBy.toLocalDate())} to accept, add evidence, or contest. " +
                        "If you do nothing, the claim is decided on the evidence already filed."
                },
                kind = BannerKind.WARNING,
            )
        }

        VGap(Space.l)
        Eyebrow("The evidence")
        VGap(Space.s)
        // The same angle twice, which is the entire claim: one mark that is in
        // the right-hand frame and not the left.
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Column(Modifier.weight(1f)) {
                Text("AT PICKUP", style = GearType.labelXs, color = GearText.tertiary)
                VGap(4.dp)
                GearImage(pickupPhoto, "${claim.area} at pickup", Modifier.fillMaxWidth().height(130.dp), GearShape.sm)
            }
            Column(Modifier.weight(1f)) {
                Text("ON RETURN", style = GearType.labelXs, color = GearText.tertiary)
                VGap(4.dp)
                Box {
                    GearImage(returnPhoto, "${claim.area} on return", Modifier.fillMaxWidth().height(130.dp), GearShape.sm)
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .clip(GearShape.full)
                            .border(3.dp, GearColor.Danger500, GearShape.full)
                            .background(GearColor.Danger500.copy(alpha = 0.12f))
                    )
                }
            }
        }
        VGap(Space.s)
        Text(claim.note, style = GearType.bodyS, color = GearText.secondary)

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Severity", style = GearType.bodyS, color = GearText.secondary, modifier = Modifier.weight(1f))
                    StatusBadge(claim.severity.label, GearColor.Warning700)
                }
                VGap(Space.s)
                // The cap and the ceiling are worked out from the severity and
                // the item's value rather than typed, so the badge above can
                // never disagree with the percentage in the sentence.
                Text(
                    "${claim.severity.label} damage is capped at ${claim.severity.capPercent}% of the " +
                        "item's value under the rubric. It is valued at ${lkr(claim.itemValue)}, " +
                        "so the maximum claim is ${lkr(claim.cap)}.",
                    style = GearType.bodyS,
                    color = GearText.secondary,
                )
                if (plan != null && cap != null) {
                    VGap(Space.sm)
                    GearDivider()
                    VGap(Space.sm)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GearIcon(Lucide.ShieldCheck, null, size = IconSize.m, tint = GearColor.Success500)
                        Spacer(Modifier.width(Space.s))
                        Text(
                            "With ${plan.label} protection the most you could pay is ${lkr(cap)}.",
                            style = GearType.bodyS,
                            color = GearText.body,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (claim.renterStatement != null || claim.renterEvidence != null) {
            VGap(Space.l)
            Eyebrow("Your side")
            VGap(Space.s)
            claim.renterStatement?.let { IconRow(Lucide.Quote, it, Modifier.padding(vertical = 4.dp), tint = GearColor.Marine500, style = GearType.bodyS) }
            claim.renterEvidence?.let { IconRow(Lucide.Paperclip, it, Modifier.padding(vertical = 4.dp), tint = GearColor.Marine500, style = GearType.bodyS) }
        }

        if (open) {
            VGap(Space.l)
            Eyebrow("If you agree it was you")
            VGap(Space.s)
            Text(
                "Accepting settles the claim at ${lkr(claim.amount)} and closes it. " +
                    "You pay ${lkr(vm.renterShare(claim, claim.amount))}. It can't be reopened afterwards.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.s)
            GearButton(
                "Accept liability", { accepting = true }, Modifier.fillMaxWidth(),
                type = ButtonType.SECONDARY, size = ButtonSize.M,
            )
        }
        VGap(Space.section)
    }

    if (contesting) {
        GearDialog(
            title = "Why don't you agree?",
            onDismiss = { contesting = false },
            confirmLabel = "Contest",
            onConfirm = { vm.contest(claim.ref, statement); contesting = false },
        ) {
            Text("The adjudicator reads this beside both photos.", style = GearType.bodyS, color = GearText.secondary)
            VGap(Space.s)
            GearTextField(
                statement, label = "Your statement",
                placeholder = "e.g. The mark was there at pickup",
                onValueChange = { statement = it }, singleLine = false,
            )
        }
    }
    if (evidence) {
        GearDialog(
            title = "Add evidence",
            onDismiss = { evidence = false },
            confirmLabel = "Add to claim",
            onConfirm = { vm.addEvidence(claim.ref, note); evidence = false },
            confirmEnabled = note.isNotBlank(),
        ) {
            Text(
                "Your pickup record is already on the claim. Add anything else: what happened, a receipt, a witness.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.s)
            GearTextField(note, label = "Evidence", onValueChange = { note = it }, singleLine = false)
        }
    }
    if (accepting) {
        GearDialog(
            title = "Accept this claim?",
            onDismiss = { accepting = false },
            confirmLabel = "Not yet",
            onConfirm = { accepting = false },
            destructiveLabel = "Accept",
            onDestructive = { vm.acceptLiability(claim.ref); accepting = false },
        ) {
            Text(
                "${lkr(vm.renterShare(claim, claim.amount))} is charged and the claim closes for good.",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
    }
}

@Composable
private fun RulingCard(claim: Claim, share: Int) {
    val award = claim.award ?: 0
    Column(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.md)
            .border(Border.medium, GearColor.Pine800, GearShape.md)
            .background(GearSurface.card)
            .padding(Space.cardPadding),
    ) {
        Eyebrow("Ruling")
        VGap(Space.s)
        Row {
            Column(Modifier.weight(1f)) {
                Text("AWARDED", style = GearType.labelXs, color = GearText.tertiary)
                Text(lkr(award), style = GearType.dataL, color = GearText.primary)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("YOU PAY", style = GearType.labelXs, color = GearText.tertiary)
                Text(lkr(share), style = GearType.dataL, color = GearText.primary)
            }
        }
        if (award > share) {
            VGap(4.dp)
            Text("Your protection covers the other ${lkr(award - share)}.", style = GearType.bodyS, color = GearText.secondary)
        }
        VGap(Space.sm)
        Text("Reason", style = GearType.labelM, color = GearText.primary)
        Text(claim.rationale.orEmpty(), style = GearType.bodyS, color = GearText.body)
        VGap(Space.s)
        Text(
            "${claim.decidedBy ?: "Claims desk"} · ${claim.decidedAt?.let(::stamp) ?: ""}".uppercase(),
            style = GearType.dataS,
            color = GearText.tertiary,
        )
    }
}

/* ------------------------------------------------------------------ S-35 */

/**
 * S-35 · Read the balance, the holds and the ledger, all derived from bookings;
 * Create a withdrawal or a top-up, each a ledger row that moves the balance.
 */
@Composable
fun S35Wallet(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val available by vm.walletAvailable.collectAsStateWithLifecycle()
    val holds by vm.holds.collectAsStateWithLifecycle()
    val covered by vm.coveredNotHeld.collectAsStateWithLifecycle()
    val txns by vm.transactions.collectAsStateWithLifecycle()
    var toppingUp by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableIntStateOf(5_000) }

    GearScreen(
        appBar = { AppBar(title = "Wallet", onBack = { nav.back() }) },
    ) {
        VGap(Space.m)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.md)
                .background(GearColor.Pine800)
                .padding(Space.cardPadding),
        ) {
            Text("AVAILABLE", style = GearType.labelXs, color = GearColor.HiVis500)
            VGap(6.dp)
            Text(lkr(available), style = GearType.dataXl, color = GearText.onDark)
            VGap(Space.sm)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                GearButton("Withdraw", { vm.withdraw() }, type = ButtonType.PRIMARY, size = ButtonSize.S, enabled = available > 0)
                GearButton("Top up", { toppingUp = true }, type = ButtonType.GHOST, size = ButtonSize.S, contentColor = GearText.onDark)
            }
        }

        VGap(Space.l)
        SectionHeader("Active holds", trailing = {
            Text("${holds.size}", style = GearType.dataM, color = GearText.tertiary)
        })
        VGap(6.dp)
        Text(
            "A hold is not a charge. The money stays on your card and is released after return.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.s)
        if (holds.isEmpty()) {
            Text("Nothing is held on your card right now.", style = GearType.bodyS, color = GearText.tertiary)
            VGap(Space.s)
        }
        holds.forEach { hold ->
            AccentCard(GearColor.Marine500, Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(lkr(hold.booking.depositHold), style = GearType.dataL, color = GearText.primary)
                        VGap(3.dp)
                        Text("${hold.booking.item.shortTitle} · ${hold.booking.ref}", style = GearType.bodyS, color = GearText.secondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        GearIcon(Lucide.LockOpen, null, size = IconSize.m, tint = GearColor.Marine500)
                        VGap(3.dp)
                        Text("RELEASES ${dayMonth(hold.releases).uppercase()}", style = GearType.dataS, color = GearColor.Marine700)
                    }
                }
            }
            VGap(Space.s)
        }

        // A live booking with nothing held against it says why, so this screen
        // agrees with S-09, S-11 and S-17 about what protection did.
        covered.forEach { booking ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(GearShape.md)
                    .border(Border.hair, GearSurface.border, GearShape.md)
                    .padding(Space.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearIcon(Lucide.ShieldCheck, null, size = IconSize.m, tint = GearColor.Success500)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text("No hold", style = GearType.dataL, color = GearText.primary)
                    VGap(3.dp)
                    Text("${booking.item.shortTitle} · ${booking.ref}", style = GearType.bodyS, color = GearText.secondary)
                }
                Text("${booking.protection.label} protection".uppercase(), style = GearType.dataS, color = GearColor.Success500)
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("Transactions")
        VGap(Space.s)
        txns.forEach { txn ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Chalk/200, not chalk/100: the page behind this row *is*
                // chalk/100, so money going out was drawing its disc in the
                // background colour and coming out as a bare floating arrow.
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(GearShape.full)
                        .background(
                            if (txn.amount >= 0) GearColor.Success100 else GearColor.Chalk200
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    GearIcon(
                        if (txn.amount >= 0) Lucide.ArrowDown else Lucide.ArrowUp,
                        null, size = IconSize.s,
                        tint = if (txn.amount >= 0) GearColor.Success500 else GearColor.Chalk600,
                    )
                }
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(txn.label, style = GearType.bodyM, color = GearText.primary, maxLines = 1)
                    Text(txn.date.uppercase(), style = GearType.dataS, color = GearText.tertiary)
                }
                Text(
                    (if (txn.amount >= 0) "+" else "−") + grouped(kotlin.math.abs(txn.amount)),
                    style = GearType.dataM,
                    color = if (txn.amount >= 0) GearColor.Success500 else GearText.primary,
                )
            }
            GearDivider()
        }
        VGap(Space.section)
    }

    if (toppingUp) {
        GearDialog(
            title = "Top up your wallet",
            onDismiss = { toppingUp = false },
            confirmLabel = "Top up ${lkr(amount)}",
            onConfirm = { vm.topUp(amount); toppingUp = false },
        ) {
            Text("From Visa •••• 4471.", style = GearType.bodyS, color = GearText.secondary)
            VGap(Space.s)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                listOf(2_000, 5_000, 10_000).forEach { option ->
                    Chip(grouped(option), selected = amount == option, onClick = { amount = option })
                }
            }
        }
    }
}
