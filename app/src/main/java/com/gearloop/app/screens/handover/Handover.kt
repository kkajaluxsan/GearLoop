package com.gearloop.app.screens.handover

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.CaptureAngle
import com.gearloop.app.data.Demo
import com.gearloop.app.data.lkr
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

/* ------------------------------------------------------------------ S-13 */

@Composable
fun S13HandoverHub(nav: Nav) {
    // Both states of the footer are reachable, because a design that only shows
    // the happy state hasn't been designed.
    var withinWindow by rememberSaveable { mutableStateOf(false) }

    GearScreen(
        appBar = { AppBar(title = Demo.BOOKING_REF, onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                Column(Modifier.fillMaxWidth()) {
                    GearButton(
                        label = "Start handover",
                        onClick = { nav.to("s14") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = withinWindow,
                    )
                    if (!withinWindow) {
                        VGap(6.dp)
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button) { withinWindow = true },
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "Unlocks 30 min before pickup · tap to preview",
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
        Countdown("02:14:06", "Until pickup")

        VGap(Space.l)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                ReadinessRow(Demo.nirmal, "You — ready", GearColor.Success500, Lucide.CircleCheck)
                VGap(Space.sm)
                GearDivider()
                VGap(Space.sm)
                ReadinessRow(
                    Demo.kavitha, "Kavitha — on her way", GearColor.Warning700, Lucide.Navigation,
                    chip = "arriving 09:04",
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
                Text("27 Temple Rd, Nallur", style = GearType.headingM, color = GearText.primary)
                VGap(Space.sm)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    GearButton(
                        "Directions", { }, type = ButtonType.SECONDARY, size = ButtonSize.M,
                        icon = Lucide.Navigation,
                    )
                    GearButton(
                        "Call", { }, type = ButtonType.SECONDARY, size = ButtonSize.M,
                        icon = Lucide.Phone,
                    )
                }
                VGap(Space.s)
                Text(
                    "Calls go through a masked number. Neither of you sees the other's real one.",
                    style = GearType.bodyS,
                    color = GearText.tertiary,
                )
            }
        }

        VGap(Space.l)
        Eyebrow("Your checklist before you go")
        VGap(Space.s)
        Demo.handoverChecklist.forEach {
            IconRow(Lucide.Circle, it, Modifier.padding(vertical = 5.dp), tint = GearColor.Chalk400)
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
 */
@Composable
fun S14ScanHandshake(nav: Nav) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )

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
            Text(Demo.BOOKING_REF, style = GearType.dataM, color = GearColor.Chalk400)
            Spacer(Modifier.width(Space.s))
        }

        Box(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Viewport stand-in. No camera permission is requested — this is a
            // UI prototype, so the frame is drawn rather than opened.
            Box(Modifier.fillMaxSize().background(GearColor.Viewport))
            val bracketColor = GearColor.HiVis500
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
            GearTag(Demo.torq.tagLine, Modifier.alpha(0.9f))
        }

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.l)
            Text(
                "Scan the tag on the board",
                style = GearType.headingL,
                color = GearText.onDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.s)
            Text(
                "Kavitha scans yours next",
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
                PartyMark(Demo.nirmal, done = true)
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
                PartyMark(Demo.kavitha, done = false, alpha = alpha)
            }

            VGap(Space.l)
            GearButton(
                "Enter 6-digit code instead",
                { nav.to("s15") },
                Modifier.fillMaxWidth(),
                type = ButtonType.GHOST,
                size = ButtonSize.M,
                contentColor = GearText.onDark,
            )
            VGap(Space.s)
            Text(
                "BOTH SCANS MUST HAPPEN WITHIN 10 MIN",
                style = GearType.dataS,
                color = GearColor.Chalk400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            VGap(Space.m)
            GearButton("Scan", { nav.to("s15") }, Modifier.fillMaxWidth(), icon = Lucide.ScanLine)
            VGap(Space.l)
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
            person.name.split(" ").first(),
            style = GearType.labelXs,
            color = GearColor.Chalk300,
        )
    }
}

/* ------------------------------------------------------------------ S-15 */

/**
 * The trust screen.
 *
 * Every damage dispute in this category comes down to *was it like that when
 * you got it*. Seven guided angles, both parties present, geotagged and
 * timestamped, answers it before it is asked.
 */
@Composable
fun S15ConditionCapture(nav: Nav) {
    // Both the pass and fail states of auto-validation are reachable — the
    // brief asks for both, and the failure path is the one that matters.
    var lastCaptureOk by rememberSaveable { mutableStateOf(true) }

    DarkScreen {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearIconButton(Lucide.X, "Close", { nav.back() }, tone = IconButtonTone.DARK, iconSize = IconSize.l)
            Text(
                "Check the board together",
                style = GearType.headingM,
                color = GearText.onDark,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text("4 / 7", style = GearType.dataM, color = GearColor.HiVis500)
            Spacer(Modifier.width(Space.s))
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize().background(GearColor.Viewport))
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
            Text(
                "DECK — FULL LENGTH",
                style = GearType.dataS,
                color = GearColor.HiVis500,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            )
            // Auto-validation verdict on the previous frame.
            Box(Modifier.align(Alignment.TopCenter).padding(top = 14.dp)) {
                ValidationChip(lastCaptureOk) { lastCaptureOk = !lastCaptureOk }
            }
        }

        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.m)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                Demo.captureAngles.forEach { AngleThumb(it) }
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
                        .clickable(role = Role.Button) { nav.to("s16") },
                    contentAlignment = Alignment.Center,
                ) {
                    GearIcon(Lucide.Camera, "Capture", size = IconSize.xl, tint = GearColor.Pine900)
                }
                GearButton(
                    "Skip angle", { nav.to("s16") },
                    type = ButtonType.GHOST, size = ButtonSize.M,
                    contentColor = GearColor.Chalk300,
                )
            }

            VGap(Space.m)
            Text(
                Demo.CAPTURE_META,
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
            .clickable(role = Role.Button, onClick = onToggle)
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
private fun AngleThumb(angle: CaptureAngle) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            if (angle.state == CaptureAngle.State.CAPTURED) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(GearColor.Success500),
                    contentAlignment = Alignment.Center,
                ) {
                    GearIcon(Lucide.Check, null, size = 10.dp, tint = GearText.onDark, strokeWidth = 3f)
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

@Composable
fun S15bDamageMarking(nav: Nav) {
    var severity by rememberSaveable { mutableStateOf(1) }

    GearScreen(
        appBar = { AppBar(title = "Mark damage", type = AppBarType.MODAL, onBack = { nav.back() }) },
        background = GearSurface.card,
        footer = {
            StickyFooter {
                GearButton("Save this mark", { nav.back() }, Modifier.fillMaxWidth())
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
            Canvas(Modifier.fillMaxSize().padding(horizontal = 120.dp, vertical = 24.dp)) {
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
                // The dropped pin — on the tail.
                val pin = Offset(w * 0.5f, h * 0.86f)
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

        VGap(Space.l)
        Eyebrow("Severity")
        VGap(Space.s)
        SegmentedControl(
            options = listOf("Cosmetic", "Functional", "Major"),
            selectedIndex = severity,
            onSelect = { severity = it },
        )
        VGap(Space.s)
        Text(
            when (severity) {
                0 -> "Marks the surface only. Doesn't affect how it rides, and never triggers a claim on its own."
                1 -> "Affects performance or will get worse if used. Capped at 60% of the item's value."
                else -> "Unsafe or unusable. The rental stops here and the claim goes to review."
            },
            style = GearType.bodyS,
            color = GearText.secondary,
        )

        VGap(Space.l)
        GearTextField(
            value = "Pressure ding on the tail, about 3 cm. Not through the glass.",
            label = "Note",
            state = FieldState.FILLED,
        )
        VGap(Space.m)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            GearImage(Demo.torqConditionPhotos[0], "Damage photo", Modifier.size(80.dp), GearShape.sm)
            Box(
                Modifier
                    .size(80.dp)
                    .clip(GearShape.sm)
                    .border(Border.medium, GearColor.Chalk300, GearShape.sm),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(Lucide.Plus, "Add photo", size = IconSize.l, tint = GearColor.Chalk400)
            }
        }
        VGap(Space.section)
    }
}
