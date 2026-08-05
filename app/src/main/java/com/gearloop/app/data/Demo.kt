package com.gearloop.app.data

import com.gearloop.app.R

/**
 * The dataset from design brief §5 — one source of truth for every screen.
 *
 * The brief is blunt about why this matters: consistency across screens is what
 * makes the work look professional. The Torq costs LKR 2,800 a day on all
 * eleven screens it appears on because it is written down exactly once.
 */
object Demo {

    // ---------------------------------------------------------------- people

    /** The renter — the demo user. 174 cm, 68 kg, shoe EU 42. */
    val nirmal = Person(
        name = "Nirmal Ravindran",
        handle = "@nirmal_r",
        area = "Jaffna",
        tier = TrustTier.SILVER,
        rating = 4.8,
        reviews = 14,
        respondsIn = "~1 h",
        memberSince = "Mar 2025",
    )

    val kavitha = Person(
        name = "Kavitha Selvam",
        handle = "@kavitha_s",
        area = "Nallur, Jaffna",
        tier = TrustTier.GOLD,
        rating = 4.94,
        reviews = 37,
        respondsIn = "~40 m",
        listings = 6,
        memberSince = "Jan 2024",
    )

    val dinesh = Person("Dinesh Kumar", "@dinesh_k", "Gurunagar", TrustTier.SILVER, 4.7, 21, "~2 h", 3)
    val thusara = Person("Thusara Perera", "@thusara_p", "Point Pedro", TrustTier.SILVER, 4.7, 12, "~3 h", 1)
    val ayesha = Person("Ayesha Fernando", "@ayesha_f", "Casuarina Beach", TrustTier.GOLD, 4.9, 44, "~25 m", 4)
    val rizwan = Person("Mohamed Rizwan", "@m_rizwan", "Arugam Bay", TrustTier.BRONZE, 4.5, 6, "~5 h", 2)

    /** Nirmal's fit profile — the two numbers the whole product argument rests on. */
    const val HEIGHT_CM = 174
    const val WEIGHT_KG = 68
    const val SHOE_EU = 42

    // --------------------------------------------------------------- the gear

    /** The hero item. Every number here is quoted somewhere in the viva script. */
    val torq = GearItem(
        id = "torq",
        title = "Torq Mod Fish 6'6\" Softtop",
        shortTitle = "Torq Mod Fish 6'6\"",
        code = "TQ-4471",
        sizeLabel = "6'6\"",
        grade = Grade.B,
        pricePerDay = 2_800,
        distanceKm = 2.4,
        photos = listOf(
            R.drawable.photo_torq_1,
            R.drawable.photo_torq_2,
            R.drawable.photo_torq_3,
            R.drawable.photo_torq_4,
            R.drawable.photo_torq_5,
        ),
        owner = kavitha,
        fit = Fit.PERFECT,
        sport = "Surfing",
        instantBook = true,
        delivery = true,
        deposit = 25_000,
    )

    val trekMarlin = GearItem(
        id = "trek",
        title = "Trek Marlin 7 — Size M",
        shortTitle = "Trek Marlin 7",
        code = "TM-8802",
        sizeLabel = "M 52",
        grade = Grade.A,
        pricePerDay = 1_900,
        distanceKm = 1.1,
        photos = listOf(R.drawable.photo_trek_marlin),
        owner = kavitha,
        fit = Fit.GOOD,
        sport = "Cycling",
        deposit = 18_000,
    )

    val nspSup = GearItem(
        id = "nsp",
        title = "NSP Elements SUP 10'6\"",
        shortTitle = "NSP Elements SUP",
        code = "NS-3310",
        sizeLabel = "10'6\"",
        grade = Grade.B,
        pricePerDay = 3_200,
        distanceKm = 4.7,
        photos = listOf(R.drawable.photo_nsp_sup),
        owner = ayesha,
        fit = Fit.GOOD,
        sport = "SUP",
        instantBook = true,
        deposit = 22_000,
    )

    val quechuaTent = GearItem(
        id = "tent",
        title = "Decathlon Quechua MH500 Tent 3P",
        shortTitle = "Quechua MH500 3P",
        code = "QU-2071",
        sizeLabel = "3P",
        grade = Grade.B,
        pricePerDay = 1_400,
        distanceKm = 3.2,
        photos = listOf(R.drawable.photo_quechua_tent),
        owner = thusara,
        fit = Fit.UNKNOWN,
        sport = "Camping",
        deposit = 9_000,
    )

    val ssBat = GearItem(
        id = "bat",
        title = "SS Ton Kashmir Willow Bat SH",
        shortTitle = "SS Ton Willow Bat",
        code = "SS-1195",
        sizeLabel = "SH",
        grade = Grade.A,
        pricePerDay = 650,
        distanceKm = 0.8,
        photos = listOf(R.drawable.photo_ss_bat),
        owner = rizwan,
        fit = Fit.PERFECT,
        sport = "Cricket",
        instantBook = true,
        delivery = true,
        deposit = 4_000,
    )

    /** Unavailable listing — the unhappy path has to be designed too. */
    val wetsuit = GearItem(
        id = "wetsuit",
        title = "O'Neill Reactor 2 mm Shorty — M",
        shortTitle = "O'Neill Reactor 2 mm",
        code = "ON-6620",
        sizeLabel = "M 2MM",
        grade = Grade.B,
        pricePerDay = 900,
        distanceKm = 2.4,
        photos = listOf(R.drawable.photo_wetsuit),
        owner = kavitha,
        fit = Fit.GOOD,
        sport = "Surfing",
        available = false,
        nextFree = "NEXT FREE 14 AUG",
        deposit = 6_000,
    )

    val helmet = GearItem(
        id = "helmet",
        title = "Giro Fixture MIPS Helmet — L",
        shortTitle = "Giro Fixture MIPS",
        code = "GR-5140",
        sizeLabel = "L 59-63",
        grade = Grade.A,
        pricePerDay = 400,
        distanceKm = 1.1,
        photos = listOf(R.drawable.photo_helmet),
        owner = kavitha,
        fit = Fit.WORKABLE,
        sport = "Cycling",
        deposit = 3_000,
    )

    val kayak = GearItem(
        id = "kayak",
        title = "Pelican Argo 100X Kayak",
        shortTitle = "Pelican Argo 100X",
        code = "PL-7734",
        sizeLabel = "10'0\"",
        grade = Grade.B,
        pricePerDay = 2_400,
        distanceKm = 5.9,
        photos = listOf(R.drawable.photo_kayak),
        owner = kavitha,
        fit = Fit.GOOD,
        sport = "Kayaking",
        deposit = 16_000,
        delivery = true,
    )

    val climbingRack = GearItem(
        id = "rack",
        title = "Trad Rack — 12 Quickdraws + Cams",
        shortTitle = "Trad rack · 12 draws",
        code = "CR-0918",
        sizeLabel = "12 DRAW",
        grade = Grade.B,
        pricePerDay = 1_800,
        distanceKm = 6.3,
        photos = listOf(R.drawable.photo_climbing_rack),
        owner = dinesh,
        fit = Fit.UNKNOWN,
        sport = "Climbing",
        deposit = 20_000,
    )

    /** Nirmal's own board — the thing he swaps with. */
    val burton = GearItem(
        id = "burton",
        title = "Burton Custom 158",
        shortTitle = "Burton Custom 158",
        code = "BC-1580",
        sizeLabel = "158",
        grade = Grade.B,
        pricePerDay = 2_600,
        distanceKm = 0.0,
        photos = listOf(R.drawable.photo_burton_board),
        owner = nirmal,
        fit = Fit.GOOD,
        sport = "Snowboarding",
        deposit = 20_000,
    )

    val santaCruz = GearItem(
        id = "santacruz",
        title = "Santa Cruz Chameleon — M",
        shortTitle = "Santa Cruz Chameleon M",
        code = "SC-4412",
        sizeLabel = "M 29",
        grade = Grade.B,
        pricePerDay = 3_400,
        distanceKm = 3.6,
        photos = listOf(R.drawable.photo_santacruz),
        owner = dinesh,
        fit = Fit.GOOD,
        sport = "Cycling",
        deposit = 30_000,
    )

    val allListings = listOf(torq, trekMarlin, nspSup, quechuaTent, ssBat, kayak, helmet, climbingRack, wetsuit)

    val weekendNearYou = listOf(torq, trekMarlin, nspSup, ssBat)
    val fitsYou = listOf(torq, ssBat, trekMarlin, nspSup)
    val newInSurfing = listOf(torq, wetsuit, nspSup, kayak)
    val searchResults = listOf(torq, wetsuit, nspSup, kayak, helmet, trekMarlin, climbingRack, quechuaTent)

    // --------------------------------------------------------- listing detail

    val torqSpecs = listOf(
        SpecRow("Length", "6'6\" · 198 cm", fitRelevant = true),
        SpecRow("Volume", "42 L", fitRelevant = true),
        SpecRow("Construction", "Soft top · EPS core"),
        SpecRow("Fin system", "Thruster · FCS II"),
        SpecRow("Tail", "Swallow"),
        SpecRow("Year", "2023"),
    )

    val torqIncluded = listOf("Board", "Fins ×3", "Leash", "Board bag")
    val torqExcluded = listOf("Wax (not included)")

    val torqConditionPhotos = listOf(
        R.drawable.photo_cond_1,
        R.drawable.photo_cond_2,
        R.drawable.photo_cond_3,
    )

    val handoverMethods = listOf(
        Triple("Self pickup", "Nallur · meet the owner", "Free"),
        Triple("Delivery", "Within 5 km of Nallur", lkr(600)),
        Triple("GearPoint Nallur", "Collect any time 08:00–20:00", lkr(200)),
    )

    val torqReviews = listOf(
        ReviewEntry(
            author = ayesha,
            date = "12 Jul 2026",
            stars = 5,
            body = "Board was exactly as described — the dings on the deck are cosmetic and " +
                "Kavitha showed me every one of them before I took it. Paddles easy, caught " +
                "plenty at Casuarina.",
            photos = listOf(R.drawable.photo_torq_3),
        ),
        ReviewEntry(
            author = thusara,
            date = "28 Jun 2026",
            stars = 5,
            body = "First time surfing and this was forgiving. Kavitha threw in a rash guard " +
                "when she heard I didn't have one.",
            ownerResponse = "Enjoy it Thusara — bring it back any time, and rinse the fins!",
        ),
        ReviewEntry(
            author = dinesh,
            date = "9 Jun 2026",
            stars = 4,
            body = "Good board, fair price. Leash was a bit worn but held up fine for three days.",
        ),
    )

    val subRatings = listOf(
        "Accuracy" to 4.98f,
        "Condition" to 4.91f,
        "Communication" to 5.00f,
        "Value" to 4.87f,
    )

    val houseRules = listOf(
        "Free cancellation until 5 Aug" to "After that, 50% refund up to 24 h before pickup.",
        "Rinse with fresh water" to "Salt kills fin boxes. There's a tap at the pickup point.",
        "No commercial lessons" to "Personal use only — teaching for payment needs a different plan.",
    )

    // ------------------------------------------------------------- booking

    const val BOOKING_REF = "GL-7K3M9"
    const val BOOKING_DATES = "8 – 10 Aug"
    const val BOOKING_DATES_LONG = "Sat 8 – Mon 10 Aug 2026"
    const val BOOKING_DAYS = 3
    const val PICKUP = "Nallur · 09:00"

    /** Design brief §6, S-11. These lines add to exactly LKR 9,240. */
    val priceBreakdown = listOf(
        PriceLine("LKR 2,800 × 3 days", grouped(8_400)),
        PriceLine("3-day discount", "−840", PriceLine.Kind.DISCOUNT),
        PriceLine("Leash", "450"),
        PriceLine("Delivery", "—", PriceLine.Kind.MUTED),
        PriceLine("Plus protection", grouped(1_260)),
        PriceLine("Service fee", "801"),
        PriceLine("Coupon FIRSTLOOP", "−831", PriceLine.Kind.DISCOUNT),
        PriceLine("Total", lkr(9_240), PriceLine.Kind.TOTAL),
    )

    const val BOOKING_TOTAL = 9_240
    const val DEPOSIT_HOLD = 25_000

    /** What the listing footer quotes before extras are chosen. */
    const val LISTING_FOOTER_TOTAL = 8_910

    val datePanelLines = listOf(
        PriceLine("LKR 2,800 × 3 days", grouped(8_400)),
        PriceLine("3-day discount", "−840", PriceLine.Kind.DISCOUNT),
        PriceLine("Subtotal", grouped(7_560), PriceLine.Kind.TOTAL),
    )

    val bookingTimeline = listOf(
        TimelineNode("Now", "Kavitha has been notified", TimelineNode.State.DONE),
        TimelineNode("Fri 7 Aug", "Pickup details and exact address unlock", TimelineNode.State.CURRENT),
        TimelineNode("Sat 8 Aug, 09:00", "Meet and check the board together", TimelineNode.State.FUTURE),
        TimelineNode("Mon 10 Aug, 17:00", "Return by this time", TimelineNode.State.FUTURE),
    )

    val waiverChecks = listOf(
        "I can swim 200 m unassisted",
        "I've surfed before, or I'm going with someone who has",
        "I'll check the surf report and conditions before I paddle out",
    )

    val waiverBody = """
        GearLoop Rental Waiver and Assumption of Risk (v2.1)

        1. NATURE OF THE ACTIVITY. Surfing and other water sports carry an inherent
        risk of serious injury or death. Waves, currents, rocks, marine life, other
        water users and the equipment itself can all cause harm. No equipment check
        and no instruction removes that risk.

        2. YOUR DECLARATION. You confirm the statements you ticked above are true,
        that you are medically fit to take part, and that you are not under the
        influence of alcohol or drugs when you use the equipment.

        3. CONDITION OF EQUIPMENT. You and the owner record the condition of the
        equipment together at handover using the GearLoop condition capture. That
        record is what both parties agree the equipment looked like. You must not
        use equipment you believe to be unsafe — stop and report it instead.

        4. YOUR RESPONSIBILITY. You are responsible for assessing conditions before
        each session, for using appropriate safety equipment, and for the equipment
        while it is in your care.

        5. ASSUMPTION OF RISK. You accept the risks described above and agree that
        GearLoop and the owner are not liable for injury or loss arising from your
        participation, except where caused by their negligence.

        6. THIS DOES NOT AFFECT your statutory rights, or any claim relating to
        equipment that was defective when handed to you.

        By typing your name below you confirm you have read and accepted this waiver.
    """.trimIndent()

    // ----------------------------------------------------------- handover

    val handoverChecklist = listOf(
        "Bring photo ID",
        "Bring a way to carry the board",
        "Charge your phone for the handover scan",
    )

    val captureAngles = listOf(
        CaptureAngle("Deck", CaptureAngle.State.CAPTURED, R.drawable.photo_torq_2),
        CaptureAngle("Bottom", CaptureAngle.State.CAPTURED, R.drawable.photo_torq_5),
        CaptureAngle("Nose", CaptureAngle.State.CAPTURED, R.drawable.photo_cond_3),
        CaptureAngle("Tail", CaptureAngle.State.ACTIVE),
        CaptureAngle("Rail L", CaptureAngle.State.PENDING),
        CaptureAngle("Rail R", CaptureAngle.State.PENDING),
        CaptureAngle("Fin box", CaptureAngle.State.PENDING),
    )

    const val CAPTURE_META = "9.7134°N, 80.0246°E · 08 AUG 08:57 · SYNCED"

    val careNotes = listOf(
        "Rinse with fresh water after each session",
        "Don't leave it in direct sun for hours",
        "Store fins-up",
    )

    /** The seven angles again at return, for the side-by-side comparison. */
    val returnComparison = listOf(
        Triple("Deck", R.drawable.photo_torq_2, false),
        Triple("Bottom", R.drawable.photo_torq_5, false),
        Triple("Nose", R.drawable.photo_cond_3, false),
        Triple("Tail", R.drawable.photo_cond_1, true),
        Triple("Rail L", R.drawable.photo_torq_4, false),
        Triple("Rail R", R.drawable.photo_torq_4, false),
        Triple("Fin box", R.drawable.photo_cond_2, false),
    )

    // --------------------------------------------------------------- swap

    const val BURTON_FMV = 42_000
    const val SANTACRUZ_FMV = 61_000
    const val BOOT_AMOUNT = 19_000
    const val SWAP_REF = "SW-2214"

    val swapMatches = listOf(
        SwapOffer(burton, santaCruz, BURTON_FMV, SANTACRUZ_FMV, dinesh),
        SwapOffer(burton, nspSup, BURTON_FMV, 38_000, ayesha),
        SwapOffer(burton, kayak, BURTON_FMV, 44_000, kavitha),
        SwapOffer(burton, climbingRack, BURTON_FMV, 29_000, dinesh, threeWay = true),
        SwapOffer(burton, trekMarlin, BURTON_FMV, 46_500, thusara),
    )

    val burtonValuation = listOf(
        "MSRP" to grouped(78_000),
        "Age 3 yr" to "−38%",
        "Grade B" to "×0.85",
        "FMV" to grouped(BURTON_FMV),
    )

    val santaCruzValuation = listOf(
        "MSRP" to grouped(96_000),
        "Age 2 yr" to "−26%",
        "Grade B" to "×0.85",
        "FMV" to grouped(SANTACRUZ_FMV),
    )

    val swapTerms = listOf(
        "Type" to "Permanent",
        "Inspection window" to "48 hours",
        "Ownership" to "Transfers on completion",
        "Escrow" to "Holds your ${lkr(BOOT_AMOUNT)} until both confirm",
    )

    val swapThread = listOf(
        ChatMessage("Board's in good shape, only ridden it two seasons. Would you go 15k?", "Tue 14:02", fromMe = false),
        ChatMessage("15 is under the valuation — I'd do 19 and I'll throw in the board bag.", "Tue 14:20", fromMe = true),
        ChatMessage("Deal if you can meet at Gurunagar on Saturday.", "Tue 14:44", fromMe = false),
    )

    // -------------------------------------------------------------- owner

    const val EARNINGS_MONTH = 47_200
    const val NEXT_PAYOUT = 12_400
    val earningsSpark = listOf(0.32f, 0.41f, 0.28f, 0.55f, 0.49f, 0.71f, 0.63f, 0.88f, 0.79f, 1f)

    val needsYouNow = listOf(
        NeedsAttention(
            NeedsAttention.Severity.URGENT,
            "Request expires in 3 h",
            "Thusara · NSP Elements SUP · 12–14 Aug",
            "Review",
        ),
        NeedsAttention(
            NeedsAttention.Severity.URGENT,
            "Return overdue by 6 h",
            "Trek Marlin 7 · Ayesha Fernando",
            "Contact renter",
        ),
        NeedsAttention(
            NeedsAttention.Severity.SOON,
            "Service due in 3 days",
            "Trek Marlin #2 · calendar blocks 12 Aug",
            "Block dates",
        ),
        NeedsAttention(
            NeedsAttention.Severity.SOON,
            "Review to write",
            "Mohamed Rizwan · expires in 4 days",
            "Write it",
        ),
    )

    val todayRows = listOf(
        Triple("09:00", "Pickup · Nirmal Ravindran", "Torq Mod Fish"),
        Triple("17:00", "Return · Ayesha Fernando", "NSP Elements SUP"),
    )

    val ownerInsights = listOf(
        InsightItem(
            "Listings with 8+ photos get 2.3× more bookings",
            "Your Torq Mod Fish has 4. Adding the fin box and rail shots takes about two minutes.",
            "Add photos",
        ),
        InsightItem(
            "14 people searched for kayaks near you last week",
            "Your Pelican Argo has had 0 bookings in 30 days. It may be priced above the local median.",
            "Review pricing",
        ),
    )

    const val OCCUPANCY = 68
    const val SUGGESTED_PRICE = 2_800
    val priceBand = 2_400 to 3_200
    val durationDiscounts = listOf(
        Triple("3+ days", "−10%", true),
        Triple("7+ days", "−20%", true),
        Triple("14+ days", "−30%", false),
    )
    val seasonCurve = listOf(0.42f, 0.38f, 0.45f, 0.58f, 0.72f, 0.86f, 0.94f, 1f, 0.88f, 0.66f, 0.5f, 0.44f)

    val calendarUnits = listOf("Torq Mod Fish", "NSP Elements SUP", "Trek Marlin #1", "Trek Marlin #2")

    val requestInbox = listOf(
        Triple(thusara, nspSup, "12 – 14 Aug"),
        Triple(rizwan, trekMarlin, "15 – 16 Aug"),
        Triple(ayesha, kayak, "20 – 24 Aug"),
    )

    // ------------------------------------------------------- account & trust

    const val TRUST_SCORE = 742
    const val POINTS_TO_GOLD = 58

    val scoreFactors = listOf(
        ScoreFactor("Verification", "5 / 6", 0.83f),
        ScoreFactor("Completed rentals", "14", 0.70f),
        ScoreFactor("On-time returns", "100%", 1f),
        ScoreFactor("Damage-free", "100%", 1f),
        ScoreFactor("Response rate", "92%", 0.92f),
        ScoreFactor("Reviews", "★ 4.8", 0.96f),
    )

    val goldUnlocks = listOf(
        "Next-day payouts",
        "Swap without inspection",
        "1% lower commission",
    )

    val badges = listOf(
        "First loop" to true,
        "10 rentals" to true,
        "Zero damage" to true,
        "Swap trader" to false,
        "Super responder" to false,
        "Gold host" to false,
    )

    val verificationSteps = listOf(
        VerificationStep("Email address", "Browse and save listings", VerificationStep.State.DONE),
        VerificationStep("Phone number", "Message owners", VerificationStep.State.DONE),
        VerificationStep("Government ID", "Rent items over LKR 100,000", VerificationStep.State.DONE),
        VerificationStep("Selfie match", "Instant book without approval", VerificationStep.State.DONE),
        VerificationStep("Payment method", "Deposits held, not charged", VerificationStep.State.DONE),
        VerificationStep("Home address", "Accept delivery and list your own gear", VerificationStep.State.CURRENT),
    )

    val walletHolds = listOf(
        WalletHold(25_000, "Torq Mod Fish 6'6\" · GL-7K3M9", "Releases 11 Aug"),
        WalletHold(9_000, "Quechua MH500 3P · GL-6B1P2", "Releases 19 Aug"),
    )

    val walletTxns = listOf(
        WalletTxn("Booking GL-7K3M9 · Torq Mod Fish", "5 Aug", -9_240),
        WalletTxn("Payout · NSP Elements SUP", "31 Jul", 6_400),
        WalletTxn("Deposit released · GL-5X8T1", "24 Jul", 12_000),
        WalletTxn("Booking GL-5X8T1 · Trek Marlin 7", "20 Jul", -5_130),
        WalletTxn("Payout · Burton Custom 158", "12 Jul", 7_800),
    )

    val notifications = listOf(
        NotificationItem(
            NotificationItem.Kind.BOOKING,
            "Kavitha confirmed your booking",
            "Torq Mod Fish 6'6\" · 8–10 Aug · $BOOKING_REF",
            "09:42", unread = true,
        ),
        NotificationItem(
            NotificationItem.Kind.SAFETY,
            "Your leash is due for replacement",
            "PPE logged 14 months ago. Leashes are rated for 12 months of regular use.",
            "08:15", unread = true,
        ),
        NotificationItem(
            NotificationItem.Kind.SWAP,
            "Dinesh countered your swap",
            "Santa Cruz Chameleon M · round 2 of 5 · expires in 5 d",
            "Yesterday",
        ),
        NotificationItem(
            NotificationItem.Kind.PAYOUT,
            "LKR 12,400 is on its way",
            "Payout for July arrives Fri 31 Jul",
            "Mon",
        ),
        NotificationItem(
            NotificationItem.Kind.REVIEW,
            "Ayesha left you a review",
            "★ 5 — “Careful with the board, returned it early.”",
            "Mon",
        ),
    )

    val chatThread = listOf(
        ChatMessage("Booking confirmed · $BOOKING_REF", "Tue 09:42", fromMe = false, kind = ChatMessage.Kind.SYSTEM),
        ChatMessage("Hi Kavitha! Looking forward to Saturday. Is 09:00 still good?", "Tue 09:44", fromMe = true),
        ChatMessage("Morning Nirmal — 09:00 works. I'll be by the temple gate.", "Tue 10:05", fromMe = false),
        ChatMessage("Call me on 077 ••• ••••", "Tue 10:06", fromMe = false, kind = ChatMessage.Kind.REDACTED),
        ChatMessage("Pickup point · 27 Temple Rd, Nallur", "Tue 10:07", fromMe = false, kind = ChatMessage.Kind.LOCATION),
        ChatMessage("Perfect. I'll bring my own wax.", "Tue 10:12", fromMe = true),
    )

    val quickReplies = listOf("On my way", "Running 10 min late", "Thanks!")

    // ------------------------------------------------------------ discovery

    val sports = listOf(
        "Surfing" to R.drawable.sport_surfing,
        "Cycling" to R.drawable.sport_cycling,
        "SUP" to R.drawable.sport_sup,
        "Cricket" to R.drawable.sport_cricket,
        "Trekking" to R.drawable.sport_trekking,
        "Camping" to R.drawable.sport_camping,
        "Swimming" to R.drawable.sport_swimming,
        "Running" to R.drawable.sport_running,
        "Climbing" to R.drawable.sport_climbing,
        "Kayaking" to R.drawable.sport_kayaking,
        "Badminton" to R.drawable.sport_badminton,
        "Football" to R.drawable.sport_football,
    )

    val homeFilters = listOf("Available today", "Instant book", "Fits me", "Under LKR 2,000", "Swap")

    /** Histogram behind the price slider on the filter sheet. */
    val priceHistogram = listOf(
        0.15f, 0.3f, 0.55f, 0.8f, 1f, 0.92f, 0.74f, 0.61f,
        0.48f, 0.4f, 0.31f, 0.26f, 0.19f, 0.14f, 0.1f, 0.06f,
    )

    // --------------------------------------------------------- trip planner

    const val TRIP_DESTINATION = "Arugam Bay"
    const val TRIP_DATES = "14 – 17 Aug"
    const val TRIP_PARTY = "2 adults"
    const val TRIP_TOTAL = 18_400
    const val TRIP_DISCOUNT = 2_100

    val tripAlreadyOwn = listOf(
        "Burton Custom 158" to "Bring it",
        "Giro Fixture MIPS Helmet" to "Swap locally?",
    )
    val tripRentLocally = listOf(
        torq to "For you",
        nspSup to "For Ayesha",
        quechuaTent to "Shared",
        wetsuit to "For you",
    )
    val tripWorthAdding = listOf(
        "Dry bag 20 L" to lkr(300),
        "Reef-safe sunscreen" to lkr(180),
        "First aid kit" to lkr(250),
    )

    // ---------------------------------------------------------- damage claim

    const val CLAIM_AMOUNT = 14_000
    const val CLAIM_REF = "CL-3081"
}
