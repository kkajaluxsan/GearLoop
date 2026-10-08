package com.gearloop.app.screens.booking

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
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
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.container
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.PriceLine
import com.gearloop.app.data.Protection
import com.gearloop.app.data.TimelineNode
import com.gearloop.app.data.dayTime
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.Booking
import com.gearloop.app.data.repo.MoneyRepository
import com.gearloop.app.data.stamp
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
import com.gearloop.app.ui.vm.MoneyViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.ZoneId

/** Opened straight from the screen index, a checkout screen starts its own draft. */
@Composable
private fun rememberDraft(vm: MoneyViewModel): Booking? {
    val draft by vm.draft.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (vm.draft.value == null) vm.ensureDraft() }
    return draft
}

/* ------------------------------------------------------------------ S-09 */

/**
 * S-09 · Read the draft and the listing's deposit; Update the draft's plan.
 *
 * Each plan now states, as figures of equal weight, what is held on the card and
 * the most the renter could lose (FR-04, FR-05). Milestone 01's critical finding
 * was that the amount carried the screen and the verb — *held* — was small print,
 * so most people read the hold as a charge. The verb is now set in the same type
 * as the number beside it.
 */
@Composable
fun S09Protection(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val draft = rememberDraft(vm)
    var details by rememberSaveable { mutableStateOf(false) }
    val selected = draft?.protection ?: Protection.PLUS
    val deposit = draft?.item?.deposit ?: Demo.torq.deposit

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    "Continue with ${selected.label}",
                    { nav.to("s10") },
                    Modifier.fillMaxWidth(),
                    enabled = draft != null,
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

        Protection.entries.forEach { plan ->
            PlanCard(plan, deposit, plan == selected) { vm.setProtection(plan) }
            VGap(Space.listGap)
        }

        VGap(Space.s)
        GearLink(
            if (details) "Hide the details" else "See exactly what's covered",
            { details = !details },
            icon = if (details) Lucide.ChevronUp else Lucide.ChevronDown,
        )
        if (details) {
            VGap(Space.s)
            Text(
                "Damage is judged against the condition record you make together at pickup, " +
                    "so anything already on that record is never yours to pay for. Cosmetic " +
                    "marks are never claimable on their own. Functional damage is capped at " +
                    "60% of the item's value, and the most you can be charged is the figure " +
                    "under \"most you could lose\" for the plan you choose. Theft is covered " +
                    "only on Pro.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
        }
        VGap(Space.section)
    }
}

@Composable
private fun PlanCard(plan: Protection, deposit: Int, selected: Boolean, onClick: () -> Unit) {
    val highlight = plan == Protection.PLUS
    val price = if (plan.perDay == 0) "Included" else "+${lkr(plan.perDay)} / day"
    val bullets = when (plan) {
        Protection.BASIC -> listOf("You cover damage up to the deposit" to true, "Theft not covered" to false)
        Protection.PLUS -> listOf("${lkr(plan.excess ?: 0)} excess on damage" to true, "Theft not covered" to false)
        Protection.PRO -> listOf("No excess" to true, "Theft covered" to true)
    }
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
            Text(plan.label, style = GearType.headingM, color = GearText.primary)
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
            Text(price, style = GearType.dataL, color = GearText.primary)
        }
        VGap(Space.sm)
        // FR-04 and FR-05: the two numbers that decide whether money leaves the
        // account, side by side and in the same type as each other.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .background(GearSurface.card)
                .border(Border.hair, GearSurface.border, GearShape.sm)
                .padding(Space.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text("HELD, NOT CHARGED", style = GearType.labelXs, color = GearText.secondary)
                VGap(3.dp)
                Text(if (plan.holdsDeposit) lkr(deposit) else "Nothing", style = GearType.dataL, color = GearText.primary)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("MOST YOU COULD LOSE", style = GearType.labelXs, color = GearText.secondary)
                VGap(3.dp)
                Text(lkr(plan.mostYouCouldLose(deposit)), style = GearType.dataL, color = GearText.primary)
            }
        }
        VGap(Space.sm)
        bullets.forEach { (label, yes) ->
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

private val waterSports = setOf("Surfing", "SUP", "Kayaking", "Swimming")

/** The declarations a renter ticks, for the kind of activity the gear is for. */
private fun waiverChecksFor(item: GearItem): List<String> = when {
    item.sport in waterSports -> Demo.waiverChecks
    item.sport == "Cycling" -> listOf(
        "I'll wear a helmet every time I ride",
        "I've ridden off-road before, or I'm going with someone who has",
        "I'll check the brakes and tyres before I set off",
    )
    else -> listOf(
        "I've used this kind of gear before, or I'm going with someone who has",
        "I'll check the gear before I use it",
        "I'll stop and report it if anything seems unsafe",
    )
}

private fun waiverBodyFor(item: GearItem): String =
    if (item.sport in waterSports) Demo.waiverBody
    else Demo.waiverBody
        .replace("Surfing and other water sports carry", "${item.sport} carries")
        .replace("Waves, currents, rocks, marine life, other\nwater users and the equipment itself", "Terrain, weather, other\npeople and the equipment itself")
        .replace("before\neach session", "before\neach use")

/**
 * The waiver is the unusual screen, and the interaction is the point: the
 * accept button stays disabled until the text has actually been scrolled to the
 * end. Consent you didn't read isn't consent.
 *
 * S-10 · Read the draft; Update it with the signed name and the time of signing.
 * While the button is disabled it now says why, and how much is left to read
 * (FR-14) — two of four Milestone 01 participants took the gate for a bug.
 */
@Composable
fun S10Waiver(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val draft = rememberDraft(vm)
    val item = draft?.item ?: Demo.torq
    val signedAlready = draft?.waiverName != null
    val checksText = waiverChecksFor(item)
    val checks = rememberSavedStateList { checksText.map { signedAlready } }
    var signature by rememberSaveable { mutableStateOf(draft?.waiverName ?: "") }
    val waiverScroll = rememberSavedScrollState()

    val scrolledToEnd by remember {
        derivedStateOf { waiverScroll.maxValue > 0 && waiverScroll.value >= waiverScroll.maxValue - 8 }
    }
    val readPercent by remember {
        derivedStateOf { if (waiverScroll.maxValue <= 0) 0 else (waiverScroll.value * 100 / waiverScroll.maxValue) }
    }
    val read = scrolledToEnd || signedAlready
    val canAccept = draft != null && checks.all { it } && read && signature.trim().length >= 3
    val missing = buildList {
        if (!read) add("read to the end ($readPercent%)")
        val left = checks.count { !it }
        if (left > 0) add("tick $left more")
        if (signature.trim().length < 3) add("type your name")
    }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                Column(Modifier.fillMaxWidth()) {
                    if (missing.isNotEmpty()) {
                        Text(
                            "To continue: " + missing.joinToString(" · "),
                            style = GearType.bodyS,
                            color = GearText.secondary,
                        )
                        VGap(Space.s)
                    }
                    GearButton(
                        label = "I accept and continue",
                        onClick = { vm.signWaiver(signature) { nav.to("s11") } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canAccept,
                    )
                }
            }
        },
    ) {
        ProgressStepper(step = 3, total = 5)
        VGap(Space.l)
        Text("Before you ride", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.m)
        Banner(
            text = "${item.sport} carries risk of injury. Read and accept before booking.",
            kind = BannerKind.SAFETY,
        )

        VGap(Space.l)
        Eyebrow("Requirements")
        VGap(Space.s)
        checksText.forEachIndexed { i, text ->
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
                Text(waiverBodyFor(item), style = GearType.bodyS, color = GearText.body)
            }
            // Fade at the bottom, so it is obvious there is more to read.
            if (!read) {
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
                if (read) Lucide.CircleCheck else Lucide.Circle,
                null,
                size = IconSize.s,
                tint = if (read) GearColor.Success500 else GearColor.Chalk400,
            )
            Text(
                if (read) "Read to the end" else "Read $readPercent% so far",
                style = GearType.bodyS,
                color = if (read) GearColor.Success500 else GearText.tertiary,
            )
        }

        VGap(Space.l)
        GearTextField(
            value = signature,
            label = "Type your full name",
            placeholder = "Your full name",
            onValueChange = { signature = it },
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        )
        if (signature.isNotBlank()) {
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
                Text(
                    if (draft?.waiverAt != null && draft.waiverName == signature.trim()) "Signed ${stamp(draft.waiverAt)}"
                    else "Signs ${stamp(DemoClock.now())} when you accept",
                    style = GearType.dataS,
                    color = GearText.tertiary,
                )
            }
        }
        if (signedAlready) {
            VGap(Space.s)
            GearLink("Withdraw my signature", { vm.clearWaiver(); signature = "" }, color = GearText.tertiary)
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

private val paymentMethods = listOf(
    Triple(Lucide.CreditCard, "Visa •••• 4471", "Expires 09/28"),
    Triple(Lucide.Smartphone, "eZ Cash", "077 ••• ••••"),
    Triple(Lucide.Banknote, "Cash on pickup", "Deposit still held"),
)

/**
 * S-11 · Read the draft and its bill; Update the payment method and the extras;
 * Create the booking on "Confirm and pay", or Delete the draft.
 *
 * Every line on the bill explains itself (FR-20). The disclosure used to say the
 * service fee was 9% capped at LKR 1,200 while the arithmetic charged 10% with no
 * cap; the sentence is now generated from the same rule that does the sum (D-01).
 */
@Composable
fun S11ReviewPay(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val draft = rememberDraft(vm)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var discarding by rememberSaveable { mutableStateOf(false) }

    if (draft == null) {
        GearScreen(appBar = { AppBar(onBack = { nav.back() }) }) {}
        return
    }
    val quote = draft.quote
    val lines = quote.lines()
    val signed = draft.waiverName != null
    val item = draft.item

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter(
                priceValue = lkr(quote.total),
                priceCaption = "${draft.days} ${if (draft.days == 1) "day" else "days"}, all in",
            ) {
                if (signed) {
                    GearButton(
                        if (item.instantBook) "Confirm and pay" else "Send request",
                        { vm.confirm { nav.to("s12") } },
                        size = ButtonSize.M,
                    )
                } else {
                    GearButton("Sign the waiver first", { nav.to("s10") }, size = ButtonSize.M, type = ButtonType.SECONDARY)
                }
            }
        },
    ) {
        ProgressStepper(step = 4, total = 5)
        VGap(Space.l)

        Row(verticalAlignment = Alignment.CenterVertically) {
            GearImage(item.hero, item.title, Modifier.size(64.dp), GearShape.sm)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(item.shortTitle, style = GearType.headingS, color = GearText.primary)
                VGap(4.dp)
                GearTag(item.tagLine)
                VGap(5.dp)
                Text(draft.datesLong, style = GearType.dataM, color = GearText.secondary)
            }
        }

        VGap(Space.l)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                lines.forEachIndexed { i, line ->
                    PriceRow(line)
                    if (i != lines.lastIndex) VGap(Space.s)
                }
            }
        }
        item.extras.forEach { extra ->
            VGap(Space.s)
            ToggleRow(
                "${extra.name} · ${lkr(extra.perDay)} a day",
                draft.includeExtras,
                { vm.setExtras(it) },
                helper = "Offered by ${item.owner.name.substringBefore(' ')}. Take it off if you have your own.",
            )
        }

        VGap(Space.s)
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("What is each line?", style = GearType.labelM, color = GearColor.Marine500)
            Spacer(Modifier.weight(1f))
            GearIcon(
                if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                null, size = IconSize.s, tint = GearColor.Marine500,
            )
        }
        if (expanded) {
            quote.explanations().forEach { (label, sentence) ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(label, style = GearType.labelM, color = GearText.primary)
                    Text(sentence, style = GearType.bodyS, color = GearText.secondary)
                }
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Banner(
            text = if (quote.depositHold > 0) {
                "${lkr(quote.depositHold)} will be held on your card, not charged. It's released within 24 h of return."
            } else {
                "No deposit with ${draft.protection.label} protection. Nothing is held on your card."
            },
            kind = BannerKind.SAFETY,
        )

        VGap(Space.l)
        Eyebrow("Payment method")
        VGap(Space.s)
        paymentMethods.forEachIndexed { i, (icon, title, sub) ->
            PaymentRow(icon, title, sub, draft.paymentMethod == i) { vm.setPaymentMethod(i) }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("Cancellation")
        VGap(Space.xs)
        Text(MoneyRepository.CANCELLATION_RULE, style = GearType.bodyS, color = GearText.secondary)

        VGap(Space.m)
        Text(
            "By confirming you agree to the GearLoop rental terms, the waiver you signed, " +
                "and ${item.owner.name.substringBefore(' ')}'s house rules." +
                if (!item.instantBook) " Nothing is charged until the owner accepts." else "",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )
        VGap(Space.m)
        GearLink("Discard this booking", { discarding = true }, color = GearText.tertiary, icon = Lucide.Trash2)
        VGap(Space.section)
    }

    if (discarding) {
        GearDialog(
            title = "Discard this booking?",
            onDismiss = { discarding = false },
            confirmLabel = "Keep it",
            onConfirm = { discarding = false },
            destructiveLabel = "Discard",
            onDestructive = { discarding = false; vm.discardDraft { nav.back() } },
        ) {
            Text(
                "Your dates, plan and signature for ${item.shortTitle} will be cleared. Nothing has been charged.",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.sm, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearRadio(selected)
        Spacer(Modifier.width(Space.sm))
        GearIcon(icon, null, size = IconSize.m, tint = GearText.primary)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.primary)
            Text(subtitle, style = GearType.bodyS, color = GearText.tertiary)
        }
    }
}

/* ------------------------------------------------------------------ S-12 */

/**
 * S-12 · Read the booking just made; Update it — written to the phone's calendar,
 * or cancelled under the cancellation rule.
 */
@Composable
fun S12Confirmed(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val current by vm.current.collectAsStateWithLifecycle()
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    var cancelling by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    // Opened directly, it shows the renter's next booking.
    val booking = current ?: upcoming.firstOrNull()

    if (booking == null) {
        GearScreen(appBar = { AppBar(title = "Bookings", onBack = { nav.back() }) }) {
            EmptyState(
                icon = Lucide.CalendarCheck,
                headline = "No booking yet",
                body = "Book something and its confirmation will be here.",
                actionLabel = "Find gear",
                onAction = { nav.tab("s04") },
            )
        }
        return
    }
    val item = booking.item
    val owner = booking.owner.name.substringBefore(' ')
    val requested = booking.status == BookingStatus.REQUESTED
    val cancelled = booking.status == BookingStatus.CANCELLED || booking.status == BookingStatus.DECLINED
    // The screen outlives the moment it was designed for: open it again after
    // the return and the booking has moved on. Anything that wasn't requested or
    // cancelled used to fall through to "Request sent" under an hourglass (D-22).
    val pickedUp = booking.status in setOf(BookingStatus.ACTIVE, BookingStatus.RETURNED, BookingStatus.COMPLETED)
    val finished = booking.status == BookingStatus.RETURNED || booking.status == BookingStatus.COMPLETED

    val timeline = when {
        cancelled -> listOf(TimelineNode(stamp(DemoClock.now()), "Booking ${booking.status.label.lowercase()}", TimelineNode.State.DONE))
        requested -> listOf(
            TimelineNode("Now", "$owner has your request", TimelineNode.State.DONE),
            TimelineNode(booking.respondBy?.let(::dayTime) ?: "Within 24 h", "$owner accepts or declines", TimelineNode.State.CURRENT),
            TimelineNode(dayTime(booking.pickupAt), "Meet and check the gear together", TimelineNode.State.FUTURE),
            TimelineNode(dayTime(booking.returnAt), "Return by this time", TimelineNode.State.FUTURE),
        )
        else -> listOfNotNull(
            TimelineNode(stamp(booking.createdAt), "$owner has been notified", TimelineNode.State.DONE),
            TimelineNode(if (pickedUp) stamp(booking.createdAt) else "Now", "Exact address unlocked in your messages", TimelineNode.State.DONE),
            TimelineNode(
                dayTime(booking.pickupAt), "Meet and check the gear together",
                if (pickedUp) TimelineNode.State.DONE else TimelineNode.State.CURRENT,
            ),
            TimelineNode(
                dayTime(booking.returnedShownAt ?: booking.returnAt),
                if (finished) "Returned" else "Return by this time",
                when {
                    finished -> TimelineNode.State.DONE
                    pickedUp -> TimelineNode.State.CURRENT
                    else -> TimelineNode.State.FUTURE
                },
            ),
            if (finished) {
                TimelineNode(
                    "Within 24 h",
                    if (booking.status == BookingStatus.COMPLETED) "Settled against the pickup record" else "$owner checks it against the pickup record",
                    if (booking.status == BookingStatus.COMPLETED) TimelineNode.State.DONE else TimelineNode.State.CURRENT,
                )
            } else null,
        )
    }

    val addToCalendar: () -> Unit = {
        val zone = ZoneId.systemDefault()
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, booking.pickupAt.atZone(zone).toInstant().toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, booking.returnAt.atZone(zone).toInstant().toEpochMilli())
            .putExtra(CalendarContract.Events.TITLE, "${item.shortTitle} · ${booking.ref}")
            .putExtra(CalendarContract.Events.EVENT_LOCATION, MoneyRepository.pickupAddress(booking.owner.handle))
        try {
            context.startActivity(intent)
            vm.markAddedToCalendar(booking.ref)
        } catch (_: ActivityNotFoundException) {
            context.container.toaster.show("No calendar app on this phone")
        }
    }

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
                GearIcon(
                    when {
                        cancelled -> Lucide.X
                        requested -> Lucide.Hourglass
                        else -> Lucide.Check
                    },
                    null, size = 44.dp, tint = GearColor.HiVis500, strokeWidth = 2.5f,
                )
            }
            VGap(Space.l)
            Text(
                when {
                    cancelled -> "Booking ${booking.status.label.lowercase()}"
                    requested -> "Request sent"
                    booking.status == BookingStatus.COMPLETED -> "Rental complete"
                    booking.status == BookingStatus.RETURNED -> "Returned"
                    else -> "You're booked"
                },
                style = GearType.displayXl,
                color = GearText.onDark,
            )
            VGap(Space.s)
            Text(booking.ref, style = GearType.dataL, color = GearColor.HiVis500)
        }

        // The card overlaps the dark panel above it by 40dp.
        Column(
            Modifier
                .offset(y = (-40).dp)
                .padding(horizontal = Space.screen)
        ) {
            GearCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                    GearImage(item.hero, item.title, Modifier.size(64.dp), GearShape.sm)
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Text(item.shortTitle, style = GearType.headingS, color = GearText.primary)
                        VGap(4.dp)
                        Text(booking.datesLong, style = GearType.dataM, color = GearText.secondary)
                        VGap(5.dp)
                        GearTag(item.tagLine)
                    }
                }
            }

            VGap(Space.l)
            Eyebrow("What happens next")
            VGap(Space.sm)
            Timeline(timeline)

            if (!cancelled) {
                VGap(Space.l)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    GearButton(
                        if (booking.calendarAdded) "In your calendar" else "Add to calendar", addToCalendar, Modifier.weight(1f),
                        type = ButtonType.SECONDARY, size = ButtonSize.M,
                        icon = if (booking.calendarAdded) Lucide.CalendarCheck else Lucide.Calendar,
                    )
                    GearButton(
                        "Message", { vm.openThread(booking.owner.handle); nav.to("s33") }, Modifier.weight(1f),
                        type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.MessageSquare,
                    )
                }
                VGap(Space.sm)
                // To wherever the booking is now: the pickup hub before it starts,
                // the live rental while it's out, the return check once it's back.
                GearButton(
                    when {
                        finished -> "See the return check"
                        pickedUp -> "Open the rental"
                        else -> "View booking"
                    },
                    {
                        vm.selectBooking(booking.ref)
                        nav.to(
                            when {
                                finished -> "s18"
                                pickedUp -> "s17"
                                else -> "s13"
                            },
                        )
                    },
                    Modifier.fillMaxWidth(),
                    type = ButtonType.GHOST, size = ButtonSize.M,
                )
                if (booking.status == BookingStatus.CONFIRMED || booking.status == BookingStatus.REQUESTED) {
                    VGap(Space.xs)
                    GearButton(
                        if (booking.status == BookingStatus.REQUESTED) "Withdraw request" else "Cancel booking",
                        { cancelling = true }, Modifier.fillMaxWidth(),
                        type = ButtonType.GHOST, size = ButtonSize.M,
                    )
                }
            } else {
                VGap(Space.l)
                GearButton("Find other gear", { nav.tab("s04") }, Modifier.fillMaxWidth(), size = ButtonSize.M)
            }
            VGap(Space.section)
        }
    }

    if (cancelling) {
        val refund = vm.refundFor(booking)
        GearDialog(
            title = if (booking.status == BookingStatus.REQUESTED) "Withdraw this request?" else "Cancel ${booking.ref}?",
            onDismiss = { cancelling = false },
            confirmLabel = "Keep it",
            onConfirm = { cancelling = false },
            destructiveLabel = "Cancel it",
            onDestructive = { cancelling = false; vm.cancel(booking.ref) },
        ) {
            Text(
                "You'll get ${lkr(refund)} of ${lkr(booking.total)} back. ${MoneyRepository.CANCELLATION_RULE}",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
    }
}
