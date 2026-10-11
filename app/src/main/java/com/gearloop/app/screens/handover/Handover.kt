package com.gearloop.app.screens.handover

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.container
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.CaptureAngle
import com.gearloop.app.data.CaptureGuide
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Phase
import com.gearloop.app.data.countdown
import com.gearloop.app.data.dayTime
import com.gearloop.app.data.metaStamp
import com.gearloop.app.data.repo.EvidenceRepository
import com.gearloop.app.data.repo.MoneyRepository
import com.gearloop.app.data.stopwatch
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.discovery.MapThumb
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.Border
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.theme.Text
import com.gearloop.app.ui.vm.EvidenceTrustViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

/** Nothing to hand over yet: the screens below need a confirmed booking. */
@Composable
private fun NoBooking(nav: Nav, title: String) {
    GearScreen(appBar = { AppBar(title = title, onBack = { nav.back() }) }) {
        EmptyState(
            icon = Lucide.PackageOpen,
            headline = "No handover coming up",
            body = "Confirmed bookings show their pickup here.",
            actionLabel = "See my rentals",
            onAction = { nav.tab("s31") },
        )
    }
}

/** How early before pickup the handover can start. */
private const val HANDOVER_WINDOW_MS = 24L * 60 * 60 * 1000

/* ------------------------------------------------------------------ pickup */

private val handoverChecklist = listOf(
    "Bring photo ID",
    "Bring a way to carry the gear",
    "Charge your phone for the photos",
)

/**
 * The pickup: when, where, the checklist, and what to check on the gear's tag.
 * The exact address is shown only once the booking is paid (FR-15). The
 * handover opens a day before pickup.
 */
@Composable
fun S13HandoverHub(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val booking = bookingState ?: return NoBooking(nav, "Pickup")

    val untilPickup = ChronoUnit.MILLIS.between(AppClock.now(), booking.pickupAt)
    val open = untilPickup <= HANDOVER_WINDOW_MS
    val address = MoneyRepository.pickupAddress(booking.owner.handle)
    val iAmOwner = booking.owner.handle == vm.me
    val other = if (iAmOwner) booking.renter else booking.owner
    val otherFirst = other.name.substringBefore(' ')
    val done = handoverChecklist.indices.count { it in booking.checklist }
    // Reached again after pickup, the hub used to keep counting down to it (D-23).
    val pickedUp = booking.status in setOf(BookingStatus.ACTIVE, BookingStatus.RETURNED, BookingStatus.COMPLETED)
    val finished = booking.status == BookingStatus.RETURNED || booking.status == BookingStatus.COMPLETED

    GearScreen(
        appBar = { AppBar(title = booking.ref, onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                Column(Modifier.fillMaxWidth()) {
                    when {
                        finished -> GearButton("See the return check", { nav.to("s18") }, Modifier.fillMaxWidth())
                        booking.status == BookingStatus.ACTIVE -> GearButton("Open the rental", { nav.to("s17") }, Modifier.fillMaxWidth())
                        booking.handshakeDone -> GearButton(
                            "Check the gear together", { vm.startCapture(Phase.PICKUP); nav.to("s15") }, Modifier.fillMaxWidth(),
                        )
                        else -> GearButton(
                            label = "Start handover",
                            onClick = { nav.to("s14") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = open && booking.paid,
                            icon = Lucide.ScanLine,
                        )
                    }
                    if (!open && !booking.handshakeDone && !pickedUp) {
                        VGap(6.dp)
                        Text(
                            "Handover opens 24 h before pickup",
                            style = GearType.bodyS,
                            color = GearText.tertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (!booking.paid && booking.status == BookingStatus.REQUESTED) {
                        VGap(6.dp)
                        Text(
                            "Waiting for ${booking.owner.name.substringBefore(' ')} to accept the request",
                            style = GearType.bodyS,
                            color = GearText.tertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
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
                    "Back with ${booking.owner.name.substringBefore(' ')} · ${dayTime(booking.returnedShownAt ?: booking.returnAt)}",
                    style = GearType.dataM,
                    color = GearText.secondary,
                )
            }
            pickedUp -> Countdown(
                countdown(ChronoUnit.MILLIS.between(AppClock.now(), booking.returnAt)),
                "Picked up · return by ${dayTime(booking.returnAt)}",
            )
            untilPickup > HANDOVER_WINDOW_MS -> Countdown(countdown(untilPickup), "Until pickup · ${dayTime(booking.pickupAt)}")
            untilPickup > 0 -> Countdown(stopwatch(untilPickup), "Until pickup · ${dayTime(booking.pickupAt)}")
            else -> Countdown("Now", "Pickup was due ${dayTime(booking.pickupAt)}")
        }

        VGap(Space.l)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                val ready = pickedUp || done == handoverChecklist.size
                ReadinessRow(
                    booking.renter,
                    when {
                        pickedUp -> "${if (iAmOwner) booking.renter.name.substringBefore(' ') else "You"} — picked up"
                        ready -> "${if (iAmOwner) booking.renter.name.substringBefore(' ') else "You"} — ready"
                        else -> "${if (iAmOwner) booking.renter.name.substringBefore(' ') else "You"} — $done of ${handoverChecklist.size} ready"
                    },
                    if (ready) GearColor.Success500 else GearColor.Warning700,
                    if (ready) Lucide.CircleCheck else Lucide.Clock,
                )
                VGap(Space.sm)
                GearDivider()
                VGap(Space.sm)
                ReadinessRow(
                    booking.owner, "${if (iAmOwner) "You" else booking.owner.name.substringBefore(' ')} — ${if (booking.paid) "confirmed" else "deciding"}",
                    if (booking.paid) GearColor.Success500 else GearColor.Warning700,
                    if (booking.paid) Lucide.CircleCheck else Lucide.Clock,
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
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode("$address, Sri Lanka"))))
                                } catch (_: ActivityNotFoundException) {
                                    context.container.toaster.show("No maps app on this phone")
                                }
                            },
                            type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Navigation,
                        )
                        GearButton(
                            "Message $otherFirst", { vm.openThread(other.handle); nav.to("s33") },
                            type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.MessageSquare,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GearIcon(Lucide.Lock, null, size = IconSize.m, tint = GearText.tertiary)
                        Spacer(Modifier.width(Space.s))
                        Text(
                            "${booking.owner.area} · exact address shown once paid",
                            style = GearType.bodyM,
                            color = GearText.secondary,
                        )
                    }
                }
            }
        }

        if (!iAmOwner) {
            VGap(Space.l)
            Eyebrow("Your checklist before you go")
            VGap(Space.s)
            handoverChecklist.forEachIndexed { i, text ->
                CheckRow(text, i in booking.checklist, { vm.toggleChecklist(booking.ref, i) })
            }
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
            GearIcon(Lucide.QrCode, null, size = IconSize.m, tint = GearText.primary)
            Spacer(Modifier.width(Space.s))
            Column(Modifier.weight(1f)) {
                Text("At pickup, scan the gear's tag", style = GearType.labelM, color = GearText.primary)
                Text(
                    "No tag? Use the booking code.",
                    style = GearType.bodyS,
                    color = GearText.tertiary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(booking.item.code, style = GearType.dataM, color = GearText.primary)
                Text(booking.handoverCode.chunked(3).joinToString(" "), style = GearType.dataS, color = GearText.tertiary)
            }
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

/* ------------------------------------------------------------------ tag check */

/** Says which gear a wrong tag belongs to, when it is a listing on this phone. */
private fun wrongTag(result: EvidenceRepository.TagResult.OtherItem, expected: String): String =
    result.title?.let { "That tag is ${result.code}, the $it. This booking is for $expected." }
        ?: "That tag is ${result.code}. This booking is for $expected."

/**
 * The only fully dark screen in the app: the phone becomes a scanner. The live
 * camera reads the QR code on the gear's tag and checks it is the gear that was
 * booked. If there is no tag, the code printed on it, or the booking's
 * six-digit code, can be typed instead.
 */
@Composable
fun S14ScanHandshake(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    var entering by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf<String?>(null) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var lastScan by rememberSaveable { mutableStateOf("") }
    val booking = bookingState ?: return NoBooking(nav, "Check the gear tag")
    val matched = booking.handshakeDone

    val check: (String) -> Unit = { text ->
        if (!checking && !matched && text != lastScan) {
            checking = true
            lastScan = text
            vm.verifyTag(booking.ref, text) { result ->
                checking = false
                wrong = when (result) {
                    EvidenceRepository.TagResult.Matched -> null
                    is EvidenceRepository.TagResult.OtherItem -> wrongTag(result, booking.item.code)
                    EvidenceRepository.TagResult.NotATag -> "That isn't a GearLoop tag"
                }
            }
        }
    }

    DarkScreen {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIconButton(Lucide.X, "Close", { nav.back() }, tone = IconButtonTone.DARK, iconSize = IconSize.l)
            Spacer(Modifier.weight(1f))
            Text(booking.ref, style = GearType.dataM, color = GearColor.Chalk400)
            Spacer(Modifier.width(Space.s))
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (matched) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).border(3.dp, GearColor.Success500, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        GearIcon(Lucide.Check, null, size = 48.dp, tint = GearColor.Success500, strokeWidth = 2.5f)
                    }
                    VGap(Space.m)
                    GearTag(booking.item.tagLine)
                }
            } else {
                CameraViewfinder(
                    Modifier.fillMaxSize(),
                    onQr = check,
                    reason = "The camera reads the QR code on the gear's tag, to check it's what you booked.",
                )
                val bracketColor = if (wrong != null) GearColor.Danger500 else GearColor.HiVis500
                Canvas(Modifier.size(240.dp)) { drawBrackets(bracketColor) }
            }
        }

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.l)
            Text(
                if (matched) "It's the right gear" else "Scan the tag on the ${booking.item.shortTitle}",
                style = GearType.headingL,
                color = GearText.onDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.s)
            Text(
                when {
                    matched -> "Now photograph it together, angle by angle."
                    wrong != null -> wrong!!
                    else -> "It should read ${booking.item.code}."
                },
                style = GearType.bodyM,
                color = GearColor.Chalk300,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.l)
            if (matched) {
                GearButton(
                    "Check the gear together", { vm.startCapture(Phase.PICKUP); nav.to("s15") },
                    Modifier.fillMaxWidth(), icon = Lucide.Camera,
                )
            } else {
                GearButton(
                    label = "Type the code instead",
                    onClick = { entering = true; wrong = null },
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.SECONDARY,
                    icon = Lucide.Hash,
                )
            }
            VGap(Space.l)
        }
    }

    if (entering) {
        GearDialog(
            title = "Type the code",
            onDismiss = { entering = false },
            confirmLabel = "Check",
            onConfirm = {
                vm.verifyTag(booking.ref, code) { result ->
                    if (result == EvidenceRepository.TagResult.Matched) { entering = false; code = "" }
                    else wrong = if (result is EvidenceRepository.TagResult.OtherItem) wrongTag(result, booking.item.code) else "That code doesn't match this booking"
                }
            },
            confirmEnabled = code.trim().length >= 6,
        ) {
            Text(
                "The code under the QR (${booking.item.code}), or the six-digit booking code.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.s)
            GearTextField(
                code,
                label = "Code",
                placeholder = "e.g. ${booking.item.code}",
                onValueChange = { code = it.take(12); wrong = null },
                keyboardType = KeyboardType.Ascii,
                visualTransformation = com.gearloop.app.ui.components.UppercaseTransformation,
                error = wrong,
            )
        }
    }
}

/** Four corner brackets and a scan line, framing what the camera should see. */
private fun DrawScope.drawBrackets(c: Color) {
    val len = 42.dp.toPx()
    val stroke = 3.dp.toPx()
    drawLine(c, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
    drawLine(c, Offset(0f, 0f), Offset(0f, len), stroke, StrokeCap.Round)
    drawLine(c, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke, StrokeCap.Round)
    drawLine(c, Offset(size.width, 0f), Offset(size.width, len), stroke, StrokeCap.Round)
    drawLine(c, Offset(0f, size.height), Offset(len, size.height), stroke, StrokeCap.Round)
    drawLine(c, Offset(0f, size.height), Offset(0f, size.height - len), stroke, StrokeCap.Round)
    drawLine(c, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke, StrokeCap.Round)
    drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke, StrokeCap.Round)
    drawLine(c.copy(alpha = 0.6f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5.dp.toPx())
}

/* ------------------------------------------------------------------ condition check */

/**
 * The trust screen.
 *
 * Every damage dispute in this category comes down to *was it like that when
 * you got it*. Seven guided angles, photographed together with the camera,
 * timestamped and — where the phone allows — geotagged, answer it before it is
 * asked. Each frame is checked for blur and light as it is taken (FR-08); a
 * failed frame has to be shot again before the record can be signed. The same
 * screen records the return set, so the return check can pair them angle by angle.
 */
@Composable
fun S15ConditionCapture(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val phase by vm.phase.collectAsStateWithLifecycle()
    val allFrames by vm.frames.collectAsStateWithLifecycle()
    val angles by vm.angles.collectAsStateWithLifecycle()
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    var retaking by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val shutter = rememberCameraShutter()
    val fix = rememberLocationFix()
    val booking = bookingState ?: return NoBooking(nav, "Condition check")

    val shape = CaptureGuide.shapeFor(booking.item.sport)
    val frames = allFrames[phase].orEmpty()
    val firstOpen = angles.firstOrNull { frames[it]?.ok != true }
    val current = target?.takeIf { it in angles } ?: firstOpen ?: angles.last()
    val frame = frames[current]
    val done = angles.all { frames[it]?.ok == true }
    val latest = frames.values.maxByOrNull { it.capturedAt }
    val showLive = frame == null || retaking

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
            Text("${frames.count { it.value.ok }} / ${angles.size}", style = GearType.dataM, color = GearColor.HiVis500)
            Spacer(Modifier.width(Space.s))
        }
        // FR-10: the reason for the sequence, at the moment of the ask.
        Text(
            if (phase == Phase.PICKUP) "Anything you record now can never be claimed for later."
            else "Anything that matches pickup is closed off for good.",
            style = GearType.bodyS,
            color = GearColor.Chalk300,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screen),
        )
        VGap(Space.s)

        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (showLive) {
                CameraViewfinder(Modifier.fillMaxSize(), shutter = shutter)
                // The guide outline for this kind of gear, over the live picture.
                val guideColor = GearColor.HiVis500
                Canvas(Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 40.dp)) {
                    drawGuide(shape, guideColor)
                }
            } else {
                // The frame on record for this angle, unstyled: condition photos
                // are evidence, not marketing.
                Box(Modifier.fillMaxSize().background(GearColor.Viewport))
                GearImage(frame!!.photo, "$current frame", Modifier.fillMaxSize().padding(Space.m), GearShape.md)
                Box(Modifier.align(Alignment.TopCenter).padding(top = 14.dp)) {
                    ValidationChip(frame.ok)
                }
            }
            Text(
                "${current.uppercase()} — ${CaptureGuide.framingFor(current)}",
                style = GearType.dataS,
                color = GearColor.HiVis500,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            )
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
                        onClick = { target = angle; retaking = false },
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
                if (showLive) {
                    Shutter("Capture $current", enabled = shutter.ready && !shutter.busy) {
                        shutter.take(context, "frame") { file ->
                            if (file == null) {
                                context.container.toaster.show("The camera couldn't take that one. Try again.")
                            } else {
                                vm.captured(booking.ref, phase, current, file, fix?.first, fix?.second) { ok ->
                                    retaking = false
                                    if (ok) target = angles.firstOrNull { it != current && frames[it]?.ok != true }
                                }
                            }
                        }
                    }
                } else {
                    Shutter("Retake $current") { retaking = true }
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
                        "Clear frame", { vm.retake(booking.ref, phase, current); retaking = false },
                        type = ButtonType.GHOST, size = ButtonSize.M,
                        contentColor = GearColor.Chalk300,
                    )
                    else -> GearButton(
                        "Skip angle", {
                            target = angles.firstOrNull { it != current && frames[it]?.ok != true && angles.indexOf(it) > angles.indexOf(current) } ?: firstOpen
                        },
                        type = ButtonType.GHOST, size = ButtonSize.M,
                        contentColor = GearColor.Chalk300,
                    )
                }
            }

            VGap(Space.m)
            val place = latest?.place ?: fix?.let { com.gearloop.app.data.coordinates(it.first, it.second) } ?: "LOCATION OFF"
            Text(
                if (latest != null) "$place · ${metaStamp(latest.capturedAt)} · SAVED"
                else "$place · ${metaStamp(AppClock.now())} · READY",
                style = GearType.dataS,
                color = GearColor.Chalk500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.m)
        }
    }
}

/** The dashed outline a frame should fill, for this kind of gear. */
private fun DrawScope.drawGuide(shape: CaptureGuide.Shape, color: Color) {
    val dashed = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
    val w = size.width
    val h = size.height
    when (shape) {
        CaptureGuide.Shape.BOARD, CaptureGuide.Shape.HULL -> {
            val bw = minOf(w, h * 0.32f)
            val left = (w - bw) / 2
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + bw / 2, 0f)
                cubicTo(left + bw * 0.98f, h * 0.26f, left + bw * 0.98f, h * 0.70f, left + bw * 0.62f, h)
                lineTo(left + bw * 0.38f, h)
                cubicTo(left + bw * 0.02f, h * 0.70f, left + bw * 0.02f, h * 0.26f, left + bw / 2, 0f)
                close()
            }
            drawPath(path, color, style = dashed)
        }
        CaptureGuide.Shape.BIKE -> {
            val r = minOf(w * 0.2f, h * 0.22f)
            val cy = h * 0.68f
            drawCircle(color, r, Offset(w * 0.25f, cy), style = dashed)
            drawCircle(color, r, Offset(w * 0.75f, cy), style = dashed)
            val frame = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.25f, cy); lineTo(w * 0.45f, cy - r * 1.4f); lineTo(w * 0.70f, cy - r * 1.4f); lineTo(w * 0.75f, cy)
                moveTo(w * 0.45f, cy - r * 1.4f); lineTo(w * 0.50f, cy); lineTo(w * 0.25f, cy)
            }
            drawPath(frame, color, style = dashed)
        }
        CaptureGuide.Shape.ITEM -> {
            val inset = minOf(w, h) * 0.1f
            drawRoundRect(color, Offset(inset, inset * 2), Size(w - inset * 2, h - inset * 4), CornerRadius(24f, 24f), style = dashed)
        }
    }
}

/**
 * What the photo check measured on this frame: its brightness and sharpness,
 * worked out from the picture the camera took. Read-only — it used to flip
 * between pass and fail on a tap, which let any photo be passed by hand. A
 * frame that fails is retaken.
 */
@Composable
private fun ValidationChip(ok: Boolean) {
    val color = if (ok) GearText.successOnImage else GearText.dangerOnImage
    Row(
        Modifier
            .clip(GearShape.full)
            .background(GearSurface.overImage)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GearIcon(if (ok) Lucide.Check else Lucide.TriangleAlert, null, size = IconSize.s, tint = color)
        Text(if (ok) "Sharp · well lit" else "Not clear enough — retake", style = GearType.labelM, color = color)
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

/* ------------------------------------------------------------------ damage marks */

/**
 * Read the marks already recorded on this side of the rental; Create a mark by
 * tapping the outline where the damage is, with a close-up photo; Update one by
 * tapping it in the list; Delete one, with Undo. Both people see every mark.
 */
@Composable
fun S15bDamageMarking(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val bookingState by vm.booking.collectAsStateWithLifecycle()
    val phase by vm.phase.collectAsStateWithLifecycle()
    val allMarks by vm.marks.collectAsStateWithLifecycle()
    val allFrames by vm.frames.collectAsStateWithLifecycle()
    val angles by vm.angles.collectAsStateWithLifecycle()
    val booking = bookingState ?: return NoBooking(nav, "Mark damage")
    val marks = allMarks.filter { it.phase == phase }
    val shape = CaptureGuide.shapeFor(booking.item.sport)

    var editingId by rememberSaveable { mutableLongStateOf(-1L) }
    // A pickup record on a board opens on the mark that belongs on one: cosmetic.
    val template = if (phase == Phase.PICKUP) Demo.PICKUP_MARK else Demo.RETURN_MARK
    val startArea = template.area.takeIf { it in angles } ?: angles.first()
    var area by rememberSaveable(angles) { mutableStateOf(startArea) }
    var severity by rememberSaveable { mutableStateOf(if (phase == Phase.PICKUP) DamageMark.Severity.COSMETIC.ordinal else DamageMark.Severity.FUNCTIONAL.ordinal) }
    var note by rememberSaveable { mutableStateOf(if (shape == CaptureGuide.Shape.BOARD) template.note else "") }
    var x by rememberSaveable { mutableFloatStateOf(0.5f) }
    var y by rememberSaveable { mutableFloatStateOf(0.5f) }
    var closeUp by rememberSaveable { mutableStateOf<String?>(null) }
    val severities = DamageMark.Severity.entries
    val frame = allFrames[phase]?.get(area)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val picker = rememberPhotoPicker(
        title = "Close-up of the damage",
        onTaken = { file -> scope.launch { closeUp = vm.adoptPhoto(file) } },
        onChosen = { uri -> scope.launch { closeUp = vm.importPhoto(uri) } },
    )

    fun load(id: Long) {
        val m = marks.firstOrNull { it.id == id } ?: return
        editingId = id; area = m.area; severity = m.severity.ordinal; note = m.note; x = m.x; y = m.y; closeUp = m.photo
    }

    GearScreen(
        appBar = { AppBar(title = if (editingId >= 0) "Edit mark" else "Mark damage", type = AppBarType.MODAL, onBack = { nav.back() }) },
        background = GearSurface.card,
        footer = {
            StickyFooter {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    if (editingId >= 0) {
                        GearButton("Delete", { vm.deleteMark(editingId) { editingId = -1L } }, type = ButtonType.DANGER, size = ButtonSize.M)
                    }
                    GearButton(
                        if (editingId >= 0) "Save changes" else "Save this mark",
                        {
                            vm.saveMark(editingId.takeIf { it >= 0 }, booking.ref, phase, area, severities[severity], note, x, y, closeUp) { nav.back() }
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
            "Tap the outline where the damage is.",
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
            val fill = GearColor.Chalk200
            val edge = GearColor.Pine800
            val pinColor = GearColor.Danger500
            val pinCore = GearSurface.overImage
            val existing = GearColor.Warning500
            val others = marks.filter { it.id != editingId }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (shape == CaptureGuide.Shape.BOARD || shape == CaptureGuide.Shape.HULL) 110.dp else 32.dp, vertical = 24.dp)
                    .pointerInput(shape) {
                        detectTapGestures { offset ->
                            x = (offset.x / size.width).coerceIn(0f, 1f)
                            y = (offset.y / size.height).coerceIn(0f, 1f)
                            // The part of the outline the tap fell on names the area.
                            area = CaptureGuide.areaAt(shape, x, y)
                        }
                    }
                    .semantics { contentDescription = "Gear outline. Pin on the ${area.lowercase()}." },
            ) {
                drawOutline(shape, fill, edge)
                others.forEach { m -> drawCircle(existing, 6.dp.toPx(), Offset(size.width * m.x, size.height * m.y)) }
                val pin = Offset(size.width * x, size.height * y)
                drawCircle(pinColor.copy(alpha = 0.20f), 26.dp.toPx(), pin)
                drawCircle(pinColor, 9.dp.toPx(), pin)
                drawCircle(pinCore, 3.5.dp.toPx(), pin)
            }
        }

        VGap(Space.m)
        Eyebrow("Where")
        VGap(Space.s)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            angles.forEach { a -> Chip(a, selected = area == a, onClick = { area = a }) }
        }

        VGap(Space.l)
        Eyebrow("Severity")
        VGap(Space.s)
        SegmentedControl(options = severities.map { it.label }, selectedIndex = severity, onSelect = { severity = it })
        VGap(Space.s)
        // The rubric wording lives on the severity itself, so this screen, the
        // claim and the claims desk all quote the same sentence.
        Text(severities[severity].rubric, style = GearType.bodyS, color = GearText.secondary)

        VGap(Space.l)
        GearTextField(value = note, label = "Note", placeholder = "What it is and how big", onValueChange = { note = it }, singleLine = false)
        VGap(Space.m)
        Eyebrow("Photo of the damage")
        VGap(Space.s)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            val photo = closeUp ?: frame?.photo
            if (photo != null) GearImage(photo, "$area photo", Modifier.size(80.dp), GearShape.sm)
            Column(Modifier.weight(1f)) {
                when {
                    closeUp != null -> "Close-up attached"
                    frame != null -> "Using the ${area.lowercase()} frame"
                    else -> null
                }?.let {
                    Text(it, style = GearType.bodyS, color = GearText.tertiary)
                    VGap(Space.xs)
                }
                GearLink(if (closeUp != null) "Replace the close-up" else "Add a close-up", { picker.open() }, icon = Lucide.Camera)
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

/** The outline a mark is pinned on, filled, for this kind of gear. */
private fun DrawScope.drawOutline(shape: CaptureGuide.Shape, fill: Color, edge: Color) {
    val w = size.width
    val h = size.height
    val line = Stroke(width = 2.dp.toPx())
    when (shape) {
        CaptureGuide.Shape.BOARD, CaptureGuide.Shape.HULL -> {
            val outline = androidx.compose.ui.graphics.Path().apply {
                moveTo(w / 2, 0f)
                cubicTo(w * 1.0f, h * 0.24f, w * 1.0f, h * 0.72f, w * 0.60f, h)
                lineTo(w * 0.40f, h)
                cubicTo(w * 0f, h * 0.72f, w * 0f, h * 0.24f, w / 2, 0f)
                close()
            }
            drawPath(outline, fill)
            drawPath(outline, edge, style = line)
            if (shape == CaptureGuide.Shape.HULL) {
                drawOval(edge, Offset(w * 0.3f, h * 0.42f), Size(w * 0.4f, h * 0.16f), style = line)
            } else {
                drawLine(edge.copy(alpha = 0.4f), Offset(w / 2, h * 0.06f), Offset(w / 2, h * 0.94f), 1.dp.toPx())
            }
        }
        CaptureGuide.Shape.BIKE -> {
            val r = minOf(w * 0.2f, h * 0.24f)
            val cy = h * 0.7f
            drawCircle(fill, r, Offset(w * 0.2f, cy))
            drawCircle(fill, r, Offset(w * 0.8f, cy))
            drawCircle(edge, r, Offset(w * 0.2f, cy), style = line)
            drawCircle(edge, r, Offset(w * 0.8f, cy), style = line)
            val frame = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.2f, cy); lineTo(w * 0.42f, cy - r * 1.5f); lineTo(w * 0.72f, cy - r * 1.5f); lineTo(w * 0.8f, cy)
                moveTo(w * 0.42f, cy - r * 1.5f); lineTo(w * 0.5f, cy); lineTo(w * 0.2f, cy)
                moveTo(w * 0.72f, cy - r * 1.5f); lineTo(w * 0.76f, cy - r * 1.9f)
            }
            drawPath(frame, edge, style = line)
        }
        CaptureGuide.Shape.ITEM -> {
            drawRoundRect(fill, Offset(0f, 0f), Size(w, h), CornerRadius(28f, 28f))
            drawRoundRect(edge, Offset(0f, 0f), Size(w, h), CornerRadius(28f, 28f), style = line)
        }
    }
}
