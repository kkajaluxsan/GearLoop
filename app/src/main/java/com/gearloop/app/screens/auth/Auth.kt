package com.gearloop.app.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.data.repo.AccountRepository
import com.gearloop.app.nav.Nav
import com.gearloop.app.nav.Routes
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
import com.gearloop.app.ui.theme.Text
import com.gearloop.app.ui.vm.AccountViewModel
import com.gearloop.app.ui.vm.gearViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------ splash */

/**
 * The brand, for a moment, on every cold start: the gear tag, the name and the
 * promise. It follows the system splash, which shows the same tag on the same
 * pine, so the hand-over is seamless.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    SystemBarIcons(darkSurface = true)
    LaunchedEffect(Unit) {
        delay(1_100)
        onDone()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(GearSurface.darkest)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Space.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GearTag("OWN LESS · RIDE MORE", size = TagSize.FULL)
        VGap(Space.l)
        Text("GearLoop", style = GearType.displayXl, color = GearText.onDark)
        VGap(Space.s)
        Text(
            "Rent, lend and swap sports gear near you",
            style = GearType.bodyL,
            color = GearColor.Chalk300,
            textAlign = TextAlign.Center,
        )
        VGap(Space.section)
        Spinner(GearColor.HiVis500, size = 24.dp)
    }
}

/* ------------------------------------------------------------------ welcome */

private data class WelcomePage(val photo: String, val eyebrow: String, val title: String, val body: String)

private val welcomePages = listOf(
    WelcomePage(
        "sport_surfing", "RENT",
        "The gear, not the garage",
        "Boards, bikes, tents and kayaks from people near you, by the day.",
    ),
    WelcomePage(
        "cond_1", "CHECK IT TOGETHER",
        "Photos settle it, not arguments",
        "You and the owner photograph the gear at pickup, and again on return.",
    ),
    WelcomePage(
        "mk_hammock", "LEND OR SWAP",
        "Your idle kit, earning",
        "Rent out what sits in a cupboard, or swap it for something you'll use.",
    ),
)

/** Three pages on what the app is for, the first time it is opened on a phone. */
@Composable
fun WelcomeScreen(nav: Nav) {
    val vm: AccountViewModel = gearViewModel()
    val pager = rememberPagerState(pageCount = { welcomePages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == welcomePages.lastIndex
    SystemBarIcons(darkSurface = true)

    Box(Modifier.fillMaxSize().background(GearSurface.darkest)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val p = welcomePages[page]
            Box(Modifier.fillMaxSize()) {
                GearImage(p.photo, null, Modifier.fillMaxSize())
                PhotoGradient(Modifier.fillMaxSize(), strength = 0.95f)
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = Space.screen)
                        .padding(bottom = 210.dp),
                ) {
                    Text(p.eyebrow, style = GearType.labelXs, color = GearColor.HiVis500)
                    VGap(Space.s)
                    Text(p.title, style = GearType.displayL, color = GearText.onDark)
                    VGap(Space.s)
                    Text(p.body, style = GearType.bodyL, color = GearColor.Chalk300)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Space.sm, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GearTag("GEARLOOP", size = TagSize.COMPACT)
            Spacer(Modifier.weight(1f))
            GearLink("Skip", { vm.markWelcomeSeen(); nav.reset(Routes.LOGIN) }, color = GearColor.Chalk300)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screen, vertical = Space.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(welcomePages.size) { i ->
                    Box(
                        Modifier
                            .size(if (i == pager.currentPage) 22.dp else 8.dp, 8.dp)
                            .clip(GearShape.full)
                            .background(if (i == pager.currentPage) GearColor.HiVis500 else GearSurface.overImage.copy(alpha = 0.4f)),
                    )
                }
            }
            VGap(Space.l)
            if (last) {
                GearButton("Create an account", { vm.markWelcomeSeen(); nav.to(Routes.SIGN_UP) }, Modifier.fillMaxWidth())
            } else {
                GearButton("Next", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth(), trailingIcon = Lucide.ArrowRight)
            }
            VGap(Space.s)
            GearButton(
                "I already have an account",
                { vm.markWelcomeSeen(); nav.to(Routes.LOGIN) },
                Modifier.fillMaxWidth(),
                type = ButtonType.GHOST,
                contentColor = GearText.onDark,
            )
        }
    }
}

/* ------------------------------------------------------------------ log in */

/**
 * Log in with an email address and password. Accounts that have been used on
 * this phone before are offered at the top, so switching between them is a
 * tap and a password.
 */
@Composable
fun LoginScreen(nav: Nav) {
    val vm: AccountViewModel = gearViewModel()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var resetting by rememberSaveable { mutableStateOf(false) }
    val recent = accounts.filter { it.lastLoginAt != null }.take(3)

    val submit = {
        emailError = null
        passwordError = null
        busy = true
        vm.signIn(email, password) { result ->
            busy = false
            if (result is AccountRepository.Result.Failed) {
                if (result.field == AccountRepository.Field.PASSWORD) passwordError = result.message else emailError = result.message
            }
            // On success the app rebuilds itself for the member who signed in.
        }
    }

    GearScreen(
        footer = {
            StickyFooter {
                Column(Modifier.fillMaxWidth()) {
                    GearButton("Log in", { submit() }, Modifier.fillMaxWidth(), enabled = !busy && email.isNotBlank() && password.isNotEmpty(), loading = busy)
                    VGap(Space.s)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("New to GearLoop?", style = GearType.bodyS, color = GearText.secondary)
                        Spacer(Modifier.width(Space.xs))
                        GearLink("Create an account", { nav.to(Routes.SIGN_UP) })
                    }
                }
            }
        },
    ) {
        VGap(Space.section)
        GearTag("GEARLOOP", size = TagSize.COMPACT)
        VGap(Space.l)
        Text("Welcome back", style = GearType.displayL, color = GearText.primary)

        if (recent.isNotEmpty()) {
            VGap(Space.l)
            Eyebrow("On this phone")
            VGap(Space.s)
            recent.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(GearShape.md)
                        .background(if (email.equals(a.email, ignoreCase = true)) GearColor.HiVis100 else GearSurface.card)
                        .clickable(role = Role.Button, onClickLabel = "Log in as ${a.person.name}") { email = a.email; password = ""; emailError = null; passwordError = null }
                        .padding(Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(a.person, size = 40.dp)
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Text(a.person.name, style = GearType.labelL, color = GearText.primary)
                        Text(a.email, style = GearType.bodyS, color = GearText.tertiary)
                    }
                    GearIcon(Lucide.ChevronRight, null, size = IconSize.s, tint = GearColor.Chalk400)
                }
                VGap(Space.s)
            }
        }

        VGap(Space.l)
        GearTextField(
            email, label = "Email", placeholder = "you@example.com",
            onValueChange = { email = it; emailError = null },
            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, error = emailError, leadingIcon = Lucide.Mail,
        )
        VGap(Space.sm)
        GearTextField(
            password, label = "Password", placeholder = "Your password",
            onValueChange = { password = it; passwordError = null },
            password = true, imeAction = ImeAction.Done, onImeAction = { if (email.isNotBlank() && password.isNotEmpty()) submit() },
            error = passwordError, leadingIcon = Lucide.Lock,
        )
        VGap(Space.s)
        GearLink("Forgot your password?", { resetting = true }, color = GearText.tertiary)
        VGap(Space.section)
    }

    if (resetting) ResetPasswordDialog(vm, email) { resetting = false }
}

@Composable
private fun ResetPasswordDialog(vm: AccountViewModel, initialEmail: String, onDismiss: () -> Unit) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var phone by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    GearDialog(
        title = "Reset your password",
        onDismiss = onDismiss,
        confirmLabel = "Reset and log in",
        onConfirm = {
            vm.resetPassword(email, phone, next) { result ->
                if (result is AccountRepository.Result.Failed) error = result.message else onDismiss()
            }
        },
        confirmEnabled = email.isNotBlank() && phone.isNotBlank() && next.length >= 8,
    ) {
        Text("Enter the mobile number on your account and a new password.", style = GearType.bodyS, color = GearText.secondary)
        VGap(Space.s)
        GearTextField(email, label = "Email", onValueChange = { email = it; error = null }, keyboardType = KeyboardType.Email)
        VGap(Space.s)
        GearTextField(phone, label = "Mobile number", placeholder = "077 123 4567", onValueChange = { phone = it; error = null }, keyboardType = KeyboardType.Phone)
        VGap(Space.s)
        GearTextField(next, label = "New password", placeholder = "At least 8 characters", onValueChange = { next = it; error = null }, password = true, error = error)
    }
}

/* ------------------------------------------------------------------ sign up */

/** Where members collect gear from: the neighbourhoods the marketplace covers. */
val areas = listOf(
    "Jaffna", "Nallur", "Chundikuli", "Gurunagar", "Jaffna Fort", "Kokuvil", "Thirunelvely", "Kondavil",
    "Ariyalai", "Kopay", "Urumpirai", "Kaithady", "Chunnakam", "Mandaitivu",
)

/**
 * Create an account: name, email, an optional mobile number, where you are, and
 * a password. The getting-started screens follow straight after.
 */
@Composable
fun SignUpScreen(nav: Nav) {
    val vm: AccountViewModel = gearViewModel()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var errorField by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun errorFor(f: AccountRepository.Field) = if (errorField == f.name) error else null
    fun clear() { errorField = null; error = null }
    // What still stands between the member and the button, said beside it — the
    // area chips and the agreement sit below the fold, so a greyed-out button
    // alone didn't say what was missing.
    val missing = buildList {
        if (name.isBlank()) add("your name")
        if (email.isBlank()) add("your email")
        if (password.isEmpty()) add("a password")
        if (area.isEmpty()) add("where you are")
        if (!agreed) add("agree to the terms")
    }

    GearScreen(
        appBar = { AppBar(title = "Create an account", onBack = { nav.back() }) },
        footer = {
            StickyFooter {
                Column(Modifier.fillMaxWidth()) {
                    if (missing.isNotEmpty()) {
                        Text("To continue: " + missing.joinToString(" · "), style = GearType.bodyS, color = GearText.secondary)
                        VGap(Space.s)
                    }
                    GearButton(
                        "Create account",
                        {
                            clear()
                            busy = true
                            vm.signUp(name, email, password, area, phone.ifBlank { null }) { result ->
                                busy = false
                                if (result is AccountRepository.Result.Failed) {
                                    errorField = result.field?.name
                                    error = result.message
                                }
                            }
                        },
                        Modifier.fillMaxWidth(),
                        enabled = !busy && missing.isEmpty(),
                        loading = busy,
                    )
                }
            }
        },
    ) {
        VGap(Space.m)
        GearTextField(name, label = "Full name", placeholder = "e.g. Anjali Rajendran", onValueChange = { name = it; clear() }, imeAction = ImeAction.Next, error = errorFor(AccountRepository.Field.NAME))
        VGap(Space.sm)
        GearTextField(email, label = "Email", placeholder = "you@example.com", onValueChange = { email = it; clear() }, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, error = errorFor(AccountRepository.Field.EMAIL))
        VGap(Space.sm)
        GearTextField(phone, label = "Mobile number (optional)", placeholder = "077 123 4567", onValueChange = { phone = it; clear() }, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next, error = errorFor(AccountRepository.Field.PHONE))
        VGap(Space.xs)
        Text("For resetting your password. Never shown to other members.", style = GearType.bodyS, color = GearText.tertiary)
        VGap(Space.sm)
        GearTextField(password, label = "Password", placeholder = "At least 8 characters, with a number", onValueChange = { password = it; clear() }, password = true, error = errorFor(AccountRepository.Field.PASSWORD))

        VGap(Space.l)
        Eyebrow("Where are you?")
        VGap(Space.s)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            areas.forEach { a -> Chip(a, selected = area == a, onClick = { area = a; clear() }) }
        }
        errorFor(AccountRepository.Field.AREA)?.let {
            VGap(Space.xs)
            Text(it, style = GearType.bodyS, color = GearColor.Danger500)
        }

        VGap(Space.l)
        CheckRow("I agree to the GearLoop terms and the privacy policy", agreed, { agreed = it })
        GearLink("Read the privacy policy", { nav.to(Routes.PRIVACY) }, icon = Lucide.ShieldCheck)
        if (error != null && errorField == null) {
            VGap(Space.s)
            Banner(error!!, kind = BannerKind.DANGER)
        }
        VGap(Space.section)
    }
}

