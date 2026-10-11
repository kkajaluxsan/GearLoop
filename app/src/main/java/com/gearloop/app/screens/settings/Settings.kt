package com.gearloop.app.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.BuildConfig
import com.gearloop.app.data.repo.AccountRepository
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.Routes
import com.gearloop.app.screens.auth.areas
import com.gearloop.app.ui.components.*
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.icons.LucideIcon
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.theme.Text
import com.gearloop.app.ui.vm.AccountViewModel
import com.gearloop.app.ui.vm.gearViewModel

/* ------------------------------------------------------------------ settings */

/** The account, the app's appearance, the policies, and leaving. */
@Composable
fun SettingsScreen(nav: Nav) {
    val vm: AccountViewModel = gearViewModel()
    val account by vm.current.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    var signingOut by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val a = account

    GearScreen(appBar = { AppBar(title = "Settings", onBack = { nav.back() }) }) {
        VGap(Space.s)
        if (a != null) {
            GearCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Edit profile") { nav.to(Routes.EDIT_PROFILE) }) {
                Row(Modifier.padding(Space.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(a.person, size = 52.dp, showTierRing = true)
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Text(a.person.name, style = GearType.headingS, color = GearText.primary)
                        Text(a.email, style = GearType.bodyS, color = GearText.secondary)
                        Text(listOfNotNull(a.phone, a.person.area.ifBlank { null }).joinToString(" · "), style = GearType.bodyS, color = GearText.tertiary)
                    }
                    GearIcon(Lucide.Pencil, "Edit profile", size = IconSize.m, tint = GearText.primary)
                }
            }
        }

        VGap(Space.l)
        Eyebrow("Account")
        SettingRow(Lucide.UserCheck, "Edit profile") { nav.to(Routes.EDIT_PROFILE) }
        SettingRow(Lucide.Key, "Change password") { nav.to(Routes.CHANGE_PASSWORD) }
        SettingRow(Lucide.BadgeCheck, "Verification") { nav.to("s41") }
        SettingRow(Lucide.CreditCard, "Wallet and card") { nav.to("s35") }
        if (a?.isAdmin == true) {
            SettingRow(Lucide.Scale, "Claims desk") { nav.to("s40") }
        }

        VGap(Space.l)
        Eyebrow("Appearance")
        VGap(Space.s)
        SegmentedControl(listOf("System", "Light", "Dark"), session.themeMode) { vm.setThemeMode(it) }

        VGap(Space.l)
        Eyebrow("About")
        SettingRow(Lucide.ShieldCheck, "Privacy policy") { nav.to(Routes.PRIVACY) }
        SettingRow(Lucide.Image, "Image credits") { nav.to(Routes.CREDITS) }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            GearIcon(Lucide.Info, null, size = IconSize.m, tint = GearText.primary)
            Spacer(Modifier.width(Space.sm))
            Text("Version", style = GearType.labelL, color = GearText.primary, modifier = Modifier.weight(1f))
            Text(BuildConfig.VERSION_NAME, style = GearType.dataM, color = GearText.tertiary)
        }
        GearDivider()

        VGap(Space.l)
        GearButton("Log out", { signingOut = true }, Modifier.fillMaxWidth(), type = ButtonType.SECONDARY, icon = Lucide.ArrowLeft)
        VGap(Space.s)
        GearButton("Delete my account", { deleting = true }, Modifier.fillMaxWidth(), type = ButtonType.GHOST, icon = Lucide.Trash2)
        VGap(Space.section)
    }

    if (signingOut) {
        GearDialog(
            title = "Log out?",
            onDismiss = { signingOut = false },
            confirmLabel = "Log out",
            onConfirm = { signingOut = false; vm.signOut {} },
        ) {
            Text("Everything stays saved on this phone.", style = GearType.bodyM, color = GearText.secondary)
        }
    }
    if (deleting) DeleteAccountDialog(vm) { deleting = false }
}

@Composable
private fun SettingRow(icon: LucideIcon, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GearIcon(icon, null, size = IconSize.m, tint = GearText.primary)
        Spacer(Modifier.width(Space.sm))
        Text(title, style = GearType.labelL, color = GearText.primary, modifier = Modifier.weight(1f))
        GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
    }
    GearDivider()
}

@Composable
private fun DeleteAccountDialog(vm: AccountViewModel, onDismiss: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    GearDialog(
        title = "Delete your account?",
        onDismiss = onDismiss,
        confirmLabel = "Keep my account",
        onConfirm = onDismiss,
        destructiveLabel = "Delete",
        onDestructive = {
            vm.deleteAccount(password) { result ->
                if (result is AccountRepository.Result.Failed) error = result.message else onDismiss()
            }
        },
    ) {
        Text(
            "Your profile, wallet, closet and saved gear are deleted and your listings come out of search. " +
                "Shared bookings stay on the other member's records. This can't be undone.",
            style = GearType.bodyS,
            color = GearText.secondary,
        )
        VGap(Space.s)
        GearTextField(password, label = "Your password", onValueChange = { password = it; error = null }, password = true, error = error)
    }
}

/* ------------------------------------------------------------------ edit profile */

@Composable
fun EditProfileScreen(nav: Nav) {
    val vm: AccountViewModel = gearViewModel()
    val account by vm.current.collectAsStateWithLifecycle()
    val a = account ?: return
    var name by rememberSaveable { mutableStateOf(a.person.name) }
    var area by rememberSaveable { mutableStateOf(a.person.area) }
    var phone by rememberSaveable { mutableStateOf(a.phone.orEmpty()) }
    var errorField by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    GearScreen(
        appBar = { AppBar(title = "Edit profile", onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    "Save",
                    {
                        vm.updateDetails(name, area, phone.ifBlank { null }) { result ->
                            if (result is AccountRepository.Result.Failed) { errorField = result.field?.name; error = result.message } else nav.back()
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && area.isNotBlank(),
                )
            }
        },
    ) {
        VGap(Space.m)
        GearTextField(name, label = "Full name", onValueChange = { name = it; error = null }, error = if (errorField == AccountRepository.Field.NAME.name) error else null)
        VGap(Space.sm)
        GearTextField(a.email, label = "Email", state = FieldState.DISABLED)
        VGap(Space.sm)
        GearTextField(phone, label = "Mobile number", placeholder = "077 123 4567", onValueChange = { phone = it; error = null }, keyboardType = KeyboardType.Phone, error = if (errorField == AccountRepository.Field.PHONE.name) error else null)
        VGap(Space.l)
        Eyebrow("Where you are")
        VGap(Space.s)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            (areas + listOfNotNull(area.takeIf { it.isNotBlank() && it !in areas })).forEach { option ->
                Chip(option, selected = area == option, onClick = { area = option })
            }
        }
        VGap(Space.section)
    }
}

/* ------------------------------------------------------------------ password */

@Composable
fun ChangePasswordScreen(nav: Nav) {
    val vm: AccountViewModel = gearViewModel()
    var current by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var currentError by rememberSaveable { mutableStateOf<String?>(null) }
    var nextError by rememberSaveable { mutableStateOf<String?>(null) }

    GearScreen(
        appBar = { AppBar(title = "Change password", onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                GearButton(
                    "Change password",
                    {
                        vm.changePassword(current, next) { result ->
                            if (result is AccountRepository.Result.Failed) {
                                if (result.field == AccountRepository.Field.PASSWORD) currentError = result.message else nextError = result.message
                            } else {
                                nav.back()
                            }
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = current.isNotEmpty() && next.isNotEmpty(),
                )
            }
        },
    ) {
        VGap(Space.m)
        GearTextField(current, label = "Current password", onValueChange = { current = it; currentError = null }, password = true, error = currentError)
        VGap(Space.sm)
        GearTextField(next, label = "New password", placeholder = "At least 8 characters, with a number", onValueChange = { next = it; nextError = null }, password = true, error = nextError)
    }
}

/* ------------------------------------------------------------------ privacy */

/**
 * The privacy policy. Every point here is checkable against the build: the
 * manifest asks for no internet permission, the camera and location are used
 * only where the screens say, and Auto Backup is switched off.
 */
@Composable
fun PrivacyScreen(nav: Nav) {
    GearScreen(appBar = { AppBar(title = "Privacy policy", onBack = { nav.back() }) }) {
        VGap(Space.s)
        Text("What you do in GearLoop stays on this phone.", style = GearType.headingM, color = GearText.primary)
        VGap(Space.m)
        PrivacyPoint(Lucide.ShieldCheck, "Nothing leaves your phone", "Your account, bookings, messages and photos are saved on this device. The app has no internet permission and is left out of cloud backup.")
        PrivacyPoint(Lucide.Lock, "Your password is never stored", "Only a salted hash is kept. For cards, only the brand and last four digits.")
        PrivacyPoint(Lucide.Camera, "The camera, when you use it", "Used only while a photo or tag-scan screen is open. Photos stay in the app's private storage.")
        PrivacyPoint(Lucide.MapPin, "Location, if you allow it", "Condition photos record where they were taken. Say no and they aren't geotagged.")
        PrivacyPoint(Lucide.Users, "What other members see", "Your name, area, rating and listings. Your exact address is shared only once a booking is paid; your email and number never are.")
        PrivacyPoint(Lucide.Trash2, "Deleting your account", "Settings → Delete my account removes your data from this phone. Uninstalling removes everything.")
        PrivacyPoint(Lucide.Banknote, "Payments", "No real money is taken by this version of the app.")
        VGap(Space.l)
        Text("VERSION ${BuildConfig.VERSION_NAME}", style = GearType.dataS, color = GearText.tertiary)
        VGap(Space.section)
    }
}

@Composable
private fun PrivacyPoint(icon: LucideIcon, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Space.s), verticalAlignment = Alignment.Top) {
        GearIcon(icon, null, size = IconSize.m, tint = GearText.primary)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = GearType.labelL, color = GearText.primary)
            VGap(2.dp)
            Text(body, style = GearType.bodyS, color = GearText.secondary)
        }
    }
}

/* ------------------------------------------------------------------ credits */

/**
 * Attribution for the photography. Every image is Creative Commons or public
 * domain, under licences that permit the cropping and colour grading applied.
 */
@Composable
fun CreditsScreen(nav: Nav) {
    GearScreen(appBar = { AppBar(title = "Image credits", onBack = { nav.back() }) }) {
        VGap(Space.s)
        Text(
            "All photography is Creative Commons or public domain, from Openverse and Wikimedia Commons.",
            style = GearType.bodyM,
            color = GearText.secondary,
        )
        VGap(Space.s)
        Text("Icons are Lucide (ISC). Type is Archivo, Instrument Sans and JetBrains Mono, all SIL Open Font Licence.", style = GearType.bodyS, color = GearText.tertiary)
        VGap(Space.l)
        com.gearloop.app.data.photoCredits.forEach { credit ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(credit.title, style = GearType.bodyM, color = GearText.primary, maxLines = 2)
                    VGap(2.dp)
                    Text("${credit.creator} · ${credit.source}", style = GearType.bodyS, color = GearText.tertiary)
                }
                Spacer(Modifier.width(Space.s))
                Text(
                    credit.licence.ifEmpty { "CC" },
                    style = GearType.dataS,
                    color = GearText.secondary,
                    modifier = Modifier.clip(GearShape.xs).background(GearColor.Chalk100).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            GearDivider()
        }
        VGap(Space.section)
    }
}
