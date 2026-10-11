package com.gearloop.app.data.repo

import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.Extra
import com.gearloop.app.data.FitLogic
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Person
import com.gearloop.app.data.Photos
import com.gearloop.app.data.ReviewEntry
import com.gearloop.app.data.SpecRow
import com.gearloop.app.data.StarDistribution
import com.gearloop.app.data.TrustTier
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.db.BookingEntity
import com.gearloop.app.data.db.CalendarBlockEntity
import com.gearloop.app.data.db.ClaimEntity
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.db.ListingEntity
import com.gearloop.app.data.db.PersonEntity
import com.gearloop.app.data.db.ProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * The read side of the data layer.
 *
 * Every table is observed once, here, as a hot [StateFlow] owned by the
 * application rather than by a screen. The dataset is small, so holding it in
 * memory costs nothing, and it buys two things: a screen has its data on the
 * first frame instead of flashing empty, and two screens looking at the same
 * booking are looking at the same object.
 *
 * Rows become domain objects in one place, narrowed to whoever is signed in
 * ([me]) — their profile, their saved listings, their notifications — so a
 * derived fact (a listing's live fit verdict, whether it is free today, a
 * person's rating read off their histogram) is derived once and agrees
 * everywhere it is shown.
 */
class GearStore(private val db: GearDatabase, private val session: SessionStore, private val scope: CoroutineScope) {

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    private val discovery = db.discovery()
    private val money = db.money()
    private val evidence = db.evidence()
    private val exchange = db.exchange()

    /** The signed-in member, or "" when signed out. */
    val me: StateFlow<String> = session.me

    // ------------------------------------------------------------ raw tables

    val peopleRows = discovery.people().hot(emptyList())
    val profileRows = discovery.profiles().hot(emptyList())
    val sportRowsAll = discovery.sports().hot(emptyList())
    val listingRows = discovery.listings().hot(emptyList())
    val savedRows = discovery.saved().hot(emptyList())
    val reviewRows = discovery.reviews().hot(emptyList())
    val tripRows = discovery.trips().hot(emptyList())
    val tripItemRowsAll = discovery.tripItems().hot(emptyList())
    val alertRowsAll = discovery.alerts().hot(emptyList())

    val bookingRows = money.bookings().hot(emptyList())
    val txnRowsAll = money.walletTxns().hot(emptyList())
    val notificationRowsAll = money.notifications().hot(emptyList())
    val messageRows = money.messages().hot(emptyList())
    val claimRows = money.claims().hot(emptyList())

    val captureRows = evidence.captures().hot(emptyList())
    val markRows = evidence.marks().hot(emptyList())
    val verificationRowsAll = evidence.verificationSteps().hot(emptyList())
    val badgeRowsAll = evidence.badges().hot(emptyList())

    val matchRowsAll = exchange.matches().hot(emptyList())
    val proposalRows = exchange.proposals().hot(emptyList())
    val cycleRows = exchange.cycleMembers().hot(emptyList())
    val blockRows = exchange.blocks().hot(emptyList())
    val taskRowsAll = exchange.tasks().hot(emptyList())
    val closetRowsAll = exchange.closet().hot(emptyList())

    // ------------------------------------------------------------ mine only

    val profileRow: StateFlow<ProfileEntity?> = combine(me, profileRows) { h, rows -> rows.firstOrNull { it.handle == h } }.hot(null)
    val sportRows = combine(me, sportRowsAll) { h, rows -> rows.filter { it.handle == h } }.hot(emptyList())
    val tripRow = combine(me, tripRows) { h, rows -> rows.firstOrNull { it.handle == h } }.hot(null)
    val tripItemRows = combine(me, tripItemRowsAll) { h, rows -> rows.filter { it.handle == h } }.hot(emptyList())
    val alertRows = combine(me, alertRowsAll) { h, rows -> rows.filter { it.handle == h } }.hot(emptyList())
    val txnRows = combine(me, txnRowsAll) { h, rows -> rows.filter { it.handle == h } }.hot(emptyList())
    val notificationRows = combine(me, notificationRowsAll) { h, rows -> rows.filter { it.handle == h } }.hot(emptyList())
    val verificationRows = combine(me, verificationRowsAll) { h, rows -> rows.filter { it.handle == h }.sortedBy { it.position } }.hot(emptyList())
    val badgeRows = combine(me, badgeRowsAll) { h, rows -> rows.filter { it.handle == h }.sortedBy { it.position } }.hot(emptyList())
    val matchRows = combine(me, matchRowsAll) { h, rows -> rows.filter { it.handle == h } }.hot(emptyList())
    val taskRows = combine(me, taskRowsAll) { h, rows -> rows.filter { it.handle == h } }.hot(emptyList())
    val closetRows = combine(me, closetRowsAll) { h, rows -> rows.filter { it.ownerHandle == h } }.hot(emptyList())

    private val savedIds: StateFlow<Set<String>> =
        combine(me, savedRows) { h, rows -> rows.filter { it.handle == h }.map { it.listingId }.toSet() }.hot(emptySet())

    // ------------------------------------------------------------ people

    /** Everyone, with their listing count read off the listings they really have up. */
    val people: StateFlow<Map<String, Person>> = combine(peopleRows, listingRows) { rows, listings ->
        val live = listings.filter { it.status == ListingStatus.PUBLISHED }.groupingBy { it.ownerHandle }.eachCount()
        rows.associate { it.handle to it.toPerson(live[it.handle] ?: 0) }
    }.hot(emptyMap())

    fun person(handle: String): Person = people.value[handle] ?: unknownPerson(handle)

    val profile: StateFlow<Profile?> = combine(profileRow, people) { row, map ->
        row?.let {
            Profile(
                person = map[it.handle] ?: unknownPerson(it.handle),
                intent = it.intent,
                heightCm = it.heightCm,
                weightKg = it.weightKg,
                shoeEu = it.shoeEu,
                metric = it.metric,
                couponUsed = it.couponUsed,
                walletAvailable = it.walletAvailable,
                address = it.address,
                onboarded = it.onboarded,
                card = if (it.cardLast4 != null && it.cardBrand != null) SavedCard(it.cardBrand, it.cardLast4, it.cardExpiry.orEmpty()) else null,
            )
        }
    }.hot(null)

    // ------------------------------------------------------------ listings

    private val itemsWithoutDates: StateFlow<List<Triple<ListingEntity, Person, Boolean>>> =
        combine(listingRows, people, savedIds) { listings, map, saved ->
            listings.map { Triple(it, map[it.ownerHandle] ?: unknownPerson(it.ownerHandle), it.id in saved) }
        }.hot(emptyList())

    /**
     * Every listing as the screens draw it: owner resolved, photos resolved, the
     * fit verdict worked out against the signed-in member's profile, and
     * availability worked out from the bookings that actually hold today.
     */
    val allItems: StateFlow<List<GearItem>> =
        combine(itemsWithoutDates, profileRow, bookingRows, blockRows) { rows, prof, bookings, blocks ->
            val today = AppClock.today
            rows.map { (row, owner, saved) -> row.toItem(owner, saved, prof, bookings, blocks, today) }
        }.hot(emptyList())

    /** What a member can find: published, and not their own (D-26). */
    val published: StateFlow<List<GearItem>> = combine(allItems, me) { list, h ->
        list.filter { it.status == ListingStatus.PUBLISHED && it.owner.handle != h }
    }.hot(emptyList())

    fun item(id: String): GearItem? = allItems.value.firstOrNull { it.id == id }

    // ------------------------------------------------------------ bookings

    val bookings: StateFlow<List<Booking>> = combine(bookingRows, allItems, people) { rows, items, map ->
        val byId = items.associateBy { it.id }
        rows.mapNotNull { row -> byId[row.listingId]?.let { row.toBooking(it, map) } }
    }.hot(emptyList())

    fun booking(ref: String?): Booking? = ref?.let { r -> bookings.value.firstOrNull { it.ref == r } }

    /** The days a unit is held, for the availability rules. */
    fun spansFor(listingId: String, unit: Int?, excluding: String? = null): List<Availability.Span> =
        bookingRows.value
            .filter { it.listingId == listingId && it.status.holdsDates && it.ref != excluding }
            .filter { unit == null || it.unit == unit }
            .map { Availability.Span(LocalDate.ofEpochDay(it.startDay), LocalDate.ofEpochDay(it.endDay)) }

    fun blockedFor(listingId: String, unit: Int?): Set<LocalDate> =
        blockRows.value
            .filter { it.listingId == listingId && (unit == null || it.unit == unit) }
            .map { LocalDate.ofEpochDay(it.day) }
            .toSet()

    /**
     * The same two facts read straight from the tables. A write that decides
     * whether dates are free must not trust a flow that may be one write behind
     * — two bookings made a second apart would both see the dates as open.
     */
    suspend fun spansNow(listingId: String, unit: Int, excluding: String = ""): List<Availability.Span> =
        db.money().holdingOn(listingId, unit, excluding)
            .map { Availability.Span(LocalDate.ofEpochDay(it.startDay), LocalDate.ofEpochDay(it.endDay)) }

    suspend fun blocksNow(listingId: String, unit: Int): Set<LocalDate> =
        db.exchange().blocksFor(listingId, unit).map { LocalDate.ofEpochDay(it.day) }.toSet()

    // ------------------------------------------------------------ claims, chat

    val claims: StateFlow<List<Claim>> = combine(claimRows, people) { rows, map ->
        rows.map { it.toClaim(map) }
    }.hot(emptyList())

    /** Every message, with "from me" worked out for whoever is signed in. */
    val messages: StateFlow<List<Message>> = combine(messageRows, me) { rows, h ->
        rows.map {
            val at = AppClock.fromMillis(it.createdAt)
            Message(it.id, it.threadId, it.fromHandle, ChatMessage(it.body, com.gearloop.app.data.chatTime(at), it.fromHandle == h, it.kind), at)
        }
    }.hot(emptyList())

    val notifications: StateFlow<List<Notification>> = notificationRows.map { rows ->
        val now = AppClock.now()
        rows.map {
            val at = AppClock.fromMillis(it.createdAt)
            Notification(it.id, NotificationItem(it.kind, it.title, it.body, com.gearloop.app.data.relativeTime(at, now), it.unread), it.route, it.ref, at)
        }
    }.hot(emptyList())

    val reviews: StateFlow<List<Review>> = combine(reviewRows, people) { rows, map ->
        rows.map {
            val author = map[it.authorHandle] ?: unknownPerson(it.authorHandle)
            Review(
                id = it.id,
                listingId = it.listingId,
                subject = map[it.subjectHandle] ?: unknownPerson(it.subjectHandle),
                author = author,
                authorHandle = it.authorHandle,
                entry = ReviewEntry(author, it.date, it.stars, it.body, it.photos, it.ownerResponse),
                bookingRef = it.bookingRef,
            )
        }
    }.hot(emptyList())

    /** Wait until the database has been read into memory, so the first frame is not empty. */
    suspend fun awaitLoaded() {
        allItems.first { it.isNotEmpty() }
        people.first { it.isNotEmpty() }
        if (session.signedIn) profile.first { it != null }
    }

    // ============================================================ mapping

    private fun unknownPerson(handle: String) =
        Person(handle.removePrefix("@"), handle, "", TrustTier.BRONZE, 0.0, 0, "")

    private fun PersonEntity.toPerson(liveListings: Int): Person {
        // Where a histogram exists the rating and count are read off it, so the
        // bars, the average and the "N reviews" label cannot disagree.
        val dist = starCounts.takeIf { it.size == 5 }?.let(::StarDistribution)
        return Person(
            name = name,
            handle = handle,
            area = area,
            tier = tier,
            rating = dist?.average ?: rating,
            reviews = dist?.total ?: reviews,
            respondsIn = respondsIn,
            listings = liveListings,
            memberSince = memberSince,
        )
    }

    fun distributionOf(handle: String): StarDistribution? =
        peopleRows.value.firstOrNull { it.handle == handle }?.starCounts?.takeIf { it.size == 5 }?.let(::StarDistribution)

    fun trustStats(handle: String): Triple<Int, Int, Int> =
        peopleRows.value.firstOrNull { it.handle == handle }
            ?.let { Triple(it.completedRentals, it.onTimeReturns, it.verifiedSteps) }
            ?: Triple(0, 100, 0)

    private fun ListingEntity.toItem(
        owner: Person,
        saved: Boolean,
        profile: ProfileEntity?,
        bookings: List<BookingEntity>,
        blocks: List<CalendarBlockEntity>,
        today: LocalDate,
    ): GearItem {
        // A draft shows the photos it really has, so the listing flow can count
        // them; anything a renter can see always has at least the sport's picture.
        val photoKeys = when {
            photos.isNotEmpty() -> photos
            status == ListingStatus.DRAFT -> emptyList()
            else -> listOf(Photos.forSport(sport))
        }

        // Free today if at least one unit is free today. Multi-unit listings
        // only go dark when every unit is out.
        val held = bookings.filter { it.listingId == id && it.status.holdsDates }
        val blocked = blocks.filter { it.listingId == id }
        val freeUnits = (1..units).filter { u ->
            val spans = held.filter { it.unit == u }.map { Availability.Span(LocalDate.ofEpochDay(it.startDay), LocalDate.ofEpochDay(it.endDay)) }
            val days = blocked.filter { it.unit == u }.map { LocalDate.ofEpochDay(it.day) }.toSet()
            spans.none { today in it } && today !in days
        }
        val available = freeUnits.isNotEmpty()
        val nextFree = if (available) null else {
            val spans = held.map { Availability.Span(LocalDate.ofEpochDay(it.startDay), LocalDate.ofEpochDay(it.endDay)) }
            Availability.nextFreeDay(today, spans, blocked.map { LocalDate.ofEpochDay(it.day) }.toSet())
                ?.let { "NEXT FREE ${dayMonth(it).uppercase(java.util.Locale.ENGLISH)}" }
        }

        val base = GearItem(
            id = id,
            title = title,
            shortTitle = shortTitle,
            code = code,
            sizeLabel = sizeLabel,
            grade = grade,
            pricePerDay = pricePerDay,
            distanceKm = distanceKm,
            photos = photoKeys,
            owner = owner,
            fit = fit,
            sport = sport,
            instantBook = instantBook,
            delivery = delivery,
            deposit = deposit,
            available = available,
            nextFree = nextFree,
            saved = saved,
            status = status,
            units = units,
            volumeL = volumeL,
            frameBand = frameBand,
            specs = specs.map { s ->
                val parts = s.split("|")
                SpecRow(parts[0], parts.getOrElse(1) { "" }, parts.getOrElse(2) { "0" } == "1")
            },
            included = included,
            excluded = excluded,
            conditionPhotos = conditionPhotos.ifEmpty { photoKeys.take(3) },
            conditionNote = conditionNote,
            conditionDate = conditionDate,
            extras = extras.map { e -> e.split("|").let { Extra(it[0], it[1].toInt()) } },
            discounts = discounts,
            waiveDepositWithPlus = waiveDepositWithPlus,
            description = description,
        )
        return base.copy(fit = FitLogic.verdict(base, profile?.heightCm, profile?.weightKg))
    }

    private fun BookingEntity.toBooking(item: GearItem, people: Map<String, Person>): Booking {
        val start = LocalDate.ofEpochDay(startDay)
        val end = LocalDate.ofEpochDay(endDay)
        fun t(ms: Long?) = ms?.let(AppClock::fromMillis)
        return Booking(
            ref = ref,
            item = item,
            unit = unit,
            renter = people[renterHandle] ?: unknownPerson(renterHandle),
            owner = people[ownerHandle] ?: unknownPerson(ownerHandle),
            start = start,
            end = end,
            protection = protection,
            includeExtras = includeExtras,
            coupon = coupon,
            paymentMethod = paymentMethod,
            waiverName = waiverName,
            waiverAt = t(waiverAt),
            status = status,
            total = total,
            depositHold = depositHold,
            ownerPayout = ownerPayout,
            handoverCode = handoverCode,
            pickupAt = Seed.dayAt(start, pickupTime),
            returnAt = Seed.dayAt(end, returnTime),
            respondBy = t(respondBy),
            checklist = checklist.toSet(),
            returnChecklist = returnChecklist.toSet(),
            renterScannedAt = t(handshakeRenterAt),
            ownerScannedAt = t(handshakeOwnerAt),
            renterSignedAt = t(renterSignedAt),
            ownerSignedAt = t(ownerSignedAt),
            renterSignature = renterSignature,
            ownerSignature = ownerSignature,
            returnedAt = t(returnedAt),
            rating = rating,
            calendarAdded = calendarAdded,
            createdAt = AppClock.fromMillis(createdAt),
        )
    }

    private fun ClaimEntity.toClaim(people: Map<String, Person>) = Claim(
        ref = ref,
        bookingRef = bookingRef,
        listingTitle = listingTitle,
        claimant = people[claimantHandle] ?: unknownPerson(claimantHandle),
        respondent = people[respondentHandle] ?: unknownPerson(respondentHandle),
        amount = amount,
        itemValue = itemValue,
        severity = severity,
        area = area,
        note = note,
        status = status,
        createdAt = AppClock.fromMillis(createdAt),
        respondBy = AppClock.fromMillis(respondBy),
        renterStatement = renterStatement,
        renterEvidence = renterEvidence,
        renterPhotos = renterPhotos,
        award = award,
        rationale = rationale,
        decidedAt = decidedAt?.let(AppClock::fromMillis),
        decidedBy = decidedBy,
    )

    /** Live bookings that count against a renter's card. */
    fun isLive(status: BookingStatus) = status in setOf(BookingStatus.REQUESTED, BookingStatus.CONFIRMED, BookingStatus.ACTIVE)
}
