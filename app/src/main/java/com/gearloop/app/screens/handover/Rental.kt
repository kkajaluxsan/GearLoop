package com.gearloop.app.screens.handover

import android.content.Intent
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.gearloop.app.container
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.Demo
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Phase
import com.gearloop.app.data.PriceLine
import com.gearloop.app.data.Protection
import com.gearloop.app.data.countdown
import com.gearloop.app.data.dayTime
import com.gearloop.app.data.roughDuration
import com.gearloop.app.data.lkr
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.repo.MoneyRepository
import com.gearloop.app.data.metaStamp
import com.gearloop.app.data.repo.AnglePair
import com.gearloop.app.data.repo.Booking
import com.gearloop.app.data.repo.Mark
import com.gearloop.app.data.repo.EvidenceRepository
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.data.stamp
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.icons.LucideIcon
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.vm.EvidenceTrustViewModel
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.temporal.ChronoUnit

@Composable
private fun NoRental(nav: Nav, title: String, body: String) {
    GearScreen(appBar = { AppBar(title = title, onBack = { nav.back() }) }) {
        EmptyState(
            icon = Lucide.Package,
            headline = "Nothing here yet",
            body = body,
            actionLabel = "See my rentals",
            onAction = { nav.tab("s31") },
        )
    }
}

/* ------------------------------------------------------------------ S-16 */

/**
 * Both of you agree this is the condition: every frame and every mark on the
 * pickup record, then a signature from each of you, drawn on this screen. It
 * cannot be signed until all seven angles have passed; once both have signed,
 * the rental starts.
 */
@Composable
fun S16SignRelease(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val allFrames by vm.frames.collectAsStateWithLifecycle()
    val allMarks by vm.marks.collectAsStateWithLifecycle()
    val angles by vm.angles.collectAsStateWithLifecycle()
    var signing by rememberSaveable { mutableStateOf<String?>(null) }
    val booking = bookingState ?: return NoRental(nav, "Sign and release", "Sign-off happens after the condition check at pickup.")
    val frames = allFrames[Phase.PICKUP].orEmpty()
    val marks = allMarks.filter { it.phase == Phase.PICKUP }
    val complete = angles.all { frames[it]?.ok == true }
    val returned = booking.status == BookingStatus.RETURNED || booking.status == BookingStatus.COMPLETED
    val next = when {
        booking.renterSignedAt == null -> EvidenceRepository.Signer.RENTER
        booking.ownerSignedAt == null -> EvidenceRepository.Signer.OWNER
        else -> null
    }
    val nextPerson = if (next == EvidenceRepository.Signer.OWNER) booking.owner else booking.renter

    // When the second signature lands, the rental has started: go to it. Only
    // when it lands while this screen is open (D-24).
    val signedOnEntry = rememberSaveable { booking.fullySigned }
    LaunchedEffect(booking.fullySigned) {
        if (!signedOnEntry && booking.fullySigned && booking.status == BookingStatus.ACTIVE) nav.to("s17")
    }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                when {
                    booking.fullySigned -> GearButton(
                        if (returned) "See the return check" else "Open the rental",
                        { nav.to(if (returned) "s18" else "s17") },
                        Modifier.fillMaxWidth(),
                    )
                    !complete -> GearButton("Finish the condition check first", { nav.to("s15") }, Modifier.fillMaxWidth(), type = ButtonType.SECONDARY)
                    next != null -> GearButton(
                        "Sign as ${nextPerson.name.substringBefore(' ')}",
                        { signing = next.name },
                        Modifier.fillMaxWidth(),
                        icon = Lucide.SquarePen,
                    )
                }
            }
        },
    ) {
        VGap(Space.s)
        Text("Both of you agree this is the condition", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.m)

        val photos = angles.mapNotNull { frames[it] }
        if (photos.isEmpty()) {
            Text("No frames recorded yet.", style = GearType.bodyS, color = GearText.tertiary)
        }
        photos.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                row.forEach {
                    Column(Modifier.weight(1f)) {
                        GearImage(it.photo, "${it.angle} at pickup", Modifier.fillMaxWidth().height(104.dp), GearShape.sm)
                        VGap(3.dp)
                        Text(it.angle.uppercase(), style = GearType.labelXs, color = GearText.tertiary)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("What's going with it")
        VGap(Space.s)
        booking.item.included.forEach {
            IconRow(Lucide.Check, it, Modifier.padding(vertical = 4.dp), tint = GearColor.Success500)
        }
        if (booking.includeExtras) {
            booking.item.extras.forEach {
                IconRow(Lucide.Check, it.name, Modifier.padding(vertical = 4.dp), tint = GearColor.Success500)
            }
        }
        booking.item.excluded.forEach {
            IconRow(Lucide.X, it, Modifier.padding(vertical = 4.dp), tint = GearColor.Chalk400, textColor = GearText.tertiary, strikethrough = true)
        }

        VGap(Space.m)
        Eyebrow("Existing damage recorded")
        VGap(Space.s)
        if (marks.isEmpty()) {
            Text("None recorded at pickup.", style = GearType.bodyS, color = GearText.tertiary)
            VGap(Space.s)
            GearLink("Mark damage before you sign", { nav.to("s15b") }, icon = Lucide.Pin)
        }
        marks.forEach { m ->
            DamageRow(m, "Recorded at pickup", booking.item.hero)
            VGap(Space.s)
        }

        VGap(Space.l)
        SignatureBlock(booking.renter, "Renter", booking.renterSignedAt, booking.renterSignature)
        VGap(Space.sm)
        SignatureBlock(booking.owner, "Owner", booking.ownerSignedAt, booking.ownerSignature)

        VGap(Space.m)
        Banner(
            text = "This record is what settles any claim later. It can't be edited after both signatures.",
            kind = BannerKind.SAFETY,
        )
        VGap(Space.section)
    }

    signing?.let { which ->
        val signer = EvidenceRepository.Signer.valueOf(which)
        val person = if (signer == EvidenceRepository.Signer.OWNER) booking.owner else booking.renter
        androidx.compose.ui.window.Dialog(onDismissRequest = { signing = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(GearShape.md)
                    .background(GearSurface.card)
                    .padding(Space.cardPadding),
            ) {
                Text("${person.name}, sign the record", style = GearType.headingM, color = GearText.primary)
                VGap(Space.xs)
                Text(
                    if (signer == EvidenceRepository.Signer.OWNER) "Hand the phone to ${person.name.substringBefore(' ')}. Signing agrees the condition photos and marks above."
                    else "Signing agrees the condition photos and marks above.",
                    style = GearType.bodyS,
                    color = GearText.secondary,
                )
                VGap(Space.sm)
                SignaturePad(
                    signer = person.name.substringBefore(' '),
                    onSigned = { bitmap -> vm.sign(booking.ref, signer, bitmap, person.name.substringBefore(' ')); signing = null },
                    onCancel = { signing = null },
                )
            }
        }
    }
}

/** A recorded mark, as a list row. The severity is stated once, in the badge. */
@Composable
private fun DamageRow(mark: Mark, recorded: String, fallback: String) {
    GearCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            GearImage(mark.photo ?: fallback, "${mark.area} photo", Modifier.size(52.dp), GearShape.sm)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text("${mark.area} · ${mark.summary.lowercase()}", style = GearType.labelL, color = GearText.primary)
                Text("$recorded · ${mark.note}", style = GearType.bodyS, color = GearText.tertiary, maxLines = 2)
            }
            StatusBadge(mark.severity.label, GearColor.Warning700)
        }
    }
}

@Composable
private fun SignatureBlock(
    person: com.gearloop.app.data.Person,
    role: String,
    signedAt: java.time.LocalDateTime?,
    signature: String?,
) {
    val signed = signedAt != null
    GearCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(person, size = 32.dp)
                Spacer(Modifier.width(Space.s))
                Column(Modifier.weight(1f)) {
                    Text(person.name, style = GearType.labelL, color = GearText.primary)
                    Text(role.uppercase(), style = GearType.labelXs, color = GearText.tertiary)
                }
                if (signed) {
                    GearIcon(Lucide.CircleCheck, null, size = IconSize.m, tint = GearColor.Success500)
                } else {
                    Text("NOT SIGNED", style = GearType.labelXs, color = GearColor.Warning700)
                }
            }
            VGap(Space.sm)
            when {
                signature != null -> GearImage(signature, "${person.name}'s signature", Modifier.fillMaxWidth().height(72.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                signed -> Text(
                    person.name,
                    style = GearType.headingL.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = GearColor.Pine900,
                )
                else -> Spacer(Modifier.height(28.dp))
            }
            GearDivider(color = GearColor.Chalk300)
            VGap(5.dp)
            Text(
                if (signedAt != null) "SIGNED ${stamp(signedAt).uppercase()}" else "NOT SIGNED YET",
                style = GearType.dataS,
                color = GearText.tertiary,
            )
        }
    }
}

/* ------------------------------------------------------------------ S-17 */

private val returnChecklist = listOf(
    "Rinse it with fresh water",
    "Everything that came with it is in the bag",
    "Straps and leashes attached",
    "Re-photograph the same 7 angles",
)

/**
 * S-17 · Read the live rental; Update it — extend by a day (only if the next
 * day and its buffer are free on this unit, and charged there and then), and
 * tick the return checklist.
 */
@Composable
fun S17ActiveRental(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    // Open from the start: it is what to do before handing the gear back, and
    // folded away it was a heading nobody thought to tap.
    var checklistOpen by rememberSaveable { mutableStateOf(true) }
    // The offer of one more day, priced, while the member decides.
    var extendTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var extendCharge by rememberSaveable { mutableStateOf(0) }
    var reporting by rememberSaveable { mutableStateOf(false) }
    var problem by rememberSaveable { mutableStateOf(ISSUES.first()) }
    var problemNote by rememberSaveable { mutableStateOf("") }
    val booking = bookingState ?: return NoRental(nav, "Rental", "A rental appears here once you and the owner have signed for it.")
    val item = booking.item
    val left = ChronoUnit.MILLIS.between(AppClock.now(), booking.returnAt)
    val owner = booking.owner.name.substringBefore(' ')
    // Opened again after the return, this kept counting down to a return that
    // had already happened, beside a Start return button that could not be used (D-23).
    val returned = booking.status == BookingStatus.RETURNED || booking.status == BookingStatus.COMPLETED

    GearScreen(
        tabBar = { MainTabBar(nav, MainTab.RENTALS) },
        contentPadding = 0.dp,
        // The hero runs pine up behind the status bar, so the clock and the
        // signal icons have to go light with it.
        darkStatusBar = true,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(GearColor.Pine800)
                .statusBarsPadding()
                .padding(horizontal = Space.screen)
                .padding(top = Space.l, bottom = Space.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GearImage(item.hero, item.title, Modifier.size(72.dp), GearShape.sm)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(item.shortTitle, style = GearType.headingS, color = GearText.onDark)
                    VGap(4.dp)
                    Text("${booking.ref} · ${booking.status.label.uppercase()}", style = GearType.dataM, color = GearColor.Chalk300)
                }
            }
            VGap(Space.m)
            if (returned) {
                Text("Returned", style = GearType.headingXl, color = GearText.onDark)
            } else {
                Text(countdown(left), style = GearType.dataXl, color = GearText.onDark)
            }
            VGap(4.dp)
            Text(
                when {
                    returned -> "BACK WITH ${owner.uppercase()} · ${dayTime(booking.returnedShownAt ?: booking.returnAt).uppercase()}"
                    booking.status == BookingStatus.CONFIRMED -> "UNTIL RETURN · NOT PICKED UP YET"
                    left < 0 -> "OVERDUE"
                    else -> "UNTIL RETURN"
                },
                style = GearType.labelXs,
                color = GearColor.HiVis500,
            )
        }

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.m)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GearIcon(Lucide.Clock, null, size = IconSize.s, tint = GearColor.Chalk500)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${dayTime(booking.returnAt)} · ${booking.owner.area.substringBefore(',')}",
                    style = GearType.dataM,
                    color = GearText.primary,
                )
            }

            VGap(Space.m)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                QuickAction(Lucide.CalendarClock, "Extend a day", Modifier.weight(1f)) {
                    vm.extendQuote(booking.ref) { quote -> extendCharge = quote.charge; extendTo = quote.newEnd.toEpochDay() }
                }
                QuickAction(Lucide.TriangleAlert, "Report issue", Modifier.weight(1f)) { reporting = true }
                QuickAction(Lucide.MessageSquare, "Message", Modifier.weight(1f)) {
                    vm.openThread(booking.owner.handle); nav.to("s33")
                }
            }

            VGap(Space.l)
            GearCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.cardPadding)) {
                    Eyebrow("Looking after it")
                    VGap(Space.s)
                    careNotesFor(item.sport).forEach {
                        IconRow(
                            Lucide.Droplet, it,
                            Modifier.padding(vertical = 4.dp),
                            tint = GearColor.Marine500,
                            style = GearType.bodyS,
                        )
                    }
                    VGap(Space.s)
                    Text("From $owner", style = GearType.bodyS, color = GearText.tertiary)
                }
            }

            VGap(Space.sm)
            GearCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { checklistOpen = !checklistOpen }
                            .padding(Space.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Return checklist",
                            style = GearType.headingS,
                            color = GearText.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${returnChecklist.indices.count { it in booking.returnChecklist }} / ${returnChecklist.size}",
                            style = GearType.dataM,
                            color = GearText.tertiary,
                        )
                        Spacer(Modifier.width(Space.s))
                        GearIcon(
                            if (checklistOpen) Lucide.ChevronUp else Lucide.ChevronDown,
                            null, size = IconSize.m, tint = GearColor.Chalk500,
                        )
                    }
                    if (checklistOpen) {
                        Column(Modifier.padding(start = Space.sm, end = Space.cardPadding, bottom = Space.cardPadding)) {
                            returnChecklist.forEachIndexed { i, text ->
                                CheckRow(text, i in booking.returnChecklist, { vm.toggleReturnChecklist(booking.ref, i) })
                            }
                        }
                    }
                }
            }

            VGap(Space.m)
            GearTag(item.tagLine, size = TagSize.FULL)

            VGap(Space.m)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(GearShape.sm)
                    .background(GearColor.HiVis100)
                    .padding(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearIcon(Lucide.ShieldCheck, null, size = IconSize.m, tint = GearColor.Pine900)
                Spacer(Modifier.width(Space.s))
                Text(
                    if (booking.depositHold > 0) "${lkr(booking.depositHold)} held on your card · ${booking.protection.label} protection"
                    else "No deposit held · ${booking.protection.label} protection active",
                    style = GearType.bodyS,
                    color = GearColor.Pine900,
                )
            }

            VGap(Space.m)
            if (returned) {
                GearButton("See the return check", { nav.to("s18") }, Modifier.fillMaxWidth())
            } else {
                GearButton(
                    "Start return",
                    { vm.startCapture(Phase.RETURN); nav.to("s15") },
                    Modifier.fillMaxWidth(),
                    enabled = booking.status == BookingStatus.ACTIVE,
                )
            }
            VGap(Space.section)
        }
    }

    if (reporting) {
        GearDialog(
            title = "What's wrong?",
            onDismiss = { reporting = false },
            confirmLabel = "Tell $owner",
            onConfirm = {
                reporting = false
                vm.reportIssue(booking.ref, problem, problemNote) { problemNote = ""; nav.to("s33") }
            },
        ) {
            Text(
                "$owner gets a message. If it's damage, photograph it now.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.sm)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                ISSUES.forEach { option ->
                    Chip(option, selected = problem == option, onClick = { problem = option })
                }
            }
            VGap(Space.sm)
            GearTextField(
                problemNote,
                label = "Details (optional)",
                placeholder = "What happened, and when",
                onValueChange = { problemNote = it.take(300) },
            )
        }
    }

    extendTo?.let { day ->
        val until = java.time.LocalDate.ofEpochDay(day)
        val how = when (booking.paymentMethod) {
            MoneyRepository.PAY_WALLET -> "taken from your wallet"
            MoneyRepository.PAY_CASH -> "paid in cash when you hand it back"
            else -> "charged to your card"
        }
        GearDialog(
            title = "Keep it until ${dayMonth(until)}?",
            onDismiss = { extendTo = null },
            confirmLabel = "Extend",
            onConfirm = { extendTo = null; vm.extend(booking.ref) },
        ) {
            Text(
                if (extendCharge > 0) "One more day costs ${lkr(extendCharge)}, $how."
                else "One more day costs nothing extra.",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
        }
    }
}

@Composable
private fun QuickAction(
    icon: LucideIcon,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(88.dp)
            .clip(GearShape.md)
            .border(Border.hair, GearSurface.border, GearShape.md)
            .background(GearSurface.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Space.s),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GearIcon(icon, null, size = IconSize.l, tint = GearText.primary)
        VGap(6.dp)
        Text(label, style = GearType.labelM, color = GearText.primary, textAlign = TextAlign.Center)
    }
}

/* ------------------------------------------------------------------ S-18 */

/**
 * S-18 · Read the pickup and return frames, paired by angle, and the settlement.
 * A change is a mark recorded on the return side, so "changed" here is a fact
 * someone recorded rather than a guess; tapping it opens S-18b to decide it.
 */
@Composable
fun S18ReturnSettlement(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val discovery: FitDiscoveryViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val pairs by vm.pairs.collectAsStateWithLifecycle()
    val claims by vm.claims.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val booking = bookingState ?: return NoRental(nav, "Return check", "The return check opens once the return frames are in.")
    val owner = booking.owner.name.substringBefore(' ')
    // The owner reads this screen too, from Requests, and is the one who acts on it.
    val iAmOwner = booking.owner.handle == vm.me
    val changes = pairs.mapNotNull { it.change }
    // A difference the owner took further: the rental is closed, the claim isn't.
    val claim = claims.firstOrNull { it.bookingRef == booking.ref }
    val settled = booking.status == BookingStatus.COMPLETED
    val returned = booking.returnedAt
    val early = returned?.let { ChronoUnit.MILLIS.between(it, booking.returnAt) }
    val reviewEnds = returned?.plusHours(24)

    GearScreen(
        appBar = { AppBar(title = "Return check", onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        Eyebrow("Pickup vs return")
        VGap(Space.sm)

        Row(Modifier.fillMaxWidth()) {
            Text("AT PICKUP", style = GearType.labelXs, color = GearText.tertiary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(COMPARISON_GUTTER))
            Text("ON RETURN", style = GearType.labelXs, color = GearText.tertiary, modifier = Modifier.weight(1f))
        }
        VGap(6.dp)

        pairs.forEach { pair ->
            ComparisonRow(pair) { if (pair.change != null) nav.to("s18b") }
            VGap(Space.s)
        }

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(Space.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                if (settled && claim != null) {
                    GearIcon(Lucide.Scale, null, size = 52.dp, tint = GearColor.Warning700)
                } else if (settled) {
                    GearIcon(Lucide.CircleCheck, null, size = 52.dp, tint = GearColor.Success500)
                } else {
                    // The ring fills as the owner's 24 hours run out; the figure is what's left.
                    val left = reviewEnds?.let { ChronoUnit.HOURS.between(AppClock.now(), it).coerceIn(0, 24) } ?: 24
                    ProgressRing(progress = ((24 - left) / 24f).coerceIn(0.05f, 1f), size = 52.dp, strokeWidth = 5.dp, color = GearColor.Warning500) {
                        Text("${left}H", style = GearType.dataS, color = GearText.primary)
                    }
                }
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    val plural = if (changes.size > 1) "s" else ""
                    Text(
                        when {
                            settled && claim != null -> if (iAmOwner) "You raised claim ${claim.ref}" else "$owner raised claim ${claim.ref}"
                            settled -> "Settled"
                            changes.isNotEmpty() && iAmOwner -> "${changes.size} change$plural for you to check"
                            changes.isNotEmpty() -> "$owner is checking ${changes.size} change$plural"
                            returned == null -> "Waiting for the return frames"
                            iAmOwner -> "Nothing new was marked"
                            else -> "$owner is checking"
                        },
                        style = GearType.headingS,
                        color = GearText.primary,
                    )
                    VGap(2.dp)
                    Text(
                        when {
                            settled && claim != null -> "Rental closed. Claim ${claim.status.label.lowercase()}."
                            settled -> "Nothing more to do on this rental."
                            reviewEnds == null -> if (iAmOwner) "You have 24 hours from the return to check it." else "$owner has 24 hours from the return to check it."
                            iAmOwner -> "If you haven't answered by ${dayTime(reviewEnds)}, it settles as it is."
                            else -> "If $owner hasn't answered by ${dayTime(reviewEnds)}, it settles as it is."
                        },
                        style = GearType.bodyS,
                        color = GearText.secondary,
                    )
                }
            }
        }
        if (claim != null) {
            VGap(Space.s)
            GearButton(
                "See claim ${claim.ref}", { vm.selectClaim(claim.ref); nav.to("s34") }, Modifier.fillMaxWidth(),
                type = ButtonType.SECONDARY, size = ButtonSize.M,
            )
        } else if (iAmOwner && !settled && changes.isNotEmpty()) {
            // The decision is the owner's next step, so it is a button rather
            // than the outlined row above it that had to be found and tapped.
            VGap(Space.s)
            GearButton(
                if (changes.size > 1) "Decide on the changes" else "Decide on the change",
                { nav.to("s18b") }, Modifier.fillMaxWidth(), size = ButtonSize.M,
            )
        }

        VGap(Space.m)
        Eyebrow("Settlement")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                if (returned != null && early != null) {
                    SettlementRow(
                        if (early >= 0) Lucide.CircleCheck else Lucide.TriangleAlert,
                        if (early >= 0) "Returned on time" else "Returned late",
                        if (kotlin.math.abs(early) < 60_000) dayTime(returned)
                        else "${dayTime(returned)} · ${roughDuration(early)} ${if (early >= 0) "early" else "late"}",
                        if (early >= 0) GearColor.Success500 else GearColor.Warning700,
                    )
                    VGap(Space.sm)
                }
                SettlementRow(
                    if (changes.isEmpty()) Lucide.CircleCheck else Lucide.TriangleAlert,
                    if (changes.isEmpty()) "No new damage reported" else "${changes.size} difference flagged",
                    if (changes.isEmpty()) "Pickup marks stay on the pickup record"
                    else changes.joinToString { "${it.area} · ${it.summary.lowercase()}" },
                    if (changes.isEmpty()) GearColor.Success500 else GearColor.Warning700,
                )
                VGap(Space.sm)
                GearDivider()
                VGap(Space.sm)
                PriceRow(
                    PriceLine(
                        "Deposit",
                        when {
                            booking.depositHold == 0 -> "No deposit held"
                            settled -> "${lkr(booking.depositHold)} released"
                            else -> "${lkr(booking.depositHold)} held"
                        },
                        PriceLine.Kind.MUTED,
                    ),
                )
                VGap(Space.s)
                PriceRow(PriceLine("Total paid", lkr(booking.total), PriceLine.Kind.TOTAL))
            }
        }

        // The rating is made here rather than by leaving for S-42: a star-only
        // review, created, changed or removed on the screen that settles the
        // rental, so the screen writes as well as reads. Words can be added on
        // S-42, where it appears with the other reviews.
        VGap(Space.m)
        Eyebrow("Rate $owner")
        VGap(Space.xs)
        if (booking.renter.handle != vm.me) {
            Text("The renter rates the rental once it has settled.", style = GearType.bodyS, color = GearText.tertiary)
        } else if (settled) {
            val rating = booking.rating
            StarPicker(rating ?: 0, { n -> if (n != rating) vm.rateOwner(booking.ref, owner, n, rating) })
            Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                if (rating != null) {
                    GearLink("Remove my rating", { vm.rateOwner(booking.ref, owner, null, rating) }, color = GearText.tertiary, icon = Lucide.Trash2)
                }
                GearLink("See the reviews", { discovery.select(booking.item); nav.to("s42") })
            }
        } else {
            Text("You can rate $owner once the return check is settled.", style = GearType.bodyS, color = GearText.tertiary)
        }
        VGap(Space.s)
        GearButton(
            "Share receipt", {
                val receipt = buildString {
                    appendLine("GearLoop receipt · ${booking.ref}")
                    appendLine("${booking.item.title} · ${booking.datesLong}")
                    booking.quote.lines().forEach { appendLine("${it.label}: ${it.value}") }
                }
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, receipt), "Share receipt"))
            },
            Modifier.fillMaxWidth(),
            type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Download,
        )
        VGap(Space.section)
    }
}

/**
 * The column of angle labels between the two photo strips.
 *
 * Wide enough for the widest thing that goes in it, which is the CHANGED badge
 * and not the label. At 54dp the badge broke to "CHANG / ED".
 */
private val COMPARISON_GUTTER = 78.dp

@Composable
private fun ComparisonRow(pair: AnglePair, onClick: () -> Unit) {
    val changed = pair.change != null
    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.sm)
            .then(
                if (changed) Modifier.border(Border.heavy, GearColor.Warning500, GearShape.sm) else Modifier
            )
            .clickable(enabled = changed, role = Role.Button, onClick = onClick)
            .padding(if (changed) 5.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Frame(pair.pickup?.photo, "${pair.angle} at pickup", Modifier.weight(1f))
        Column(
            Modifier.width(COMPARISON_GUTTER).padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = pair.angle.uppercase(),
                style = GearType.labelXs,
                color = GearText.tertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (changed) {
                VGap(5.dp)
                Text(
                    text = "CHANGED",
                    style = GearType.labelXs,
                    color = GearText.onDark,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(GearShape.xs)
                        .background(GearColor.Warning500)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        Frame(pair.back?.photo, "${pair.angle} on return", Modifier.weight(1f))
    }
}

/** A frame, or the empty slot where one has not been shot yet. */
@Composable
private fun Frame(photo: String?, description: String, modifier: Modifier) {
    if (photo != null) {
        GearImage(photo, description, modifier.height(96.dp), GearShape.sm)
    } else {
        Box(
            modifier
                .height(96.dp)
                .clip(GearShape.sm)
                .border(Border.medium, GearColor.Chalk300, GearShape.sm)
                .background(GearColor.Chalk50),
            contentAlignment = Alignment.Center,
        ) {
            GearIcon(Lucide.Camera, "$description — not shot yet", size = IconSize.m, tint = GearColor.Chalk400)
        }
    }
}

@Composable
private fun SettlementRow(
    icon: LucideIcon,
    title: String,
    detail: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        GearIcon(icon, null, size = IconSize.m, tint = color)
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.primary)
            VGap(2.dp)
            Text(detail, style = GearType.dataM, color = GearText.secondary)
        }
    }
}

/* ----------------------------------------------------------------- S-18b */

/**
 * S-18b · Read one flagged difference — the same angle at pickup and on return,
 * and the mark that flagged it; Update it as not new ("It's the same"), or
 * Create a claim from it, which opens S-34 for the renter and joins S-40's queue.
 */
@Composable
fun S18bEvidenceDiff(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val pairs by vm.pairs.collectAsStateWithLifecycle()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val me by LocalContext.current.container.session.me.collectAsStateWithLifecycle()
    val pair = pairs.firstOrNull { it.change != null }
    val mark = pair?.change
    val booking = bookingState
    if (pair == null || mark == null || booking == null) {
        NoRental(nav, "Differences", "Nothing on the return differs from the pickup record.")
        return
    }
    val claims by vm.claims.collectAsStateWithLifecycle()
    val claim = claims.firstOrNull { it.bookingRef == booking.ref && it.area == mark.area }
    // Whether a difference is new is the owner's call, not the renter's, and
    // once it's a claim it is decided there.
    val deciding = me == booking.owner.handle && claim == null
    val owner = booking.owner.name.substringBefore(' ')
    GearScreen(
        appBar = { AppBar(title = "${mark.area} · pickup vs return", type = AppBarType.MODAL, onBack = { nav.back() }) },
        background = GearSurface.card,
        footer = {
            StickyFooter {
                if (claim != null) {
                    GearButton(
                        "See claim ${claim.ref} · ${claim.status.label.lowercase()}",
                        { vm.selectClaim(claim.ref); nav.to("s34") }, Modifier.fillMaxWidth(),
                        type = ButtonType.SECONDARY, size = ButtonSize.M,
                    )
                } else if (deciding) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        GearButton(
                            "It's the same", { vm.resolveChange(mark.id) { nav.back() } }, Modifier.weight(1f),
                            type = ButtonType.SECONDARY, size = ButtonSize.M,
                        )
                        GearButton("Raise a claim", { vm.raiseClaim(mark.id) { nav.to("s34") } }, Modifier.weight(1f), size = ButtonSize.M)
                    }
                } else {
                    Column {
                        Text(
                            "$owner decides whether this is new.",
                            style = GearType.bodyS,
                            color = GearText.secondary,
                        )
                        VGap(Space.s)
                        GearButton(
                            "Message $owner", { vm.openThread(booking.owner.handle); nav.to("s33") }, Modifier.fillMaxWidth(),
                            type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.MessageSquare,
                        )
                    }
                }
            }
        },
    ) {
        VGap(Space.m)
        Column {
            Eyebrow("At pickup · ${pair.pickup?.capturedAt?.let(::metaStamp) ?: "not shot"}")
            VGap(6.dp)
            Frame(pair.pickup?.photo, "${mark.area} at pickup", Modifier.fillMaxWidth().height(200.dp))
        }
        VGap(Space.m)
        Column {
            Eyebrow("On return · ${pair.back?.capturedAt?.let(::metaStamp) ?: "not shot"}")
            VGap(6.dp)
            Box {
                Frame(pair.back?.photo, "${mark.area} on return", Modifier.fillMaxWidth().height(200.dp))
                // The diff highlight — a ring over where the mark was pinned.
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(96.dp)
                        .clip(GearShape.full)
                        .border(3.dp, GearColor.Danger500, GearShape.full)
                        .background(GearColor.Danger500.copy(alpha = 0.12f))
                )
            }
        }
        VGap(Space.m)
        Banner(
            title = "Difference recorded on the ${mark.area.lowercase()}",
            text = (mark.note.trim().trimEnd('.').takeIf { it.isNotEmpty() }?.let { "$it. " } ?: "") +
                "It isn't in the pickup record.",
            kind = BannerKind.WARNING,
        )
        VGap(Space.m)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            MetaChip("SAME ANGLE")
            MetaChip(mark.severity.label.uppercase())
            MetaChip(pair.back?.place ?: pair.pickup?.place ?: "LOCATION OFF")
        }
        VGap(Space.s)
        Text(mark.severity.rubric, style = GearType.bodyS, color = GearText.secondary)
        VGap(Space.section)
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text,
        style = GearType.dataS,
        color = GearText.tertiary,
        modifier = Modifier
            .clip(GearShape.xs)
            .background(GearColor.Chalk100)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

/** The problems a renter reports most, in their words. */
private val ISSUES = listOf("It's damaged", "It isn't working", "Something's missing", "It's lost or stolen", "Something else")

/** How to look after this kind of gear while it is out. */
private fun careNotesFor(sport: String): List<String> = when (sport) {
    "Surfing" -> Demo.careNotes
    "SUP" -> listOf("Rinse with fresh water after each session", "Don't leave it in direct sun for hours", "Take the fin out before you carry it")
    "Kayaking" -> listOf("Rinse with fresh water after each session", "Don't leave it in direct sun for hours", "Store it on its side, never flat on the hull")
    "Swimming" -> listOf("Rinse it in cold fresh water", "Dry it inside out, in the shade", "Fold it over a hanger at the waist, never by the shoulders")
    "Cycling" -> listOf("Lock it whenever you leave it", "Keep it out of the rain overnight", "Check the brakes before each ride")
    "Camping", "Trekking" -> listOf("Dry it out before packing it away", "Keep it off rough ground and sharp rocks", "Shake out sand and grit")
    else -> listOf("Keep it clean and dry", "Store it out of direct sun", "Tell the owner straight away if anything breaks")
}
