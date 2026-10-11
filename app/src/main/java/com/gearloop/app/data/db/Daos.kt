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
 * Data access, one DAO per area. Reads are Flows, so a screen observing a table
 * redraws when any other screen writes to it — that is what makes a request
 * accepted in the owner's inbox appear as confirmed in the renter's rentals
 * without either screen knowing about the other. Writes are suspend functions
 * and never run on the main thread.
 *
 * Tables that belong to one member are read by handle on the write paths; the
 * observing flows read every row and the store narrows them to whoever is signed
 * in, because the dataset is small and the narrowing has to follow sign-in.
 */

// ============================================================ accounts

@Dao
interface AccountDao {

    @Query("SELECT * FROM account ORDER BY lastLoginAt DESC")
    fun accounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE email = :email LIMIT 1")
    suspend fun byEmail(email: String): AccountEntity?

    @Query("SELECT * FROM account WHERE handle = :handle")
    suspend fun byHandle(handle: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM account")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query("UPDATE account SET lastLoginAt = :at WHERE handle = :handle")
    suspend fun touch(handle: String, at: Long)

    @Query("DELETE FROM account WHERE handle = :handle")
    suspend fun delete(handle: String)

    // ---- what deleting an account removes: everything that was only theirs
    @Query("DELETE FROM sport_interest WHERE handle = :handle") suspend fun deleteSports(handle: String)
    @Query("DELETE FROM search_prefs WHERE handle = :handle") suspend fun deletePrefs(handle: String)
    @Query("DELETE FROM saved_listing WHERE handle = :handle") suspend fun deleteSaved(handle: String)
    @Query("DELETE FROM trip WHERE handle = :handle") suspend fun deleteTrip(handle: String)
    @Query("DELETE FROM trip_item WHERE handle = :handle") suspend fun deleteTripItems(handle: String)
    @Query("DELETE FROM search_alert WHERE handle = :handle") suspend fun deleteAlerts(handle: String)
    @Query("DELETE FROM wallet_txn WHERE handle = :handle") suspend fun deleteTxns(handle: String)
    @Query("DELETE FROM notification WHERE handle = :handle") suspend fun deleteNotifications(handle: String)
    @Query("DELETE FROM verification_step WHERE handle = :handle") suspend fun deleteVerification(handle: String)
    @Query("DELETE FROM badge WHERE handle = :handle") suspend fun deleteBadges(handle: String)
    @Query("DELETE FROM swap_match WHERE handle = :handle") suspend fun deleteMatches(handle: String)
    @Query("DELETE FROM closet_item WHERE ownerHandle = :handle") suspend fun deleteCloset(handle: String)
    @Query("DELETE FROM owner_task WHERE handle = :handle") suspend fun deleteTasks(handle: String)
    @Query("UPDATE listing SET status = 'PAUSED' WHERE ownerHandle = :handle") suspend fun pauseListings(handle: String)
}

// ============================================================ discovery

@Dao
interface DiscoveryDao {

    // ---- people and profiles
    @Query("SELECT * FROM person")
    fun people(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM person WHERE handle = :handle")
    suspend fun person(handle: String): PersonEntity?

    @Query("SELECT COUNT(*) FROM person WHERE handle = :handle")
    suspend fun personExists(handle: String): Int

    @Upsert
    suspend fun upsertPerson(person: PersonEntity)

    @Query("SELECT * FROM profile")
    fun profiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile WHERE handle = :handle")
    suspend fun profileNow(handle: String): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("UPDATE profile SET intent = :intent WHERE handle = :handle")
    suspend fun setIntent(handle: String, intent: Int)

    @Query("UPDATE profile SET heightCm = :heightCm, weightKg = :weightKg, metric = :metric WHERE handle = :handle")
    suspend fun setMeasurements(handle: String, heightCm: Int?, weightKg: Int?, metric: Boolean)

    @Query("UPDATE profile SET onboarded = 1 WHERE handle = :handle")
    suspend fun setOnboarded(handle: String)

    @Query("DELETE FROM profile WHERE handle = :handle")
    suspend fun deleteProfile(handle: String)

    // ---- sport interests
    @Query("SELECT * FROM sport_interest ORDER BY addedAt")
    fun sports(): Flow<List<SportInterestEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSport(sport: SportInterestEntity)

    @Query("SELECT COUNT(*) FROM sport_interest WHERE handle = :handle AND sport = :sport")
    suspend fun hasSport(handle: String, sport: String): Int

    @Query("DELETE FROM sport_interest WHERE handle = :handle AND sport = :sport")
    suspend fun removeSport(handle: String, sport: String)

    // ---- filter sheet
    @Query("SELECT * FROM search_prefs WHERE handle = :handle")
    suspend fun searchPrefs(handle: String): SearchPrefsEntity?

    @Upsert
    suspend fun saveSearchPrefs(prefs: SearchPrefsEntity)

    @Query("DELETE FROM search_prefs WHERE handle = :handle")
    suspend fun clearSearchPrefs(handle: String)

    // ---- listings
    @Query("SELECT * FROM listing ORDER BY sortOrder, createdAt")
    fun listings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listing WHERE id = :id")
    suspend fun listing(id: String): ListingEntity?

    @Query("SELECT * FROM listing WHERE ownerHandle = :handle")
    suspend fun listingsOf(handle: String): List<ListingEntity>

    @Query("SELECT COUNT(*) FROM listing WHERE code = :code")
    suspend fun codeCount(code: String): Int

    @Query("SELECT * FROM listing WHERE code = :code LIMIT 1")
    suspend fun listingByCode(code: String): ListingEntity?

    @Upsert
    suspend fun upsertListing(listing: ListingEntity)

    @Query("UPDATE listing SET status = :status WHERE id = :id")
    suspend fun setListingStatus(id: String, status: ListingStatus)

    @Query("DELETE FROM listing WHERE id = :id")
    suspend fun deleteListing(id: String)

    // ---- saved listings
    @Query("SELECT * FROM saved_listing")
    fun saved(): Flow<List<SavedListingEntity>>

    @Query("SELECT COUNT(*) FROM saved_listing WHERE handle = :handle AND listingId = :listingId")
    suspend fun isSaved(handle: String, listingId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: SavedListingEntity)

    @Query("DELETE FROM saved_listing WHERE handle = :handle AND listingId = :listingId")
    suspend fun unsave(handle: String, listingId: String)

    // ---- reviews
    @Query("SELECT * FROM review ORDER BY createdAt DESC")
    fun reviews(): Flow<List<ReviewEntity>>

    @Insert
    suspend fun insertReview(review: ReviewEntity): Long

    @Query("SELECT * FROM review WHERE id = :id")
    suspend fun review(id: Long): ReviewEntity?

    @Query("SELECT * FROM review WHERE bookingRef = :ref AND authorHandle = :author LIMIT 1")
    suspend fun reviewFor(ref: String, author: String): ReviewEntity?

    @Query("DELETE FROM review WHERE id = :id")
    suspend fun deleteReview(id: Long)

    // ---- trip planner
    @Query("SELECT * FROM trip")
    fun trips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trip WHERE handle = :handle")
    suspend fun tripNow(handle: String): TripEntity?

    @Upsert
    suspend fun upsertTrip(trip: TripEntity)

    @Query("SELECT * FROM trip_item ORDER BY section, position")
    fun tripItems(): Flow<List<TripItemEntity>>

    @Insert
    suspend fun insertTripItem(item: TripItemEntity): Long

    @Query("SELECT * FROM trip_item WHERE id = :id")
    suspend fun tripItem(id: Long): TripItemEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM trip_item WHERE handle = :handle AND section = :section")
    suspend fun maxTripPosition(handle: String, section: String): Int

    @Query("UPDATE trip_item SET added = :added WHERE id = :id")
    suspend fun setTripItemAdded(id: Long, added: Boolean)

    @Query("DELETE FROM trip_item WHERE id = :id")
    suspend fun deleteTripItem(id: Long)

    // ---- alerts and wanted posts
    @Query("SELECT * FROM search_alert ORDER BY createdAt DESC")
    fun alerts(): Flow<List<SearchAlertEntity>>

    @Insert
    suspend fun insertAlert(alert: SearchAlertEntity): Long

    @Query("DELETE FROM search_alert WHERE id = :id")
    suspend fun deleteAlert(id: Long)
}

// ============================================================ money

@Dao
interface MoneyDao {

    @Query("SELECT * FROM booking ORDER BY startDay, createdAt")
    fun bookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM booking WHERE ref = :ref")
    suspend fun booking(ref: String): BookingEntity?

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

    /** Returns still waiting on the owner's review that came back before [before]. */
    @Query("SELECT * FROM booking WHERE status = 'RETURNED' AND returnedAt IS NOT NULL AND returnedAt < :before")
    suspend fun returnsAwaitingReviewSince(before: Long): List<BookingEntity>

    /** Requests the owner did not answer in time. */
    @Query("SELECT * FROM booking WHERE status = 'REQUESTED' AND respondBy IS NOT NULL AND respondBy < :now")
    suspend fun expiredRequests(now: Long): List<BookingEntity>

    // ---- wallet
    @Query("SELECT * FROM wallet_txn ORDER BY createdAt DESC, id DESC")
    fun walletTxns(): Flow<List<WalletTxnEntity>>

    @Insert
    suspend fun insertTxn(txn: WalletTxnEntity): Long

    @Query("UPDATE profile SET walletAvailable = :amount WHERE handle = :handle")
    suspend fun setWalletAvailable(handle: String, amount: Int)

    // ---- notifications
    @Query("SELECT * FROM notification ORDER BY createdAt DESC, id DESC")
    fun notifications(): Flow<List<NotificationEntity>>

    @Insert
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notification SET unread = 0 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notification SET unread = 0 WHERE handle = :handle")
    suspend fun markAllRead(handle: String)

    @Query("SELECT * FROM notification WHERE id = :id")
    suspend fun notification(id: Long): NotificationEntity?

    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    // ---- messages
    @Query("SELECT * FROM message ORDER BY createdAt, id")
    fun messages(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM message WHERE id = :id")
    suspend fun message(id: Long): MessageEntity?

    @Query("DELETE FROM message WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    // ---- claims
    @Query("SELECT * FROM claim ORDER BY createdAt DESC")
    fun claims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claim WHERE ref = :ref")
    suspend fun claim(ref: String): ClaimEntity?

    @Upsert
    suspend fun upsertClaim(claim: ClaimEntity)

    @Query("UPDATE claim SET status = :status WHERE ref = :ref")
    suspend fun setClaimStatus(ref: String, status: ClaimStatus)

    @Query("SELECT ref FROM claim")
    suspend fun claimRefs(): List<String>

    @Query("SELECT * FROM claim WHERE bookingRef = :bookingRef AND area = :area LIMIT 1")
    suspend fun claimFor(bookingRef: String, area: String): ClaimEntity?
}

// ============================================================ evidence

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

    @Query("SELECT * FROM verification_step ORDER BY handle, position")
    fun verificationSteps(): Flow<List<VerificationStepEntity>>

    @Upsert
    suspend fun upsertVerificationStep(step: VerificationStepEntity)

    @Query("SELECT * FROM verification_step WHERE handle = :handle ORDER BY position")
    suspend fun verificationStepsNow(handle: String): List<VerificationStepEntity>

    @Query("SELECT * FROM badge ORDER BY handle, position")
    fun badges(): Flow<List<BadgeEntity>>

    @Query("UPDATE badge SET featured = :featured WHERE handle = :handle AND name = :name AND earned = 1")
    suspend fun setBadgeFeatured(handle: String, name: String, featured: Boolean)

    @Query("UPDATE badge SET earned = 1 WHERE handle = :handle AND name = :name")
    suspend fun earnBadge(handle: String, name: String)

    @Query("UPDATE profile SET address = :address WHERE handle = :handle")
    suspend fun setAddress(handle: String, address: String?)
}

// ============================================================ exchange & supply

@Dao
interface ExchangeDao {

    @Query("SELECT * FROM swap_match ORDER BY position")
    fun matches(): Flow<List<SwapMatchEntity>>

    @Query("SELECT * FROM swap_match WHERE handle = :handle")
    suspend fun matchesOf(handle: String): List<SwapMatchEntity>

    @Upsert
    suspend fun upsertMatch(match: SwapMatchEntity)

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

    @Query("SELECT ref FROM swap_proposal")
    suspend fun proposalRefs(): List<String>

    @Query("SELECT * FROM swap_proposal WHERE matchId = :matchId AND status IN ('DRAFT', 'SENT', 'COUNTERED') LIMIT 1")
    suspend fun openProposalFor(matchId: String): SwapProposalEntity?

    @Query("SELECT * FROM cycle_member ORDER BY cycleId, position")
    fun cycleMembers(): Flow<List<CycleMemberEntity>>

    @Query("UPDATE cycle_member SET accepted = :accepted WHERE cycleId = :cycleId AND handle = :handle")
    suspend fun setCycleAcceptance(cycleId: String, handle: String, accepted: Boolean?)

    // ---- owner calendar
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

    @Query("SELECT COUNT(*) FROM calendar_block WHERE listingId = :listingId AND unit = :unit AND day = :day")
    suspend fun blockCount(listingId: String, unit: Int, day: Long): Int

    @Query("SELECT * FROM booking WHERE listingId = :listingId AND unit = :unit AND ref != :excluding AND status IN ('CONFIRMED', 'ACTIVE', 'RETURNED')")
    suspend fun confirmedOn(listingId: String, unit: Int, excluding: String): List<BookingEntity>

    // ---- owner tasks
    @Query("SELECT * FROM owner_task ORDER BY position")
    fun tasks(): Flow<List<OwnerTaskEntity>>

    @Query("UPDATE owner_task SET done = :done WHERE id = :id")
    suspend fun setTaskDone(id: String, done: Boolean)

    // ---- gear closet
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

    @Query("SELECT COALESCE(MAX(position), 0) FROM closet_item WHERE ownerHandle = :handle")
    suspend fun maxClosetPosition(handle: String): Int

    @Query("SELECT * FROM closet_item WHERE listingId = :listingId LIMIT 1")
    suspend fun closetForListing(listingId: String): ClosetItemEntity?
}

// ============================================================ seeding

/** Bulk inserts for the sample marketplace a new install starts with. */
@Dao
interface SeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun accounts(rows: List<AccountEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun people(rows: List<PersonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun profiles(rows: List<ProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun sports(rows: List<SportInterestEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun listings(rows: List<ListingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun reviews(rows: List<ReviewEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun trips(rows: List<TripEntity>)
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
