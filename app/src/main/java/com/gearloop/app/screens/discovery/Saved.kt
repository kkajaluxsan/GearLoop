package com.gearloop.app.screens.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.nav.Nav
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel

/** Everything the member has hearted, to come back to. */
@Composable
fun SavedScreen(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val saved by vm.saved.collectAsStateWithLifecycle()

    GearLazyScreen(appBar = { AppBar(title = "Saved", onBack = { nav.back() }) }) {
        if (saved.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Lucide.Heart,
                    headline = "Nothing saved yet",
                    body = "Tap the heart on any listing to keep it here.",
                    actionLabel = "Find gear",
                    onAction = { nav.tab("s04") },
                )
            }
        }
        items(saved.size, key = { saved[it].id }) { i ->
            val item = saved[i]
            VGap(Space.s)
            ListingCard(item, { vm.select(item); nav.to("s07") }, layout = ListingLayout.LIST, onToggleSave = { vm.toggleSaved(item) })
        }
        item(key = "end") { VGap(Space.section) }
    }
}
