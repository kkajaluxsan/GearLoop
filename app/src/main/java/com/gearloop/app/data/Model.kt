package com.gearloop.app.data

import androidx.annotation.DrawableRes

/**
 * The shapes behind the screens.
 *
 * This is a UI/UX prototype — there is no backend, no persistence and no
 * network. Everything renders from the fixed dataset in [Demo], which is the
 * dataset named in the design brief. Holding it in one place is what keeps the
 * Torq's price identical on all eleven screens it appears on.
 *
 * These types carry no colours. A colour depends on the active theme, so it
 * cannot be settled when an enum constant is created — the accompanying
 * `Fit.color` and `TrustTier.accent` live in `ui/theme/TokenColors.kt` as
 * composable extensions. Call sites read exactly the same.
 */

enum class TrustTier(val label: String) { BRONZE("BRONZE"), SILVER("SILVER"), GOLD("GOLD") }

/** The fit ramp is its own scale — fit is a different system from status. */
enum class Fit(val label: String) {
    PERFECT("PERFECT FIT"),
    GOOD("GOOD FIT"),
    WORKABLE("WORKABLE"),
    POOR("POOR FIT"),
    BLOCKED("NOT YOUR SIZE"),
    UNKNOWN("ADD YOUR HEIGHT"),
}

enum class Grade(val letter: String, val summary: String) {
    A("A", "As new — no marks, everything original"),
    B("B", "Good — light pressure dings on deck, fully watertight"),
    C("C", "Worn — repaired dings, still structurally sound"),
}

data class Person(
    val name: String,
    val handle: String,
    val area: String,
    val tier: TrustTier,
    val rating: Double,
    val reviews: Int,
    val respondsIn: String,
    val listings: Int = 0,
    val memberSince: String = "Mar 2025",
) {
    /** Monogram avatars — no stock faces standing in for people who don't exist. */
    val initials: String
        get() = name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
}

data class GearItem(
    val id: String,
    val title: String,
    val shortTitle: String,
    val code: String,
    val sizeLabel: String,
    val grade: Grade,
    val pricePerDay: Int,
    val distanceKm: Double,
    @param:DrawableRes val photos: List<Int>,
    val owner: Person,
    val fit: Fit,
    val sport: String,
    val instantBook: Boolean = false,
    /** Owner will drop it off. Filterable, so it has to be real data. */
    val delivery: Boolean = false,
    val deposit: Int = 0,
    val available: Boolean = true,
    val nextFree: String? = null,
    val saved: Boolean = false,
) {
    @get:DrawableRes
    val hero: Int get() = photos.first()

    /** `6'6" · GRADE B · TQ-4471` — the full tag, for detail and list views. */
    val tagLine: String get() = "$sizeLabel · GRADE ${grade.letter} · $code"

    /**
     * `6'6" · GRADE B` — the tag on a grid card, where there isn't room for the
     * item code. A real tag doesn't wrap onto a second line, so the compact
     * form drops the code rather than the shape.
     */
    val tagLineCompact: String get() = "$sizeLabel · GRADE ${grade.letter}"
}

data class SpecRow(val label: String, val value: String, val fitRelevant: Boolean = false)

data class PriceLine(
    val label: String,
    val value: String,
    val kind: Kind = Kind.NORMAL,
) {
    enum class Kind { NORMAL, DISCOUNT, TOTAL, MUTED }
}

data class ReviewEntry(
    val author: Person,
    val date: String,
    val stars: Int,
    val body: String,
    @param:DrawableRes val photos: List<Int> = emptyList(),
    val ownerResponse: String? = null,
)

data class TimelineNode(
    val time: String,
    val title: String,
    val state: State,
) {
    enum class State { DONE, CURRENT, FUTURE }
}

/** One of the seven guided angles in the condition-capture flow. */
data class CaptureAngle(
    val label: String,
    val state: State,
    @get:DrawableRes val photo: Int? = null,
) {
    enum class State { CAPTURED, ACTIVE, PENDING }
}

data class SwapOffer(
    val give: GearItem,
    val get: GearItem,
    val giveValue: Int,
    val getValue: Int,
    val counterparty: Person,
    val threeWay: Boolean = false,
) {
    val delta: Int get() = getValue - giveValue
    val deltaPercent: Int
        get() = if (giveValue == 0) 0 else Math.round(delta * 100f / giveValue)
}

data class InsightItem(
    val headline: String,
    val body: String,
    val action: String,
)

data class NeedsAttention(
    val severity: Severity,
    val title: String,
    val detail: String,
    val action: String,
) {
    enum class Severity { URGENT, SOON }
}

data class NotificationItem(
    val kind: Kind,
    val title: String,
    val body: String,
    val time: String,
    val unread: Boolean = false,
) {
    enum class Kind { BOOKING, SWAP, SAFETY, PAYOUT, REVIEW }
}

data class ChatMessage(
    val body: String,
    val time: String,
    val fromMe: Boolean,
    val kind: Kind = Kind.TEXT,
) {
    enum class Kind { TEXT, SYSTEM, LOCATION, REDACTED }
}

data class ScoreFactor(val label: String, val value: String, val progress: Float)

data class VerificationStep(
    val title: String,
    val unlocks: String,
    val state: State,
) {
    enum class State { DONE, CURRENT, LOCKED }
}

data class WalletHold(val amount: Int, val item: String, val releases: String)

data class WalletTxn(val label: String, val date: String, val amount: Int)

/** One cell in the owner's master calendar. */
enum class DayState { AVAILABLE, SELECTED, IN_RANGE, BOOKED, BUFFER, BLOCKED, TODAY, DISABLED }
