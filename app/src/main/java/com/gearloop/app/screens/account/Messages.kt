package com.gearloop.app.screens.account

import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.nav.Nav
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
/* ------------------------------------------------------------------ S-33 */

@Composable
fun S33MessageThread(nav: Nav) {
    GearScreen(
        appBar = {
            AppBar(
                title = Demo.kavitha.name,
                subtitle = "Responds in ${Demo.kavitha.respondsIn}",
                onBack = { nav.back() },
                actions = { GearIconButton(Lucide.Phone, "Call", { }, size = 40.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.MESSAGES) },
        contentPadding = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = Space.screen)) {
            VGap(Space.s)
            GearCard(Modifier.fillMaxWidth(), fill = GearColor.Chalk50) {
                Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                    GearImage(Demo.torq.hero, Demo.torq.title, Modifier.size(44.dp), GearShape.sm)
                    Spacer(Modifier.width(Space.s))
                    Column(Modifier.weight(1f)) {
                        Text(Demo.torq.shortTitle, style = GearType.labelL, color = GearText.primary, maxLines = 1)
                        Text("${Demo.BOOKING_DATES} · ${Demo.BOOKING_REF}", style = GearType.dataS, color = GearText.tertiary)
                    }
                    StatusBadge("Confirmed", GearColor.Success500)
                }
            }

            VGap(Space.m)
            Demo.chatThread.forEach { msg ->
                when (msg.kind) {
                    com.gearloop.app.data.ChatMessage.Kind.SYSTEM -> SystemCard(msg.body, msg.time)
                    com.gearloop.app.data.ChatMessage.Kind.LOCATION -> LocationCard(msg.body, msg.time)
                    com.gearloop.app.data.ChatMessage.Kind.REDACTED -> RedactedCard(msg.body)
                    else -> Bubble(msg.body, msg.time, msg.fromMe)
                }
                VGap(Space.s)
            }
            VGap(Space.m)
        }

        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberSavedScrollState())
                    .padding(horizontal = Space.screen),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                Demo.quickReplies.forEach { Chip(it, onClick = { }) }
            }
            VGap(Space.s)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(GearSurface.card)
                    .padding(horizontal = Space.screen, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                GearIconButton(Lucide.Paperclip, "Attach", { }, size = 40.dp)
                Box(Modifier.weight(1f)) {
                    GearTextField(value = "", placeholder = "Message Kavitha")
                }
                GearIconButton(Lucide.Send, "Send", { }, tone = IconButtonTone.HI_VIS, size = 44.dp)
            }
        }
    }
}

@Composable
private fun Bubble(text: String, time: String, fromMe: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .clip(GearShape.md)
                .then(
                    if (fromMe) Modifier.background(GearColor.Pine800)
                    else Modifier.border(Border.hair, GearSurface.border, GearShape.md).background(GearSurface.card)
                )
                .padding(Space.sm),
            horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start,
        ) {
            Text(text, style = GearType.bodyM, color = if (fromMe) GearText.onDark else GearText.body)
            VGap(3.dp)
            Text(time, style = GearType.dataS, color = if (fromMe) GearColor.Chalk400 else GearText.tertiary)
        }
    }
}

@Composable
private fun SystemCard(text: String, time: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.sm)
            .background(GearColor.HiVis100)
            .padding(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearIcon(Lucide.CircleCheck, null, size = IconSize.s, tint = GearColor.Pine900)
        Spacer(Modifier.width(Space.s))
        Text(text, style = GearType.dataM, color = GearColor.Pine900, modifier = Modifier.weight(1f))
        Text(time, style = GearType.dataS, color = GearColor.Pine900.copy(alpha = 0.6f))
    }
}

@Composable
private fun LocationCard(text: String, time: String) {
    Column(
        Modifier
            .fillMaxWidth(0.8f)
            .clip(GearShape.md)
            .border(Border.hair, GearSurface.border, GearShape.md)
            .background(GearSurface.card),
    ) {
        Box(Modifier.fillMaxWidth().height(100.dp)) {
            MapCanvas(Modifier.fillMaxSize())
        }
        Column(Modifier.padding(Space.sm)) {
            Text(text, style = GearType.bodyM, color = GearText.body)
            VGap(3.dp)
            Text(time, style = GearType.dataS, color = GearText.tertiary)
        }
    }
}

/** Contact details stay masked until the booking exists. */
@Composable
private fun RedactedCard(text: String) {
    Column(
        Modifier
            .fillMaxWidth(0.8f)
            .clip(GearShape.md)
            .border(Border.hair, GearSurface.border, GearShape.md)
            .background(GearColor.Chalk100)
            .padding(Space.sm),
    ) {
        Text(text, style = GearType.bodyM, color = GearColor.Chalk400)
        VGap(4.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GearIcon(Lucide.Lock, null, size = 12.dp, tint = GearText.tertiary)
            Text("Contact details unlock at booking", style = GearType.bodyS, color = GearText.tertiary)
        }
    }
}

/* ------------------------------------------------------------------ S-38 */

@Composable
fun S38Notifications(nav: Nav) {
    GearScreen(
        appBar = {
            AppBar(
                title = "Notifications",
                onBack = { nav.back() },
                actions = { GearLink("Mark all read", { }, color = GearText.tertiary) },
            )
        },
    ) {
        VGap(Space.s)
        Eyebrow("Today")
        VGap(Space.s)
        Demo.notifications.take(2).forEach { NotificationRow(it, nav) }

        VGap(Space.m)
        Eyebrow("This week")
        VGap(Space.s)
        Demo.notifications.drop(2).forEach { NotificationRow(it, nav) }
        VGap(Space.section)
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, nav: Nav) {
    val (icon, tint) = when (item.kind) {
        NotificationItem.Kind.BOOKING -> Lucide.CircleCheck to GearColor.Success500
        NotificationItem.Kind.SWAP -> Lucide.ArrowRightLeft to GearColor.Marine500
        NotificationItem.Kind.SAFETY -> Lucide.ShieldAlert to GearColor.Pine900
        NotificationItem.Kind.PAYOUT -> Lucide.Wallet to GearColor.Pine800
        NotificationItem.Kind.REVIEW -> Lucide.Star to GearColor.Warning500
    }
    val safety = item.kind == NotificationItem.Kind.SAFETY

    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.sm)
            .background(if (safety) GearColor.HiVis100 else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Button) {
                when (item.kind) {
                    NotificationItem.Kind.SWAP -> nav.to("s22")
                    NotificationItem.Kind.PAYOUT -> nav.to("s35")
                    else -> nav.to("s13")
                }
            }
            .padding(Space.sm),
        verticalAlignment = Alignment.Top,
    ) {
        GearIcon(icon, null, size = IconSize.m, tint = tint)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = GearType.labelL, color = GearText.primary)
            VGap(3.dp)
            Text(item.body, style = GearType.bodyS, color = GearText.secondary)
        }
        Spacer(Modifier.width(Space.s))
        Column(horizontalAlignment = Alignment.End) {
            Text(item.time, style = GearType.dataS, color = GearText.tertiary)
            if (item.unread) {
                VGap(6.dp)
                Box(Modifier.size(8.dp).clip(GearShape.full).background(GearColor.HiVis600))
            }
        }
    }
    GearDivider(Modifier.padding(vertical = 4.dp))
}

