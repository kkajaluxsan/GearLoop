@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.gearloop.app.nav

import com.gearloop.app.container
import com.gearloop.app.ui.feedback.ToastHost
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.gearloop.app.data.SearchFilters
import com.gearloop.app.data.LocalSearchFilters
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.gearloop.app.BuildConfig
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gearloop.app.screens.account.*
import com.gearloop.app.screens.admin.S40AdminClaims
import com.gearloop.app.screens.booking.*
import com.gearloop.app.screens.boards.*
import com.gearloop.app.screens.discovery.*
import com.gearloop.app.screens.exchange.*
import com.gearloop.app.screens.handover.*
import com.gearloop.app.screens.onboarding.*
import com.gearloop.app.screens.owner.*

/**
 * Navigation actions, wrapped so screens never touch NavController directly.
 */
class Nav(
    private val controller: NavHostController,
    private val demo: DemoState,
    /**
     * Puts the database back to the seed, then runs the remount. Since Milestone
     * 03 the screens' state lives in Room as well as in `rememberSaveable`, so a
     * reset that only remounted would put the slider back and leave the booking.
     */
    private val resetData: (afterReset: () -> Unit) -> Unit = { it() },
    private val playScenario: (com.gearloop.app.data.repo.DemoData.Scenario, afterReset: () -> Unit) -> Unit = { _, done -> done() },
) {

    fun to(route: String) {
        controller.navigate(route) { launchSingleTop = true }
        // Someone navigating by hand mid-walkthrough shouldn't desync the bar.
        demo.syncTo(route)
    }

    fun back() {
        if (!controller.popBackStack()) controller.navigate(Routes.COVER)
    }

    /** Tab switches replace rather than stack, so the back stack stays honest. */
    fun tab(route: String) {
        controller.navigate(route) {
            popUpTo(Routes.COVER) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        demo.syncTo(route)
    }

    fun cover() {
        controller.navigate(Routes.COVER) { popUpTo(Routes.COVER) { inclusive = true } }
    }

    /** Begin a walkthrough: the bar appears and the first screen opens. */
    fun startFlow(flow: Flow) {
        demo.start(flow)
        controller.navigate(flow.routes.first()) {
            popUpTo(Routes.COVER)
            launchSingleTop = true
        }
    }

    /** Jump to a numbered stop in the running flow. */
    fun goToStep(index: Int) {
        val flow = demo.flow ?: return
        demo.moveTo(index)
        val route = flow.steps[demo.step].route
        controller.navigate(route) {
            popUpTo(Routes.COVER)
            launchSingleTop = true
        }
    }

    fun exitFlow() {
        demo.exit()
        cover()
    }

    /** Clear every screen's state and every record, without starting a walkthrough. */
    fun resetDemo() {
        resetData {
            demo.exit()
            demo.reset()
        }
    }

    /**
     * Back to a clean demo: data reseeded, state cleared, walkthrough restarted
     * from step one. Everything a previous run touched — the booking it made, the
     * fairness slider, the fit profile, the filter sheet — goes back to how it
     * was designed.
     *
     * No navigation call here on purpose. Bumping the generation remounts the
     * graph, and the new one opens at whichever step the walkthrough is on.
     */
    fun restartFlow() {
        resetData {
            demo.moveTo(0)
            demo.reset()
        }
    }

    /** Fast-forward to a prepared state, then open the screen it is about. */
    fun play(scenario: com.gearloop.app.data.repo.DemoData.Scenario, landOn: String) {
        playScenario(scenario) {
            demo.exit()
            demo.landing = landOn
            demo.reset()
        }
    }
}

private const val SCREEN_MS = 250
private const val SHEET_MS = 300

@Composable
fun GearLoopApp(
    modifier: Modifier = Modifier,
    startDestination: String = Routes.COVER,
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    val demo = rememberSaveable(saver = DemoState.Saver) { DemoState() }
    val filters = rememberSaveable(saver = SearchFilters.Saver) { SearchFilters() }
    val app = LocalContext.current.container
    val scope = rememberCoroutineScope()

    // The filter sheet opens as it was left. Read once, then written back a
    // moment after each change — debounced, so dragging the price slider is one
    // write rather than sixty.
    LaunchedEffect(filters) {
        app.discovery.loadSearchPrefs()?.let { if (filters.isDefault) filters.restoreFrom(it) }
        snapshotFlow { filters.snapshot }
            .drop(1)
            .debounce(300)
            .collect {
                // Back at the defaults is a delete, not a save of the defaults —
                // "Reset" on S-06 removes the stored row.
                if (filters.isDefault) app.discovery.clearSearchPrefs()
                else app.discovery.saveSearchPrefs(filters)
            }
    }

    val resetData: (() -> Unit) -> Unit = { after ->
        scope.launch {
            app.demo.reset()
            filters.reset()
            after()
        }
    }
    val playScenario: (com.gearloop.app.data.repo.DemoData.Scenario, () -> Unit) -> Unit = { scenario, after ->
        scope.launch {
            app.demo.play(scenario)
            filters.reset()
            after()
        }
    }

    CompositionLocalProvider(
        LocalDemoState provides demo,
        LocalSearchFilters provides filters,
        LocalThemeToggle provides ThemeToggle(darkTheme, onToggleTheme),
    ) {
        Box(modifier) {
            Column(Modifier.fillMaxSize()) {
                // Remounting on `generation` is what makes "reset the demo" real —
                // every screen's saved state goes with it. The controller lives
                // inside the key so it is rebuilt too, and the bar and the host are
                // both inside it so they share one Nav rather than reaching for a
                // global.
                key(demo.generation) {
                    val controller = rememberNavController()
                    val nav = remember(controller, demo) { Nav(controller, demo, resetData, playScenario) }

                    // After a reset the graph reopens wherever the walkthrough is,
                    // which is step one — that is the whole point of restarting.
                    // A scenario names its own landing screen instead.
                    val start = demo.flow?.routes?.getOrNull(demo.step)
                        ?: demo.landing
                        ?: startDestination
                    LaunchedEffect(Unit) { demo.landing = null }

                    GuidedBar(
                        state = demo,
                        onStep = nav::goToStep,
                        onRestart = nav::restartFlow,
                        onExit = nav::exitFlow,
                    )
                    GearLoopNavHost(controller, nav, start, Modifier.weight(1f))
                }
            }
            ToastHost(app.toaster)
        }
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
        // One transition everywhere, matching the prototype spec: 250 ms, ease out.
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
        // ---- presentation shell -------------------------------------------
        composable(Routes.COVER) { CoverScreen(nav) }
        composable(Routes.SCREEN_INDEX) { ScreenIndex(nav) }
        composable(Routes.BOARD_FOUNDATIONS) { FoundationsBoard(nav) }
        composable(Routes.BOARD_COMPONENTS) { ComponentsBoard(nav) }
        composable(Routes.BOARD_JOURNEY) { JourneyBoard(nav) }
        composable(Routes.BOARD_SIGNATURE) { SignatureBoard(nav) }
        composable(Routes.BOARD_MAP) { ScreenMapBoard(nav) }
        composable(Routes.CREDITS) { CreditsScreen(nav) }
        composable(Routes.PRIVACY) { PrivacyScreen(nav) }

        // ---- Tier 1 — the golden path --------------------------------------
        composable("s01") { S01IntentSelector(nav) }
        composable("s02") { S02FitProfile(nav) }
        composable("s03") { S03SportInterests(nav) }
        composable("s04") { S04Home(nav) }
        composable("s05") { S05SearchResults(nav) }
        sheet("s06") { S06FilterSheet(nav) }
        composable("s07") { S07ListingDetail(nav) }
        sheet("s08") { S08DatePicker(nav) }
        composable("s09") { S09Protection(nav) }
        composable("s10") { S10Waiver(nav) }
        composable("s11") { S11ReviewPay(nav) }
        composable("s12") { S12Confirmed(nav) }
        composable("s13") { S13HandoverHub(nav) }
        composable("s14") { S14ScanHandshake(nav) }
        composable("s15") { S15ConditionCapture(nav) }
        composable("s15b") { S15bDamageMarking(nav) }
        composable("s16") { S16SignRelease(nav) }
        composable("s17") { S17ActiveRental(nav) }
        composable("s18") { S18ReturnSettlement(nav) }
        composable("s18b") { S18bEvidenceDiff(nav) }

        // ---- Tier 2 — the differentiators ----------------------------------
        composable("s19") { S19SwapMatchFeed(nav) }
        composable("s20") { S20BuildProposal(nav) }
        composable("s21") { S21FairnessBoot(nav) }
        composable("s22") { S22ProposalDetail(nav) }
        composable("s23") { S23CyclePreview(nav) }
        composable("s24") { S24OwnerDashboard(nav) }
        composable("s25") { S25CreateListingPhotos(nav) }
        composable("s26") { S26CreateListingPricing(nav) }
        composable("s27") { S27MasterCalendar(nav) }
        composable("s28") { S28RequestsInbox(nav) }
        composable("s29") { S29SearchMap(nav) }
        composable("s30") { S30TripPlanner(nav) }
        composable("s31") { S31MyRentals(nav) }
        composable("s32") { S32TrustProfile(nav) }

        // ---- Tier 3 — depth -------------------------------------------------
        composable("s33") { S33MessageThread(nav) }
        composable("s34") { S34DamageClaim(nav) }
        composable("s35") { S35Wallet(nav) }
        composable("s36") { S36GearCloset(nav) }
        composable("s37") { S37PublicProfile(nav) }
        composable("s38") { S38Notifications(nav) }
        composable("s39") { S39NoResults(nav) }
        composable("s40") { S40AdminClaims(nav) }
        composable("s41") { S41Verification(nav) }
        composable("s42") { S42ListingReviews(nav) }
    }

    if (BuildConfig.DEBUG) VerifyCatalogue(controller)
}

/**
 * Adding a screen takes two edits — a `ScreenSpec` in [Catalogue] and a
 * `composable(...)` above — and nothing but this connects them. A route
 * registered in only one place is half-added: the screen index offers a row
 * that navigates nowhere, or a screen exists that the index never lists.
 *
 * Debug builds only, and it fails loudly on launch rather than in front of an
 * examiner three taps into a flow.
 */
@Composable
private fun VerifyCatalogue(controller: NavHostController) {
    LaunchedEffect(controller) {
        val registered = controller.graph.mapNotNull { it.route }.toSet()

        val missing = Catalogue.allRoutes.filterNot { it in registered }
        check(missing.isEmpty()) {
            "Catalogue routes with no composable in the graph: $missing"
        }

        val orphaned = registered.filterNot { it in Catalogue.allRoutes }
        check(orphaned.isEmpty()) {
            "Routes in the graph that the catalogue doesn't list: $orphaned"
        }

        val duplicateIds = Catalogue.screens.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        check(duplicateIds.isEmpty()) { "Duplicate screen ids: ${duplicateIds.keys}" }

        val duplicateRoutes = Catalogue.screens.groupingBy { it.route }.eachCount().filterValues { it > 1 }
        check(duplicateRoutes.isEmpty()) { "Duplicate routes: ${duplicateRoutes.keys}" }

        Catalogue.flows.forEach { flow ->
            val unknown = flow.routes.filter { Catalogue.byRoute(it) == null }
            check(unknown.isEmpty()) {
                "Flow ${flow.id} walks routes that aren't in the catalogue: $unknown"
            }
            // A walkthrough with a blank caption is a step nobody wrote a point
            // for, and it shows up as an empty bar in front of an audience.
            val uncaptioned = flow.steps.filter { it.caption.isBlank() }.map { it.route }
            check(uncaptioned.isEmpty()) {
                "Flow ${flow.id} has steps with no caption: $uncaptioned"
            }
        }

        val duplicateFlowIds = Catalogue.flows.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        check(duplicateFlowIds.isEmpty()) { "Duplicate flow ids: ${duplicateFlowIds.keys}" }
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
