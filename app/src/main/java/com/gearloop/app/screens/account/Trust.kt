package com.gearloop.app.screens.account

import com.gearloop.app.ui.theme.GearSurface
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gearloop.app.R
import com.gearloop.app.data.Demo
import com.gearloop.app.data.VerificationStep
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
import com.gearloop.app.ui.theme.Space
/* ------------------------------------------------------------------ S-32 */

@Composable
fun S32TrustProfile(nav: Nav) {
    GearScreen(
        appBar = {
            AppBar(
                title = "Trust",
                actions = { GearIconButton(Lucide.Settings, "Settings", { nav.to("s41") }, size = 40.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.PROFILE) },
    ) {
        VGap(Space.m)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing(
                progress = Demo.TRUST_SCORE / 1000f,
                size = 160.dp,
                color = GearColor.Pine800,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${Demo.TRUST_SCORE}", style = GearType.dataXl, color = GearText.primary)
                    Text("SILVER", style = GearType.labelXs, color = GearColor.HiVis600)
                }
            }
            VGap(Space.m)
            Text(
                "${Demo.POINTS_TO_GOLD} points to GOLD",
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
                        .fillMaxWidth(0.78f)
                        .height(8.dp)
                        .clip(GearShape.full)
                        .background(GearColor.HiVis500)
                )
            }
        }

        VGap(Space.l)
        Eyebrow("What makes up your score")
        VGap(Space.s)
        Demo.scoreFactors.forEach { factor ->
            RatingBar(
                label = factor.label,
                fraction = factor.progress,
                modifier = Modifier.padding(vertical = 5.dp),
                value = factor.value,
            )
        }

        VGap(Space.l)
        Eyebrow("Unlock at GOLD")
        VGap(Space.s)
        Demo.goldUnlocks.forEach {
            IconRow(
                Lucide.Lock, it,
                Modifier.padding(vertical = 6.dp),
                tint = GearColor.Chalk400,
                textColor = GearText.secondary,
            )
        }

        VGap(Space.l)
        Eyebrow("Badges")
        VGap(Space.s)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Demo.badges.forEach { (name, earned) ->
                Row(
                    Modifier
                        .clip(GearShape.full)
                        .background(if (earned) GearColor.HiVis200 else GearColor.Chalk100)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GearIcon(
                        if (earned) Lucide.Award else Lucide.Lock,
                        null, size = IconSize.s,
                        tint = if (earned) GearColor.Pine900 else GearColor.Chalk400,
                    )
                    Text(
                        name,
                        style = GearType.labelM,
                        color = if (earned) GearColor.Pine900 else GearColor.Chalk400,
                    )
                }
            }
        }

        VGap(Space.l)
        GearButton("Verify your address", { nav.to("s41") }, Modifier.fillMaxWidth(), icon = Lucide.House)
        VGap(Space.s)
        Text(
            "The one thing left. It unlocks delivery and lets you list your own gear.",
            style = GearType.bodyS,
            color = GearText.tertiary,
        )

        VGap(Space.l)
        GearDivider()
        VGap(Space.s)
        listOf(
            Triple(Lucide.Wallet, "Wallet & deposits", "s35"),
            Triple(Lucide.Boxes, "Gear closet", "s36"),
            Triple(Lucide.ChartColumn, "Owner dashboard", "s24"),
            Triple(Lucide.BadgeCheck, "Verification", "s41"),
            Triple(Lucide.CircleUser, "Public profile", "s37"),
        ).forEach { (icon, label, route) ->
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { nav.to(route) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearIcon(icon, null, size = IconSize.m, tint = GearColor.Pine800)
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

@Composable
fun S37PublicProfile(nav: Nav) {
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
                    Avatar(Demo.kavitha, size = 66.dp, showTierRing = true)
                }
            }
            Column(Modifier.offset(y = (-20).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Text(Demo.kavitha.name, style = GearType.headingXl, color = GearText.primary)
                    TrustBadge(Demo.kavitha)
                }
                VGap(4.dp)
                Text(
                    "Member since ${Demo.kavitha.memberSince} · ${Demo.kavitha.area}",
                    style = GearType.bodyS,
                    color = GearText.secondary,
                )

                VGap(Space.m)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    StatTile("14", "Rentals", Modifier.weight(1f))
                    StatTile("★ ${Demo.kavitha.rating}", "Rating", Modifier.weight(1f))
                    StatTile("92%", "Response", Modifier.weight(1f))
                }

                VGap(Space.m)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Chip("Surfing", icon = Lucide.Waves)
                    Chip("SUP")
                    Chip("Kayaking")
                }

                VGap(Space.m)
                Banner(
                    text = "Exact location and contact details are never shown on a public profile. " +
                        "They unlock for the other party only once a booking exists.",
                    kind = BannerKind.INFO,
                )

                VGap(Space.l)
                Eyebrow("Their listings")
                VGap(Space.s)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberSavedScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    listOf(Demo.torq, Demo.wetsuit, Demo.kayak, Demo.helmet).forEach {
                        ListingCard(it, { nav.to("s07") }, layout = ListingLayout.GRID)
                    }
                }

                VGap(Space.l)
                Eyebrow("Reviews received")
                VGap(Space.s)
                Demo.torqReviews.take(2).forEach {
                    ReviewCard(it)
                    VGap(Space.sm)
                }
                VGap(Space.section)
            }
        }
    }
}

/* ------------------------------------------------------------------ S-41 */

@Composable
fun S41Verification(nav: Nav) {
    GearScreen(
        appBar = { AppBar(title = "Verification", onBack = { nav.back() }) },
    ) {
        VGap(Space.m)
        Text("Five of six done", style = GearType.headingXl, color = GearText.primary)
        VGap(Space.s)
        Text(
            "Each step unlocks something specific. Nothing is asked for without a reason.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )

        VGap(Space.l)
        Demo.verificationSteps.forEachIndexed { i, step ->
            VerificationRow(step, isLast = i == Demo.verificationSteps.lastIndex)
        }
        VGap(Space.section)
    }
}

@Composable
private fun VerificationRow(step: VerificationStep, isLast: Boolean) {
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
                        .height(if (current) 96.dp else 52.dp)
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
                GearButton("Verify now", { }, type = ButtonType.PRIMARY, size = ButtonSize.M, icon = Lucide.House)
            }
        }
    }
}
