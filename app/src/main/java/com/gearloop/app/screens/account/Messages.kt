package com.gearloop.app.screens.account

import com.gearloop.app.ui.theme.Border
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.container
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.repo.Notification
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
import com.gearloop.app.ui.vm.ExchangeSupplyViewModel
import com.gearloop.app.ui.vm.MoneyViewModel
import com.gearloop.app.ui.vm.gearViewModel

/* ------------------------------------------------------------------ S-33 */

/**
 * S-33 · Read the thread with whoever it is open on; Create a message by typing
 * or with a quick reply; Delete one of your own with a long press, with Undo.
 *
 * Booking events arrive here as system cards — confirmed, cancelled, signed for —
 * and the pickup point appears the moment payment completes (FR-15).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun S33MessageThread(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val thread by vm.thread.collectAsStateWithLifecycle()
    val threads by vm.threads.collectAsStateWithLifecycle()
    val threadId by vm.threadId.collectAsStateWithLifecycle()
    val header by vm.threadBooking.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }
    var deleting by rememberSaveable { mutableLongStateOf(-1L) }
    val context = LocalContext.current
    val other = people[threadId]
    val first = other?.name?.substringBefore(' ') ?: "them"
    val scroll = rememberSavedScrollState()
    // A new message scrolls the thread to the bottom, where it lands.
    LaunchedEffect(thread.size) { scroll.animateScrollTo(scroll.maxValue) }

    GearScreen(
        appBar = {
            AppBar(
                title = other?.name ?: "Messages",
                subtitle = other?.let { "Responds in ${it.respondsIn}" },
                onBack = { nav.back() },
                actions = {
                    GearIconButton(Lucide.Phone, "Call $first", {
                        context.container.toaster.show("Calls go through a masked number once the live service is connected")
                    }, size = 44.dp)
                },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.MESSAGES) },
        contentPadding = 0.dp,
        scrollable = false,
    ) {
        // Every conversation, so the Messages tab is an inbox rather than one
        // thread: switching is a tap, not a trip back through a profile.
        if (threads.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberSavedScrollState())
                    .padding(horizontal = Space.screen, vertical = Space.xs),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                threads.forEach { id ->
                    val person = people[id] ?: return@forEach
                    Chip(
                        person.name.substringBefore(' '),
                        selected = id == threadId,
                        onClick = { vm.openThread(id) },
                    )
                }
            }
            GearDivider()
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = Space.screen),
        ) {
            VGap(Space.s)
            header?.let { b ->
                GearCard(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.selectBooking(b.ref); nav.to("s12") },
                    fill = GearColor.Chalk50,
                ) {
                    Row(Modifier.padding(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                        GearImage(b.item.hero, b.item.title, Modifier.size(44.dp), GearShape.sm)
                        Spacer(Modifier.width(Space.s))
                        Column(Modifier.weight(1f)) {
                            Text(b.item.shortTitle, style = GearType.labelL, color = GearText.primary, maxLines = 1)
                            Text("${b.dates} · ${b.ref}", style = GearType.dataS, color = GearText.tertiary)
                        }
                        StatusBadge(
                            b.status.label,
                            when (b.status) {
                                BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.COMPLETED -> GearColor.Success500
                                BookingStatus.REQUESTED, BookingStatus.RETURNED -> GearColor.Warning700
                                else -> GearColor.Chalk600
                            },
                        )
                    }
                }
            }

            VGap(Space.m)
            if (thread.isEmpty()) {
                Text("No messages yet. Say hello.", style = GearType.bodyS, color = GearText.tertiary)
            }
            thread.forEach { msg ->
                when (msg.chat.kind) {
                    ChatMessage.Kind.SYSTEM -> SystemCard(msg.chat.body, msg.chat.time)
                    ChatMessage.Kind.LOCATION -> LocationCard(msg.chat.body, msg.chat.time)
                    ChatMessage.Kind.REDACTED -> RedactedCard(msg.chat.body, first)
                    else -> Bubble(
                        msg.chat.body, msg.chat.time, msg.chat.fromMe,
                        onLongPress = if (msg.chat.fromMe) ({ deleting = msg.id }) else null,
                    )
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
                Demo.quickReplies.forEach { reply -> Chip(reply, onClick = { vm.send(reply) }) }
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
                // Shares the booking this thread is about, so neither side has to
                // copy a reference by hand.
                GearIconButton(
                    Lucide.Paperclip, "Attach the booking reference",
                    { header?.let { vm.send("Re: ${it.ref} · ${it.item.shortTitle} · ${it.dates}") } },
                    size = 44.dp,
                )
                Box(Modifier.weight(1f)) {
                    GearTextField(
                        value = draft,
                        placeholder = "Message $first",
                        onValueChange = { draft = it },
                        imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                        onImeAction = { if (draft.isNotBlank()) { vm.send(draft); draft = "" } },
                    )
                }
                GearIconButton(
                    Lucide.Send, "Send",
                    { if (draft.isNotBlank()) { vm.send(draft); draft = "" } },
                    tone = IconButtonTone.HI_VIS, size = 44.dp,
                )
            }
        }
    }

    if (deleting >= 0) {
        GearDialog(
            title = "Delete this message?",
            onDismiss = { deleting = -1L },
            confirmLabel = "Keep it",
            onConfirm = { deleting = -1L },
            destructiveLabel = "Delete",
            onDestructive = { vm.deleteMessage(deleting); deleting = -1L },
        ) {
            Text("It's removed for both of you.", style = GearType.bodyM, color = GearText.secondary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(text: String, time: String, fromMe: Boolean, onLongPress: (() -> Unit)? = null) {
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
                .then(
                    if (onLongPress != null) {
                        Modifier.combinedClickable(
                            role = Role.Button,
                            onClickLabel = null,
                            onLongClickLabel = "Delete message",
                            onLongClick = onLongPress,
                            onClick = {},
                        )
                    } else Modifier
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
            MapCanvas(Modifier.fillMaxWidth().height(100.dp))
        }
        Column(Modifier.padding(Space.sm)) {
            Text(text, style = GearType.bodyM, color = GearText.body)
            VGap(3.dp)
            Text(time, style = GearType.dataS, color = GearText.tertiary)
        }
    }
}

/**
 * A contact detail that was masked when it was sent.
 *
 * The number stays masked because it always does — GearLoop routes calls through
 * a masked line, which is the same thing S-13 says beside its Call button.
 */
@Composable
private fun RedactedCard(text: String, first: String) {
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
            Text(
                "Numbers stay masked — use Call to reach $first",
                style = GearType.bodyS,
                color = GearText.tertiary,
            )
        }
    }
}

/* ------------------------------------------------------------------ S-38 */

/**
 * S-38 · Read the notifications, newest first; Update — opening one marks it
 * read, or mark them all; Delete one, with Undo.
 */
@Composable
fun S38Notifications(nav: Nav) {
    val vm: MoneyViewModel = gearViewModel()
    val exchange: ExchangeSupplyViewModel = gearViewModel()
    val items by vm.notifications.collectAsStateWithLifecycle()
    val today = DemoClock.today
    val (fresh, older) = items.partition { it.createdAt.toLocalDate() == today }

    val open: (Notification) -> Unit = { n ->
        vm.markRead(n.id)
        when (n.route) {
            "s22" -> n.ref?.let(exchange::selectProposal)
            "s34" -> n.ref?.let(vm::selectClaim)
            "s12", "s13", "s17", "s18", "s31" -> n.ref?.let(vm::selectBooking)
        }
        nav.to(n.route)
    }

    GearScreen(
        appBar = {
            AppBar(
                title = "Notifications",
                onBack = { nav.back() },
                actions = {
                    if (items.any { it.item.unread }) GearLink("Mark all read", { vm.markAllRead() }, color = GearText.tertiary)
                },
            )
        },
    ) {
        if (items.isEmpty()) {
            EmptyState(icon = Lucide.Bell, headline = "All caught up", body = "Bookings, swaps and payouts will show up here.")
        }
        if (fresh.isNotEmpty()) {
            VGap(Space.s)
            Eyebrow("Today")
            VGap(Space.s)
            fresh.forEach { NotificationRow(it, { open(it) }, { vm.deleteNotification(it.id) }) }
        }
        if (older.isNotEmpty()) {
            VGap(Space.m)
            Eyebrow("Earlier")
            VGap(Space.s)
            older.forEach { NotificationRow(it, { open(it) }, { vm.deleteNotification(it.id) }) }
        }
        VGap(Space.section)
    }
}

@Composable
private fun NotificationRow(n: Notification, onOpen: () -> Unit, onDelete: () -> Unit) {
    val item = n.item
    val (icon, tint) = when (item.kind) {
        NotificationItem.Kind.BOOKING -> Lucide.CircleCheck to GearColor.Success500
        NotificationItem.Kind.SWAP -> Lucide.ArrowRightLeft to GearColor.Marine500
        NotificationItem.Kind.SAFETY -> Lucide.ShieldAlert to GearColor.Pine900
        NotificationItem.Kind.PAYOUT -> Lucide.Wallet to GearText.primary
        NotificationItem.Kind.REVIEW -> Lucide.Star to GearColor.Warning500
    }
    val safety = item.kind == NotificationItem.Kind.SAFETY

    Row(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.sm)
            .background(if (safety) GearColor.HiVis100 else androidx.compose.ui.graphics.Color.Transparent),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClickLabel = "Open", onClick = onOpen)
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
                Text(item.time.uppercase(), style = GearType.dataS, color = GearText.tertiary)
                if (item.unread) {
                    VGap(6.dp)
                    Box(Modifier.size(8.dp).clip(GearShape.full).background(GearColor.HiVis600))
                }
            }
        }
        GearIconButton(Lucide.X, "Dismiss ${item.title}", onDelete, size = 44.dp, iconSize = IconSize.s)
    }
    GearDivider(Modifier.padding(vertical = 4.dp))
}
