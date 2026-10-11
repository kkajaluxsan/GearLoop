package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.CaptureGuide
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Demo
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Phase
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.Quote
import com.gearloop.app.data.ScoreFactor
import com.gearloop.app.data.TrustTier
import com.gearloop.app.data.Validate
import com.gearloop.app.data.VerificationStep
import com.gearloop.app.data.db.BookingEntity
import com.gearloop.app.data.db.CaptureEntity
import com.gearloop.app.data.db.ClaimEntity
import com.gearloop.app.data.db.DamageMarkEntity
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.db.WalletTxnEntity
import com.gearloop.app.data.lkr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Evidence & Trust: the handover, the condition record, the rental, the return,
 * the trust score and verification.
 *
 * The evidence chain is the product's core promise, so it is stored as records
 * rather than pictures: each frame knows its booking, its angle, which side of
 * the rental it was taken on, when, and — if the phone allowed it — where. The
 * return check pairs the two sides by angle, a difference is a damage mark
 * recorded at return, and a claim is raised from that mark — so the claim and
 * the claims desk argue about the same record the pickup made.
 *
 * Handover happens with both people present and one phone between them: the
 * renter scans the gear's tag, the two of them photograph it together, and each
 * signs the record on the screen.
 */
class EvidenceRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val money: MoneyRepository,
    private val scope: CoroutineScope,
) {
    private val dao = db.evidence()
    private val me: String get() = session.handle

    /** One read-modify-write of a booking at a time (D-14). */
    private val edits = Mutex()

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    // ============================================================ the booking

    /**
     * The booking the handover screens are about: the one selected, else the
     * member's live rental, else their next confirmed one.
     */
    val booking: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        val h = s.currentHandle.orEmpty()
        val involved = list.filter { (it.renter.handle == h || it.owner.handle == h) && it.status != BookingStatus.DRAFT }
        val mine = involved.filter { it.renter.handle == h }
        involved.firstOrNull { it.ref == s.bookingRef }
            ?: mine.filter { it.status == BookingStatus.ACTIVE || it.status == BookingStatus.RETURNED }.minByOrNull { it.start }
            ?: mine.filter { it.status == BookingStatus.CONFIRMED }.minByOrNull { it.start }
    }.hot(null)

    val phase: StateFlow<Phase> = session.session.map { it.capturePhase }.hot(Phase.PICKUP)

    /** The seven angles for the gear in the booking on screen. */
    val angles: StateFlow<List<String>> = booking.map { b -> CaptureGuide.anglesFor(b?.item?.sport ?: "Surfing") }
        .hot(CaptureGuide.anglesFor("Surfing"))

    val frames: StateFlow<Map<Phase, Map<String, CaptureFrame>>> = combine(booking, store.captureRows) { b, rows ->
        if (b == null) emptyMap()
        else rows.filter { it.bookingRef == b.ref }
            .groupBy { it.phase }
            .mapValues { (_, list) -> list.associate { it.angle to it.toFrame() } }
    }.hot(emptyMap())

    val marks: StateFlow<List<Mark>> = combine(booking, store.markRows) { b, rows ->
        if (b == null) emptyList() else rows.filter { it.bookingRef == b.ref }.map { it.toMark() }
    }.hot(emptyList())

    /** The same angle at pickup and at return, and the mark that changed it. */
    val pairs: StateFlow<List<AnglePair>> = combine(angles, frames, marks) { list, f, m ->
        list.map { angle ->
            AnglePair(
                angle = angle,
                pickup = f[Phase.PICKUP]?.get(angle),
                back = f[Phase.RETURN]?.get(angle),
                change = m.firstOrNull { it.phase == Phase.RETURN && it.area == angle && !it.resolved },
            )
        }
    }.hot(emptyList())

    fun startCapture(phase: Phase) = session.selectCapturePhase(phase)

    fun select(ref: String) = session.selectBooking(ref)

    // ============================================================ before pickup

    /** Tick or untick an item on the "before you go" checklist. */
    suspend fun toggleChecklist(ref: String, index: Int) = editBooking(ref) { row ->
        row.copy(checklist = (if (index in row.checklist) row.checklist - index else row.checklist + index).sorted())
    }

    // ============================================================ the gear tag

    sealed interface TagResult {
        data object Matched : TagResult
        /** A GearLoop tag, but for different gear — [title] when the code is a listing here. */
        data class OtherItem(val code: String, val title: String? = null) : TagResult
        data object NotATag : TagResult
    }

    /**
     * Check what was scanned or typed against the booking. A GearLoop tag
     * encodes `GEARLOOP:<item code>`; the code printed under the QR is accepted
     * typed, and so is the booking's six-digit handover code. A match records the
     * handover as started for both people, who are standing together.
     */
    suspend fun verifyTag(ref: String, scanned: String): TagResult {
        val row = db.money().booking(ref) ?: return TagResult.NotATag
        val listing = db.discovery().listing(row.listingId) ?: return TagResult.NotATag
        val value = scanned.trim()
        val code = value.removePrefix(TAG_PREFIX).trim().uppercase()
        val digits = value.filter(Char::isDigit)
        // "PL-7734", "PL7734" and "pl 7734" are the same printed code.
        val bare = { s: String -> s.uppercase().filter(Char::isLetterOrDigit) }
        val matched = bare(code) == bare(listing.code) || (digits.length == 6 && digits == row.handoverCode)
        if (!matched) {
            if (!value.startsWith(TAG_PREFIX) && !Regex("^[A-Z]{2}[- ]?\\d{4}$").matches(code)) return TagResult.NotATag
            val other = db.discovery().listingByCode("${bare(code).take(2)}-${bare(code).drop(2)}")
            return TagResult.OtherItem(other?.code ?: code, other?.title)
        }
        val now = AppClock.nowMillis()
        editBooking(ref) { it.copy(handshakeRenterAt = it.handshakeRenterAt ?: now, handshakeOwnerAt = it.handshakeOwnerAt ?: now) }
        return TagResult.Matched
    }

    /** The six-digit fallback, kept as its own call for the screens that only take digits. */
    suspend fun enterCode(ref: String, code: String): Boolean = verifyTag(ref, code) == TagResult.Matched

    // ============================================================ the condition record

    /**
     * Create (or replace, on a retake) the frame for one angle: the photo just
     * taken, whether it passed the capture-time check, and where it was taken.
     */
    suspend fun capture(
        ref: String, phase: Phase, angle: String, photoKey: String, ok: Boolean = true,
        latitude: Double? = null, longitude: Double? = null,
    ) {
        val existing = dao.capture(ref, phase, angle)
        dao.upsertCapture(
            CaptureEntity(
                id = existing?.id ?: 0, bookingRef = ref, phase = phase, angle = angle, photoKey = photoKey,
                ok = ok, capturedAt = AppClock.nowMillis(), latitude = latitude, longitude = longitude,
            ),
        )
    }

    /** The capture-time check overridden by the people holding the phone. */
    suspend fun setFrameOk(ref: String, phase: Phase, angle: String, ok: Boolean) {
        val existing = dao.capture(ref, phase, angle) ?: return
        dao.upsertCapture(existing.copy(ok = ok))
    }

    /** Delete a frame, so the angle can be shot again. Returns its photo key. */
    suspend fun deleteFrame(ref: String, phase: Phase, angle: String): String? {
        val key = dao.capture(ref, phase, angle)?.photoKey
        dao.deleteCapture(ref, phase, angle)
        return key
    }

    // ============================================================ damage marks

    /** Create a damage mark. Cosmetic marks at pickup are what protect the renter later. */
    suspend fun addMark(
        ref: String, phase: Phase, area: String, severity: DamageMark.Severity, note: String, x: Float, y: Float,
        photoKey: String? = null,
    ): Long = dao.insertMark(
        DamageMarkEntity(
            bookingRef = ref, phase = phase, area = area, summary = summaryFor(severity), severity = severity,
            note = note.trim(), x = x, y = y,
            photoKey = photoKey ?: dao.capture(ref, phase, area)?.photoKey,
            createdAt = AppClock.nowMillis(), resolved = false,
        ),
    )

    suspend fun updateMark(id: Long, area: String, severity: DamageMark.Severity, note: String, x: Float, y: Float, photoKey: String? = null) {
        val row = dao.mark(id) ?: return
        dao.upsertMark(
            row.copy(
                area = area, severity = severity, summary = summaryFor(severity), note = note.trim(), x = x, y = y,
                photoKey = photoKey ?: row.photoKey ?: dao.capture(row.bookingRef, row.phase, area)?.photoKey,
            ),
        )
    }

    /** Delete a mark. Returns it so the toast can put it back. */
    suspend fun deleteMark(id: Long): DamageMarkEntity? {
        val row = dao.mark(id) ?: return null
        dao.deleteMark(id)
        return row
    }

    suspend fun restoreMark(row: DamageMarkEntity) {
        dao.insertMark(row.copy(id = 0))
    }

    private fun summaryFor(severity: DamageMark.Severity) = when (severity) {
        DamageMark.Severity.COSMETIC -> "Surface mark"
        DamageMark.Severity.FUNCTIONAL -> "Functional damage"
        DamageMark.Severity.MAJOR -> "Structural damage"
    }

    // ============================================================ signatures

    enum class Signer { RENTER, OWNER }

    /**
     * One of the two signatures on the pickup record, drawn on the screen.
     * When both are on it, the rental starts.
     */
    suspend fun sign(ref: String, signer: Signer, signatureKey: String?) {
        val now = AppClock.nowMillis()
        val before = db.money().booking(ref) ?: return
        editBooking(ref) {
            when (signer) {
                Signer.RENTER -> it.copy(renterSignedAt = now, renterSignature = signatureKey ?: it.renterSignature)
                Signer.OWNER -> it.copy(ownerSignedAt = now, ownerSignature = signatureKey ?: it.ownerSignature)
            }
        }
        val row = db.money().booking(ref) ?: return
        val wasFullySigned = before.renterSignedAt != null && before.ownerSignedAt != null
        if (row.renterSignedAt != null && row.ownerSignedAt != null && !wasFullySigned) start(row.ref)
    }

    /** Both signatures are on the record: the rental is live. */
    private suspend fun start(ref: String) {
        val row = db.money().booking(ref) ?: return
        val item = store.item(row.listingId)
        val now = AppClock.nowMillis()
        val returnBy = com.gearloop.app.data.dayTime(Seed.dayAt(LocalDate.ofEpochDay(row.endDay), row.returnTime))
        db.withTransaction {
            if (row.status == BookingStatus.CONFIRMED) {
                db.money().upsertBooking(row.copy(status = BookingStatus.ACTIVE, updatedAt = now))
            }
            money.postDm(row.renterHandle, row.ownerHandle, MoneyRepository.SYSTEM, "Condition record signed by both · ${row.ref}", ChatMessage.Kind.SYSTEM)
            money.notify(row.renterHandle, NotificationItem.Kind.BOOKING, "Rental started · ${item?.shortTitle ?: row.ref}", "Return by $returnBy", "s17", row.ref)
            money.notify(row.ownerHandle, NotificationItem.Kind.BOOKING, "Handed over · ${item?.shortTitle ?: row.ref}", "Due back $returnBy", "s28", row.ref)
        }
    }

    /** The renter's signature with no drawing — used where the record is signed in code. */
    suspend fun signAsRenter(ref: String) = sign(ref, Signer.RENTER, null)

    suspend fun ownerSigns(ref: String) = sign(ref, Signer.OWNER, null)

    // ============================================================ the rental

    suspend fun toggleReturnChecklist(ref: String, index: Int) = editBooking(ref) { row ->
        row.copy(returnChecklist = (if (index in row.returnChecklist) row.returnChecklist - index else row.returnChecklist + index).sorted())
    }

    sealed interface ExtendResult {
        /** What the extra day would cost, before anything is written. */
        data class Quote(val newEnd: LocalDate, val charge: Int) : ExtendResult
        data class Done(val newEnd: LocalDate, val charged: Int) : ExtendResult
        data object Unavailable : ExtendResult
        /** Paid from the wallet, and the wallet can't cover the extra day. */
        data class LowBalance(val charge: Int) : ExtendResult
        /** Already back with the owner, or never going out: nothing to extend. */
        data object Closed : ExtendResult
    }

    private sealed interface Priced {
        class Ok(val row: BookingEntity, val item: GearItem, val end: LocalDate, val quote: Quote, val charge: Int) : Priced
        class No(val why: ExtendResult) : Priced
    }

    /**
     * The booking kept [days] longer — only if those days, and the buffer after
     * them, are free on this unit — priced at the booking's own terms.
     */
    private suspend fun priceExtension(ref: String, days: Int): Priced {
        val row = db.money().booking(ref) ?: return Priced.No(ExtendResult.Unavailable)
        if (row.status != BookingStatus.ACTIVE && row.status != BookingStatus.CONFIRMED) return Priced.No(ExtendResult.Closed)
        val item = store.item(row.listingId) ?: return Priced.No(ExtendResult.Unavailable)
        val start = LocalDate.ofEpochDay(row.startDay)
        val end = LocalDate.ofEpochDay(row.endDay)
        val extra = Availability.Span(end.plusDays(1), end.plusDays(days.toLong()))
        val ok = Availability.isRangeFree(
            extra, AppClock.today, store.spansNow(row.listingId, row.unit, excluding = ref), store.blocksNow(row.listingId, row.unit),
        )
        if (!ok) return Priced.No(ExtendResult.Unavailable)
        val newDays = (extra.end.toEpochDay() - start.toEpochDay() + 1).toInt()
        val quote = Pricing.quote(item, newDays, row.protection, row.coupon > 0, row.includeExtras)
        return Priced.Ok(row, item, extra.end, quote, (quote.total - row.total).coerceAtLeast(0))
    }

    /** What keeping it [days] longer would cost, so the member can decide first. */
    suspend fun extendQuote(ref: String, days: Int = 1): ExtendResult =
        when (val x = priceExtension(ref, days)) {
            is Priced.Ok -> ExtendResult.Quote(x.end, x.charge)
            is Priced.No -> x.why
        }

    /**
     * Keep it a day longer. The difference in price is taken there and then
     * (D-25), the same way the booking was paid for.
     */
    suspend fun extend(ref: String, days: Int = 1): ExtendResult = edits.withLock {
        val x = when (val priced = priceExtension(ref, days)) {
            is Priced.Ok -> priced
            is Priced.No -> return@withLock priced.why
        }
        val row = x.row
        val renter = db.discovery().profileNow(row.renterHandle)
        if (row.paymentMethod == MoneyRepository.PAY_WALLET && (renter?.walletAvailable ?: 0) < x.charge) {
            return@withLock ExtendResult.LowBalance(x.charge)
        }
        val now = AppClock.nowMillis()
        db.withTransaction {
            db.money().upsertBooking(
                row.copy(endDay = x.end.toEpochDay(), total = x.quote.total, depositHold = x.quote.depositHold, ownerPayout = x.quote.ownerPayout, updatedAt = now),
            )
            if (x.charge > 0) money.charge(row.renterHandle, row.paymentMethod, x.charge, "Extension · ${row.ref} +$days day", renter?.cardBrand, renter?.cardLast4)
            money.postDm(row.renterHandle, row.ownerHandle, MoneyRepository.SYSTEM, "Rental extended to ${com.gearloop.app.data.dayMonth(x.end)} · ${row.ref}", ChatMessage.Kind.SYSTEM)
            val more = (x.quote.ownerPayout - row.ownerPayout).coerceAtLeast(0)
            money.notify(row.ownerHandle, NotificationItem.Kind.BOOKING, "${x.item.shortTitle} extended", "Now back on ${com.gearloop.app.data.dayMonth(x.end)} · ${lkr(more)} more for you", "s28", row.ref)
        }
        ExtendResult.Done(x.end, x.charge)
    }

    /**
     * Something has gone wrong during the rental. It goes to the other person as
     * a message carrying the booking reference, so the conversation about it
     * starts with the facts, and they're told at once.
     */
    suspend fun reportIssue(ref: String, problem: String, note: String): String? {
        val row = db.money().booking(ref) ?: return null
        val item = store.item(row.listingId) ?: return null
        val other = if (me == row.ownerHandle) row.renterHandle else row.ownerHandle
        val name = store.person(me).name.substringBefore(' ')
        val body = buildString {
            append("Problem with the ${item.shortTitle} (${row.ref}): $problem.")
            note.trim().takeIf { it.isNotEmpty() }?.let { append(" ").append(it) }
        }
        db.withTransaction {
            money.postDm(me, other, me, body, ChatMessage.Kind.TEXT)
            money.notify(other, NotificationItem.Kind.MESSAGE, "$name reported a problem", body.take(80), "s33", me)
        }
        return other
    }

    // ============================================================ the return

    /**
     * The return frames are in. If nothing was marked as new, the rental settles
     * there and then; if something was, it waits for the owner's decision on the
     * difference.
     */
    suspend fun finishReturn(ref: String) {
        val row = db.money().booking(ref) ?: return
        val now = AppClock.nowMillis()
        val changes = dao.marksFor(ref).count { it.phase == Phase.RETURN && !it.resolved }
        val openChanges = changes > 0
        val title = store.item(row.listingId)?.shortTitle ?: ref
        val renter = store.person(row.renterHandle).name.substringBefore(' ')
        db.withTransaction {
            db.money().upsertBooking(
                row.copy(
                    status = if (openChanges) BookingStatus.RETURNED else BookingStatus.COMPLETED,
                    returnedAt = row.returnedAt ?: now,
                    updatedAt = now,
                ),
            )
            if (openChanges) {
                money.notify(
                    row.ownerHandle, NotificationItem.Kind.SAFETY, "$renter returned the $title · check $changes change${if (changes > 1) "s" else ""}",
                    "Something on the return isn't in the pickup photos. You have 24 hours to look.", "s18b", row.ref,
                )
            } else {
                money.notify(row.ownerHandle, NotificationItem.Kind.BOOKING, "$renter returned the $title", "Nothing new on the return. Your payout is on its way.", "s24", row.ref)
                settle(row.ref)
            }
        }
        session.selectCapturePhase(Phase.PICKUP)
    }

    /**
     * The rental is over: the deposit released, the owner paid, the renter's
     * record moved on. [claim] is set when a difference went on to a claim, which
     * runs on its own from here and is what the renter needs to hear about.
     */
    private suspend fun settle(ref: String, claim: String? = null) {
        val row = db.money().booking(ref) ?: return
        val item = store.item(row.listingId)
        money.notify(
            row.renterHandle,
            NotificationItem.Kind.BOOKING,
            "Rental complete · ${item?.shortTitle ?: ref}",
            when {
                claim != null -> "The rental is closed. Claim $claim stays open until it's decided."
                row.depositHold > 0 -> "Your ${lkr(row.depositHold)} hold has been released."
                else -> "No deposit was held. Nothing more to pay."
            },
            "s18",
            ref,
        )
        money.payOwner(row, item?.shortTitle ?: "rental")
        db.discovery().person(row.renterHandle)?.let { p ->
            val completed = p.completedRentals + 1
            db.discovery().upsertPerson(p.copy(completedRentals = completed))
            dao.earnBadge(row.renterHandle, "First loop")
            if (completed >= 10) dao.earnBadge(row.renterHandle, "10 rentals")
        }
    }

    /**
     * The owner has 24 hours to look at a difference flagged on return. One left
     * unanswered is taken as agreed to be wear, and the rental settles, so the
     * renter is never left waiting on someone who has gone quiet. A difference
     * already turned into a claim stays with the claim, which has its own clock.
     */
    suspend fun closeLapsedReviews() {
        val now = AppClock.nowMillis()
        db.money().returnsAwaitingReviewSince(now - REVIEW_WINDOW_MS).forEach { row ->
            val open = dao.marksFor(row.ref).filter { it.phase == Phase.RETURN && !it.resolved }
            val claim = open.firstNotNullOfOrNull { db.money().claimFor(row.ref, it.area)?.ref }
            val title = store.item(row.listingId)?.shortTitle ?: row.ref
            db.withTransaction {
                open.filter { db.money().claimFor(row.ref, it.area) == null }.forEach { dao.setMarkResolved(it.id, true) }
                db.money().upsertBooking(row.copy(status = BookingStatus.COMPLETED, updatedAt = now))
                settle(row.ref, claim)
                money.notify(
                    row.ownerHandle, NotificationItem.Kind.BOOKING, "Return review closed · $title",
                    "The 24 hours to check the return passed, so the rental has settled.", "s24", row.ref,
                )
            }
        }
    }

    /** "It's the same" — the difference is agreed not to be new. */
    suspend fun resolveChange(markId: Long) {
        val mark = dao.mark(markId) ?: return
        val now = AppClock.nowMillis()
        db.withTransaction {
            dao.setMarkResolved(markId, true)
            // A difference that became a claim is the claim's to settle, not this review's.
            val rest = dao.marksFor(mark.bookingRef).filter { it.phase == Phase.RETURN && !it.resolved && it.id != markId }
            val claim = rest.firstNotNullOfOrNull { db.money().claimFor(mark.bookingRef, it.area)?.ref }
            val stillOpen = rest.any { db.money().claimFor(mark.bookingRef, it.area) == null }
            val row = db.money().booking(mark.bookingRef)
            if (!stillOpen && row != null && row.status == BookingStatus.RETURNED) {
                db.money().upsertBooking(row.copy(status = BookingStatus.COMPLETED, updatedAt = now))
                settle(row.ref, claim)
            }
        }
    }

    /**
     * "Raise a claim" — Create the claim from the return mark. Cosmetic marks
     * cannot be claimed for on their own; the rubric says so, and this refuses
     * the same thing.
     */
    suspend fun raiseClaim(markId: Long): String? {
        val mark = dao.mark(markId) ?: return null
        if (mark.severity == DamageMark.Severity.COSMETIC) return null
        val row = db.money().booking(mark.bookingRef) ?: return null
        val item = store.item(row.listingId) ?: return null
        val value = if (item.id == Demo.torq.id) Demo.BOARD_VALUE else Pricing.estimatedValue(item)
        val cap = value * mark.severity.capPercent / 100
        val estimate = when {
            item.id == Demo.torq.id && mark.severity == DamageMark.Severity.FUNCTIONAL -> Demo.CLAIM_AMOUNT
            mark.severity == DamageMark.Severity.FUNCTIONAL -> value * 15 / 100
            else -> value * 60 / 100
        }.coerceAtMost(cap)
        val existing = db.money().claimFor(row.ref, mark.area)
        if (existing != null) {
            db.withTransaction { closeIfAllClaimed(row.ref, existing.ref) }
            session.selectClaim(existing.ref)
            return existing.ref
        }
        val ref = nextClaimRef(db.money().claimRefs().toSet())
        val now = AppClock.nowMillis()
        db.withTransaction {
            db.money().upsertClaim(
                ClaimEntity(
                    ref = ref, bookingRef = row.ref, listingTitle = item.shortTitle,
                    claimantHandle = row.ownerHandle, respondentHandle = row.renterHandle,
                    amount = estimate, itemValue = value, severity = mark.severity, area = mark.area,
                    note = mark.note.ifBlank { "Found on return; not in the pickup record." },
                    status = ClaimStatus.OPEN, createdAt = now, respondBy = now + Demo.CLAIM_RESPOND_DAYS * MoneyRepository.DAY_MS,
                    renterStatement = null, renterEvidence = null, renterPhotos = emptyList(),
                    award = null, rationale = null, decidedAt = null, decidedBy = null,
                ),
            )
            money.notify(
                row.renterHandle, NotificationItem.Kind.SAFETY,
                "${item.owner.name.substringBefore(' ')} reported new damage",
                "${item.shortTitle} · ${mark.area} · respond within ${Demo.CLAIM_RESPOND_DAYS} days", "s34", ref,
            )
            money.notify(row.ownerHandle, NotificationItem.Kind.SAFETY, "Claim $ref raised", "${item.shortTitle} · ${mark.area} · ${lkr(estimate)}", "s34", ref)
            closeIfAllClaimed(row.ref, ref)
        }
        session.selectClaim(ref)
        return ref
    }

    /**
     * Once every difference on a return is either agreed or claimed for, the
     * rental closes and pays out; the claims carry on by themselves rather than
     * hold the rental open through a dispute that may take days.
     */
    private suspend fun closeIfAllClaimed(bookingRef: String, claim: String) {
        val row = db.money().booking(bookingRef) ?: return
        if (row.status != BookingStatus.RETURNED) return
        val open = dao.marksFor(bookingRef).filter { it.phase == Phase.RETURN && !it.resolved }
        if (open.any { db.money().claimFor(bookingRef, it.area) == null }) return
        db.money().upsertBooking(row.copy(status = BookingStatus.COMPLETED, updatedAt = AppClock.nowMillis()))
        settle(bookingRef, claim)
    }

    private fun nextClaimRef(used: Set<String>): String {
        var n = 3081
        while ("CL-$n" in used) n++
        return "CL-$n"
    }

    // ============================================================ trust

    val verification: StateFlow<List<VerificationStep>> = store.verificationRows.map { rows ->
        rows.map { VerificationStep(it.title, it.unlocks, it.state) }
    }.hot(emptyList())

    /** What each completed step recorded, by title: the phone number, the address. */
    val verificationDetails: StateFlow<Map<String, String>> = store.verificationRows.map { rows ->
        rows.mapNotNull { r -> r.detail?.let { r.title to it } }.toMap()
    }.hot(emptyMap())

    val badges: StateFlow<List<Badge>> = store.badgeRows.map { rows ->
        rows.map { Badge(it.name, it.earned, it.featured) }
    }.hot(emptyList())

    data class Trust(
        val score: Int,
        val tier: TrustTier,
        val pointsToGold: Int,
        val progress: Float,
        val factors: List<ScoreFactor>,
        val verified: Int,
    )

    /**
     * The score, recomputed from the things it says it is made of: 30 points per
     * verification step, 6 per completed rental, and a damage claim upheld
     * against you costs 40.
     */
    val trust: StateFlow<Trust> = combine(store.me, store.peopleRows, store.verificationRows, store.claimRows) { h, people, steps, claims ->
        val person = people.firstOrNull { it.handle == h }
        val verified = steps.count { it.state == VerificationStep.State.DONE }
        val completed = person?.completedRentals ?: 0
        val upheld = claims.count { it.respondentHandle == h && (it.award ?: 0) > 0 }
        val score = (742 + 30 * (verified - 5) + 6 * (completed - 14) - 40 * upheld).coerceIn(0, 1000)
        val earned = when {
            score >= GOLD -> TrustTier.GOLD
            score >= 600 -> TrustTier.SILVER
            else -> TrustTier.BRONZE
        }
        // A tier already earned — GOLD as a host — is kept; only an upheld claim takes one away.
        val tier = if (upheld == 0 && person != null && person.tier.ordinal > earned.ordinal) person.tier else earned
        val damageFree = if (completed == 0) 100 else ((completed - upheld).coerceAtLeast(0) * 100 / completed)
        val shown = store.people.value[h]
        val rating = shown?.takeIf { it.reviews > 0 }?.ratingText
        Trust(
            score = score,
            tier = tier,
            pointsToGold = if (tier == TrustTier.GOLD) 0 else (GOLD - score).coerceAtLeast(0),
            progress = ((score - 600f) / (GOLD - 600f)).coerceIn(0f, 1f),
            factors = listOf(
                ScoreFactor("Verification", "$verified / 6", verified / 6f),
                ScoreFactor("Completed rentals", "$completed", (completed / 20f).coerceAtMost(1f)),
                ScoreFactor("On-time returns", "${person?.onTimeReturns ?: 100}%", (person?.onTimeReturns ?: 100) / 100f),
                ScoreFactor("Damage-free", "$damageFree%", damageFree / 100f),
                ScoreFactor("Reviews", if (rating != null) "★ $rating" else "None yet", ((shown?.rating ?: 0.0) / 5.0).toFloat()),
            ),
            verified = verified,
        )
    }.hot(Trust(0, TrustTier.BRONZE, GOLD, 0f, emptyList(), 0))

    init {
        // The tier other members see follows the score, for whoever is signed in.
        scope.launch {
            trust.collect { t ->
                val h = me
                if (h.isEmpty() || t.factors.isEmpty()) return@collect
                val p = db.discovery().person(h) ?: return@collect
                if (p.tier.ordinal < t.tier.ordinal || p.verifiedSteps != t.verified) {
                    db.discovery().upsertPerson(p.copy(tier = maxOf(p.tier, t.tier), verifiedSteps = t.verified))
                }
            }
        }
    }

    suspend fun toggleFeatured(badge: Badge) = dao.setBadgeFeatured(me, badge.name, !badge.featured)

    // ============================================================ verification

    /** What a step is completed with. Each step asks for the one thing it names. */
    data class Proof(
        val phone: String? = null,
        val photoKey: String? = null,
        val cardNumber: String? = null,
        val cardExpiry: String? = null,
        val address: String? = null,
    )

    /**
     * Complete the current step with its proof, and open the one after it.
     * Returns null on success, or the sentence explaining what is missing.
     */
    suspend fun verifyCurrent(proof: Proof): String? {
        val steps = dao.verificationStepsNow(me)
        val current = steps.firstOrNull { it.state == VerificationStep.State.CURRENT } ?: return "Nothing left to verify"
        val detail: String? = when (current.title) {
            "Phone number" -> {
                val phone = proof.phone.orEmpty()
                Validate.phone(phone)?.let { return it }
                Validate.normalisePhone(phone)
            }
            "Government ID", "Selfie match" -> proof.photoKey ?: return "Take the photo first"
            "Payment method" -> {
                money.saveCard(proof.cardNumber.orEmpty(), proof.cardExpiry.orEmpty())?.let { return it }
                proof.cardNumber?.filter(Char::isDigit)?.takeLast(4)?.let { "Card ending $it" }
            }
            VerificationStep.HOME_ADDRESS -> {
                val address = proof.address?.trim().orEmpty()
                if (address.length < 8) return "Enter your full address"
                address
            }
            else -> null
        }
        val now = AppClock.nowMillis()
        db.withTransaction {
            dao.upsertVerificationStep(current.copy(state = VerificationStep.State.DONE, completedAt = now, detail = detail))
            steps.firstOrNull { it.position > current.position && it.state == VerificationStep.State.LOCKED }
                ?.let { dao.upsertVerificationStep(it.copy(state = VerificationStep.State.CURRENT)) }
            if (current.title == VerificationStep.HOME_ADDRESS) dao.setAddress(me, detail)
            if (current.title == "Phone number") {
                db.accounts().byHandle(me)?.let { db.accounts().upsert(it.copy(phone = detail)) }
            }
        }
        return null
    }

    /** The older single-step call, for the home address. */
    suspend fun verifyCurrent(address: String?): Boolean = verifyCurrent(Proof(address = address)) == null

    /** Withdraw a verified address, which re-opens the step. */
    suspend fun removeAddress() {
        val steps = dao.verificationStepsNow(me)
        val step = steps.firstOrNull { it.title == VerificationStep.HOME_ADDRESS } ?: return
        if (step.state != VerificationStep.State.DONE) return
        db.withTransaction {
            // Any step after it closes again, so there is only ever one open step.
            steps.filter { it.position > step.position && it.state == VerificationStep.State.CURRENT }
                .forEach { dao.upsertVerificationStep(it.copy(state = VerificationStep.State.LOCKED)) }
            dao.upsertVerificationStep(step.copy(state = VerificationStep.State.CURRENT, completedAt = null, detail = null))
            dao.setAddress(me, null)
        }
    }

    val address: StateFlow<String?> = store.profileRow.map { it?.address }.hot(null)

    // ============================================================ helpers

    private suspend fun editBooking(ref: String, change: (com.gearloop.app.data.db.BookingEntity) -> com.gearloop.app.data.db.BookingEntity) = edits.withLock {
        val row = db.money().booking(ref) ?: return@withLock
        db.money().upsertBooking(change(row).copy(updatedAt = AppClock.nowMillis()))
    }

    private fun CaptureEntity.toFrame() = CaptureFrame(id, angle, photoKey, ok, AppClock.fromMillis(capturedAt), latitude, longitude)

    private fun DamageMarkEntity.toMark() = Mark(
        id = id, phase = phase, area = area, summary = summary, severity = severity, note = note,
        x = x, y = y, photo = photoKey, createdAt = AppClock.fromMillis(createdAt), resolved = resolved,
    )

    companion object {
        const val GOLD = 800

        /** What a GearLoop gear tag's QR code holds before the item code. */
        const val TAG_PREFIX = "GEARLOOP:"

        /** How long an owner has to review a difference flagged on return. */
        const val REVIEW_WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}
