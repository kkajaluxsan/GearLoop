package com.gearloop.app.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.container
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.Routes
import com.gearloop.app.ui.components.TabBar
import com.gearloop.app.ui.components.TabItem
import com.gearloop.app.ui.icons.Lucide

/** The five top-level destinations. */
enum class MainTab(val label: String, val route: String) {
    EXPLORE("Explore", Routes.HOME),
    SWAP("Swap", Routes.SWAP),
    RENTALS("Rentals", Routes.RENTALS),
    MESSAGES("Messages", Routes.INBOX),
    PROFILE("Profile", Routes.PROFILE),
}

/**
 * The tab bar. The dots are real: Swap shows one when a proposal is waiting on
 * the member, Rentals when a rental is out or a pickup is within a day, and
 * Profile when there are unread notifications or a booking request is waiting
 * on them as an owner.
 */
@Composable
fun MainTabBar(nav: Nav, active: MainTab, modifier: Modifier = Modifier) {
    val app = LocalContext.current.container
    val proposals by app.exchange.proposals.collectAsStateWithLifecycle()
    val bookings by app.money.active.collectAsStateWithLifecycle()
    val upcoming by app.money.upcoming.collectAsStateWithLifecycle()
    val unread by app.discovery.unreadNotifications.collectAsStateWithLifecycle()
    val requests by app.exchange.requests.collectAsStateWithLifecycle()
    val swapWaiting = proposals.any { app.exchange.myMove(it) && it.open }
    val rentalsLive = bookings.isNotEmpty() || upcoming.any {
        it.status == BookingStatus.CONFIRMED && !it.start.isAfter(com.gearloop.app.data.AppClock.today.plusDays(1))
    }
    TabBar(
        tabs = listOf(
            TabItem("Explore", Lucide.Compass),
            TabItem("Swap", Lucide.ArrowRightLeft, badge = swapWaiting),
            TabItem("Rentals", Lucide.Package, badge = rentalsLive),
            TabItem("Messages", Lucide.MessageSquare),
            TabItem("Profile", Lucide.User, badge = unread > 0 || requests.isNotEmpty()),
        ),
        selectedIndex = active.ordinal,
        onSelect = { i -> nav.tab(MainTab.entries[i].route) },
        modifier = modifier,
    )
}
