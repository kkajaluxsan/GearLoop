package com.gearloop.app.screens.booking

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.PriceLine
import com.gearloop.app.data.lkr
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

/* ------------------------------------------------------------------ S-09 */

private data class Plan(
    val name: String,
    val price: String,
    val bullets: List<Pair<String, Boolean>>,
    val mostChosen: Boolean = false,
)

private val plans = listOf(
    Plan(
        "Basic", "Included",
        listOf(
            "${lkr(25_000)} deposit held" to true,
            "You cover damage up to the deposit" to true,
            "Theft not covered" to false,
        ),
    ),
    Plan(
        "Plus", "+${lkr(420)} / day",
        listOf(
            "No deposit" to true,
            "${lkr(3_000)} excess on damage" to true,
            "Theft not covered" to false,
        ),
        mostChosen = true,
    ),
    Plan(
        "Pro", "+${lkr(780)} / day",
        listOf(
            "No deposit" to true,
            "No excess" to true,
            "Theft covered" to true,
        ),
    ),
)

@Composable
fun S09Protection(nav: Nav) {
    var selected by rememberSaveable { mutableIntStateOf(1) }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    "Continue with ${plans[selected].name}",
                    { nav.to("s10") },
                    Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        ProgressStepper(step = 2, total = 5)
        VGap(Space.l)
        Text("How do you want to be covered?", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.s)
        Text(
            "A deposit is a hold, not a charge. Protection replaces the hold with an excess.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.l)

        plans.forEachIndexed { i, plan ->
            PlanCard(plan, i == selected) { selected = i }
            VGap(Space.listGap)
        }

        VGap(Space.s)
        GearLink("See exactly what's covered", { }, icon = Lucide.ExternalLink)
        VGap(Space.section)
    }
}

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    val highlight = plan.mostChosen
    Column(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.md)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) GearColor.Pine800 else GearSurface.border,
                shape = GearShape.md,
            )
            .background(if (highlight) GearColor.HiVis200 else GearSurface.card)
            // One of three, not three separate buttons — the whole card is the
            // radio, and the dot inside it is only the picture of one.
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Space.cardPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GearRadio(selected)
            Spacer(Modifier.width(Space.sm))
            Text(plan.name, style = GearType.headingM, color = GearText.primary)
            if (highlight) {
                Spacer(Modifier.width(Space.s))
                Text(
                    "MOST CHOSEN",
                    style = GearType.labelXs,
                    color = GearColor.Pine900,
                    modifier = Modifier
                        .clip(GearShape.xs)
                        .background(GearColor.HiVis500)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(plan.price, style = GearType.dataL, color = GearText.primary)
        }
        VGap(Space.sm)
        plan.bullets.forEach { (label, yes) ->
            IconRow(
                icon = if (yes) Lucide.Check else Lucide.X,
                text = label,
                tint = if (yes) GearColor.Success500 else GearColor.Chalk400,
                textColor = if (yes) GearText.body else GearText.tertiary,
                style = GearType.bodyS,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ S-10 */

/**
 * The waiver is the unusual screen, and the interaction is the point: the
 * accept button stays disabled until the text has actually been scrolled to the
 * end. Consent you didn't read isn't consent.
 */
@Composable
fun S10Waiver(nav: Nav) {
    val checks = rememberSavedStateList { listOf(false, false, false) }
    var signature by rememberSaveable { mutableStateOf("") }
    val waiverScroll = rememberSavedScrollState()

    val scrolledToEnd by remember {
        derivedStateOf { waiverScroll.value >= waiverScroll.maxValue - 8 }
    }
    val canAccept = checks.all { it } && scrolledToEnd && signature.isNotEmpty()

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    label = "I accept and continue",
                    onClick = { nav.to("s11") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canAccept,
                )
            }
        },
    ) {
        ProgressStepper(step = 3, total = 5)
        VGap(Space.l)
        Text("Before you ride", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.m)
        Banner(
            text = "Surfing carries risk of injury. Read and accept before booking.",
            kind = BannerKind.SAFETY,
        )

        VGap(Space.l)
        Eyebrow("Requirements")
        VGap(Space.s)
        Demo.waiverChecks.forEachIndexed { i, text ->
            CheckRow(text, checks[i], { checks[i] = it })
            VGap(Space.xs)
        }

        VGap(Space.m)
        GearDivider()
        VGap(Space.m)

        Eyebrow("Waiver")
        VGap(Space.s)
        Box(Modifier.fillMaxWidth().height(220.dp)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(GearShape.sm)
                    .border(Border.hair, GearSurface.border, GearShape.sm)
                    .background(GearColor.Chalk50)
                    .verticalScroll(waiverScroll)
                    .padding(Space.sm),
            ) {
                Text(Demo.waiverBody, style = GearType.bodyS, color = GearText.body)
            }
            // Fade at the bottom, so it is obvious there is more to read.
            if (!scrolledToEnd) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, GearColor.Chalk50)
                            )
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Row(
                        Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Scroll to the end", style = GearType.labelXs, color = GearText.tertiary)
                        GearIcon(Lucide.ChevronDown, null, size = IconSize.s, tint = GearText.tertiary)
                    }
                }
            }
        }
        VGap(Space.s)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GearIcon(
                if (scrolledToEnd) Lucide.CircleCheck else Lucide.Circle,
                null,
                size = IconSize.s,
                tint = if (scrolledToEnd) GearColor.Success500 else GearColor.Chalk400,
            )
            Text(
                if (scrolledToEnd) "Read to the end" else "Not read yet",
                style = GearType.bodyS,
                color = if (scrolledToEnd) GearColor.Success500 else GearText.tertiary,
            )
        }

        VGap(Space.l)
        GearTextField(
            value = signature,
            label = "Type your full name",
            placeholder = "Your full name",
            state = if (signature.isEmpty()) FieldState.DEFAULT else FieldState.FILLED,
            onClick = { signature = Demo.nirmal.name },
        )
        if (signature.isNotEmpty()) {
            VGap(Space.s)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(GearShape.sm)
                    .background(GearColor.Chalk50)
                    .padding(Space.sm),
            ) {
                Text(
                    signature,
                    style = GearType.headingL.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                    color = GearColor.Pine900,
                )
                GearDivider(Modifier.padding(top = 4.dp), color = GearColor.Chalk300)
                VGap(4.dp)
                Text("Signed 5 Aug 2026, 09:14", style = GearType.dataS, color = GearText.tertiary)
            }
        }

        VGap(Space.m)
        Text(
            "Waiver v2.1 · also available in தமிழ் and සිංහල",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )
        VGap(Space.section)
    }
}

/* ------------------------------------------------------------------ S-11 */

@Composable
fun S11ReviewPay(nav: Nav) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var payment by rememberSaveable { mutableIntStateOf(0) }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter(priceValue = lkr(Demo.BOOKING_TOTAL), priceCaption = "3 days, all in") {
                GearButton("Confirm and pay", { nav.to("s12") }, size = ButtonSize.M)
            }
        },
    ) {
        ProgressStepper(step = 4, total = 5)
        VGap(Space.l)

        Row(verticalAlignment = Alignment.CenterVertically) {
            GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(64.dp), GearShape.sm)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(Demo.torq.shortTitle, style = GearType.headingS, color = GearText.primary)
                VGap(4.dp)
                GearTag(Demo.torq.tagLine)
                VGap(5.dp)
                Text(Demo.BOOKING_DATES_LONG, style = GearType.dataM, color = GearText.secondary)
            }
        }

        VGap(Space.l)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Demo.priceBreakdown.forEachIndexed { i, line ->
                    PriceRow(line)
                    if (i != Demo.priceBreakdown.lastIndex) VGap(Space.s)
                }
            }
        }

        VGap(Space.s)
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("How is this calculated?", style = GearType.labelM, color = GearColor.Marine500)
            Spacer(Modifier.weight(1f))
            GearIcon(
                if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                null, size = IconSize.s, tint = GearColor.Marine500,
            )
        }
        if (expanded) {
            Text(
                "The service fee is 9% of the rental after discounts, capped at LKR 1,200. " +
                    "Protection is charged per day and is refunded pro rata if you return early. " +
                    "Coupons apply to the rental line only, never to the deposit.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.s)
        }

        VGap(Space.m)
        Banner(
            text = "No deposit with Plus protection. Nothing is held on your card.",
            kind = BannerKind.SAFETY,
        )

        VGap(Space.l)
        Eyebrow("Payment method")
        VGap(Space.s)
        listOf(
            Triple(Lucide.CreditCard, "Visa •••• 4471", "Expires 09/28"),
            Triple(Lucide.Smartphone, "eZ Cash", "077 ••• ••••"),
            Triple(Lucide.Banknote, "Cash on pickup", "Deposit still held"),
        ).forEachIndexed { i, (icon, title, sub) ->
            PaymentRow(icon, title, sub, payment == i) { payment = i }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("Cancellation")
        VGap(Space.xs)
        Text(
            "Free cancellation until Wed 5 Aug, 09:00. After that, 50% refund up to 24 h before pickup.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )

        VGap(Space.m)
        Text(
            "By confirming you agree to the GearLoop rental terms, the waiver you signed, " +
                "and Kavitha's house rules.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )
        VGap(Space.section)
    }
}

@Composable
private fun PaymentRow(
    icon: LucideIcon,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.sm)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) GearColor.Pine800 else GearSurface.border,
                GearShape.sm,
            )
            .background(GearSurface.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.sm, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearRadio(selected)
        Spacer(Modifier.width(Space.sm))
        GearIcon(icon, null, size = IconSize.m, tint = GearColor.Pine800)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.primary)
            Text(subtitle, style = GearType.bodyS, color = GearText.tertiary)
        }
    }
}

/* ------------------------------------------------------------------ S-12 */

@Composable
fun S12Confirmed(nav: Nav) {
    Column(
        Modifier
            .fillMaxSize()
            .background(GearSurface.app)
            .verticalScroll(rememberSavedScrollState()),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(GearColor.Pine800)
                .padding(horizontal = Space.screen),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(GearShape.full)
                    .border(3.dp, GearColor.HiVis500, GearShape.full),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(Lucide.Check, null, size = 44.dp, tint = GearColor.HiVis500, strokeWidth = 2.5f)
            }
            VGap(Space.l)
            Text("You're booked", style = GearType.displayXl, color = GearText.onDark)
            VGap(Space.s)
            Text(Demo.BOOKING_REF, style = GearType.dataL, color = GearColor.HiVis500)
        }

        // The card overlaps the dark panel above it by 40dp.
        Column(
            Modifier
                .offset(y = (-40).dp)
                .padding(horizontal = Space.screen)
        ) {
            GearCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                    GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(64.dp), GearShape.sm)
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Text(Demo.torq.shortTitle, style = GearType.headingS, color = GearText.primary)
                        VGap(4.dp)
                        Text(Demo.BOOKING_DATES_LONG, style = GearType.dataM, color = GearText.secondary)
                        VGap(5.dp)
                        GearTag(Demo.torq.tagLine)
                    }
                }
            }

            VGap(Space.l)
            Eyebrow("What happens next")
            VGap(Space.sm)
            Timeline(Demo.bookingTimeline)

            VGap(Space.l)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                GearButton(
                    "Add to calendar", { }, Modifier.weight(1f),
                    type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Calendar,
                )
                GearButton(
                    "Message", { nav.to("s33") }, Modifier.weight(1f),
                    type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.MessageSquare,
                )
            }
            VGap(Space.sm)
            GearButton(
                "View booking", { nav.to("s13") }, Modifier.fillMaxWidth(),
                type = ButtonType.GHOST, size = ButtonSize.M,
            )
            VGap(Space.section)
        }
    }
}
