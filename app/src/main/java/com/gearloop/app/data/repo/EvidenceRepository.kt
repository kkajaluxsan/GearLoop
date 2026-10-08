package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Phase
import com.gearloop.app.data.Photos
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.ScoreFactor
import com.gearloop.app.data.TrustTier
import com.gearloop.app.data.VerificationStep
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Member 3 — Evidence & Trust.
 *
 * S-13 Handover hub · S-14 Handshake · S-15 Condition capture · S-15b Damage
 * marking · S-16 Sign & release · S-17 Active rental · S-18 Return & settlement ·
 * S-18b Evidence diff · S-32 Trust profile · S-37 Public profile · S-41
 * Verification.
 *
 * The evidence chain is the product's core promise, so it is stored as records
 * rather than pictures: each frame knows its booking, its angle, which side of
 * the rental it was taken on, when, and where. S-18 pairs the two sides by
 * angle, a difference is a damage mark recorded at return, and a claim is raised
 * from that mark — so S-34 and S-40 argue about the same record S-15 made.
 */
class EvidenceRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val money: MoneyRepository,
    private val scope: CoroutineScope,
) {
    private val dao = db.evidence()

    /** One read-modify-write of a booking at a time (see ExchangeRepository, D-14). */
    private val edits = Mutex()

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    /** The seven angles, named once — S-25's photo slots ask for the same ones. */
    val angles: List<String> = Demo.captureAngles.map { it.label }

    // ============================================================ the booking

    /**
     * The booking the handover screens are about: the one selected, else the
     * renter's live rental, else their next confirmed one.
     */
    val booking: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        val mine = list.filter { it.renter.handle == Seed.ME && it.status != BookingStatus.DRAFT }
        mine.firstOrNull { it.ref == s.bookingRef }
            ?: mine.filter { it.status == BookingStatus.ACTIVE || it.status == BookingStatus.RETURNED }.minByOrNull { it.start }
            ?: mine.filter { it.status == BookingStatus.CONFIRMED }.minByOrNull { it.start }
    }.hot(null)

    val phase: StateFlow<Phase> = session.session.map { it.capturePhase }.hot(Phase.PICKUP)

    val frames: StateFlow<Map<Phase, Map<String, CaptureFrame>>> = combine(booking, store.captureRows) { b, rows ->
        if (b == null) emptyMap()
        else rows.filter { it.bookingRef == b.ref }
            .groupBy { it.phase }
            .mapValues { (_, list) -> list.associate { it.angle to it.toFrame() } }
    }.hot(emptyMap())

    val marks: StateFlow<List<Mark>> = combine(booking, store.markRows) { b, rows ->
        if (b == null) emptyList() else rows.filter { it.bookingRef == b.ref }.map { it.toMark() }
    }.hot(emptyList())

    /** S-18: the same angle at pickup and at return, and the mark that changed it. */
    val pairs: StateFlow<List<AnglePair>> = combine(frames, marks) { f, m ->
        angles.map { angle ->
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

    // ============================================================ S-13 handover hub

    /** Update: tick or untick an item on the "before you go" checklist. */
    suspend fun toggleChecklist(ref: String, index: Int) = editBooking(ref) { row ->
        row.copy(checklist = (if (index in row.checklist) row.checklist - index else row.checklist + index).sorted())
    }

    // ============================================================ S-14 handshake

    /** Update: the renter has scanned the tag on the gear. */
    suspend fun renterScanned(ref: String) = editBooking(ref) { it.copy(handshakeRenterAt = DemoClock.nowMillis()) }

    /**
     * Update: the owner has scanned the renter's code. On a real deployment that
     * arrives from the owner's phone; on one device it is simulated a moment
     * after the renter's scan, and logged as a deviation. Like [ownerSigns], it
     * answers the renter's half only, so a scan that lands after a reset is a no-op
     * (D-20: since then it can outlive the screen that started it).
     */
    suspend fun ownerScanned(ref: String) = editBooking(ref) {
        if (it.handshakeRenterAt == null) it else it.copy(handshakeOwnerAt = DemoClock.nowMillis())
    }

    /** Update: the fallback — both sides confirmed by the six-digit code. */
    suspend fun enterCode(ref: String, code: String): Boolean {
        val row = db.money().booking(ref) ?: return false
        if (code.filter(Char::isDigit) != row.handoverCode) return false
        val now = DemoClock.nowMillis()
        db.money().upsertBooking(row.copy(handshakeRenterAt = now, handshakeOwnerAt = now, updatedAt = now))
        return true
    }

    // ============================================================ S-15 capture

    private val pickupPoint = 9.7134 to 80.0246
    private val returnPoint = 9.7131 to 80.0244

    /** Create (or replace, on a retake) the frame for one angle. */
    suspend fun capture(ref: String, phase: Phase, angle: String, ok: Boolean = true) {
        val existing = dao.capture(ref, phase, angle)
        val (lat, lon) = if (phase == Phase.PICKUP) pickupPoint else returnPoint
        dao.upsertCapture(
            CaptureEntity(
                id = existing?.id ?: 0,
                bookingRef = ref,
                phase = phase,
                angle = angle,
                photoKey = Photos.sampleFrame[angle] ?: "cond_1",
                ok = ok,
                capturedAt = DemoClock.nowMillis(),
                latitude = lat,
                longitude = lon,
            ),
        )
    }

    /** Update: the capture-time check on a frame — pass, or "too blurry, retake". */
    suspend fun setFrameOk(ref: String, phase: Phase, angle: String, ok: Boolean) {
        val existing = dao.capture(ref, phase, angle) ?: return
        dao.upsertCapture(existing.copy(ok = ok))
    }

    /** Delete a frame, so the angle can be shot again. */
    suspend fun deleteFrame(ref: String, phase: Phase, angle: String) = dao.deleteCapture(ref, phase, angle)

    // ============================================================ S-15b marks

    /** Create a damage mark. Cosmetic marks at pickup are what protect the renter later. */
    suspend fun addMark(
        ref: String, phase: Phase, area: String, severity: DamageMark.Severity, note: String, x: Float, y: Float,
    ): Long = dao.insertMark(
        DamageMarkEntity(
            bookingRef = ref,
            phase = phase,
            area = area,
            summary = summaryFor(severity),
            severity = severity,
            note = note.trim(),
            x = x,
            y = y,
            photoKey = Photos.sampleFrame[area] ?: "cond_1",
            createdAt = DemoClock.nowMillis(),
            resolved = false,
        ),
    )

    /** Update an existing mark. */
    suspend fun updateMark(id: Long, area: String, severity: DamageMark.Severity, note: String, x: Float, y: Float) {
        val row = dao.mark(id) ?: return
        dao.upsertMark(row.copy(area = area, severity = severity, summary = summaryFor(severity), note = note.trim(), x = x, y = y))
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
        DamageMark.Severity.FUNCTIONAL -> "Pressure ding"
        DamageMark.Severity.MAJOR -> "Structural damage"
    }

    // ============================================================ S-16 sign & release

    /** Update: the renter signs the condition record. */
    suspend fun signAsRenter(ref: String) = editBooking(ref) { it.copy(renterSignedAt = DemoClock.nowMillis()) }

    /**
     * Update: the owner's signature lands, and with both on the record the rental
     * starts. Simulated on one device, like the owner's scan.
     */
    suspend fun ownerSigns(ref: String) {
        val row = db.money().booking(ref) ?: return
        if (row.renterSignedAt == null) return
        val now = DemoClock.nowMillis()
        val item = store.item(row.listingId)
        db.withTransaction {
            db.money().upsertBooking(
                row.copy(
                    ownerSignedAt = now,
                    status = if (row.status == BookingStatus.CONFIRMED) BookingStatus.ACTIVE else row.status,
                    updatedAt = now,
                ),
            )
            money.post(row.ownerHandle, "Condition record signed by both · ${row.ref}", ChatMessage.Kind.SYSTEM, fromMe = false)
            money.notify(
                NotificationItem.Kind.BOOKING,
                "Rental started · ${item?.shortTitle ?: row.ref}",
                "Return by ${com.gearloop.app.data.dayTime(Seed.dayAt(LocalDate.ofEpochDay(row.endDay), row.returnTime))}",
                "s17",
                row.ref,
            )
        }
    }

    // ============================================================ S-17 active rental

    /** Update: tick an item on the return checklist. */
    suspend fun toggleReturnChecklist(ref: String, index: Int) = editBooking(ref) { row ->
        row.copy(returnChecklist = (if (index in row.returnChecklist) row.returnChecklist - index else row.returnChecklist + index).sorted())
    }

    sealed interface ExtendResult {
        data class Done(val newEnd: LocalDate, val charged: Int) : ExtendResult
        data object Unavailable : ExtendResult
        /** Already back with the owner, or never going out: nothing to extend. */
        data object Closed : ExtendResult
    }

    /**
     * Update: keep it a day longer — only if that day, and the buffer after it,
     * are free on this unit. The difference in price is charged there and then.
     *
     * Under [edits], because it reads the row and writes it back: two quick taps
     * used to read the same end date, extend once and charge twice. And only
     * while the rental can still run longer — a returned booking was being
     * extended, and charged for the day, from the S-17 it had already left (D-25).
     */
    suspend fun extend(ref: String, days: Int = 1): ExtendResult = edits.withLock {
        val row = db.money().booking(ref) ?: return@withLock ExtendResult.Unavailable
        if (row.status != BookingStatus.ACTIVE && row.status != BookingStatus.CONFIRMED) return@withLock ExtendResult.Closed
        val item = store.item(row.listingId) ?: return@withLock ExtendResult.Unavailable
        val start = LocalDate.ofEpochDay(row.startDay)
        val end = LocalDate.ofEpochDay(row.endDay)
        val extra = Availability.Span(end.plusDays(1), end.plusDays(days.toLong()))
        val ok = Availability.isRangeFree(
            extra, DemoClock.today, store.spansNow(row.listingId, row.unit, excluding = ref), store.blocksNow(row.listingId, row.unit),
        )
        if (!ok) return@withLock ExtendResult.Unavailable
        val newDays = (extra.end.toEpochDay() - start.toEpochDay() + 1).toInt()
        val quote = Pricing.quote(item, newDays, row.protection, row.coupon > 0, row.includeExtras)
        val charged = (quote.total - row.total).coerceAtLeast(0)
        val now = DemoClock.nowMillis()
        db.withTransaction {
            db.money().upsertBooking(
                row.copy(endDay = extra.end.toEpochDay(), total = quote.total, depositHold = quote.depositHold, ownerPayout = quote.ownerPayout, updatedAt = now),
            )
            if (charged > 0) db.money().insertTxn(WalletTxnEntity(label = "Extension · ${row.ref} +$days day", amount = -charged, createdAt = now))
            money.post(row.ownerHandle, "Rental extended to ${com.gearloop.app.data.dayMonth(extra.end)} · ${row.ref}", ChatMessage.Kind.SYSTEM, fromMe = false)
        }
        ExtendResult.Done(extra.end, charged)
    }

    // ============================================================ S-18 return

    /**
     * Update: the return frames are in. If nothing was marked as new, the owner's
     * check has nothing to find and the rental completes; if something was, it
     * waits on S-18b for the owner's decision.
     */
    suspend fun finishReturn(ref: String) {
        val row = db.money().booking(ref) ?: return
        val now = DemoClock.nowMillis()
        val openChanges = dao.marksFor(ref).any { it.phase == Phase.RETURN && !it.resolved }
        db.withTransaction {
            db.money().upsertBooking(
                row.copy(
                    status = if (openChanges) BookingStatus.RETURNED else BookingStatus.COMPLETED,
                    returnedAt = row.returnedAt ?: now,
                    updatedAt = now,
                ),
            )
            if (!openChanges) settle(row.ref)
        }
        session.selectCapturePhase(Phase.PICKUP)
    }

    private suspend fun settle(ref: String) {
        val row = db.money().booking(ref) ?: return
        val item = store.item(row.listingId)
        money.notify(
            NotificationItem.Kind.BOOKING,
            "Rental complete · ${item?.shortTitle ?: ref}",
            if (row.depositHold > 0) "Your ${lkr(row.depositHold)} hold has been released." else "No deposit was held. Nothing more to pay.",
            "s18",
            ref,
        )
        db.discovery().person(Seed.ME)?.let {
            db.discovery().upsertPerson(it.copy(completedRentals = it.completedRentals + 1))
        }
    }

    /** S-18b "It's the same" — Update: the difference is agreed not to be new. */
    suspend fun resolveChange(markId: Long) {
        val mark = dao.mark(markId) ?: return
        val now = DemoClock.nowMillis()
        db.withTransaction {
            dao.setMarkResolved(markId, true)
            val stillOpen = dao.marksFor(mark.bookingRef).any { it.phase == Phase.RETURN && !it.resolved && it.id != markId }
            val row = db.money().booking(mark.bookingRef)
            if (!stillOpen && row != null && row.status == BookingStatus.RETURNED) {
                db.money().upsertBooking(row.copy(status = BookingStatus.COMPLETED, updatedAt = now))
                settle(row.ref)
            }
        }
    }

    /**
     * S-18b "Raise a claim" — Create the claim from the return mark.
     *
     * Cosmetic marks cannot be claimed for on their own; the rubric says so on
     * S-15b and the adjudicator's console, and this refuses the same thing.
     */
    suspend fun raiseClaim(markId: Long): String? {
        val mark = dao.mark(markId) ?: return null
        if (mark.severity == DamageMark.Severity.COSMETIC) return null
        val row = db.money().booking(mark.bookingRef) ?: return null
        val item = store.item(row.listingId) ?: return null
        val value = if (item.id == Demo.torq.id) Demo.BOARD_VALUE else item.deposit * 2
        val cap = value * mark.severity.capPercent / 100
        val estimate = when {
            item.id == Demo.torq.id && mark.severity == DamageMark.Severity.FUNCTIONAL -> Demo.CLAIM_AMOUNT
            mark.severity == DamageMark.Severity.FUNCTIONAL -> value * 15 / 100
            else -> value * 60 / 100
        }.coerceAtMost(cap)
        val existing = db.money().claimFor(row.ref, mark.area)
        if (existing != null) {
            session.selectClaim(existing.ref)
            return existing.ref
        }
        val ref = nextClaimRef(db.money().claimRefs().toSet())
        val now = DemoClock.nowMillis()
        db.withTransaction {
            db.money().upsertClaim(
                ClaimEntity(
                    ref = ref,
                    bookingRef = row.ref,
                    listingTitle = item.shortTitle,
                    claimantHandle = row.ownerHandle,
                    respondentHandle = row.renterHandle,
                    amount = estimate,
                    itemValue = value,
                    severity = mark.severity,
                    area = mark.area,
                    note = mark.note.ifBlank { "Found on return; not in the pickup record." },
                    status = ClaimStatus.OPEN,
                    createdAt = now,
                    respondBy = now + Demo.CLAIM_RESPOND_DAYS * MoneyRepository.DAY_MS,
                    renterStatement = null,
                    renterEvidence = null,
                    award = null,
                    rationale = null,
                    decidedAt = null,
                    decidedBy = null,
                ),
            )
            money.notify(
                NotificationItem.Kind.SAFETY,
                "${item.owner.name.substringBefore(' ')} reported new damage",
                "${item.shortTitle} · ${mark.area} · respond within ${Demo.CLAIM_RESPOND_DAYS} days",
                "s34",
                ref,
            )
        }
        session.selectClaim(ref)
        return ref
    }

    private fun nextClaimRef(used: Set<String>): String {
        var n = 3081
        while ("CL-$n" in used) n++
        return "CL-$n"
    }

    // ============================================================ S-32 trust

    val verification: StateFlow<List<VerificationStep>> = store.verificationRows.map { rows ->
        rows.map { VerificationStep(it.title, it.unlocks, it.state) }
    }.hot(emptyList())

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
     * The score, recomputed from the things it says it is made of. It starts at
     * the designed 742 and moves when they do: 30 points per verification step,
     * 6 per completed rental, and a damage claim upheld against you costs 40.
     */
    val trust: StateFlow<Trust> = combine(store.peopleRows, store.verificationRows, store.claimRows) { people, steps, claims ->
        val me = people.firstOrNull { it.handle == Seed.ME }
        val verified = steps.count { it.state == VerificationStep.State.DONE }
        val completed = me?.completedRentals ?: 14
        val upheld = claims.count { it.respondentHandle == Seed.ME && (it.award ?: 0) > 0 }
        val score = (742 + 30 * (verified - 5) + 6 * (completed - 14) - 40 * upheld).coerceIn(0, 1000)
        val tier = when {
            score >= GOLD -> TrustTier.GOLD
            score >= 600 -> TrustTier.SILVER
            else -> TrustTier.BRONZE
        }
        val damageFree = if (completed == 0) 100 else ((completed - upheld) * 100 / completed)
        val rating = store.people.value[Seed.ME]?.ratingText ?: "4.8"
        Trust(
            score = score,
            tier = tier,
            pointsToGold = (GOLD - score).coerceAtLeast(0),
            progress = ((score - 600f) / (GOLD - 600f)).coerceIn(0f, 1f),
            factors = listOf(
                ScoreFactor("Verification", "$verified / 6", verified / 6f),
                ScoreFactor("Completed rentals", "$completed", (completed / 20f).coerceAtMost(1f)),
                ScoreFactor("On-time returns", "${me?.onTimeReturns ?: 100}%", (me?.onTimeReturns ?: 100) / 100f),
                ScoreFactor("Damage-free", "$damageFree%", damageFree / 100f),
                ScoreFactor("Response rate", "92%", 0.92f),
                ScoreFactor("Reviews", "★ $rating", (store.people.value[Seed.ME]?.rating ?: 4.8).toFloat() / 5f),
            ),
            verified = verified,
        )
    }.hot(Trust(742, TrustTier.SILVER, 58, 0.71f, Demo.scoreFactors, 5))

    /** Update: show or hide an earned badge on the public profile. */
    suspend fun toggleFeatured(badge: Badge) = dao.setBadgeFeatured(badge.name, !badge.featured)

    // ============================================================ S-41 verification

    /**
     * Update: complete the current step. The home address step stores the
     * address it verified, and the step after it — if there is one — opens.
     */
    suspend fun verifyCurrent(address: String?): Boolean {
        val steps = dao.verificationStepsNow()
        val current = steps.firstOrNull { it.state == VerificationStep.State.CURRENT } ?: return false
        if (current.title == VerificationStep.HOME_ADDRESS && address.isNullOrBlank()) return false
        val now = DemoClock.nowMillis()
        db.withTransaction {
            dao.upsertVerificationStep(current.copy(state = VerificationStep.State.DONE, completedAt = now))
            steps.firstOrNull { it.position > current.position && it.state == VerificationStep.State.LOCKED }
                ?.let { dao.upsertVerificationStep(it.copy(state = VerificationStep.State.CURRENT)) }
            if (!address.isNullOrBlank()) dao.setAddress(address.trim())
            db.discovery().person(Seed.ME)?.let { db.discovery().upsertPerson(it.copy(verifiedSteps = (it.verifiedSteps + 1).coerceAtMost(6))) }
        }
        return true
    }

    /** Delete: withdraw a verified address, which re-opens the step. */
    suspend fun removeAddress() {
        val steps = dao.verificationStepsNow()
        val step = steps.firstOrNull { it.title == VerificationStep.HOME_ADDRESS } ?: return
        if (step.state != VerificationStep.State.DONE) return
        db.withTransaction {
            dao.upsertVerificationStep(step.copy(state = VerificationStep.State.CURRENT, completedAt = null))
            dao.setAddress(null)
            db.discovery().person(Seed.ME)?.let { db.discovery().upsertPerson(it.copy(verifiedSteps = (it.verifiedSteps - 1).coerceAtLeast(0))) }
        }
    }

    val address: StateFlow<String?> = store.profileRow.map { it?.address }.hot(null)

    // ============================================================ helpers

    private suspend fun editBooking(ref: String, change: (com.gearloop.app.data.db.BookingEntity) -> com.gearloop.app.data.db.BookingEntity) = edits.withLock {
        val row = db.money().booking(ref) ?: return@withLock
        db.money().upsertBooking(change(row).copy(updatedAt = DemoClock.nowMillis()))
    }

    private fun CaptureEntity.toFrame() = CaptureFrame(id, angle, Photos.res(photoKey), ok, DemoClock.fromMillis(capturedAt), latitude, longitude)

    private fun DamageMarkEntity.toMark() = Mark(
        id = id, phase = phase, area = area, summary = summary, severity = severity, note = note,
        x = x, y = y, photo = photoKey?.let(Photos::res), createdAt = DemoClock.fromMillis(createdAt), resolved = resolved,
    )

    companion object {
        const val GOLD = 800
    }
}
