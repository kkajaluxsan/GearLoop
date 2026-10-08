package com.gearloop.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Fit
import com.gearloop.app.data.Grade
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Phase
import com.gearloop.app.data.ProposalStatus
import com.gearloop.app.data.Protection
import com.gearloop.app.data.TrustTier
import com.gearloop.app.data.VerificationStep

/*
 * The tables.
 *
 * One file, grouped by the member whose interfaces own each table, so the schema
 * can be read in the order the report presents it:
 *
 *   M1 Fit & Discovery       person, profile, sport_interest, search_prefs,
 *                            listing, rail_entry, review, trip, trip_item,
 *                            search_alert
 *   M2 Money & Commitment    booking, wallet_txn, notification, message, claim
 *   M3 Evidence & Trust      capture, damage_mark, verification_step, badge
 *   M4 Exchange & Supply     swap_match, swap_proposal, cycle_member,
 *                            calendar_block, owner_task, closet_item
 *   shared                   session
 *
 * Times are epoch millis on the [com.gearloop.app.data.DemoClock]; days are
 * epoch days, because a booking covers whole days and a day compares cleanly.
 */

// ============================================================ M1 · discovery

@Entity(tableName = "person")
data class PersonEntity(
    @PrimaryKey val handle: String,
    val name: String,
    val area: String,
    val tier: TrustTier,
    val rating: Double,
    val reviews: Int,
    val respondsIn: String,
    val listings: Int,
    val memberSince: String,
    /** Five counts, five-star first. Present only where a histogram is shown. */
    val starCounts: List<Int> = emptyList(),
    val completedRentals: Int = 0,
    val onTimeReturns: Int = 100,
    val verifiedSteps: Int = 0,
)

/** The signed-in renter. One row. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val handle: String,
    /** S-01: 0 rent, 1 earn, 2 swap, 3 all of it. */
    val intent: Int,
    val heightCm: Int?,
    val weightKg: Int?,
    val shoeEu: Int,
    val metric: Boolean,
    val couponUsed: Boolean,
    val walletAvailable: Int,
    val address: String?,
)

@Entity(tableName = "sport_interest")
data class SportInterestEntity(
    @PrimaryKey val sport: String,
    val addedAt: Long,
)

/** The filter sheet's last state, so it survives the app being closed. */
@Entity(tableName = "search_prefs")
data class SearchPrefsEntity(
    @PrimaryKey val id: Int = 1,
    val fitOnly: Boolean,
    val instantOnly: Boolean,
    val delivery: Boolean,
    val distanceIndex: Int,
    val priceLower: Float,
    val priceUpper: Float,
    val query: String,
    val lengths: List<String>,
    val grades: List<String>,
)

@Entity(tableName = "listing", indices = [Index("ownerHandle"), Index("status")])
data class ListingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val shortTitle: String,
    val code: String,
    val sizeLabel: String,
    val grade: Grade,
    val pricePerDay: Int,
    val distanceKm: Double,
    /** Photo keys, see [com.gearloop.app.data.Photos]. */
    val photos: List<String>,
    val ownerHandle: String,
    /** The verdict at the reference measurements; the live one is derived. */
    val fit: Fit,
    val sport: String,
    val instantBook: Boolean,
    val delivery: Boolean,
    val deposit: Int,
    val saved: Boolean,
    val status: ListingStatus,
    val units: Int,
    val volumeL: Int?,
    val frameBand: String?,
    /** `label|value|1` — the trailing flag marks a row that affects fit. */
    val specs: List<String>,
    val included: List<String>,
    val excluded: List<String>,
    val conditionPhotos: List<String>,
    val conditionNote: String,
    val conditionDate: String,
    /** `name|perDay` */
    val extras: List<String>,
    val discounts: List<Boolean>,
    val waiveDepositWithPlus: Boolean,
    val createdAt: Long,
    val sortOrder: Int,
)

/** Which listings the home screen's rails show, and in what order. */
@Entity(tableName = "rail_entry", primaryKeys = ["rail", "listingId"])
data class RailEntryEntity(
    val rail: String,
    val listingId: String,
    val position: Int,
)

@Entity(tableName = "review", indices = [Index("listingId"), Index("subjectHandle")])
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listingId: String,
    /** The owner being reviewed. */
    val subjectHandle: String,
    val authorHandle: String,
    val date: String,
    val stars: Int,
    val body: String,
    val photos: List<String>,
    val ownerResponse: String?,
    val bookingRef: String?,
    val createdAt: Long,
)

@Entity(tableName = "trip")
data class TripEntity(
    @PrimaryKey val id: Int = 1,
    val destination: String,
    val startDay: Long,
    val endDay: Long,
    val adults: Int,
    val sports: List<String>,
)

@Entity(tableName = "trip_item")
data class TripItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** OWN, RENT or ADD. */
    val section: String,
    val label: String,
    val listingId: String?,
    val note: String,
    /** A flat price for an ADD item; RENT items are priced from their listing. */
    val price: Int,
    /** For ADD items: whether it has been put in the kit. */
    val added: Boolean,
    val position: Int,
)

/** "Alert me" and "Post what you're looking for", from S-39. */
@Entity(tableName = "search_alert")
data class SearchAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ALERT or WANTED. */
    val kind: String,
    val summary: String,
    val createdAt: Long,
)

// ============================================================ M2 · money

@Entity(
    tableName = "booking",
    indices = [Index("listingId"), Index("renterHandle"), Index("ownerHandle"), Index("status")],
)
data class BookingEntity(
    @PrimaryKey val ref: String,
    val listingId: String,
    val unit: Int,
    val renterHandle: String,
    val ownerHandle: String,
    val startDay: Long,
    val endDay: Long,
    val protection: Protection,
    val includeExtras: Boolean,
    val coupon: Int,
    val paymentMethod: Int,
    val waiverName: String?,
    val waiverAt: Long?,
    val status: BookingStatus,
    val total: Int,
    val depositHold: Int,
    val ownerPayout: Int,
    val handoverCode: String,
    val pickupTime: String,
    val returnTime: String,
    /** When the owner has to answer a request by. */
    val respondBy: Long?,
    val checklist: List<Int>,
    val returnChecklist: List<Int>,
    val handshakeRenterAt: Long?,
    val handshakeOwnerAt: Long?,
    val renterSignedAt: Long?,
    val ownerSignedAt: Long?,
    val returnedAt: Long?,
    val rating: Int?,
    val calendarAdded: Boolean,
    val hiddenFromHistory: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "wallet_txn")
data class WalletTxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amount: Int,
    val createdAt: Long,
)

@Entity(tableName = "notification")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: NotificationItem.Kind,
    val title: String,
    val body: String,
    val unread: Boolean,
    val createdAt: Long,
    /** Where tapping it goes, and what it should select on the way. */
    val route: String,
    val ref: String?,
)

@Entity(tableName = "message", indices = [Index("threadId")])
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: String,
    val body: String,
    val fromMe: Boolean,
    val kind: ChatMessage.Kind,
    val createdAt: Long,
)

@Entity(tableName = "claim")
data class ClaimEntity(
    @PrimaryKey val ref: String,
    val bookingRef: String?,
    val listingTitle: String,
    val claimantHandle: String,
    val respondentHandle: String,
    val amount: Int,
    val itemValue: Int,
    val severity: DamageMark.Severity,
    val area: String,
    val note: String,
    val status: ClaimStatus,
    val createdAt: Long,
    val respondBy: Long,
    val renterStatement: String?,
    val renterEvidence: String?,
    val award: Int?,
    val rationale: String?,
    val decidedAt: Long?,
    val decidedBy: String?,
)

// ============================================================ M3 · evidence

/** One frame of the guided condition record. */
@Entity(tableName = "capture", indices = [Index(value = ["bookingRef", "phase", "angle"], unique = true)])
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingRef: String,
    val phase: Phase,
    val angle: String,
    val photoKey: String,
    /** Framing passed the capture-time check (FR-08). */
    val ok: Boolean,
    val capturedAt: Long,
    val latitude: Double,
    val longitude: Double,
)

@Entity(tableName = "damage_mark", indices = [Index("bookingRef")])
data class DamageMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingRef: String,
    val phase: Phase,
    val area: String,
    val summary: String,
    val severity: DamageMark.Severity,
    val note: String,
    /** Where the pin sits on the outline, as fractions of its width and height. */
    val x: Float,
    val y: Float,
    val photoKey: String?,
    val createdAt: Long,
    /** A return difference both parties agreed was not new. */
    val resolved: Boolean,
)

@Entity(tableName = "verification_step")
data class VerificationStepEntity(
    @PrimaryKey val position: Int,
    val title: String,
    val unlocks: String,
    val state: VerificationStep.State,
    val completedAt: Long?,
)

@Entity(tableName = "badge")
data class BadgeEntity(
    @PrimaryKey val name: String,
    val earned: Boolean,
    /** Shown on the public profile. */
    val featured: Boolean,
    val position: Int,
)

// ============================================================ M4 · exchange & supply

@Entity(tableName = "swap_match")
data class SwapMatchEntity(
    @PrimaryKey val id: String,
    val giveListingId: String,
    val getListingId: String,
    val giveValue: Int,
    val getValue: Int,
    val counterpartyHandle: String,
    val threeWay: Boolean,
    val hidden: Boolean,
    val position: Int,
)

@Entity(tableName = "swap_proposal")
data class SwapProposalEntity(
    @PrimaryKey val ref: String,
    val matchId: String?,
    val giveListingId: String,
    val getListingId: String,
    val giveValue: Int,
    val getValue: Int,
    val counterpartyHandle: String,
    val boot: Int,
    val status: ProposalStatus,
    val round: Int,
    val expiresAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A seat in a multi-party swap, and whether its owner has accepted. */
@Entity(tableName = "cycle_member", primaryKeys = ["cycleId", "handle"])
data class CycleMemberEntity(
    val cycleId: String,
    val handle: String,
    val givesListingId: String,
    val givesValue: Int,
    /** Null while undecided. */
    val accepted: Boolean?,
    val position: Int,
)

@Entity(tableName = "calendar_block", indices = [Index("listingId")])
data class CalendarBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listingId: String,
    val unit: Int,
    val day: Long,
    val reason: String,
)

@Entity(tableName = "owner_task")
data class OwnerTaskEntity(
    @PrimaryKey val id: String,
    val severity: NeedsAttention.Severity,
    val title: String,
    val detail: String,
    val action: String,
    val done: Boolean,
    val position: Int,
)

/** The renter's own kit, listed or not (S-36). */
@Entity(tableName = "closet_item")
data class ClosetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val shortTitle: String,
    val photoKey: String,
    val sport: String,
    val sizeLabel: String,
    val grade: Grade,
    val code: String,
    val listed: Boolean,
    /** The marketplace listing made from it, once it has been listed. */
    val listingId: String?,
    val idleSince: Long,
    val position: Int,
)

// ============================================================ shared

/**
 * What the user has selected, persisted so it survives the process being killed
 * — and the anchor that starts the demo clock.
 */
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    val listingId: String,
    val bookingRef: String?,
    val proposalRef: String?,
    val claimRef: String?,
    val threadId: String,
    val draftListingId: String?,
    val capturePhase: Phase,
    val clockAnchor: Long,
    val bookingSeq: Int,
)
