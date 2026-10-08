package com.gearloop.app.data.repo

import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.Extra
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Person
import com.gearloop.app.data.Phase
import com.gearloop.app.data.Photos
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.ProposalStatus
import com.gearloop.app.data.Protection
import com.gearloop.app.data.SpecRow
import com.gearloop.app.data.db.BadgeEntity
import com.gearloop.app.data.db.BookingEntity
import com.gearloop.app.data.db.CalendarBlockEntity
import com.gearloop.app.data.db.ClaimEntity
import com.gearloop.app.data.db.ClosetItemEntity
import com.gearloop.app.data.db.CycleMemberEntity
import com.gearloop.app.data.db.ListingEntity
import com.gearloop.app.data.db.MessageEntity
import com.gearloop.app.data.db.NotificationEntity
import com.gearloop.app.data.db.OwnerTaskEntity
import com.gearloop.app.data.db.PersonEntity
import com.gearloop.app.data.db.ProfileEntity
import com.gearloop.app.data.db.RailEntryEntity
import com.gearloop.app.data.db.ReviewEntity
import com.gearloop.app.data.db.SessionEntity
import com.gearloop.app.data.db.SportInterestEntity
import com.gearloop.app.data.db.SwapMatchEntity
import com.gearloop.app.data.db.SwapProposalEntity
import com.gearloop.app.data.db.TripEntity
import com.gearloop.app.data.db.TripItemEntity
import com.gearloop.app.data.db.VerificationStepEntity
import com.gearloop.app.data.db.WalletTxnEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The world the database starts in, built from [Demo].
 *
 * [Demo] stays the one place the facts are written: the Torq's price, the
 * reviews, the waiver, the valuations. This file decides which of those facts
 * become rows, and adds what a working app needs that a prototype never did —
 * bookings with real dates, so availability can be computed rather than drawn.
 *
 * Three things are deliberately *not* seeded, because the first walk through
 * Flow A creates them: booking GL-7K3M9, its payment, and the messages and
 * notification that follow it. Seeding them would mean the rent flow books a
 * board the renter already has.
 *
 * Where the prototype contradicted itself, one owner per item wins. The NSP SUP
 * is Ayesha's in search and Kavitha's in the owner calendar; here it is
 * Ayesha's throughout, and the requests and calendar rows that used it now use
 * Kavitha's own kayak. Those changes are logged in the deviation table.
 */
object Seed {

    // ------------------------------------------------------------ handles

    const val ME = "@nirmal_r"
    const val OWNER = "@kavitha_s"
    const val ADJUDICATOR = "ops"

    val today: LocalDate get() = DemoClock.WORLD_START.toLocalDate()

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Long =
        DemoClock.toMillis(day.atTime(hour, minute))

    private fun ago(minutes: Long): Long = DemoClock.toMillis(DemoClock.WORLD_START.minusMinutes(minutes))

    private fun aug(day: Int): LocalDate = LocalDate.of(2026, 8, day)
    private fun jul(day: Int): LocalDate = LocalDate.of(2026, 7, day)
    private fun jun(day: Int): LocalDate = LocalDate.of(2026, 6, day)
    private fun sep(day: Int): LocalDate = LocalDate.of(2026, 9, day)

    // ------------------------------------------------------------ people

    private fun Person.toEntity(
        starCounts: List<Int> = emptyList(),
        completed: Int = 0,
        onTime: Int = 100,
        verified: Int = 0,
    ) = PersonEntity(
        handle = handle,
        name = name,
        area = area,
        tier = tier,
        rating = rating,
        reviews = reviews,
        respondsIn = respondsIn,
        listings = listings,
        memberSince = memberSince,
        starCounts = starCounts,
        completedRentals = completed,
        onTimeReturns = onTime,
        verifiedSteps = verified,
    )

    fun people(): List<PersonEntity> = listOf(
        Demo.nirmal.toEntity(completed = 14, onTime = 100, verified = 5),
        // 14 rentals is what S-37 shows on her public profile.
        Demo.kavitha.toEntity(starCounts = Demo.starDistribution.counts, completed = 14, verified = 6),
        Demo.dinesh.toEntity(completed = 12, onTime = 92, verified = 5),
        // The requests inbox's trust breakdown, per renter.
        Demo.thusara.toEntity(completed = 8, onTime = 100, verified = 5),
        Demo.ayesha.toEntity(completed = 21, onTime = 100, verified = 6),
        Demo.rizwan.toEntity(completed = 3, onTime = 67, verified = 3),
    )

    fun profile() = ProfileEntity(
        handle = ME,
        intent = 0,
        heightCm = Demo.HEIGHT_CM,
        weightKg = Demo.WEIGHT_KG,
        shoeEu = Demo.SHOE_EU,
        metric = true,
        couponUsed = false,
        walletAvailable = 6_480,
        address = null,
    )

    fun sports(): List<SportInterestEntity> =
        listOf("Surfing", "Cycling", "SUP").mapIndexed { i, s -> SportInterestEntity(s, ago(10_000L - i)) }

    // ------------------------------------------------------------ listings

    private fun spec(label: String, value: String, fit: Boolean = false) = SpecRow(label, value, fit)

    /** What the prototype showed only for the Torq, written for every item. */
    private data class Detail(
        val specs: List<SpecRow>,
        val included: List<String>,
        val excluded: List<String> = emptyList(),
        val conditionNote: String,
        val conditionDate: String,
        val volumeL: Int? = null,
        val frameBand: String? = null,
        val units: Int = 1,
        val extras: List<Extra> = emptyList(),
        val conditionPhotos: List<Int>? = null,
    )

    private val details: Map<String, Detail> = mapOf(
        "torq" to Detail(
            specs = Demo.torqSpecs,
            included = Demo.torqIncluded,
            excluded = Demo.torqExcluded,
            conditionNote = "Used 47 days · last serviced 2 Jul",
            conditionDate = "12 Jul 2026",
            volumeL = 42,
            extras = listOf(Extra("Rash guard", Demo.EXTRA_RASHGUARD / Demo.BOOKING_DAYS)),
            conditionPhotos = Demo.torqConditionPhotos,
        ),
        "trek" to Detail(
            specs = listOf(
                spec("Frame", "M · 52 cm", true), spec("Wheels", "29\""),
                spec("Drivetrain", "1 × 10"), spec("Brakes", "Hydraulic disc"), spec("Year", "2024"),
            ),
            included = listOf("Bike", "Lights", "Lock", "Repair kit"),
            excluded = listOf("Helmet (rent separately)"),
            conditionNote = "Used 31 days · last serviced 28 Jul",
            conditionDate = "28 Jul 2026",
            frameBand = "M",
            units = 2,
        ),
        "nsp" to Detail(
            specs = listOf(
                spec("Length", "10'6\" · 320 cm", true), spec("Width", "32\""),
                spec("Construction", "Inflatable"), spec("Year", "2023"),
            ),
            included = listOf("Board", "Paddle", "Leash", "Pump"),
            conditionNote = "Used 22 days · last checked 30 Jul",
            conditionDate = "30 Jul 2026",
        ),
        "tent" to Detail(
            specs = listOf(spec("Sleeps", "3"), spec("Weight", "4.2 kg"), spec("Season", "3-season"), spec("Year", "2022")),
            included = listOf("Tent", "Poles", "Pegs", "Footprint"),
            conditionNote = "Used 18 nights · seams resealed Jun",
            conditionDate = "20 Jul 2026",
        ),
        "bat" to Detail(
            specs = listOf(spec("Size", "Short handle", true), spec("Weight", "1.18 kg"), spec("Willow", "Kashmir")),
            included = listOf("Bat", "Cover", "Grip"),
            conditionNote = "Used 9 matches · knocked in",
            conditionDate = "1 Jul 2026",
        ),
        "kayak" to Detail(
            specs = listOf(spec("Length", "10'0\" · 305 cm", true), spec("Capacity", "125 kg"), spec("Type", "Sit-in recreational")),
            included = listOf("Kayak", "Paddle", "Buoyancy aid"),
            conditionNote = "Used 12 days · hull checked 1 Aug",
            conditionDate = "1 Aug 2026",
        ),
        "helmet" to Detail(
            specs = listOf(spec("Size", "L · 59–63 cm", true), spec("Safety", "MIPS"), spec("Year", "2025")),
            included = listOf("Helmet"),
            conditionNote = "No impacts · pads replaced Jul",
            conditionDate = "15 Jul 2026",
        ),
        "rack" to Detail(
            specs = listOf(spec("Quickdraws", "12"), spec("Cams", "0.5 – 3"), spec("Rated", "Since 2024")),
            included = listOf("12 quickdraws", "Cams", "Rack sling"),
            conditionNote = "Retired slings replaced May",
            conditionDate = "10 Jul 2026",
        ),
        "wetsuit" to Detail(
            specs = listOf(spec("Size", "M", true), spec("Thickness", "2 mm"), spec("Zip", "Back")),
            included = listOf("Wetsuit"),
            conditionNote = "Used 25 days · no tears",
            conditionDate = "18 Jul 2026",
        ),
        "burton" to Detail(
            specs = listOf(spec("Length", "158 cm", true), spec("Profile", "Camber"), spec("Year", "2023")),
            included = listOf("Board", "Bindings"),
            conditionNote = "Two seasons · base waxed Jan",
            conditionDate = "2 Jul 2026",
        ),
        "santacruz" to Detail(
            specs = listOf(spec("Frame", "M · 29\"", true), spec("Travel", "140 mm"), spec("Year", "2024")),
            included = listOf("Bike"),
            conditionNote = "Serviced Jun · tyres new",
            conditionDate = "25 Jul 2026",
            frameBand = "M",
        ),
        // Nirmal's own, listed for swap like the Burton.
        "myhelmet" to Detail(
            specs = listOf(spec("Size", "M · 54–58 cm", true), spec("Year", "2024")),
            included = listOf("Helmet"),
            conditionNote = "No impacts · bought new Mar 2025",
            conditionDate = "20 Jul 2026",
        ),
        "mycamp" to Detail(
            specs = listOf(spec("Stove", "Gas, 1 burner"), spec("Mat", "Closed-cell foam"), spec("Lamp", "Rechargeable")),
            included = listOf("Stove", "Mat", "Lamp", "Dry bag"),
            conditionNote = "Used 6 nights · all working",
            conditionDate = "20 Jul 2026",
        ),
        // Ayesha's, listed for swap only: it is what she offers in the loop.
        "sotkayak" to Detail(
            specs = listOf(spec("Length", "12'0\" · 366 cm", true), spec("Capacity", "150 kg"), spec("Type", "Sit-on-top touring")),
            included = listOf("Kayak", "Paddle", "Seat"),
            conditionNote = "Used 30 days · hull checked Jul",
            conditionDate = "24 Jul 2026",
        ),
    )

    private fun encodeSpecs(specs: List<SpecRow>) = specs.map { "${it.label}|${it.value}|${if (it.fitRelevant) 1 else 0}" }
    private fun encodeExtras(extras: List<Extra>) = extras.map { "${it.name}|${it.perDay}" }

    private fun GearItem.toEntity(order: Int, status: ListingStatus = ListingStatus.PUBLISHED): ListingEntity {
        val d = details.getValue(id)
        return ListingEntity(
            id = id,
            title = title,
            shortTitle = shortTitle,
            code = code,
            sizeLabel = sizeLabel,
            grade = grade,
            pricePerDay = pricePerDay,
            distanceKm = distanceKm,
            photos = Photos.keyList(photos),
            ownerHandle = owner.handle,
            fit = fit,
            sport = sport,
            instantBook = instantBook,
            delivery = delivery,
            deposit = deposit,
            saved = false,
            status = status,
            units = d.units,
            volumeL = d.volumeL,
            frameBand = d.frameBand,
            specs = encodeSpecs(d.specs),
            included = d.included,
            excluded = d.excluded,
            conditionPhotos = Photos.keyList(d.conditionPhotos ?: listOf(hero)),
            conditionNote = d.conditionNote,
            conditionDate = d.conditionDate,
            extras = encodeExtras(d.extras),
            discounts = listOf(true, true, false),
            waiveDepositWithPlus = false,
            createdAt = ago(60L * 24 * (40 - order)),
            sortOrder = order,
        )
    }

    fun listings(): List<ListingEntity> =
        Demo.allListings.mapIndexed { i, item -> item.toEntity(i) } +
            // Listed for swap, not for rent: they belong in S-19, not in search.
            Demo.burton.toEntity(20, ListingStatus.PAUSED) +
            Demo.santaCruz.toEntity(21, ListingStatus.PAUSED) +
            Demo.myHelmet.toEntity(22, ListingStatus.PAUSED) +
            Demo.myCampKit.toEntity(23, ListingStatus.PAUSED) +
            Demo.ayeshaKayak.toEntity(24, ListingStatus.PAUSED)

    fun rails(): List<RailEntryEntity> =
        Demo.weekendNearYou.mapIndexed { i, it -> RailEntryEntity("weekend", it.id, i) } +
            Demo.fitsYou.mapIndexed { i, it -> RailEntryEntity("fits", it.id, i) } +
            Demo.newInSurfing.mapIndexed { i, it -> RailEntryEntity("surf", it.id, i) }

    fun reviews(): List<ReviewEntity> = Demo.torqReviews.mapIndexed { i, r ->
        ReviewEntity(
            listingId = "torq",
            subjectHandle = OWNER,
            authorHandle = r.author.handle,
            date = r.date,
            stars = r.stars,
            body = r.body,
            photos = Photos.keyList(r.photos),
            ownerResponse = r.ownerResponse,
            bookingRef = null,
            createdAt = ago(60L * 24 * (30 + i * 12)),
        )
    } + otherReviews()

    private fun review(
        listingId: String,
        subject: String,
        author: String,
        day: LocalDate,
        stars: Int,
        body: String,
        bookingRef: String? = null,
    ) = ReviewEntity(
        listingId = listingId,
        subjectHandle = subject,
        authorHandle = author,
        date = com.gearloop.app.data.dayMonth(day) + " " + day.year,
        stars = stars,
        body = body,
        photos = emptyList(),
        ownerResponse = null,
        bookingRef = bookingRef,
        createdAt = at(day, 12),
    )

    /**
     * Written reviews for the rest of the catalogue, which used to say "No
     * written reviews for this item yet" on every listing but the Torq (D-32).
     *
     * They are some of the reviews each person's count already includes, so no
     * total moves. Kavitha's histogram has exactly two four-star reviews and both
     * are written ones (Dinesh on the Torq, Nirmal on the kayak), so every other
     * review of hers here is five stars. Nothing is written about the bat:
     * TC-16 reviews it from empty.
     */
    private fun otherReviews(): List<ReviewEntity> = listOf(
        // Nirmal's own, for the rentals S-31 shows as rated.
        review("trek", OWNER, ME, jul(20), 5, "Bike was set up for my height before I arrived. Brakes and gears spot on.", "GL-5X8T1"),
        review("kayak", OWNER, ME, jun(16), 4, "Stable and easy to paddle. The seat back is a bit loose, but Kavitha told me before I took it.", "GL-2H6Q3"),
        review("nsp", Demo.ayesha.handle, ME, jul(16), 5, "Pumped up in five minutes and tracked straight. Ayesha met me at the lagoon with the pump.", "GL-3P9V2"),
        // Other renters, of other listings.
        review("trek", OWNER, Demo.thusara.handle, jun(30), 5, "Comfortable on the coast road to Point Pedro and back. Lights and lock included, which helped."),
        review("helmet", OWNER, Demo.dinesh.handle, jul(11), 5, "True to size and the MIPS liner is clean. Kavitha had it sanitised before handover."),
        review("wetsuit", OWNER, Demo.thusara.handle, jul(5), 5, "Warm enough for the early sessions and no wear on the seams."),
        review("nsp", Demo.ayesha.handle, Demo.thusara.handle, jul(14), 5, "Stable enough to kneel on. Came with a pump and a dry bag."),
        review("tent", Demo.thusara.handle, Demo.ayesha.handle, jul(22), 5, "Roomy for two with our bags inside. Pitched it in ten minutes."),
        review("rack", Demo.dinesh.handle, Demo.thusara.handle, jul(8), 5, "Every draw racked and labelled, slings recently replaced. Dinesh talked me through the cams."),
        // About Nirmal, as a renter: S-37 lists these when he opens his own profile.
        review("nsp", ME, Demo.ayesha.handle, jul(17), 5, "Careful with the board, returned it early.", "GL-3P9V2"),
        review("trek", ME, OWNER, jul(21), 5, "Brought the Marlin back clean and on time. Welcome back any time."),
        review("kayak", ME, OWNER, jun(17), 5, "Rinsed it before returning it, which almost nobody does."),
    )

    fun trip() = TripEntity(
        destination = Demo.TRIP_DESTINATION,
        startDay = aug(14).toEpochDay(),
        endDay = aug(17).toEpochDay(),
        adults = 2,
        sports = listOf("Surfing", "Camping"),
    )

    fun tripItems(): List<TripItemEntity> =
        Demo.tripAlreadyOwn.mapIndexed { i, (name, note) ->
            TripItemEntity(section = "OWN", label = name, listingId = null, note = note, price = 0, added = false, position = i)
        } + Demo.tripRentLocally.mapIndexed { i, (item, who) ->
            TripItemEntity(section = "RENT", label = item.shortTitle, listingId = item.id, note = who, price = 0, added = true, position = i)
        } + listOf("Dry bag 20 L" to 300, "Reef-safe sunscreen" to 180, "First aid kit" to 250).mapIndexed { i, (name, price) ->
            TripItemEntity(section = "ADD", label = name, listingId = null, note = "", price = price, added = false, position = i)
        }

    // ------------------------------------------------------------ bookings

    private val listingsById: Map<String, GearItem> by lazy {
        (Demo.allListings + Demo.burton + Demo.santaCruz + Demo.myHelmet + Demo.myCampKit + Demo.ayeshaKayak).associateBy { it.id }
    }

    private fun itemWithDetails(id: String): GearItem {
        val base = listingsById.getValue(id)
        val d = details.getValue(id)
        return base.copy(extras = d.extras)
    }

    /** A handover code that is stable for a booking reference. */
    fun handoverCode(ref: String): String {
        val n = (ref.hashCode().toLong() and 0x7fffffff) % 900_000 + 100_000
        return n.toString()
    }

    fun booking(
        ref: String,
        listingId: String,
        renter: String,
        start: LocalDate,
        end: LocalDate,
        status: BookingStatus,
        protection: Protection = Protection.BASIC,
        unit: Int = 1,
        pickupTime: String = "09:00",
        returnTime: String = "17:00",
        respondBy: Long? = null,
        rating: Int? = null,
        createdAt: Long = ago(60L * 24 * 3),
        couponEligible: Boolean = false,
        includeExtras: Boolean = false,
        handshake: Long? = null,
        signed: Long? = null,
        returnedAt: Long? = null,
    ): BookingEntity {
        val item = itemWithDetails(listingId)
        val days = (end.toEpochDay() - start.toEpochDay() + 1).toInt()
        val quote = Pricing.quote(item, days, protection, couponEligible, includeExtras)
        return BookingEntity(
            ref = ref,
            listingId = listingId,
            unit = unit,
            renterHandle = renter,
            ownerHandle = item.owner.handle,
            startDay = start.toEpochDay(),
            endDay = end.toEpochDay(),
            protection = protection,
            includeExtras = includeExtras,
            coupon = quote.coupon,
            paymentMethod = 0,
            waiverName = null,
            waiverAt = null,
            status = status,
            total = quote.total,
            depositHold = quote.depositHold,
            ownerPayout = quote.ownerPayout,
            handoverCode = handoverCode(ref),
            pickupTime = pickupTime,
            returnTime = returnTime,
            respondBy = respondBy,
            checklist = emptyList(),
            returnChecklist = emptyList(),
            handshakeRenterAt = handshake,
            handshakeOwnerAt = handshake,
            renterSignedAt = signed,
            ownerSignedAt = signed,
            returnedAt = returnedAt,
            rating = rating,
            calendarAdded = false,
            hiddenFromHistory = false,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    fun bookings(): List<BookingEntity> = listOf(
        // ---- Nirmal's own, as S-31 lists them
        booking("GL-6B1P2", "nsp", ME, aug(12), aug(14), BookingStatus.CONFIRMED, createdAt = at(aug(1), 18, 30)),
        booking("GL-9R2K4", "tent", ME, aug(22), aug(25), BookingStatus.CONFIRMED, createdAt = at(aug(2), 20, 5)),
        booking("GL-5X8T1", "trek", ME, jul(18), jul(20), BookingStatus.COMPLETED, rating = 5, createdAt = at(jul(12), 9, 0), handshake = at(jul(18), 9, 0), signed = at(jul(18), 9, 5), returnedAt = at(jul(20), 16, 10)),
        booking("GL-4M0N7", "bat", ME, jul(2), jul(3), BookingStatus.COMPLETED, createdAt = at(jun(28), 12, 0), handshake = at(jul(2), 8, 30), signed = at(jul(2), 8, 35), returnedAt = at(jul(3), 18, 0)),
        booking("GL-2H6Q3", "kayak", ME, jun(14), jun(16), BookingStatus.COMPLETED, rating = 4, createdAt = at(jun(8), 10, 0), handshake = at(jun(14), 7, 0), signed = at(jun(14), 7, 5), returnedAt = at(jun(16), 17, 30)),
        // Back two hours early: the rental Ayesha's "returned it early" review
        // (S-38) is about. It had no booking behind it before. Kept before
        // 18 Jul so GL-5X8T1 stays the newest past rental, which TC-26 removes.
        booking("GL-3P9V2", "nsp", ME, jul(15), jul(16), BookingStatus.COMPLETED, rating = 5, createdAt = at(jul(11), 19, 0), handshake = at(jul(15), 9, 0), signed = at(jul(15), 9, 4), returnedAt = at(jul(16), 15, 0)),

        // ---- the owner's side: Kavitha's gear, other renters
        booking("GL-3D9X2", "torq", Demo.dinesh.handle, aug(14), aug(16), BookingStatus.CONFIRMED, Protection.PLUS, createdAt = at(aug(3), 11, 0)),
        booking("GL-8N4C7", "torq", Demo.thusara.handle, sep(4), sep(6), BookingStatus.CONFIRMED, Protection.PLUS, createdAt = at(aug(4), 15, 20)),
        // Overdue since 03:00 this morning, and the same unit goes out again at
        // 11:00 — the conflict S-27 surfaces and "Move to unit #2" resolves.
        booking("GL-5P7R4", "trek", Demo.ayesha.handle, aug(2), aug(5), BookingStatus.ACTIVE, unit = 1, returnTime = "03:00", createdAt = at(jul(30), 9, 0), handshake = at(aug(2), 9, 0), signed = at(aug(2), 9, 4)),
        booking("GL-4C2J8", "trek", Demo.dinesh.handle, aug(5), aug(6), BookingStatus.CONFIRMED, unit = 1, pickupTime = "11:00", createdAt = at(aug(1), 14, 0)),
        booking("GL-6T1Y8", "trek", Demo.thusara.handle, aug(17), aug(19), BookingStatus.CONFIRMED, unit = 2, createdAt = at(aug(3), 19, 0)),
        booking("GL-1W5Z3", "wetsuit", Demo.rizwan.handle, aug(3), aug(12), BookingStatus.ACTIVE, createdAt = at(jul(29), 10, 0), handshake = at(aug(3), 9, 0), signed = at(aug(3), 9, 2)),

        // ---- requests waiting on Kavitha, each with its own clock (S-28)
        booking("GL-2K8R5", "kayak", Demo.thusara.handle, aug(12), aug(14), BookingStatus.REQUESTED, respondBy = at(today, 12, 26), createdAt = at(aug(4), 12, 26)),
        booking("GL-7Q3M1", "trek", Demo.rizwan.handle, aug(15), aug(16), BookingStatus.REQUESTED, unit = 1, respondBy = at(today, 18, 54), createdAt = at(aug(4), 18, 54)),
        booking("GL-9F4T6", "kayak", Demo.ayesha.handle, aug(20), aug(24), BookingStatus.REQUESTED, respondBy = at(aug(6), 13, 14), createdAt = at(today, 7, 50)),
    )

    fun blocks(): List<CalendarBlockEntity> = listOf(
        CalendarBlockEntity(listingId = "torq", unit = 1, day = aug(24).toEpochDay(), reason = "Fin box repair"),
        CalendarBlockEntity(listingId = "torq", unit = 1, day = aug(25).toEpochDay(), reason = "Fin box repair"),
    )

    // ------------------------------------------------------------ money

    fun walletTxns(): List<WalletTxnEntity> = listOf(
        WalletTxnEntity(label = "Payout · Burton Custom 158", amount = 6_400, createdAt = at(jul(31), 10, 0)),
        WalletTxnEntity(label = "Booking GL-5X8T1 · Trek Marlin 7", amount = -5_130, createdAt = at(jul(20), 9, 0)),
        WalletTxnEntity(label = "Payout · Burton Custom 158", amount = 7_800, createdAt = at(jul(12), 10, 0)),
    )

    fun notifications(): List<NotificationEntity> = listOf(
        NotificationEntity(
            kind = NotificationItem.Kind.SAFETY,
            title = "Your leash is due for replacement",
            body = "PPE logged 14 months ago. Leashes are rated for 12 months of regular use.",
            unread = true,
            createdAt = at(today, 8, 15),
            route = "s36",
            ref = null,
        ),
        NotificationEntity(
            kind = NotificationItem.Kind.SWAP,
            title = "Dinesh countered your swap",
            body = "Santa Cruz Chameleon M · round 2 of 5 · expires in 5 d",
            unread = false,
            createdAt = at(aug(4), 14, 44),
            route = "s22",
            ref = Demo.SWAP_REF,
        ),
        NotificationEntity(
            kind = NotificationItem.Kind.PAYOUT,
            title = "LKR 12,400 is on its way",
            body = "Payout for July arrives ${Demo.NEXT_PAYOUT_DATE}",
            unread = false,
            createdAt = at(aug(3), 9, 30),
            route = "s35",
            ref = null,
        ),
        NotificationEntity(
            kind = NotificationItem.Kind.REVIEW,
            title = "Ayesha left you a review",
            body = "★ 5 — “Careful with the board, returned it early.”",
            unread = false,
            createdAt = at(jul(17), 9, 30),
            route = "s32",
            ref = null,
        ),
    )

    /**
     * The thread with Kavitha starts as the enquiry before booking. The booking
     * itself adds the confirmation and unlocks the pickup point (FR-15).
     */
    fun messages(): List<MessageEntity> = listOf(
        MessageEntity(threadId = THREAD_OWNER, body = "Hi Kavitha! Is the Torq OK for someone who has surfed a few times?", fromMe = true, kind = ChatMessage.Kind.TEXT, createdAt = at(aug(3), 18, 20)),
        MessageEntity(threadId = THREAD_OWNER, body = "Hi Nirmal — yes, it's a soft top and very forgiving. Book whenever suits you.", fromMe = false, kind = ChatMessage.Kind.TEXT, createdAt = at(aug(3), 18, 41)),
        MessageEntity(threadId = THREAD_OWNER, body = "Call me on 077 ••• ••••", fromMe = false, kind = ChatMessage.Kind.REDACTED, createdAt = at(aug(3), 18, 42)),
        MessageEntity(threadId = THREAD_OWNER, body = "Perfect, thanks. I'll book it for the weekend.", fromMe = true, kind = ChatMessage.Kind.TEXT, createdAt = at(aug(3), 18, 50)),
    ) + Demo.swapThread.mapIndexed { i, m ->
        MessageEntity(threadId = Demo.SWAP_REF, body = m.body, fromMe = m.fromMe, kind = m.kind, createdAt = at(aug(4), 14, 2 + i * 21))
    }

    /** The adjudicator's queue: other people's claims, so S-40 is never empty. */
    fun claims(): List<ClaimEntity> = listOf(
        claim("CL-3079", "Trek Marlin 7", OWNER, Demo.ayesha.handle, 3_200, 46_000, DamageMark.Severity.COSMETIC, "Top tube", "Paint chip on the top tube, about 1 cm.", 28),
        claim("CL-3074", "NSP Elements SUP", Demo.ayesha.handle, Demo.thusara.handle, 41_000, 60_000, DamageMark.Severity.MAJOR, "Seam", "Side seam split; the board no longer holds pressure.", 50),
        claim("CL-3068", "Quechua MH500 3P", Demo.thusara.handle, Demo.dinesh.handle, 1_800, 24_000, DamageMark.Severity.COSMETIC, "Fly", "Small burn hole in the fly, away from the seams.", 80),
        claim("CL-3061", "Pelican Argo 100X", OWNER, Demo.rizwan.handle, 22_500, 52_000, DamageMark.Severity.FUNCTIONAL, "Hull", "Gouge on the hull that lets water in under load.", 97),
    )

    private fun claim(
        ref: String, title: String, claimant: String, respondent: String, amount: Int, value: Int,
        severity: DamageMark.Severity, area: String, note: String, ageHours: Long,
    ) = ClaimEntity(
        ref = ref,
        bookingRef = null,
        listingTitle = title,
        claimantHandle = claimant,
        respondentHandle = respondent,
        amount = amount,
        itemValue = value,
        severity = severity,
        area = area,
        note = note,
        status = ClaimStatus.CONTESTED,
        createdAt = ago(ageHours * 60),
        respondBy = ago(ageHours * 60 - 5L * 24 * 60),
        renterStatement = "The mark was there at pickup.",
        renterEvidence = null,
        award = null,
        rationale = null,
        decidedAt = null,
        decidedBy = null,
    )

    // ------------------------------------------------------------ trust

    fun verificationSteps(): List<VerificationStepEntity> = Demo.verificationSteps.mapIndexed { i, s ->
        VerificationStepEntity(position = i, title = s.title, unlocks = s.unlocks, state = s.state, completedAt = null)
    }

    fun badges(): List<BadgeEntity> = Demo.badges.mapIndexed { i, (name, earned) ->
        BadgeEntity(name = name, earned = earned, featured = earned && i == 0, position = i)
    }

    // ------------------------------------------------------------ exchange

    fun matches(): List<SwapMatchEntity> = Demo.swapMatches.mapIndexed { i, m ->
        SwapMatchEntity(
            id = "m${i + 1}",
            giveListingId = m.give.id,
            getListingId = m.get.id,
            giveValue = m.giveValue,
            getValue = m.getValue,
            counterpartyHandle = m.counterparty.handle,
            threeWay = m.threeWay,
            hidden = false,
            position = i,
        )
    }

    /** Dinesh agreed at LKR 19,000 in round two. It is Nirmal's move. */
    fun proposals(): List<SwapProposalEntity> = listOf(
        SwapProposalEntity(
            ref = Demo.SWAP_REF,
            matchId = "m1",
            giveListingId = Demo.burton.id,
            getListingId = Demo.santaCruz.id,
            giveValue = Demo.BURTON_FMV,
            getValue = Demo.SANTACRUZ_FMV,
            counterpartyHandle = Demo.dinesh.handle,
            boot = Demo.BOOT_AMOUNT,
            status = ProposalStatus.COUNTERED,
            round = 2,
            expiresAt = DemoClock.toMillis(DemoClock.WORLD_START.plusDays(5).plusHours(6)),
            createdAt = at(aug(3), 16, 0),
            updatedAt = at(aug(4), 14, 44),
        ),
    )

    /**
     * The three-way loop: Nirmal's Burton goes to Dinesh, Dinesh's Santa Cruz
     * to Ayesha, Ayesha's own sit-on-top kayak to Nirmal. Dinesh has accepted; the other two
     * have not, so the first thing the renter can do on S-23 is accept.
     */
    fun cycle(): List<CycleMemberEntity> = listOf(
        CycleMemberEntity(CYCLE_ID, ME, Demo.burton.id, Demo.BURTON_FMV, null, 0),
        CycleMemberEntity(CYCLE_ID, Demo.dinesh.handle, Demo.santaCruz.id, Demo.SANTACRUZ_FMV, true, 1),
        CycleMemberEntity(CYCLE_ID, Demo.ayesha.handle, Demo.ayeshaKayak.id, Demo.AYESHA_KAYAK_FMV, null, 2),
    )

    // ------------------------------------------------------------ owner

    fun tasks(): List<OwnerTaskEntity> = listOf(
        OwnerTaskEntity("service", NeedsAttention.Severity.SOON, "Service due in 3 days", "Trek Marlin #2 · calendar blocks 12 Aug", "Block dates", false, 0),
        OwnerTaskEntity("review", NeedsAttention.Severity.SOON, "Review to write", "Mohamed Rizwan · expires in 4 days", "Write it", false, 1),
    )

    fun closet(): List<ClosetItemEntity> {
        fun item(g: GearItem, listed: Boolean, idleDays: Long, pos: Int) = ClosetItemEntity(
            title = g.title,
            shortTitle = g.shortTitle,
            photoKey = Photos.key(g.hero),
            sport = g.sport,
            sizeLabel = g.sizeLabel,
            grade = g.grade,
            code = g.code,
            listed = listed,
            listingId = null,
            idleSince = ago(idleDays * 24 * 60),
            position = pos,
        )
        // Nirmal's own gear, and only his. The three listed ones are the three he
        // has listed for swap, so the closet and S-20 offer the same things.
        return listOf(
            item(Demo.burton, true, 6, 0).copy(listingId = Demo.burton.id),
            item(Demo.myHelmet, true, 20, 1).copy(listingId = Demo.myHelmet.id),
            item(Demo.myKayak, false, 94, 2),
            item(Demo.myCampKit, true, 12, 3).copy(listingId = Demo.myCampKit.id),
            item(Demo.myBat, false, 33, 4),
        )
    }

    fun session(anchor: Long) = SessionEntity(
        listingId = Demo.torq.id,
        bookingRef = null,
        proposalRef = Demo.SWAP_REF,
        claimRef = null,
        threadId = THREAD_OWNER,
        draftListingId = null,
        capturePhase = Phase.PICKUP,
        clockAnchor = anchor,
        bookingSeq = 0,
    )

    /** A message thread is keyed by the other person's handle. */
    const val THREAD_OWNER = OWNER
    const val CYCLE_ID = "C-1"

    /**
     * New booking references, in the order they are handed out. The first is the
     * one the whole prototype quoted, so the first walk through Flow A produces
     * the booking every caption and screenshot refers to.
     */
    val bookingRefs = listOf("GL-7K3M9", "GL-8D2F5", "GL-3H7Q1", "GL-6M4V9", "GL-2T8X3", "GL-5N1B7")

    /** `LocalDateTime` of a day and an `HH:mm` string. */
    fun dayAt(day: LocalDate, hhmm: String): LocalDateTime {
        val (h, m) = hhmm.split(":").map(String::toInt)
        return day.atTime(h, m)
    }
}
