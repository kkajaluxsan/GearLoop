package com.gearloop.app.data.repo

import com.gearloop.app.data.AccountRole
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Extra
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.Marketplace
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Person
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.ProposalStatus
import com.gearloop.app.data.Protection
import com.gearloop.app.data.SpecRow
import com.gearloop.app.data.TrustTier
import com.gearloop.app.data.VerificationStep
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.db.AccountEntity
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
import com.gearloop.app.data.db.ReviewEntity
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
 * The sample marketplace a new install starts with, built from [Demo] and
 * [Marketplace].
 *
 * [Demo] stays the one place the original facts are written — the Torq's price,
 * the reviews, the waiver, the valuations. This file decides which of those
 * facts become rows, gives every sample member an account to sign in with, and
 * writes every date relative to the moment the database is created ([World]),
 * so the bookings, requests and conversations are always about this week
 * rather than a fixed date.
 *
 * Three things are deliberately *not* written, because the first booking made
 * in the app creates them: booking GL-7K3M9, its payment, and the messages and
 * notification that follow it.
 */
object Seed {

    // ------------------------------------------------------------ handles

    /** The sample renter account. */
    const val ME = "@nirmal_r"

    /** The sample owner account. */
    const val OWNER = "@kavitha_s"

    /** The claims desk: an admin account, staff rather than a member. */
    const val ADMIN = "@ruwan_ops"

    /** The password every sample account signs in with. */
    const val SAMPLE_PASSWORD = "gearloop123"

    const val CYCLE_ID = "C-1"

    val admin = Person("Ruwan Jayasuriya", ADMIN, "GearLoop claims desk", TrustTier.BRONZE, 0.0, 0, "", 0, "Jan 2024")

    /** Sample members, by handle: the original catalogue, then the wider marketplace. */
    val members: List<Person> =
        listOf(Demo.nirmal, Demo.kavitha, Demo.dinesh, Demo.thusara, Demo.ayesha, Demo.rizwan) + Marketplace.owners

    fun emailOf(handle: String): String = when (handle) {
        ME -> "nirmal@gearloop.lk"
        OWNER -> "kavitha@gearloop.lk"
        ADMIN -> "ruwan@gearloop.lk"
        "@m_rizwan" -> "rizwan@gearloop.lk"
        else -> handle.removePrefix("@").substringBefore('_') + "@gearloop.lk"
    }

    /**
     * PBKDF2 hashes of [SAMPLE_PASSWORD], one salt per account, computed ahead
     * of time with the same parameters as [com.gearloop.app.data.Passwords] so
     * the first launch does not spend seconds hashing.
     */
    private val sampleHashes: Map<String, Pair<String, String>> = mapOf(
        "@nirmal_r" to ("+C5U83MEU5gMMyj4N0gH1kHierx57WJQ2UjA7AqfCs8=" to "bM4090pNR73p1UUtrOe1Jg=="),
        "@kavitha_s" to ("EfVYxpjfACUBmW5KoB8qIvCMwVnz7ypFRiZ5yGE1gVw=" to "HQaA9GRtKAWY9IgJG2SiNA=="),
        "@dinesh_k" to ("iBP+psibDmZjLJ0Gc3CqTr0KOxDkeyoJofo+KWhS7fY=" to "21Hvt6rSgtWDW9eolwZTPw=="),
        "@thusara_p" to ("/oUnWwrqseQmbOUPX/xdoY4+d/Ge05p9orKZ7LshAB4=" to "69KYsrxM53uL9fcmAvMP7Q=="),
        "@ayesha_f" to ("qtHS/PElXjOVGboQaEz4r40NCVAjLB9eLZyTGub9/EQ=" to "4hfCTefVR93z3SKA7n5fEg=="),
        "@m_rizwan" to ("1WuZPNzcPH7WHYCcGnXxEMWM3q7xwPS2sqgYoDwB2VU=" to "uf/Af/vQ90uz3TVUSucnvQ=="),
        "@priya_n" to ("IeacpBwZ0wJ+261Kas/CWnryR4zRtPrZd6/MNw8dSCk=" to "W3UvT9rEFPOj16mmneyj+g=="),
        "@arun_s" to ("bxna17nYxb9tLulVWGYH6ls0+1VjuzuSG32D1z+m/aM=" to "/b1r0zm+7y4pAPj9jsn0Aw=="),
        "@fathima_r" to ("RP5VwLoWiibpbFoRbU4fKRaKRdNbzkCQoWiU2nRGzSY=" to "5ZUo9z5B9hrzFXLbUv/S/g=="),
        "@kasun_w" to ("HuWwNoElg/MXggTkXtJrZse+D/TTjBGK0vk4Rn0kQLM=" to "1u+3B7jKwQjGCAWV562wxw=="),
        "@tharsika_m" to ("2H2YwNH76IaW0/uNX276loFFYDw2nBW82bbt0oiamhQ=" to "5MaMOnDZWyzNJ1A10eviMQ=="),
        "@sanjay_p" to ("q1yE0TWT6NfEEkMQAlZD7b3Buogh3HRNowxti7YrKBY=" to "bQAOM3U5JQERxKAa7aodBA=="),
        "@meera_g" to ("qlGtXCCJH3B0pZPnlqNQ5tkmcke3X7MuCklhdtR0r8c=" to "g1GX+BzEr1lX4gdfswZQ8A=="),
        "@rohan_ds" to ("XbTHubSbCeiLBkIXGIqMJT0N60m8TEWJu5diZclGlWE=" to "4HC+EqNN+s/PIfOE3/OJVg=="),
        "@ruwan_ops" to ("pAPHqp5/L0QIgjdFFnIk5kBlwQvIJ0TsESGSBcdcse8=" to "6e93k7PQNQivRJ7losW1BA=="),
    )

    /** A handover code that is stable for a booking reference. */
    fun handoverCode(ref: String): String {
        val n = (ref.hashCode().toLong() and 0x7fffffff) % 900_000 + 100_000
        return n.toString()
    }

    /** `LocalDateTime` of a day and an `HH:mm` string. */
    fun dayAt(day: LocalDate, hhmm: String): LocalDateTime {
        val (h, m) = hhmm.split(":").map(String::toInt)
        return day.atTime(h, m)
    }

    /**
     * New booking references, in the order they are handed out. The first is the
     * one the original prototype quoted, so the first booking made in the app is
     * GL-7K3M9.
     */
    val bookingRefs = listOf("GL-7K3M9", "GL-8D2F5", "GL-3H7Q1", "GL-6M4V9", "GL-2T8X3", "GL-5N1B7")

    // ------------------------------------------------------------ a new member

    fun newProfile(handle: String) = ProfileEntity(
        handle = handle, intent = 0, heightCm = null, weightKg = null, shoeEu = 42, metric = true,
        couponUsed = false, walletAvailable = 0, address = null, onboarded = false,
        cardBrand = null, cardLast4 = null, cardExpiry = null,
    )

    private val stepTitles = Demo.verificationSteps.map { it.title to it.unlocks }

    /** Email is verified by signing up with it; the phone too, if one was given. */
    fun newVerification(handle: String, phoneDone: Boolean, phone: String?, at: Long): List<VerificationStepEntity> =
        stepTitles.mapIndexed { i, (title, unlocks) ->
            val done = i == 0 || (i == 1 && phoneDone)
            val current = !done && i == (if (phoneDone) 2 else 1)
            VerificationStepEntity(
                handle = handle, position = i, title = title, unlocks = unlocks,
                state = when {
                    done -> VerificationStep.State.DONE
                    current -> VerificationStep.State.CURRENT
                    else -> VerificationStep.State.LOCKED
                },
                completedAt = if (done) at else null,
                detail = if (i == 1 && phoneDone) phone else null,
            )
        }

    fun newBadges(handle: String): List<BadgeEntity> = Demo.badges.mapIndexed { i, (name, _) ->
        BadgeEntity(handle = handle, name = name, earned = false, featured = false, position = i)
    }

    /** A blank trip for next weekend, ready to be planned. */
    fun newTrip(handle: String): TripEntity {
        val start = AppClock.today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.SATURDAY))
        return TripEntity(handle, "Arugam Bay", start.toEpochDay(), start.plusDays(2).toEpochDay(), 1, emptyList())
    }

    /** The small extras the trip planner suggests, not yet in the kit. */
    fun tripExtras(handle: String): List<TripItemEntity> =
        listOf("Dry bag 20 L" to 300, "Reef-safe sunscreen" to 180, "First aid kit" to 250).mapIndexed { i, (name, price) ->
            TripItemEntity(handle = handle, section = "ADD", label = name, listingId = null, note = "", price = price, added = false, position = i)
        }

    // ============================================================ the world

    /** Everything the sample marketplace holds, dated relative to [now]. */
    class World(private val now: LocalDateTime = AppClock.now()) {

        private val today: LocalDate = now.toLocalDate()
        private fun day(n: Int): LocalDate = today.plusDays(n.toLong())
        private fun at(day: LocalDate, hour: Int, minute: Int = 0): Long = AppClock.toMillis(day.atTime(hour, minute))
        private fun ago(minutes: Long): Long = AppClock.toMillis(now.minusMinutes(minutes))
        private fun ahead(minutes: Long): Long = AppClock.toMillis(now.plusMinutes(minutes))
        private fun dated(daysAgo: Int): String = day(-daysAgo).let { dayMonth(it) + " " + it.year }
        private fun hhmm(t: LocalDateTime) = "%02d:%02d".format(t.hour, t.minute)

        // -------------------------------------------------------- accounts

        fun accounts(): List<AccountEntity> = (members + admin).mapIndexed { i, p ->
            val (hash, salt) = sampleHashes.getValue(p.handle)
            AccountEntity(
                handle = p.handle,
                email = emailOf(p.handle),
                passwordHash = hash,
                salt = salt,
                phone = "07${(i * 7 + 1) % 8} ${(412 + i * 37) % 900 + 100} ${(3381 + i * 1213) % 9000 + 1000}",
                role = if (p.handle == ADMIN) AccountRole.ADMIN else AccountRole.MEMBER,
                createdAt = ago(60L * 24 * 200),
                lastLoginAt = null,
            )
        }

        // -------------------------------------------------------- people

        private data class Stats(val completed: Int, val onTime: Int, val verified: Int, val stars: List<Int> = emptyList())

        private val stats: Map<String, Stats> = mapOf(
            ME to Stats(14, 100, 5),
            // 14 rentals is what her public profile shows.
            OWNER to Stats(14, 100, 6, Demo.starDistribution.counts),
            Demo.dinesh.handle to Stats(12, 92, 5),
            Demo.thusara.handle to Stats(8, 100, 5),
            Demo.ayesha.handle to Stats(21, 100, 6),
            Demo.rizwan.handle to Stats(3, 67, 3),
            Marketplace.priya.handle to Stats(11, 100, 5),
            Marketplace.arun.handle to Stats(26, 100, 6),
            Marketplace.fathima.handle to Stats(6, 100, 4),
            Marketplace.kasun.handle to Stats(2, 100, 3),
            Marketplace.tharsika.handle to Stats(9, 100, 5),
            Marketplace.sanjay.handle to Stats(23, 96, 6),
            Marketplace.meera.handle to Stats(7, 100, 5),
            Marketplace.rohan.handle to Stats(1, 100, 3),
            ADMIN to Stats(0, 100, 6),
        )

        fun people(): List<PersonEntity> = (members + admin).map { p ->
            val s = stats.getValue(p.handle)
            PersonEntity(
                handle = p.handle, name = p.name, area = p.area, tier = p.tier, rating = p.rating,
                reviews = p.reviews, respondsIn = p.respondsIn, listings = p.listings, memberSince = p.memberSince,
                starCounts = s.stars, completedRentals = s.completed, onTimeReturns = s.onTime, verifiedSteps = s.verified,
            )
        }

        fun profiles(): List<ProfileEntity> = (members + admin).map { p ->
            when (p.handle) {
                ME -> ProfileEntity(
                    handle = ME, intent = 0, heightCm = Demo.HEIGHT_CM, weightKg = Demo.WEIGHT_KG, shoeEu = Demo.SHOE_EU,
                    metric = true, couponUsed = false, walletAvailable = 6_480, address = null, onboarded = true,
                    cardBrand = "Visa", cardLast4 = "4471", cardExpiry = "09/28",
                )
                OWNER -> ProfileEntity(
                    handle = OWNER, intent = 1, heightCm = 163, weightKg = 56, shoeEu = 38, metric = true,
                    couponUsed = true, walletAvailable = 12_400, address = MoneyRepository.pickupAddress(OWNER), onboarded = true,
                    cardBrand = "Mastercard", cardLast4 = "2290", cardExpiry = "04/29",
                )
                else -> newProfile(p.handle).copy(onboarded = true, intent = if (p.handle == ADMIN) 0 else 1, walletAvailable = 2_000)
            }
        }

        fun sports(): List<SportInterestEntity> {
            val mine = listOf("Surfing", "Cycling", "SUP").mapIndexed { i, s -> SportInterestEntity(ME, s, ago(10_000L - i)) }
            val owners = allListings().groupBy { it.owner.handle }
                .filterKeys { it != ME }
                .flatMap { (handle, items) ->
                    items.map { it.sport }.distinct().take(3).mapIndexed { i, s -> SportInterestEntity(handle, s, ago(20_000L - i)) }
                }
            return mine + owners
        }

        // -------------------------------------------------------- listings

        private fun spec(label: String, value: String, fit: Boolean = false) = SpecRow(label, value, fit)

        /** What the original prototype showed only for the Torq, written for every item. */
        private data class Detail(
            val specs: List<SpecRow>,
            val included: List<String>,
            val excluded: List<String> = emptyList(),
            val conditionNote: String,
            val conditionDaysAgo: Int,
            val volumeL: Int? = null,
            val frameBand: String? = null,
            val units: Int = 1,
            val extras: List<Extra> = emptyList(),
            val conditionPhotos: List<String>? = null,
            val description: String = "",
        )

        private val details: Map<String, Detail> = mapOf(
            "torq" to Detail(
                specs = Demo.torqSpecs,
                included = Demo.torqIncluded,
                excluded = Demo.torqExcluded,
                conditionNote = "Used 47 days · serviced last month",
                conditionDaysAgo = 24,
                volumeL = 42,
                extras = listOf(Extra("Rash guard", Demo.EXTRA_RASHGUARD / Demo.BOOKING_DAYS)),
                conditionPhotos = Demo.torqConditionPhotos,
                description = "A soft-top fish that paddles like a longboard and turns like a shortboard. The dings on the deck are cosmetic and on the condition record.",
            ),
            "trek" to Detail(
                specs = listOf(
                    spec("Frame", "M · 52 cm", true), spec("Wheels", "29\""),
                    spec("Drivetrain", "1 × 10"), spec("Brakes", "Hydraulic disc"), spec("Year", "2024"),
                ),
                included = listOf("Bike", "Lights", "Lock", "Repair kit"),
                excluded = listOf("Helmet (rent separately)"),
                conditionNote = "Used 31 days · serviced last week",
                conditionDaysAgo = 8,
                frameBand = "M",
                units = 2,
                description = "Set up for the coast road and the farm tracks. Two identical bikes, so a pair can ride together.",
            ),
            "nsp" to Detail(
                specs = listOf(spec("Length", "10'6\" · 320 cm", true), spec("Width", "32\""), spec("Construction", "Inflatable"), spec("Year", "2023")),
                included = listOf("Board", "Paddle", "Leash", "Pump"),
                conditionNote = "Used 22 days · checked last week",
                conditionDaysAgo = 6,
                description = "Stable enough to kneel on in the lagoon, quick enough for a morning paddle to the fort.",
            ),
            "tent" to Detail(
                specs = listOf(spec("Sleeps", "3"), spec("Weight", "4.2 kg"), spec("Season", "3-season"), spec("Year", "2022")),
                included = listOf("Tent", "Poles", "Pegs", "Footprint"),
                conditionNote = "Used 18 nights · seams resealed",
                conditionDaysAgo = 16,
                description = "Roomy for two with bags inside, or three friends who don't mind.",
            ),
            "bat" to Detail(
                specs = listOf(spec("Size", "Short handle", true), spec("Weight", "1.18 kg"), spec("Willow", "Kashmir")),
                included = listOf("Bat", "Cover", "Grip"),
                conditionNote = "Used 9 matches · knocked in",
                conditionDaysAgo = 35,
                description = "Knocked in and ready to play. Short handle.",
            ),
            "kayak" to Detail(
                specs = listOf(spec("Length", "10'0\" · 305 cm", true), spec("Capacity", "125 kg"), spec("Type", "Sit-in recreational")),
                included = listOf("Kayak", "Paddle", "Buoyancy aid"),
                conditionNote = "Used 12 days · hull checked",
                conditionDaysAgo = 4,
                description = "A steady sit-in kayak for the lagoon. Kavitha can deliver it within 5 km of Nallur.",
            ),
            "helmet" to Detail(
                specs = listOf(spec("Size", "L · 59–63 cm", true), spec("Safety", "MIPS"), spec("Year", "2025")),
                included = listOf("Helmet"),
                conditionNote = "No impacts · pads replaced",
                conditionDaysAgo = 21,
                description = "MIPS helmet, sanitised between every rental.",
            ),
            "rack" to Detail(
                specs = listOf(spec("Quickdraws", "12"), spec("Cams", "0.5 – 3"), spec("Rated", "Since 2024")),
                included = listOf("12 quickdraws", "Cams", "Rack sling"),
                conditionNote = "Retired slings replaced",
                conditionDaysAgo = 26,
                description = "A full trad rack, every piece labelled. Dinesh walks you through the cams at pickup.",
            ),
            "wetsuit" to Detail(
                specs = listOf(spec("Size", "M", true), spec("Thickness", "2 mm"), spec("Zip", "Back")),
                included = listOf("Wetsuit"),
                conditionNote = "Used 25 days · no tears",
                conditionDaysAgo = 18,
                description = "A 2 mm shorty for the early sessions.",
            ),
            "burton" to Detail(
                specs = listOf(spec("Length", "158 cm", true), spec("Profile", "Camber"), spec("Year", "2023")),
                included = listOf("Board", "Bindings"),
                conditionNote = "Two seasons · base waxed",
                conditionDaysAgo = 34,
            ),
            "santacruz" to Detail(
                specs = listOf(spec("Frame", "M · 29\"", true), spec("Travel", "140 mm"), spec("Year", "2024")),
                included = listOf("Bike"),
                conditionNote = "Serviced · tyres new",
                conditionDaysAgo = 11,
                frameBand = "M",
            ),
            // Nirmal's own, listed for swap like the Burton.
            "myhelmet" to Detail(
                specs = listOf(spec("Size", "M · 54–58 cm", true), spec("Year", "2024")),
                included = listOf("Helmet"),
                conditionNote = "No impacts · bought new last year",
                conditionDaysAgo = 16,
            ),
            "mycamp" to Detail(
                specs = listOf(spec("Stove", "Gas, 1 burner"), spec("Mat", "Closed-cell foam"), spec("Lamp", "Rechargeable")),
                included = listOf("Stove", "Mat", "Lamp", "Dry bag"),
                conditionNote = "Used 6 nights · all working",
                conditionDaysAgo = 16,
            ),
            // Ayesha's, listed for swap only: it is what she offers in the loop.
            "sotkayak" to Detail(
                specs = listOf(spec("Length", "12'0\" · 366 cm", true), spec("Capacity", "150 kg"), spec("Type", "Sit-on-top touring")),
                included = listOf("Kayak", "Paddle", "Seat"),
                conditionNote = "Used 30 days · hull checked",
                conditionDaysAgo = 12,
            ),
        )

        private fun encodeSpecs(specs: List<SpecRow>) = specs.map { "${it.label}|${it.value}|${if (it.fitRelevant) 1 else 0}" }
        private fun encodeExtras(extras: List<Extra>) = extras.map { "${it.name}|${it.perDay}" }

        private fun GearItem.toEntity(order: Int, status: ListingStatus = ListingStatus.PUBLISHED): ListingEntity {
            val d = details.getValue(id)
            return ListingEntity(
                id = id, title = title, shortTitle = shortTitle, code = code, sizeLabel = sizeLabel, grade = grade,
                pricePerDay = pricePerDay, distanceKm = distanceKm, photos = photos, ownerHandle = owner.handle,
                fit = fit, sport = sport, instantBook = instantBook, delivery = delivery, deposit = deposit,
                status = status, units = d.units, volumeL = d.volumeL, frameBand = d.frameBand,
                specs = encodeSpecs(d.specs), included = d.included, excluded = d.excluded,
                conditionPhotos = d.conditionPhotos ?: listOf(hero), conditionNote = d.conditionNote,
                conditionDate = dated(d.conditionDaysAgo), extras = encodeExtras(d.extras),
                discounts = listOf(true, true, false), waiveDepositWithPlus = false, description = d.description,
                createdAt = ago(60L * 24 * (40 - order)), sortOrder = order,
            )
        }

        private fun Marketplace.Entry.toEntity(order: Int): ListingEntity = ListingEntity(
            id = item.id, title = item.title, shortTitle = item.shortTitle, code = item.code, sizeLabel = item.sizeLabel,
            grade = item.grade, pricePerDay = item.pricePerDay, distanceKm = item.distanceKm, photos = item.photos,
            ownerHandle = item.owner.handle, fit = item.fit, sport = item.sport, instantBook = item.instantBook,
            delivery = item.delivery, deposit = item.deposit, status = ListingStatus.PUBLISHED, units = detail.units,
            volumeL = item.volumeL, frameBand = item.frameBand, specs = encodeSpecs(detail.specs),
            included = detail.included, excluded = detail.excluded, conditionPhotos = item.photos,
            conditionNote = detail.conditionNote, conditionDate = dated(detail.conditionDaysAgo), extras = emptyList(),
            discounts = listOf(true, true, false), waiveDepositWithPlus = false, description = detail.description,
            // Spread over the last two months, newest first in the "new" rails.
            createdAt = ago(60L * 24 * ((order * 7) % 55 + 1)), sortOrder = 30 + order,
        )

        /** Listed for swap, not for rent: they belong in the swap feed, not in search. */
        private val swapOnly = listOf(Demo.burton, Demo.santaCruz, Demo.myHelmet, Demo.myCampKit, Demo.ayeshaKayak)

        private fun allListings(): List<GearItem> = Demo.allListings + swapOnly + Marketplace.listings

        fun listings(): List<ListingEntity> =
            Demo.allListings.mapIndexed { i, item -> item.toEntity(i) } +
                swapOnly.mapIndexed { i, item -> item.toEntity(20 + i, ListingStatus.PAUSED) } +
                Marketplace.entries.mapIndexed { i, e -> e.toEntity(i) }

        // -------------------------------------------------------- reviews

        private fun review(
            listingId: String, subject: String, author: String, daysAgo: Int, stars: Int, body: String,
            bookingRef: String? = null, photos: List<String> = emptyList(), response: String? = null,
        ) = ReviewEntity(
            listingId = listingId, subjectHandle = subject, authorHandle = author, date = dated(daysAgo), stars = stars,
            body = body, photos = photos, ownerResponse = response, bookingRef = bookingRef, createdAt = at(day(-daysAgo), 12),
        )

        /**
         * Written reviews. They are some of the reviews each person's count
         * already includes, so no total moves. Kavitha's histogram has exactly two
         * four-star reviews and both are written ones (Dinesh on the Torq, Nirmal
         * on the kayak), so every other review of hers is five stars. Nothing is
         * written about the bat, which is reviewed from empty.
         */
        fun reviews(): List<ReviewEntity> {
            val torqAges = listOf(24, 38, 57)
            val torq = Demo.torqReviews.mapIndexed { i, r ->
                review("torq", OWNER, r.author.handle, torqAges[i], r.stars, r.body, photos = r.photos, response = r.ownerResponse)
            }
            return torq + listOf(
                // Nirmal's own, for the rentals shown as rated.
                review("trek", OWNER, ME, 16, 5, "Bike was set up for my height before I arrived. Brakes and gears spot on.", "GL-5X8T1"),
                review("kayak", OWNER, ME, 50, 4, "Stable and easy to paddle. The seat back is a bit loose, but Kavitha told me before I took it.", "GL-2H6Q3"),
                review("nsp", Demo.ayesha.handle, ME, 20, 5, "Pumped up in five minutes and tracked straight. Ayesha met me at the lagoon with the pump.", "GL-3P9V2"),
                // Other renters, of other listings.
                review("trek", OWNER, Demo.thusara.handle, 36, 5, "Comfortable on the coast road to Point Pedro and back. Lights and lock included, which helped."),
                review("helmet", OWNER, Demo.dinesh.handle, 25, 5, "True to size and the MIPS liner is clean. Kavitha had it sanitised before handover."),
                review("wetsuit", OWNER, Demo.thusara.handle, 31, 5, "Warm enough for the early sessions and no wear on the seams."),
                review("nsp", Demo.ayesha.handle, Demo.thusara.handle, 22, 5, "Stable enough to kneel on. Came with a pump and a dry bag."),
                review("tent", Demo.thusara.handle, Demo.ayesha.handle, 14, 5, "Roomy for two with our bags inside. Pitched it in ten minutes."),
                review("rack", Demo.dinesh.handle, Demo.thusara.handle, 28, 5, "Every draw racked and labelled, slings recently replaced. Dinesh talked me through the cams."),
                // About Nirmal, as a renter: listed when he opens his own profile.
                review("nsp", ME, Demo.ayesha.handle, 19, 5, "Careful with the board, returned it early.", "GL-3P9V2"),
                review("trek", ME, OWNER, 15, 5, "Brought the Marlin back clean and on time. Welcome back any time."),
                review("kayak", ME, OWNER, 49, 5, "Rinsed it before returning it, which almost nobody does."),
                // The wider marketplace.
                review("longboard", Marketplace.sanjay.handle, Demo.ayesha.handle, 12, 5, "Glides forever on the small days. Sanjay waxed it before I came."),
                review("foamie", Marketplace.priya.handle, Demo.rizwan.handle, 9, 5, "My first time standing up. Couldn't have asked for an easier board."),
                review("isup", Marketplace.rohan.handle, Marketplace.meera.handle, 6, 4, "Great board. Pump is slow, so give yourself ten minutes."),
                review("roadbike", Marketplace.sanjay.handle, Marketplace.arun.handle, 17, 5, "Fast and set up properly. Sanjay even adjusted the saddle height for me."),
                review("ebike", Marketplace.meera.handle, Demo.thusara.handle, 8, 5, "Did 60 km on one charge. Lovely way to see the peninsula."),
                review("packtent", Marketplace.tharsika.handle, Demo.dinesh.handle, 20, 5, "Light, dry and up in no time. Perfect for Delft."),
                review("snorkelset", Marketplace.fathima.handle, Marketplace.priya.handle, 5, 5, "Mask sealed perfectly and everything was spotless."),
                review("cricketkit", Marketplace.sanjay.handle, Demo.rizwan.handle, 26, 4, "Everything we needed for the weekend match. Gloves a little big for me."),
                review("harness", Demo.dinesh.handle, Marketplace.arun.handle, 11, 5, "Inspection dates on the tag, which is exactly what you want to see."),
                review("gpswatch", Marketplace.priya.handle, Marketplace.tharsika.handle, 3, 5, "Tried it for a week before buying my own. Saved me from the wrong model."),
            )
        }

        // -------------------------------------------------------- trips

        fun trips(): List<TripEntity> = (members + admin).map { p ->
            if (p.handle == ME) TripEntity(ME, Demo.TRIP_DESTINATION, day(9).toEpochDay(), day(12).toEpochDay(), 2, listOf("Surfing", "Camping"))
            else newTrip(p.handle)
        }

        fun tripItems(): List<TripItemEntity> =
            Demo.tripAlreadyOwn.mapIndexed { i, (name, note) ->
                TripItemEntity(handle = ME, section = "OWN", label = name, listingId = null, note = note, price = 0, added = false, position = i)
            } + Demo.tripRentLocally.mapIndexed { i, (item, who) ->
                TripItemEntity(handle = ME, section = "RENT", label = item.shortTitle, listingId = item.id, note = who, price = 0, added = true, position = i)
            } + (members + admin).flatMap { tripExtras(it.handle) }

        // -------------------------------------------------------- bookings

        private val listingsById: Map<String, GearItem> by lazy { allListings().associateBy { it.id } }

        private fun itemWithDetails(id: String): GearItem {
            val base = listingsById.getValue(id)
            return base.copy(extras = details[id]?.extras.orEmpty())
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
            handshake: Long? = null,
            signed: Long? = null,
            returnedAt: Long? = null,
        ): BookingEntity {
            val item = itemWithDetails(listingId)
            val days = (end.toEpochDay() - start.toEpochDay() + 1).toInt()
            val quote = Pricing.quote(item, days, protection, false, false)
            return BookingEntity(
                ref = ref, listingId = listingId, unit = unit, renterHandle = renter, ownerHandle = item.owner.handle,
                startDay = start.toEpochDay(), endDay = end.toEpochDay(), protection = protection, includeExtras = false,
                coupon = quote.coupon, paymentMethod = 0, waiverName = null, waiverAt = null, status = status,
                total = quote.total, depositHold = quote.depositHold, ownerPayout = quote.ownerPayout,
                handoverCode = handoverCode(ref), pickupTime = pickupTime, returnTime = returnTime, respondBy = respondBy,
                checklist = emptyList(), returnChecklist = emptyList(), handshakeRenterAt = handshake, handshakeOwnerAt = handshake,
                renterSignedAt = signed, ownerSignedAt = signed, renterSignature = null, ownerSignature = null,
                returnedAt = returnedAt, rating = rating, calendarAdded = false, hiddenFromHistory = false,
                createdAt = createdAt, updatedAt = createdAt,
            )
        }

        fun bookings(): List<BookingEntity> {
            // Overdue since six hours ago, and the same unit goes out again in two
            // hours: the conflict the owner calendar surfaces and "Move to unit #2" resolves.
            val dueBack = now.minusHours(6)
            val nextOut = now.plusHours(2)
            return listOf(
                // ---- Nirmal's own
                booking("GL-6B1P2", "nsp", ME, day(7), day(9), BookingStatus.CONFIRMED, createdAt = at(day(-4), 18, 30)),
                booking("GL-9R2K4", "tent", ME, day(17), day(20), BookingStatus.CONFIRMED, createdAt = at(day(-3), 20, 5)),
                booking("GL-5X8T1", "trek", ME, day(-18), day(-16), BookingStatus.COMPLETED, rating = 5, createdAt = at(day(-24), 9, 0), handshake = at(day(-18), 9, 0), signed = at(day(-18), 9, 5), returnedAt = at(day(-16), 16, 10)),
                booking("GL-4M0N7", "bat", ME, day(-34), day(-33), BookingStatus.COMPLETED, createdAt = at(day(-38), 12, 0), handshake = at(day(-34), 8, 30), signed = at(day(-34), 8, 35), returnedAt = at(day(-33), 18, 0)),
                booking("GL-2H6Q3", "kayak", ME, day(-52), day(-50), BookingStatus.COMPLETED, rating = 4, createdAt = at(day(-58), 10, 0), handshake = at(day(-52), 7, 0), signed = at(day(-52), 7, 5), returnedAt = at(day(-50), 17, 30)),
                // Back two hours early: the rental Ayesha's "returned it early" review is about.
                booking("GL-3P9V2", "nsp", ME, day(-21), day(-20), BookingStatus.COMPLETED, rating = 5, createdAt = at(day(-25), 19, 0), handshake = at(day(-21), 9, 0), signed = at(day(-21), 9, 4), returnedAt = at(day(-20), 15, 0)),

                // ---- the owner's side: Kavitha's gear, other renters
                booking("GL-3D9X2", "torq", Demo.dinesh.handle, day(9), day(11), BookingStatus.CONFIRMED, Protection.PLUS, createdAt = at(day(-2), 11, 0)),
                booking("GL-8N4C7", "torq", Demo.thusara.handle, day(30), day(32), BookingStatus.CONFIRMED, Protection.PLUS, createdAt = at(day(-1), 15, 20)),
                booking(
                    "GL-5P7R4", "trek", Demo.ayesha.handle, dueBack.toLocalDate().minusDays(3), dueBack.toLocalDate(), BookingStatus.ACTIVE,
                    unit = 1, returnTime = hhmm(dueBack), createdAt = at(day(-6), 9, 0), handshake = at(dueBack.toLocalDate().minusDays(3), 9, 0), signed = at(dueBack.toLocalDate().minusDays(3), 9, 4),
                ),
                booking("GL-4C2J8", "trek", Demo.dinesh.handle, nextOut.toLocalDate(), nextOut.toLocalDate().plusDays(1), BookingStatus.CONFIRMED, unit = 1, pickupTime = hhmm(nextOut), createdAt = at(day(-4), 14, 0)),
                booking("GL-6T1Y8", "trek", Demo.thusara.handle, day(12), day(14), BookingStatus.CONFIRMED, unit = 2, createdAt = at(day(-2), 19, 0)),
                booking("GL-1W5Z3", "wetsuit", Demo.rizwan.handle, day(-2), day(7), BookingStatus.ACTIVE, createdAt = at(day(-7), 10, 0), handshake = at(day(-2), 9, 0), signed = at(day(-2), 9, 2)),

                // ---- requests waiting on Kavitha, each with its own clock
                booking("GL-2K8R5", "kayak", Demo.thusara.handle, day(7), day(9), BookingStatus.REQUESTED, respondBy = ahead(3 * 60 + 12), createdAt = ago(20 * 60 + 48)),
                booking("GL-7Q3M1", "trek", Demo.rizwan.handle, day(10), day(11), BookingStatus.REQUESTED, unit = 1, respondBy = ahead(9 * 60 + 40), createdAt = ago(14 * 60 + 20)),
                booking("GL-9F4T6", "kayak", Demo.ayesha.handle, day(15), day(19), BookingStatus.REQUESTED, respondBy = ahead(28 * 60), createdAt = ago(84)),

                // ---- the wider marketplace has a life of its own
                booking("GL-4R8T2", "longboard", Demo.ayesha.handle, day(-12), day(-11), BookingStatus.COMPLETED, createdAt = at(day(-15), 8, 0), handshake = at(day(-12), 8, 0), signed = at(day(-12), 8, 5), returnedAt = at(day(-11), 16, 0)),
                booking("GL-7E2B5", "ebike", Demo.thusara.handle, day(-8), day(-8), BookingStatus.COMPLETED, createdAt = at(day(-10), 18, 0), handshake = at(day(-8), 8, 0), signed = at(day(-8), 8, 3), returnedAt = at(day(-8), 19, 0)),
                booking("GL-2S9K1", "isup", Marketplace.meera.handle, day(2), day(3), BookingStatus.CONFIRMED, createdAt = at(day(-1), 9, 30)),
                booking("GL-8P3D6", "roadbike", Marketplace.arun.handle, day(4), day(6), BookingStatus.CONFIRMED, Protection.PLUS, unit = 1, createdAt = at(day(-2), 21, 0)),
            )
        }

        fun blocks(): List<CalendarBlockEntity> = listOf(
            CalendarBlockEntity(listingId = "torq", unit = 1, day = day(19).toEpochDay(), reason = "Fin box repair"),
            CalendarBlockEntity(listingId = "torq", unit = 1, day = day(20).toEpochDay(), reason = "Fin box repair"),
        )

        // -------------------------------------------------------- money

        fun walletTxns(): List<WalletTxnEntity> = listOf(
            WalletTxnEntity(handle = ME, label = "Payout · Burton Custom 158", amount = 6_400, createdAt = at(day(-5), 10, 0)),
            WalletTxnEntity(handle = ME, label = "Booking GL-5X8T1 · Trek Marlin 7", amount = -5_130, createdAt = at(day(-16), 9, 0)),
            WalletTxnEntity(handle = ME, label = "Payout · Burton Custom 158", amount = 7_800, createdAt = at(day(-24), 10, 0)),
            WalletTxnEntity(handle = OWNER, label = "Payout · GL-5X8T1 · Trek Marlin 7", amount = 4_617, createdAt = at(day(-16), 18, 0)),
            WalletTxnEntity(handle = OWNER, label = "Payout · GL-2H6Q3 · Pelican Argo 100X", amount = 5_832, createdAt = at(day(-50), 18, 0)),
            WalletTxnEntity(handle = OWNER, label = "Withdrawal to bank", amount = -10_449, createdAt = at(day(-40), 9, 0)),
        )

        private fun note(handle: String, kind: NotificationItem.Kind, title: String, body: String, at: Long, route: String, ref: String? = null, unread: Boolean = false) =
            NotificationEntity(handle = handle, kind = kind, title = title, body = body, unread = unread, createdAt = at, route = route, ref = ref)

        fun notifications(): List<NotificationEntity> = listOf(
            note(ME, NotificationItem.Kind.SAFETY, "Your leash is due for replacement", "PPE logged 14 months ago. Leashes are rated for 12 months of regular use.", ago(59), "s36", unread = true),
            note(ME, NotificationItem.Kind.SWAP, "Dinesh countered your swap", "Santa Cruz Chameleon M · round 2 of 5 · expires in 5 d", ago(18 * 60 + 30), "s22", Demo.SWAP_REF),
            note(ME, NotificationItem.Kind.PAYOUT, "LKR 6,400 paid into your wallet", "Burton Custom 158 · rental payout", ago(5 * 24 * 60), "s35"),
            note(ME, NotificationItem.Kind.REVIEW, "Ayesha left you a review", "★ 5 — “Careful with the board, returned it early.”", ago(19 * 24 * 60), "s32"),
            note(OWNER, NotificationItem.Kind.BOOKING, "New request from Ayesha", "Pelican Argo 100X · 5 days · respond within 28 h", ago(84), "s28", "GL-9F4T6", unread = true),
            note(OWNER, NotificationItem.Kind.BOOKING, "New request from Mohamed", "Trek Marlin 7 · 2 days · respond within 10 h", ago(14 * 60 + 20), "s28", "GL-7Q3M1", unread = true),
            note(OWNER, NotificationItem.Kind.BOOKING, "New request from Thusara", "Pelican Argo 100X · 3 days · respond within 3 h", ago(20 * 60 + 48), "s28", "GL-2K8R5"),
            note(OWNER, NotificationItem.Kind.SAFETY, "Trek Marlin #1 is overdue", "Ayesha was due back 6 h ago, and Dinesh collects it in 2 h", ago(30), "s27", "GL-5P7R4", unread = true),
            note(Demo.dinesh.handle, NotificationItem.Kind.SWAP, "Nirmal sent you a swap proposal", "Burton Custom 158 for your Santa Cruz Chameleon", ago(2 * 24 * 60), "s22", Demo.SWAP_REF),
        )

        private fun dm(a: String, b: String) = MoneyRepository.dmKey(a, b)

        fun messages(): List<MessageEntity> {
            val enquiry = day(-2)
            val swapDay = day(-1)
            return listOf(
                MessageEntity(threadId = dm(ME, OWNER), fromHandle = ME, body = "Hi Kavitha! Is the Torq OK for someone who has surfed a few times?", kind = ChatMessage.Kind.TEXT, createdAt = at(enquiry, 18, 20)),
                MessageEntity(threadId = dm(ME, OWNER), fromHandle = OWNER, body = "Hi Nirmal — yes, it's a soft top and very forgiving. Book whenever suits you.", kind = ChatMessage.Kind.TEXT, createdAt = at(enquiry, 18, 41)),
                MessageEntity(threadId = dm(ME, OWNER), fromHandle = OWNER, body = "Call me on 077 ••• ••••", kind = ChatMessage.Kind.REDACTED, createdAt = at(enquiry, 18, 42)),
                MessageEntity(threadId = dm(ME, OWNER), fromHandle = ME, body = "Perfect, thanks. I'll book it for the weekend.", kind = ChatMessage.Kind.TEXT, createdAt = at(enquiry, 18, 50)),
                MessageEntity(threadId = dm(Demo.thusara.handle, OWNER), fromHandle = Demo.thusara.handle, body = "Hi Kavitha, is the Pelican OK for a beginner? I've only paddled twice.", kind = ChatMessage.Kind.TEXT, createdAt = ago(20 * 60 + 40)),
                MessageEntity(threadId = dm(Demo.ayesha.handle, OWNER), fromHandle = Demo.ayesha.handle, body = "Sorry, running late with the Marlin — back within the hour.", kind = ChatMessage.Kind.TEXT, createdAt = ago(25)),
            ) + Demo.swapThread.mapIndexed { i, m ->
                MessageEntity(threadId = Demo.SWAP_REF, fromHandle = if (m.fromMe) ME else Demo.dinesh.handle, body = m.body, kind = m.kind, createdAt = at(swapDay, 14, 2 + i * 21))
            }
        }

        /** The claims desk's queue: other members' claims, so it is never empty. */
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
            ref = ref, bookingRef = null, listingTitle = title, claimantHandle = claimant, respondentHandle = respondent,
            amount = amount, itemValue = value, severity = severity, area = area, note = note, status = ClaimStatus.CONTESTED,
            createdAt = ago(ageHours * 60), respondBy = ago(ageHours * 60 - 5L * 24 * 60),
            renterStatement = "The mark was there at pickup.", renterEvidence = null, renterPhotos = emptyList(),
            award = null, rationale = null, decidedAt = null, decidedBy = null,
        )

        // -------------------------------------------------------- trust

        fun verificationSteps(): List<VerificationStepEntity> = (members + admin).flatMap { p ->
            val done = stats.getValue(p.handle).verified
            stepTitles.mapIndexed { i, (title, unlocks) ->
                VerificationStepEntity(
                    handle = p.handle, position = i, title = title, unlocks = unlocks,
                    state = when {
                        i < done -> VerificationStep.State.DONE
                        i == done -> VerificationStep.State.CURRENT
                        else -> VerificationStep.State.LOCKED
                    },
                    completedAt = if (i < done) ago(60L * 24 * (90 - i * 7)) else null,
                    detail = when {
                        i == 0 -> emailOf(p.handle)
                        title == VerificationStep.HOME_ADDRESS && i < done -> MoneyRepository.pickupAddress(p.handle)
                        else -> null
                    },
                )
            }
        }

        fun badges(): List<BadgeEntity> = (members + admin).flatMap { p ->
            val s = stats.getValue(p.handle)
            Demo.badges.mapIndexed { i, (name, _) ->
                val earned = when (name) {
                    "First loop" -> s.completed >= 1
                    "10 rentals" -> s.completed >= 10
                    "Zero damage" -> s.completed >= 1 && s.onTime == 100
                    "Gold host" -> p.tier == TrustTier.GOLD && p.handle != ADMIN
                    else -> false
                }
                BadgeEntity(handle = p.handle, name = name, earned = earned, featured = earned && i == 0, position = i)
            }
        }

        // -------------------------------------------------------- exchange

        fun matches(): List<SwapMatchEntity> = Demo.swapMatches.mapIndexed { i, m ->
            SwapMatchEntity(
                id = "m${i + 1}", handle = ME, giveListingId = m.give.id, getListingId = m.get.id,
                giveValue = m.giveValue, getValue = m.getValue, counterpartyHandle = m.counterparty.handle,
                threeWay = m.threeWay, hidden = false, position = i,
            )
        }

        /** Dinesh came back at LKR 19,000 in round two. It is Nirmal's move. */
        fun proposals(): List<SwapProposalEntity> = listOf(
            SwapProposalEntity(
                ref = Demo.SWAP_REF, matchId = "m1", proposerHandle = ME,
                giveListingId = Demo.burton.id, getListingId = Demo.santaCruz.id,
                giveValue = Demo.BURTON_FMV, getValue = Demo.SANTACRUZ_FMV,
                counterpartyHandle = Demo.dinesh.handle, boot = Demo.BOOT_AMOUNT,
                status = ProposalStatus.COUNTERED, round = 2,
                expiresAt = ahead(5L * 24 * 60 + 6 * 60), createdAt = at(day(-2), 16, 0), updatedAt = at(day(-1), 14, 44),
            ),
        )

        /**
         * The three-way loop: Nirmal's Burton goes to Dinesh, Dinesh's Santa Cruz
         * to Ayesha, Ayesha's own sit-on-top kayak to Nirmal. Dinesh has accepted.
         */
        fun cycle(): List<CycleMemberEntity> = listOf(
            CycleMemberEntity(CYCLE_ID, ME, Demo.burton.id, Demo.BURTON_FMV, null, 0),
            CycleMemberEntity(CYCLE_ID, Demo.dinesh.handle, Demo.santaCruz.id, Demo.SANTACRUZ_FMV, true, 1),
            CycleMemberEntity(CYCLE_ID, Demo.ayesha.handle, Demo.ayeshaKayak.id, Demo.AYESHA_KAYAK_FMV, null, 2),
        )

        // -------------------------------------------------------- owner

        fun tasks(): List<OwnerTaskEntity> = listOf(
            OwnerTaskEntity("service", OWNER, NeedsAttention.Severity.SOON, "Service due in 3 days", "Trek Marlin #2 · block ${dayMonth(day(7))} for it", "Block dates", false, 0),
            OwnerTaskEntity("review", OWNER, NeedsAttention.Severity.SOON, "Review to write", "Mohamed Rizwan · expires in 4 days", "Write it", false, 1),
        )

        /** The day the "service" task blocks. */
        val serviceDay: LocalDate get() = day(7)

        fun closet(): List<ClosetItemEntity> {
            fun item(g: GearItem, listed: Boolean, idleDays: Long, pos: Int) = ClosetItemEntity(
                ownerHandle = ME, title = g.title, shortTitle = g.shortTitle, photoKey = g.hero, sport = g.sport,
                sizeLabel = g.sizeLabel, grade = g.grade, code = g.code, listed = listed, listingId = null,
                idleSince = ago(idleDays * 24 * 60), position = pos,
            )
            // Nirmal's own gear, and only his. The three listed ones are the three he
            // has listed for swap, so the closet and the swap screens offer the same things.
            return listOf(
                item(Demo.burton, true, 6, 0).copy(listingId = Demo.burton.id),
                item(Demo.myHelmet, true, 20, 1).copy(listingId = Demo.myHelmet.id),
                item(Demo.myKayak, false, 94, 2),
                item(Demo.myCampKit, true, 12, 3).copy(listingId = Demo.myCampKit.id),
                item(Demo.myBat, false, 33, 4),
            )
        }

    }
}
