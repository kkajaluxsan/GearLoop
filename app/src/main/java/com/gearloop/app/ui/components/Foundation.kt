package com.gearloop.app.ui.components

import com.gearloop.app.ui.theme.accent
import com.gearloop.app.ui.theme.Border
import com.gearloop.app.ui.preview.PreviewFrame
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.GearPreview
import com.gearloop.app.data.Demo
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Person
import com.gearloop.app.data.Thumbs
import com.gearloop.app.data.Photos
import com.gearloop.app.data.PhotoStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.LocalWireframe
import com.gearloop.app.ui.theme.Space

/** Shared building blocks — the pieces every other component is assembled from. */

/**
 * A scroll position that survives leaving the composition.
 *
 * `rememberScrollState()` is scoped to the composition, so walking forward and
 * backing up returns to the top of the previous screen — which looks like a bug
 * when the app is being demonstrated. Backed by the navigation entry's saved
 * state, this restores where the reader actually was.
 */
@Composable
fun rememberSavedScrollState(): ScrollState =
    rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

/**
 * A multi-select list that survives leaving the composition.
 *
 * The saver is written out rather than left to the automatic one, because the
 * automatic saver cannot store a snapshot list — the elements have to be handed
 * over as a plain list and rebuilt on the way back.
 */
@Composable
fun <T : Any> rememberSavedStateList(init: () -> List<T>): SnapshotStateList<T> =
    rememberSaveable(
        saver = listSaver<SnapshotStateList<T>, T>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { init().toMutableStateList() }

/**
 * Every photograph in the app goes through here, which is what keeps the
 * imagery reading as one set. Photos are colour-graded to a common warm-neutral
 * treatment when they are packaged; this handles crop, corner radius and the
 * two overlays the design uses — a pine scrim for text legibility, and the
 * dimming applied to unavailable listings.
 *
 * It also decides which copy of the picture to decode. A 1200×900 frame costs
 * 4.1 MB of heap whether it is filling the screen or sitting in a 48 dp chip,
 * and a list screen holds eight of them at once. Measuring first and reaching
 * for the 600 px copy when the slot is small keeps that off the heap without
 * any call site having to think about it. See [Thumbs].
 */
@Composable
fun GearImage(
    @DrawableRes res: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    scrim: Float = 0f,
    dimmed: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
) {
    // In wireframe mode the picture is replaced by the slot it occupies.
    if (LocalWireframe.current) {
        WireSlot(modifier, shape)
        return
    }

    BoxWithConstraints(modifier.clip(shape).background(GearColor.Chalk200)) {
        // An unbounded slot could be laid out at any size, so it gets the full frame.
        val bounded = constraints.hasBoundedWidth && constraints.hasBoundedHeight
        val widest = maxOf(constraints.maxWidth, constraints.maxHeight)
        val chosen = if (bounded && widest <= Thumbs.MAX_PX) Thumbs.of(res) else res

        Image(
            painter = painterResource(chosen),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.45f else 1f),
        )
        if (scrim > 0f) {
            Box(Modifier.fillMaxSize().background(GearColor.Pine900.copy(alpha = scrim)))
        }
    }
}

/**
 * A photograph by its key: a bundled picture, or one taken or chosen on this
 * phone. Bundled keys go through the resource version above, with its
 * thumbnail choice; a picture from the phone is decoded off the main thread
 * at about the size of its slot, and cached.
 */
@Composable
fun GearImage(
    photo: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    scrim: Float = 0f,
    dimmed: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (!Photos.isFile(photo)) {
        GearImage(Photos.res(photo), contentDescription, modifier, shape, scrim, dimmed, contentScale)
        return
    }
    if (LocalWireframe.current) {
        WireSlot(modifier, shape)
        return
    }
    val context = LocalContext.current
    BoxWithConstraints(modifier.clip(shape).background(GearColor.Chalk200)) {
        val target = when {
            constraints.hasBoundedWidth && constraints.hasBoundedHeight -> maxOf(constraints.maxWidth, constraints.maxHeight)
            constraints.hasBoundedWidth -> constraints.maxWidth
            else -> PhotoStore.MAX_EDGE
        }
        val bitmap by produceState<ImageBitmap?>(null, photo, target) { value = PhotoStore.load(context, photo, target) }
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.45f else 1f),
            )
        }
        if (scrim > 0f) {
            Box(Modifier.fillMaxSize().background(GearColor.Pine900.copy(alpha = scrim)))
        }
    }
}

/**
 * The slot a picture occupies, drawn as a crossed box in wireframe mode. Colours
 * are read here rather than inside the draw lambda, because a DrawScope is not
 * a composable scope.
 */
@Composable
private fun WireSlot(modifier: Modifier, shape: Shape) {
    val edge = GearSurface.borderStrong
    Box(
        modifier
            .clip(shape)
            .background(GearSurface.sunken)
            .border(Border.hair, edge, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawLine(edge, Offset(0f, 0f), Offset(size.width, size.height), Border.hair.toPx())
            drawLine(edge, Offset(size.width, 0f), Offset(0f, size.height), Border.hair.toPx())
        }
    }
}

/**
 * A bottom-weighted gradient for text sitting over a photograph. Softer than a
 * flat scrim, so the image is still legible underneath it.
 */
@Composable
fun PhotoGradient(modifier: Modifier = Modifier, strength: Float = 0.8f) {
    Box(
        modifier.background(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.45f to GearColor.Pine900.copy(alpha = strength * 0.3f),
                1f to GearColor.Pine900.copy(alpha = strength),
            )
        )
    )
}

/**
 * Monogram avatar.
 *
 * Stock portraits would put real faces behind users who don't exist, so the app
 * uses initials on a pine chip instead. It also keeps the people layer visually
 * quiet, which lets the gear photography carry the screen.
 */
@Composable
fun Avatar(
    person: Person,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showTierRing: Boolean = false,
) = Avatar(
    initials = person.initials,
    modifier = modifier,
    size = size,
    ringColor = if (showTierRing) person.tier.accent else null,
)

/**
 * The same monogram for someone who has no marketplace record.
 *
 * GearLoop Ops staff have no trust tier, so the ring is passed in rather than
 * derived — a tier ring drawn round an adjudicator would claim a standing the
 * app has no way to know.
 */
@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    ringColor: Color? = null,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(GearColor.Pine800)
            .then(if (ringColor != null) Modifier.border(Border.heavy, ringColor, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = if (size >= 36.dp) GearType.dataM else GearType.dataS,
            color = GearText.onDark,
            textAlign = TextAlign.Center,
        )
    }
}

/** Section eyebrow — label/xs uppercase in chalk/500. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GearText.tertiary,
) {
    Text(text.uppercase(), style = GearType.labelXs, color = color, modifier = modifier)
}

/** Eyebrow plus optional leading mark and trailing action, e.g. "See all". */
@Composable
fun SectionHeader(
    eyebrow: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Eyebrow(eyebrow)
            leading?.invoke()
        }
        trailing?.invoke()
    }
}

@Composable
fun GearDivider(modifier: Modifier = Modifier, color: Color = GearSurface.border) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/**
 * The default card treatment from the brief: white fill, 1px chalk/200 border,
 * radius/md, no shadow. Shadow is reserved for things that genuinely float.
 */
@Composable
fun GearCard(
    modifier: Modifier = Modifier,
    shape: Shape = GearShape.md,
    fill: Color = GearSurface.card,
    borderColor: Color = GearSurface.border,
    borderWidth: Dp = Border.hair,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(shape)
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)
            .background(fill),
        content = content,
    )
}

/** A card with a coloured spine — used for insights and priority rows. */
@Composable
fun AccentCard(
    accent: Color,
    modifier: Modifier = Modifier,
    fill: Color = GearSurface.card,
    accentWidth: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .clip(GearShape.md)
            .border(Border.hair, GearSurface.border, GearShape.md)
            .background(fill),
    ) {
        Box(Modifier.width(accentWidth).fillMaxHeight().background(accent))
        Column(Modifier.padding(Space.cardPadding), content = content)
    }
}

/** Vertical gap, so screens read as a stack of sections. */
@Composable
fun VGap(height: Dp) {
    Spacer(Modifier.height(height))
}

/** Small mono pill — photo counts, status marks, metadata over images. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = GearColor.Pine900.copy(alpha = 0.72f),
    textColor: Color = GearText.onDark,
    shape: Shape = RoundedCornerShape(999.dp),
) {
    Text(
        text = text,
        style = GearType.dataS,
        color = textColor,
        modifier = modifier
            .clip(shape)
            .background(fill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Star rating rendered in mono, because it is a number people act on. */
@Composable
fun StarValue(
    rating: Double,
    reviews: Int? = null,
    modifier: Modifier = Modifier,
    color: Color = GearText.primary,
    style: androidx.compose.ui.text.TextStyle = GearType.dataM,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("★ ", style = style, color = color)
        Text(String.format("%.2f", rating).trimEnd('0').trimEnd('.'), style = style, color = color)
        if (reviews != null) {
            Text(" ($reviews)", style = style, color = GearText.tertiary)
        }
    }
}

/* ------------------------------------------------------------- previews */

@GearPreview
@Composable
private fun AvatarsPreview() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
        Avatar(Demo.kavitha, size = 56.dp, showTierRing = true)
        Avatar(Demo.kavitha)
        Avatar(Demo.kavitha, size = 32.dp)
        Avatar(Demo.ADJUDICATOR_INITIALS, size = 36.dp)
    }
}

@GearTypePreview
@Composable
private fun HeadersPreview() = PreviewFrame {
    Eyebrow("What this means for you")
    SectionHeader("This weekend near you", trailing = { GearLink("See all", {}) })
    GearDivider()
    StarValue(4.9, 27)
}

@GearPreview
@Composable
private fun CardsAndPillsPreview() = PreviewFrame {
    GearCard {
        Column(Modifier.padding(Space.cardPadding)) {
            Text("A plain card", style = GearType.headingS, color = GearText.primary)
            Text(
                "Separated by a 1 px border, never a shadow.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
        }
    }
    AccentCard(GearColor.HiVis500) {
        Column(Modifier.padding(Space.cardPadding)) {
            Text("An accent card", style = GearType.headingS, color = GearText.primary)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        Pill("1 / 5")
        Pill("GEOTAGGED")
    }
}

/**
 * The two copies of the same photograph. `GearImage` picks between them from the
 * space it is given, so this preview is also the check that the map in [Thumbs]
 * is complete — a missing entry shows up as the full frame in the small slot.
 */
@GearPreview
@Composable
private fun GearImagePreview() = PreviewFrame {
    GearImage(Demo.torq.hero, Demo.torq.title, Modifier.fillMaxWidth().height(160.dp), GearShape.md)
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(96.dp), GearShape.sm)
        GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(64.dp), GearShape.sm, dimmed = true)
        GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(44.dp), GearShape.sm, scrim = 0.5f)
    }
}
