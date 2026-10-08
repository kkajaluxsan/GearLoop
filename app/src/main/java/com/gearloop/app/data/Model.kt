package com.gearloop.app.data

import androidx.annotation.DrawableRes

/**
 * The shapes behind the screens.
 *
 * Since Milestone 03 these are the *domain* types: the repositories read rows out
 * of the Room database and hand the screens these, so a screen never sees an
 * entity or a cursor. [Demo] is still the single source of truth for the data
 * the database is seeded with — the Torq costs the same everywhere because it is
 * written down once, and the seed copies it from there.
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

    /**
     * `4.94` — two decimals, trailing zeros dropped.
     *
     * A rating is a number people act on, so it goes through here rather than
     * being interpolated raw: a derived average prints as 4.944444444444445 if
     * you let it, and the four screens that show this one would each have shown
     * a different length of it.
     */
    val ratingText: String
        get() = String.format("%.2f", rating).trimEnd('0').trimEnd('.')
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
    // ---- Milestone 03: fields the database carries per listing. Defaults keep
    // every GearItem in [Demo] compiling unchanged; the seed fills them in.
    val status: ListingStatus = ListingStatus.PUBLISHED,
    /** Identical units of the same listing, each a row in the owner calendar. */
    val units: Int = 1,
    /** Board volume in litres, where fit is judged on float rather than size. */
    val volumeL: Int? = null,
    /** Frame band for bikes — `M` — where fit is judged against the rider's height. */
    val frameBand: String? = null,
    val specs: List<SpecRow> = emptyList(),
    val included: List<String> = emptyList(),
    val excluded: List<String> = emptyList(),
    @param:DrawableRes val conditionPhotos: List<Int> = emptyList(),
    val conditionNote: String = "",
    val conditionDate: String = "",
    /** Extras that travel with every booking of this item, priced per day. */
    val extras: List<Extra> = emptyList(),
    /** 3+, 7+ and 14+ day discounts, on or off. */
    val discounts: List<Boolean> = listOf(true, true, false),
    val waiveDepositWithPlus: Boolean = false,
) {
    /** The first photograph; a draft with none yet shows the neutral flat-lay. */
    @get:DrawableRes
    val hero: Int get() = photos.firstOrNull() ?: com.gearloop.app.R.drawable.photo_gear_flatlay

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

/** Something rented alongside the item, like the Torq's rash guard. */
data class Extra(val name: String, val perDay: Int)

enum class ListingStatus { DRAFT, PUBLISHED, PAUSED }

/**
 * A booking's life, in order. The renter's screens and the owner's screens read
 * the same row, so a request accepted in the owner's inbox is confirmed in the
 * renter's rentals list without anything being copied between them.
 */
enum class BookingStatus(val label: String) {
    DRAFT("Draft"),
    REQUESTED("Awaiting owner"),
    CONFIRMED("Confirmed"),
    ACTIVE("In progress"),
    RETURNED("Owner checking"),
    COMPLETED("Completed"),
    DECLINED("Declined"),
    CANCELLED("Cancelled"),
    ;

    /** Statuses that occupy the calendar — a declined request frees its dates. */
    val holdsDates: Boolean get() = this in setOf(REQUESTED, CONFIRMED, ACTIVE, RETURNED)
}

/**
 * The three protection plans on S-09. The numbers that decide what a renter can
 * lose live here, so the plan card, the bill and the wallet quote one source.
 */
enum class Protection(val label: String, val perDay: Int, val excess: Int?, val theftCovered: Boolean) {
    BASIC("Basic", 0, null, false),
    PLUS("Plus", 420, 3_000, false),
    PRO("Pro", 780, 0, true),
    ;

    /** Basic is the only plan that holds a deposit. */
    val holdsDeposit: Boolean get() = this == BASIC

    /** The most a renter could be asked to pay for damage under this plan (FR-05). */
    fun mostYouCouldLose(deposit: Int): Int = excess ?: deposit
}

/** Which side of the rental a capture or a damage mark belongs to. */
enum class Phase { PICKUP, RETURN }

enum class ClaimStatus(val label: String) {
    OPEN("Awaiting renter"),
    CONTESTED("Contested"),
    ACCEPTED("Accepted by renter"),
    MORE_EVIDENCE("More evidence requested"),
    ESCALATED("Escalated"),
    DECIDED("Ruling issued"),
}

enum class ProposalStatus(val label: String) {
    DRAFT("Draft"),
    SENT("Sent"),
    COUNTERED("Countered"),
    ACCEPTED("Accepted"),
    DECLINED("Declined"),
    WITHDRAWN("Withdrawn"),
}

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

/**
 * A booking request waiting on the owner.
 *
 * [days] is here because the quoted price has to match the dates beside it: the
 * inbox used to multiply the daily rate by three for every row, so a two-night
 * request was priced as three and a five-night one as three as well.
 */
data class BookingRequest(
    val renter: Person,
    val item: GearItem,
    val dates: String,
    val days: Int,
    /** Each request has its own clock; they were all showing the same countdown. */
    val respondWithin: String,
    val completedRentals: Int,
    val onTimeReturns: Int,
    val verified: Int,
) {
    val total: Int get() = item.pricePerDay * days

    /** The rows behind the "trust breakdown" disclosure, per renter. */
    val trustBreakdown: List<Pair<String, String>>
        get() = listOf(
            "Completed rentals" to "$completedRentals",
            "On-time returns" to "$onTimeReturns%",
            "Verification" to "$verified / 6",
        )
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

/**
 * How a listing's reviews are spread across the five star values, five-first.
 *
 * The count is the only thing written down. The review total, the average and
 * the bar lengths are all read back off it, so the histogram, the "N reviews"
 * label and the ★ figure on four different screens cannot disagree — which they
 * did when the bars were percentages and the average was typed by hand.
 */
data class StarDistribution(val counts: List<Int>) {
    init {
        require(counts.size == 5) { "five star values, five counts" }
    }

    /** Reviews at [stars], where 5 is the first entry. */
    fun at(stars: Int): Int = counts[5 - stars]

    /** Share of the total, for the bar behind the count. */
    fun fractionAt(stars: Int): Float = if (total == 0) 0f else at(stars).toFloat() / total

    val total: Int get() = counts.sum()

    val average: Double
        get() = if (total == 0) 0.0
        else counts.mapIndexed { i, n -> (5 - i) * n }.sum().toDouble() / total
}

data class VerificationStep(
    val title: String,
    val unlocks: String,
    val state: State,
) {
    enum class State { DONE, CURRENT, LOCKED }

    /** The one step that stores something: the address it verified. */
    val isHomeAddress: Boolean get() = title == HOME_ADDRESS

    companion object {
        /**
         * Matched by name, not by the word "address": "Email address" contains
         * it too, which put the home address and its remove link under the
         * email step and made removing it re-open email instead (D-12).
         */
        const val HOME_ADDRESS = "Home address"
    }
}

/**
 * One recorded mark on a piece of gear.
 *
 * The severity is the field that matters: it decides what the rubric caps a
 * claim at, and it is quoted on S-15b, S-16, S-34 and the admin console. Holding
 * it once is what stops the same ding being cosmetic on one screen and
 * functional on the next.
 */
data class DamageMark(
    val area: String,
    val summary: String,
    val severity: Severity,
    val note: String,
    val recordedAt: String,
) {
    enum class Severity(val label: String, val capPercent: Int, val rubric: String) {
        COSMETIC(
            "Cosmetic", 0,
            "Marks the surface only. Doesn't affect how it rides, and never triggers a claim on its own.",
        ),
        FUNCTIONAL(
            "Functional", 60,
            "Affects performance or will get worse if used. Capped at 60% of the item's value.",
        ),
        MAJOR(
            "Major", 100,
            "Unsafe or unusable. The rental stops here and the claim goes to review.",
        ),
    }

    /** `Tail · pressure ding` — the one-line form used in a list row. */
    val headline: String get() = "$area · ${summary.lowercase()}"

    /** The most this severity could ever be awarded against [value]. */
    fun capOn(value: Int): Int = value * severity.capPercent / 100
}

data class WalletHold(val amount: Int, val item: String, val releases: String)

data class WalletTxn(val label: String, val date: String, val amount: Int)

/** One cell in the owner's master calendar. */
enum class DayState { AVAILABLE, SELECTED, IN_RANGE, BOOKED, BUFFER, BLOCKED, TODAY, DISABLED }
