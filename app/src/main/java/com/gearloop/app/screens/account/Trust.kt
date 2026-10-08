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
import com.gearloop.app.data.repo.EvidenceRepository
import com.gearloop.app.data.repo.Seed
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.nav.Nav
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
 * S-32 · Read the trust score and what it is made of — recomputed from the
 * verification steps, completed rentals and upheld claims rather than typed —
 * and the badges; Update which earned badges show on the public profile.
 *
 * Identity and reputation stay separate facts on this screen (FR-07): the score
 * is never shown on its own as if it were enough.
 */
@Composable
fun S32TrustProfile(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val trust by vm.trust.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val steps by vm.verification.collectAsStateWithLifecycle()
    val addressDone = steps.any { it.isHomeAddress && it.state == VerificationStep.State.DONE }

    GearScreen(
        appBar = {
            AppBar(
                title = "Trust",
                actions = { GearIconButton(Lucide.Settings, "Verification settings", { nav.to("s41") }, size = 44.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.PROFILE) },
    ) {
        VGap(Space.m)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing(
                progress = trust.score / 1000f,
                size = 160.dp,
                color = GearColor.Pine800,
            ) {
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(GearShape.full)
                    .background(GearColor.Chalk200),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(trust.progress)
                        .height(8.dp)
                        .clip(GearShape.full)
                        .background(GearColor.HiVis500)
                )
            }
        }

        VGap(Space.l)
        Eyebrow("What makes up your score")
        VGap(Space.s)
        trust.factors.forEach { factor ->
            RatingBar(
                label = factor.label,
                fraction = factor.progress,
                modifier = Modifier.padding(vertical = 5.dp),
                value = factor.value,
            )
        }
        VGap(Space.s)
        Text(
            "Each verification step adds ${30} points and each completed rental ${6}. A damage claim upheld against you takes 40 off.",
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
            Text("Tap an earned badge to show it on your profile", style = GearType.bodyS, color = GearText.tertiary)
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
                        Text(
                            badge.name,
                            style = GearType.labelM,
                            color = if (badge.earned) GearColor.Pine900 else GearColor.Chalk400,
                        )
                    }
                }
            }
        }

        if (!addressDone) {
            VGap(Space.l)
            GearButton("Verify your address", { nav.to("s41") }, Modifier.fillMaxWidth(), icon = Lucide.House)
            VGap(Space.s)
            Text(
                "The one thing left. It unlocks delivery and lets you list your own gear.",
                style = GearType.bodyS,
                color = GearText.tertiary,
            )
        }

        VGap(Space.l)
        GearDivider()
        VGap(Space.s)
        listOf(
            Triple(Lucide.Wallet, "Wallet & deposits", "s35"),
            Triple(Lucide.Boxes, "Gear closet", "s36"),
            Triple(Lucide.Bell, "Notifications", "s38"),
            Triple(Lucide.ChartColumn, "Owner dashboard (Kavitha)", "s24"),
            Triple(Lucide.BadgeCheck, "Verification", "s41"),
            Triple(Lucide.CircleUser, "Your public profile", "s37"),
        ).forEach { (icon, label, route) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        // Your own public profile, as other members see it.
                        if (route == "s37") vm.showProfile(Seed.ME)
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
    val handle = chosen ?: selected?.owner?.handle ?: Demo.kavitha.handle
    val person = people[handle] ?: return
    val mine = handle == Seed.ME
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
                    StatTile("★ ${person.ratingText}", "Rating", Modifier.weight(1f))
                    StatTile(person.respondsIn, "Responds", Modifier.weight(1f))
                }

                VGap(Space.m)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    theirs.map { it.sport }.distinct().take(3).forEach { Chip(it) }
                }

                VGap(Space.m)
                Banner(
                    text = if (mine) {
                        "This is how other members see you. Your address and number are never part of it."
                    } else {
                        "Exact location and contact details are never shown on a public profile. " +
                            "They unlock for the other party only once a booking exists."
                    },
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
                        if (mine) "Nothing listed for rent yet. Anything in your closet can be listed in a couple of minutes."
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
 * S-41 · Read the verification steps; Update the current one — the home address
 * step stores the address it verifies, which unlocks delivery and listing your
 * own gear; Delete the address again, which re-opens the step.
 */
@Composable
fun S41Verification(nav: Nav) {
    val vm: EvidenceTrustViewModel = gearViewModel()
    val steps by vm.verification.collectAsStateWithLifecycle()
    val address by vm.address.collectAsStateWithLifecycle()
    var verifying by rememberSaveable { mutableStateOf(false) }
    var typed by rememberSaveable { mutableStateOf("") }
    val done = steps.count { it.state == VerificationStep.State.DONE }
    val words = listOf("None", "One", "Two", "Three", "Four", "Five", "Six")

    GearScreen(
        appBar = { AppBar(title = "Verification", onBack = { nav.back() }) },
    ) {
        VGap(Space.m)
        Text(
            if (done == steps.size && steps.isNotEmpty()) "All six done" else "${words.getOrElse(done) { "$done" }} of six done",
            style = GearType.headingXl,
            color = GearText.primary,
        )
        VGap(Space.s)
        Text(
            "Each step unlocks something specific. Nothing is asked for without a reason.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )

        VGap(Space.l)
        steps.forEachIndexed { i, step ->
            VerificationRow(
                step,
                isLast = i == steps.lastIndex,
                detail = if (step.isHomeAddress && step.state == VerificationStep.State.DONE) address else null,
                onVerify = { typed = ""; verifying = true },
                onRemove = { vm.removeAddress() },
            )
        }
        VGap(Space.section)
    }

    if (verifying) {
        GearDialog(
            title = "Verify your home address",
            onDismiss = { verifying = false },
            confirmLabel = "Verify",
            onConfirm = { vm.verifyCurrent(typed) { ok -> if (ok) verifying = false } },
            confirmEnabled = typed.trim().length >= 8,
        ) {
            Text(
                "Used only to unlock delivery and to list your own gear. It is never shown on your profile.",
                style = GearType.bodyS,
                color = GearText.secondary,
            )
            VGap(Space.s)
            GearTextField(typed, label = "Address", placeholder = "e.g. 11 Stanley Rd, Jaffna", onValueChange = { typed = it }, singleLine = false)
        }
    }
}

@Composable
private fun VerificationRow(
    step: VerificationStep,
    isLast: Boolean,
    detail: String?,
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
                GearButton("Verify now", onVerify, type = ButtonType.PRIMARY, size = ButtonSize.M, icon = Lucide.House)
            }
            if (detail != null) {
                VGap(Space.s)
                Text(detail, style = GearType.bodyS, color = GearText.body)
                GearLink("Remove this address", onRemove, color = GearText.tertiary, icon = Lucide.Trash2)
            }
        }
    }
}
