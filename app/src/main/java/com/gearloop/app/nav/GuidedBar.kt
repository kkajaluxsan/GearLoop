package com.gearloop.app.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gearloop.app.ui.components.VGap
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.DarkPalette
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.LocalPalette
import com.gearloop.app.ui.theme.Metrics
import com.gearloop.app.ui.theme.Space

/**
 * The walkthrough bar.
 *
 * Deliberately not styled like the product. It is always drawn on the dark
 * palette regardless of the app's theme, so nobody in the room ever mistakes
 * presentation chrome for a screen someone designed. It sits above the app bar
 * and pushes content down rather than covering it, because the screen underneath
 * is the thing being demonstrated.
 */
@Composable
fun GuidedBar(
    state: DemoState,
    onStep: (Int) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flow = state.flow
    AnimatedVisibility(
        visible = flow != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        // Chrome, not product — it keeps the dark palette in either theme.
        val p = DarkPalette
        if (flow == null) return@AnimatedVisibility

        Column(
            Modifier
                .fillMaxWidth()
                .background(p.viewport)
                .statusBarsPadding()
                .padding(horizontal = Space.sm, vertical = Space.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(
                    icon = Lucide.ChevronLeft,
                    description = "Previous step",
                    enabled = state.hasPrevious,
                ) { onStep(state.step - 1) }

                Column(
                    Modifier.weight(1f).padding(horizontal = Space.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "FLOW ${flow.id} · ${flow.label.uppercase()} · " +
                            "STEP ${state.step + 1} OF ${flow.size}",
                        style = GearType.labelXs,
                        color = p.hiVis500,
                    )
                }

                StepButton(
                    icon = Lucide.ChevronRight,
                    description = "Next step",
                    enabled = state.hasNext,
                ) { onStep(state.step + 1) }
            }

            VGap(Space.s)
            // The progress track: one segment per stop, so how far in you are is
            // readable without counting.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(flow.size) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(GearShape.full)
                            .background(
                                when {
                                    i < state.step -> p.hiVis600
                                    i == state.step -> p.hiVis500
                                    else -> p.pine700
                                }
                            )
                    )
                }
            }

            VGap(Space.s)
            Text(
                state.currentStep?.caption.orEmpty(),
                style = GearType.bodyS,
                color = p.chalk700,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            VGap(Space.s)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarAction(Lucide.RotateCw, "Restart", onRestart)
                BarAction(Lucide.X, "End walkthrough", onExit)
                Spacer(Modifier.weight(1f))
                Text(
                    Catalogue.byRoute(state.currentStep?.route.orEmpty())?.id.orEmpty(),
                    style = GearType.dataS,
                    color = p.chalk500,
                )
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: com.gearloop.app.ui.icons.LucideIcon,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val p = DarkPalette
    Box(
        Modifier
            .size(Metrics.tapTarget)
            .clip(GearShape.sm)
            .background(if (enabled) p.pine800 else p.chalk100)
            .then(
                if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
            )
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(icon, description, size = IconSize.m, tint = p.hiVis500, strokeWidth = 2.25f)
    }
}

@Composable
private fun BarAction(
    icon: com.gearloop.app.ui.icons.LucideIcon,
    label: String,
    onClick: () -> Unit,
) {
    val p = DarkPalette
    Row(
        Modifier
            .clip(GearShape.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearIcon(icon, null, size = IconSize.s, tint = p.chalk600)
        Spacer(Modifier.width(6.dp))
        Text(label, style = GearType.labelM, color = p.chalk600)
    }
}

/** True when the app is currently drawing in the dark palette. */
@Composable
fun isDarkTheme(): Boolean = LocalPalette.current.isDark
