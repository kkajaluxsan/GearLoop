package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Fit
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.SearchFilters
import com.gearloop.app.data.StarDistribution
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.db.ReviewEntity
import com.gearloop.app.data.db.SavedListingEntity
import com.gearloop.app.data.db.SearchAlertEntity
import com.gearloop.app.data.db.SearchPrefsEntity
import com.gearloop.app.data.db.SportInterestEntity
import com.gearloop.app.data.db.TripItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Fit & Discovery: the getting-started screens, home, search, filters, the
 * listing, the map, the trip planner, alerts and reviews.
 *
 * The profile written on the fit screen is the one every fit stamp in the app
 * reads, so changing a height re-judges every listing on every screen.
 */
class DiscoveryRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val scope: CoroutineScope,
) {
    private val dao = db.discovery()
    private val me: String get() = session.handle

    private fun <T> kotlinx.coroutines.flow.Flow<T>.hot(initial: T): StateFlow<T> =
        stateIn(scope, SharingStarted.Eagerly, initial)

    // ============================================================ reads

    val profile: StateFlow<Profile?> = store.profile

    val sports: StateFlow<List<String>> = store.sportRows.map { rows -> rows.map { it.sport } }.hot(emptyList())

    /** Everything a member can search, in catalogue order. */
    val searchable: StateFlow<List<GearItem>> = store.published

    /**
     * The three rails on home, worked out from the listings themselves rather
     * than curated: what is free near you, what fits you, and what is new in the
     * sport you picked first.
     */
    val rails: StateFlow<Map<String, List<GearItem>>> = combine(store.published, sports) { items, picked ->
        val near = items.filter { it.available }.sortedBy { it.distanceKm }.take(8)
        val fits = items.filter { it.fit in setOf(Fit.PERFECT, Fit.GOOD) }
            .sortedWith(compareBy({ it.fit.ordinal }, { it.distanceKm })).take(8)
        val sport = railSport(picked, items)
        val fresh = items.filter { it.sport == sport }.sortedByDescending { createdAt(it.id) }.take(8)
        mapOf("weekend" to near, "fits" to fits, "new" to fresh)
    }.hot(emptyMap())

    /** The sport the "New in" rail is about: the first one the member picked that has listings. */
    val railSportName: StateFlow<String> = combine(store.published, sports) { items, picked -> railSport(picked, items) }.hot("Surfing")

    private fun railSport(picked: List<String>, items: List<GearItem>): String =
        picked.firstOrNull { s -> items.any { it.sport == s } } ?: "Surfing"

    private fun createdAt(id: String): Long = store.listingRows.value.firstOrNull { it.id == id }?.createdAt ?: 0

    val selected: StateFlow<GearItem?> = combine(session.session, store.allItems) { s, items ->
        items.firstOrNull { it.id == s.listingId } ?: items.firstOrNull()
    }.hot(null)

    val saved: StateFlow<List<GearItem>> = store.allItems.map { list -> list.filter { it.saved } }.hot(emptyList())

    val unreadNotifications: StateFlow<Int> =
        store.notificationRows.map { rows -> rows.count { it.unread } }.hot(0)

    val reviews: StateFlow<List<Review>> = store.reviews

    val trip: StateFlow<Trip?> = combine(store.tripRow, store.tripItemRows, store.allItems) { t, rows, items ->
        t?.let {
            val byId = items.associateBy { i -> i.id }
            fun map(section: String) = rows.filter { r -> r.section == section }.map { r ->
                TripItem(r.id, r.section, r.label, r.listingId?.let(byId::get), r.note, r.price, r.added)
            }
            Trip(
                destination = it.destination,
                start = LocalDate.ofEpochDay(it.startDay),
                end = LocalDate.ofEpochDay(it.endDay),
                adults = it.adults,
                sports = it.sports,
                own = map("OWN"),
                rent = map("RENT"),
                extras = map("ADD"),
            )
        }
    }.hot(null)

    val alerts: StateFlow<List<Alert>> = store.alertRows.map { rows ->
        rows.map { Alert(it.id, it.kind, it.summary, AppClock.fromMillis(it.createdAt)) }
    }.hot(emptyList())

    fun item(id: String): GearItem? = store.item(id)

    fun distributionOf(handle: String): StarDistribution? = store.distributionOf(handle)

    /**
     * Gear like this one: the same sport first, then the same kind of water or
     * land, then whatever is nearest — four of them, never the listing itself.
     */
    fun similarTo(item: GearItem, limit: Int = 4): List<GearItem> {
        val water = setOf("Surfing", "SUP", "Kayaking", "Swimming")
        fun group(s: String) = if (s in water) "water" else s
        return store.published.value
            .filter { it.id != item.id }
            .sortedWith(
                compareBy<GearItem>(
                    { if (it.sport == item.sport) 0 else if (group(it.sport) == group(item.sport)) 1 else 2 },
                    { it.distanceKm },
                ),
            )
            .take(limit)
    }

    /** A member's own published listings, for their public profile. */
    fun listingsBy(handle: String): List<GearItem> =
        store.allItems.value.filter { it.owner.handle == handle && it.status == com.gearloop.app.data.ListingStatus.PUBLISHED }

    // ============================================================ writes

    fun select(listingId: String) = session.selectListing(listingId)

    suspend fun setIntent(intent: Int) = dao.setIntent(me, intent)

    /** Height and weight are clamped to the ranges the steppers allow. */
    suspend fun saveMeasurements(heightCm: Int, weightKg: Int, metric: Boolean) =
        dao.setMeasurements(me, heightCm.coerceIn(130, 215), weightKg.coerceIn(35, 160), metric)

    /** Every verdict falls back to "add your height" (FR-03). */
    suspend fun clearMeasurements() {
        val metric = dao.profileNow(me)?.metric ?: true
        dao.setMeasurements(me, null, null, metric)
    }

    /** The getting-started screens are done; the next launch opens on home. */
    suspend fun finishOnboarding() = dao.setOnboarded(me)

    /** Create or Delete, depending on whether it was already chosen. */
    suspend fun toggleSport(sport: String) {
        if (dao.hasSport(me, sport) > 0) dao.removeSport(me, sport)
        else dao.addSport(SportInterestEntity(me, sport, AppClock.nowMillis()))
    }

    /** Read from the table, so a quick double tap toggles twice. */
    suspend fun toggleSaved(listingId: String) {
        if (dao.isSaved(me, listingId) > 0) dao.unsave(me, listingId)
        else dao.save(SavedListingEntity(me, listingId, AppClock.nowMillis()))
    }

    // ---- the filter sheet's state, kept between launches

    suspend fun loadSearchPrefs(): SearchPrefsEntity? = dao.searchPrefs(me)

    suspend fun saveSearchPrefs(filters: SearchFilters) {
        if (me.isEmpty()) return
        dao.saveSearchPrefs(
            SearchPrefsEntity(
                handle = me,
                fitOnly = filters.fitOnly,
                instantOnly = filters.instantOnly,
                delivery = filters.delivery,
                distanceIndex = filters.distanceIndex,
                priceLower = filters.priceLower,
                priceUpper = filters.priceUpper,
                query = filters.query,
                lengths = filters.lengths.toList(),
                grades = filters.grades.toList(),
                sports = filters.sports.toList(),
            ),
        )
    }

    suspend fun clearSearchPrefs() = dao.clearSearchPrefs(me)

    // ---- reviews

    /**
     * Create a review. The subject's histogram moves with it, so the average and
     * the count on every screen that quotes them move too.
     */
    suspend fun addReview(listingId: String, stars: Int, body: String, bookingRef: String?): Long {
        val listing = dao.listing(listingId) ?: return -1
        return db.withTransaction {
            val id = dao.insertReview(
                ReviewEntity(
                    listingId = listingId,
                    subjectHandle = listing.ownerHandle,
                    authorHandle = me,
                    date = dayMonth(AppClock.today) + " " + AppClock.today.year,
                    stars = stars.coerceIn(1, 5),
                    body = body.trim(),
                    photos = emptyList(),
                    ownerResponse = null,
                    bookingRef = bookingRef,
                    createdAt = AppClock.nowMillis(),
                ),
            )
            adjustRating(listing.ownerHandle, stars, +1)
            if (bookingRef != null) {
                db.money().booking(bookingRef)?.let { db.money().upsertBooking(it.copy(rating = stars, updatedAt = AppClock.nowMillis())) }
            }
            id
        }
    }

    private val ratings = Mutex()

    /**
     * The stars on the settlement screen are a review without words: Create
     * one, Update it by picking again, or Delete it with null.
     */
    suspend fun rateBooking(bookingRef: String, stars: Int?) = ratings.withLock {
        val booking = db.money().booking(bookingRef) ?: return@withLock
        dao.reviewFor(bookingRef, me)?.let { deleteReview(it.id) }
        if (stars != null) addReview(booking.listingId, stars, "", bookingRef)
    }

    /** Delete one of your own reviews, and take it back out of the histogram. */
    suspend fun deleteReview(id: Long) {
        val review = dao.review(id) ?: return
        if (review.authorHandle != me) return
        db.withTransaction {
            dao.deleteReview(id)
            adjustRating(review.subjectHandle, review.stars, -1)
            review.bookingRef?.let { ref ->
                db.money().booking(ref)?.let { db.money().upsertBooking(it.copy(rating = null, updatedAt = AppClock.nowMillis())) }
            }
        }
    }

    private suspend fun adjustRating(handle: String, stars: Int, delta: Int) {
        val person = dao.person(handle) ?: return
        val updated = if (person.starCounts.size == 5) {
            val counts = person.starCounts.toMutableList()
            val i = 5 - stars.coerceIn(1, 5)
            counts[i] = (counts[i] + delta).coerceAtLeast(0)
            person.copy(starCounts = counts)
        } else {
            val n = (person.reviews + delta).coerceAtLeast(0)
            val sum = person.rating * person.reviews + stars * delta
            person.copy(reviews = n, rating = if (n == 0) 0.0 else sum / n)
        }
        dao.upsertPerson(updated)
    }

    // ---- the trip planner

    suspend fun setTripAdults(adults: Int) {
        val t = dao.tripNow(me) ?: return
        dao.upsertTrip(t.copy(adults = adults.coerceIn(1, 8)))
    }

    suspend fun toggleTripSport(sport: String) {
        val t = dao.tripNow(me) ?: return
        val next = if (sport in t.sports) t.sports - sport else t.sports + sport
        dao.upsertTrip(t.copy(sports = next))
    }

    suspend fun setTripDestination(destination: String) {
        val t = dao.tripNow(me) ?: return
        if (destination.isBlank()) return
        dao.upsertTrip(t.copy(destination = destination.trim()))
    }

    /** Move the trip to start on [start] and last [days] days. Never in the past. */
    suspend fun setTripDates(start: LocalDate, days: Int) {
        val t = dao.tripNow(me) ?: return
        val from = if (start.isBefore(AppClock.today)) AppClock.today else start
        val length = days.coerceIn(1, 21)
        dao.upsertTrip(t.copy(startDay = from.toEpochDay(), endDay = from.plusDays(length - 1L).toEpochDay()))
    }

    /** Put a "worth adding" item in the kit, or take it out again. */
    suspend fun setTripExtraAdded(id: Long, added: Boolean) = dao.setTripItemAdded(id, added)

    /** Delete a rental from the kit. Returns the row so it can be put back. */
    suspend fun removeTripItem(id: Long): TripItemEntity? {
        val row = dao.tripItem(id) ?: return null
        dao.deleteTripItem(id)
        return row
    }

    suspend fun restoreTripItem(row: TripItemEntity) {
        dao.insertTripItem(row.copy(id = 0))
    }

    /** Create a rental in the kit from a listing. */
    suspend fun addTripRental(listingId: String, note: String) {
        val item = dao.listing(listingId) ?: return
        val position = dao.maxTripPosition(me, "RENT") + 1
        dao.insertTripItem(TripItemEntity(handle = me, section = "RENT", label = item.shortTitle, listingId = listingId, note = note, price = 0, added = true, position = position))
    }

    // ---- alerts and wanted posts

    suspend fun createAlert(kind: String, summary: String): Long =
        dao.insertAlert(SearchAlertEntity(handle = me, kind = kind, summary = summary, createdAt = AppClock.nowMillis()))

    suspend fun deleteAlert(id: Long) = dao.deleteAlert(id)
}
