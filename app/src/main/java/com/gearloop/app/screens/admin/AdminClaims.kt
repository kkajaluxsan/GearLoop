package com.gearloop.app.screens.admin

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.countdown
import com.gearloop.app.data.lkr
import com.gearloop.app.data.stamp
import com.gearloop.app.data.repo.Claim
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
 * The one desktop screen: the claims adjudication console.
 *
 * Designed at 1440×1024 as the brief specifies. On a phone it is presented as a
 * board — the full-width canvas scrolls horizontally inside a labelled frame,
 * which is how a desktop artboard would be reviewed on a handset anyway.
 */
@Composable
fun S40AdminClaims(nav: Nav) {
    // A 1440-wide artboard on a 390-wide phone is a genuine presentation
    // problem. "Fit" scales the whole board down so the layout can be read at a
    // glance; "Actual size" is 1:1 and pans, for reading the detail.
    var fitToWidth by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().background(GearColor.Chalk100)) {
        SystemBarIcons(darkSurface = false)
        AppBar(title = "Admin console", subtitle = "Desktop · 1440 × 1024", onBack = { nav.back() })

        Box(Modifier.padding(horizontal = Space.screen, vertical = Space.s)) {
            SegmentedControl(
                options = listOf("Fit to screen", "Actual size"),
                selectedIndex = if (fitToWidth) 0 else 1,
            ) { fitToWidth = it == 0 }
        }

        if (fitToWidth) {
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
                    "Scaled to fit. Switch to actual size to read the panels.",
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
                val sla = ChronoUnit.MILLIS.between(DemoClock.now(), claim.createdAt.plusHours(48))
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
                Text(Demo.ADJUDICATOR, style = GearType.labelM, color = GearText.onDark)
                Text("ADJUDICATOR", style = GearType.labelXs, color = GearColor.Chalk300)
            }
            Spacer(Modifier.width(Space.sm))
            Avatar(Demo.ADJUDICATOR_INITIALS, size = 36.dp)
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            ClaimQueue(queue, claim?.ref, Modifier.width(320.dp).fillMaxHeight()) { selectedRef = it }
            if (claim != null) {
                EvidenceWorkspace(claim, vm.evidenceFor(claim), Modifier.weight(1f).fillMaxHeight())
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
    val hours = ChronoUnit.HOURS.between(claim.createdAt, DemoClock.now()).coerceAtLeast(0)
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
private fun EvidenceWorkspace(claim: Claim, frames: Pair<Int, Int>, modifier: Modifier = Modifier) {
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
                stamp = "9.7134°N 80.0246°E",
                photo = frames.first,
                modifier = Modifier.weight(1f),
            )
            EvidencePane(
                label = "ON RETURN",
                stamp = "9.7131°N 80.0244°E",
                photo = frames.second,
                modifier = Modifier.weight(1f),
                highlight = overlay,
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

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.m)) {
                Eyebrow("Capture integrity")
                VGap(Space.s)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.l)) {
                    IntegrityCheck("Both parties present", true)
                    IntegrityCheck("Same angle", true)
                    IntegrityCheck("Geotag within 40 m", true)
                    IntegrityCheck("Unedited originals", true)
                }
            }
        }

        VGap(Space.m)
        Eyebrow("All seven angles")
        VGap(Space.s)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Demo.returnComparison.forEach { (label, photo, _) ->
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
    photo: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Column(modifier) {
        Text(
            label,
            style = GearType.labelXs,
            color = if (highlight) GearColor.Danger500 else GearText.tertiary,
        )
        VGap(6.dp)
        Box(Modifier.fillMaxWidth().height(300.dp)) {
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
private fun AdjudicationPanel(claim: Claim, vm: MoneyViewModel, modifier: Modifier = Modifier) {
    // The award can never exceed what was claimed, or the cap the severity sets.
    val ceiling = minOf(claim.amount, claim.cap)
    // Keyed on the claim, so moving to another claim starts its own draft ruling.
    var award by rememberSaveable(claim.ref) { mutableIntStateOf(claim.award ?: (ceiling * 7 / 10) / 100 * 100) }
    var rationale by rememberSaveable(claim.ref) { mutableStateOf(claim.rationale.orEmpty()) }
    val median = (ceiling * 67 / 100) / 100 * 100
    Column(
        modifier
            .background(GearSurface.card)
            .verticalScroll(rememberSavedScrollState())
            .padding(Space.l),
    ) {
        Text("Adjudication", style = GearType.headingL, color = GearText.primary)
        Spacer(Modifier.height(3.dp))
        Text(
            "${Demo.ADJUDICATOR} · Trust & safety",
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
                    "${claim.severity.label} damage, Grade B gear. Median award ${lkr(median)}.",
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
            VGap(Space.s)
            Text("Shown to both parties with the ruling.", style = GearType.bodyS, color = GearText.tertiary)
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
    median: Int,
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
            Chip("Median", selected = award == median, onClick = { onAward(median) })
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
            if (rationale.isBlank()) "A ruling can't be issued without a reason. It's shown to both parties."
            else "The rationale is shown to both parties and stored with the ruling.",
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
