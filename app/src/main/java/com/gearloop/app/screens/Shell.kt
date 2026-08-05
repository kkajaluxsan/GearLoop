package com.gearloop.app.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.components.TabBar
import com.gearloop.app.ui.components.TabItem
import com.gearloop.app.ui.icons.Lucide

/** The five top-level destinations. */
enum class MainTab(val label: String, val route: String) {
    EXPLORE("Explore", "s04"),
    SWAP("Swap", "s19"),
    RENTALS("Rentals", "s31"),
    MESSAGES("Messages", "s33"),
    PROFILE("Profile", "s32"),
}

private val tabItems = listOf(
    TabItem("Explore", Lucide.Compass),
    TabItem("Swap", Lucide.ArrowRightLeft, badge = true),
    TabItem("Rentals", Lucide.Package, badge = true),
    TabItem("Messages", Lucide.MessageSquare),
    TabItem("Profile", Lucide.User),
)

@Composable
fun MainTabBar(nav: Nav, active: MainTab, modifier: Modifier = Modifier) {
    TabBar(
        tabs = tabItems,
        selectedIndex = active.ordinal,
        onSelect = { i -> nav.tab(MainTab.entries[i].route) },
        modifier = modifier,
    )
}
