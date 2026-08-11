package com.gearloop.app.screens.owner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gearloop.app.data.Demo
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.lkr
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
/* ------------------------------------------------------------------ S-24 */

@Composable
fun S24OwnerDashboard(nav: Nav) {
    GearScreen(
        appBar = {
            AppBar(
                title = "Your gear",
                actions = { GearIconButton(Lucide.Settings, "Settings", { }, size = 40.dp) },
            )
        },
        tabBar = { MainTabBar(nav, MainTab.PROFILE) },
    ) {
        VGap(Space.s)
        EarningsHero()

        VGap(Space.sm)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            StatTile("${Demo.OCCUPANCY}%", "Occupancy", Modifier.weight(1f))
            StatTile("${Demo.kavitha.listings}", "Listings", Modifier.weight(1f))
            StatTile("★ ${Demo.kavitha.ratingText}", "Rating", Modifier.weight(1f))
        }

        VGap(Space.l)
        Eyebrow("Needs you now")
        VGap(Space.s)
        Demo.needsYouNow.forEach { row ->
            AttentionRow(row) {
                when (row.action) {
                    "Review" -> nav.to("s28")
                    "Block dates" -> nav.to("s27")
                    else -> nav.to("s33")
                }
            }
            VGap(Space.s)
        }

        VGap(Space.m)
        Eyebrow("Today")
        VGap(Space.s)
        GearCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Space.cardPadding)) {
                Demo.todayRows.forEachIndexed { i, (time, who, what) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(time, style = GearType.dataM, color = GearText.primary, modifier = Modifier.width(56.dp))
                        Column(Modifier.weight(1f)) {
                            Text(who, style = GearType.labelL, color = GearText.primary)
                            Text(what, style = GearType.bodyS, color = GearText.tertiary)
                        }
                        GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
                    }
                    if (i != Demo.todayRows.lastIndex) GearDivider()
                }
            }
        }

        VGap(Space.l)
        Eyebrow("Worth doing")
        VGap(Space.s)
        Demo.ownerInsights.forEach { insight ->
            InsightCard(insight.headline, insight.body, insight.action, {
                if (insight.action == "Add photos") nav.to("s25") else nav.to("s26")
            })
            VGap(Space.s)
        }

        VGap(Space.m)
        GearButton(
            "Add a listing", { nav.to("s25") }, Modifier.fillMaxWidth(),
            icon = Lucide.Plus,
        )
        VGap(Space.section)
    }
}

@Composable
private fun EarningsHero() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(GearShape.md)
            .background(GearColor.Pine800)
            .padding(Space.cardPadding),
    ) {
        Text("THIS MONTH", style = GearType.labelXs, color = GearColor.HiVis500)
        VGap(6.dp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(lkr(Demo.EARNINGS_MONTH), style = GearType.dataXl, color = GearText.onDark)
            Spacer(Modifier.weight(1f))
            Sparkline(
                Demo.earningsSpark,
                Modifier.width(96.dp).height(36.dp),
                color = GearColor.HiVis500,
            )
        }
        VGap(Space.sm)
        Text(
            "Next payout ${lkr(Demo.NEXT_PAYOUT)} · ${Demo.NEXT_PAYOUT_DATE}",
            style = GearType.bodyS,
            color = GearColor.Chalk300,
        )
    }
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - (v.coerceIn(0f, 1f) * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun AttentionRow(row: NeedsAttention, onAction: () -> Unit) {
    val accent = if (row.severity == NeedsAttention.Severity.URGENT) {
        GearColor.Danger500
    } else {
        GearColor.Warning500
    }
    AccentCard(accent, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.title, style = GearType.headingS, color = GearText.primary)
                VGap(3.dp)
                Text(row.detail, style = GearType.bodyS, color = GearText.secondary)
            }
            Spacer(Modifier.width(Space.s))
            GearButton(row.action, onAction, type = ButtonType.SECONDARY, size = ButtonSize.S)
        }
    }
}

