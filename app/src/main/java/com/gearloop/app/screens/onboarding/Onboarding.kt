package com.gearloop.app.screens.onboarding

import com.gearloop.app.ui.theme.Border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
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
import com.gearloop.app.data.Demo
import com.gearloop.app.data.FitLogic
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.icons.LucideIcon
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space

/**
 * How many screens onboarding is, held once.
 *
 * S-02 and S-03 each counted to four independently and there is no fourth
 * screen, so the track never filled and the last step read "3 of 4".
 */
private const val ONBOARDING_STEPS = 3

/* ------------------------------------------------------------------ S-01 */

private data class Intent(val icon: LucideIcon, val title: String, val subtitle: String)

private val intents = listOf(
    Intent(Lucide.Compass, "Rent gear", "Get what you need, only when you need it"),
    Intent(Lucide.Coins, "Earn from my gear", "Your kit pays for itself"),
    Intent(Lucide.ArrowRightLeft, "Swap gear", "Trade up without buying new"),
    Intent(Lucide.Workflow, "All of it", "The full loop"),
)

@Composable
fun S01IntentSelector(nav: Nav) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    GearScreen(
        appBar = {
            AppBar(
                onBack = { nav.back() },
                actions = { GearLink("Skip", { nav.to("s04") }, color = GearText.tertiary) },
            )
        },
        footer = {
            StickyFooter {
                GearButton("Continue", { nav.to("s02") }, Modifier.fillMaxWidth())
            }
        },
    ) {
        // Step 1 of the same three, drawn the same way. It used to open on a
        // bare "GETTING STARTED" eyebrow and then produce a progress track out
        // of nowhere on the next screen, reading "step 2 of 4" — of four, when
        // onboarding is three screens long.
        ProgressStepper(step = 1, total = ONBOARDING_STEPS)
        VGap(Space.l)
        Text("What brings you here?", style = GearType.displayL, color = GearText.primary)
        VGap(Space.l)

        intents.forEachIndexed { i, intent ->
            IntentCard(intent, i == selected) { selected = i }
            if (i != intents.lastIndex) VGap(Space.listGap)
        }
        VGap(Space.l)
        Text(
            "You can change this later. It only sets what we show you first.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )
        VGap(Space.section)
    }
}

@Composable
private fun IntentCard(intent: Intent, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(GearShape.md)
            .border(
                width = Border.medium,
                color = if (selected) GearColor.Pine800 else GearSurface.border,
                shape = GearShape.md,
            )
            .background(if (selected) GearColor.HiVis100 else GearSurface.card)
            // Pick one of four, so it announces as a radio rather than as a
            // button that gives no hint the others just became unselected.
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearIcon(
            intent.icon, null,
            size = IconSize.xl,
            tint = if (selected) GearColor.Pine800 else GearColor.Chalk500,
        )
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(intent.title, style = GearType.headingM, color = GearText.primary)
            Spacer(Modifier.height(3.dp))
            Text(intent.subtitle, style = GearType.bodyS, color = GearText.secondary)
        }
        if (selected) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(GearColor.Pine800),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(Lucide.Check, null, size = 14.dp, tint = GearColor.HiVis500, strokeWidth = 2.5f)
            }
        }
    }
}

/* ------------------------------------------------------------------ S-02 */

/**
 * The screen to lead with in the viva.
 *
 * The derived-size panel is computed live from the two numbers above it — move
 * either stepper and every row changes. That is the claim the whole product
 * rests on, so it had to actually work rather than be drawn once.
 */
@Composable
fun S02FitProfile(nav: Nav) {
    var height by rememberSaveable { mutableIntStateOf(Demo.HEIGHT_CM) }
    var weight by rememberSaveable { mutableIntStateOf(Demo.WEIGHT_KG) }
    var metric by rememberSaveable { mutableStateOf(true) }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton("Save and continue", { nav.to("s03") }, Modifier.fillMaxWidth())
            }
        },
    ) {
        ProgressStepper(step = 2, total = ONBOARDING_STEPS)
        VGap(Space.l)
        Text("Let's get your sizes right", style = GearType.displayL, color = GearText.primary)
        VGap(Space.m)
        Banner(
            text = "Wrong-sized gear isn't just uncomfortable. Helmets and harnesses have to fit.",
            kind = BannerKind.SAFETY,
        )
        VGap(Space.l)

        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            MeasureField(
                label = "Height",
                value = height,
                unit = if (metric) "cm" else "in",
                modifier = Modifier.weight(1f),
                onMinus = { height = (height - 1).coerceAtLeast(130) },
                onPlus = { height = (height + 1).coerceAtMost(215) },
            )
            MeasureField(
                label = "Weight",
                value = weight,
                unit = if (metric) "kg" else "lb",
                modifier = Modifier.weight(1f),
                onMinus = { weight = (weight - 1).coerceAtLeast(35) },
                onPlus = { weight = (weight + 1).coerceAtMost(160) },
            )
        }
        VGap(Space.sm)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Chip("Metric", selected = metric, onClick = { metric = true })
            Chip("Imperial", selected = !metric, onClick = { metric = false })
            Spacer(Modifier.weight(1f))
            Text("Shoe EU ${Demo.SHOE_EU}", style = GearType.dataM, color = GearText.tertiary)
        }

        VGap(Space.l)
        GearDivider()
        VGap(Space.l)

        Eyebrow("What this means for you")
        VGap(Space.sm)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.md)
                .background(GearColor.Chalk50)
                .padding(Space.cardPadding),
        ) {
            val rows = FitLogic.derived(height, weight)
            rows.forEachIndexed { i, (label, value) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = GearType.dataS, color = GearText.tertiary, modifier = Modifier.width(96.dp))
                    Spacer(Modifier.weight(1f))
                    Text(value, style = GearType.dataM, color = GearText.primary)
                }
                if (i != rows.lastIndex) {
                    VGap(Space.sm)
                    GearDivider(color = GearColor.Chalk200)
                    VGap(Space.sm)
                }
            }
        }
        VGap(Space.sm)
        Text(
            "These come straight from your height and weight — change either and they " +
                "update. Every listing you see gets a fit verdict from them.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )

        VGap(Space.m)
        GearButton("Skip for now", { nav.to("s03") }, type = ButtonType.GHOST, size = ButtonSize.M)
        VGap(Space.section)
    }
}

@Composable
private fun MeasureField(
    label: String,
    value: Int,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(GearShape.md)
            .border(Border.medium, GearSurface.border, GearShape.md)
            .background(GearSurface.card)
            .padding(Space.sm),
    ) {
        Text(label, style = GearType.labelM, color = GearText.secondary)
        VGap(Space.s)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$value", style = GearType.dataXl, color = GearText.primary)
            Text(" $unit", style = GearType.bodyM, color = GearText.tertiary, modifier = Modifier.padding(bottom = 4.dp))
        }
        VGap(Space.s)
        Stepper(value = "", onMinus = onMinus, onPlus = onPlus, modifier = Modifier.fillMaxWidth())
    }
}

/* ------------------------------------------------------------------ S-03 */

@Composable
fun S03SportInterests(nav: Nav) {
    val chosen = rememberSavedStateList { listOf("Surfing", "Cycling", "SUP") }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    label = "Continue (${chosen.size} selected)",
                    onClick = { nav.to("s04") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = chosen.isNotEmpty(),
                )
            }
        },
    ) {
        ProgressStepper(step = 3, total = ONBOARDING_STEPS)
        VGap(Space.l)
        Text("Pick your sports", style = GearType.displayL, color = GearText.primary)
        VGap(Space.s)
        Text(
            "We'll use these to work out what fits you and what's worth swapping.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.l)

        Demo.sports.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                pair.forEach { (name, photo) ->
                    SportTile(
                        name = name,
                        photo = photo,
                        selected = name in chosen,
                        modifier = Modifier.weight(1f),
                        onClick = { if (name in chosen) chosen.remove(name) else chosen.add(name) },
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            VGap(Space.sm)
        }
        VGap(Space.section)
    }
}

@Composable
private fun SportTile(
    name: String,
    photo: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(1.5f)
            .clip(GearShape.md)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (selected) Modifier.border(2.5.dp, GearColor.HiVis500, GearShape.md) else Modifier
            ),
    ) {
        GearImage(photo, name, Modifier.fillMaxSize(), scrim = 0.55f)
        Text(
            name,
            style = GearType.headingS,
            color = GearText.onDark,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(GearColor.HiVis500),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(Lucide.Check, null, size = 14.dp, tint = GearColor.Pine900, strokeWidth = 2.5f)
            }
        }
    }
}
