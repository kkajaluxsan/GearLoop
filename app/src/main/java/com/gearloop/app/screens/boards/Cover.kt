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
import com.gearloop.app.ui.theme.Text
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
import androidx.compose.ui.platform.LocalUriHandler
import com.gearloop.app.BuildConfig
import com.gearloop.app.data.repo.DemoData

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
        // Counted from the catalogue, so the cover agrees with the index it opens:
        // a hardcoded 42 sat over the 44 the app now has (S-15b and S-18b included).
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            CoverStat("${Catalogue.screens.size}", "Screens")
            CoverStat("26", "Components")
            CoverStat("${Catalogue.flows.size}", "Flows")
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
        BoardRow("Screen map", "All ${Catalogue.screens.size}, grouped by area", Lucide.LayoutGrid) { nav.to(Routes.BOARD_MAP) }

        VGap(Space.l)
        Eyebrow("Everything else", color = GearColor.HiVis500)
        VGap(Space.sm)
        BoardRow("Screen index", "Jump to any screen by number", Lucide.List) { nav.to(Routes.SCREEN_INDEX) }
        BoardRow("Admin console (desktop)", "1440 × 1024 claims adjudication", Lucide.Monitor) { nav.to("s40") }
        BoardRow("Image credits", "Photography licences and attribution", Lucide.CircleHelp) { nav.to(Routes.CREDITS) }
        BoardRow("Privacy policy", "What stays on the phone, which is everything", Lucide.ShieldCheck) { nav.to(Routes.PRIVACY) }

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
            body = "Puts every record back as seeded and clears every screen. Run it between usability sessions.",
            icon = Lucide.RotateCw,
            onClick = nav::resetDemo,
        )

        VGap(Space.l)
        Eyebrow("Prepared states", color = GearColor.HiVis500)
        VGap(Space.xs)
        Text(
            "Each one resets the data, then runs the same steps a renter would, so " +
                "the screen it opens on is reached by the app rather than drawn.",
            style = GearType.bodyS,
            color = GearColor.Chalk400,
        )
        VGap(Space.sm)
        BoardRow(
            title = DemoData.Scenario.MID_RENTAL.title,
            body = DemoData.Scenario.MID_RENTAL.blurb,
            icon = Lucide.Package,
        ) { nav.play(DemoData.Scenario.MID_RENTAL, "s17") }
        BoardRow(
            title = DemoData.Scenario.CLAIM_OPEN.title,
            body = DemoData.Scenario.CLAIM_OPEN.blurb,
            icon = Lucide.TriangleAlert,
        ) { nav.play(DemoData.Scenario.CLAIM_OPEN, "s34") }

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
        appBar = { AppBar(title = "All ${Catalogue.screens.size} screens", onBack = { nav.back() }) },
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
                "Openverse and Wikimedia Commons. Only licences that permit derivative works were used, because " +
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

/**
 * Where the full policy is published. Play Console and App Store Connect both
 * require a public URL, and the store listing, the policy page and this screen
 * must all quote the same one.
 */
private const val PRIVACY_POLICY_URL = "https://kkajaluxsan.github.io/gearloop-privacy/"

/**
 * The privacy policy, in the app as well as on the web. The app runs offline,
 * so the substance is on this screen and the link to the full text is an extra.
 * Every point here is checkable against the build: the merged manifest asks for
 * no platform permission, so the app cannot reach the network, the camera or
 * location even if a later screen tried, and Auto Backup is switched off.
 */
@Composable
fun PrivacyScreen(nav: Nav) {
    val uriHandler = LocalUriHandler.current
    GearScreen(
        appBar = { AppBar(title = "Privacy policy", onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        Text(
            "What you do in GearLoop stays on this phone.",
            style = GearType.headingM,
            color = GearText.primary,
        )
        VGap(Space.s)
        Text(
            "Bookings, messages, reviews and your fit profile are saved in a database " +
                "on the device so they are still there tomorrow. None of it is sent anywhere.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.l)

        PrivacyPoint(
            Lucide.ShieldCheck, "Nothing leaves your phone",
            "The app has no internet permission, so Android will not let it open a " +
                "connection, and it is excluded from cloud backup.",
        )
        PrivacyPoint(
            Lucide.Lock, "No account",
            "There is no sign-up and no login. The profile on this phone is a sample " +
                "one, so there is nothing to identify you by.",
        )
        PrivacyPoint(
            Lucide.FileText, "Stored only on the device",
            "Your measurements, bookings and messages live in the app's private " +
                "storage. Uninstalling the app, or Reset the demo on the cover, removes them.",
        )
        PrivacyPoint(
            Lucide.Shield, "No camera, location or contacts",
            "Condition capture records sample frames with the agreed pickup point, " +
                "and the map is drawn. The app never asks for your camera, location, contacts or photos.",
        )
        PrivacyPoint(
            Lucide.CircleHelp, "Sample people, sample money",
            "The other people, listings and prices are demonstration data. Payments " +
                "are recorded in the app's wallet, and no real money is ever taken.",
        )

        VGap(Space.l)
        Text(
            "The full policy, including your rights under the GDPR and Sri Lanka's " +
                "Personal Data Protection Act, is published online.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.sm)
        GearButton(
            label = "Read the full policy",
            onClick = { runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) } },
            type = ButtonType.SECONDARY,
            trailingIcon = Lucide.ExternalLink,
            modifier = Modifier.fillMaxWidth(),
        )
        VGap(Space.s)
        Text(
            "Opens in your browser. That request is made by the browser, not by GearLoop.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )
        VGap(Space.l)
        Text(
            "VERSION ${BuildConfig.VERSION_NAME} · UPDATED 2 OCT 2026",
            style = GearType.dataS,
            color = GearText.tertiary,
        )
        VGap(Space.section)
    }
}

@Composable
private fun PrivacyPoint(icon: LucideIcon, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Space.s), verticalAlignment = Alignment.Top) {
        // Ink, like the title beside it: pine/800 vanished on the dark surface (QA-08).
        GearIcon(icon, null, size = IconSize.m, tint = GearText.primary)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.primary)
            VGap(2.dp)
            Text(body, style = GearType.bodyS, color = GearText.secondary)
        }
    }
}
