package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DayState
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.Protection
import com.gearloop.app.data.Quote
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.db.BookingEntity
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.db.MessageEntity
import com.gearloop.app.data.db.NotificationEntity
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Member 2 — Money & Commitment.
 *
 * S-08 Dates · S-09 Protection · S-10 Waiver · S-11 Review & pay · S-12 Booked ·
 * S-31 Rentals · S-33 Messages · S-34 Claim · S-35 Wallet · S-38 Notifications ·
 * S-40 Admin console.
 *
 * A booking is built as a draft row — one per renter and listing — and the four
 * checkout screens each edit that row. Nothing is held in navigation arguments,
 * so a renter can leave on S-10, reopen the app tomorrow, and find their dates,
 * plan and signature where they left them.
 */
class MoneyRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val scope: CoroutineScope,
) {
    private val dao = db.money()

    /** One read-modify-write of the draft at a time (see ExchangeRepository, D-14). */
    private val edits = Mutex()

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    // ============================================================ the draft

    private fun draftRef(listingId: String) = "D-$listingId"

    /** The draft for the listing currently open on S-07. */
    val draft: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        list.firstOrNull { it.ref == draftRef(s.listingId) && it.status == BookingStatus.DRAFT }
    }.hot(null)

    fun quoteFor(booking: Booking): Quote = booking.quote

    /** S-07's dates row and footer, before a draft exists: three days from the next Saturday. */
    fun defaultSpan(item: GearItem): Availability.Span {
        val today = DemoClock.today
        val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        return (1..item.units).firstNotNullOfOrNull { u ->
            Availability.firstFreeRun(saturday, 3, today, store.spansFor(item.id, u), store.blockedFor(item.id, u))
        } ?: Availability.Span(saturday, saturday.plusDays(2))
    }

    /** S-07 "Book" — Create the draft, or carry on with the one already started. */
    suspend fun startBooking(listingId: String): String {
        session.selectListing(listingId)
        val ref = draftRef(listingId)
        if (dao.booking(ref) != null) return ref
        val item = store.item(listingId) ?: return ref
        val span = defaultSpan(item)
        val unit = unitFor(item, span, ref) ?: 1
        val couponEligible = db.discovery().profileNow()?.couponUsed == false
        val protection = Protection.PLUS
        val quote = Pricing.quote(item, Availability.daysIn(span), protection, couponEligible, includeExtras = true)
        val now = DemoClock.nowMillis()
        dao.upsertBooking(
            BookingEntity(
                ref = ref, listingId = listingId, unit = unit,
                renterHandle = Seed.ME, ownerHandle = item.owner.handle,
                startDay = span.start.toEpochDay(), endDay = span.end.toEpochDay(),
                protection = protection, includeExtras = true, coupon = quote.coupon, paymentMethod = 0,
                waiverName = null, waiverAt = null, status = BookingStatus.DRAFT,
                total = quote.total, depositHold = quote.depositHold, ownerPayout = quote.ownerPayout,
                handoverCode = Seed.handoverCode(ref), pickupTime = "09:00", returnTime = "17:00",
                respondBy = null, checklist = emptyList(), returnChecklist = emptyList(),
                handshakeRenterAt = null, handshakeOwnerAt = null, renterSignedAt = null, ownerSignedAt = null,
                returnedAt = null, rating = null, calendarAdded = false, hiddenFromHistory = false,
                createdAt = now, updatedAt = now,
            ),
        )
        return ref
    }

    private suspend fun editDraft(change: (BookingEntity, GearItem) -> BookingEntity) = edits.withLock {
        val ref = draftRef(session.listingId)
        val row = dao.booking(ref) ?: return@withLock
        val item = store.item(row.listingId) ?: return@withLock
        val next = change(row, item)
        val days = (next.endDay - next.startDay + 1).toInt()
        val quote = Pricing.quote(item, days, next.protection, next.coupon > 0, next.includeExtras)
        dao.upsertBooking(
            next.copy(total = quote.total, depositHold = quote.depositHold, ownerPayout = quote.ownerPayout, updatedAt = DemoClock.nowMillis()),
        )
    }

    /** The first unit that can take the whole span, or null if none can. Read from the tables. */
    private suspend fun unitFor(item: GearItem, span: Availability.Span, excluding: String = ""): Int? =
        (1..item.units).firstOrNull { u ->
            Availability.isRangeFree(span, DemoClock.today, store.spansNow(item.id, u, excluding), store.blocksNow(item.id, u))
        }

    /**
     * Two month grids for S-08, every cell's state worked out from real bookings
     * and blocks. A listing with several units shows a day as free if any unit
     * is free that day.
     */
    fun monthsFor(booking: Booking, count: Int = 2): List<Pair<YearMonth, List<DayCell>>> {
        val today = DemoClock.today
        val item = booking.item
        val perUnit = (1..item.units).map { u ->
            store.spansFor(item.id, u, excluding = booking.ref) to store.blockedFor(item.id, u)
        }
        val first = YearMonth.from(today)
        return (0 until count).map { m ->
            val month = first.plusMonths(m.toLong())
            month to (1..month.lengthOfMonth()).map { d ->
                val day = month.atDay(d)
                val states = perUnit.map { (spans, blocks) -> Availability.stateOf(day, today, spans, blocks, booking.span) }
                DayCell(day, merge(states))
            }
        }
    }

    private fun merge(states: List<DayState>): DayState = when {
        states.any { it == DayState.DISABLED } -> DayState.DISABLED
        states.any { it == DayState.SELECTED } -> DayState.SELECTED
        states.any { it == DayState.IN_RANGE } -> DayState.IN_RANGE
        states.any { it == DayState.AVAILABLE } -> DayState.AVAILABLE
        states.any { it == DayState.TODAY } -> DayState.TODAY
        states.any { it == DayState.BUFFER } -> DayState.BUFFER
        states.all { it == DayState.BLOCKED } -> DayState.BLOCKED
        else -> DayState.BOOKED
    }

    /** S-08 — Update the draft's dates from a tap. Booked and blocked days are ignored. */
    suspend fun tapDay(day: LocalDate) {
        val booking = draft.value ?: return
        val item = booking.item
        val today = DemoClock.today
        val current = booking.span
        val candidates = (1..item.units).map { u ->
            Availability.extend(current, day, today, store.spansFor(item.id, u, booking.ref), store.blockedFor(item.id, u)) to u
        }
        val best = candidates.firstOrNull { (s, _) -> s != null && s.start != s.end && s.start == current.start && s != current }
            ?: candidates.firstOrNull { (s, _) -> s != null && s != current }
            ?: return
        val (span, unit) = best
        editDraft { row, _ -> row.copy(startDay = span!!.start.toEpochDay(), endDay = span.end.toEpochDay(), unit = unit) }
    }

    /** S-09 — Update. */
    suspend fun setProtection(protection: Protection) = editDraft { row, _ -> row.copy(protection = protection) }

    /** S-10 — Update: the waiver accepted, under the name the renter typed. */
    suspend fun signWaiver(name: String) =
        editDraft { row, _ -> row.copy(waiverName = name.trim(), waiverAt = DemoClock.nowMillis()) }

    suspend fun clearWaiver() = editDraft { row, _ -> row.copy(waiverName = null, waiverAt = null) }

    /** S-11 — Update. */
    suspend fun setPaymentMethod(method: Int) = editDraft { row, _ -> row.copy(paymentMethod = method) }

    suspend fun setExtras(include: Boolean) = editDraft { row, _ -> row.copy(includeExtras = include) }

    /** S-11 — Delete the draft. */
    suspend fun discardDraft() {
        dao.deleteBooking(draftRef(session.listingId))
    }

    /**
     * S-11 "Confirm and pay" — Create the booking.
     *
     * One transaction, because a booking without its payment, or a payment
     * without its booking, is the inconsistency the whole app is built to avoid.
     * Instant-book items confirm on the spot; anything else goes to the owner as
     * a request with 24 hours to answer, which is what puts it in S-28's inbox.
     */
    suspend fun confirmDraft(): String? {
        val draftRow = dao.booking(draftRef(session.listingId)) ?: return null
        val item = store.item(draftRow.listingId) ?: return null
        val span = Availability.Span(LocalDate.ofEpochDay(draftRow.startDay), LocalDate.ofEpochDay(draftRow.endDay))
        val unit = unitFor(item, span, excluding = draftRow.ref) ?: return null
        val ref = nextBookingRef()
        val now = DemoClock.nowMillis()
        val status = if (item.instantBook) BookingStatus.CONFIRMED else BookingStatus.REQUESTED
        val days = Availability.daysIn(span)
        val quote = Pricing.quote(item, days, draftRow.protection, draftRow.coupon > 0, draftRow.includeExtras)
        val owner = item.owner

        db.withTransaction {
            dao.upsertBooking(
                draftRow.copy(
                    ref = ref,
                    unit = unit,
                    status = status,
                    total = quote.total,
                    depositHold = quote.depositHold,
                    ownerPayout = quote.ownerPayout,
                    handoverCode = Seed.handoverCode(ref),
                    respondBy = if (status == BookingStatus.REQUESTED) now + DAY_MS else null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            dao.deleteBooking(draftRow.ref)
            if (draftRow.coupon > 0) {
                db.discovery().profileNow()?.let { db.discovery().upsertProfile(it.copy(couponUsed = true)) }
            }
            dao.insertTxn(WalletTxnEntity(label = "Booking $ref · ${item.shortTitle}", amount = -quote.total, createdAt = now))
            val first = owner.name.substringBefore(' ')
            if (status == BookingStatus.CONFIRMED) {
                notify(NotificationItem.Kind.BOOKING, "$first confirmed your booking", "${item.title} · ${com.gearloop.app.data.dateRange(span.start, span.end)} · $ref", "s12", ref)
                post(owner.handle, "Booking confirmed · $ref", ChatMessage.Kind.SYSTEM, fromMe = false)
                // The address is released the moment payment completes (FR-15).
                post(owner.handle, "Pickup point · ${pickupAddress(owner.handle)}", ChatMessage.Kind.LOCATION, fromMe = false)
            } else {
                notify(NotificationItem.Kind.BOOKING, "Request sent to $first", "${item.title} · ${com.gearloop.app.data.dateRange(span.start, span.end)} · $first has 24 h to reply", "s31", ref)
                post(owner.handle, "Booking requested · $ref", ChatMessage.Kind.SYSTEM, fromMe = false)
            }
        }
        session.selectBooking(ref)
        return ref
    }

    private suspend fun nextBookingRef(): String {
        while (true) {
            val n = session.takeBookingSeq()
            val candidate = Seed.bookingRefs.getOrNull(n) ?: generatedRef(n)
            if (dao.bookingExists(candidate) == 0) return candidate
        }
    }

    private fun generatedRef(n: Int): String {
        val alphabet = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        var x = (n.toLong() * 7_919 + 104_729) % 28_629_151L
        val sb = StringBuilder()
        repeat(5) { sb.append(alphabet[(x % alphabet.length).toInt()]); x /= alphabet.length }
        return "GL-$sb"
    }

    // ============================================================ bookings

    /** The booking S-12 and the handover screens are about. */
    val current: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        list.firstOrNull { it.ref == s.bookingRef && it.status != BookingStatus.DRAFT }
    }.hot(null)

    private val mine: StateFlow<List<Booking>> = store.bookings
        .map { list -> list.filter { it.renter.handle == Seed.ME && it.status != BookingStatus.DRAFT } }
        .hot(emptyList())

    val active: StateFlow<List<Booking>> = mine
        .map { list -> list.filter { it.status == BookingStatus.ACTIVE || it.status == BookingStatus.RETURNED } }
        .hot(emptyList())

    val upcoming: StateFlow<List<Booking>> = mine
        .map { list -> list.filter { it.status == BookingStatus.REQUESTED || it.status == BookingStatus.CONFIRMED }.sortedBy { it.start } }
        .hot(emptyList())

    private val hidden: StateFlow<Set<String>> = store.bookingRows
        .map { rows -> rows.filter { it.hiddenFromHistory }.map { it.ref }.toSet() }
        .hot(emptySet())

    val past: StateFlow<List<Booking>> = combine(mine, hidden) { list, gone ->
        list.filter { it.status in setOf(BookingStatus.COMPLETED, BookingStatus.DECLINED, BookingStatus.CANCELLED) && it.ref !in gone }
            .sortedByDescending { it.start }
    }.hot(emptyList())

    fun selectBooking(ref: String) = session.selectBooking(ref)

    /** S-12, S-31 — Update: cancel, refunding by the cancellation rule. */
    suspend fun cancel(ref: String): Int {
        val row = dao.booking(ref) ?: return 0
        if (row.status !in setOf(BookingStatus.REQUESTED, BookingStatus.CONFIRMED)) return 0
        val refund = refundFor(row)
        val now = DemoClock.nowMillis()
        val item = store.item(row.listingId)
        db.withTransaction {
            dao.upsertBooking(row.copy(status = BookingStatus.CANCELLED, updatedAt = now))
            if (refund > 0) dao.insertTxn(WalletTxnEntity(label = "Refund · $ref cancelled", amount = refund, createdAt = now))
            notify(NotificationItem.Kind.BOOKING, "Booking $ref cancelled", "${item?.title ?: "Your booking"} · ${lkr(refund)} refunded", "s31", ref)
            post(row.ownerHandle, "Booking cancelled · $ref", ChatMessage.Kind.SYSTEM, fromMe = false)
        }
        return refund
    }

    /**
     * Free within 24 hours of booking, or more than three days before pickup;
     * half until a day before; nothing after that. The same sentence is printed
     * on S-11 from [cancellationRule].
     */
    fun refundFor(row: BookingEntity): Int {
        val now = DemoClock.nowMillis()
        val pickup = DemoClock.toMillis(Seed.dayAt(LocalDate.ofEpochDay(row.startDay), row.pickupTime))
        return when {
            now - row.createdAt <= DAY_MS || pickup - now >= 3 * DAY_MS -> row.total
            pickup - now >= DAY_MS -> row.total / 2
            else -> 0
        }
    }

    val cancellationRule: String get() = CANCELLATION_RULE

    /** The same rule over the domain booking, for quoting the refund before cancelling. */
    fun refundFor(booking: Booking): Int {
        val now = DemoClock.nowMillis()
        val pickup = DemoClock.toMillis(booking.pickupAt)
        val created = DemoClock.toMillis(booking.createdAt)
        return when {
            now - created <= DAY_MS || pickup - now >= 3 * DAY_MS -> booking.total
            pickup - now >= DAY_MS -> booking.total / 2
            else -> 0
        }
    }

    /** S-31 — the user's "remove from history". Restorable, so the toast can offer undo. */
    suspend fun setHidden(ref: String, hide: Boolean) {
        val row = dao.booking(ref) ?: return
        dao.upsertBooking(row.copy(hiddenFromHistory = hide, updatedAt = DemoClock.nowMillis()))
    }

    /** S-12 — Update: the booking has been written to the phone's calendar. */
    suspend fun markAddedToCalendar(ref: String) {
        val row = dao.booking(ref) ?: return
        dao.upsertBooking(row.copy(calendarAdded = true, updatedAt = DemoClock.nowMillis()))
    }

    // ============================================================ wallet

    val walletAvailable: StateFlow<Int> = store.profileRow.map { it?.walletAvailable ?: 0 }.hot(0)

    val holds: StateFlow<List<Hold>> = mine.map { list ->
        list.filter { it.depositHold > 0 && it.status in setOf(BookingStatus.REQUESTED, BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.RETURNED) }
            .map { Hold(it, it.end.plusDays(1)) }
    }.hot(emptyList())

    /** Live bookings with nothing held, and the plan that is why. */
    val coveredNotHeld: StateFlow<List<Booking>> = mine.map { list ->
        list.filter { it.depositHold == 0 && it.protection != Protection.BASIC && it.status in setOf(BookingStatus.CONFIRMED, BookingStatus.ACTIVE) }
    }.hot(emptyList())

    val transactions: StateFlow<List<WalletLine>> = store.txnRows.map { rows ->
        rows.map { WalletLine(it.label, dayMonth(DemoClock.fromMillis(it.createdAt).toLocalDate()), it.amount) }
    }.hot(emptyList())

    /** S-35 — Create a withdrawal and empty the available balance. */
    suspend fun withdraw(): Int = db.withTransaction {
        val amount = db.discovery().profileNow()?.walletAvailable ?: 0
        if (amount > 0) {
            dao.insertTxn(WalletTxnEntity(label = "Withdrawal to BOC ••••8812", amount = -amount, createdAt = DemoClock.nowMillis()))
            dao.setWalletAvailable(0)
        }
        amount
    }

    /** S-35 — Create a top-up. */
    suspend fun topUp(amount: Int) {
        if (amount <= 0) return
        db.withTransaction {
            val available = db.discovery().profileNow()?.walletAvailable ?: 0
            dao.insertTxn(WalletTxnEntity(label = "Top up from Visa ••••4471", amount = amount, createdAt = DemoClock.nowMillis()))
            dao.setWalletAvailable(available + amount)
        }
    }

    // ============================================================ notifications

    val notifications: StateFlow<List<Notification>> = store.notifications

    suspend fun markRead(id: Long) = dao.markRead(id)
    suspend fun markAllRead() = dao.markAllRead()

    suspend fun deleteNotification(id: Long): NotificationEntity? {
        val row = dao.notification(id) ?: return null
        dao.deleteNotification(id)
        return row
    }

    suspend fun restoreNotification(row: NotificationEntity) {
        dao.insertNotification(row.copy(id = 0))
    }

    internal suspend fun notify(kind: NotificationItem.Kind, title: String, body: String, route: String, ref: String?) {
        dao.insertNotification(
            NotificationEntity(kind = kind, title = title, body = body, unread = true, createdAt = DemoClock.nowMillis(), route = route, ref = ref),
        )
    }

    // ============================================================ messages

    val threadId: StateFlow<String> = session.session.map { it.threadId }.hot(Seed.THREAD_OWNER)

    val thread: StateFlow<List<Message>> = combine(session.session, store.messages) { s, list ->
        list.filter { it.threadId == s.threadId }
    }.hot(emptyList())

    /** The latest live booking with whoever the thread is with, for its header card. */
    val threadBooking: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        list.filter { it.owner.handle == s.threadId && it.renter.handle == Seed.ME && it.status != BookingStatus.DRAFT }
            .maxByOrNull { it.createdAt }
    }.hot(null)

    /**
     * Everyone the renter has a conversation with, most recent first. The tab
     * bar's Messages opens one thread, and this is how it reaches the others —
     * before it, a thread opened from a profile could only be left by going
     * back to the screen that opened it. Swap negotiations, keyed by proposal
     * reference rather than by a person, stay on S-22. (D-29, DV-33)
     */
    val threads: StateFlow<List<String>> = combine(session.session, store.messages, store.people) { s, list, people ->
        val latest = list.filter { it.threadId in people }.groupBy { it.threadId }.mapValues { (_, m) -> m.maxOf { it.createdAt } }
        (latest.keys + s.threadId).filter { it in people && it != Seed.ME }.distinct().sortedByDescending { latest[it] }
    }.hot(emptyList())

    fun openThread(handle: String) = session.selectThread(handle)

    /** S-33 — Create. */
    suspend fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        post(session.threadId, body, ChatMessage.Kind.TEXT, fromMe = true)
    }

    /** S-33 — Delete one of your own messages. Returns it so it can be restored. */
    suspend fun deleteMessage(id: Long): MessageEntity? {
        val row = dao.message(id) ?: return null
        if (!row.fromMe || row.kind != ChatMessage.Kind.TEXT) return null
        dao.deleteMessage(id)
        return row
    }

    suspend fun restoreMessage(row: MessageEntity) {
        dao.insertMessage(row.copy(id = 0))
    }

    internal suspend fun post(threadId: String, body: String, kind: ChatMessage.Kind, fromMe: Boolean) {
        dao.insertMessage(MessageEntity(threadId = threadId, body = body, fromMe = fromMe, kind = kind, createdAt = DemoClock.nowMillis()))
    }

    // ============================================================ claims

    val claims: StateFlow<List<Claim>> = store.claims

    /** The claim S-34 shows: the selected one, else the newest against the renter. */
    val currentClaim: StateFlow<Claim?> = combine(session.session, store.claims) { s, list ->
        list.firstOrNull { it.ref == s.claimRef }
            ?: list.filter { it.respondent.handle == Seed.ME }.maxByOrNull { it.createdAt }
    }.hot(null)

    /** The adjudicator's queue on S-40: undecided first, newest first. */
    val queue: StateFlow<List<Claim>> = store.claims.map { list ->
        list.sortedWith(compareBy<Claim>({ it.decided }, { -DemoClock.toMillis(it.createdAt) }))
    }.hot(emptyList())

    fun selectClaim(ref: String) = session.selectClaim(ref)

    /** The booking a claim is about, for its protection plan. */
    fun bookingFor(claim: Claim): Booking? = store.booking(claim.bookingRef)

    /**
     * The two frames a claim is argued over: the claimed angle at pickup and at
     * return, read from the condition record the handover made. Claims with no
     * record behind them (the seeded queue) fall back to the dataset's photos.
     */
    fun evidenceFor(claim: Claim): Pair<Int, Int> {
        val rows = store.captureRows.value.filter { it.bookingRef == claim.bookingRef && it.angle == claim.area }
        val pickup = rows.firstOrNull { it.phase == com.gearloop.app.data.Phase.PICKUP }?.photoKey
        val back = rows.firstOrNull { it.phase == com.gearloop.app.data.Phase.RETURN }?.photoKey
        return com.gearloop.app.data.Photos.res(pickup ?: "cond_1") to com.gearloop.app.data.Photos.res(back ?: "cond_2")
    }

    /** What the renter actually pays on an award: capped by their plan (FR-05). */
    fun renterShare(claim: Claim, award: Int): Int {
        val booking = bookingFor(claim) ?: return award
        val cap = booking.protection.mostYouCouldLose(booking.item.deposit)
        return minOf(award, cap)
    }

    /** The same rule, read straight from the tables, for the write paths. */
    private suspend fun renterShare(bookingRef: String?, award: Int): Int {
        val booking = bookingRef?.let { dao.booking(it) } ?: return award
        val deposit = db.discovery().listing(booking.listingId)?.deposit ?: 0
        return minOf(award, booking.protection.mostYouCouldLose(deposit))
    }

    /** S-34 — Update: contest, with the renter's side in their words. */
    suspend fun contest(ref: String, statement: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED || row.status == ClaimStatus.ACCEPTED) return
        dao.upsertClaim(row.copy(status = ClaimStatus.CONTESTED, renterStatement = statement.trim().ifEmpty { "I don't accept this claim." }))
    }

    /** S-34 — Update: evidence added to the claim file. */
    suspend fun addEvidence(ref: String, note: String) {
        val row = dao.claim(ref) ?: return
        val status = if (row.status == ClaimStatus.MORE_EVIDENCE || row.status == ClaimStatus.OPEN) ClaimStatus.CONTESTED else row.status
        dao.upsertClaim(row.copy(renterEvidence = note.trim(), status = status))
    }

    /** S-34 — Update: liability accepted, settled at the renter's share. */
    suspend fun acceptLiability(ref: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED || row.status == ClaimStatus.ACCEPTED) return
        val share = renterShare(row.bookingRef, row.amount)
        db.withTransaction {
            dao.upsertClaim(row.copy(status = ClaimStatus.ACCEPTED, award = row.amount, decidedAt = DemoClock.nowMillis(), decidedBy = "Renter"))
            if (share > 0) dao.insertTxn(WalletTxnEntity(label = "Claim $ref settled", amount = -share, createdAt = DemoClock.nowMillis()))
        }
    }

    /**
     * S-40 — Update: issue a ruling. Refused without a stated reason (FR-19), and
     * the award can never exceed the cap the severity sets.
     */
    suspend fun issueRuling(ref: String, award: Int, rationale: String): Boolean {
        val row = dao.claim(ref) ?: return false
        if (rationale.isBlank() || row.status == ClaimStatus.DECIDED) return false
        val cap = row.itemValue * row.severity.capPercent / 100
        val capped = award.coerceIn(0, minOf(row.amount, cap))
        val now = DemoClock.nowMillis()
        db.withTransaction {
            dao.upsertClaim(
                row.copy(status = ClaimStatus.DECIDED, award = capped, rationale = rationale.trim(), decidedAt = now, decidedBy = com.gearloop.app.data.Demo.ADJUDICATOR),
            )
            if (row.respondentHandle == Seed.ME) {
                val share = renterShare(row.bookingRef, capped)
                if (share > 0) dao.insertTxn(WalletTxnEntity(label = "Claim $ref ruling", amount = -share, createdAt = now))
                notify(NotificationItem.Kind.SAFETY, "Ruling on claim $ref", "Awarded ${lkr(capped)}. You pay ${lkr(share)}. The reason is on the claim.", "s34", ref)
            }
        }
        return true
    }

    /** S-40 — Update: ask the renter for more. */
    suspend fun requestMoreEvidence(ref: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED) return
        db.withTransaction {
            dao.setClaimStatus(ref, ClaimStatus.MORE_EVIDENCE)
            if (row.respondentHandle == Seed.ME) {
                notify(NotificationItem.Kind.SAFETY, "More evidence needed · $ref", "The claims desk has asked for anything else you have.", "s34", ref)
            }
        }
    }

    /** S-40 — Update. */
    suspend fun escalate(ref: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED) return
        dao.setClaimStatus(ref, ClaimStatus.ESCALATED)
    }

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000

        /** The rule [refundFor] applies, in the words S-07 and S-11 print. */
        const val CANCELLATION_RULE =
            "Free cancellation for 24 h after booking, or up to 3 days before pickup. " +
                "After that, 50% refund up to 24 h before pickup."

        private val addresses = mapOf(
            Seed.OWNER to "27 Temple Rd, Nallur",
            "@ayesha_f" to "14 Lagoon Rd, Ariyalai",
            "@thusara_p" to "3 Station Rd, Kokuvil",
            "@dinesh_k" to "8 Church Ln, Gurunagar",
            "@m_rizwan" to "5 Main St, Chundikuli",
            Seed.ME to "11 Stanley Rd, Jaffna",
        )

        fun pickupAddress(handle: String): String = addresses[handle] ?: "Jaffna"
    }
}
