package com.gearloop.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.Phase
import com.gearloop.app.data.ProposalStatus
import kotlinx.coroutines.flow.Flow

/*
 * Data access, one DAO per member area. Reads are Flows, so a screen observing
 * a table redraws when any other screen writes to it — that is what makes a
 * request accepted on S-28 appear as confirmed on S-31 without either screen
 * knowing about the other. Writes are suspend functions and never run on the
 * main thread.
 */

// ============================================================ M1 · Fit & Discovery

@Dao
interface DiscoveryDao {

    // ---- people and the signed-in profile
    @Query("SELECT * FROM person")
    fun people(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM person WHERE handle = :handle")
    suspend fun person(handle: String): PersonEntity?

    @Upsert
    suspend fun upsertPerson(person: PersonEntity)

    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profileNow(): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("UPDATE profile SET intent = :intent WHERE id = 1")
    suspend fun setIntent(intent: Int)

    @Query("UPDATE profile SET heightCm = :heightCm, weightKg = :weightKg, metric = :metric WHERE id = 1")
    suspend fun setMeasurements(heightCm: Int?, weightKg: Int?, metric: Boolean)

    // ---- sport interests (S-03)
    @Query("SELECT * FROM sport_interest ORDER BY addedAt")
    fun sports(): Flow<List<SportInterestEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSport(sport: SportInterestEntity)

    @Query("SELECT COUNT(*) FROM sport_interest WHERE sport = :sport")
    suspend fun hasSport(sport: String): Int

    @Query("DELETE FROM sport_interest WHERE sport = :sport")
    suspend fun removeSport(sport: String)

    // ---- filter sheet (S-06)
    @Query("SELECT * FROM search_prefs WHERE id = 1")
    suspend fun searchPrefs(): SearchPrefsEntity?

    @Upsert
    suspend fun saveSearchPrefs(prefs: SearchPrefsEntity)

    @Query("DELETE FROM search_prefs")
    suspend fun clearSearchPrefs()

    // ---- listings
    @Query("SELECT * FROM listing ORDER BY sortOrder, createdAt")
    fun listings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listing WHERE id = :id")
    suspend fun listing(id: String): ListingEntity?

    @Upsert
    suspend fun upsertListing(listing: ListingEntity)

    @Query("UPDATE listing SET saved = :saved WHERE id = :id")
    suspend fun setSaved(id: String, saved: Boolean)

    @Query("UPDATE listing SET status = :status WHERE id = :id")
    suspend fun setListingStatus(id: String, status: ListingStatus)

    @Query("DELETE FROM listing WHERE id = :id")
    suspend fun deleteListing(id: String)

    @Query("SELECT * FROM rail_entry ORDER BY rail, position")
    fun rails(): Flow<List<RailEntryEntity>>

    @Upsert
    suspend fun upsertRailEntry(entry: RailEntryEntity)

    @Query("UPDATE rail_entry SET position = position + 1 WHERE rail = :rail")
    suspend fun shiftRail(rail: String)

    // ---- reviews (S-42)
    @Query("SELECT * FROM review ORDER BY createdAt DESC")
    fun reviews(): Flow<List<ReviewEntity>>

    @Insert
    suspend fun insertReview(review: ReviewEntity): Long

    @Query("SELECT * FROM review WHERE id = :id")
    suspend fun review(id: Long): ReviewEntity?

    @Query("DELETE FROM review WHERE id = :id")
    suspend fun deleteReview(id: Long)

    // ---- trip planner (S-30)
    @Query("SELECT * FROM trip WHERE id = 1")
    fun trip(): Flow<TripEntity?>

    @Query("SELECT * FROM trip WHERE id = 1")
    suspend fun tripNow(): TripEntity?

    @Upsert
    suspend fun upsertTrip(trip: TripEntity)

    @Query("SELECT * FROM trip_item ORDER BY section, position")
    fun tripItems(): Flow<List<TripItemEntity>>

    @Insert
    suspend fun insertTripItem(item: TripItemEntity): Long

    @Query("SELECT * FROM trip_item WHERE id = :id")
    suspend fun tripItem(id: Long): TripItemEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM trip_item WHERE section = :section")
    suspend fun maxTripPosition(section: String): Int

    @Query("UPDATE trip_item SET added = :added WHERE id = :id")
    suspend fun setTripItemAdded(id: Long, added: Boolean)

    @Query("DELETE FROM trip_item WHERE id = :id")
    suspend fun deleteTripItem(id: Long)

    // ---- alerts and wanted posts (S-39)
    @Query("SELECT * FROM search_alert ORDER BY createdAt DESC")
    fun alerts(): Flow<List<SearchAlertEntity>>

    @Insert
    suspend fun insertAlert(alert: SearchAlertEntity): Long

    @Query("DELETE FROM search_alert WHERE id = :id")
    suspend fun deleteAlert(id: Long)
}

// ============================================================ M2 · Money & Commitment

@Dao
interface MoneyDao {

    @Query("SELECT * FROM booking ORDER BY startDay, createdAt")
    fun bookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM booking WHERE ref = :ref")
    suspend fun booking(ref: String): BookingEntity?

    @Query("SELECT * FROM booking WHERE status = 'DRAFT' AND renterHandle = :renter AND listingId = :listingId LIMIT 1")
    suspend fun draftFor(renter: String, listingId: String): BookingEntity?

    @Upsert
    suspend fun upsertBooking(booking: BookingEntity)

    @Query("UPDATE booking SET status = :status, updatedAt = :at WHERE ref = :ref")
    suspend fun setBookingStatus(ref: String, status: BookingStatus, at: Long)

    @Query("DELETE FROM booking WHERE ref = :ref")
    suspend fun deleteBooking(ref: String)

    @Query("SELECT COUNT(*) FROM booking WHERE ref = :ref")
    suspend fun bookingExists(ref: String): Int

    /** Every booking that occupies a unit's calendar, for the availability rules on a write. */
    @Query("SELECT * FROM booking WHERE listingId = :listingId AND unit = :unit AND ref != :excluding AND status IN ('REQUESTED', 'CONFIRMED', 'ACTIVE', 'RETURNED')")
    suspend fun holdingOn(listingId: String, unit: Int, excluding: String): List<BookingEntity>

    // ---- wallet (S-35)
    @Query("SELECT * FROM wallet_txn ORDER BY createdAt DESC")
    fun walletTxns(): Flow<List<WalletTxnEntity>>

    @Insert
    suspend fun insertTxn(txn: WalletTxnEntity): Long

    @Query("UPDATE profile SET walletAvailable = :amount WHERE id = 1")
    suspend fun setWalletAvailable(amount: Int)

    // ---- notifications (S-38)
    @Query("SELECT * FROM notification ORDER BY createdAt DESC")
    fun notifications(): Flow<List<NotificationEntity>>

    @Insert
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notification SET unread = 0 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notification SET unread = 0")
    suspend fun markAllRead()

    @Query("SELECT * FROM notification WHERE id = :id")
    suspend fun notification(id: Long): NotificationEntity?

    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    // ---- messages (S-33)
    @Query("SELECT * FROM message ORDER BY createdAt, id")
    fun messages(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM message WHERE id = :id")
    suspend fun message(id: Long): MessageEntity?

    @Query("DELETE FROM message WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    // ---- claims (S-34, S-40)
    @Query("SELECT * FROM claim ORDER BY createdAt DESC")
    fun claims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claim WHERE ref = :ref")
    suspend fun claim(ref: String): ClaimEntity?

    @Upsert
    suspend fun upsertClaim(claim: ClaimEntity)

    @Query("UPDATE claim SET status = :status WHERE ref = :ref")
    suspend fun setClaimStatus(ref: String, status: ClaimStatus)

    @Query("SELECT COUNT(*) FROM claim")
    suspend fun claimCount(): Int

    @Query("SELECT ref FROM claim")
    suspend fun claimRefs(): List<String>

    @Query("SELECT * FROM claim WHERE bookingRef = :bookingRef AND area = :area LIMIT 1")
    suspend fun claimFor(bookingRef: String, area: String): ClaimEntity?
}

// ============================================================ M3 · Evidence & Trust

@Dao
interface EvidenceDao {

    @Query("SELECT * FROM capture ORDER BY capturedAt")
    fun captures(): Flow<List<CaptureEntity>>

    @Upsert
    suspend fun upsertCapture(capture: CaptureEntity)

    @Query("SELECT * FROM capture WHERE bookingRef = :ref AND phase = :phase AND angle = :angle")
    suspend fun capture(ref: String, phase: Phase, angle: String): CaptureEntity?

    @Query("DELETE FROM capture WHERE bookingRef = :ref AND phase = :phase AND angle = :angle")
    suspend fun deleteCapture(ref: String, phase: Phase, angle: String)

    @Query("SELECT * FROM damage_mark ORDER BY createdAt")
    fun marks(): Flow<List<DamageMarkEntity>>

    @Insert
    suspend fun insertMark(mark: DamageMarkEntity): Long

    @Upsert
    suspend fun upsertMark(mark: DamageMarkEntity)

    @Query("SELECT * FROM damage_mark WHERE id = :id")
    suspend fun mark(id: Long): DamageMarkEntity?

    @Query("UPDATE damage_mark SET resolved = :resolved WHERE id = :id")
    suspend fun setMarkResolved(id: Long, resolved: Boolean)

    @Query("SELECT * FROM damage_mark WHERE bookingRef = :ref")
    suspend fun marksFor(ref: String): List<DamageMarkEntity>

    @Query("DELETE FROM damage_mark WHERE id = :id")
    suspend fun deleteMark(id: Long)

    @Query("SELECT * FROM verification_step ORDER BY position")
    fun verificationSteps(): Flow<List<VerificationStepEntity>>

    @Upsert
    suspend fun upsertVerificationStep(step: VerificationStepEntity)

    @Query("SELECT * FROM verification_step ORDER BY position")
    suspend fun verificationStepsNow(): List<VerificationStepEntity>

    @Query("SELECT * FROM badge ORDER BY position")
    fun badges(): Flow<List<BadgeEntity>>

    @Query("UPDATE badge SET featured = :featured WHERE name = :name AND earned = 1")
    suspend fun setBadgeFeatured(name: String, featured: Boolean)

    @Query("UPDATE profile SET address = :address WHERE id = 1")
    suspend fun setAddress(address: String?)
}

// ============================================================ M4 · Exchange & Supply

@Dao
interface ExchangeDao {

    @Query("SELECT * FROM swap_match ORDER BY position")
    fun matches(): Flow<List<SwapMatchEntity>>

    @Query("UPDATE swap_match SET hidden = :hidden WHERE id = :id")
    suspend fun setMatchHidden(id: String, hidden: Boolean)

    @Query("SELECT * FROM swap_match WHERE id = :id")
    suspend fun match(id: String): SwapMatchEntity?

    @Query("SELECT * FROM swap_proposal ORDER BY updatedAt DESC")
    fun proposals(): Flow<List<SwapProposalEntity>>

    @Query("SELECT * FROM swap_proposal WHERE ref = :ref")
    suspend fun proposal(ref: String): SwapProposalEntity?

    @Upsert
    suspend fun upsertProposal(proposal: SwapProposalEntity)

    @Query("UPDATE swap_proposal SET status = :status, updatedAt = :at WHERE ref = :ref")
    suspend fun setProposalStatus(ref: String, status: ProposalStatus, at: Long)

    @Query("DELETE FROM swap_proposal WHERE ref = :ref")
    suspend fun deleteProposal(ref: String)

    @Query("SELECT COUNT(*) FROM swap_proposal")
    suspend fun proposalCount(): Int

    @Query("SELECT ref FROM swap_proposal")
    suspend fun proposalRefs(): List<String>

    @Query("SELECT * FROM swap_proposal WHERE matchId = :matchId AND status IN ('DRAFT', 'SENT', 'COUNTERED') LIMIT 1")
    suspend fun openProposalFor(matchId: String): SwapProposalEntity?

    @Query("SELECT * FROM cycle_member ORDER BY cycleId, position")
    fun cycleMembers(): Flow<List<CycleMemberEntity>>

    @Query("UPDATE cycle_member SET accepted = :accepted WHERE cycleId = :cycleId AND handle = :handle")
    suspend fun setCycleAcceptance(cycleId: String, handle: String, accepted: Boolean?)

    // ---- owner calendar (S-27)
    @Query("SELECT * FROM calendar_block ORDER BY day")
    fun blocks(): Flow<List<CalendarBlockEntity>>

    @Insert
    suspend fun insertBlock(block: CalendarBlockEntity): Long

    @Query("DELETE FROM calendar_block WHERE id = :id")
    suspend fun deleteBlock(id: Long)

    @Query("SELECT * FROM calendar_block WHERE id = :id")
    suspend fun block(id: Long): CalendarBlockEntity?

    @Query("SELECT * FROM calendar_block WHERE listingId = :listingId AND unit = :unit")
    suspend fun blocksFor(listingId: String, unit: Int): List<CalendarBlockEntity>

    // ---- owner tasks (S-24)
    @Query("SELECT * FROM owner_task ORDER BY position")
    fun tasks(): Flow<List<OwnerTaskEntity>>

    @Query("UPDATE owner_task SET done = :done WHERE id = :id")
    suspend fun setTaskDone(id: String, done: Boolean)

    // ---- gear closet (S-36)
    @Query("SELECT * FROM closet_item ORDER BY position, id")
    fun closet(): Flow<List<ClosetItemEntity>>

    @Query("SELECT * FROM closet_item WHERE id = :id")
    suspend fun closetItem(id: Long): ClosetItemEntity?

    @Insert
    suspend fun insertClosetItem(item: ClosetItemEntity): Long

    @Upsert
    suspend fun upsertClosetItem(item: ClosetItemEntity)

    @Query("DELETE FROM closet_item WHERE id = :id")
    suspend fun deleteClosetItem(id: Long)

    @Query("SELECT COALESCE(MAX(position), 0) FROM closet_item")
    suspend fun maxClosetPosition(): Int

    @Query("SELECT * FROM closet_item WHERE listingId = :listingId LIMIT 1")
    suspend fun closetForListing(listingId: String): ClosetItemEntity?

    @Query("SELECT COUNT(*) FROM calendar_block WHERE listingId = :listingId AND unit = :unit AND day = :day")
    suspend fun blockCount(listingId: String, unit: Int, day: Long): Int

    @Query("SELECT * FROM booking WHERE listingId = :listingId AND unit = :unit AND ref != :excluding AND status IN ('CONFIRMED', 'ACTIVE', 'RETURNED')")
    suspend fun confirmedOn(listingId: String, unit: Int, excluding: String): List<BookingEntity>
}

// ============================================================ shared

/** Bulk inserts for seeding and resetting the demo. Nothing else writes through here. */
@Dao
interface SeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun people(rows: List<PersonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun profile(row: ProfileEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun sports(rows: List<SportInterestEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun listings(rows: List<ListingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun rails(rows: List<RailEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun reviews(rows: List<ReviewEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun trip(row: TripEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun tripItems(rows: List<TripItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun bookings(rows: List<BookingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun txns(rows: List<WalletTxnEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun notifications(rows: List<NotificationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun messages(rows: List<MessageEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun claims(rows: List<ClaimEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun verification(rows: List<VerificationStepEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun badges(rows: List<BadgeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun matches(rows: List<SwapMatchEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun proposals(rows: List<SwapProposalEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun cycle(rows: List<CycleMemberEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun blocks(rows: List<CalendarBlockEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun tasks(rows: List<OwnerTaskEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun closet(rows: List<ClosetItemEntity>)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM session WHERE id = 1")
    suspend fun session(): SessionEntity?

    @Upsert
    suspend fun save(session: SessionEntity)
}
