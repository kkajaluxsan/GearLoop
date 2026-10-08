package com.gearloop.app.screens.handover

import android.content.Intent
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.Phase
import com.gearloop.app.data.PriceLine
import com.gearloop.app.data.Protection
import com.gearloop.app.data.countdown
import com.gearloop.app.data.dayTime
import com.gearloop.app.data.lkr
import com.gearloop.app.data.metaStamp
import com.gearloop.app.data.repo.AnglePair
import com.gearloop.app.data.repo.Booking
import com.gearloop.app.data.repo.Mark
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
import com.gearloop.app.ui.theme.GearSurface
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
 * S-16 · Read the pickup record — every frame and every mark; Update the
 * booking with the renter's signature, then the owner's, at which point the
 * rental starts. It cannot be signed until all seven angles have passed.
 */
@Composable
fun S16SignRelease(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val allFrames by vm.frames.collectAsStateWithLifecycle()
    val allMarks by vm.marks.collectAsStateWithLifecycle()
    val pulse = rememberInfiniteTransition(label = "await")
    val alpha by pulse.animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "a",
    )
    val booking = bookingState ?: return NoRental(nav, "Sign and release", "Sign-off happens after the condition check at pickup.")
    val frames = allFrames[Phase.PICKUP].orEmpty()
    val marks = allMarks.filter { it.phase == Phase.PICKUP }
    val complete = vm.angles.all { frames[it]?.ok == true }
    val signedByMe = booking.renterSignedAt != null
    val returned = booking.status == BookingStatus.RETURNED || booking.status == BookingStatus.COMPLETED

    // When the owner's signature lands, the rental has started: go to it. Only
    // when it lands while this screen is open — a record that was already signed
    // when the screen opened used to bounce straight on to S-17, so Back from
    // the rental could never reach the record it was signed on (D-24).
    val signedOnEntry = rememberSaveable { booking.ownerSignedAt != null }
    LaunchedEffect(booking.ownerSignedAt) {
        if (!signedOnEntry && booking.ownerSignedAt != null && booking.status == BookingStatus.ACTIVE) nav.to("s17")
    }

    GearScreen(
        appBar = { AppBar(onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                if (booking.fullySigned) {
                    GearButton(
                        if (returned) "See the return check" else "Open the rental",
                        { nav.to(if (returned) "s18" else "s17") },
                        Modifier.fillMaxWidth(),
                    )
                } else if (!complete) {
                    GearButton("Finish the condition check first", { nav.to("s15") }, Modifier.fillMaxWidth(), type = ButtonType.SECONDARY)
                } else {
                    GearButton(
                        if (signedByMe) "Waiting for ${booking.owner.name.substringBefore(' ')}…" else "Sign and release",
                        { vm.sign(booking.ref) },
                        Modifier.fillMaxWidth(),
                        enabled = !signedByMe,
                    )
                }
            }
        },
    ) {
        VGap(Space.s)
        Text(
            "Both of you agree this is the condition",
            style = GearType.headingXl,
            color = GearText.primary,
        )
        VGap(Space.m)

        val photos = vm.angles.mapNotNull { frames[it] }
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
            IconRow(
                Lucide.X, it,
                Modifier.padding(vertical = 4.dp),
                tint = GearColor.Chalk400,
                textColor = GearText.tertiary,
                strikethrough = true,
            )
        }

        VGap(Space.m)
        Eyebrow("Existing damage recorded")
        VGap(Space.s)
        if (marks.isEmpty()) {
            Text("None recorded at pickup.", style = GearType.bodyS, color = GearText.tertiary)
            VGap(Space.s)
            GearLink("Mark damage before you sign", { nav.to("s15b") }, icon = Lucide.Pin)
        }
        // One mark, one source: the severity is stated once, in the badge.
        marks.forEach { m ->
            DamageRow(m, "Recorded at pickup")
            VGap(Space.s)
        }

        VGap(Space.l)
        SignatureBlock(booking.renter, booking.renterSignedAt, alpha = 1f)
        VGap(Space.sm)
        SignatureBlock(booking.owner, booking.ownerSignedAt, alpha = alpha)

        VGap(Space.m)
        Banner(
            text = "This record is what settles any claim later. It can't be edited after both signatures.",
            kind = BannerKind.SAFETY,
        )
        VGap(Space.section)
    }
}

/** A recorded mark, as a list row. The severity is stated once, in the badge. */
@Composable
private fun DamageRow(mark: Mark, recorded: String) {
    GearCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            GearImage(mark.photo ?: Demo.torqConditionPhotos[0], "${mark.area} photo", Modifier.size(52.dp), GearShape.sm)
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
    signedAt: java.time.LocalDateTime?,
    alpha: Float,
) {
    val signed = signedAt != null
    GearCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(person, size = 32.dp)
                Spacer(Modifier.width(Space.s))
                Text(person.name, style = GearType.labelL, color = GearText.primary)
                Spacer(Modifier.weight(1f))
                if (signed) {
                    GearIcon(Lucide.CircleCheck, null, size = IconSize.m, tint = GearColor.Success500)
                } else {
                    Text(
                        "AWAITING",
                        style = GearType.labelXs,
                        color = GearColor.Warning700,
                        modifier = Modifier.alpha(alpha),
                    )
                }
            }
            VGap(Space.sm)
            if (signed) {
                Text(
                    person.name,
                    style = GearType.headingL.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                    color = GearColor.Pine900,
                )
            } else {
                Spacer(Modifier.height(28.dp))
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
    var checklistOpen by rememberSaveable { mutableStateOf(false) }
    val booking = bookingState ?: return NoRental(nav, "Rental", "A rental appears here once you and the owner have signed for it.")
    val item = booking.item
    val left = ChronoUnit.MILLIS.between(DemoClock.now(), booking.returnAt)
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
                QuickAction(Lucide.CalendarClock, "Extend a day", Modifier.weight(1f)) { vm.extend(booking.ref) }
                QuickAction(Lucide.TriangleAlert, "Report issue", Modifier.weight(1f)) {
                    vm.openThread(booking.owner.handle); nav.to("s33")
                }
                QuickAction(Lucide.MessageSquare, "Message", Modifier.weight(1f)) {
                    vm.openThread(booking.owner.handle); nav.to("s33")
                }
            }

            VGap(Space.l)
            GearCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.cardPadding)) {
                    Eyebrow("Looking after it")
                    VGap(Space.s)
                    Demo.careNotes.forEach {
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
    val context = LocalContext.current
    val booking = bookingState ?: return NoRental(nav, "Return check", "The return check opens once the return frames are in.")
    val owner = booking.owner.name.substringBefore(' ')
    val changes = pairs.mapNotNull { it.change }
    val settled = booking.status == BookingStatus.COMPLETED
    val returned = booking.returnedAt
    val early = returned?.let { ChronoUnit.MINUTES.between(it, booking.returnAt) }

    GearScreen(
        appBar = { AppBar(title = "Return check", onBack = { nav.back() }) },
    ) {
        VGap(Space.s)
        Eyebrow("Pickup vs return")
        VGap(6.dp)
        Text(
            "The same seven angles, shot at handover and again on return. $owner reviews rather than hunts.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
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
                if (settled) {
                    GearIcon(Lucide.CircleCheck, null, size = 52.dp, tint = GearColor.Success500)
                } else {
                    val elapsed = returned?.let { ChronoUnit.HOURS.between(it, DemoClock.now()) } ?: 0
                    ProgressRing(progress = (elapsed / 24f).coerceIn(0.05f, 1f), size = 52.dp, strokeWidth = 5.dp, color = GearColor.Warning500) {
                        Text("${elapsed}h", style = GearType.dataS, color = GearText.primary)
                    }
                }
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            settled -> "Settled"
                            changes.isNotEmpty() -> "$owner is checking ${changes.size} change${if (changes.size > 1) "s" else ""}"
                            returned == null -> "Waiting for the return frames"
                            else -> "$owner is checking"
                        },
                        style = GearType.headingS,
                        color = GearText.primary,
                    )
                    VGap(2.dp)
                    Text(
                        if (settled) "Nothing more to do on this rental." else "24 h window from the return",
                        style = GearType.bodyS,
                        color = GearText.secondary,
                    )
                }
            }
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
                        "${dayTime(returned)} · ${kotlin.math.abs(early)} min ${if (early >= 0) "early" else "late"}",
                        if (early >= 0) GearColor.Success500 else GearColor.Warning700,
                    )
                    VGap(Space.sm)
                }
                SettlementRow(
                    if (changes.isEmpty()) Lucide.CircleCheck else Lucide.TriangleAlert,
                    if (changes.isEmpty()) "No new damage reported" else "${changes.size} difference flagged",
                    if (changes.isEmpty()) "Anything marked at pickup stays on the pickup record"
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
        if (settled) {
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

        VGap(Space.m)
        Banner(
            text = "Your rating shows on $owner's profile and listing straight away.",
            kind = BannerKind.INFO,
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
private fun Frame(photo: Int?, description: String, modifier: Modifier) {
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
    val pair = pairs.firstOrNull { it.change != null }
    val mark = pair?.change
    if (pair == null || mark == null) {
        NoRental(nav, "Differences", "Nothing on the return differs from the pickup record.")
        return
    }
    GearScreen(
        appBar = { AppBar(title = "${mark.area} · pickup vs return", type = AppBarType.MODAL, onBack = { nav.back() }) },
        background = GearSurface.card,
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton(
                        "It's the same", { vm.resolveChange(mark.id) { nav.back() } }, Modifier.weight(1f),
                        type = ButtonType.SECONDARY, size = ButtonSize.M,
                    )
                    GearButton("Raise a claim", { vm.raiseClaim(mark.id) { nav.to("s34") } }, Modifier.weight(1f), size = ButtonSize.M)
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
            text = "${mark.note} It isn't in the pickup record. Both frames are geotagged to the pickup point and timestamped.",
            kind = BannerKind.WARNING,
        )
        VGap(Space.m)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            MetaChip("SAME ANGLE")
            MetaChip(mark.severity.label.uppercase())
            MetaChip("9.7134°N 80.0246°E")
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
