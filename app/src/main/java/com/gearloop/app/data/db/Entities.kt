package com.gearloop.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gearloop.app.data.AccountRole
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
 * Every member has an account, and everything that belongs to one member —
 * their profile, wallet, notifications, closet, saved gear, trip, alerts,
 * verification steps and badges — carries that member's handle, so the phone
 * can hold several accounts and each one sees only its own. What two members
 * share — a booking, a swap, a claim, a conversation — names both of them.
 *
 *   accounts                 account, person, profile
 *   discovery                sport_interest, search_prefs, listing, saved_listing,
 *                            review, trip, trip_item, search_alert
 *   money                    booking, wallet_txn, notification, message, claim
 *   evidence & trust         capture, damage_mark, verification_step, badge
 *   exchange & supply        swap_match, swap_proposal, cycle_member,
 *                            calendar_block, owner_task, closet_item
 *   this device              session
 *
 * Times are epoch millis on the [com.gearloop.app.data.AppClock]; days are
 * epoch days, because a booking covers whole days and a day compares cleanly.
 */

// ============================================================ accounts

/** How a member signs in. The password is never stored, only its salted hash. */
@Entity(tableName = "account", indices = [Index(value = ["email"], unique = true)])
data class AccountEntity(
    @PrimaryKey val handle: String,
    /** Lower-cased, so sign-in is not case sensitive. */
    val email: String,
    val passwordHash: String,
    val salt: String,
    val phone: String?,
    val role: AccountRole,
    val createdAt: Long,
    val lastLoginAt: Long?,
)

/** A member as other members see them. */
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

/** A member's own settings and measurements. One row per account. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val handle: String,
    /** 0 rent, 1 earn, 2 swap, 3 all of it. */
    val intent: Int,
    val heightCm: Int?,
    val weightKg: Int?,
    val shoeEu: Int,
    val metric: Boolean,
    val couponUsed: Boolean,
    val walletAvailable: Int,
    val address: String?,
    /** The three getting-started screens have been finished or skipped. */
    val onboarded: Boolean,
    /** The saved card: brand and last four digits only, never the number. */
    val cardBrand: String?,
    val cardLast4: String?,
    val cardExpiry: String?,
)

// ============================================================ discovery

@Entity(tableName = "sport_interest", primaryKeys = ["handle", "sport"])
data class SportInterestEntity(
    val handle: String,
    val sport: String,
    val addedAt: Long,
)

/** The filter sheet's last state, so it survives the app being closed. */
@Entity(tableName = "search_prefs")
data class SearchPrefsEntity(
    @PrimaryKey val handle: String,
    val fitOnly: Boolean,
    val instantOnly: Boolean,
    val delivery: Boolean,
    val distanceIndex: Int,
    val priceLower: Float,
    val priceUpper: Float,
    val query: String,
    val lengths: List<String>,
    val grades: List<String>,
    val sports: List<String>,
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
    val description: String,
    val createdAt: Long,
    val sortOrder: Int,
)

/** A listing a member has hearted. */
@Entity(tableName = "saved_listing", primaryKeys = ["handle", "listingId"])
data class SavedListingEntity(
    val handle: String,
    val listingId: String,
    val savedAt: Long,
)

@Entity(tableName = "review", indices = [Index("listingId"), Index("subjectHandle")])
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listingId: String,
    /** The member being reviewed. */
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
    @PrimaryKey val handle: String,
    val destination: String,
    val startDay: Long,
    val endDay: Long,
    val adults: Int,
    val sports: List<String>,
)

@Entity(tableName = "trip_item", indices = [Index("handle")])
data class TripItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val handle: String,
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

/** "Alert me" and "Post what you're looking for". */
@Entity(tableName = "search_alert", indices = [Index("handle")])
data class SearchAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val handle: String,
    /** ALERT or WANTED. */
    val kind: String,
    val summary: String,
    val createdAt: Long,
)

// ============================================================ money

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
    /** 0 card, 1 GearLoop wallet, 2 cash at pickup. */
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
    /** When the gear tag was checked against the booking at pickup. */
    val handshakeRenterAt: Long?,
    val handshakeOwnerAt: Long?,
    val renterSignedAt: Long?,
    val ownerSignedAt: Long?,
    /** The two signatures on the pickup record, as drawn on the screen. */
    val renterSignature: String?,
    val ownerSignature: String?,
    val returnedAt: Long?,
    val rating: Int?,
    val calendarAdded: Boolean,
    val hiddenFromHistory: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "wallet_txn", indices = [Index("handle")])
data class WalletTxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val handle: String,
    val label: String,
    val amount: Int,
    val createdAt: Long,
)

@Entity(tableName = "notification", indices = [Index("handle")])
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Who it is for. */
    val handle: String,
    val kind: NotificationItem.Kind,
    val title: String,
    val body: String,
    val unread: Boolean,
    val createdAt: Long,
    /** Where tapping it goes, and what it should select on the way. */
    val route: String,
    val ref: String?,
)

/**
 * One message. A conversation between two members is keyed by both handles
 * (see `MoneyRepository.dmKey`); a swap negotiation by its proposal reference.
 */
@Entity(tableName = "message", indices = [Index("threadId")])
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: String,
    /** Who sent it; [com.gearloop.app.data.repo.MoneyRepository.SYSTEM] for booking events. */
    val fromHandle: String,
    val body: String,
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
    /** Photos the renter added to their side of the claim. */
    val renterPhotos: List<String>,
    val award: Int?,
    val rationale: String?,
    val decidedAt: Long?,
    val decidedBy: String?,
)

// ============================================================ evidence

/** One frame of the guided condition record. */
@Entity(tableName = "capture", indices = [Index(value = ["bookingRef", "phase", "angle"], unique = true)])
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingRef: String,
    val phase: Phase,
    val angle: String,
    /** The photo taken with the camera, see [com.gearloop.app.data.PhotoStore]. */
    val photoKey: String,
    /** Framing passed the capture-time check (FR-08). */
    val ok: Boolean,
    val capturedAt: Long,
    /** Where it was taken, when the phone allowed the app to know. */
    val latitude: Double?,
    val longitude: Double?,
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

@Entity(tableName = "verification_step", primaryKeys = ["handle", "position"])
data class VerificationStepEntity(
    val handle: String,
    val position: Int,
    val title: String,
    val unlocks: String,
    val state: VerificationStep.State,
    val completedAt: Long?,
    /** What the step recorded: a phone number, a photo of the ID, the address. */
    val detail: String?,
)

@Entity(tableName = "badge", primaryKeys = ["handle", "name"])
data class BadgeEntity(
    val handle: String,
    val name: String,
    val earned: Boolean,
    /** Shown on the public profile. */
    val featured: Boolean,
    val position: Int,
)

// ============================================================ exchange & supply

/** A trade the matcher found for one member's gear. */
@Entity(tableName = "swap_match", indices = [Index("handle")])
data class SwapMatchEntity(
    @PrimaryKey val id: String,
    /** Whose feed it is in. */
    val handle: String,
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
    /** Who made the offer. [giveListingId] is theirs, [getListingId] is the counterparty's. */
    val proposerHandle: String,
    val giveListingId: String,
    val getListingId: String,
    val giveValue: Int,
    val getValue: Int,
    val counterpartyHandle: String,
    /** Cash the proposer adds to their side. */
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

@Entity(tableName = "owner_task", indices = [Index("handle")])
data class OwnerTaskEntity(
    @PrimaryKey val id: String,
    /** The owner it is for. */
    val handle: String,
    val severity: NeedsAttention.Severity,
    val title: String,
    val detail: String,
    val action: String,
    val done: Boolean,
    val position: Int,
)

/** A member's own kit, listed or not. */
@Entity(tableName = "closet_item", indices = [Index("ownerHandle")])
data class ClosetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerHandle: String,
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

// ============================================================ this device

/**
 * This phone's state: who is signed in, what they have open, and the
 * appearance setting. One row.
 */
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    /** The signed-in member, or null when nobody is. */
    val currentHandle: String?,
    /** The welcome pages have been seen on this phone. */
    val welcomeSeen: Boolean,
    /** 0 follow the system, 1 light, 2 dark. */
    val themeMode: Int,
    val listingId: String,
    val bookingRef: String?,
    val proposalRef: String?,
    val claimRef: String?,
    val threadId: String,
    val draftListingId: String?,
    val capturePhase: Phase,
    val bookingSeq: Int,
)
