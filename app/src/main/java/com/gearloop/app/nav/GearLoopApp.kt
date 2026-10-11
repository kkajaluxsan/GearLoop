@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.gearloop.app.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gearloop.app.BuildConfig
import com.gearloop.app.container
import com.gearloop.app.data.LocalSearchFilters
import com.gearloop.app.data.SearchFilters
import com.gearloop.app.screens.account.*
import com.gearloop.app.screens.admin.S40AdminClaims
import com.gearloop.app.screens.auth.LoginScreen
import com.gearloop.app.screens.auth.SignUpScreen
import com.gearloop.app.screens.auth.SplashScreen
import com.gearloop.app.screens.auth.WelcomeScreen
import com.gearloop.app.screens.booking.*
import com.gearloop.app.screens.discovery.*
import com.gearloop.app.screens.exchange.*
import com.gearloop.app.screens.handover.*
import com.gearloop.app.screens.onboarding.*
import com.gearloop.app.screens.owner.*
import com.gearloop.app.screens.settings.ChangePasswordScreen
import com.gearloop.app.screens.settings.CreditsScreen
import com.gearloop.app.screens.settings.EditProfileScreen
import com.gearloop.app.screens.settings.PrivacyScreen
import com.gearloop.app.screens.settings.SettingsScreen
import com.gearloop.app.ui.feedback.ToastHost
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * Navigation actions, wrapped so screens never touch NavController directly.
 */
class Nav(private val controller: NavHostController) {

    fun to(route: String) {
        controller.navigate(route) { launchSingleTop = true }
    }

    /** Back, or home if there is nowhere to go back to. */
    fun back() {
        if (!controller.popBackStack()) home()
    }

    /**
     * The end of a multi-step flow: open [route] and drop the steps above
     * [flowStart], so Back from the result returns to where the flow began
     * rather than into a form that has already been submitted.
     */
    fun finishFlow(route: String, flowStart: String) {
        controller.navigate(route) {
            popUpTo(flowStart)
            launchSingleTop = true
        }
    }

    /**
     * Close this screen and land on [route]: back to it when it is directly
     * behind, otherwise in its place. The filter sheet ends on the results
     * whether it was opened from them or from home.
     */
    fun closeTo(route: String) {
        if (controller.previousBackStackEntry?.destination?.route == route) {
            controller.popBackStack()
        } else {
            val current = controller.currentDestination?.id
            controller.navigate(route) {
                if (current != null) popUpTo(current) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    /** Back to [route], dropping everything above it, or plain back when it isn't on the stack. */
    fun backTo(route: String) {
        if (!controller.popBackStack(route, inclusive = false)) back()
    }

    /**
     * Back past [route] to whatever opened it — plain back when it isn't on the
     * stack, and home when it is the bottom of it, which would leave nothing.
     */
    fun backPast(route: String) {
        when {
            controller.graph.startDestinationRoute == route -> home()
            runCatching { controller.getBackStackEntry(route) }.isSuccess -> controller.popBackStack(route, inclusive = true)
            else -> back()
        }
    }

    fun home() {
        controller.navigate(Routes.HOME) {
            popUpTo(controller.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    /** Tab switches replace rather than stack, so the back stack stays honest. */
    fun tab(route: String) {
        controller.navigate(route) {
            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Replace everything behind with [route] — after sign-in, or finishing getting started. */
    fun reset(route: String) {
        controller.navigate(route) {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

private const val SCREEN_MS = 250
private const val SHEET_MS = 300

/**
 * The app: a splash on a cold start, then the signed-out screens or the
 * member's own app. The navigation graph is rebuilt whenever a different member
 * signs in, so nothing one member had open — a half-filled form, a saved tab,
 * the back stack — is ever shown to the next.
 */
@Composable
fun GearLoopApp(
    modifier: Modifier = Modifier,
    startDestination: String? = null,
) {
    val app = LocalContext.current.container
    val session by app.session.session.collectAsStateWithLifecycle()
    val profile by app.store.profile.collectAsStateWithLifecycle()
    val account by app.accounts.current.collectAsStateWithLifecycle()
    var splashDone by rememberSaveable { mutableStateOf(startDestination != null) }
    val handle = session.currentHandle
    // A screen asked for at launch applies to whoever was signed in then, not
    // to the next member after a sign-out or sign-in.
    val launchHandle = rememberSaveable { mutableStateOf(handle) }.value

    Box(modifier) {
        // A member who has just signed in is shown once their profile is in
        // memory, so the graph starts in the right place for them.
        val ready = handle == null || (profile?.person?.handle == handle && account?.handle == handle)
        if (!splashDone) {
            SplashScreen(onDone = { splashDone = true })
        } else if (ready) {
            key(handle) {
                val filters = rememberSaveable(saver = SearchFilters.Saver) { SearchFilters() }
                // The filter sheet opens as the member left it. Read once, then
                // written back a moment after each change — debounced, so
                // dragging the price slider is one write rather than sixty.
                LaunchedEffect(filters, handle) {
                    if (handle == null) return@LaunchedEffect
                    app.discovery.loadSearchPrefs()?.let { if (filters.isDefault) filters.restoreFrom(it) }
                    snapshotFlow { filters.snapshot }
                        .drop(1)
                        .debounce(300)
                        .collect {
                            if (filters.isDefault) app.discovery.clearSearchPrefs()
                            else app.discovery.saveSearchPrefs(filters)
                        }
                }
                // Decided once per member: the graph's start must not move while it
                // is in use, or marking the welcome pages seen would throw the
                // member back to it mid-navigation.
                val start = remember {
                    when {
                        startDestination != null && handle == launchHandle -> startDestination
                        handle == null -> if (session.welcomeSeen) Routes.LOGIN else Routes.WELCOME
                        // The claims desk is the admin's job; the shop is one tap back.
                        account?.isAdmin == true -> "s40"
                        profile?.onboarded == false -> Routes.INTENT
                        else -> Routes.HOME
                    }
                }
                CompositionLocalProvider(LocalSearchFilters provides filters) {
                    val controller = rememberNavController()
                    val nav = remember(controller) { Nav(controller) }
                    GearLoopNavHost(controller, nav, start)
                }
            }
        }
        ToastHost(app.toaster)
    }
}

@Composable
private fun GearLoopNavHost(
    controller: NavHostController,
    nav: Nav,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = controller,
        startDestination = startDestination,
        modifier = modifier,
        // One transition everywhere: 250 ms, ease out.
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(SCREEN_MS)) +
                fadeIn(tween(SCREEN_MS))
        },
        exitTransition = { fadeOut(tween(SCREEN_MS / 2)) },
        popEnterTransition = { fadeIn(tween(SCREEN_MS)) },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(SCREEN_MS)) +
                fadeOut(tween(SCREEN_MS))
        },
    ) {
        // ---- signed out
        composable(Routes.WELCOME) { WelcomeScreen(nav) }
        composable(Routes.LOGIN) { LoginScreen(nav) }
        composable(Routes.SIGN_UP) { SignUpScreen(nav) }

        // ---- getting started
        composable("s01") { S01IntentSelector(nav) }
        composable("s02") { S02FitProfile(nav) }
        composable("s03") { S03SportInterests(nav) }

        // ---- discovery
        composable("s04") { S04Home(nav) }
        composable("s05") { S05SearchResults(nav) }
        sheet("s06") { S06FilterSheet(nav) }
        composable("s07") { S07ListingDetail(nav) }
        composable("s29") { S29SearchMap(nav) }
        composable("s30") { S30TripPlanner(nav) }
        composable("s39") { S39NoResults(nav) }
        composable("s42") { S42ListingReviews(nav) }
        composable(Routes.SAVED) { SavedScreen(nav) }

        // ---- booking
        sheet("s08") { S08DatePicker(nav) }
        composable("s09") { S09Protection(nav) }
        composable("s10") { S10Waiver(nav) }
        composable("s11") { S11ReviewPay(nav) }
        composable("s12") { S12Confirmed(nav) }

        // ---- handover and the rental
        composable("s13") { S13HandoverHub(nav) }
        composable("s14") { S14ScanHandshake(nav) }
        composable("s15") { S15ConditionCapture(nav) }
        composable("s15b") { S15bDamageMarking(nav) }
        composable("s16") { S16SignRelease(nav) }
        composable("s17") { S17ActiveRental(nav) }
        composable("s18") { S18ReturnSettlement(nav) }
        composable("s18b") { S18bEvidenceDiff(nav) }

        // ---- swapping
        composable("s19") { S19SwapMatchFeed(nav) }
        composable("s20") { S20BuildProposal(nav) }
        composable("s21") { S21FairnessBoot(nav) }
        composable("s22") { S22ProposalDetail(nav) }
        composable("s23") { S23CyclePreview(nav) }

        // ---- owning
        composable("s24") { S24OwnerDashboard(nav) }
        composable("s25") { S25CreateListingPhotos(nav) }
        composable("s26") { S26CreateListingPricing(nav) }
        composable("s27") { S27MasterCalendar(nav) }
        composable("s28") { S28RequestsInbox(nav) }
        composable(Routes.MY_LISTINGS) { MyListingsScreen(nav) }
        composable(Routes.GEAR_TAG) { GearTagScreen(nav) }

        // ---- the account
        composable("s31") { S31MyRentals(nav) }
        composable("s32") { S32TrustProfile(nav) }
        composable(Routes.INBOX) { InboxScreen(nav) }
        composable("s33") { S33MessageThread(nav) }
        composable("s34") { S34DamageClaim(nav) }
        composable("s35") { S35Wallet(nav) }
        composable("s36") { S36GearCloset(nav) }
        composable("s37") { S37PublicProfile(nav) }
        composable("s38") { S38Notifications(nav) }
        composable("s40") { S40AdminClaims(nav) }
        composable("s41") { S41Verification(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen(nav) }
        composable(Routes.CHANGE_PASSWORD) { ChangePasswordScreen(nav) }
        composable(Routes.PRIVACY) { PrivacyScreen(nav) }
        composable(Routes.CREDITS) { CreditsScreen(nav) }
    }

    if (BuildConfig.DEBUG) VerifyCatalogue(controller)
}

/**
 * Every route the catalogue lists has a screen, and every screen is listed.
 * Debug builds only; it fails loudly on launch rather than three taps into a flow.
 */
@Composable
private fun VerifyCatalogue(controller: NavHostController) {
    LaunchedEffect(controller) {
        val registered = controller.graph.mapNotNull { it.route }.toSet()
        val missing = Catalogue.allRoutes.filterNot { it in registered }
        check(missing.isEmpty()) { "Catalogue routes with no composable in the graph: $missing" }
        val orphaned = registered.filterNot { it in Catalogue.allRoutes }
        check(orphaned.isEmpty()) { "Routes in the graph that the catalogue doesn't list: $orphaned" }
    }
}

/** Sheets move in from the bottom over the screen behind them. */
private fun androidx.navigation.NavGraphBuilder.sheet(
    route: String,
    content: @Composable () -> Unit,
) = composable(
    route = route,
    enterTransition = { slideInVertically(tween(SHEET_MS)) { it } + fadeIn(tween(SHEET_MS)) },
    exitTransition = { fadeOut(tween(SHEET_MS / 2)) },
    popEnterTransition = { fadeIn(tween(SHEET_MS)) },
    popExitTransition = { slideOutVertically(tween(SHEET_MS)) { it } + fadeOut(tween(SHEET_MS)) },
) { content() }
