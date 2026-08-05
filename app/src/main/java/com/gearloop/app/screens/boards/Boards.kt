package com.gearloop.app.screens.boards

import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Fit
import com.gearloop.app.data.PriceLine
import com.gearloop.app.data.grouped
import com.gearloop.app.data.lkr
import com.gearloop.app.nav.Catalogue
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.ScreenGroup
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.Elevation
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Radius
import com.gearloop.app.ui.theme.Space

/* ---------------------------------------------------------- foundations */

@Composable
fun FoundationsBoard(nav: Nav) {
    GearScreen(appBar = { AppBar(title = "Design system", onBack = { nav.back() }) }) {
        VGap(Space.s)
        BoardIntro(
            "The whole visual language comes from one object: the tag zip-tied to " +
                "rental gear. Mono type for anything measurable, hi-vis reserved for " +
                "action and safety, pine for structure."
        )

        BoardSection("Pine — structure")
        SwatchRow(
            listOf(
                "pine/900" to GearColor.Pine900, "pine/800" to GearColor.Pine800,
                "pine/700" to GearColor.Pine700, "pine/600" to GearColor.Pine600,
                "pine/500" to GearColor.Pine500,
            )
        )

        BoardSection("Chalk — neutrals")
        SwatchRow(
            listOf(
                "chalk/000" to GearColor.Chalk000, "chalk/50" to GearColor.Chalk50,
                "chalk/100" to GearColor.Chalk100, "chalk/200" to GearColor.Chalk200,
                "chalk/300" to GearColor.Chalk300,
            )
        )
        VGap(Space.s)
        SwatchRow(
            listOf(
                "chalk/400" to GearColor.Chalk400, "chalk/500" to GearColor.Chalk500,
                "chalk/600" to GearColor.Chalk600, "chalk/700" to GearColor.Chalk700,
            )
        )

        BoardSection("Hi-vis — action and safety only")
        SwatchRow(
            listOf(
                "hivis/600" to GearColor.HiVis600, "hivis/500" to GearColor.HiVis500,
                "hivis/400" to GearColor.HiVis400, "hivis/200" to GearColor.HiVis200,
                "hivis/100" to GearColor.HiVis100,
            )
        )
        VGap(Space.s)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .background(GearColor.HiVis500)
                .padding(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Text on hi-vis is always pine/900", style = GearType.labelL, color = GearColor.Pine900)
            Spacer(Modifier.weight(1f))
            Text("11.9:1", style = GearType.dataM, color = GearColor.Pine900)
        }

        BoardSection("Marine — informational")
        SwatchRow(
            listOf(
                "marine/700" to GearColor.Marine700, "marine/500" to GearColor.Marine500,
                "marine/300" to GearColor.Marine300, "marine/100" to GearColor.Marine100,
            )
        )

        BoardSection("Semantic")
        SwatchRow(
            listOf(
                "success" to GearColor.Success500, "warning" to GearColor.Warning500,
                "danger" to GearColor.Danger500,
            )
        )

        BoardSection("Fit ramp — its own scale")
        VGap(Space.xs)
        Text(
            "Fit is a distinct system from status, so it gets its own colours. " +
                "Wrong-sized gear is a safety problem, not a preference.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.s)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Fit.entries.forEach { FitChip(it) }
        }

        BoardSection("Type — Archivo · Instrument Sans · JetBrains Mono")
        TypeSpecimen("display/xl", "Own less", GearType.displayXl)
        TypeSpecimen("display/l", "Ride more", GearType.displayL)
        TypeSpecimen("heading/xl", "Torq Mod Fish 6'6\"", GearType.headingXl)
        TypeSpecimen("heading/l", "Is this a fair trade?", GearType.headingL)
        TypeSpecimen("heading/m", "What's included", GearType.headingM)
        TypeSpecimen("heading/s", "Kavitha Selvam", GearType.headingS)
        TypeSpecimen("body/l", "Get what you need, only when you need it", GearType.bodyL)
        TypeSpecimen("body/m", "Exact address shared once booked", GearType.bodyM)
        TypeSpecimen("body/s", "Based on 174 cm, 68 kg", GearType.bodyS)
        TypeSpecimen("label/l", "Book instantly", GearType.labelL)
        TypeSpecimen("label/m", "Instant book", GearType.labelM)
        TypeSpecimen("label/xs", "GETTING STARTED", GearType.labelXs)
        TypeSpecimen("data/xl", "LKR 9,240", GearType.dataXl)
        TypeSpecimen("data/l", "02:14:06", GearType.dataL)
        TypeSpecimen("data/m", "42 L · 2.4 km", GearType.dataM)
        TypeSpecimen("data/s", "6'6\" · GRADE B · TQ-4471", GearType.dataS)

        BoardSection("Spacing — 4 base")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64).forEach { v ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width(v.dp)
                            .height(48.dp)
                            .clip(GearShape.xs)
                            .background(GearColor.Pine800)
                    )
                    VGap(4.dp)
                    Text("$v", style = GearType.dataS, color = GearText.tertiary)
                }
            }
        }

        BoardSection("Radius — engineered, not soft")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(
                "xs" to Radius.xs, "sm" to Radius.sm, "md" to Radius.md,
                "lg" to Radius.lg, "full" to Radius.full,
            ).forEach { (name, r) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(r))
                            .background(GearColor.Chalk200)
                            .border(Border.hair, GearColor.Chalk300, androidx.compose.foundation.shape.RoundedCornerShape(r))
                    )
                    VGap(4.dp)
                    Text(name, style = GearType.dataS, color = GearText.tertiary)
                }
            }
        }

        BoardSection("Elevation — sparse")
        VGap(Space.xs)
        Text(
            "Cards are separated by a 1px border, not a shadow. Shadow is reserved " +
                "for things that genuinely float above the content.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.sm)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf("sm" to Elevation.sm, "md" to Elevation.md, "lg" to Elevation.lg).forEach { (name, e) ->
                Box(
                    Modifier
                        .size(72.dp)
                        .shadow(e, GearShape.md, spotColor = GearColor.Pine900)
                        .clip(GearShape.md)
                        .background(GearSurface.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(name, style = GearType.dataS, color = GearText.tertiary)
                }
            }
        }
        VGap(Space.section)
    }
}

@Composable
private fun SwatchRow(items: List<Pair<String, Color>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        items.forEach { (name, color) ->
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(GearShape.sm)
                        .background(color)
                        .border(Border.hair, GearColor.Chalk200, GearShape.sm)
                )
                VGap(5.dp)
                Text(name, style = GearType.dataS, color = GearText.tertiary)
                Text(color.hex(), style = GearType.dataS, color = GearText.placeholder)
            }
        }
    }
}

private fun Color.hex(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

@Composable
private fun TypeSpecimen(name: String, sample: String, style: TextStyle) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(name, style = GearType.dataS, color = GearText.placeholder)
        VGap(3.dp)
        Text(sample, style = style, color = GearText.primary, maxLines = 1)
    }
    GearDivider()
}

@Composable
private fun BoardSection(title: String) {
    VGap(Space.l)
    Eyebrow(title)
    VGap(Space.sm)
}

@Composable
private fun BoardIntro(text: String) {
    Text(text, style = GearType.bodyM, color = GearText.secondary)
}

/* ----------------------------------------------------------- components */

@Composable
fun ComponentsBoard(nav: Nav) {
    var toggleA by rememberSaveable { mutableStateOf(true) }
    var toggleB by rememberSaveable { mutableStateOf(false) }
    var checked by rememberSaveable { mutableStateOf(true) }
    var segment by rememberSaveable { mutableStateOf(0) }
    var slider by rememberSaveable { mutableFloatStateOf(0.42f) }

    GearScreen(appBar = { AppBar(title = "Components", onBack = { nav.back() }) }) {
        VGap(Space.s)
        Text(
            "26 components. Every screen in the file is assembled from these — " +
                "nothing is drawn twice.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )

        Spec("Button — type × size × state")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            GearButton("Primary L", {}, size = ButtonSize.L)
            GearButton("Primary M", {}, size = ButtonSize.M)
            GearButton("Small", {}, size = ButtonSize.S)
        }
        VGap(Space.s)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            GearButton("Secondary", {}, type = ButtonType.SECONDARY, size = ButtonSize.M)
            GearButton("Ghost", {}, type = ButtonType.GHOST, size = ButtonSize.M)
            GearButton("Danger", {}, type = ButtonType.DANGER, size = ButtonSize.M)
        }
        VGap(Space.s)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            GearButton("Disabled", {}, size = ButtonSize.M, enabled = false)
            GearButton("Loading", {}, size = ButtonSize.M, loading = true)
            GearButton("With icon", {}, size = ButtonSize.M, icon = Lucide.Zap)
        }

        Spec("IconButton")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            GearIconButton(Lucide.Heart, "Save", {})
            GearIconButton(Lucide.Share2, "Share", {}, tone = IconButtonTone.ON_IMAGE)
            GearIconButton(Lucide.Navigation, "Directions", {}, tone = IconButtonTone.DARK)
            GearIconButton(Lucide.Send, "Send", {}, tone = IconButtonTone.HI_VIS)
            GearIconButton(Lucide.Bell, "Alerts", {}, badge = "3")
        }

        Spec("Chip")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Chip("Default")
            Chip("Selected", selected = true)
            Chip("With icon", icon = Lucide.Zap)
            Chip("With count", count = 24)
            Chip("Removable", selected = true, trailingIcon = Lucide.X)
        }

        Spec("GearTag ⭐")
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            GearTag(Demo.torq.tagLine)
            GearTag(Demo.torq.tagLine, size = TagSize.FULL)
            GearTag(Demo.wetsuit.tagLine, tone = TagTone.MUTED)
            GearTag("GEARLOOP · OPS", tone = TagTone.DARK)
        }

        Spec("FitStamp ⭐")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.m), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Fit.entries.forEach { FitStamp(it) }
        }

        Spec("FairnessMeter ⭐")
        FairnessMeter(percent = 31, leftValue = 42_000, rightValue = 61_000, deltaAmount = 19_000)
        VGap(Space.m)
        FairnessMeter(percent = 0, leftValue = 61_000, rightValue = 61_000, deltaAmount = 0)

        Spec("FairnessBar — compact variant")
        FairnessBar(31)
        VGap(Space.s)
        FairnessBar(-8)

        Spec("ListingCard — grid · list · wide")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            ListingCard(Demo.torq, {}, layout = ListingLayout.GRID)
            ListingCard(Demo.wetsuit, {}, layout = ListingLayout.GRID)
        }
        VGap(Space.s)
        ListingCard(Demo.trekMarlin, {}, layout = ListingLayout.LIST)
        VGap(Space.s)
        ListingCard(Demo.nspSup, {}, layout = ListingLayout.WIDE)

        Spec("SwapCard")
        SwapCard(Demo.swapMatches[0], {})

        Spec("BookingCard")
        BookingCard(Demo.torq, Demo.BOOKING_DATES, "In progress", GearColor.Success500, "Start return", {}, reference = Demo.BOOKING_REF)

        Spec("OwnerStrip")
        OwnerStrip(Demo.kavitha)

        Spec("PriceRow")
        PriceRow(PriceLine("LKR 2,800 × 3 days", grouped(8_400)))
        VGap(Space.s)
        PriceRow(PriceLine("3-day discount", "−840", PriceLine.Kind.DISCOUNT))
        VGap(Space.s)
        PriceRow(PriceLine("Total", lkr(9_240), PriceLine.Kind.TOTAL))

        Spec("ReviewCard")
        ReviewCard(Demo.torqReviews[1])

        Spec("InsightCard")
        InsightCard(Demo.ownerInsights[0].headline, Demo.ownerInsights[0].body, Demo.ownerInsights[0].action, {})

        Spec("TextField — default · focus · error · disabled")
        GearTextField("", label = "Email", placeholder = "you@example.com")
        VGap(Space.s)
        GearTextField("nirmal@", label = "Email", state = FieldState.FOCUS)
        VGap(Space.s)
        GearTextField("nirmal@", label = "Email", error = "That doesn't look like an email address")
        VGap(Space.s)
        GearTextField("Locked", label = "Reference", state = FieldState.DISABLED)
        VGap(Space.s)
        GearTextField("2,800", label = "Price", prefix = "LKR", suffix = "/ day", state = FieldState.FILLED)

        Spec("SearchBar")
        SearchBar("Search gear, sports, brands", onFilter = {}, filterCount = 2)

        Spec("Stepper · Toggle · Checkbox · Radio")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Stepper("174", {}, {})
            Toggle(checked = toggleA, onCheckedChange = { toggleA = it })
            Toggle(checked = toggleB, onCheckedChange = { toggleB = it })
            GearCheckbox(checked = checked, onCheckedChange = { checked = it })
            GearRadio(true)
            GearRadio(false)
        }

        Spec("Slider")
        GearSlider(slider, { slider = it }, valueLabel = lkr((slider * 5000).toInt()))

        Spec("DateRangeCell — all eight states")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            com.gearloop.app.data.DayState.entries.forEachIndexed { i, state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DateRangeCell("${i + 8}", state)
                    Text(state.name.lowercase(), style = GearType.labelXs, color = GearText.tertiary)
                }
            }
        }

        Spec("SegmentedControl")
        SegmentedControl(listOf("Active", "Upcoming", "Past"), segment) { segment = it }

        Spec("AppBar · TabBar · StickyFooter · ProgressStepper")
        ProgressStepper(step = 3, total = 6)
        VGap(Space.m)
        Box(Modifier.fillMaxWidth().clip(GearShape.md).border(Border.hair, GearSurface.border, GearShape.md)) {
            com.gearloop.app.screens.MainTabBar(nav, com.gearloop.app.screens.MainTab.EXPLORE)
        }

        Spec("Banner — five kinds")
        Banner("A neutral, informational message.", kind = BannerKind.INFO)
        VGap(Space.s)
        Banner("The thing you wanted to happen, happened.", kind = BannerKind.SUCCESS)
        VGap(Space.s)
        Banner("Something needs attention before it becomes a problem.", kind = BannerKind.WARNING)
        VGap(Space.s)
        Banner("Something has gone wrong and needs a decision.", kind = BannerKind.DANGER)
        VGap(Space.s)
        Banner(
            "Helmets and harnesses have to fit. Safety notices use hi-vis, the same way a life jacket does.",
            kind = BannerKind.SAFETY,
        )

        Spec("Toast")
        Toast("Saved to your closet", undoLabel = "Undo", onUndo = {})

        Spec("Timeline")
        Timeline(Demo.bookingTimeline)

        Spec("EmptyState")
        EmptyState(
            icon = Lucide.Inbox,
            headline = "No requests yet",
            body = "When someone asks to rent your gear, it lands here.",
            actionLabel = "Share your listing",
            onAction = {},
        )

        Spec("Skeleton")
        SkeletonListRow()
        VGap(Space.sm)
        SkeletonListRow()

        VGap(Space.section)
    }
}

@Composable
private fun Spec(title: String) {
    VGap(Space.l)
    GearDivider()
    VGap(Space.sm)
    Eyebrow(title)
    VGap(Space.sm)
}

/* ------------------------------------------------------------ signature */

@Composable
fun SignatureBoard(nav: Nav) {
    GearScreen(appBar = { AppBar(title = "Signature elements", onBack = { nav.back() }) }) {
        VGap(Space.s)
        Text(
            "Three elements carry the whole design. Everything else on screen stays " +
                "quiet so these land.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )

        VGap(Space.l)
        SignatureBlockView(
            number = "①",
            title = "The Gear Tag",
            why = "Rental gear in the real world carries a tag zip-tied to it — item code, " +
                "size, condition grade, punched in monospace on hi-vis card stock. This is " +
                "that tag. It replaces the generic specs row every marketplace app has, and " +
                "it says: this is a real, individual, tracked object, not a catalogue SKU.",
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                GearTag(Demo.torq.tagLine, size = TagSize.FULL)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearTag(Demo.trekMarlin.tagLine)
                    GearTag(Demo.wetsuit.tagLine, tone = TagTone.MUTED)
                }
            }
        }

        VGap(Space.l)
        SignatureBlockView(
            number = "②",
            title = "The Fit Stamp",
            why = "Fit gets its own colour ramp because it is a safety property, not a " +
                "preference. Two numbers in onboarding — height and weight — turn into a " +
                "verdict on every listing, with the reason stated underneath it.",
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.m)) {
                FitStamp(Fit.PERFECT)
                Text(
                    "42 L for your 68 kg at intermediate level",
                    style = GearType.bodyS,
                    color = GearText.secondary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.m), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    FitStamp(Fit.GOOD)
                    FitStamp(Fit.WORKABLE)
                    FitStamp(Fit.POOR)
                    FitStamp(Fit.BLOCKED)
                    FitStamp(Fit.UNKNOWN)
                }
            }
        }

        VGap(Space.l)
        SignatureBlockView(
            number = "③",
            title = "The Fairness Meter",
            why = "An actual beam on a pivot, not a progress bar. It tilts with the value " +
                "difference between two people's gear and levels out as cash is added to " +
                "balance the trade. It is the one moment of motion in the app.",
        ) {
            Column {
                FairnessMeter(percent = 31, leftValue = 42_000, rightValue = 61_000, deltaAmount = 19_000)
                VGap(Space.m)
                FairnessMeter(percent = 0, leftValue = 61_000, rightValue = 61_000, deltaAmount = 0)
            }
        }

        VGap(Space.l)
        GearButton("See the fairness meter live", { nav.to("s21") }, Modifier.fillMaxWidth(), icon = Lucide.Scale)
        VGap(Space.section)
    }
}

@Composable
private fun SignatureBlockView(
    number: String,
    title: String,
    why: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(number, style = GearType.displayL, color = GearColor.HiVis600)
            Spacer(Modifier.width(Space.sm))
            Text(title, style = GearType.headingXl, color = GearText.primary)
        }
        VGap(Space.m)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.md)
                .background(GearColor.Chalk50)
                .border(Border.hair, GearSurface.border, GearShape.md)
                .padding(vertical = Space.l, horizontal = Space.m),
            contentAlignment = Alignment.Center,
        ) { content() }
        VGap(Space.sm)
        Text(why, style = GearType.bodyM, color = GearText.secondary)
    }
}

/* -------------------------------------------------------------- journey */

@Composable
fun JourneyBoard(nav: Nav) {
    GearScreen(appBar = { AppBar(title = "The user journey", onBack = { nav.back() }) }) {
        VGap(Space.s)
        Text(
            "All three flows, stop by stop, with the point each screen is making. " +
                "These are the same captions the walkthrough puts on screen — one " +
                "source, so the board and the demo can't drift apart.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )

        Catalogue.flows.forEach { flow ->
            VGap(Space.l)
            GearDivider()
            VGap(Space.l)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Flow ${flow.id} · ${flow.label}",
                    style = GearType.headingXl,
                    color = GearText.primary,
                    modifier = Modifier.weight(1f),
                )
                Text("${flow.size} screens", style = GearType.dataM, color = GearText.tertiary)
            }
            VGap(Space.xs)
            Text(flow.blurb, style = GearType.bodyS, color = GearText.secondary)
            VGap(Space.m)
            GearButton(
                "Walk this flow",
                { nav.startFlow(flow) },
                Modifier.fillMaxWidth(),
                type = ButtonType.SECONDARY,
                size = ButtonSize.M,
                icon = Lucide.Play,
            )
            VGap(Space.m)

            flow.steps.forEachIndexed { i, step ->
                val spec = Catalogue.byRoute(step.route) ?: return@forEachIndexed
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp),
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(GearShape.full)
                                .background(GearColor.Pine800),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", style = GearType.dataS, color = GearColor.HiVis500)
                        }
                        if (i != flow.steps.lastIndex) {
                            Box(Modifier.width(2.dp).height(74.dp).background(GearColor.Chalk200))
                        }
                    }
                    Spacer(Modifier.width(Space.sm))
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(role = Role.Button) { nav.to(step.route) }
                            .padding(bottom = Space.m),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                        ) {
                            Text(spec.id, style = GearType.dataM, color = GearText.tertiary)
                            Text(spec.title, style = GearType.headingS, color = GearText.primary)
                        }
                        VGap(3.dp)
                        Text(step.caption, style = GearType.bodyS, color = GearText.secondary)
                    }
                    GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
                }
            }
        }
        VGap(Space.section)
    }
}

/* ----------------------------------------------------------- screen map */

@Composable
fun ScreenMapBoard(nav: Nav) {
    GearScreen(appBar = { AppBar(title = "Screen map", onBack = { nav.back() }) }) {
        VGap(Space.s)
        Text(
            "42 screens designed across ten areas. The design system's job is to make " +
                "the rest buildable from the same 26 components.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.l)

        ScreenGroup.entries.forEach { group ->
            val screens = Catalogue.screens.filter { it.group == group }
            if (screens.isEmpty()) return@forEach
            Eyebrow("${group.page} · ${group.label}")
            VGap(Space.s)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                screens.forEach { spec ->
                    Column(
                        Modifier
                            .width(96.dp)
                            .clickable(role = Role.Button) { nav.to(spec.route) },
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(128.dp)
                                .clip(GearShape.sm)
                                .background(GearSurface.card)
                                .border(
                                    if (spec.starred) 2.dp else 1.dp,
                                    if (spec.starred) GearColor.HiVis500 else GearSurface.border,
                                    GearShape.sm,
                                ),
                        ) {
                            // A miniature of the frame: app bar, body, footer.
                            Column(Modifier.padding(5.dp)) {
                                Box(Modifier.fillMaxWidth().height(9.dp).clip(GearShape.xs).background(GearColor.Chalk200))
                                VGap(4.dp)
                                Box(Modifier.fillMaxWidth().height(40.dp).clip(GearShape.xs).background(GearColor.Chalk100))
                                VGap(4.dp)
                                repeat(3) {
                                    Box(Modifier.fillMaxWidth(if (it == 2) 0.6f else 1f).height(6.dp).clip(GearShape.xs).background(GearColor.Chalk200))
                                    VGap(3.dp)
                                }
                            }
                            Text(
                                spec.id,
                                style = GearType.dataS,
                                color = GearText.tertiary,
                                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp),
                            )
                            Text(
                                "T${spec.tier}",
                                style = GearType.labelXs,
                                color = GearText.placeholder,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                            )
                        }
                        VGap(4.dp)
                        Text(
                            spec.title,
                            style = GearType.labelXs,
                            color = GearText.secondary,
                            maxLines = 2,
                        )
                    }
                }
            }
            VGap(Space.l)
        }
        VGap(Space.section)
    }
}
