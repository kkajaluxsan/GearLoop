package com.gearloop.app.screens.handover

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.container
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.CaptureAngle
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.Phase
import com.gearloop.app.data.countdown
import com.gearloop.app.data.dayTime
import com.gearloop.app.data.metaStamp
import com.gearloop.app.data.repo.Booking
import com.gearloop.app.data.repo.MoneyRepository
import com.gearloop.app.data.stopwatch
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.discovery.MapThumb
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.vm.EvidenceTrustViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.temporal.ChronoUnit

/** Nothing to hand over yet: the screens below need a confirmed booking. */
@Composable
private fun NoBooking(nav: Nav, title: String) {
    GearScreen(appBar = { AppBar(title = title, onBack = { nav.back() }) }) {
        EmptyState(
            icon = Lucide.PackageOpen,
            headline = "No handover coming up",
            body = "Once a booking is confirmed, pickup and the condition check happen here.",
            actionLabel = "See my rentals",
            onAction = { nav.tab("s31") },
        )
    }
}

/* ------------------------------------------------------------------ S-13 */

private val handoverChecklist = listOf(
    "Bring photo ID",
    "Bring a way to carry the gear",
    "Charge your phone for the handover scan",
)

/**
 * S-13 · Read the booking, the pickup point and the checklist; Update the
 * checklist as items are ticked. The exact address is shown only once the
 * booking is paid (FR-15), and the code shown here is the fallback S-14 accepts.
 */
@Composable
fun S13HandoverHub(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    // Both states of the footer are reachable, because a design that only shows
    // the happy state hasn't been designed.
    var preview by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val booking = bookingState ?: return NoBooking(nav, "Handover")

    val untilPickup = ChronoUnit.MILLIS.between(DemoClock.now(), booking.pickupAt)
    val withinWindow = untilPickup <= 30 * 60 * 1000L || preview
    val address = MoneyRepository.pickupAddress(booking.owner.handle)
    val owner = booking.owner.name.substringBefore(' ')
    val done = handoverChecklist.indices.count { it in booking.checklist }
    // Reached again after pickup, the hub used to keep counting down to it and
    // ask for the checklist — for a board that was already back with its owner (D-23).
    val pickedUp = booking.status in setOf(BookingStatus.ACTIVE, BookingStatus.RETURNED, BookingStatus.COMPLETED)
    val finished = booking.status == BookingStatus.RETURNED || booking.status == BookingStatus.COMPLETED

    GearScreen(
        appBar = { AppBar(title = booking.ref, onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                Column(Modifier.fillMaxWidth()) {
                    when {
                        finished -> GearButton(
                            "See the return check", { nav.to("s18") }, Modifier.fillMaxWidth(),
                        )
                        booking.status == BookingStatus.ACTIVE -> GearButton(
                            "Open the rental", { nav.to("s17") }, Modifier.fillMaxWidth(),
                        )
                        booking.handshakeDone -> GearButton(
                            "Check the gear together", { vm.startCapture(Phase.PICKUP); nav.to("s15") }, Modifier.fillMaxWidth(),
                        )
                        else -> GearButton(
                            label = "Start handover",
                            onClick = { nav.to("s14") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = withinWindow && booking.paid,
                        )
                    }
                    if (!withinWindow && !booking.handshakeDone && !pickedUp) {
                        VGap(6.dp)
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button) { preview = true },
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "Unlocks 30 min before pickup · tap to start early for the demo",
                                style = GearType.bodyS,
                                color = GearText.tertiary,
                            )
                        }
                    }
                }
            }
        },
    ) {
        VGap(Space.m)
        when {
            finished -> {
                Text("Returned", style = GearType.headingXl, color = GearText.primary)
                VGap(4.dp)
                Text(
                    "Back with $owner · ${dayTime(booking.returnedShownAt ?: booking.returnAt)}",
                    style = GearType.dataM,
                    color = GearText.secondary,
                )
            }
            pickedUp -> Countdown(
                countdown(ChronoUnit.MILLIS.between(DemoClock.now(), booking.returnAt)),
                "Picked up · return by ${dayTime(booking.returnAt)}",
            )
            untilPickup > 24 * 60 * 60 * 1000L -> Countdown(countdown(untilPickup), "Until pickup · ${dayTime(booking.pickupAt)}")
            else -> Countdown(stopwatch(untilPickup), "Until pickup")
        }

        VGap(Space.l)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                val ready = pickedUp || done == handoverChecklist.size
                ReadinessRow(
                    booking.renter,
                    when {
                        pickedUp -> "You — picked up"
                        ready -> "You — ready"
                        else -> "You — $done of ${handoverChecklist.size} ready"
                    },
                    if (ready) GearColor.Success500 else GearColor.Warning700,
                    if (ready) Lucide.CircleCheck else Lucide.Clock,
                )
                VGap(Space.sm)
                GearDivider()
                VGap(Space.sm)
                ReadinessRow(
                    booking.owner, "$owner — confirmed", GearColor.Success500, Lucide.CircleCheck,
                    chip = booking.pickupAt.toLocalTime().toString(),
                )
            }
        }

        VGap(Space.m)
        MapThumb(Modifier.fillMaxWidth().height(180.dp), showRoute = true)

        VGap(Space.m)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Eyebrow("Pickup address")
                VGap(6.dp)
                if (booking.paid) {
                    Text(address, style = GearType.headingM, color = GearText.primary)
                    VGap(Space.sm)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        GearButton(
                            "Directions", {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:9.7134,80.0246?q=" + Uri.encode(address))))
                                } catch (_: ActivityNotFoundException) {
                                    context.container.toaster.show("No maps app on this phone")
                                }
                            },
                            type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Navigation,
                        )
                        GearButton(
                            "Message", { vm.openThread(booking.owner.handle); nav.to("s33") },
                            type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.MessageSquare,
                        )
                    }
                    VGap(Space.s)
                    Text(
                        "Calls go through a masked number. Neither of you sees the other's real one.",
                        style = GearType.bodyS,
                        color = GearText.tertiary,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GearIcon(Lucide.Lock, null, size = IconSize.m, tint = GearText.tertiary)
                        Spacer(Modifier.width(Space.s))
                        Text(
                            "${booking.owner.area} · the exact address unlocks once $owner accepts and the booking is paid.",
                            style = GearType.bodyM,
                            color = GearText.secondary,
                        )
                    }
                }
            }
        }

        VGap(Space.l)
        Eyebrow("Your checklist before you go")
        VGap(Space.s)
        handoverChecklist.forEachIndexed { i, text ->
            CheckRow(text, i in booking.checklist, { vm.toggleChecklist(booking.ref, i) })
        }

        VGap(Space.m)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .background(GearColor.Chalk50)
                .padding(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIcon(Lucide.Hash, null, size = IconSize.m, tint = GearText.primary)
            Spacer(Modifier.width(Space.s))
            Column(Modifier.weight(1f)) {
                Text("Handover code", style = GearType.labelM, color = GearText.primary)
                Text("If the tag won't scan, read this to $owner.", style = GearType.bodyS, color = GearText.tertiary)
            }
            Text(booking.handoverCode.chunked(3).joinToString(" "), style = GearType.dataL, color = GearText.primary)
        }

        VGap(Space.m)
        Banner("Meet in a public spot in daylight where you can.", kind = BannerKind.INFO)
        VGap(Space.section)
    }
}

@Composable
private fun ReadinessRow(
    person: com.gearloop.app.data.Person,
    label: String,
    color: Color,
    icon: com.gearloop.app.ui.icons.LucideIcon,
    chip: String? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Avatar(person, size = 36.dp)
        Spacer(Modifier.width(Space.sm))
        Text(label, style = GearType.labelL, color = GearText.primary, modifier = Modifier.weight(1f))
        if (chip != null) {
            Text(
                chip,
                style = GearType.dataS,
                color = color,
                modifier = Modifier
                    .clip(GearShape.xs)
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
            Spacer(Modifier.width(Space.s))
        }
        GearIcon(icon, null, size = IconSize.m, tint = color)
    }
}

/* ------------------------------------------------------------------ S-14 */

/**
 * The only fully dark screen in the app. The contrast is intentional — this is
 * the moment the phone becomes a scanner rather than a catalogue.
 *
 * S-14 · Read the handshake state; Update it — the renter's scan, then the
 * owner's, or both at once with the six-digit code. On one device the owner's
 * scan is simulated a moment after the renter's.
 */
@Composable
fun S14ScanHandshake(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    var entering by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    val booking = bookingState ?: return NoBooking(nav, "Handshake")
    val ownerFirst = booking.owner.name.substringBefore(' ')
    val renterDone = booking.renterScannedAt != null
    val ownerDone = booking.ownerScannedAt != null

    DarkScreen {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIconButton(
                Lucide.X, "Close", { nav.back() },
                tone = IconButtonTone.DARK, iconSize = IconSize.l,
            )
            Spacer(Modifier.weight(1f))
            Text(booking.ref, style = GearType.dataM, color = GearColor.Chalk400)
            Spacer(Modifier.width(Space.s))
        }

        Box(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // The scanner's viewport, drawn. The scan itself is recorded when the
            // button below is pressed — camera scanning is the release after this.
            Box(Modifier.fillMaxSize().background(GearColor.Viewport))
            val bracketColor = if (renterDone) GearColor.Success500 else GearColor.HiVis500
            Canvas(Modifier.size(240.dp)) {
                val len = 42.dp.toPx()
                val stroke = 3.dp.toPx()
                val c = bracketColor
                // four corner brackets
                drawLine(c, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
                drawLine(c, Offset(0f, 0f), Offset(0f, len), stroke, StrokeCap.Round)
                drawLine(c, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke, StrokeCap.Round)
                drawLine(c, Offset(size.width, 0f), Offset(size.width, len), stroke, StrokeCap.Round)
                drawLine(c, Offset(0f, size.height), Offset(len, size.height), stroke, StrokeCap.Round)
                drawLine(c, Offset(0f, size.height), Offset(0f, size.height - len), stroke, StrokeCap.Round)
                drawLine(c, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke, StrokeCap.Round)
                drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke, StrokeCap.Round)
                drawLine(
                    c.copy(alpha = 0.6f),
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    1.5.dp.toPx(),
                )
            }
            GearTag(booking.item.tagLine, Modifier.alpha(0.9f))
        }

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.l)
            Text(
                when {
                    ownerDone && renterDone -> "Handshake complete"
                    renterDone -> "$ownerFirst is scanning yours"
                    else -> "Scan the tag on the ${booking.item.shortTitle}"
                },
                style = GearType.headingL,
                color = GearText.onDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.s)
            Text(
                if (ownerDone && renterDone) "Both scans recorded. Now check it together." else "$ownerFirst scans yours next",
                style = GearType.bodyM,
                color = GearColor.Chalk300,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            VGap(Space.l)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PartyMark(booking.renter, done = renterDone, alpha = alpha)
                val linkColor = GearColor.Chalk500
                Canvas(Modifier.width(64.dp).height(2.dp)) {
                    drawLine(
                        linkColor,
                        Offset(0f, 0f),
                        Offset(size.width, 0f),
                        2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    )
                }
                PartyMark(booking.owner, done = ownerDone, alpha = alpha)
            }

            VGap(Space.l)
            Text(
                "BOTH SCANS MUST HAPPEN WITHIN 10 MIN",
                style = GearType.dataS,
                color = GearColor.Chalk400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.m)
            if (renterDone && ownerDone) {
                GearButton(
                    "Check the gear together", { vm.startCapture(Phase.PICKUP); nav.to("s15") },
                    Modifier.fillMaxWidth(), icon = Lucide.Camera,
                )
            } else {
                GearButton(
                    if (renterDone) "Waiting for $ownerFirst…" else "Scan",
                    { vm.scan(booking.ref) },
                    Modifier.fillMaxWidth(), icon = Lucide.ScanLine,
                    enabled = !renterDone,
                )
            }
            VGap(Space.s)
            // Below the primary, and carrying an icon. As a bare ghost button it
            // was white centred text between two other lines of white centred
            // text, with nothing about it saying it could be tapped.
            if (!(renterDone && ownerDone)) {
                GearButton(
                    label = "Enter 6-digit code instead",
                    onClick = { entering = true; wrong = false },
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.GHOST,
                    size = ButtonSize.M,
                    icon = Lucide.Hash,
                    contentColor = GearText.onDark,
                )
            }
            VGap(Space.l)
        }
    }

    if (entering) {
        GearDialog(
            title = "Enter the handover code",
            onDismiss = { entering = false },
            confirmLabel = "Confirm",
            onConfirm = {
                vm.enterCode(booking.ref, code) { ok ->
                    if (ok) { entering = false; code = "" } else wrong = true
                }
            },
            confirmEnabled = code.filter(Char::isDigit).length == 6,
        ) {
            Text("It's on the booking in the renter's app (S-13). Both phones show the same six digits.", style = GearType.bodyS, color = GearText.secondary)
            VGap(Space.s)
            GearTextField(
                code,
                label = "Six-digit code",
                placeholder = "000 000",
                onValueChange = { code = it.filter(Char::isDigit).take(6); wrong = false },
                keyboardType = KeyboardType.NumberPassword,
                error = if (wrong) "That code doesn't match this booking" else null,
            )
        }
    }
}

@Composable
private fun PartyMark(
    person: com.gearloop.app.data.Person,
    done: Boolean,
    alpha: Float = 1f,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .size(48.dp)
                    .alpha(if (done) 1f else alpha)
                    .clip(CircleShape)
                    .border(Border.heavy, if (done) GearColor.Success500 else GearColor.HiVis500, CircleShape)
                    .background(GearColor.Pine800),
                contentAlignment = Alignment.Center,
            ) {
                Text(person.initials, style = GearType.dataM, color = GearText.onDark)
            }
            if (done) {
                Box(
                    Modifier.size(18.dp).clip(CircleShape).background(GearColor.Success500),
                    contentAlignment = Alignment.Center,
                ) {
                    GearIcon(Lucide.Check, null, size = 11.dp, tint = GearText.onDark, strokeWidth = 3f)
                }
            }
        }
        VGap(6.dp)
        Text(
            person.name.split(" ").first().uppercase(),
            style = GearType.labelXs,
            color = GearColor.Chalk300,
        )
    }
}

/* ------------------------------------------------------------------ S-15 */

/** What each angle asks for, under the guide. */
private fun framingFor(angle: String): String = when (angle) {
    "Deck", "Bottom" -> "FULL LENGTH"
    "Rail L", "Rail R" -> "SIDE ON"
    else -> "CLOSE UP"
}

/**
 * The trust screen.
 *
 * Every damage dispute in this category comes down to *was it like that when
 * you got it*. Seven guided angles, both parties present, geotagged and
 * timestamped, answers it before it is asked.
 *
 * S-15 · Read the frames recorded so far; Create one per angle with the
 * shutter; Update a frame's check (failed with a stated reason, FR-08) or Delete
 * it to shoot again. The same screen records the return set, so S-18 can pair
 * them angle by angle.
 *
 * The frame recorded is the condition photograph the dataset holds for that
 * angle — the record, its time, place and check are real; the camera is the
 * next release (deviation DV-05).
 */
@Composable
fun S15ConditionCapture(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val phase by vm.phase.collectAsStateWithLifecycle()
    val allFrames by vm.frames.collectAsStateWithLifecycle()
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    val booking = bookingState ?: return NoBooking(nav, "Condition check")

    val frames = allFrames[phase].orEmpty()
    val angles = vm.angles
    // An angle is open until it has a frame that passed its check (FR-08): a
    // failed frame has to be shot again before the record can be signed.
    val firstOpen = angles.firstOrNull { frames[it]?.ok != true }
    val current = target?.takeIf { it in angles } ?: firstOpen ?: angles.last()
    val frame = frames[current]
    val done = angles.all { frames[it]?.ok == true }
    val latest = frames.values.maxByOrNull { it.capturedAt }

    DarkScreen {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIconButton(Lucide.X, "Close", { nav.back() }, tone = IconButtonTone.DARK, iconSize = IconSize.l)
            Text(
                if (phase == Phase.PICKUP) "Check it together" else "Check it on return",
                style = GearType.headingM,
                color = GearText.onDark,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text("${frames.size} / ${angles.size}", style = GearType.dataM, color = GearColor.HiVis500)
            Spacer(Modifier.width(Space.s))
        }
        // FR-10: the reason for the sequence, at the moment of the ask rather
        // than only at return. Milestone 01 found renters read it as distrust.
        Text(
            if (phase == Phase.PICKUP) "Anything you record now can never be claimed for later. These photos protect you as much as the owner."
            else "The same seven angles as pickup. Anything that matches is closed off for good.",
            style = GearType.bodyS,
            color = GearColor.Chalk300,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screen),
        )
        VGap(Space.s)

        Box(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize().background(GearColor.Viewport))
            if (frame != null) {
                // The frame on record for this angle, unstyled: condition photos
                // are evidence, not marketing.
                GearImage(frame.photo, "$current frame", Modifier.fillMaxSize().padding(Space.m), GearShape.md)
            } else {
                // Silhouette guide — a surfboard deck outline in dashed hi-vis.
                val guideColor = GearColor.HiVis500
                Canvas(Modifier.fillMaxSize().padding(horizontal = 104.dp, vertical = 40.dp)) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        val w = size.width
                        val h = size.height
                        moveTo(w / 2, 0f)
                        cubicTo(w * 0.98f, h * 0.26f, w * 0.98f, h * 0.70f, w * 0.62f, h)
                        lineTo(w * 0.38f, h)
                        cubicTo(w * 0.02f, h * 0.70f, w * 0.02f, h * 0.26f, w / 2, 0f)
                        close()
                    }
                    drawPath(
                        path,
                        guideColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                        ),
                    )
                    drawLine(
                        guideColor.copy(alpha = 0.4f),
                        Offset(size.width / 2, 0f),
                        Offset(size.width / 2, size.height),
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                    )
                }
            }
            Text(
                "${current.uppercase()} — ${framingFor(current)}",
                style = GearType.dataS,
                color = GearColor.HiVis500,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            )
            // The capture-time check on this frame. Tap it to fail the frame with
            // its reason; a failed frame is the one the shutter shoots again.
            if (frame != null) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 14.dp)) {
                    ValidationChip(frame.ok) { vm.setFrameOk(booking.ref, phase, current, !frame.ok) }
                }
            }
        }

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.m)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                angles.forEach { angle ->
                    val state = when {
                        angle == current -> CaptureAngle.State.ACTIVE
                        angle in frames -> CaptureAngle.State.CAPTURED
                        else -> CaptureAngle.State.PENDING
                    }
                    AngleThumb(
                        CaptureAngle(angle, state, frames[angle]?.photo),
                        captured = angle in frames,
                        failed = frames[angle]?.ok == false,
                        onClick = { target = angle },
                    )
                }
            }

            VGap(Space.m)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearButton(
                    "Mark damage", { nav.to("s15b") },
                    type = ButtonType.GHOST, size = ButtonSize.M, icon = Lucide.Pin,
                    contentColor = GearText.onDark,
                )
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(4.dp, GearSurface.overImage.copy(alpha = 0.25f), CircleShape)
                        .background(GearColor.HiVis500)
                        .clickable(role = Role.Button, onClickLabel = "Capture $current") {
                            vm.capture(booking.ref, phase, current)
                            target = angles.firstOrNull { it != current && frames[it]?.ok != true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    GearIcon(Lucide.Camera, "Capture $current", size = IconSize.xl, tint = GearColor.Pine900)
                }
                when {
                    done -> GearButton(
                        "Continue", {
                            if (phase == Phase.PICKUP) nav.to("s16")
                            else vm.finishReturn(booking.ref) { nav.to("s18") }
                        },
                        type = ButtonType.GHOST, size = ButtonSize.M,
                        contentColor = GearColor.HiVis500,
                    )
                    frame != null -> GearButton(
                        "Clear frame", { vm.retake(booking.ref, phase, current) },
                        type = ButtonType.GHOST, size = ButtonSize.M,
                        contentColor = GearColor.Chalk300,
                    )
                    else -> GearButton(
                        "Skip angle", { target = angles.firstOrNull { it != current && frames[it]?.ok != true && angles.indexOf(it) > angles.indexOf(current) } ?: firstOpen },
                        type = ButtonType.GHOST, size = ButtonSize.M,
                        contentColor = GearColor.Chalk300,
                    )
                }
            }

            VGap(Space.m)
            Text(
                if (latest != null) "%.4f°N, %.4f°E · %s · SAVED".format(latest.latitude, latest.longitude, metaStamp(latest.capturedAt))
                else "9.7134°N, 80.0246°E · ${metaStamp(DemoClock.now())} · READY",
                style = GearType.dataS,
                color = GearColor.Chalk500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.m)
        }
    }
}

@Composable
private fun ValidationChip(ok: Boolean, onToggle: () -> Unit) {
    val color = if (ok) GearText.successOnImage else GearText.dangerOnImage
    Row(
        Modifier
            .clip(GearShape.full)
            .background(GearSurface.overImage)
            .clickable(role = Role.Button, onClickLabel = if (ok) "Fail this frame" else "Pass this frame", onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GearIcon(
            if (ok) Lucide.Check else Lucide.TriangleAlert,
            null, size = IconSize.s, tint = color,
        )
        Text(
            if (ok) "Sharp · well lit" else "Too blurry — retake",
            style = GearType.labelM,
            color = color,
        )
    }
}

@Composable
private fun AngleThumb(angle: CaptureAngle, captured: Boolean, failed: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(role = Role.Button, onClickLabel = "Go to ${angle.label}", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(GearShape.sm)
                .then(
                    when (angle.state) {
                        CaptureAngle.State.ACTIVE -> Modifier.border(2.5.dp, GearColor.HiVis500, GearShape.sm)
                        CaptureAngle.State.PENDING -> Modifier.border(Border.medium, GearColor.Chalk600, GearShape.sm)
                        else -> Modifier
                    }
                )
                .background(GearColor.ViewportChip),
            contentAlignment = Alignment.Center,
        ) {
            if (angle.photo != null) {
                GearImage(angle.photo, angle.label, Modifier.fillMaxSize(), GearShape.sm)
            } else {
                GearIcon(
                    Lucide.Camera, null, size = IconSize.m,
                    tint = if (angle.state == CaptureAngle.State.ACTIVE) GearColor.HiVis500 else GearColor.Chalk600,
                )
            }
            if (captured) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (failed) GearColor.Danger500 else GearColor.Success500),
                    contentAlignment = Alignment.Center,
                ) {
                    GearIcon(if (failed) Lucide.X else Lucide.Check, null, size = 10.dp, tint = GearText.onDark, strokeWidth = 3f)
                }
            }
        }
        VGap(5.dp)
        Text(
            angle.label.uppercase(),
            style = GearType.labelXs,
            color = when (angle.state) {
                CaptureAngle.State.ACTIVE -> GearColor.HiVis500
                CaptureAngle.State.CAPTURED -> GearColor.Chalk300
                else -> GearColor.Chalk500
            },
        )
    }
}

/* ----------------------------------------------------------------- S-15b */

/**
 * S-15b · Read the marks already recorded on this side of the rental; Create a
 * mark by tapping the outline where the damage is; Update one by tapping it in
 * the list; Delete one, with Undo. Both parties see every mark.
 */
@Composable
fun S15bDamageMarking(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val phase by vm.phase.collectAsStateWithLifecycle()
    val allMarks by vm.marks.collectAsStateWithLifecycle()
    val allFrames by vm.frames.collectAsStateWithLifecycle()
    val booking = bookingState ?: return NoBooking(nav, "Mark damage")
    val marks = allMarks.filter { it.phase == phase }

    var editingId by rememberSaveable { mutableLongStateOf(-1L) }
    // A pickup record opens on the mark that belongs on one: cosmetic. Opening on
    // functional put a claimable ding on the board before the renter took it.
    val template = if (phase == Phase.PICKUP) Demo.PICKUP_MARK else Demo.RETURN_MARK
    var area by rememberSaveable { mutableStateOf(template.area) }
    var severity by rememberSaveable { mutableStateOf(template.severity.ordinal) }
    var note by rememberSaveable { mutableStateOf(template.note) }
    var x by rememberSaveable { mutableFloatStateOf(0.5f) }
    var y by rememberSaveable { mutableFloatStateOf(0.86f) }
    val severities = DamageMark.Severity.entries
    val frame = allFrames[phase]?.get(area)

    fun load(id: Long) {
        val m = marks.firstOrNull { it.id == id } ?: return
        editingId = id; area = m.area; severity = m.severity.ordinal; note = m.note; x = m.x; y = m.y
    }

    GearScreen(
        appBar = { AppBar(title = if (editingId >= 0) "Edit mark" else "Mark damage", type = AppBarType.MODAL, onBack = { nav.back() }) },
        background = GearSurface.card,
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    if (editingId >= 0) {
                        GearButton(
                            "Delete", { vm.deleteMark(editingId) { editingId = -1L } },
                            type = ButtonType.DANGER, size = ButtonSize.M,
                        )
                    }
                    GearButton(
                        if (editingId >= 0) "Save changes" else "Save this mark",
                        {
                            vm.saveMark(
                                editingId.takeIf { it >= 0 }, booking.ref, phase, area,
                                severities[severity], note, x, y,
                            ) { nav.back() }
                        },
                        Modifier.weight(1f),
                        enabled = note.isNotBlank(),
                    )
                }
            }
        },
    ) {
        VGap(Space.m)
        Text(
            "Tap the board where the damage is. Both of you will see this mark.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.m)

        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(GearShape.md)
                .background(GearColor.Chalk50)
                .border(Border.hair, GearSurface.border, GearShape.md),
            contentAlignment = Alignment.Center,
        ) {
            val boardFill = GearColor.Chalk200
            val boardEdge = GearColor.Pine800
            val centreLine = GearColor.Chalk300
            val pinColor = GearColor.Danger500
            val pinCore = GearSurface.overImage
            val existing = GearColor.Warning500
            val others = marks.filter { it.id != editingId }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 120.dp, vertical = 24.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            x = (offset.x / size.width).coerceIn(0f, 1f)
                            y = (offset.y / size.height).coerceIn(0f, 1f)
                            // The part of the board the tap fell on names the area.
                            area = when {
                                y < 0.18f -> "Nose"
                                y > 0.80f -> "Tail"
                                x < 0.25f -> "Rail L"
                                x > 0.75f -> "Rail R"
                                else -> "Deck"
                            }
                        }
                    }
                    .semantics { contentDescription = "Board outline. Pin on the ${area.lowercase()}." },
            ) {
                val w = size.width
                val h = size.height
                val outline = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w / 2, 0f)
                    cubicTo(w * 1.0f, h * 0.24f, w * 1.0f, h * 0.72f, w * 0.60f, h)
                    lineTo(w * 0.40f, h)
                    cubicTo(w * 0f, h * 0.72f, w * 0f, h * 0.24f, w / 2, 0f)
                    close()
                }
                drawPath(outline, boardFill)
                drawPath(
                    outline, boardEdge,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
                drawLine(
                    centreLine,
                    Offset(w / 2, h * 0.06f),
                    Offset(w / 2, h * 0.94f),
                    1.dp.toPx(),
                )
                // Marks already on the record, smaller and in amber.
                others.forEach { m -> drawCircle(existing, 6.dp.toPx(), Offset(w * m.x, h * m.y)) }
                val pin = Offset(w * x, h * y)
                drawCircle(pinColor.copy(alpha = 0.20f), 26.dp.toPx(), pin)
                drawCircle(pinColor, 9.dp.toPx(), pin)
                drawCircle(pinCore, 3.5.dp.toPx(), pin)
            }
            Text(
                "DECK",
                style = GearType.labelXs,
                color = GearText.tertiary,
                modifier = Modifier.align(Alignment.TopStart).padding(Space.sm),
            )
        }

        VGap(Space.m)
        Eyebrow("Where")
        VGap(Space.s)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            vm.angles.forEach { a -> Chip(a, selected = area == a, onClick = { area = a }) }
        }

        VGap(Space.l)
        Eyebrow("Severity")
        VGap(Space.s)
        SegmentedControl(
            options = severities.map { it.label },
            selectedIndex = severity,
            onSelect = { severity = it },
        )
        VGap(Space.s)
        // The rubric wording lives on the severity itself, so this screen, the
        // claim and the adjudicator's console all quote the same sentence.
        Text(
            severities[severity].rubric,
            style = GearType.bodyS,
            color = GearText.secondary,
        )

        VGap(Space.l)
        GearTextField(
            value = note,
            label = "Note",
            placeholder = "What it is and how big",
            onValueChange = { note = it },
            singleLine = false,
        )
        VGap(Space.m)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            if (frame != null) {
                GearImage(frame.photo, "$area frame", Modifier.size(80.dp), GearShape.sm)
                Text(
                    "The ${area.lowercase()} frame from the condition check goes with this mark.",
                    style = GearType.bodyS,
                    color = GearText.tertiary,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    "Shoot the ${area.lowercase()} angle on the condition check and its frame goes with this mark.",
                    style = GearType.bodyS,
                    color = GearText.tertiary,
                )
            }
        }

        if (marks.isNotEmpty()) {
            VGap(Space.l)
            Eyebrow("Already marked (${marks.size})")
            VGap(Space.s)
            marks.forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.sm)
                        .background(if (m.id == editingId) GearColor.HiVis100 else Color.Transparent)
                        .clickable(role = Role.Button, onClickLabel = "Edit this mark") { load(m.id) }
                        .padding(vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GearIcon(Lucide.Pin, null, size = IconSize.m, tint = GearColor.Danger500)
                    Spacer(Modifier.width(Space.s))
                    Column(Modifier.weight(1f)) {
                        Text("${m.area} · ${m.summary.lowercase()}", style = GearType.labelL, color = GearText.primary)
                        Text(m.note, style = GearType.bodyS, color = GearText.tertiary, maxLines = 2)
                    }
                    StatusBadge(m.severity.label, GearColor.Warning700)
                }
                GearDivider()
            }
        }
        VGap(Space.section)
    }
}
