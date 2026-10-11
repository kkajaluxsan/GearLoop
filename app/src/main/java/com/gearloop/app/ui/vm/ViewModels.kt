package com.gearloop.app.ui.vm

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gearloop.app.AppContainer
import com.gearloop.app.GearLoopApplication
import com.gearloop.app.data.CaptureGuide
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Grade
import com.gearloop.app.data.Phase
import com.gearloop.app.data.PhotoStore
import com.gearloop.app.data.Protection
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.lkr
import com.gearloop.app.data.repo.AccountRepository
import com.gearloop.app.data.repo.Badge
import com.gearloop.app.data.repo.CalendarUnit
import com.gearloop.app.data.repo.EvidenceRepository
import com.gearloop.app.data.repo.MoneyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.time.LocalDate

/*
 * The UI layer's state holders: one ViewModel per area, matching the
 * repositories underneath them, plus one for the account.
 *
 * A screen gets its ViewModel from [gearViewModel], which scopes it to the
 * screen's navigation entry. The ViewModel exposes the repository's StateFlows
 * — already hot, already in memory — and turns each user action into a write on
 * `viewModelScope`, followed by a toast that says what happened. Navigation
 * that depends on a write happening first is passed in as `then`, and runs once
 * the read side has caught up, so the next screen never opens on stale data.
 */

abstract class GearViewModel(protected val app: AppContainer) : ViewModel() {

    protected fun act(block: suspend () -> Unit): Job = viewModelScope.launch { block() }

    protected fun toast(text: String, onUndo: (suspend () -> Unit)? = null) = app.toaster.show(text, onUndo = onUndo)

    /** Wait for a flow-backed read to reflect a write that just completed. */
    protected suspend fun <T> await(flow: kotlinx.coroutines.flow.Flow<T>, until: (T) -> Boolean) {
        withTimeoutOrNull(1_500) { flow.first(until) }
    }

    /** The signed-in member's handle, or "" when nobody is. */
    val me: String get() = app.session.handle

    /** A picture chosen from the gallery, copied into the app's own storage. */
    protected suspend fun import(uri: Uri, prefix: String): String? = PhotoStore.importUri(app.appContext, uri, prefix)
}

object GearViewModelFactory : ViewModelProvider.Factory by viewModelFactory({
    fun CreationExtrasApp(extras: androidx.lifecycle.viewmodel.CreationExtras): AppContainer =
        (extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GearLoopApplication).container
    initializer { AccountViewModel(CreationExtrasApp(this)) }
    initializer { FitDiscoveryViewModel(CreationExtrasApp(this)) }
    initializer { MoneyViewModel(CreationExtrasApp(this)) }
    initializer { EvidenceTrustViewModel(CreationExtrasApp(this)) }
    initializer { ExchangeSupplyViewModel(CreationExtrasApp(this)) }
})

@Composable
inline fun <reified VM : ViewModel> gearViewModel(): VM = viewModel(factory = GearViewModelFactory)

// ============================================================ the account

class AccountViewModel(app: AppContainer) : GearViewModel(app) {
    private val repo = app.accounts

    val accounts = repo.accounts
    val current = repo.current
    val session = app.session.session
    val profile = app.store.profile

    /** Each runs [then] on success, or hands the failure back for the form to show. */
    fun signUp(name: String, email: String, password: String, area: String, phone: String?, onResult: (AccountRepository.Result) -> Unit) = act {
        val result = repo.signUp(name, email, password, area, phone)
        if (result == AccountRepository.Result.Ok) {
            await(app.store.profile) { it != null }
            toast("Welcome to GearLoop, ${name.trim().substringBefore(' ')}")
        }
        onResult(result)
    }

    fun signIn(email: String, password: String, onResult: (AccountRepository.Result) -> Unit) = act {
        val result = repo.signIn(email, password)
        if (result == AccountRepository.Result.Ok) {
            await(app.store.profile) { it != null }
            toast("Signed in as ${app.store.person(me).name}")
        }
        onResult(result)
    }

    fun resetPassword(email: String, phone: String, next: String, onResult: (AccountRepository.Result) -> Unit) = act {
        val result = repo.resetPassword(email, phone, next)
        if (result == AccountRepository.Result.Ok) {
            await(app.store.profile) { it != null }
            toast("Password reset. You're signed in.")
        }
        onResult(result)
    }

    fun signOut(then: () -> Unit) = act {
        repo.signOut()
        then()
    }

    fun updateDetails(name: String, area: String, phone: String?, onResult: (AccountRepository.Result) -> Unit) = act {
        val result = repo.updateDetails(name, area, phone)
        if (result == AccountRepository.Result.Ok) toast("Profile saved")
        onResult(result)
    }

    fun changePassword(current: String, next: String, onResult: (AccountRepository.Result) -> Unit) = act {
        val result = repo.changePassword(current, next)
        if (result == AccountRepository.Result.Ok) toast("Password changed")
        onResult(result)
    }

    fun deleteAccount(password: String, onResult: (AccountRepository.Result) -> Unit) = act {
        val result = repo.deleteAccount(password)
        if (result == AccountRepository.Result.Ok) toast("Your account has been deleted")
        onResult(result)
    }

    fun markWelcomeSeen() = app.session.markWelcomeSeen()

    fun setThemeMode(mode: Int) = app.session.setThemeMode(mode)
}

// ============================================================ Fit & Discovery

class FitDiscoveryViewModel(app: AppContainer) : GearViewModel(app) {
    private val repo = app.discovery

    val profile = repo.profile
    val sports = repo.sports
    val rails = repo.rails
    val railSport = repo.railSportName
    val searchable = repo.searchable
    val saved = repo.saved
    val selected = repo.selected
    val unread = repo.unreadNotifications
    val reviews = repo.reviews
    val trip = repo.trip
    val alerts = repo.alerts
    val draft = app.money.draft
    val myBookings = app.money.past
    val matches = app.exchange.matches
    val people = app.store.people

    fun select(item: GearItem) = repo.select(item.id)
    fun openThread(handle: String) = app.money.openThread(handle)
    fun showProfile(handle: String) = app.session.selectProfile(handle)
    fun similarTo(item: GearItem) = repo.similarTo(item)
    fun listingsBy(handle: String) = repo.listingsBy(handle)
    fun distributionOf(handle: String) = repo.distributionOf(handle)
    fun defaultSpan(item: GearItem) = app.money.defaultSpan(item)

    fun setIntent(intent: Int, then: () -> Unit) = act {
        repo.setIntent(intent)
        then()
    }

    fun saveMeasurements(heightCm: Int, weightKg: Int, metric: Boolean, then: () -> Unit) = act {
        repo.saveMeasurements(heightCm, weightKg, metric)
        toast("Saved: $heightCm cm, $weightKg kg. Every fit verdict has been updated.")
        then()
    }

    fun clearMeasurements() = act {
        val before = profile.value
        repo.clearMeasurements()
        toast("Measurements removed. Listings now ask for your height.") {
            if (before?.heightCm != null && before.weightKg != null) repo.saveMeasurements(before.heightCm, before.weightKg, before.metric)
        }
    }

    fun toggleSport(sport: String) = act { repo.toggleSport(sport) }

    /** The getting-started screens are done or skipped; home is next. */
    fun finishOnboarding(then: () -> Unit) = act {
        repo.finishOnboarding()
        await(app.store.profile) { it?.onboarded == true }
        then()
    }

    fun toggleSaved(item: GearItem) = act {
        repo.toggleSaved(item.id)
        toast(if (item.saved) "Removed ${item.shortTitle} from saved" else "Saved ${item.shortTitle}") { repo.toggleSaved(item.id) }
    }

    /** "Book" — create or reopen the draft, then go to the dates. */
    fun startBooking(item: GearItem, then: () -> Unit) = act {
        val ref = app.money.startBooking(item.id)
        await(app.money.draft) { it?.ref == ref }
        then()
    }

    fun addReview(listingId: String, stars: Int, body: String, bookingRef: String?) = act {
        val id = repo.addReview(listingId, stars, body, bookingRef)
        if (id > 0) toast("Review posted · $stars★") { repo.deleteReview(id) }
    }

    fun deleteReview(id: Long) = act {
        repo.deleteReview(id)
        toast("Review deleted")
    }

    // ---- trip planner
    fun setTripAdults(adults: Int) = act { repo.setTripAdults(adults) }
    fun toggleTripSport(sport: String) = act { repo.toggleTripSport(sport) }
    fun setTripDestination(destination: String) = act { repo.setTripDestination(destination) }
    fun setTripDates(start: LocalDate, days: Int) = act { repo.setTripDates(start, days) }
    fun setTripExtraAdded(id: Long, label: String, added: Boolean) = act {
        repo.setTripExtraAdded(id, added)
        toast(if (added) "Added $label to the kit" else "Took $label out of the kit")
    }
    fun removeTripItem(id: Long, label: String) = act {
        val row = repo.removeTripItem(id) ?: return@act
        toast("Removed $label from the kit") { repo.restoreTripItem(row) }
    }

    /**
     * "Build my kit list" — Create a rental for each chosen sport the kit does
     * not cover yet, using the nearest listing for that sport.
     */
    fun buildKit(trip: com.gearloop.app.data.repo.Trip) = act {
        val covered = trip.rent.mapNotNull { it.listing?.sport }.toSet()
        val wanted = trip.sports.filter { it !in covered }
        val added = wanted.mapNotNull { sport ->
            searchable.value.filter { it.sport == sport && it.available }.minByOrNull { it.distanceKm }?.also {
                repo.addTripRental(it.id, "For you")
            }
        }
        toast(
            when {
                trip.sports.isEmpty() -> "Pick at least one sport first"
                added.isEmpty() -> "Your kit already covers ${trip.sports.joinToString(" and ")}"
                else -> "Added ${added.joinToString { it.shortTitle }}"
            },
        )
    }

    // ---- alerts
    fun createAlert(kind: String, summary: String) = act {
        val id = repo.createAlert(kind, summary)
        toast(if (kind == "ALERT") "Alert set. We'll tell you when one is listed." else "Posted. Owners nearby can see what you're after.") {
            repo.deleteAlert(id)
        }
    }
    fun deleteAlert(id: Long, kind: String, summary: String) = act {
        repo.deleteAlert(id)
        toast(if (kind == "ALERT") "Alert removed" else "Post removed") { repo.createAlert(kind, summary) }
    }

    // ---- filter persistence
    suspend fun loadSearchPrefs() = repo.loadSearchPrefs()
    fun saveSearchPrefs(filters: com.gearloop.app.data.SearchFilters) = act { repo.saveSearchPrefs(filters) }
    fun clearSearchPrefs() = act { repo.clearSearchPrefs() }
}

// ============================================================ Money & Commitment

class MoneyViewModel(app: AppContainer) : GearViewModel(app) {
    private val repo = app.money

    val draft = repo.draft
    val current = repo.current
    val active = repo.active
    val upcoming = repo.upcoming
    val past = repo.past
    val walletAvailable = repo.walletAvailable
    val holds = repo.holds
    val coveredNotHeld = repo.coveredNotHeld
    val transactions = repo.transactions
    val notifications = repo.notifications
    val thread = repo.thread
    val conversations = repo.conversations
    val threadId = repo.threadId
    val threadBooking = repo.threadBooking
    val currentClaim = repo.currentClaim
    val queue = repo.queue
    val people = app.store.people
    val profile = app.store.profile
    val account = app.accounts.current

    val cancellationRule: String get() = repo.cancellationRule

    fun monthsFor(booking: com.gearloop.app.data.repo.Booking) = repo.monthsFor(booking)
    fun refundFor(booking: com.gearloop.app.data.repo.Booking) = repo.refundFor(booking)
    fun renterShare(claim: com.gearloop.app.data.repo.Claim, award: Int) = repo.renterShare(claim, award)
    fun bookingFor(claim: com.gearloop.app.data.repo.Claim) = repo.bookingFor(claim)
    fun evidenceFor(claim: com.gearloop.app.data.repo.Claim) = repo.evidenceFor(claim)

    /** Every return frame of the claimed booking, or the listing's own photos for a claim with none. */
    fun returnFrames(claim: com.gearloop.app.data.repo.Claim): List<Pair<String, String>> {
        val rows = app.store.captureRows.value.filter { it.bookingRef == claim.bookingRef && it.phase == Phase.RETURN }
        return if (rows.isNotEmpty()) rows.map { it.angle to it.photoKey }
        else com.gearloop.app.data.Demo.returnComparison.map { it.first to it.second }
    }
    fun selectBooking(ref: String) = repo.selectBooking(ref)
    fun selectClaim(ref: String) = repo.selectClaim(ref)
    fun openThread(handle: String) = repo.openThread(handle)
    fun showProfile(handle: String) = app.session.selectProfile(handle)
    fun selectListing(id: String) = app.session.selectListing(id)

    fun ensureDraft(then: () -> Unit = {}) = act {
        val ref = repo.startBooking(app.session.listingId)
        await(repo.draft) { it?.ref == ref }
        then()
    }

    fun tapDay(day: LocalDate) = act { repo.tapDay(day) }
    fun setProtection(p: Protection) = act { repo.setProtection(p) }
    fun signWaiver(name: String, then: () -> Unit) = act {
        repo.signWaiver(name)
        then()
    }
    fun clearWaiver() = act { repo.clearWaiver() }
    fun setPaymentMethod(i: Int) = act { repo.setPaymentMethod(i) }
    fun setExtras(include: Boolean) = act { repo.setExtras(include) }

    fun discardDraft(then: () -> Unit) = act {
        repo.discardDraft()
        toast("Booking discarded. Nothing was charged.")
        then()
    }

    /** "Confirm and pay" — create the booking, then open its confirmation. */
    fun confirm(then: () -> Unit) = act {
        when (val result = repo.confirmDraft()) {
            is MoneyRepository.ConfirmResult.Booked -> {
                await(repo.current) { it?.ref == result.ref }
                then()
            }
            MoneyRepository.ConfirmResult.DatesTaken -> toast("Those dates were just taken. Pick new ones.")
            MoneyRepository.ConfirmResult.NoCard -> toast("Add a card first, or pay from your wallet or in cash")
            is MoneyRepository.ConfirmResult.LowBalance -> toast("Your wallet has ${lkr(result.available)}. Top up or choose another way to pay.")
            MoneyRepository.ConfirmResult.Unavailable -> toast("This listing can't be booked right now")
        }
    }

    fun cancel(ref: String) = act {
        val refund = repo.cancel(ref)
        toast("Booking $ref cancelled · ${lkr(refund)} refunded")
    }

    fun hideFromHistory(ref: String) = act {
        repo.setHidden(ref, true)
        toast("Removed $ref from your history") { repo.setHidden(ref, false) }
    }

    fun markAddedToCalendar(ref: String) = act { repo.markAddedToCalendar(ref) }

    fun withdraw() = act {
        val amount = repo.withdraw()
        toast(if (amount > 0) "${lkr(amount)} is on its way to your bank" else "Nothing available to withdraw")
    }

    fun topUp(amount: Int) = act {
        if (repo.topUp(amount)) toast("Topped up ${lkr(amount)}") else toast("Add a card to top up from")
    }

    /** Save a card; [onResult] gets null on success or the reason it was refused. */
    fun saveCard(number: String, expiry: String, onResult: (String?) -> Unit) = act {
        val error = repo.saveCard(number, expiry)
        if (error == null) toast("Card saved. Only the last four digits are kept.")
        onResult(error)
    }

    fun removeCard() = act {
        repo.removeCard()
        toast("Card removed")
    }

    fun markRead(id: Long) = act { repo.markRead(id) }
    fun markAllRead() = act {
        repo.markAllRead()
        toast("All caught up")
    }
    fun deleteNotification(id: Long) = act {
        val row = repo.deleteNotification(id) ?: return@act
        toast("Notification removed") { repo.restoreNotification(row) }
    }

    fun send(text: String) = act { repo.send(text) }
    fun deleteMessage(id: Long) = act {
        val row = repo.deleteMessage(id) ?: return@act
        toast("Message deleted") { repo.restoreMessage(row) }
    }

    fun contest(ref: String, statement: String) = act {
        repo.contest(ref, statement)
        toast("Contested. The claims desk will review both sides.")
    }

    /** Evidence: a note, and any photos — taken with the camera, or chosen. */
    fun addEvidence(ref: String, note: String, photos: List<String>) = act {
        repo.addEvidence(ref, note, photos)
        toast("Evidence added to $ref")
    }

    suspend fun importPhoto(uri: Uri): String? = import(uri, "claim")

    fun acceptLiability(ref: String) = act {
        repo.acceptLiability(ref)
        toast("Claim $ref settled")
    }
    fun issueRuling(ref: String, award: Int, rationale: String) = act {
        val ok = repo.issueRuling(ref, award, rationale)
        toast(if (ok) "Ruling issued on $ref · ${lkr(award)}" else "A ruling needs a stated reason")
    }
    fun requestMoreEvidence(ref: String) = act {
        repo.requestMoreEvidence(ref)
        toast("Asked the renter for more evidence")
    }
    fun escalate(ref: String) = act {
        repo.escalate(ref)
        toast("$ref escalated to a senior adjudicator")
    }
}

// ============================================================ Evidence & Trust

class EvidenceTrustViewModel(app: AppContainer) : GearViewModel(app) {
    private val repo = app.evidence

    val booking = repo.booking
    val phase = repo.phase
    val angles = repo.angles
    val frames = repo.frames
    val marks = repo.marks
    val pairs = repo.pairs
    val trust = repo.trust
    val badges = repo.badges
    val verification = repo.verification
    val verificationDetails = repo.verificationDetails
    val address = repo.address
    val people = app.store.people
    val reviews = app.store.reviews
    val claims = app.money.claims
    val profileHandle = app.session.profileHandle
    val listings = app.store.allItems
    val profile = app.store.profile
    val account = app.accounts.current

    fun select(ref: String) = repo.select(ref)
    fun selectClaim(ref: String) = app.money.selectClaim(ref)
    fun showProfile(handle: String) = app.session.selectProfile(handle)
    fun trustStats(handle: String) = app.store.trustStats(handle)
    fun listingsBy(handle: String) = app.discovery.listingsBy(handle)
    fun selectListing(id: String) = app.session.selectListing(id)
    fun openThread(handle: String) = app.money.openThread(handle)

    fun toggleChecklist(ref: String, index: Int) = act { repo.toggleChecklist(ref, index) }

    /** What the camera read off a gear tag, or what was typed from it. */
    fun verifyTag(ref: String, scanned: String, onResult: (EvidenceRepository.TagResult) -> Unit) = act {
        val result = repo.verifyTag(ref, scanned)
        when (result) {
            EvidenceRepository.TagResult.Matched -> toast("Tag matches the booking. Now check the gear together.")
            // A wrong tag is explained on the scan screen, beside the code.
            is EvidenceRepository.TagResult.OtherItem, EvidenceRepository.TagResult.NotATag -> Unit
        }
        onResult(result)
    }

    fun enterCode(ref: String, code: String, onResult: (Boolean) -> Unit) = act {
        val ok = repo.enterCode(ref, code)
        if (ok) toast("Code accepted. Now check the gear together.")
        onResult(ok)
    }

    fun startCapture(phase: Phase) = repo.startCapture(phase)

    /**
     * A frame the camera just wrote to [file]: made upright and resized, checked
     * for blur and light, and recorded against the angle with where it was taken.
     */
    fun captured(ref: String, phase: Phase, angle: String, file: File, latitude: Double?, longitude: Double?, then: (Boolean) -> Unit = {}) = act {
        val key = PhotoStore.adoptCapture(file)
        if (key == null) {
            toast("That photo couldn't be saved. Try again.")
            then(false)
            return@act
        }
        val check = PhotoStore.check(app.appContext, key)
        val old = app.store.captureRows.value.firstOrNull { it.bookingRef == ref && it.phase == phase && it.angle == angle }?.photoKey
        repo.capture(ref, phase, angle, key, check.ok, latitude, longitude)
        if (old != null && old != key) PhotoStore.delete(app.appContext, old)
        if (!check.ok) toast("$angle: ${check.label.lowercase()}")
        then(check.ok)
    }

    fun retake(ref: String, phase: Phase, angle: String) = act {
        repo.deleteFrame(ref, phase, angle)?.let { PhotoStore.delete(app.appContext, it) }
        toast("$angle cleared. Shoot it again.")
    }

    fun saveMark(id: Long?, ref: String, phase: Phase, area: String, severity: DamageMark.Severity, note: String, x: Float, y: Float, photoKey: String?, then: () -> Unit) = act {
        if (id == null) repo.addMark(ref, phase, area, severity, note, x, y, photoKey)
        else repo.updateMark(id, area, severity, note, x, y, photoKey)
        toast(if (id == null) "Mark saved on the ${area.lowercase()}. Both of you can see it." else "Mark updated")
        then()
    }

    fun deleteMark(id: Long, then: () -> Unit = {}) = act {
        val row = repo.deleteMark(id) ?: return@act
        toast("Mark deleted") { repo.restoreMark(row) }
        then()
    }

    /** A close-up of a mark, taken with the camera. */
    suspend fun adoptPhoto(file: File): String? = PhotoStore.adoptCapture(file)

    suspend fun importPhoto(uri: Uri): String? = import(uri, "mark")

    /** One person's signature, drawn on the screen and saved as an image. */
    fun sign(ref: String, signer: EvidenceRepository.Signer, signature: android.graphics.Bitmap, name: String) = act {
        val key = PhotoStore.saveBitmap(app.appContext, signature, "sign")
        repo.sign(ref, signer, key)
        toast("Signed by $name")
    }

    fun toggleReturnChecklist(ref: String, index: Int) = act { repo.toggleReturnChecklist(ref, index) }

    /** Tell the other person something is wrong, then open the conversation with them. */
    fun reportIssue(ref: String, problem: String, note: String, then: () -> Unit) = act {
        val other = repo.reportIssue(ref, problem, note) ?: return@act
        app.money.openThread(other)
        toast("Sent. They've been told.")
        then()
    }

    /** The price of one more day, for the member to agree to; or why there isn't one. */
    fun extendQuote(ref: String, onQuote: (EvidenceRepository.ExtendResult.Quote) -> Unit) = act {
        when (val result = repo.extendQuote(ref)) {
            is EvidenceRepository.ExtendResult.Quote -> onQuote(result)
            else -> extendRefused(result)
        }
    }

    fun extend(ref: String) = act {
        when (val result = repo.extend(ref)) {
            is EvidenceRepository.ExtendResult.Done ->
                toast(if (result.charged > 0) "Extended to ${dayMonth(result.newEnd)} · ${lkr(result.charged)} charged" else "Extended to ${dayMonth(result.newEnd)}")
            else -> extendRefused(result)
        }
    }

    private fun extendRefused(result: EvidenceRepository.ExtendResult) {
        when (result) {
            EvidenceRepository.ExtendResult.Unavailable -> toast("Can't extend: the next day is already booked")
            EvidenceRepository.ExtendResult.Closed -> toast("Can't extend: this rental has already been returned")
            is EvidenceRepository.ExtendResult.LowBalance -> toast("Your wallet can't cover the extra ${lkr(result.charge)}. Top it up first.")
            is EvidenceRepository.ExtendResult.Quote, is EvidenceRepository.ExtendResult.Done -> Unit
        }
    }

    fun finishReturn(ref: String, then: () -> Unit) = act {
        repo.finishReturn(ref)
        await(repo.booking) { it?.ref == ref && it.returnedAt != null }
        then()
    }

    fun resolveChange(markId: Long, then: () -> Unit) = act {
        repo.resolveChange(markId)
        toast("Agreed: not new. The rental has settled.")
        then()
    }

    fun raiseClaim(markId: Long, then: () -> Unit) = act {
        val ref = repo.raiseClaim(markId)
        if (ref == null) {
            toast("Cosmetic marks can't be claimed for on their own")
            return@act
        }
        await(app.money.currentClaim) { it?.ref == ref }
        toast("Claim $ref raised")
        then()
    }

    fun toggleFeatured(badge: Badge) = act {
        repo.toggleFeatured(badge)
        toast(if (badge.featured) "${badge.name} hidden from your profile" else "${badge.name} shown on your profile")
    }

    /** Rate the owner once the rental has settled; null removes the rating, with Undo. */
    fun rateOwner(ref: String, owner: String, stars: Int?, before: Int?) = act {
        app.discovery.rateBooking(ref, stars)
        if (stars != null) toast("Rated $owner ★$stars")
        else toast("Rating removed") { app.discovery.rateBooking(ref, before) }
    }

    /** Complete the open verification step; [onResult] gets null, or what is missing. */
    fun verify(proof: EvidenceRepository.Proof, onResult: (String?) -> Unit) = act {
        val error = repo.verifyCurrent(proof)
        if (error == null) toast("Verified")
        onResult(error)
    }

    fun removeAddress() = act {
        val before = repo.address.value
        repo.removeAddress()
        toast("Address removed") { if (before != null) repo.verifyCurrent(EvidenceRepository.Proof(address = before)) }
    }
}

// ============================================================ Exchange & Supply

class ExchangeSupplyViewModel(app: AppContainer) : GearViewModel(app) {
    private val repo = app.exchange

    val matches = repo.matches
    val hiddenCount = repo.hiddenCount
    val proposals = repo.proposals
    val proposal = repo.proposal
    val negotiation = repo.negotiation
    val cycle = repo.cycle
    val dashboard = repo.dashboard
    val attention = repo.attention
    val comingUp = repo.comingUp
    val units = repo.units
    val conflicts = repo.conflicts
    val requests = repo.requests
    val ownerUpcoming = repo.ownerUpcoming
    val ownerPast = repo.ownerPast
    val draftListing = repo.draftListing
    val myListings = repo.myListings
    val closet = repo.closet
    val blocks = app.store.blockRows
    val bookings = app.store.bookings
    val people = app.store.people
    val listings = app.store.allItems
    val account = app.accounts.current

    fun valueOf(item: GearItem) = repo.valueOf(item)
    fun valuationRows(item: GearItem) = repo.valuationRows(item)
    fun myGear() = repo.myGear()
    fun theirGear(p: com.gearloop.app.data.repo.Proposal) = repo.theirGear(p)
    fun myMove(p: com.gearloop.app.data.repo.Proposal) = repo.myMove(p)
    fun trustStats(handle: String) = repo.trustStats(handle)
    fun marketFor(sport: String, excluding: String? = null) = repo.marketFor(sport, excluding)
    fun cellsFor(u: CalendarUnit, from: LocalDate, bookings: List<com.gearloop.app.data.repo.Booking>, blocks: List<com.gearloop.app.data.db.CalendarBlockEntity>) =
        repo.cellsFor(u, from, bookings = bookings, blocks = blocks)
    fun occupancyFor(u: CalendarUnit, from: LocalDate) = repo.occupancyFor(u, from)
    fun blockAt(u: CalendarUnit, day: LocalDate) = repo.blockAt(u, day)
    fun selectProposal(ref: String) = repo.selectProposal(ref)
    fun openThread(handle: String) = app.money.openThread(handle)
    fun selectBooking(ref: String) = app.money.selectBooking(ref)
    fun selectListing(id: String) = app.session.selectListing(id)

    fun hideMatch(id: String, label: String) = act {
        repo.setMatchHidden(id, true)
        toast("Hid the $label match") { repo.setMatchHidden(id, false) }
    }

    /** Look for new trades for the member's gear. Quiet when there is nothing new. */
    fun refreshMatches(announce: Boolean = false) = act {
        val added = repo.refreshMatches()
        if (announce) toast(if (added > 0) "Found $added new ${if (added == 1) "trade" else "trades"}" else "No new trades right now")
    }

    fun propose(matchId: String, then: () -> Unit) = act {
        val ref = repo.proposeFromMatch(matchId) ?: return@act
        await(repo.proposal) { it?.ref == ref }
        then()
    }

    fun setGive(item: GearItem) = act { repo.setGive(item) }
    fun setGet(item: GearItem) = act { repo.setGet(item) }

    /** The cash on the slider travels with the send, in one write (D-14). */
    fun send(boot: Int? = null, then: () -> Unit) = act {
        val ref = repo.proposal.value?.ref
        repo.send(boot)
        toast("Proposal ${ref ?: ""} sent")
        then()
    }

    /** Create a request in the negotiation thread for a third-party valuation. */
    fun requestValuation() = act {
        val p = repo.proposal.value ?: return@act
        app.money.post(p.ref, me, "Requested an independent valuation of both items", com.gearloop.app.data.ChatMessage.Kind.SYSTEM)
        toast("Valuation requested. An assessor replies within 48 h.")
    }

    fun accept() = act {
        repo.accept()
        toast("Swap agreed. Arrange the handover in messages.")
    }

    fun decline() = act {
        repo.decline()
        toast("Proposal declined")
    }

    fun withdraw(then: () -> Unit) = act {
        repo.withdraw()
        toast("Proposal withdrawn")
        then()
    }

    fun setMyAcceptance(accepted: Boolean?) = act {
        repo.setMyAcceptance(accepted)
        toast(
            when (accepted) {
                true -> "You've accepted your part of the loop"
                false -> "You've declined. The loop won't go ahead."
                null -> "Your answer has been withdrawn"
            },
        )
    }

    fun completeTask(taskId: String) = act {
        val message = repo.completeTask(taskId)
        toast(message ?: "Done")
    }

    fun reviewRenter(taskId: String, renterHandle: String, listingId: String, stars: Int, body: String) = act {
        repo.reviewRenter(taskId, renterHandle, listingId, stars, body)
        toast("Review posted for the renter")
    }

    fun toggleBlock(u: CalendarUnit, day: LocalDate) = act {
        val existing = repo.blockAt(u, day)
        if (existing != null) {
            val row = repo.unblock(existing.id) ?: return@act
            toast("${u.label} is free again on ${dayMonth(day)}") { repo.restoreBlock(row) }
        } else {
            val id = repo.block(u, day)
            if (id == null) toast("${dayMonth(day)} is already booked on ${u.label}")
            else toast("Blocked ${dayMonth(day)} on ${u.label}") { repo.unblock(id) }
        }
    }

    fun moveToUnit(ref: String, unit: Int) = act {
        repo.moveToUnit(ref, unit)
        toast("Moved $ref to unit #$unit. The conflict is cleared.")
    }

    fun acceptRequest(ref: String, renter: String) = act {
        val ok = repo.accept(ref)
        toast(if (ok) "Accepted. $renter has been told." else "Can't accept: those dates are no longer free")
    }

    fun declineRequest(ref: String, renter: String) = act {
        repo.decline(ref)
        toast("Declined. $renter has been refunded.")
    }

    // ---- listings

    fun startListing(fromClosetId: Long? = null, sport: String? = null, then: () -> Unit) = act {
        val id = repo.startListing(fromClosetId, sport)
        await(repo.draftListing) { it?.id == id }
        then()
    }

    fun editListing(id: String, then: () -> Unit) = act {
        repo.editListing(id)
        await(repo.draftListing) { it?.id == id }
        then()
    }

    /** A photo just taken with the camera into [file], for slot [slot]. */
    fun photoTaken(slot: Int, file: File) = act {
        val key = PhotoStore.adoptCapture(file) ?: return@act toast("That photo couldn't be saved. Try again.")
        replacePhoto(slot, key)
    }

    /** A photo chosen from the gallery, for slot [slot]. */
    fun photoChosen(slot: Int, uri: Uri) = act {
        val key = import(uri, "listing") ?: return@act toast("That picture couldn't be read")
        replacePhoto(slot, key)
    }

    private suspend fun replacePhoto(slot: Int, key: String) {
        val old = repo.draftListing.value?.photos?.getOrNull(slot)
        repo.setPhoto(slot, key)
        if (old != null && old != key) PhotoStore.delete(app.appContext, old)
    }

    fun removePhoto(slot: Int, label: String) = act {
        repo.removePhoto(slot)?.let { PhotoStore.delete(app.appContext, it) }
        toast("Removed the $label photo")
    }

    fun saveDraft() = act {
        repo.saveDraft()
        toast("Draft saved. Finish it from your listings any time.")
    }
    fun setDetails(title: String, size: String, sport: String? = null, grade: Grade? = null, description: String? = null) =
        act { repo.setDetails(title, size, sport, grade, description) }
    fun setPrice(price: Int) = act { repo.setPrice(price) }
    fun setDeposit(deposit: Int) = act { repo.setDeposit(deposit) }
    fun setDiscount(index: Int, on: Boolean) = act { repo.setDiscount(index, on) }
    fun setWaiveDeposit(waive: Boolean) = act { repo.setWaiveDeposit(waive) }
    fun setInstantBook(on: Boolean) = act { repo.setInstantBook(on) }
    fun setDelivery(on: Boolean) = act { repo.setDelivery(on) }

    fun publish(then: () -> Unit) = act {
        val wasDraft = repo.draftListing.value?.status == com.gearloop.app.data.ListingStatus.DRAFT
        repo.publish() ?: return@act
        toast(if (wasDraft) "Listed. It's live in search now." else "Changes saved")
        repo.refreshMatches()
        then()
    }

    fun setPaused(item: GearItem, paused: Boolean) = act {
        repo.setPaused(item.id, paused)
        toast(if (paused) "${item.shortTitle} paused. It's out of search until you resume it." else "${item.shortTitle} is live again")
    }

    fun deleteListing(item: GearItem) = act {
        if (repo.deleteListing(item.id)) toast("Deleted ${item.shortTitle}")
        else toast("Can't delete ${item.shortTitle} while a booking holds dates on it. Pause it instead.")
    }

    fun discardListing(then: () -> Unit) = act {
        repo.discardDraft()
        toast("Draft deleted")
        then()
    }

    // ---- closet

    fun addClosetItem(title: String, sport: String, size: String, grade: Grade, photoKey: String?) = act {
        repo.addClosetItem(title, sport, size, grade, photoKey)
        toast("Added ${title.trim().ifEmpty { "gear" }} to your closet")
    }

    fun updateClosetItem(id: Long, title: String, size: String, grade: Grade, photoKey: String?) = act {
        repo.updateClosetItem(id, title, size, grade, photoKey)
        toast("Saved")
    }

    fun deleteClosetItem(id: Long, title: String) = act {
        val row = repo.deleteClosetItem(id) ?: return@act
        toast("Removed $title from your closet") { repo.restoreClosetItem(row) }
    }

    suspend fun adoptPhoto(file: File): String? = PhotoStore.adoptCapture(file)
    suspend fun importPhoto(uri: Uri): String? = import(uri, "closet")

    /** The angles a listing's photos are taken from: the same seven the handover checks. */
    fun anglesFor(item: GearItem) = CaptureGuide.anglesFor(item.sport)
}
