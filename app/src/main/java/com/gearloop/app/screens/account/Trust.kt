package com.gearloop.app.screens.account

import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.ink
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import com.gearloop.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.R
import com.gearloop.app.data.Demo
import com.gearloop.app.data.TrustTier
import com.gearloop.app.data.VerificationStep
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.Routes
import com.gearloop.app.container
import com.gearloop.app.data.repo.EvidenceRepository
import com.gearloop.app.data.lkr
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.gearloop.app.screens.MainTab
import com.gearloop.app.screens.MainTabBar
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Metrics
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.vm.EvidenceTrustViewModel
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel

/* ------------------------------------------------------------------ S-32 */

/**
 * The Profile tab: who you are, everything that is yours, and your trust score
 * — recomputed from the verification steps, completed rentals and upheld
 * claims rather than typed. Identity and reputation stay separate facts (FR-07).
 */
@Composable
fun S32TrustProfile(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val trust by vm.trust.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val steps by vm.verification.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val app = androidx.compose.ui.platform.LocalContext.current.container
    val unread by app.discovery.unreadNotifications.collectAsStateWithLifecycle()
    val requests by app.exchange.requests.collectAsStateWithLifecycle()
    val addressDone = steps.any { it.isHomeAddress && it.state == VerificationStep.State.DONE }
    val me = account?.person

    GearScreen(
        appBar = {
            AppBar(
                title = "Profile",
                actions = { GearIconButton(Lucide.Settings, "Settings", { nav.to(Routes.SETTINGS) }, size = 44.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.PROFILE) },
    ) {
        VGap(Space.s)
        if (me != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(me, size = 64.dp, showTierRing = true)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        Text(me.name, style = GearType.headingL, color = GearText.primary)
                        TrustBadge(me.copy(tier = trust.tier))
                    }
                    Text(account?.email.orEmpty(), style = GearType.bodyS, color = GearText.secondary)
                    Text("Member since ${me.memberSince}${if (me.area.isNotBlank()) " · ${me.area}" else ""}", style = GearType.bodyS, color = GearText.tertiary)
                }
            }
            VGap(Space.m)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                StatTile(lkr(profile?.walletAvailable ?: 0), "Wallet", Modifier.weight(1f).clickable(role = Role.Button) { nav.to("s35") })
                StatTile("${trust.score}", "Trust", Modifier.weight(1f))
                StatTile(if (me.reviews > 0) "★ ${me.ratingText}" else "New", "Rating", Modifier.weight(1f))
            }
        }

        // A renter is waiting on the answer, so an owner with a request gets it at
        // the top of their profile, a tap away, not three screens down.
        if (requests.isNotEmpty()) {
            VGap(Space.l)
            Banner(
                title = if (requests.size == 1) "1 booking request is waiting" else "${requests.size} booking requests are waiting",
                text = "Answer each within 24 hours of asking.",
                kind = BannerKind.WARNING,
                action = "See requests",
                onAction = { nav.to("s28") },
            )
        }

        VGap(Space.l)
        listOf(
            Triple(Lucide.Wallet, "Wallet and payments", "s35"),
            Triple(Lucide.Store, "Lend your gear", "s24"),
            Triple(Lucide.Tags, "Your listings", Routes.MY_LISTINGS),
            Triple(Lucide.Boxes, "Gear closet", "s36"),
            Triple(Lucide.Heart, "Saved gear", Routes.SAVED),
            Triple(Lucide.Bell, if (unread > 0) "Notifications ($unread new)" else "Notifications", "s38"),
            Triple(Lucide.Ruler, "Fit profile and sports", "s02"),
            Triple(Lucide.BadgeCheck, "Verification", "s41"),
            Triple(Lucide.CircleUser, "Your public profile", "s37"),
        ).plus(if (account?.isAdmin == true) listOf(Triple(Lucide.Scale, "Claims desk", "s40")) else emptyList())
            .forEach { (icon, label, route) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            // Your own public profile, as other members see it.
                            if (route == "s37" && me != null) vm.showProfile(me.handle)
                            nav.to(route)
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GearIcon(icon, null, size = IconSize.m, tint = GearText.primary)
                    Spacer(Modifier.width(Space.sm))
                    Text(label, style = GearType.labelL, color = GearText.primary, modifier = Modifier.weight(1f))
                    GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
                }
                GearDivider()
            }

        VGap(Space.l)
        Eyebrow("Your trust score")
        VGap(Space.m)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing(progress = trust.score / 1000f, size = 160.dp, color = GearColor.Pine800) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${trust.score}", style = GearType.dataXl, color = GearText.primary)
                    // The tier, in the tier's own ink — never in hi-vis.
                    Text(trust.tier.label, style = GearType.labelXs, color = trust.tier.ink)
                }
            }
            VGap(Space.m)
            Text(
                if (trust.tier == TrustTier.GOLD) "You're GOLD" else "${trust.pointsToGold} points to GOLD",
                style = GearType.bodyM,
                color = GearText.secondary,
            )
            VGap(Space.s)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(GearShape.full).background(GearColor.Chalk200)) {
                Box(Modifier.fillMaxWidth(trust.progress).height(8.dp).clip(GearShape.full).background(GearColor.HiVis500))
            }
        }

        VGap(Space.l)
        Eyebrow("What makes up your score")
        VGap(Space.s)
        trust.factors.forEach { factor ->
            RatingBar(label = factor.label, fraction = factor.progress, modifier = Modifier.padding(vertical = 5.dp), value = factor.value)
        }
        VGap(Space.s)
        Text(
            "+30 for each verification step, +6 for each rental, −40 for an upheld claim.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )

        VGap(Space.l)
        Eyebrow("Unlock at GOLD")
        VGap(Space.s)
        Demo.goldUnlocks.forEach {
            IconRow(
                if (trust.tier == TrustTier.GOLD) Lucide.LockOpen else Lucide.Lock, it,
                Modifier.padding(vertical = 6.dp),
                tint = GearColor.Chalk400,
                textColor = GearText.secondary,
            )
        }

        VGap(Space.l)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Badges")
            Spacer(Modifier.weight(1f))
            Text("Tap one to show it on your profile", style = GearType.bodyS, color = GearText.tertiary)
        }
        VGap(Space.s)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            badges.forEach { badge ->
                Box(
                    Modifier
                        .heightIn(min = Metrics.tapTarget)
                        .then(
                            if (badge.earned) Modifier.toggleable(value = badge.featured, role = Role.Switch) { vm.toggleFeatured(badge) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier
                            .clip(GearShape.full)
                            .background(
                                when {
                                    badge.featured -> GearColor.HiVis500
                                    badge.earned -> GearColor.HiVis200
                                    else -> GearColor.Chalk100
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        GearIcon(
                            when {
                                badge.featured -> Lucide.Eye
                                badge.earned -> Lucide.Award
                                else -> Lucide.Lock
                            },
                            null, size = IconSize.s,
                            tint = if (badge.earned) GearColor.Pine900 else GearColor.Chalk400,
                        )
                        Text(badge.name, style = GearType.labelM, color = if (badge.earned) GearColor.Pine900 else GearColor.Chalk400)
                    }
                }
            }
        }

        if (!addressDone) {
            VGap(Space.l)
            GearButton("Continue verification", { nav.to("s41") }, Modifier.fillMaxWidth(), icon = Lucide.BadgeCheck)
        }
        VGap(Space.section)
    }
}

/* ------------------------------------------------------------------ S-37 */

/**
 * S-37 · Read the public profile of whoever owns the listing in view — their
 * listings and the reviews written about them; Update the saved state of any of
 * their listings. Contact details are never part of it.
 */
@Composable
fun S37PublicProfile(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val discovery: FitDiscoveryViewModel = gearViewModel()
    val selected by discovery.selected.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val listings by vm.listings.collectAsStateWithLifecycle()
    val chosen by vm.profileHandle.collectAsStateWithLifecycle()
    // Whoever was chosen on the way here — a listing's owner, or the renter
    // from their own Profile tab — else the owner of the listing in view.
    val handle = chosen ?: selected?.owner?.handle ?: vm.me
    val person = people[handle] ?: return
    val mine = handle == vm.me
    // From every listing, not from search: search leaves out the renter's own.
    val theirs = listings.filter { it.owner.handle == handle && it.status == ListingStatus.PUBLISHED }
    val about = reviews.filter { it.subject.handle == handle }
    val first = person.name.substringBefore(' ')

    GearScreen(contentPadding = 0.dp) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            GearImage(R.drawable.place_casuarina, "Cover", Modifier.fillMaxSize())
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(Space.sm)) {
                GearIconButton(Lucide.ChevronLeft, "Back", { nav.back() }, tone = IconButtonTone.ON_IMAGE, iconSize = IconSize.l)
            }
        }
        Column(Modifier.padding(horizontal = Space.screen)) {
            Box(Modifier.offset(y = (-32).dp)) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(GearShape.full)
                        .background(GearSurface.overImage)
                        .padding(3.dp),
                ) {
                    Avatar(person, size = 66.dp, showTierRing = true)
                }
            }
            Column(Modifier.offset(y = (-20).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Text(person.name, style = GearType.headingXl, color = GearText.primary)
                    TrustBadge(person)
                }
                VGap(4.dp)
                Text(
                    "Member since ${person.memberSince} · ${person.area}",
                    style = GearType.bodyS,
                    color = GearText.secondary,
                )

                VGap(Space.m)
                val stats = vm.trustStats(handle)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    StatTile("${stats.first}", "Rentals", Modifier.weight(1f))
                    StatTile(if (person.reviews > 0) "★ ${person.ratingText}" else "New", "Rating", Modifier.weight(1f))
                    StatTile(person.respondsIn.ifBlank { "—" }, "Replies", Modifier.weight(1f))
                }

                VGap(Space.m)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    theirs.map { it.sport }.distinct().take(3).forEach { Chip(it) }
                }

                VGap(Space.m)
                Banner(
                    text = if (mine) "This is how others see you. Your address and number stay hidden."
                    else "Exact location and contact details are never shown until a booking exists.",
                    kind = BannerKind.INFO,
                )
                VGap(Space.s)
                if (mine) {
                    GearButton(
                        "Your gear closet", { nav.to("s36") }, Modifier.fillMaxWidth(),
                        type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Boxes,
                    )
                } else {
                    GearButton(
                        "Message $first", { vm.openThread(handle); nav.to("s33") }, Modifier.fillMaxWidth(),
                        type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.MessageSquare,
                    )
                }

                VGap(Space.l)
                Eyebrow("${if (mine) "Your" else "Their"} listings (${theirs.size})")
                VGap(Space.s)
                if (theirs.isEmpty()) {
                    Text(
                        if (mine) "Nothing listed for rent yet."
                        else "Nothing listed for rent right now.",
                        style = GearType.bodyS,
                        color = GearText.tertiary,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    theirs.forEach {
                        ListingCard(
                            it, { discovery.select(it); nav.to("s07") }, layout = ListingLayout.GRID,
                            onToggleSave = { discovery.toggleSaved(it) },
                        )
                    }
                }

                VGap(Space.l)
                Eyebrow("Reviews received (${person.reviews})")
                VGap(Space.s)
                if (about.isEmpty()) {
                    Text("No written reviews yet.", style = GearType.bodyS, color = GearText.tertiary)
                }
                about.take(3).forEach {
                    ReviewCard(it.entry)
                    VGap(Space.sm)
                }
                VGap(Space.section)
            }
        }
    }
}

/* ------------------------------------------------------------------ S-41 */

/**
 * The verification steps, in order. Each one asks for the one thing it names —
 * a mobile number, a photo of an ID, a selfie, a card, an address — and
 * unlocks something specific. The address can be withdrawn, which re-opens it.
 */
@Composable
fun S41Verification(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val steps by vm.verification.collectAsStateWithLifecycle()
    val details by vm.verificationDetails.collectAsStateWithLifecycle()
    val address by vm.address.collectAsStateWithLifecycle()
    var verifying by rememberSaveable { mutableStateOf<String?>(null) }
    val done = steps.count { it.state == VerificationStep.State.DONE }
    val words = listOf("None", "One", "Two", "Three", "Four", "Five", "Six")

    GearScreen(appBar = { AppBar(title = "Verification", onBack = { nav.back() }) }) {
        VGap(Space.m)
        Text(
            if (done == steps.size && steps.isNotEmpty()) "All six done" else "${words.getOrElse(done) { "$done" }} of six done",
            style = GearType.headingXl,
            color = GearText.primary,
        )
        VGap(Space.l)
        steps.forEachIndexed { i, step ->
            VerificationRow(
                step,
                isLast = i == steps.lastIndex,
                detail = when {
                    step.isHomeAddress && step.state == VerificationStep.State.DONE -> address
                    step.state == VerificationStep.State.DONE -> details[step.title]?.takeIf { !it.startsWith("file:") }
                    else -> null
                },
                removable = step.isHomeAddress,
                onVerify = { verifying = step.title },
                onRemove = { vm.removeAddress() },
            )
        }
        VGap(Space.section)
    }

    verifying?.let { title -> VerifyDialog(vm, title) { verifying = null } }
}

/** The dialog for one step, asking only for what that step needs. */
@Composable
private fun VerifyDialog(vm: EvidenceTrustViewModel, title: String, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selfie = title == "Selfie match"
    val picker = rememberPhotoPicker(
        title = if (selfie) "A selfie" else "Your ID",
        onTaken = { file -> scope.launch { photo = vm.adoptPhoto(file) } },
        onChosen = if (selfie) null else ({ uri -> scope.launch { photo = vm.importPhoto(uri) } }),
        front = selfie,
    )
    val proof = when (title) {
        "Phone number" -> EvidenceRepository.Proof(phone = text)
        "Government ID", "Selfie match" -> EvidenceRepository.Proof(photoKey = photo)
        "Payment method" -> EvidenceRepository.Proof(cardNumber = text, cardExpiry = expiry)
        else -> EvidenceRepository.Proof(address = text)
    }
    GearDialog(
        title = when (title) {
            "Phone number" -> "Verify your mobile number"
            "Government ID" -> "Photograph your ID"
            "Selfie match" -> "Take a selfie"
            "Payment method" -> "Add a payment card"
            else -> "Verify your home address"
        },
        onDismiss = onDismiss,
        confirmLabel = "Verify",
        onConfirm = { vm.verify(proof) { e -> if (e == null) onDismiss() else error = e } },
        confirmEnabled = when (title) {
            "Government ID", "Selfie match" -> photo != null
            "Payment method" -> text.filter(Char::isDigit).length >= 13 && expiry.filter(Char::isDigit).length == 4
            else -> text.trim().length >= 8
        },
    ) {
        Text(
            when (title) {
                "Phone number" -> "Lets owners message you about a booking. It's never shown on your profile."
                "Government ID" -> "A clear photo of your NIC, passport or driving licence. It stays on this phone."
                "Selfie match" -> "A clear photo of your face. It stays on this phone."
                "Payment method" -> "Only the brand and last four digits are kept. Deposits are held, not charged."
                else -> "Never shown on your profile."
            },
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.s)
        when (title) {
            "Phone number" -> GearTextField(text, label = "Mobile number", placeholder = "077 123 4567", onValueChange = { text = it; error = null }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone, error = error)
            "Government ID", "Selfie match" -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    photo?.let { GearImage(it, "Photo", Modifier.size(72.dp), GearShape.sm) }
                    GearButton(if (photo == null) "Take the photo" else "Retake", { picker.open() }, type = ButtonType.SECONDARY, size = ButtonSize.M, icon = Lucide.Camera)
                }
                error?.let { VGap(Space.xs); Text(it, style = GearType.bodyS, color = GearColor.Danger500) }
            }
            "Payment method" -> {
                GearTextField(text, label = "Card number", onValueChange = { text = it.filter(Char::isDigit).take(19); error = null }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, visualTransformation = CardNumberTransformation)
                VGap(Space.s)
                GearTextField(expiry, label = "Expiry", placeholder = "MM/YY", onValueChange = { expiry = it.filter(Char::isDigit).take(4); error = null }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, error = error, visualTransformation = ExpiryTransformation)
            }
            else -> GearTextField(text, label = "Address", placeholder = "e.g. 11 Stanley Rd, Jaffna", onValueChange = { text = it; error = null }, singleLine = false, error = error)
        }
    }
}

@Composable
private fun VerificationRow(
    step: VerificationStep,
    isLast: Boolean,
    detail: String?,
    removable: Boolean,
    onVerify: () -> Unit,
    onRemove: () -> Unit,
) {
    val done = step.state == VerificationStep.State.DONE
    val current = step.state == VerificationStep.State.CURRENT

    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(GearShape.full)
                    .background(
                        when {
                            done -> GearColor.Success500
                            current -> GearColor.HiVis500
                            else -> GearColor.Chalk200
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(
                    if (done) Lucide.Check else Lucide.Lock,
                    null, size = 13.dp,
                    tint = if (done) GearText.onDark else GearText.onHiVis,
                    strokeWidth = 2.5f,
                )
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(if (current || detail != null) 96.dp else 52.dp)
                        .background(GearColor.Chalk200)
                )
            }
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f).padding(bottom = Space.m)) {
            Text(
                step.title,
                style = GearType.headingS,
                color = if (step.state == VerificationStep.State.LOCKED) GearText.tertiary else GearText.primary,
            )
            VGap(3.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                GearIcon(Lucide.ArrowRight, null, size = 12.dp, tint = GearText.tertiary)
                Text("Unlocks: ${step.unlocks}", style = GearType.bodyS, color = GearText.secondary)
            }
            if (current) {
                VGap(Space.s)
                GearButton("Verify now", onVerify, type = ButtonType.PRIMARY, size = ButtonSize.M, icon = Lucide.BadgeCheck)
            }
            if (detail != null) {
                VGap(Space.s)
                Text(detail, style = GearType.bodyS, color = GearText.body)
                if (removable) GearLink("Remove this address", onRemove, color = GearText.tertiary, icon = Lucide.Trash2)
            }
        }
    }
}
