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
import androidx.compose.material3.Text
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
import com.gearloop.app.data.Demo
import com.gearloop.app.data.lkr
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

@Composable
private fun DesktopCanvas(modifier: Modifier = Modifier) {
    var selectedClaim by rememberSaveable { mutableIntStateOf(0) }
    var award by rememberSaveable { mutableIntStateOf(9_800) }

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
            Text("Claim ${Demo.CLAIM_REF}", style = GearType.headingM, color = GearText.onDark)
            Spacer(Modifier.width(Space.l))
            Text(
                "${Demo.nirmal.name}  vs  ${Demo.kavitha.name}",
                style = GearType.bodyM,
                color = GearColor.Chalk300,
            )
            Spacer(Modifier.weight(1f))
            Text(lkr(Demo.CLAIM_AMOUNT), style = GearType.dataL, color = GearText.onDark)
            Spacer(Modifier.width(Space.l))
            Row(
                Modifier
                    .clip(GearShape.xs)
                    .background(GearColor.Warning500.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GearIcon(Lucide.Timer, null, size = IconSize.s, tint = GearColor.Warning500)
                Text("SLA 19h 40m", style = GearType.dataM, color = GearColor.Warning700)
            }
            Spacer(Modifier.width(Space.l))
            Avatar(Demo.ayesha, size = 36.dp)
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            ClaimQueue(selectedClaim, Modifier.width(320.dp).fillMaxHeight()) { selectedClaim = it }
            EvidenceWorkspace(Modifier.weight(1f).fillMaxHeight())
            AdjudicationPanel(award, Modifier.width(380.dp).fillMaxHeight()) { award = it }
        }
    }
}

private data class QueueRow(val ref: String, val item: String, val severity: String, val age: String, val amount: Int)

private val queue = listOf(
    QueueRow("CL-3081", "Torq Mod Fish 6'6\"", "Functional", "19h", 14_000),
    QueueRow("CL-3079", "Trek Marlin 7", "Cosmetic", "1d 4h", 3_200),
    QueueRow("CL-3074", "NSP Elements SUP", "Major", "2d 2h", 41_000),
    QueueRow("CL-3068", "Quechua MH500 3P", "Cosmetic", "3d 8h", 1_800),
    QueueRow("CL-3061", "Pelican Argo 100X", "Functional", "4d 1h", 22_500),
)

@Composable
private fun ClaimQueue(selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Column(modifier.background(GearSurface.card)) {
        Row(
            Modifier.fillMaxWidth().padding(Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Queue", style = GearType.headingM, color = GearText.primary)
            Spacer(Modifier.width(Space.s))
            Text("${queue.size}", style = GearType.dataM, color = GearText.tertiary)
            Spacer(Modifier.weight(1f))
            GearIcon(Lucide.Funnel, "Filter", size = IconSize.m, tint = GearColor.Chalk500)
        }
        GearDivider()
        queue.forEachIndexed { i, row ->
            val active = i == selected
            val severityColor = when (row.severity) {
                "Major" -> GearColor.Danger500
                "Functional" -> GearColor.Warning700
                else -> GearColor.Chalk600
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (active) GearColor.HiVis100 else Color.Transparent)
                    .clickable(role = Role.Button) { onSelect(i) }
                    .padding(Space.m),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.ref, style = GearType.dataM, color = GearText.primary)
                    Spacer(Modifier.weight(1f))
                    Text(row.age, style = GearType.dataS, color = GearText.tertiary)
                }
                VGap(4.dp)
                Text(row.item, style = GearType.bodyM, color = GearText.body, maxLines = 1)
                VGap(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(row.severity, severityColor)
                    Spacer(Modifier.weight(1f))
                    Text(lkr(row.amount), style = GearType.dataM, color = GearText.primary)
                }
            }
            GearDivider()
        }
    }
}

@Composable
private fun EvidenceWorkspace(modifier: Modifier = Modifier) {
    Column(modifier.background(GearColor.Chalk100).padding(Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Evidence", style = GearType.headingL, color = GearText.primary)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Chip("Diff overlay", selected = true, icon = Lucide.Aperture)
                Chip("Zoom", icon = Lucide.ZoomIn)
                Chip("Metadata", icon = Lucide.Info)
            }
        }

        VGap(Space.m)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            EvidencePane(
                label = "AT PICKUP",
                stamp = "08 AUG 08:57 · 9.7134°N 80.0246°E",
                photo = Demo.torqConditionPhotos[0],
                modifier = Modifier.weight(1f),
            )
            EvidencePane(
                label = "ON RETURN",
                stamp = "10 AUG 16:41 · 9.7131°N 80.0244°E",
                photo = Demo.torqConditionPhotos[1],
                modifier = Modifier.weight(1f),
                highlight = true,
            )
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
            Demo.returnComparison.forEach { (label, photo, changed) ->
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
private fun AdjudicationPanel(award: Int, modifier: Modifier = Modifier, onAward: (Int) -> Unit) {
    Column(
        modifier
            .background(GearSurface.card)
            .verticalScroll(rememberSavedScrollState())
            .padding(Space.l),
    ) {
        Text("Adjudication", style = GearType.headingL, color = GearText.primary)

        VGap(Space.m)
        Eyebrow("Rubric")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth(), fill = GearColor.Chalk50) {
            Column(Modifier.padding(Space.sm)) {
                listOf(
                    "Cosmetic" to "No award. Wear is priced into the rental.",
                    "Functional" to "Up to 60% of item value, less depreciation.",
                    "Major" to "Up to 100%, plus lost-booking compensation.",
                ).forEachIndexed { i, (severity, rule) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(
                            severity,
                            style = GearType.labelM,
                            color = if (i == 1) GearColor.Warning700 else GearText.tertiary,
                            modifier = Modifier.width(88.dp),
                        )
                        Text(
                            rule,
                            style = GearType.bodyS,
                            color = if (i == 1) GearText.body else GearText.tertiary,
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
                Text("4 similar rulings", style = GearType.labelL, color = GearColor.Marine700)
                Text(
                    "Tail dings, Grade B softtops. Median award ${lkr(9_400)}.",
                    style = GearType.bodyS,
                    color = GearColor.Marine700,
                )
            }
            GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Marine700)
        }

        VGap(Space.m)
        Eyebrow("Award")
        VGap(Space.s)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(lkr(award), style = GearType.dataXl, color = GearText.primary)
            Spacer(Modifier.weight(1f))
            Text("of ${lkr(Demo.CLAIM_AMOUNT)} claimed", style = GearType.bodyS, color = GearText.tertiary)
        }
        VGap(Space.s)
        GearSlider(
            value = award / 14_000f,
            onValueChange = { onAward((it * 14_000).toInt() / 100 * 100) },
        )
        VGap(Space.s)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Chip("Nil", onClick = { onAward(0) })
            Chip("Median", onClick = { onAward(9_400) })
            Chip("Full", onClick = { onAward(14_000) })
        }

        VGap(Space.m)
        Eyebrow("Rationale — required")
        VGap(Space.s)
        GearTextField(
            value = "Ding is post-pickup and functional but not through the glass. " +
                "Repair quote accepted at LKR 9,800; depreciation applied for a 3-year-old softtop.",
            state = FieldState.FILLED,
        )
        VGap(6.dp)
        Text(
            "The rationale is shown to both parties and stored with the ruling.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )

        VGap(Space.l)
        GearButton("Issue ruling", { }, Modifier.fillMaxWidth())
        VGap(Space.s)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            GearButton("Request more evidence", { }, Modifier.weight(1f), type = ButtonType.SECONDARY, size = ButtonSize.M)
            GearButton("Escalate", { }, type = ButtonType.GHOST, size = ButtonSize.M)
        }
    }
}
