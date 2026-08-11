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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
/* ------------------------------------------------------------------ S-34 */

@Composable
fun S34DamageClaim(nav: Nav) {
    val mark = Demo.RETURN_MARK

    GearScreen(
        appBar = { AppBar(title = "Claim ${Demo.CLAIM_REF}", onBack = { nav.back() }) },
        footer = {
            // The two answers Nirmal is most likely to give, and the ones that
            // keep the claim open. Accepting liability is the irreversible one
            // and sits further down the page, in outline — it used to be a
            // filled red bar at the top of the stack while "Contest this" was
            // parked alone in the footer, which pointed the loudest control on
            // the screen at the worst outcome for the person reading it.
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton(
                        "Contest this", { }, Modifier.weight(1f),
                        type = ButtonType.SECONDARY, size = ButtonSize.M,
                    )
                    GearButton(
                        "Add my evidence", { nav.to("s18b") }, Modifier.weight(1f),
                        size = ButtonSize.M, icon = Lucide.Camera,
                    )
                }
            }
        },
    ) {
        VGap(Space.m)
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Amount at stake")
                VGap(4.dp)
                Text(lkr(Demo.CLAIM_AMOUNT), style = GearType.dataXl, color = GearText.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow("Respond within")
                VGap(4.dp)
                Text(Demo.CLAIM_RESPOND_WITHIN, style = GearType.dataL, color = GearColor.Warning700)
            }
        }

        VGap(Space.m)
        Banner(
            title = "Kavitha reported new damage on the ${mark.area.lowercase()}",
            // The window was written out as "five days" beside a counter reading
            // 4d 06h. It counts down from the same constant now.
            text = "You have ${Demo.CLAIM_RESPOND_DAYS} days to accept, add evidence, or contest. " +
                "If you do nothing, the claim is decided on the evidence already filed.",
            kind = BannerKind.WARNING,
        )

        VGap(Space.l)
        Eyebrow("The evidence")
        VGap(Space.s)
        // The same frame twice, which is the entire claim: same angle, same
        // lighting, one mark that is in the right-hand one and not the left.
        // Two *different* photographs used to sit here, under the words "same
        // angle" — showing a different shot on the return side quietly proves
        // the opposite of what the screen is arguing.
        val evidencePhoto = Demo.returnComparison[3].second
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Column(Modifier.weight(1f)) {
                Text("AT PICKUP", style = GearType.labelXs, color = GearText.tertiary)
                VGap(4.dp)
                GearImage(evidencePhoto, "Tail at pickup", Modifier.fillMaxWidth().height(130.dp), GearShape.sm)
            }
            Column(Modifier.weight(1f)) {
                // Both labels are captions, not warnings — the red one read as
                // an alert on a word that only says which photo this is.
                Text("ON RETURN", style = GearType.labelXs, color = GearText.tertiary)
                VGap(4.dp)
                Box {
                    GearImage(evidencePhoto, "Tail on return", Modifier.fillMaxWidth().height(130.dp), GearShape.sm)
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

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Severity", style = GearType.bodyS, color = GearText.secondary, modifier = Modifier.weight(1f))
                    StatusBadge(mark.severity.label, GearColor.Warning700)
                }
                VGap(Space.s)
                // The cap and the ceiling are worked out from the severity and
                // the board's value rather than typed, so the badge above can
                // never disagree with the percentage in the sentence.
                Text(
                    "${mark.severity.label} damage is capped at ${mark.severity.capPercent}% of the " +
                        "item's value under the rubric. The board is valued at ${lkr(Demo.BOARD_VALUE)}, " +
                        "so the maximum claim is ${lkr(mark.capOn(Demo.BOARD_VALUE))}.",
                    style = GearType.bodyS,
                    color = GearText.secondary,
                )
                VGap(Space.sm)
                GearDivider()
                VGap(Space.sm)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GearIcon(Lucide.FileText, null, size = IconSize.m, tint = GearColor.Marine500)
                    Spacer(Modifier.width(Space.s))
                    Column(Modifier.weight(1f)) {
                        Text("Repair quote — Nallur Board Works", style = GearType.labelL, color = GearText.primary)
                        Text("PDF · 240 KB · uploaded 11 Aug", style = GearType.bodyS, color = GearText.tertiary)
                    }
                    GearIcon(Lucide.Download, "Download", size = IconSize.m, tint = GearColor.Pine800)
                }
            }
        }

        VGap(Space.l)
        Eyebrow("If you agree it was you")
        VGap(Space.s)
        Text(
            "Accepting settles the claim at ${lkr(Demo.CLAIM_AMOUNT)} and closes it. " +
                "It can't be reopened afterwards.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.s)
        GearButton(
            "Accept liability", { }, Modifier.fillMaxWidth(),
            type = ButtonType.SECONDARY, size = ButtonSize.M,
        )
        VGap(Space.section)
    }
}

/* ------------------------------------------------------------------ S-35 */

@Composable
fun S35Wallet(nav: Nav) {
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
            Text(lkr(6_480), style = GearType.dataXl, color = GearText.onDark)
            VGap(Space.sm)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                GearButton("Withdraw", { }, type = ButtonType.PRIMARY, size = ButtonSize.S)
                GearButton("Top up", { }, type = ButtonType.GHOST, size = ButtonSize.S)
            }
        }

        VGap(Space.l)
        SectionHeader("Active holds", trailing = {
            Text("${Demo.walletHolds.size}", style = GearType.dataM, color = GearText.tertiary)
        })
        VGap(6.dp)
        Text(
            "A hold is not a charge. The money stays on your card and is released after return.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.s)
        Demo.walletHolds.forEach { hold ->
            AccentCard(GearColor.Marine500, Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(lkr(hold.amount), style = GearType.dataL, color = GearText.primary)
                        VGap(3.dp)
                        Text(hold.item, style = GearType.bodyS, color = GearText.secondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        GearIcon(Lucide.LockOpen, null, size = IconSize.m, tint = GearColor.Marine500)
                        VGap(3.dp)
                        Text(hold.releases, style = GearType.dataS, color = GearColor.Marine500)
                    }
                }
            }
            VGap(Space.s)
        }

        // Nirmal's live rental is the one booking with nothing held against it,
        // which is the whole argument for Plus protection. Saying so here is
        // what stops this screen contradicting S-09, S-11 and S-17.
        Demo.coveredNotHeld.forEach { (item, why) ->
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
                    Text(item, style = GearType.bodyS, color = GearText.secondary)
                }
                // dataS is an uppercase style, so the reason arrives uppercased
                // rather than being sliced out of a longer sentence here.
                Text(why, style = GearType.dataS, color = GearColor.Success500)
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("Transactions")
        VGap(Space.s)
        Demo.walletTxns.forEach { txn ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Chalk/200, not chalk/100: the page behind this row *is*
                // chalk/100, so money going out was drawing its disc in the
                // background colour and coming out as a bare floating arrow
                // beside the tinted discs on the money-in rows.
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
                    Text(txn.date, style = GearType.dataS, color = GearText.tertiary)
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
}

