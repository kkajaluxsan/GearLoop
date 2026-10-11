package com.gearloop.app.data.repo

import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Grade
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Person
import com.gearloop.app.data.Phase
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.ProposalStatus
import com.gearloop.app.data.Protection
import com.gearloop.app.data.Quote
import com.gearloop.app.data.dateRange
import com.gearloop.app.data.dateRangeLong
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * The Milestone 03 domain: what the repositories hand the screens. Nothing here
 * knows about Room — a screen that reads a [Booking] cannot tell whether it came
 * from SQLite, a server or a test.
 */

data class Profile(
    val person: Person,
    val intent: Int,
    val heightCm: Int?,
    val weightKg: Int?,
    val shoeEu: Int,
    val metric: Boolean,
    val couponUsed: Boolean,
    val walletAvailable: Int,
    val address: String?,
    val onboarded: Boolean,
    val card: SavedCard?,
) {
    val hasMeasurements: Boolean get() = heightCm != null && weightKg != null
}

/** The card a member saved: brand and last four digits, never the number. */
data class SavedCard(val brand: String, val last4: String, val expiry: String) {
    val label: String get() = "$brand •••• $last4"
}

data class Booking(
    val ref: String,
    val item: GearItem,
    val unit: Int,
    val renter: Person,
    val owner: Person,
    val start: LocalDate,
    val end: LocalDate,
    val protection: Protection,
    val includeExtras: Boolean,
    val coupon: Int,
    val paymentMethod: Int,
    val waiverName: String?,
    val waiverAt: LocalDateTime?,
    val status: BookingStatus,
    val total: Int,
    val depositHold: Int,
    val ownerPayout: Int,
    val handoverCode: String,
    val pickupAt: LocalDateTime,
    val returnAt: LocalDateTime,
    val respondBy: LocalDateTime?,
    val checklist: Set<Int>,
    val returnChecklist: Set<Int>,
    val renterScannedAt: LocalDateTime?,
    val ownerScannedAt: LocalDateTime?,
    val renterSignedAt: LocalDateTime?,
    val ownerSignedAt: LocalDateTime?,
    val renterSignature: String?,
    val ownerSignature: String?,
    val returnedAt: LocalDateTime?,
    val rating: Int?,
    val calendarAdded: Boolean,
    val createdAt: LocalDateTime,
) {
    val days: Int get() = (end.toEpochDay() - start.toEpochDay() + 1).toInt()
    val span: Availability.Span get() = Availability.Span(start, end)
    val dates: String get() = dateRange(start, end)
    val datesLong: String get() = dateRangeLong(start, end)

    /** The bill, recomputed from the item so it can always be shown line by line. */
    val quote: Quote get() = Pricing.quote(item, days, protection, coupon > 0, includeExtras)

    /**
     * When to say it came back. A rental fast-forwarded in a test is returned
     * before its own pickup time; then the time it was due back reads true.
     */
    val returnedShownAt: LocalDateTime? get() = returnedAt?.let { if (it.isBefore(pickupAt)) returnAt else it }

    val handshakeDone: Boolean get() = renterScannedAt != null && ownerScannedAt != null
    val fullySigned: Boolean get() = renterSignedAt != null && ownerSignedAt != null
    val paid: Boolean get() = status in setOf(BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.RETURNED, BookingStatus.COMPLETED)

    /** Item code and unit, for the owner's calendar: `Trek Marlin #2`. */
    val unitLabel: String get() = if (item.units > 1) "${item.shortTitle} #$unit" else item.shortTitle
}

data class Review(
    val id: Long,
    val listingId: String,
    val subject: Person,
    val author: Person,
    val authorHandle: String,
    val entry: com.gearloop.app.data.ReviewEntry,
    val bookingRef: String?,
)

data class Notification(
    val id: Long,
    val item: NotificationItem,
    val route: String,
    val ref: String?,
    val createdAt: LocalDateTime,
)

data class Message(
    val id: Long,
    val threadId: String,
    val fromHandle: String,
    val chat: ChatMessage,
    val createdAt: LocalDateTime,
)

/** One conversation in the inbox: who it is with, and the last thing said. */
data class Conversation(
    val other: Person,
    val last: Message?,
)

data class WalletLine(val label: String, val date: String, val amount: Int)

/** A deposit sitting on the renter's card for a live booking. */
data class Hold(val booking: Booking, val releases: LocalDate)

data class Claim(
    val ref: String,
    val bookingRef: String?,
    val listingTitle: String,
    val claimant: Person,
    val respondent: Person,
    val amount: Int,
    val itemValue: Int,
    val severity: DamageMark.Severity,
    val area: String,
    val note: String,
    val status: ClaimStatus,
    val createdAt: LocalDateTime,
    val respondBy: LocalDateTime,
    val renterStatement: String?,
    val renterEvidence: String?,
    val renterPhotos: List<String>,
    val award: Int?,
    val rationale: String?,
    val decidedAt: LocalDateTime?,
    val decidedBy: String?,
) {
    /** The most this severity could ever be awarded, read off the rubric. */
    val cap: Int get() = itemValue * severity.capPercent / 100
    val decided: Boolean get() = status == ClaimStatus.DECIDED
}

data class CaptureFrame(
    val id: Long,
    val angle: String,
    val photo: String,
    val ok: Boolean,
    val capturedAt: LocalDateTime,
    val latitude: Double?,
    val longitude: Double?,
) {
    /** `9.7134°N, 80.0246°E`, or a plain statement that it isn't known. */
    val place: String
        get() = if (latitude == null || longitude == null) "LOCATION OFF"
        else com.gearloop.app.data.coordinates(latitude, longitude)
}

data class Mark(
    val id: Long,
    val phase: Phase,
    val area: String,
    val summary: String,
    val severity: DamageMark.Severity,
    val note: String,
    val x: Float,
    val y: Float,
    val photo: String?,
    val createdAt: LocalDateTime,
    val resolved: Boolean,
) {
    fun asDamageMark(recordedAt: String) = DamageMark(area, summary, severity, note, recordedAt)
}

/** One angle on S-18: what it looked like at pickup, at return, and whether it changed. */
/**
 * What the claims desk weighs: the claimed angle at pickup and on return, where
 * and when each was taken, and the checks that say whether to trust them.
 */
data class ClaimEvidence(
    val pickupPhoto: String,
    val returnPhoto: String,
    val pickupStamp: String,
    val returnStamp: String,
    val checks: List<Pair<String, Boolean>>,
)

data class AnglePair(
    val angle: String,
    val pickup: CaptureFrame?,
    val back: CaptureFrame?,
    val change: Mark?,
)

data class Badge(val name: String, val earned: Boolean, val featured: Boolean)

data class Proposal(
    val ref: String,
    val matchId: String?,
    /** Who made the offer: [give] is theirs. */
    val proposer: Person,
    val give: GearItem,
    val get: GearItem,
    val giveValue: Int,
    val getValue: Int,
    val counterparty: Person,
    val boot: Int,
    val status: ProposalStatus,
    val round: Int,
    val expiresAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    /** What the cash leaves unbalanced, relative to the more valuable side. */
    val remainingGap: Int get() = getValue - giveValue - boot
    val open: Boolean get() = status in setOf(ProposalStatus.DRAFT, ProposalStatus.SENT, ProposalStatus.COUNTERED)
}

data class Match(
    val id: String,
    val offer: com.gearloop.app.data.SwapOffer,
    val proposalRef: String?,
)

data class CycleMember(
    val person: Person,
    val gives: GearItem,
    val givesValue: Int,
    val accepted: Boolean?,
)

/** Who gives what to whom, worked out from the order of the loop (FR-13). */
data class CycleLeg(
    val giver: Person,
    val item: GearItem,
    val value: Int,
    val receiver: Person,
)

data class Cycle(val id: String, val members: List<CycleMember>) {
    val legs: List<CycleLeg>
        get() = members.mapIndexed { i, m ->
            CycleLeg(m.person, m.gives, m.givesValue, members[(i + 1) % members.size].person)
        }
    val acceptedCount: Int get() = members.count { it.accepted == true }
    val declined: Boolean get() = members.any { it.accepted == false }

    /** What a member receives: whatever the member before them in the loop gives. */
    fun receivedBy(handle: String): CycleMember? {
        val i = members.indexOfFirst { it.person.handle == handle }
        if (i < 0) return null
        return members[(i - 1 + members.size) % members.size]
    }
}

data class CalendarUnit(val item: GearItem, val unit: Int) {
    val label: String get() = if (item.units > 1) "${item.shortTitle} #$unit" else item.shortTitle
}

data class CalendarBlock(val id: Long, val listingId: String, val unit: Int, val day: LocalDate, val reason: String)

data class OwnerTask(
    val id: String,
    val severity: NeedsAttention.Severity,
    val title: String,
    val detail: String,
    val action: String,
)

/** A row in "Needs you now": a stored task, or one derived from a booking. */
data class Attention(
    val key: String,
    val item: NeedsAttention,
    val route: String,
    val taskId: String? = null,
    val bookingRef: String? = null,
)

/** A pickup or return on the owner's calendar in the next few days. */
data class ComingUp(val at: LocalDateTime, val label: String, val what: String, val ref: String)

data class ClosetItem(
    val id: Long,
    val title: String,
    val shortTitle: String,
    val photo: String,
    val sport: String,
    val sizeLabel: String,
    val grade: Grade,
    val code: String,
    val listed: Boolean,
    val listingId: String?,
    val idleDays: Long,
) {
    val tagLineCompact: String get() = "$sizeLabel · GRADE ${grade.letter}"
}

data class TripItem(
    val id: Long,
    val section: String,
    val label: String,
    val listing: GearItem?,
    val note: String,
    val price: Int,
    val added: Boolean,
)

data class Trip(
    val destination: String,
    val start: LocalDate,
    val end: LocalDate,
    val adults: Int,
    val sports: List<String>,
    val own: List<TripItem>,
    val rent: List<TripItem>,
    val extras: List<TripItem>,
) {
    val days: Int get() = (end.toEpochDay() - start.toEpochDay() + 1).toInt()
    val dates: String get() = dateRange(start, end)
    val rentalTotal: Int get() = rent.sumOf { (it.listing?.pricePerDay ?: 0) * days }
    val extrasTotal: Int get() = extras.filter { it.added }.sumOf { it.price }

    /** 10 % off the rentals once three or more come from the same trip. */
    val bundleDiscount: Int get() = if (rent.size >= 3) rentalTotal / 10 else 0
    val total: Int get() = rentalTotal + extrasTotal - bundleDiscount
    val itemCount: Int get() = rent.size + extras.count { it.added }
}

data class Alert(val id: Long, val kind: String, val summary: String, val createdAt: LocalDateTime)

/** A day in a month grid, with the state the calendar cell draws. */
data class DayCell(val date: LocalDate, val state: com.gearloop.app.data.DayState)
