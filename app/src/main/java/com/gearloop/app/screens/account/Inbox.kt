package com.gearloop.app.screens.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.relativeTime
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.theme.Text
import com.gearloop.app.ui.vm.MoneyViewModel
import com.gearloop.app.ui.vm.gearViewModel

/**
 * The Messages tab: every conversation the member has, most recent first, with
 * the last thing said. Opening one shows the whole thread.
 */
@Composable
fun InboxScreen(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val now = AppClock.now()

    GearScreen(
        appBar = { AppBar(title = "Messages") },
        tabBar = { MainTabBar(nav, MainTab.MESSAGES) },
    ) {
        VGap(Space.s)
        if (conversations.isEmpty()) {
            EmptyState(
                icon = Lucide.MessageSquare,
                headline = "No conversations yet",
                body = "Message an owner from any listing.",
                actionLabel = "Find gear",
                onAction = { nav.tab("s04") },
            )
        }
        conversations.forEach { c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "Open the conversation with ${c.other.name}") {
                        vm.openThread(c.other.handle)
                        nav.to("s33")
                    }
                    .padding(vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(c.other, size = 48.dp, showTierRing = true)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.other.name, style = GearType.labelL, color = GearText.primary, modifier = Modifier.weight(1f), maxLines = 1)
                        c.last?.let { Text(relativeTime(it.createdAt, now).uppercase(), style = GearType.dataS, color = GearText.tertiary) }
                    }
                    val last = c.last
                    Text(
                        when {
                            last == null -> "No messages yet"
                            last.chat.kind == ChatMessage.Kind.SYSTEM -> last.chat.body
                            last.chat.kind == ChatMessage.Kind.LOCATION -> "Shared the pickup point"
                            last.chat.fromMe -> "You: ${last.chat.body}"
                            else -> last.chat.body
                        },
                        style = GearType.bodyS,
                        color = GearText.secondary,
                        maxLines = 1,
                    )
                }
            }
            GearDivider()
        }
        VGap(Space.section)
    }
}
