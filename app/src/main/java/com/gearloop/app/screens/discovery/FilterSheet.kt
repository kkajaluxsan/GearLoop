package com.gearloop.app.screens.discovery

import com.gearloop.app.data.SearchFilters
import com.gearloop.app.data.LocalSearchFilters
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import kotlin.math.roundToInt

/* ------------------------------------------------------------------ S-06 */


/**
 * S-06 · Read the sheet's stored state (restored by the app shell on launch);
 * every change is an Update written back a moment later; Reset deletes the
 * stored row, so the next launch opens on the defaults.
 */
@Composable
fun S06FilterSheet(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val all by vm.searchable.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    // The sheet doesn't own these — it edits the same object S-05 renders from,
    // which is what makes the result count underneath it true rather than drawn.
    val filters = LocalSearchFilters.current
    val matches = filters.apply(all).size
    // The bars behind the price slider are the listings themselves, counted
    // into sixteen bands of the slider's range.
    val histogram = remember(all) {
        val bands = IntArray(16)
        all.forEach { item ->
            val f = SearchFilters.fractionOf(item.pricePerDay)
            bands[(f * 15.999f).toInt().coerceIn(0, 15)]++
        }
        val most = bands.maxOrNull()?.takeIf { it > 0 } ?: 1
        bands.map { (it.toFloat() / most).coerceAtLeast(0.04f) }
    }
    val boards = filters.sports.isEmpty() || filters.sports.any { it in setOf("Surfing", "SUP") }

    Column(Modifier.fillMaxSize().background(GearSurface.darkest.copy(alpha = 0.45f))) {
        // Tapping the dimmed area behind the sheet dismisses it.
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { nav.back() }
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(GearShape.sheet)
                .background(GearSurface.card),
        ) {
            SheetGrabber()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.screen, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Filters", style = GearType.headingL, color = GearText.primary)
                Spacer(Modifier.weight(1f))
                GearButton(
                    "Reset",
                    filters::reset,
                    type = ButtonType.GHOST,
                    size = ButtonSize.S,
                )
            }
            GearDivider()

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberSavedScrollState())
                    .padding(horizontal = Space.screen),
            ) {
                VGap(Space.m)
                Eyebrow("Sport")
                VGap(Space.s)
                ChipWrap(options = Demo.sports.map { it.first }, selected = filters.sports)

                SectionGap()
                Eyebrow("Fit")
                VGap(Space.s)
                ToggleRow(
                    title = "Only show gear that fits me",
                    checked = filters.fitOnly,
                    onCheckedChange = { filters.fitOnly = it },
                    helper = profile?.let { p ->
                        if (p.heightCm != null && p.weightKg != null) "Based on ${p.heightCm} cm, ${p.weightKg} kg"
                        else "Add your height on your fit profile first"
                    },
                )

                SectionGap()
                Eyebrow("Price per day")
                VGap(Space.s)
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        lkr(filters.priceAt(filters.priceLower)),
                        style = GearType.dataM,
                        color = GearText.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (filters.priceUpper >= 1f) "${lkr(filters.priceAt(1f))}+" else lkr(filters.priceAt(filters.priceUpper)),
                        style = GearType.dataM,
                        color = GearText.primary,
                    )
                }
                VGap(Space.xs)
                GearRangeSlider(
                    lower = filters.priceLower,
                    upper = filters.priceUpper,
                    onRangeChange = { l, u -> filters.priceLower = l; filters.priceUpper = u },
                    histogram = histogram,
                    lowerLabel = "Minimum price",
                    upperLabel = "Maximum price",
                    valueText = { lkr(filters.priceAt(it)) },
                )

                SectionGap()
                Eyebrow("Distance")
                VGap(Space.s)
                SegmentedControl(
                    options = SearchFilters.DISTANCES.map { "$it km" },
                    selectedIndex = filters.distanceIndex,
                    onSelect = { filters.distanceIndex = it },
                )

                if (boards) {
                    SectionGap()
                    Eyebrow("Board length")
                    VGap(Space.s)
                    ChipWrap(
                        options = listOf("5'8\"", "6'0\"", "6'6\"", "7'0\"", "8'0\"+", "9'0\"", "10'6\"", "11'0\""),
                        selected = filters.lengths,
                    )
                }

                SectionGap()
                Eyebrow("Condition")
                VGap(Space.s)
                ChipWrap(
                    options = listOf("Grade A", "Grade B", "Grade C"),
                    selected = filters.grades,
                )

                SectionGap()
                Eyebrow("Booking")
                VGap(Space.s)
                ToggleRow("Instant book only", filters.instantOnly, { filters.instantOnly = it })
                VGap(Space.m)
                ToggleRow("Delivery available", filters.delivery, { filters.delivery = it })
                VGap(Space.section)
            }

            StickyFooter {
                // The count is the real one, recomputed as the controls move. A
                // filter set that matches nothing says so before it is applied.
                GearButton(
                    label = when (matches) {
                        0 -> "No gear matches"
                        1 -> "Show 1 result"
                        else -> "Show $matches results"
                    },
                    // To the results, which is what the button promises. Opened
                    // from home it used to drop the member back on the rails.
                    onClick = { nav.closeTo("s05") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = matches > 0,
                )
            }
        }
    }
}

@Composable
private fun SectionGap() {
    VGap(Space.l)
    GearDivider()
    VGap(Space.l)
}

@Composable
private fun ChipWrap(options: List<String>, selected: MutableList<String>) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        options.forEach { option ->
            Chip(
                label = option,
                selected = option in selected,
                onClick = { if (option in selected) selected.remove(option) else selected.add(option) },
            )
        }
    }
}
