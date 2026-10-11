package com.gearloop.app.nav

/**
 * Every place the app can go.
 *
 * The product screens keep the short routes they have always had (`s04` is
 * home, `s07` a listing), because the functional tests open them by route; the
 * account and settings screens added for the live app have names.
 */
object Routes {
    // ---- signed out
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGN_UP = "signup"

    // ---- getting started, once, after sign-up
    const val INTENT = "s01"
    const val FIT = "s02"
    const val SPORTS = "s03"

    // ---- the five tabs
    const val HOME = "s04"
    const val SWAP = "s19"
    const val RENTALS = "s31"
    const val INBOX = "inbox"
    const val PROFILE = "s32"

    // ---- account and settings
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "profile/edit"
    const val CHANGE_PASSWORD = "profile/password"
    const val SAVED = "saved"
    const val MY_LISTINGS = "listings/mine"
    const val GEAR_TAG = "listings/tag"
    const val PRIVACY = "privacy"
    const val CREDITS = "credits"
}

/** One destination, with the name the app shows for it. */
data class ScreenSpec(val route: String, val title: String)

object Catalogue {

    /** The product screens: browsing, booking, handover, swapping, owning, the account. */
    val screens = listOf(
        ScreenSpec("s01", "What brings you here"),
        ScreenSpec("s02", "Fit profile"),
        ScreenSpec("s03", "Your sports"),
        ScreenSpec("s04", "Home"),
        ScreenSpec("s05", "Search"),
        ScreenSpec("s06", "Filters"),
        ScreenSpec("s07", "Listing"),
        ScreenSpec("s08", "Dates"),
        ScreenSpec("s09", "Protection"),
        ScreenSpec("s10", "Safety and waiver"),
        ScreenSpec("s11", "Review and pay"),
        ScreenSpec("s12", "Booking"),
        ScreenSpec("s13", "Pickup"),
        ScreenSpec("s14", "Check the gear tag"),
        ScreenSpec("s15", "Condition check"),
        ScreenSpec("s15b", "Mark damage"),
        ScreenSpec("s16", "Sign and release"),
        ScreenSpec("s17", "Rental"),
        ScreenSpec("s18", "Return check"),
        ScreenSpec("s18b", "Difference"),
        ScreenSpec("s19", "Swap"),
        ScreenSpec("s20", "Build a proposal"),
        ScreenSpec("s21", "Is it fair?"),
        ScreenSpec("s22", "Proposal"),
        ScreenSpec("s23", "Three-way swap"),
        ScreenSpec("s24", "Your gear"),
        ScreenSpec("s25", "Listing photos"),
        ScreenSpec("s26", "Listing price"),
        ScreenSpec("s27", "Calendar"),
        ScreenSpec("s28", "Requests"),
        ScreenSpec("s29", "Map"),
        ScreenSpec("s30", "Trip planner"),
        ScreenSpec("s31", "Rentals"),
        ScreenSpec("s32", "Profile"),
        ScreenSpec("s33", "Conversation"),
        ScreenSpec("s34", "Claim"),
        ScreenSpec("s35", "Wallet"),
        ScreenSpec("s36", "Gear closet"),
        ScreenSpec("s37", "Public profile"),
        ScreenSpec("s38", "Notifications"),
        ScreenSpec("s39", "Nothing found"),
        ScreenSpec("s40", "Claims desk"),
        ScreenSpec("s41", "Verification"),
        ScreenSpec("s42", "Reviews"),
    )

    /** The account, settings and signed-out screens. */
    val accountRoutes = listOf(
        Routes.WELCOME, Routes.LOGIN, Routes.SIGN_UP, Routes.INBOX, Routes.SETTINGS, Routes.EDIT_PROFILE,
        Routes.CHANGE_PASSWORD, Routes.SAVED, Routes.MY_LISTINGS, Routes.GEAR_TAG, Routes.PRIVACY, Routes.CREDITS,
    )

    /** Every route the app can open. */
    val allRoutes: List<String> get() = accountRoutes + screens.map { it.route }

    fun byRoute(route: String): ScreenSpec? = screens.firstOrNull { it.route == route }
}
