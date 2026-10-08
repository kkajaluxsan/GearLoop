package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.SearchFilters
import com.gearloop.app.data.StarDistribution
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.db.ReviewEntity
import com.gearloop.app.data.db.SearchAlertEntity
import com.gearloop.app.data.db.SearchPrefsEntity
import com.gearloop.app.data.db.SportInterestEntity
import com.gearloop.app.data.db.TripItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Member 1 — Fit & Discovery.
 *
 * S-01 Intent · S-02 Fit profile · S-03 Sports · S-04 Home · S-05 Search ·
 * S-06 Filters · S-07 Listing · S-29 Map · S-30 Trip planner · S-39 No results ·
 * S-42 Reviews.
 *
 * The profile written on S-02 is the one every fit stamp in the app reads, so
 * changing a height here re-judges the Torq on S-04, S-05, S-07 and S-28.
 */
class DiscoveryRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val scope: CoroutineScope,
) {
    private val dao = db.discovery()

    private fun <T> kotlinx.coroutines.flow.Flow<T>.hot(initial: T): StateFlow<T> =
        stateIn(scope, SharingStarted.Eagerly, initial)

    // ============================================================ reads

    val profile: StateFlow<Profile?> = store.profile

    val sports: StateFlow<List<String>> = store.sportRows.map { rows -> rows.map { it.sport } }.hot(emptyList())

    /** Everything a renter can search, in catalogue order. */
    val searchable: StateFlow<List<GearItem>> = store.published

    /** The three rails on S-04, each in the order the design set them. */
    val rails: StateFlow<Map<String, List<GearItem>>> = combine(store.railRows, store.published) { entries, items ->
        val byId = items.associateBy { it.id }
        entries.groupBy { it.rail }.mapValues { (_, rail) ->
            rail.sortedBy { it.position }.mapNotNull { byId[it.listingId] }
        }
    }.hot(emptyMap())

    val selected: StateFlow<GearItem?> = combine(session.session, store.allItems) { s, items ->
        items.firstOrNull { it.id == s.listingId } ?: items.firstOrNull()
    }.hot(null)

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
        rows.map { Alert(it.id, it.kind, it.summary, DemoClock.fromMillis(it.createdAt)) }
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
            .filter { it.id != item.id && it.owner.handle != Seed.ME }
            .sortedWith(
                compareBy<GearItem>(
                    { if (it.sport == item.sport) 0 else if (group(it.sport) == group(item.sport)) 1 else 2 },
                    { it.distanceKm },
                ),
            )
            .take(limit)
    }

    /** The owner's own published listings, for the public profile. */
    fun listingsBy(handle: String): List<GearItem> = store.published.value.filter { it.owner.handle == handle }

    // ============================================================ writes

    fun select(listingId: String) = session.selectListing(listingId)

    /** S-01 — Update. */
    suspend fun setIntent(intent: Int) = dao.setIntent(intent)

    /** S-02 — Update. Height and weight are clamped to the ranges the steppers allow. */
    suspend fun saveMeasurements(heightCm: Int, weightKg: Int, metric: Boolean) =
        dao.setMeasurements(heightCm.coerceIn(130, 215), weightKg.coerceIn(35, 160), metric)

    /** S-02 — Delete. Every verdict falls back to "add your height" (FR-03). */
    suspend fun clearMeasurements() {
        val metric = dao.profileNow()?.metric ?: true
        dao.setMeasurements(null, null, metric)
    }

    /** S-03 — Create or Delete, depending on whether it was already chosen. */
    suspend fun toggleSport(sport: String) {
        if (dao.hasSport(sport) > 0) dao.removeSport(sport)
        else dao.addSport(SportInterestEntity(sport, DemoClock.nowMillis()))
    }

    /** S-04, S-05, S-07, S-29, S-37 — Update. Read from the table, so a quick double tap toggles twice. */
    suspend fun toggleSaved(listingId: String) {
        val current = dao.listing(listingId) ?: return
        dao.setSaved(listingId, !current.saved)
    }

    // ---- S-06: the filter sheet's state, kept between launches

    suspend fun loadSearchPrefs(): SearchPrefsEntity? = dao.searchPrefs()

    suspend fun saveSearchPrefs(filters: SearchFilters) = dao.saveSearchPrefs(
        SearchPrefsEntity(
            fitOnly = filters.fitOnly,
            instantOnly = filters.instantOnly,
            delivery = filters.delivery,
            distanceIndex = filters.distanceIndex,
            priceLower = filters.priceLower,
            priceUpper = filters.priceUpper,
            query = filters.query,
            lengths = filters.lengths.toList(),
            grades = filters.grades.toList(),
        ),
    )

    suspend fun clearSearchPrefs() = dao.clearSearchPrefs()

    // ---- S-42: reviews

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
                    authorHandle = Seed.ME,
                    date = dayMonth(DemoClock.today) + " " + DemoClock.today.year,
                    stars = stars.coerceIn(1, 5),
                    body = body.trim(),
                    photos = emptyList(),
                    ownerResponse = null,
                    bookingRef = bookingRef,
                    createdAt = DemoClock.nowMillis(),
                ),
            )
            adjustRating(listing.ownerHandle, stars, +1)
            if (bookingRef != null) {
                db.money().booking(bookingRef)?.let { db.money().upsertBooking(it.copy(rating = stars, updatedAt = DemoClock.nowMillis())) }
            }
            id
        }
    }

    private val ratings = Mutex()

    /**
     * S-18 — the stars on the settlement screen are a review without words:
     * Create one, Update it by picking again, or Delete it with null. It goes
     * through [addReview] and [deleteReview], so S-31, S-42 and the owner's
     * histogram all agree with it.
     */
    suspend fun rateBooking(bookingRef: String, stars: Int?) = ratings.withLock {
        val booking = db.money().booking(bookingRef) ?: return@withLock
        dao.reviews().first().firstOrNull { it.bookingRef == bookingRef && it.authorHandle == Seed.ME }
            ?.let { deleteReview(it.id) }
        if (stars != null) addReview(booking.listingId, stars, "", bookingRef)
    }

    /** Delete one of your own reviews, and take it back out of the histogram. */
    suspend fun deleteReview(id: Long) {
        val review = dao.review(id) ?: return
        if (review.authorHandle != Seed.ME) return
        db.withTransaction {
            dao.deleteReview(id)
            adjustRating(review.subjectHandle, review.stars, -1)
            review.bookingRef?.let { ref ->
                db.money().booking(ref)?.let { db.money().upsertBooking(it.copy(rating = null, updatedAt = DemoClock.nowMillis())) }
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

    // ---- S-30: the trip planner

    suspend fun setTripAdults(adults: Int) {
        val t = dao.tripNow() ?: return
        dao.upsertTrip(t.copy(adults = adults.coerceIn(1, 8)))
    }

    suspend fun toggleTripSport(sport: String) {
        val t = dao.tripNow() ?: return
        val next = if (sport in t.sports) t.sports - sport else t.sports + sport
        dao.upsertTrip(t.copy(sports = next))
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
        val position = dao.maxTripPosition("RENT") + 1
        dao.insertTripItem(TripItemEntity(section = "RENT", label = item.shortTitle, listingId = listingId, note = note, price = 0, added = true, position = position))
    }

    // ---- S-39: alerts and wanted posts

    suspend fun createAlert(kind: String, summary: String): Long =
        dao.insertAlert(SearchAlertEntity(kind = kind, summary = summary, createdAt = DemoClock.nowMillis()))

    suspend fun deleteAlert(id: Long) = dao.deleteAlert(id)
}
