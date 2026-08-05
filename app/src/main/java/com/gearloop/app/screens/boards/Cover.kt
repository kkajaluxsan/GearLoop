package com.gearloop.app.screens.boards

import com.gearloop.app.nav.LocalThemeToggle
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gearloop.app.nav.Catalogue
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.Routes
import com.gearloop.app.nav.ScreenGroup
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
 * Page 00 — Cover.
 *
 * Also the demo's control panel: the three prototype flows, the presentation
 * boards, and the full screen index are all one tap from here, so nothing has
 * to be hunted for mid-viva.
 */
@Composable
fun CoverScreen(nav: Nav) {
    SystemBarIcons(darkSurface = true)
    Column(
        Modifier
            .fillMaxSize()
            .background(GearSurface.darkest)
            .verticalScroll(rememberSavedScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Space.screen),
    ) {
        VGap(Space.section)
        GearTag("OWN LESS · RIDE MORE", size = TagSize.FULL)
        VGap(Space.l)
        Text("GearLoop", style = GearType.displayXl, color = GearText.onDark)
        VGap(Space.s)
        Text(
            "Sports gear rental and exchange, built around the one thing that " +
                "actually stops people renting: not knowing what condition it's in, " +
                "or whether it will fit.",
            style = GearType.bodyL,
            color = GearColor.Chalk300,
        )

        VGap(Space.l)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            CoverStat("42", "Screens")
            CoverStat("26", "Components")
            CoverStat("3", "Flows")
        }

        VGap(Space.section)
        Eyebrow("Walk a flow", color = GearColor.HiVis500)
        VGap(Space.xs)
        Text(
            "Each one opens with a guide bar: the point of every screen, and " +
                "next / back so nothing has to be found mid-sentence.",
            style = GearType.bodyS,
            color = GearColor.Chalk400,
        )
        VGap(Space.sm)
        val icons = mapOf("A" to Lucide.Waves, "B" to Lucide.ArrowRightLeft, "C" to Lucide.ChartColumn)
        Catalogue.flows.forEachIndexed { i, flow ->
            if (i > 0) VGap(Space.s)
            FlowCard(
                title = "${flow.id} · ${flow.label}",
                body = flow.blurb,
                meta = "${flow.size} screens",
                icon = icons[flow.id] ?: Lucide.Workflow,
            ) { nav.startFlow(flow) }
        }

        VGap(Space.l)
        Eyebrow("Presentation boards", color = GearColor.HiVis500)
        VGap(Space.sm)
        BoardRow("Design system", "Palette, type specimen, spacing, radius", Lucide.Aperture) { nav.to(Routes.BOARD_FOUNDATIONS) }
        BoardRow("Components", "All 26, every state", Lucide.Boxes) { nav.to(Routes.BOARD_COMPONENTS) }
        BoardRow("Signature elements", "Gear tag, fit stamp, fairness meter", Lucide.Tag) { nav.to(Routes.BOARD_SIGNATURE) }
        BoardRow("The user journey", "Flow A, screen by screen", Lucide.Workflow) { nav.to(Routes.BOARD_JOURNEY) }
        BoardRow("Screen map", "All 42, grouped by area", Lucide.LayoutGrid) { nav.to(Routes.BOARD_MAP) }

        VGap(Space.l)
        Eyebrow("Everything else", color = GearColor.HiVis500)
        VGap(Space.sm)
        BoardRow("Screen index", "Jump to any screen by number", Lucide.List) { nav.to(Routes.SCREEN_INDEX) }
        BoardRow("Admin console (desktop)", "1440 × 1024 claims adjudication", Lucide.Monitor) { nav.to("s40") }
        BoardRow("Image credits", "Photography licences and attribution", Lucide.CircleHelp) { nav.to(Routes.CREDITS) }

        val theme = LocalThemeToggle.current
        BoardRow(
            title = if (theme.isDark) "Switch to light" else "Switch to dark",
            body = "Both themes are built. Hi-vis is the same in each — safety " +
                "information shouldn't depend on a setting.",
            icon = if (theme.isDark) Lucide.Sun else Lucide.Moon,
            onClick = theme.toggle,
        )
        BoardRow(
            title = "Reset the demo",
            body = "Clears every screen — the fairness slider, the fit profile, the filters.",
            icon = Lucide.RotateCw,
            onClick = nav::resetDemo,
        )

        VGap(Space.l)
        Text(
            "Start onboarding from S-01",
            style = GearType.labelM,
            color = GearColor.HiVis500,
            modifier = Modifier
                .clip(GearShape.sm)
                .clickable(role = Role.Button) { nav.to("s01") }
                .padding(vertical = 10.dp),
        )
        VGap(Space.section)
    }
}

@Composable
private fun CoverStat(value: String, label: String) {
    Column(
        Modifier
            .clip(GearShape.md)
            .border(Border.hair, GearColor.Pine700, GearShape.md)
            .padding(horizontal = Space.m, vertical = Space.sm),
    ) {
        Text(value, style = GearType.dataXl, color = GearColor.HiVis500)
        Text(label.uppercase(), style = GearType.labelXs, color = GearColor.Chalk400)
    }
}

@Composable
private fun FlowCard(
    title: String,
    body: String,
    meta: String,
    icon: LucideIcon,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.md)
            .background(GearColor.Pine800)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(GearShape.sm)
                .background(GearColor.HiVis500),
            contentAlignment = Alignment.Center,
        ) {
            GearIcon(icon, null, size = IconSize.m, tint = GearColor.Pine900)
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.headingM, color = GearText.onDark)
            VGap(4.dp)
            Text(body, style = GearType.bodyS, color = GearColor.Chalk400)
            VGap(6.dp)
            Text(meta, style = GearType.dataS, color = GearColor.HiVis500)
        }
        GearIcon(Lucide.ArrowRight, null, size = IconSize.m, tint = GearColor.Chalk300)
    }
}

@Composable
private fun BoardRow(title: String, body: String, icon: LucideIcon, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearIcon(icon, null, size = IconSize.m, tint = GearColor.HiVis500)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.onDark)
            Text(body, style = GearType.bodyS, color = GearColor.Chalk400)
        }
        GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk500)
    }
}

/**
 * Every screen in the file, grouped the way the design brief groups them.
 * This is the screen that makes it possible to answer "can you show me S-27?"
 * without scrolling through a prototype.
 */
@Composable
fun ScreenIndex(nav: Nav) {
    GearScreen(
        appBar = { AppBar(title = "All 42 screens", onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Catalogue.tierCounts.toSortedMap().forEach { (tier, count) ->
                StatTile("$count", "Tier $tier", Modifier.weight(1f))
            }
        }
        VGap(Space.l)

        ScreenGroup.entries.forEach { group ->
            val screens = Catalogue.screens.filter { it.group == group }
            if (screens.isEmpty()) return@forEach
            SectionHeader(
                eyebrow = "${group.page} · ${group.label}",
                trailing = { Text("${screens.size}", style = GearType.dataM, color = GearText.tertiary) },
            )
            VGap(Space.s)
            screens.forEach { spec ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.sm)
                        .clickable(role = Role.Button) { nav.to(spec.route) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        spec.id,
                        style = GearType.dataM,
                        color = GearText.tertiary,
                        modifier = Modifier.width(52.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(spec.title, style = GearType.labelL, color = GearText.primary)
                            if (spec.starred) {
                                GearIcon(Lucide.Star, "Highlighted", size = 13.dp, tint = GearColor.HiVis600, filled = true)
                            }
                        }
                        if (spec.note.isNotEmpty()) {
                            VGap(2.dp)
                            Text(spec.note, style = GearType.bodyS, color = GearText.tertiary)
                        }
                    }
                    GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
                }
                GearDivider()
            }
            VGap(Space.l)
        }
        VGap(Space.section)
    }
}

/**
 * Attribution for the photography. Every image is CC or public domain, and the
 * licences used all permit the cropping and colour grading the app applies.
 */
@Composable
fun CreditsScreen(nav: Nav) {
    GearScreen(
        appBar = { AppBar(title = "Image credits", onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        Text(
            "All photography is Creative Commons or public domain, sourced through " +
                "Openverse. Only licences that permit derivative works were used, because " +
                "every frame is re-cropped and colour-graded to a common treatment.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.s)
        Text(
            "Icons are Lucide (ISC). Type is Archivo, Instrument Sans and JetBrains Mono, " +
                "all SIL Open Font Licence.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )
        VGap(Space.l)

        com.gearloop.app.data.photoCredits.forEach { credit ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(credit.title, style = GearType.bodyM, color = GearText.primary, maxLines = 2)
                    VGap(2.dp)
                    Text(
                        "${credit.creator} · ${credit.source}",
                        style = GearType.bodyS,
                        color = GearText.tertiary,
                    )
                }
                Spacer(Modifier.width(Space.s))
                Text(
                    credit.licence.ifEmpty { "CC" },
                    style = GearType.dataS,
                    color = GearText.secondary,
                    modifier = Modifier
                        .clip(GearShape.xs)
                        .background(GearColor.Chalk100)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            GearDivider()
        }
        VGap(Space.section)
    }
}
