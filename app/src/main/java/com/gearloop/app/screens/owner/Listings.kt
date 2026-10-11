package com.gearloop.app.screens.owner

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.container
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.EvidenceRepository
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.Routes
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.theme.Text
import com.gearloop.app.ui.vm.ExchangeSupplyViewModel
import com.gearloop.app.ui.vm.FitDiscoveryViewModel
import com.gearloop.app.ui.vm.gearViewModel
import java.io.File
import java.io.FileOutputStream

/* ------------------------------------------------------------------ your listings */

/**
 * Everything the member has listed, drafts and paused ones included: edit it,
 * pause or resume it, print its gear tag, or delete it.
 */
@Composable
fun MyListingsScreen(nav: Nav) {
    val vm: ExchangeSupplyViewModel = gearViewModel()
    val listings by vm.myListings.collectAsStateWithLifecycle()
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    GearScreen(
        appBar = {
            AppBar(
                title = "Your listings",
                onBack = { nav.back() },
                actions = { GearIconButton(Lucide.Plus, "Add a listing", { vm.startListing { nav.to("s25") } }, size = 44.dp) },
            )
        },
    ) {
        VGap(Space.s)
        if (listings.isEmpty()) {
            EmptyState(
                icon = Lucide.Tags,
                headline = "Nothing listed yet",
                body = "List something you own and it's in search straight away.",
                actionLabel = "Add a listing",
                onAction = { vm.startListing { nav.to("s25") } },
            )
        } else {
            Text(
                "${listings.count { it.status == ListingStatus.PUBLISHED }} live · ${listings.count { it.status == ListingStatus.PAUSED }} paused · ${listings.count { it.status == ListingStatus.DRAFT }} draft",
                style = GearType.dataM,
                color = GearText.secondary,
            )
        }
        listings.forEach { item ->
            VGap(Space.sm)
            ListingRow(
                item,
                onOpen = { if (item.status == ListingStatus.DRAFT) vm.editListing(item.id) { nav.to("s25") } else { vm.selectListing(item.id); nav.to("s07") } },
                onEdit = { vm.editListing(item.id) { nav.to("s25") } },
                onPause = { vm.setPaused(item, item.status == ListingStatus.PUBLISHED) },
                onTag = { vm.selectListing(item.id); nav.to(Routes.GEAR_TAG) },
                onDelete = { deleting = item.id },
            )
        }
        VGap(Space.section)
    }

    deleting?.let { id ->
        val item = listings.firstOrNull { it.id == id }
        GearDialog(
            title = "Delete ${item?.shortTitle ?: "this listing"}?",
            onDismiss = { deleting = null },
            confirmLabel = "Keep it",
            onConfirm = { deleting = null },
            destructiveLabel = "Delete",
            onDestructive = { item?.let(vm::deleteListing); deleting = null },
        ) {
            Text("It comes out of search and off your calendar. Past bookings stay in your history.", style = GearType.bodyM, color = GearText.secondary)
        }
    }
}

@Composable
private fun ListingRow(item: GearItem, onOpen: () -> Unit, onEdit: () -> Unit, onPause: () -> Unit, onTag: () -> Unit, onDelete: () -> Unit) {
    GearCard(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open ${item.shortTitle}", onClick = onOpen).padding(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GearImage(item.hero, item.title, Modifier.size(64.dp), GearShape.sm)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(item.shortTitle, style = GearType.labelL, color = GearText.primary, maxLines = 1)
                    Text("${lkr(item.pricePerDay)} a day · ${item.code}", style = GearType.dataS, color = GearText.tertiary)
                }
                StatusBadge(
                    when (item.status) {
                        ListingStatus.PUBLISHED -> "Live"
                        ListingStatus.PAUSED -> "Paused"
                        ListingStatus.DRAFT -> "Draft"
                    },
                    when (item.status) {
                        ListingStatus.PUBLISHED -> GearColor.Success500
                        ListingStatus.PAUSED -> GearColor.Chalk600
                        ListingStatus.DRAFT -> GearColor.Warning700
                    },
                )
            }
            GearDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.s), horizontalArrangement = Arrangement.SpaceBetween) {
                GearButton("Edit", onEdit, type = ButtonType.GHOST, size = ButtonSize.S, icon = Lucide.Pencil)
                if (item.status != ListingStatus.DRAFT) {
                    GearButton(if (item.status == ListingStatus.PUBLISHED) "Pause" else "Resume", onPause, type = ButtonType.GHOST, size = ButtonSize.S, icon = if (item.status == ListingStatus.PUBLISHED) Lucide.CircleMinus else Lucide.Play)
                    GearButton("Tag", onTag, type = ButtonType.GHOST, size = ButtonSize.S, icon = Lucide.QrCode)
                }
                GearButton("Delete", onDelete, type = ButtonType.GHOST, size = ButtonSize.S, icon = Lucide.Trash2)
            }
        }
    }
}

/* ------------------------------------------------------------------ the gear tag */

/**
 * The tag that goes on the gear: its code as a QR, which the renter's camera
 * reads at handover, and the same code printed under it. Share it to print it,
 * or show it on this screen at pickup.
 */
@Composable
fun GearTagScreen(nav: Nav) {
    val vm: FitDiscoveryViewModel = gearViewModel()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val item = selected ?: return
    val payload = EvidenceRepository.TAG_PREFIX + item.code
    val dark = GearColor.Pine900.toArgb()
    val light = GearColor.Chalk000.toArgb()

    GearScreen(
        appBar = { AppBar(title = "Gear tag", onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    "Share or print the tag",
                    {
                        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                        val file = File(dir, "gearloop-tag-${item.code}.png")
                        FileOutputStream(file).use { qrAndroidBitmap(payload, 900, dark, light).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        val send = Intent(Intent.ACTION_SEND)
                            .setType("image/png")
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .putExtra(Intent.EXTRA_TEXT, "GearLoop tag for ${item.title} · ${item.code}")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        runCatching { context.startActivity(Intent.createChooser(send, "Share the tag")) }
                            .onFailure { context.container.toaster.show("No app on this phone can share an image") }
                    },
                    Modifier.fillMaxWidth(),
                    icon = Lucide.Printer,
                )
            }
        },
    ) {
        VGap(Space.m)
        Text(item.title, style = GearType.headingL, color = GearText.primary)
        VGap(Space.s)
        Text(
            "Print it and tie it to the gear. The renter scans it at pickup.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.l)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GearShape.md)
                .background(GearSurface.card)
                .padding(Space.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GearTag(item.tagLine, size = TagSize.FULL)
            VGap(Space.m)
            QrCode(payload, Modifier.fillMaxWidth(0.8f).aspectRatio(1f), contentDescription = "QR code for ${item.code}")
            VGap(Space.m)
            Text(item.code, style = GearType.dataXl, color = GearText.primary, textAlign = TextAlign.Center)
            Text("TYPE THIS IF THE QR WON'T SCAN", style = GearType.labelXs, color = GearText.tertiary)
        }
        VGap(Space.section)
    }
}
