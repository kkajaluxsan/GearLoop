package com.gearloop.app.screens.admin

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.countdown
import com.gearloop.app.data.lkr
import com.gearloop.app.data.stamp
import com.gearloop.app.data.repo.Claim
import com.gearloop.app.data.repo.ClaimEvidence
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.vm.MoneyViewModel
import com.gearloop.app.ui.vm.gearViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.temporal.ChronoUnit
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

/* ------------------------------------------------------------------ S-40 */

/**
 * The claims adjudication console.
 *
 * Designed at 1440×1024 as the brief specifies, for a desk. On a phone it opens
 * in a phone layout — the queue, then one claim at a time with its evidence
 * and the ruling under it — so a claim can be ruled on from the handset in
 * hand. The desktop board is still one tap away, scaled to fit or at 1:1.
 */
@Composable
fun S40AdminClaims(nav: Nav) {
    // 0 the phone layout, 1 the board scaled to fit, 2 the board at actual size.
    var view by rememberSaveable { mutableIntStateOf(0) }
    val vm: MoneyViewModel = gearViewModel()
    val account by vm.account.collectAsStateWithLifecycle()
    if (account?.isAdmin != true) {
        GearScreen(appBar = { AppBar(title = "Claims desk", onBack = { nav.back() }) }) {
            EmptyState(
                icon = Lucide.Lock,
                headline = "For the claims desk only",
                body = "Your own claims are in your notifications and on each rental.",
            )
        }
        return
    }

    // Its own scaffold, so it needs the keyboard inset the shared ones carry:
    // without it the rationale field's keyboard covered Issue ruling.
    Column(Modifier.fillMaxSize().background(GearColor.Chalk100).imePadding()) {
        SystemBarIcons(darkSurface = false)
        AppBar(title = "Claims desk", subtitle = account?.person?.name?.let { "${it.uppercase()} · ADJUDICATOR" }, onBack = { nav.back() })

        Box(Modifier.padding(horizontal = Space.screen, vertical = Space.s)) {
            SegmentedControl(
                options = listOf("Phone", "Desktop", "Desktop 1:1"),
                selectedIndex = view,
            ) { view = it }
        }

        if (view == 0) {
            PhoneDesk(vm, Modifier.weight(1f).fillMaxWidth())
        } else if (view == 1) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberSavedScrollState())
                    .padding(horizontal = Space.screen),
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Shrinking the density rather than transforming the drawing:
                // the board still lays out at a true 1440×1024, but every dp is
                // worth fewer pixels, so it measures to exactly the phone's
                // width and text scales with it.
                    val scale = maxWidth / BOARD_W
                    val outer = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(outer.density * scale, outer.fontScale)
                    ) {
                        DesktopCanvas(Modifier.width(BOARD_W).height(BOARD_H))
                    }
                }
                Spacer(Modifier.height(Space.s))
                Text(
                    "Scaled to fit. Desktop 1:1 pans at full size.",
                    style = GearType.bodyS,
                    color = GearText.tertiary,
                )
                Spacer(Modifier.height(Space.section).navigationBarsPadding())
            }
        } else {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(rememberSavedScrollState())
                    .verticalScroll(rememberSavedScrollState()),
            ) {
                DesktopCanvas(Modifier.width(BOARD_W).height(BOARD_H))
            }
        }
    }
}

private val BOARD_W = 1440.dp
private val BOARD_H = 1024.dp

/** The desk on a handset: the queue, or one claim with its evidence and ruling stacked. */
@Composable
private fun PhoneDesk(vm: MoneyViewModel, modifier: Modifier = Modifier) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    var openRef by rememberSaveable { mutableStateOf<String?>(null) }
    val claim = queue.firstOrNull { it.ref == openRef }
    // Back from a claim is back to the queue, not off the desk.
    androidx.activity.compose.BackHandler(enabled = claim != null) { openRef = null }
    Column(modifier.verticalScroll(rememberSavedScrollState())) {
        if (claim == null) {
            if (queue.isEmpty()) {
                Box(Modifier.padding(Space.screen)) {
                    EmptyState(icon = Lucide.Inbox, headline = "The queue is empty", body = "No claims are waiting.")
                }
            } else {
                ClaimQueue(queue, null, Modifier.fillMaxWidth()) { openRef = it }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.screen, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearButton("All claims", { openRef = null }, type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.ChevronLeft)
                Spacer(Modifier.weight(1f))
                Text(lkr(claim.amount), style = GearType.dataL, color = GearText.primary)
            }
            Text(
                "${claim.ref} · ${claim.respondent.name} vs ${claim.claimant.name}",
                style = GearType.bodyS,
                color = GearText.secondary,
                modifier = Modifier.padding(horizontal = Space.screen),
            )
            EvidenceWorkspace(claim, vm.evidenceFor(claim), vm.returnFrames(claim), Modifier.fillMaxWidth(), paneHeight = 200.dp)
            AdjudicationPanel(claim, vm, Modifier.fillMaxWidth(), scroll = false)
            Spacer(Modifier.height(Space.section).navigationBarsPadding())
        }
    }
}

/**
 * S-40 · Read the claims queue — every claim on the platform, undecided first;
 * Update one with a ruling (award and stated reason), a request for more
 * evidence, or an escalation. The ruling is refused without a reason (FR-19),
 * and it is what the renter then reads on S-34.
 */
@Composable
private fun DesktopCanvas(modifier: Modifier = Modifier) {
    val vm: MoneyViewModel = gearViewModel()
    val queue by vm.queue.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    var selectedRef by rememberSaveable { mutableStateOf<String?>(null) }
    val claim = queue.firstOrNull { it.ref == selectedRef } ?: queue.firstOrNull()

    Column(modifier.background(GearColor.Chalk100)) {
        // ---- top bar ------------------------------------------------------
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(GearSurface.darkest)
                .padding(horizontal = Space.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearTag("GEARLOOP · OPS", tone = TagTone.DARK)
            Spacer(Modifier.width(Space.l))
            Text("Claim ${claim?.ref ?: "—"}", style = GearType.headingM, color = GearText.onDark)
            Spacer(Modifier.width(Space.l))
            if (claim != null) {
                Text(
                    "${claim.respondent.name}  vs  ${claim.claimant.name}",
                    style = GearType.bodyM,
                    color = GearColor.Chalk300,
                )
            }
            Spacer(Modifier.weight(1f))
            if (claim != null) {
                Text(lkr(claim.amount), style = GearType.dataL, color = GearText.onDark)
                Spacer(Modifier.width(Space.l))
                // A 48-hour service level from the moment the claim was raised.
                val sla = ChronoUnit.MILLIS.between(AppClock.now(), claim.createdAt.plusHours(48))
                Row(
                    Modifier
                        .clip(GearShape.xs)
                        .background(GearColor.Warning500.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GearIcon(Lucide.Timer, null, size = IconSize.s, tint = GearColor.Warning500)
                    Text(
                        if (claim.decided) "DECIDED" else if (sla > 0) "SLA ${countdown(sla).uppercase()}" else "SLA BREACHED",
                        style = GearType.dataM,
                        color = GearColor.Warning700,
                    )
                }
            }
            Spacer(Modifier.width(Space.l))
            // Whoever is on the desk, not a party to the claim.
            Column(horizontalAlignment = Alignment.End) {
                Text(account?.person?.name ?: Demo.ADJUDICATOR, style = GearType.labelM, color = GearText.onDark)
                Text("ADJUDICATOR", style = GearType.labelXs, color = GearColor.Chalk300)
            }
            Spacer(Modifier.width(Space.sm))
            Avatar(account?.person?.initials ?: Demo.ADJUDICATOR_INITIALS, size = 36.dp)
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            ClaimQueue(queue, claim?.ref, Modifier.width(320.dp).fillMaxHeight()) { selectedRef = it }
            if (claim != null) {
                EvidenceWorkspace(claim, vm.evidenceFor(claim), vm.returnFrames(claim), Modifier.weight(1f).fillMaxHeight())
                AdjudicationPanel(claim, vm, Modifier.width(380.dp).fillMaxHeight())
            } else {
                Box(Modifier.weight(1f).fillMaxHeight().padding(Space.l)) {
                    EmptyState(icon = Lucide.Inbox, headline = "The queue is empty", body = "No claims are waiting.")
                }
            }
        }
    }
}

/** How long a claim has been waiting: `19h`, `1d 4h`. */
private fun ageOf(claim: Claim): String {
    val hours = ChronoUnit.HOURS.between(claim.createdAt, AppClock.now()).coerceAtLeast(0)
    return if (hours < 24) "${hours}h" else "${hours / 24}d ${hours % 24}h"
}

@Composable
private fun ClaimQueue(queue: List<Claim>, selected: String?, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Column(modifier.background(GearSurface.card)) {
        Row(
            Modifier.fillMaxWidth().padding(Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Queue", style = GearType.headingM, color = GearText.primary)
            Spacer(Modifier.width(Space.s))
            Text("${queue.count { !it.decided }} open", style = GearType.dataM, color = GearText.tertiary)
        }
        GearDivider()
        queue.forEach { row ->
            val active = row.ref == selected
            // Danger/700, not /500: the badge writes the word on a 12 %-alpha
            // wash of the same colour, and the accent step is the one drawn to
            // be a shape. MAJOR was the loudest thing in the queue and the
            // hardest row in it to actually read.
            val severityColor = when (row.severity) {
                DamageMark.Severity.MAJOR -> GearColor.Danger700
                DamageMark.Severity.FUNCTIONAL -> GearColor.Warning700
                DamageMark.Severity.COSMETIC -> GearColor.Chalk600
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (active) GearColor.HiVis100 else Color.Transparent)
                    .clickable(role = Role.Button) { onSelect(row.ref) }
                    .padding(Space.m),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.ref, style = GearType.dataM, color = GearText.primary)
                    Spacer(Modifier.weight(1f))
                    Text(ageOf(row).uppercase(), style = GearType.dataS, color = GearText.tertiary)
                }
                VGap(4.dp)
                Text(row.listingTitle, style = GearType.bodyM, color = GearText.body, maxLines = 1)
                VGap(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(row.severity.label, severityColor)
                    Spacer(Modifier.width(Space.s))
                    Text(row.status.label, style = GearType.bodyS, color = GearText.tertiary, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Text(lkr(row.award ?: row.amount), style = GearType.dataM, color = GearText.primary)
                }
            }
            GearDivider()
        }
    }
}

@Composable
private fun EvidenceWorkspace(
    claim: Claim,
    evidence: ClaimEvidence,
    frames: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    paneHeight: Dp = 300.dp,
) {
    var overlay by rememberSaveable { mutableStateOf(true) }
    Column(modifier.background(GearColor.Chalk100).padding(Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Evidence · ${claim.area}", style = GearType.headingL, color = GearText.primary)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Chip("Diff overlay", selected = overlay, icon = Lucide.Aperture, onClick = { overlay = !overlay })
            }
        }

        VGap(Space.m)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            EvidencePane(
                label = "AT PICKUP",
                stamp = evidence.pickupStamp,
                photo = evidence.pickupPhoto,
                modifier = Modifier.weight(1f),
                height = paneHeight,
            )
            EvidencePane(
                label = "ON RETURN",
                stamp = evidence.returnStamp,
                photo = evidence.returnPhoto,
                modifier = Modifier.weight(1f),
                highlight = overlay,
                height = paneHeight,
            )
        }
        VGap(Space.s)
        Text(claim.note, style = GearType.bodyM, color = GearText.body)
        if (claim.renterStatement != null) {
            VGap(Space.s)
            Text("Renter: “${claim.renterStatement}”", style = GearType.bodyM, color = GearText.secondary)
        }
        if (claim.renterEvidence != null) {
            VGap(4.dp)
            Text("Renter's evidence: ${claim.renterEvidence}", style = GearType.bodyS, color = GearText.secondary)
        }
        if (claim.renterPhotos.isNotEmpty()) {
            VGap(Space.s)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                claim.renterPhotos.take(6).forEach { GearImage(it, "Renter's evidence photo", Modifier.size(96.dp), GearShape.sm) }
            }
        }

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.m)) {
                Eyebrow("Capture integrity")
                VGap(Space.s)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    evidence.checks.forEach { (label, ok) -> IntegrityCheck(label, ok) }
                }
            }
        }

        VGap(Space.m)
        Eyebrow("Every angle on return")
        VGap(Space.s)
        Row(Modifier.horizontalScroll(rememberSavedScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            frames.forEach { (label, photo) ->
                val changed = label == claim.area
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(GearShape.sm)
                            .then(
                                if (changed) Modifier.border(2.5.dp, GearColor.Warning500, GearShape.sm)
                                else Modifier
                            ),
                    ) {
                        GearImage(photo, label, Modifier.fillMaxSize(), GearShape.sm)
                    }
                    VGap(4.dp)
                    Text(
                        label.uppercase(),
                        style = GearType.labelXs,
                        color = if (changed) GearColor.Warning700 else GearText.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidencePane(
    label: String,
    stamp: String,
    photo: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    height: Dp = 300.dp,
) {
    Column(modifier) {
        Text(
            label,
            style = GearType.labelXs,
            color = if (highlight) GearColor.Danger500 else GearText.tertiary,
        )
        VGap(6.dp)
        Box(Modifier.fillMaxWidth().height(height)) {
            GearImage(photo, label, Modifier.fillMaxSize(), GearShape.md)
            if (highlight) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(140.dp)
                        .clip(GearShape.full)
                        .border(3.dp, GearColor.Danger500, GearShape.full)
                        .background(GearColor.Danger500.copy(alpha = 0.10f))
                )
            }
            Text(
                stamp,
                style = GearType.dataS,
                color = GearText.onDark,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Space.s)
                    .clip(GearShape.xs)
                    .background(GearColor.Pine900.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun IntegrityCheck(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GearIcon(
            if (ok) Lucide.CircleCheck else Lucide.CircleX,
            null, size = IconSize.s,
            tint = if (ok) GearColor.Success500 else GearColor.Danger500,
        )
        Text(label, style = GearType.bodyS, color = GearText.secondary)
    }
}

@Composable
private fun AdjudicationPanel(claim: Claim, vm: MoneyViewModel, modifier: Modifier = Modifier, scroll: Boolean = true) {
    // The award can never exceed what was claimed, or the cap the severity sets.
    val ceiling = minOf(claim.amount, claim.cap)
    // Keyed on the claim, so moving to another claim starts its own draft ruling.
    var award by rememberSaveable(claim.ref) { mutableIntStateOf(claim.award ?: (ceiling * 7 / 10) / 100 * 100) }
    var rationale by rememberSaveable(claim.ref) { mutableStateOf(claim.rationale.orEmpty()) }
    // Precedent is what this desk has actually awarded at the same severity.
    val queue by vm.queue.collectAsStateWithLifecycle()
    val earlier = queue.filter { it.decided && it.severity == claim.severity && it.ref != claim.ref }.mapNotNull { it.award }.sorted()
    val median = earlier.getOrNull(earlier.size / 2)?.coerceAtMost(ceiling)
    Column(
        modifier
            .background(GearSurface.card)
            .then(if (scroll) Modifier.verticalScroll(rememberSavedScrollState()) else Modifier)
            .padding(Space.l),
    ) {
        Text("Adjudication", style = GearType.headingL, color = GearText.primary)
        Spacer(Modifier.height(3.dp))
        Text(
            "${vm.account.collectAsStateWithLifecycle().value?.person?.name ?: Demo.ADJUDICATOR} · Trust & safety",
            style = GearType.bodyS,
            color = GearText.secondary,
        )

        VGap(Space.m)
        Eyebrow("Rubric")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth(), fill = GearColor.Chalk50) {
            Column(Modifier.padding(Space.sm)) {
                // The rubric the renter is shown on S-15b and S-34, read off the
                // same enum. The row for the claim in hand is the one lit up, so
                // the adjudicator's ceiling and the renter's cap are visibly the
                // same rule rather than two separately typed sentences.
                DamageMark.Severity.entries.forEach { severity ->
                    val current = severity == claim.severity
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(
                            severity.label,
                            style = GearType.labelM,
                            color = if (current) GearColor.Warning700 else GearText.tertiary,
                            modifier = Modifier.width(88.dp),
                        )
                        Text(
                            severity.rubric,
                            style = GearType.bodyS,
                            color = if (current) GearText.body else GearText.tertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        VGap(Space.m)
        Eyebrow("Precedent")
        VGap(Space.s)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .background(GearColor.Marine100)
                .padding(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIcon(Lucide.History, null, size = IconSize.m, tint = GearColor.Marine700)
            Spacer(Modifier.width(Space.s))
            Column(Modifier.weight(1f)) {
                Text("Similar rulings", style = GearType.labelL, color = GearColor.Marine700)
                Text(
                    if (median == null) "No earlier rulings on ${claim.severity.label.lowercase()} damage yet."
                    else "${earlier.size} earlier ${claim.severity.label.lowercase()} ruling${if (earlier.size > 1) "s" else ""}. Median award ${lkr(median)}.",
                    style = GearType.bodyS,
                    color = GearColor.Marine700,
                )
            }
        }

        VGap(Space.m)
        Eyebrow("Award")
        VGap(Space.s)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(lkr(award), style = GearType.dataXl, color = GearText.primary)
            Spacer(Modifier.weight(1f))
            Text("of ${lkr(claim.amount)} claimed", style = GearType.bodyS, color = GearText.tertiary)
        }

        if (claim.decided) {
            VGap(Space.s)
            Text("RULED ${claim.decidedAt?.let(::stamp)?.uppercase() ?: ""}", style = GearType.dataS, color = GearText.tertiary)
            VGap(Space.m)
            Eyebrow("Rationale")
            VGap(Space.s)
            Text(claim.rationale.orEmpty(), style = GearType.bodyM, color = GearText.body)
        } else {
            RulingControls(claim, vm, ceiling, median, award, { award = it }, rationale, { rationale = it })
        }
    }
}

@Composable
private fun RulingControls(
    claim: Claim,
    vm: MoneyViewModel,
    ceiling: Int,
    median: Int?,
    award: Int,
    onAward: (Int) -> Unit,
    rationale: String,
    onRationale: (String) -> Unit,
) {
    Column {
        VGap(Space.s)
        GearSlider(
            value = if (ceiling == 0) 0f else award / ceiling.toFloat(),
            onValueChange = { onAward((it * ceiling).toInt() / 100 * 100) },
            label = "Award",
            stateText = lkr(award),
        )
        VGap(Space.s)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Chip("Nil", selected = award == 0, onClick = { onAward(0) })
            if (median != null) Chip("Median", selected = award == median, onClick = { onAward(median) })
            Chip("Full", selected = award == ceiling, onClick = { onAward(ceiling) })
        }

        VGap(Space.m)
        Eyebrow("Rationale — required")
        VGap(Space.s)
        GearTextField(
            value = rationale,
            placeholder = "Why this award: what the evidence shows, and the rule applied",
            onValueChange = onRationale,
            singleLine = false,
        )
        VGap(6.dp)
        Text(
            "Shown to both parties with the ruling.",
            style = GearType.bodyS,
            color = if (rationale.isBlank()) GearColor.Warning700 else GearText.tertiary,
        )

        VGap(Space.l)
        GearButton(
            "Issue ruling", { vm.issueRuling(claim.ref, award, rationale) }, Modifier.fillMaxWidth(),
            enabled = rationale.trim().length >= 10,
        )
        VGap(Space.s)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            GearButton(
                "Request more evidence", { vm.requestMoreEvidence(claim.ref) }, Modifier.weight(1f),
                type = ButtonType.SECONDARY, size = ButtonSize.M,
                enabled = claim.status != com.gearloop.app.data.ClaimStatus.MORE_EVIDENCE,
            )
            GearButton(
                "Escalate", { vm.escalate(claim.ref) }, type = ButtonType.GHOST, size = ButtonSize.M,
                enabled = claim.status != com.gearloop.app.data.ClaimStatus.ESCALATED,
            )
        }
    }
}
