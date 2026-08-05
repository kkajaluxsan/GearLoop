package com.gearloop.app.ui.components

import com.gearloop.app.ui.theme.color
import com.gearloop.app.ui.theme.accent
import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import com.gearloop.app.ui.preview.PreviewFrame
import com.gearloop.app.ui.preview.GearTypePreview
import com.gearloop.app.ui.preview.GearPreview
import com.gearloop.app.data.Demo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Fit
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Person
import com.gearloop.app.data.PriceLine
import com.gearloop.app.data.ReviewEntry
import com.gearloop.app.data.SwapOffer
import com.gearloop.app.data.grouped
import com.gearloop.app.data.km
import com.gearloop.app.data.kmAway
import com.gearloop.app.data.lkr
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space

/** Content components — design brief §4.3. */

enum class ListingLayout { GRID, LIST, WIDE }

@Composable
fun ListingCard(
    item: GearItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    layout: ListingLayout = ListingLayout.GRID,
    showFit: Boolean = true,
) = when (layout) {
    ListingLayout.GRID -> ListingCardGrid(item, onClick, modifier, showFit)
    ListingLayout.LIST -> ListingCardList(item, onClick, modifier, showFit)
    ListingLayout.WIDE -> ListingCardWide(item, onClick, modifier, showFit)
}

@Composable
private fun ListingCardGrid(item: GearItem, onClick: () -> Unit, modifier: Modifier, showFit: Boolean) {
    GearCard(
        modifier
            .width(185.dp)
            .clickable(role = Role.Button, onClick = onClick),
        borderWidth = Border.hair,
    ) {
        Box {
            GearImage(
                res = item.hero,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth().height(139.dp),
                shape = GearShape.imageTop,
                dimmed = !item.available,
            )
            SaveHeart(
                saved = item.saved,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
            if (!item.available && item.nextFree != null) {
                UnavailableStrip(item.nextFree, Modifier.align(Alignment.BottomCenter))
            }
            // The tag hangs off the bottom edge of the photo, the way a real one
            // hangs off the gear.
            GearTag(
                text = item.tagLineCompact,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 10.dp, y = 13.dp),
                tone = if (item.available) TagTone.HI_VIS else TagTone.MUTED,
            )
        }
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 12.dp)) {
            Text(
                text = item.shortTitle,
                style = GearType.headingS,
                color = GearText.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(42.dp),
            )
            if (showFit) {
                Spacer(Modifier.height(6.dp))
                FitStamp(item.fit, Modifier.padding(start = 3.dp))
            }
            Spacer(Modifier.height(10.dp))
            PriceInline(item.pricePerDay)
            Spacer(Modifier.height(3.dp))
            Text(km(item.distanceKm), style = GearType.dataM, color = GearText.secondary)
        }
    }
}

@Composable
private fun ListingCardList(item: GearItem, onClick: () -> Unit, modifier: Modifier, showFit: Boolean) {
    GearCard(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
            Box {
                GearImage(
                    res = item.hero,
                    contentDescription = item.title,
                    modifier = Modifier.size(96.dp),
                    shape = GearShape.sm,
                    dimmed = !item.available,
                )
                if (!item.available) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(GearColor.Chalk700.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "UNAVAILABLE",
                            style = GearType.dataS,
                            color = GearText.onDark,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = item.shortTitle,
                        style = GearType.headingS,
                        color = GearText.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    SaveHeart(item.saved)
                }
                Spacer(Modifier.height(5.dp))
                GearTag(
                    text = item.tagLine,
                    tone = if (item.available) TagTone.HI_VIS else TagTone.MUTED,
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceInline(item.pricePerDay)
                    Spacer(Modifier.weight(1f))
                    if (showFit) FitStamp(item.fit)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(km(item.distanceKm), style = GearType.dataM, color = GearText.secondary)
                    Text("·", style = GearType.dataM, color = GearColor.Chalk300)
                    Text(item.owner.area, style = GearType.bodyS, color = GearText.tertiary)
                    if (item.instantBook) {
                        Spacer(Modifier.weight(1f))
                        InstantBookPill()
                    }
                }
            }
        }
        if (!item.available && item.nextFree != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(GearColor.Chalk700)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(item.nextFree, style = GearType.dataS, color = GearText.onDark)
            }
        }
    }
}

/** The wide variant exists to break the rhythm of a long result list. */
@Composable
private fun ListingCardWide(item: GearItem, onClick: () -> Unit, modifier: Modifier, showFit: Boolean) {
    GearCard(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Box {
            GearImage(
                res = item.hero,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth().height(168.dp),
                shape = GearShape.imageTop,
                dimmed = !item.available,
            )
            SaveHeart(item.saved, Modifier.align(Alignment.TopEnd).padding(10.dp))
            GearTag(
                text = item.tagLine,
                size = TagSize.FULL,
                modifier = Modifier.align(Alignment.BottomStart).offset(x = 12.dp, y = 15.dp),
                tone = if (item.available) TagTone.HI_VIS else TagTone.MUTED,
            )
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 26.dp, bottom = 14.dp)) {
            Text(item.title, style = GearType.headingM, color = GearText.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriceInline(item.pricePerDay)
                Spacer(Modifier.weight(1f))
                if (showFit) FitStamp(item.fit)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(kmAway(item.distanceKm), style = GearType.dataM, color = GearText.secondary)
                Text("·", style = GearType.dataM, color = GearColor.Chalk300)
                Text(item.owner.area, style = GearType.bodyS, color = GearText.tertiary)
            }
        }
    }
}

@Composable
private fun UnavailableStrip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(GearColor.Chalk700.copy(alpha = 0.94f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, style = GearType.dataS, color = GearText.onDark)
    }
}

@Composable
fun SaveHeart(saved: Boolean, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(999.dp))
            .background(GearSurface.overImage.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(
            Lucide.Heart,
            if (saved) "Saved" else "Save",
            size = IconSize.s,
            tint = if (saved) GearColor.HiVis600 else GearColor.Pine800,
            filled = saved,
        )
    }
}

/** `LKR 2,800` in mono with a quiet `/ day` beside it. */
@Composable
fun PriceInline(
    pricePerDay: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = GearType.dataL,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(lkr(pricePerDay), style = style, color = GearText.primary)
        Text(" / day", style = GearType.bodyS, color = GearText.tertiary, modifier = Modifier.padding(bottom = 2.dp))
    }
}

@Composable
fun InstantBookPill(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(GearShape.full)
            .background(GearColor.HiVis200)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        GearIcon(Lucide.Zap, null, size = 12.dp, tint = GearColor.Pine900)
        Text("Instant", style = GearType.labelXs, color = GearColor.Pine900)
    }
}

@Composable
fun SwapCard(
    offer: SwapOffer,
    onPropose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (offer.threeWay) GearColor.Marine500 else GearSurface.border
    GearCard(modifier.fillMaxWidth(), borderColor = accent, borderWidth = if (offer.threeWay) 1.5.dp else 1.dp) {
        Column(Modifier.padding(Space.cardPadding)) {
            if (offer.threeWay) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GearIcon(Lucide.Workflow, null, size = IconSize.s, tint = GearColor.Marine500)
                    Text("3-WAY TRADE", style = GearType.labelXs, color = GearColor.Marine500)
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SwapSide("YOU GIVE", offer.give, offer.giveValue, Modifier.weight(1f))
                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                    GearIcon(Lucide.ArrowRightLeft, "swaps with", size = IconSize.xl, tint = GearColor.Marine500)
                }
                SwapSide("YOU GET", offer.get, offer.getValue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.sm))
            FairnessBar(offer.deltaPercent)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(offer.counterparty, size = 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(offer.counterparty.name, style = GearType.bodyS, color = GearText.secondary)
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (offer.delta >= 0) "You add ${grouped(offer.delta)}" else "They add ${grouped(-offer.delta)}",
                    style = GearType.dataM,
                    color = GearText.primary,
                )
            }
            Spacer(Modifier.height(Space.sm))
            GearButton(
                label = "Propose swap",
                onClick = onPropose,
                type = ButtonType.SECONDARY,
                size = ButtonSize.M,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SwapSide(label: String, item: GearItem, value: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(label)
        Spacer(Modifier.height(6.dp))
        GearImage(item.hero, item.title, Modifier.size(88.dp), GearShape.sm)
        Spacer(Modifier.height(6.dp))
        Text(
            item.shortTitle,
            style = GearType.bodyS,
            color = GearText.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(36.dp),
        )
        Text(grouped(value), style = GearType.dataM, color = GearText.secondary)
    }
}

@Composable
fun BookingCard(
    item: GearItem,
    dates: String,
    status: String,
    statusColor: Color,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    reference: String? = null,
    onClick: (() -> Unit)? = null,
) {
    GearCard(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)) {
        Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.Top) {
            GearImage(item.hero, item.title, Modifier.size(72.dp), GearShape.sm)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status, statusColor)
                    if (reference != null) {
                        Spacer(Modifier.weight(1f))
                        Text(reference, style = GearType.dataS, color = GearText.tertiary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(item.shortTitle, style = GearType.headingS, color = GearText.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(dates, style = GearType.dataM, color = GearText.secondary)
            }
        }
        Row(Modifier.padding(start = Space.sm, end = Space.sm, bottom = Space.sm)) {
            GearButton(actionLabel, onAction, Modifier.fillMaxWidth(), ButtonType.SECONDARY, ButtonSize.M)
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = GearType.labelXs,
        color = color,
        modifier = modifier
            .clip(GearShape.xs)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun OwnerStrip(
    person: Person,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showResponseTime: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(person, size = 44.dp, showTierRing = true)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(person.name, style = GearType.headingS, color = GearText.primary)
                TrustBadge(person)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarValue(person.rating, person.reviews)
                if (showResponseTime) {
                    Text("  ·  Responds in ${person.respondsIn}", style = GearType.bodyS, color = GearText.tertiary)
                }
            }
        }
    }
}

@Composable
fun TrustBadge(person: Person, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(GearShape.xs)
            .background(person.tier.accent.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        GearIcon(Lucide.BadgeCheck, null, size = 12.dp, tint = person.tier.accent)
        Text(person.tier.label, style = GearType.labelXs, color = person.tier.accent)
    }
}

@Composable
fun PriceRow(line: PriceLine, modifier: Modifier = Modifier) {
    val isTotal = line.kind == PriceLine.Kind.TOTAL
    Column(modifier.fillMaxWidth()) {
        if (isTotal) {
            GearDivider(Modifier.padding(bottom = Space.sm))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = line.label,
                style = if (isTotal) GearType.headingM else GearType.bodyM,
                color = if (line.kind == PriceLine.Kind.MUTED) GearText.tertiary else if (isTotal) GearText.primary else GearText.body,
            )
            Text(
                text = line.value,
                style = if (isTotal) GearType.dataL else GearType.dataM,
                color = when (line.kind) {
                    PriceLine.Kind.DISCOUNT -> GearColor.Success500
                    PriceLine.Kind.MUTED -> GearText.tertiary
                    else -> GearText.primary
                },
            )
        }
    }
}

@Composable
fun ReviewCard(
    review: ReviewEntry,
    modifier: Modifier = Modifier,
    showSubRatings: Boolean = false,
) {
    GearCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(review.author, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(review.author.name, style = GearType.headingS, color = GearText.primary)
                    Text(review.date, style = GearType.dataS, color = GearText.tertiary)
                }
                Text("★".repeat(review.stars), style = GearType.dataM, color = GearColor.Pine900)
            }
            if (showSubRatings) {
                Spacer(Modifier.height(Space.sm))
                RatingBar("Condition", 0.98f)
            }
            Spacer(Modifier.height(10.dp))
            Text(review.body, style = GearType.bodyM, color = GearText.body)
            if (review.photos.isNotEmpty()) {
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    review.photos.forEach {
                        GearImage(it, "Review photo", Modifier.size(72.dp), GearShape.sm)
                    }
                }
            }
            if (review.ownerResponse != null) {
                Spacer(Modifier.height(Space.sm))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.sm)
                        .background(GearColor.Chalk50)
                        .padding(Space.sm)
                ) {
                    Box(Modifier.width(2.dp).height(36.dp).background(GearColor.Chalk300))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Eyebrow("Owner response")
                        Spacer(Modifier.height(3.dp))
                        Text(review.ownerResponse, style = GearType.bodyS, color = GearText.body)
                    }
                }
            }
        }
    }
}

/** Sub-rating bar — label, track, value. Used under the review aggregate. */
@Composable
fun RatingBar(
    label: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    value: String? = null,
    color: Color = GearColor.Pine800,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = GearType.bodyS, color = GearText.secondary, modifier = Modifier.width(120.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(GearShape.full)
                .background(GearColor.Chalk200)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(GearShape.full)
                    .background(color)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = value ?: String.format("%.2f", fraction * 5f),
            style = GearType.dataM,
            color = GearText.primary,
            modifier = Modifier.width(44.dp),
        )
    }
}

/**
 * Owner-dashboard insight. Every insight card ends in an action — an
 * observation the owner can't act on is just noise.
 */
@Composable
fun InsightCard(
    headline: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccentCard(GearColor.HiVis500, modifier.fillMaxWidth().wrapContentHeight()) {
        Row(verticalAlignment = Alignment.Top) {
            GearIcon(Lucide.TrendingUp, null, size = IconSize.m, tint = GearColor.Pine800)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(headline, style = GearType.headingS, color = GearText.primary)
                Spacer(Modifier.height(4.dp))
                Text(body, style = GearType.bodyS, color = GearText.secondary)
                Spacer(Modifier.height(8.dp))
                GearButton(action, onAction, type = ButtonType.GHOST, size = ButtonSize.S)
            }
        }
    }
}

/** Stat tile — the three-across row on the owner dashboard. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = GearText.primary,
) {
    GearCard(modifier) {
        Column(Modifier.padding(Space.sm)) {
            Text(value, style = GearType.dataL, color = valueColor)
            Spacer(Modifier.height(2.dp))
            Text(label, style = GearType.bodyS, color = GearText.tertiary)
        }
    }
}

/** A bordered row that behaves as a button — dates, payment method, and so on. */
@Composable
fun SelectRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingColor: Color = GearColor.Success500,
    leadingIcon: com.gearloop.app.ui.icons.LucideIcon? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.sm)
                .border(Border.medium, GearSurface.border, GearShape.sm)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Space.m, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                GearIcon(leadingIcon, null, size = IconSize.m, tint = GearColor.Pine800)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = GearType.labelM, color = GearText.tertiary)
                Spacer(Modifier.height(2.dp))
                Text(value, style = GearType.dataM, color = GearText.primary)
            }
            GearIcon(Lucide.ChevronRight, null, size = IconSize.m, tint = GearColor.Chalk500)
        }
        if (supporting != null) {
            Spacer(Modifier.height(6.dp))
            Text(supporting, style = GearType.bodyS, color = supportingColor)
        }
    }
}

/** Fit ramp legend chip, used on the foundations board and in filters. */
@Composable
fun FitChip(fit: Fit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(GearShape.xs)
            .background(fit.color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(GearShape.full).background(fit.color))
        Text(fit.label, style = GearType.dataS, color = fit.color)
    }
}

/* ------------------------------------------------------------- previews */

@GearPreview
@Composable
private fun ListingCardLayoutsPreview() = PreviewFrame {
    ListingCard(Demo.torq, {}, layout = ListingLayout.GRID)
    ListingCard(Demo.torq, {}, layout = ListingLayout.LIST)
    ListingCard(Demo.torq, {}, layout = ListingLayout.WIDE)
}

/**
 * The list card is the one that breaks first at 200 %: title, tag, price and
 * distance all share a fixed-height row beside a 96 dp thumbnail.
 */
@GearTypePreview
@Composable
private fun ListingCardTypePreview() = PreviewFrame {
    ListingCard(Demo.torq, {}, layout = ListingLayout.LIST)
    ListingCard(Demo.wetsuit, {}, layout = ListingLayout.LIST, showFit = false)
}

@GearPreview
@Composable
private fun SwapCardPreview() = PreviewFrame {
    SwapCard(Demo.swapMatches[0], onPropose = {})
}

@GearPreview
@Composable
private fun BookingCardPreview() = PreviewFrame {
    BookingCard(
        item = Demo.torq,
        dates = "8 – 10 Aug",
        status = "CONFIRMED",
        statusColor = GearColor.Success500,
        actionLabel = "View handover",
        onAction = {},
        reference = "GL-2481",
    )
}

@GearTypePreview
@Composable
private fun PeopleAndPricePreview() = PreviewFrame {
    OwnerStrip(Demo.kavitha)
    GearDivider()
    PriceInline(Demo.torq.pricePerDay)
    PriceRow(PriceLine("3-day discount", "−840", PriceLine.Kind.DISCOUNT))
    PriceRow(PriceLine("Total", lkr(9_240), PriceLine.Kind.TOTAL))
}

@GearPreview
@Composable
private fun ReviewAndInsightPreview() = PreviewFrame {
    ReviewCard(Demo.torqReviews[0])
    InsightCard(
        Demo.ownerInsights[0].headline,
        Demo.ownerInsights[0].body,
        Demo.ownerInsights[0].action,
        {},
    )
}

@GearPreview
@Composable
private fun SmallCardPartsPreview() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        StatTile("68%", "OCCUPANCY", Modifier.weight(1f))
        StatTile("4.9", "RATING", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        Fit.entries.take(3).forEach { FitChip(it) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        InstantBookPill()
        StatusBadge("CONFIRMED", GearColor.Success500)
        SaveHeart(saved = true)
    }
    SelectRow("Dates", "8 – 10 Aug", {}, supporting = "3 days")
}
