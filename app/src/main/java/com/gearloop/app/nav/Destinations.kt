package com.gearloop.app.nav

/**
 * The screen catalogue.
 *
 * The design brief organises 42 screens across ten Figma pages and three tiers.
 * That structure is kept here rather than thrown away at build time, because it
 * drives the in-app screen index — which is what lets a whole file be walked
 * during a viva without hunting for a route.
 */
enum class ScreenGroup(val page: String, val label: String) {
    ONBOARDING("03", "Onboarding"),
    DISCOVERY("04", "Discovery"),
    BOOKING("05", "Booking"),
    HANDOVER("06", "Handover & Rental"),
    EXCHANGE("07", "Exchange"),
    OWNER("08", "Owner"),
    ACCOUNT("09", "Account & Trust"),
    ADMIN("10", "Admin (desktop)"),
}

data class ScreenSpec(
    val id: String,
    val title: String,
    val route: String,
    val group: ScreenGroup,
    val tier: Int,
    /** Screens the brief marks with a ⭐ — the ones to stop on in the viva. */
    val starred: Boolean = false,
    val note: String = "",
)

/**
 * One stop on a walkthrough.
 *
 * The caption is the sentence to say out loud when the screen comes up. Keeping
 * it beside the route rather than in the screen that renders it means the
 * journey board and the guided demo bar cannot drift apart — they read the same
 * words from the same place.
 */
data class FlowStep(val route: String, val caption: String)

data class Flow(
    val id: String,
    val label: String,
    val blurb: String,
    val steps: List<FlowStep>,
) {
    val routes: List<String> get() = steps.map { it.route }
    val size: Int get() = steps.size

    /** Where [route] sits in this flow, or -1 if it isn't part of it. */
    fun indexOf(route: String): Int = steps.indexOfFirst { it.route == route }
}

object Routes {
    const val COVER = "cover"
    const val SCREEN_INDEX = "index"
    const val BOARD_FOUNDATIONS = "board/foundations"
    const val BOARD_COMPONENTS = "board/components"
    const val BOARD_JOURNEY = "board/journey"
    const val BOARD_SIGNATURE = "board/signature"
    const val BOARD_MAP = "board/map"
    const val CREDITS = "credits"
}

object Catalogue {

    val screens = listOf(
        // ---- Tier 1 — the golden path -----------------------------------
        ScreenSpec("S-01", "Intent Selector", "s01", ScreenGroup.ONBOARDING, 1),
        ScreenSpec(
            "S-02", "Fit Profile", "s02", ScreenGroup.ONBOARDING, 1, starred = true,
            note = "Two numbers, and the app knows your board volume, ski length and frame size.",
        ),
        ScreenSpec("S-03", "Sport Interests", "s03", ScreenGroup.ONBOARDING, 1),
        ScreenSpec("S-04", "Home", "s04", ScreenGroup.DISCOVERY, 1),
        ScreenSpec("S-05", "Search Results", "s05", ScreenGroup.DISCOVERY, 1),
        ScreenSpec("S-06", "Filter Sheet", "s06", ScreenGroup.DISCOVERY, 1),
        ScreenSpec(
            "S-07", "Listing Detail", "s07", ScreenGroup.DISCOVERY, 1, starred = true,
            note = "The money screen. Tag, fit verdict, condition evidence, deposit language.",
        ),
        ScreenSpec("S-08", "Date Picker", "s08", ScreenGroup.BOOKING, 1),
        ScreenSpec("S-09", "Protection & Deposit", "s09", ScreenGroup.BOOKING, 1),
        ScreenSpec(
            "S-10", "Safety & Waiver", "s10", ScreenGroup.BOOKING, 1, starred = true,
            note = "Accept stays disabled until the waiver is actually scrolled.",
        ),
        ScreenSpec("S-11", "Review & Pay", "s11", ScreenGroup.BOOKING, 1),
        ScreenSpec("S-12", "Booking Confirmed", "s12", ScreenGroup.BOOKING, 1),
        ScreenSpec("S-13", "Handover Hub", "s13", ScreenGroup.HANDOVER, 1),
        ScreenSpec("S-14", "Scan Handshake", "s14", ScreenGroup.HANDOVER, 1),
        ScreenSpec(
            "S-15", "Condition Capture", "s15", ScreenGroup.HANDOVER, 1, starred = true,
            note = "The trust screen. Seven guided angles, both parties present, geotagged.",
        ),
        ScreenSpec("S-15b", "Damage Marking", "s15b", ScreenGroup.HANDOVER, 1),
        ScreenSpec("S-16", "Sign & Release", "s16", ScreenGroup.HANDOVER, 1),
        ScreenSpec("S-17", "Active Rental", "s17", ScreenGroup.HANDOVER, 1),
        ScreenSpec(
            "S-18", "Return & Settlement", "s18", ScreenGroup.HANDOVER, 1, starred = true,
            note = "The same seven angles side by side. The owner reviews rather than hunts.",
        ),
        ScreenSpec("S-18b", "Evidence Diff", "s18b", ScreenGroup.HANDOVER, 1),

        // ---- Tier 2 — the differentiators --------------------------------
        ScreenSpec(
            "S-19", "Swap Match Feed", "s19", ScreenGroup.EXCHANGE, 2, starred = true,
            note = "Rental is the volume. Exchange is the differentiator.",
        ),
        ScreenSpec("S-20", "Build Proposal", "s20", ScreenGroup.EXCHANGE, 2),
        ScreenSpec(
            "S-21", "Fairness & Boot", "s21", ScreenGroup.EXCHANGE, 2, starred = true,
            note = "Move the boot slider and the beam levels out. The one moment of motion.",
        ),
        ScreenSpec("S-22", "Proposal Detail", "s22", ScreenGroup.EXCHANGE, 2),
        ScreenSpec(
            "S-23", "Cycle Preview", "s23", ScreenGroup.EXCHANGE, 2, starred = true,
            note = "Three people, none of whom could trade directly, all get what they want.",
        ),
        ScreenSpec("S-24", "Owner Dashboard", "s24", ScreenGroup.OWNER, 2),
        ScreenSpec("S-25", "Create Listing — Photos", "s25", ScreenGroup.OWNER, 2),
        ScreenSpec("S-26", "Create Listing — Pricing", "s26", ScreenGroup.OWNER, 2),
        ScreenSpec("S-27", "Master Calendar", "s27", ScreenGroup.OWNER, 2),
        ScreenSpec("S-28", "Requests Inbox", "s28", ScreenGroup.OWNER, 2),
        ScreenSpec("S-29", "Search Map", "s29", ScreenGroup.DISCOVERY, 2),
        ScreenSpec("S-30", "Trip Planner", "s30", ScreenGroup.DISCOVERY, 2),
        ScreenSpec("S-31", "My Rentals", "s31", ScreenGroup.ACCOUNT, 2),
        ScreenSpec("S-32", "Trust Profile", "s32", ScreenGroup.ACCOUNT, 2),

        // ---- Tier 3 — depth ----------------------------------------------
        ScreenSpec("S-33", "Message Thread", "s33", ScreenGroup.ACCOUNT, 3),
        ScreenSpec("S-34", "Damage Claim", "s34", ScreenGroup.ACCOUNT, 3),
        ScreenSpec("S-35", "Wallet & Deposits", "s35", ScreenGroup.ACCOUNT, 3),
        ScreenSpec("S-36", "Gear Closet", "s36", ScreenGroup.ACCOUNT, 3),
        ScreenSpec("S-37", "Public Profile", "s37", ScreenGroup.ACCOUNT, 3),
        ScreenSpec("S-38", "Notifications", "s38", ScreenGroup.ACCOUNT, 3),
        ScreenSpec("S-39", "No Results", "s39", ScreenGroup.DISCOVERY, 3),
        ScreenSpec(
            "S-40", "Admin — Claims Console", "s40", ScreenGroup.ADMIN, 3,
            note = "The one desktop screen. Proves the back office was designed, not skipped.",
        ),
        ScreenSpec("S-41", "Verification Center", "s41", ScreenGroup.ACCOUNT, 3),
        ScreenSpec("S-42", "Listing Reviews", "s42", ScreenGroup.DISCOVERY, 3),
    )

    fun byRoute(route: String): ScreenSpec? = screens.firstOrNull { it.route == route }

    /** Presentation shell routes — the cover, the boards, the credits. */
    val shellRoutes = listOf(
        Routes.COVER, Routes.SCREEN_INDEX, Routes.BOARD_FOUNDATIONS,
        Routes.BOARD_COMPONENTS, Routes.BOARD_JOURNEY, Routes.BOARD_SIGNATURE,
        Routes.BOARD_MAP, Routes.CREDITS,
    )

    /** Every route the app can open, catalogue and shell together. */
    val allRoutes: List<String> get() = shellRoutes + screens.map { it.route }

    /** Flow A — Rent. The main demo, and the order it is walked in. */
    val flowRent = Flow(
        id = "A",
        label = "Rent",
        blurb = "From \"I need a surfboard\" to \"I returned it and got my deposit back\".",
        steps = listOf(
            FlowStep("s04", "Rails are ranked by fit, not by ad spend."),
            FlowStep("s05", "One result is unavailable — the unhappy path is designed too."),
            FlowStep("s07", "Tag, fit verdict, condition evidence, deposit in plain language."),
            FlowStep("s08", "Buffer days are hatched, so turnaround time is visible."),
            FlowStep("s09", "A deposit is a hold. Protection replaces it with an excess."),
            FlowStep("s10", "Accept stays disabled until the waiver is actually scrolled."),
            FlowStep("s11", "Every line adds up to LKR 9,240. Nothing is hidden."),
            FlowStep("s12", "What happens next, with dates attached."),
            FlowStep("s13", "Address unlocks. Both parties' readiness is visible."),
            FlowStep("s15", "Seven guided angles, both present, geotagged."),
            FlowStep("s18", "The same seven angles, side by side, differences flagged."),
        ),
    )

    /** Flow B — Swap. Rental is the volume; exchange is the differentiator. */
    val flowSwap = Flow(
        id = "B",
        label = "Swap",
        blurb = "Trading gear you've outgrown, without either side being short-changed.",
        steps = listOf(
            FlowStep("s19", "Six trades found from one board you already own."),
            FlowStep("s20", "Pick what you give and what you want. Values are shown, not hidden."),
            FlowStep("s21", "Drag the boot slider and the beam levels out. The one moment of motion."),
            FlowStep("s22", "The whole trade on one screen before anyone commits."),
            FlowStep("s23", "Three people, none of whom could trade directly, all get what they want."),
        ),
    )

    /** Flow C — Owner. The side of the market most student projects skip. */
    val flowOwner = Flow(
        id = "C",
        label = "Owner",
        blurb = "The other side of the market: what it looks like to lend your gear out.",
        steps = listOf(
            FlowStep("s24", "Earnings, occupancy, and what needs attention today."),
            FlowStep("s28", "Requests with the renter's fit and trust tier already attached."),
            FlowStep("s27", "Every unit, every booking, buffer days included."),
        ),
    )

    val flows = listOf(flowRent, flowSwap, flowOwner)

    fun flowById(id: String): Flow? = flows.firstOrNull { it.id == id }

    val tierCounts: Map<Int, Int> get() = screens.groupingBy { it.tier }.eachCount()
}
