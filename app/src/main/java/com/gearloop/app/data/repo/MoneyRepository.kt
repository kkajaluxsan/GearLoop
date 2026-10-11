package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DayState
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.Protection
import com.gearloop.app.data.Quote
import com.gearloop.app.data.Validate
import com.gearloop.app.data.dateRange
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
 * Money & Commitment: dates, protection, the waiver, payment, the booking, the
 * member's rentals, messages, claims, the wallet and notifications.
 *
 * A booking is built as a draft row — one per member and listing — and the four
 * checkout screens each edit that row. Nothing is held in navigation arguments,
 * so a renter can leave on the waiver, reopen the app tomorrow, and find their
 * dates, plan and signature where they left them.
 */
class MoneyRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val scope: CoroutineScope,
) {
    private val dao = db.money()
    private val me: String get() = session.handle

    /** One read-modify-write of the draft at a time (D-14). */
    private val edits = Mutex()

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    // ============================================================ the draft

    private fun draftRef(listingId: String, handle: String = me) = "D-$listingId$handle"

    /** The draft for the listing currently open. */
    val draft: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        val ref = draftRef(s.listingId, s.currentHandle.orEmpty())
        list.firstOrNull { it.ref == ref && it.status == BookingStatus.DRAFT }
    }.hot(null)

    fun quoteFor(booking: Booking): Quote = booking.quote

    /** The dates a new booking starts with: three days from the next Saturday a unit is free. */
    fun defaultSpan(item: GearItem): Availability.Span {
        val today = AppClock.today
        val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        return (1..item.units).firstNotNullOfOrNull { u ->
            Availability.firstFreeRun(saturday, 3, today, store.spansFor(item.id, u), store.blockedFor(item.id, u))
        } ?: Availability.Span(saturday, saturday.plusDays(2))
    }

    /** Pickup at nine, or — for a booking that starts today — at the next whole hour. */
    private fun pickupTimeFor(start: LocalDate): String {
        val now = AppClock.now()
        if (start != now.toLocalDate() || now.hour < 8) return "09:00"
        val hour = (now.hour + 1).coerceAtMost(21)
        return "%02d:00".format(hour)
    }

    /** "Book" — Create the draft, or carry on with the one already started. */
    suspend fun startBooking(listingId: String): String {
        session.selectListing(listingId)
        val ref = draftRef(listingId)
        if (dao.booking(ref) != null) return ref
        val item = store.item(listingId) ?: return ref
        val span = defaultSpan(item)
        val unit = unitFor(item, span, ref) ?: 1
        val profile = db.discovery().profileNow(me)
        val couponEligible = profile?.couponUsed == false
        val protection = Protection.PLUS
        val quote = Pricing.quote(item, Availability.daysIn(span), protection, couponEligible, includeExtras = true)
        val now = AppClock.nowMillis()
        dao.upsertBooking(
            BookingEntity(
                ref = ref, listingId = listingId, unit = unit,
                renterHandle = me, ownerHandle = item.owner.handle,
                startDay = span.start.toEpochDay(), endDay = span.end.toEpochDay(),
                protection = protection, includeExtras = true, coupon = quote.coupon,
                paymentMethod = if (profile?.cardLast4 != null) PAY_CARD else PAY_WALLET,
                waiverName = null, waiverAt = null, status = BookingStatus.DRAFT,
                total = quote.total, depositHold = quote.depositHold, ownerPayout = quote.ownerPayout,
                handoverCode = Seed.handoverCode(ref), pickupTime = pickupTimeFor(span.start), returnTime = "17:00",
                respondBy = null, checklist = emptyList(), returnChecklist = emptyList(),
                handshakeRenterAt = null, handshakeOwnerAt = null, renterSignedAt = null, ownerSignedAt = null,
                renterSignature = null, ownerSignature = null,
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
            next.copy(total = quote.total, depositHold = quote.depositHold, ownerPayout = quote.ownerPayout, updatedAt = AppClock.nowMillis()),
        )
    }

    /** The first unit that can take the whole span, or null if none can. Read from the tables. */
    private suspend fun unitFor(item: GearItem, span: Availability.Span, excluding: String = ""): Int? =
        (1..item.units).firstOrNull { u ->
            Availability.isRangeFree(span, AppClock.today, store.spansNow(item.id, u, excluding), store.blocksNow(item.id, u))
        }

    /**
     * Two month grids for the date picker, every cell's state worked out from
     * real bookings and blocks. A listing with several units shows a day as free
     * if any unit is free that day.
     */
    fun monthsFor(booking: Booking, count: Int = 2): List<Pair<YearMonth, List<DayCell>>> {
        val today = AppClock.today
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

    /** Update the draft's dates from a tap. Booked and blocked days are ignored. */
    suspend fun tapDay(day: LocalDate) {
        val booking = draft.value ?: return
        val item = booking.item
        val today = AppClock.today
        val current = booking.span
        val candidates = (1..item.units).map { u ->
            Availability.extend(current, day, today, store.spansFor(item.id, u, booking.ref), store.blockedFor(item.id, u)) to u
        }
        val best = candidates.firstOrNull { (s, _) -> s != null && s.start != s.end && s.start == current.start && s != current }
            ?: candidates.firstOrNull { (s, _) -> s != null && s != current }
            ?: return
        val (span, unit) = best
        editDraft { row, _ ->
            row.copy(startDay = span!!.start.toEpochDay(), endDay = span.end.toEpochDay(), unit = unit, pickupTime = pickupTimeFor(span.start))
        }
    }

    suspend fun setProtection(protection: Protection) = editDraft { row, _ -> row.copy(protection = protection) }

    /** The waiver accepted, under the name the renter typed. */
    suspend fun signWaiver(name: String) =
        editDraft { row, _ -> row.copy(waiverName = name.trim(), waiverAt = AppClock.nowMillis()) }

    suspend fun clearWaiver() = editDraft { row, _ -> row.copy(waiverName = null, waiverAt = null) }

    suspend fun setPaymentMethod(method: Int) = editDraft { row, _ -> row.copy(paymentMethod = method.coerceIn(0, 2)) }

    suspend fun setExtras(include: Boolean) = editDraft { row, _ -> row.copy(includeExtras = include) }

    suspend fun discardDraft() {
        dao.deleteBooking(draftRef(session.listingId))
    }

    sealed interface ConfirmResult {
        data class Booked(val ref: String) : ConfirmResult
        data object DatesTaken : ConfirmResult
        data object NoCard : ConfirmResult
        data class LowBalance(val available: Int, val total: Int) : ConfirmResult
        data object Unavailable : ConfirmResult
    }

    /**
     * "Confirm and pay" — Create the booking.
     *
     * One transaction, because a booking without its payment, or a payment
     * without its booking, is the inconsistency the whole app is built to avoid.
     * Instant-book items confirm on the spot; anything else goes to the owner as
     * a request with 24 hours to answer, which is what puts it in their inbox.
     */
    suspend fun confirmDraft(): ConfirmResult {
        val draftRow = dao.booking(draftRef(session.listingId)) ?: return ConfirmResult.Unavailable
        val item = store.item(draftRow.listingId) ?: return ConfirmResult.Unavailable
        val span = Availability.Span(LocalDate.ofEpochDay(draftRow.startDay), LocalDate.ofEpochDay(draftRow.endDay))
        val unit = unitFor(item, span, excluding = draftRow.ref) ?: return ConfirmResult.DatesTaken
        val profile = db.discovery().profileNow(me)
        val days = Availability.daysIn(span)
        val quote = Pricing.quote(item, days, draftRow.protection, draftRow.coupon > 0, draftRow.includeExtras)
        when (draftRow.paymentMethod) {
            PAY_CARD -> if (profile?.cardLast4 == null) return ConfirmResult.NoCard
            PAY_WALLET -> {
                val available = profile?.walletAvailable ?: 0
                if (available < quote.total) return ConfirmResult.LowBalance(available, quote.total)
            }
        }
        val ref = nextBookingRef()
        val now = AppClock.nowMillis()
        val status = if (item.instantBook) BookingStatus.CONFIRMED else BookingStatus.REQUESTED
        val owner = item.owner
        val renterName = store.person(me).name.substringBefore(' ')
        val dates = dateRange(span.start, span.end)

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
            if (draftRow.coupon > 0 && profile != null) db.discovery().upsertProfile(profile.copy(couponUsed = true))
            charge(me, draftRow.paymentMethod, quote.total, "Booking $ref · ${item.shortTitle}", profile?.cardBrand, profile?.cardLast4)
            val first = owner.name.substringBefore(' ')
            if (status == BookingStatus.CONFIRMED) {
                notify(me, NotificationItem.Kind.BOOKING, "$first's ${item.shortTitle} is booked", "$dates · $ref", "s12", ref)
                notify(owner.handle, NotificationItem.Kind.BOOKING, "New booking from $renterName", "${item.shortTitle} · $dates · $ref", "s28", ref)
                postDm(me, owner.handle, SYSTEM, "Booking confirmed · $ref", ChatMessage.Kind.SYSTEM)
                // The address is released the moment payment completes (FR-15).
                postDm(me, owner.handle, owner.handle, "Pickup point · ${pickupAddress(owner.handle)}", ChatMessage.Kind.LOCATION)
            } else {
                notify(me, NotificationItem.Kind.BOOKING, "Request sent to $first", "${item.title} · $dates · $first has 24 h to reply", "s31", ref)
                notify(owner.handle, NotificationItem.Kind.BOOKING, "New request from $renterName", "${item.shortTitle} · $days ${if (days == 1) "day" else "days"} · respond within 24 h", "s28", ref)
                postDm(me, owner.handle, SYSTEM, "Booking requested · $ref", ChatMessage.Kind.SYSTEM)
            }
        }
        session.selectBooking(ref)
        return ConfirmResult.Booked(ref)
    }

    /** Take a payment: from the wallet balance, recorded against the card, or owed in cash. */
    internal suspend fun charge(handle: String, method: Int, amount: Int, label: String, brand: String?, last4: String?) {
        val now = AppClock.nowMillis()
        when (method) {
            PAY_WALLET -> {
                val available = db.discovery().profileNow(handle)?.walletAvailable ?: 0
                dao.setWalletAvailable(handle, (available - amount).coerceAtLeast(0))
                dao.insertTxn(WalletTxnEntity(handle = handle, label = "$label · wallet", amount = -amount, createdAt = now))
            }
            PAY_CASH -> dao.insertTxn(WalletTxnEntity(handle = handle, label = "$label · cash at pickup", amount = -amount, createdAt = now))
            else -> dao.insertTxn(WalletTxnEntity(handle = handle, label = "$label · ${brand ?: "Card"} ••••${last4.orEmpty()}", amount = -amount, createdAt = now))
        }
    }

    /** Give money back the way it came: into the wallet if it was paid from there. */
    private suspend fun refund(row: BookingEntity, amount: Int, label: String) {
        if (amount <= 0) return
        val now = AppClock.nowMillis()
        if (row.paymentMethod == PAY_WALLET) {
            val available = db.discovery().profileNow(row.renterHandle)?.walletAvailable ?: 0
            dao.setWalletAvailable(row.renterHandle, available + amount)
        }
        dao.insertTxn(WalletTxnEntity(handle = row.renterHandle, label = label, amount = amount, createdAt = now))
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

    /** The booking the confirmation and the handover screens are about. */
    val current: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        list.firstOrNull { it.ref == s.bookingRef && it.status != BookingStatus.DRAFT }
    }.hot(null)

    private val mine: StateFlow<List<Booking>> = combine(store.bookings, store.me) { list, h ->
        list.filter { it.renter.handle == h && it.status != BookingStatus.DRAFT }
    }.hot(emptyList())

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

    /** Cancel, refunding by the cancellation rule. */
    suspend fun cancel(ref: String): Int {
        val row = dao.booking(ref) ?: return 0
        if (row.status !in setOf(BookingStatus.REQUESTED, BookingStatus.CONFIRMED)) return 0
        val amount = if (row.status == BookingStatus.REQUESTED) row.total else refundFor(row)
        val now = AppClock.nowMillis()
        val item = store.item(row.listingId)
        val renter = store.person(row.renterHandle).name.substringBefore(' ')
        db.withTransaction {
            dao.upsertBooking(row.copy(status = BookingStatus.CANCELLED, updatedAt = now))
            refund(row, amount, "Refund · $ref cancelled")
            notify(row.renterHandle, NotificationItem.Kind.BOOKING, "Booking $ref cancelled", "${item?.title ?: "Your booking"} · ${lkr(amount)} refunded", "s31", ref)
            notify(row.ownerHandle, NotificationItem.Kind.BOOKING, "$renter cancelled $ref", "${item?.shortTitle ?: ref} · the dates are free again", "s28", ref)
            postDm(row.renterHandle, row.ownerHandle, SYSTEM, "Booking cancelled · $ref", ChatMessage.Kind.SYSTEM)
        }
        return amount
    }

    /**
     * Requests the owner did not answer within 24 hours are declined and
     * refunded in full, so a renter is never left waiting on silence.
     */
    suspend fun expireStaleRequests() {
        val now = AppClock.nowMillis()
        dao.expiredRequests(now).forEach { row ->
            val item = store.item(row.listingId)
            db.withTransaction {
                dao.upsertBooking(row.copy(status = BookingStatus.DECLINED, respondBy = null, updatedAt = now))
                refund(row, row.total, "Refund · ${row.ref} expired")
                notify(row.renterHandle, NotificationItem.Kind.BOOKING, "Request expired · ${item?.shortTitle ?: row.ref}", "The owner didn't reply in time. ${lkr(row.total)} refunded.", "s31", row.ref)
                notify(row.ownerHandle, NotificationItem.Kind.BOOKING, "A request expired", "${item?.shortTitle ?: row.ref} · you didn't reply within 24 h", "s28", row.ref)
            }
        }
    }

    /**
     * Free within 24 hours of booking, or more than three days before pickup;
     * half until a day before; nothing after that.
     */
    fun refundFor(row: BookingEntity): Int {
        val now = AppClock.nowMillis()
        val pickup = AppClock.toMillis(Seed.dayAt(LocalDate.ofEpochDay(row.startDay), row.pickupTime))
        return when {
            now - row.createdAt <= DAY_MS || pickup - now >= 3 * DAY_MS -> row.total
            pickup - now >= DAY_MS -> row.total / 2
            else -> 0
        }
    }

    val cancellationRule: String get() = CANCELLATION_RULE

    /** The same rule over the domain booking, for quoting the refund before cancelling. */
    fun refundFor(booking: Booking): Int {
        if (booking.status == BookingStatus.REQUESTED) return booking.total
        val now = AppClock.nowMillis()
        val pickup = AppClock.toMillis(booking.pickupAt)
        val created = AppClock.toMillis(booking.createdAt)
        return when {
            now - created <= DAY_MS || pickup - now >= 3 * DAY_MS -> booking.total
            pickup - now >= DAY_MS -> booking.total / 2
            else -> 0
        }
    }

    /** "Remove from history". Restorable, so the toast can offer undo. */
    suspend fun setHidden(ref: String, hide: Boolean) {
        val row = dao.booking(ref) ?: return
        dao.upsertBooking(row.copy(hiddenFromHistory = hide, updatedAt = AppClock.nowMillis()))
    }

    /** The booking has been written to the phone's calendar. */
    suspend fun markAddedToCalendar(ref: String) {
        val row = dao.booking(ref) ?: return
        dao.upsertBooking(row.copy(calendarAdded = true, updatedAt = AppClock.nowMillis()))
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
        rows.map { WalletLine(it.label, dayMonth(AppClock.fromMillis(it.createdAt).toLocalDate()), it.amount) }
    }.hot(emptyList())

    /** Create a withdrawal and empty the available balance. */
    suspend fun withdraw(): Int = db.withTransaction {
        val amount = db.discovery().profileNow(me)?.walletAvailable ?: 0
        if (amount > 0) {
            dao.insertTxn(WalletTxnEntity(handle = me, label = "Withdrawal to your bank", amount = -amount, createdAt = AppClock.nowMillis()))
            dao.setWalletAvailable(me, 0)
        }
        amount
    }

    /** Create a top-up from the saved card. False if there is no card to take it from. */
    suspend fun topUp(amount: Int): Boolean {
        if (amount <= 0) return false
        val profile = db.discovery().profileNow(me) ?: return false
        if (profile.cardLast4 == null) return false
        db.withTransaction {
            dao.insertTxn(WalletTxnEntity(handle = me, label = "Top up from ${profile.cardBrand} ••••${profile.cardLast4}", amount = amount, createdAt = AppClock.nowMillis()))
            dao.setWalletAvailable(me, profile.walletAvailable + amount)
        }
        return true
    }

    /**
     * Save a card. Only the brand, the last four digits and the expiry are
     * kept; the full number is checked (Luhn) and then forgotten.
     */
    suspend fun saveCard(number: String, expiry: String): String? {
        Validate.cardNumber(number)?.let { return it }
        Validate.cardExpiry(expiry)?.let { return it }
        val profile = db.discovery().profileNow(me) ?: return "You're signed out"
        val digits = number.filter(Char::isDigit)
        val exp = expiry.filter(Char::isDigit).let { "${it.take(2)}/${it.drop(2)}" }
        db.discovery().upsertProfile(profile.copy(cardBrand = Validate.cardBrand(digits), cardLast4 = digits.takeLast(4), cardExpiry = exp))
        return null
    }

    suspend fun removeCard() {
        val profile = db.discovery().profileNow(me) ?: return
        db.discovery().upsertProfile(profile.copy(cardBrand = null, cardLast4 = null, cardExpiry = null))
    }

    /** An owner is paid into their wallet when a rental settles. */
    internal suspend fun payOwner(row: BookingEntity, label: String) {
        if (row.ownerPayout <= 0 || row.paymentMethod == PAY_CASH) return
        val available = db.discovery().profileNow(row.ownerHandle)?.walletAvailable ?: 0
        dao.setWalletAvailable(row.ownerHandle, available + row.ownerPayout)
        dao.insertTxn(WalletTxnEntity(handle = row.ownerHandle, label = "Payout · ${row.ref} · $label", amount = row.ownerPayout, createdAt = AppClock.nowMillis()))
        notify(row.ownerHandle, NotificationItem.Kind.PAYOUT, "${lkr(row.ownerPayout)} paid into your wallet", "$label · ${row.ref}", "s35", row.ref)
    }

    // ============================================================ notifications

    val notifications: StateFlow<List<Notification>> = store.notifications

    suspend fun markRead(id: Long) = dao.markRead(id)
    suspend fun markAllRead() = dao.markAllRead(me)

    suspend fun deleteNotification(id: Long): NotificationEntity? {
        val row = dao.notification(id) ?: return null
        dao.deleteNotification(id)
        return row
    }

    suspend fun restoreNotification(row: NotificationEntity) {
        dao.insertNotification(row.copy(id = 0))
    }

    internal suspend fun notify(handle: String, kind: NotificationItem.Kind, title: String, body: String, route: String, ref: String?) {
        if (handle.isEmpty()) return
        dao.insertNotification(
            NotificationEntity(handle = handle, kind = kind, title = title, body = body, unread = true, createdAt = AppClock.nowMillis(), route = route, ref = ref),
        )
    }

    // ============================================================ messages

    /** Who the open conversation is with. */
    val threadId: StateFlow<String> = session.session.map { it.threadId }.hot("")

    val thread: StateFlow<List<Message>> = combine(session.session, store.messages) { s, list ->
        val key = dmKey(s.currentHandle.orEmpty(), s.threadId)
        list.filter { it.threadId == key }
    }.hot(emptyList())

    /** The latest booking between the two people in the open conversation, for its header card. */
    val threadBooking: StateFlow<Booking?> = combine(session.session, store.bookings) { s, list ->
        val a = s.currentHandle.orEmpty()
        val b = s.threadId
        list.filter {
            it.status != BookingStatus.DRAFT &&
                ((it.renter.handle == a && it.owner.handle == b) || (it.renter.handle == b && it.owner.handle == a))
        }.maxByOrNull { it.createdAt }
    }.hot(null)

    /** Everyone the member has a conversation with, most recent first. */
    val conversations: StateFlow<List<Conversation>> = combine(store.me, store.messages, store.people) { h, list, people ->
        list.filter { it.threadId.startsWith(DM) && h in it.threadId.removePrefix(DM).split('|') }
            .groupBy { it.threadId }
            .mapNotNull { (key, messages) ->
                val other = key.removePrefix(DM).split('|').firstOrNull { it != h } ?: return@mapNotNull null
                val person = people[other] ?: return@mapNotNull null
                Conversation(person, messages.maxByOrNull { it.createdAt })
            }
            .sortedByDescending { it.last?.createdAt }
    }.hot(emptyList())

    /** The other people in [conversations], for code that only needs who. */
    val threads: StateFlow<List<String>> = conversations.map { list -> list.map { it.other.handle } }.hot(emptyList())

    fun openThread(handle: String) = session.selectThread(handle)

    /** Create a message, and tell the other person it arrived. */
    suspend fun send(text: String) {
        val body = text.trim()
        val other = session.threadId
        if (body.isEmpty() || other.isEmpty() || me.isEmpty()) return
        val name = store.person(me).name.substringBefore(' ')
        postDm(me, other, me, body, ChatMessage.Kind.TEXT)
        notify(other, NotificationItem.Kind.MESSAGE, "New message from $name", body.take(80), "s33", me)
    }

    /** Delete one of your own messages. Returns it so it can be restored. */
    suspend fun deleteMessage(id: Long): MessageEntity? {
        val row = dao.message(id) ?: return null
        if (row.fromHandle != me || row.kind != ChatMessage.Kind.TEXT) return null
        dao.deleteMessage(id)
        return row
    }

    suspend fun restoreMessage(row: MessageEntity) {
        dao.insertMessage(row.copy(id = 0))
    }

    /** Post into the conversation between [a] and [b]. */
    internal suspend fun postDm(a: String, b: String, from: String, body: String, kind: ChatMessage.Kind) {
        dao.insertMessage(MessageEntity(threadId = dmKey(a, b), fromHandle = from, body = body, kind = kind, createdAt = AppClock.nowMillis()))
    }

    /** Post into a thread with its own key — a swap negotiation. */
    internal suspend fun post(threadId: String, from: String, body: String, kind: ChatMessage.Kind) {
        dao.insertMessage(MessageEntity(threadId = threadId, fromHandle = from, body = body, kind = kind, createdAt = AppClock.nowMillis()))
    }

    // ============================================================ claims

    val claims: StateFlow<List<Claim>> = store.claims

    /** The claim on screen: the selected one, else the newest against the member. */
    val currentClaim: StateFlow<Claim?> = combine(session.session, store.claims) { s, list ->
        val h = s.currentHandle.orEmpty()
        list.firstOrNull { it.ref == s.claimRef && (it.respondent.handle == h || it.claimant.handle == h || isAdmin(h)) }
            ?: list.filter { it.respondent.handle == h }.maxByOrNull { it.createdAt }
            ?: list.filter { it.claimant.handle == h }.maxByOrNull { it.createdAt }
    }.hot(null)

    private fun isAdmin(handle: String) = handle == Seed.ADMIN

    /** The claims desk's queue: undecided first, newest first. */
    val queue: StateFlow<List<Claim>> = store.claims.map { list ->
        list.sortedWith(compareBy<Claim>({ it.decided }, { -AppClock.toMillis(it.createdAt) }))
    }.hot(emptyList())

    fun selectClaim(ref: String) = session.selectClaim(ref)

    /** The booking a claim is about, for its protection plan. */
    fun bookingFor(claim: Claim): Booking? = store.booking(claim.bookingRef)

    /**
     * The two frames a claim is argued over: the claimed angle at pickup and at
     * return, read from the condition record the handover made. A claim with no
     * record behind it (the sample history) keeps the frames it was filed with.
     */
    fun evidenceFor(claim: Claim): ClaimEvidence {
        val rows = store.captureRows.value.filter { it.bookingRef == claim.bookingRef && it.angle == claim.area }
        val pickup = rows.firstOrNull { it.phase == com.gearloop.app.data.Phase.PICKUP }
        val back = rows.firstOrNull { it.phase == com.gearloop.app.data.Phase.RETURN }
        // A claim filed before this phone kept a condition record (the sample
        // history) carries the frames and the integrity it was filed with.
        if (pickup == null && back == null) {
            return ClaimEvidence(
                "cond_1", "cond_2", "9.7134°N, 80.0246°E", "9.7131°N, 80.0244°E",
                listOf("Both parties signed" to true, "Same angle" to true, "Taken 40 m apart" to true, "Passed the photo check" to true),
            )
        }
        val stamp = { c: com.gearloop.app.data.db.CaptureEntity? ->
            if (c == null) "NOT SHOT"
            else listOfNotNull(
                if (c.latitude != null && c.longitude != null) com.gearloop.app.data.coordinates(c.latitude, c.longitude) else "NO GEOTAG",
                com.gearloop.app.data.metaStamp(AppClock.fromMillis(c.capturedAt)),
            ).joinToString(" · ")
        }
        val apart = if (pickup?.latitude != null && pickup.longitude != null && back?.latitude != null && back.longitude != null) {
            metresBetween(pickup.latitude, pickup.longitude, back.latitude, back.longitude)
        } else null
        return ClaimEvidence(
            pickupPhoto = pickup?.photoKey ?: "cond_1",
            returnPhoto = back?.photoKey ?: "cond_2",
            pickupStamp = stamp(pickup),
            returnStamp = stamp(back),
            checks = listOf(
                "Both parties signed" to (bookingFor(claim)?.fullySigned == true),
                "Same angle" to (pickup != null && back != null),
                (if (apart == null) "No geotag to compare" else "Taken ${com.gearloop.app.data.grouped(apart)} m apart") to (apart != null && apart <= 100),
                "Passed the photo check" to (pickup?.ok == true && back?.ok == true),
            ),
        )
    }

    /** Great-circle distance, to the metre: plenty for "were these taken in the same place". */
    private fun metresBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2).let { it * it }
        return (2 * r * kotlin.math.asin(kotlin.math.sqrt(a))).toInt()
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

    /** Contest, with the renter's side in their words. */
    suspend fun contest(ref: String, statement: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED || row.status == ClaimStatus.ACCEPTED) return
        dao.upsertClaim(row.copy(status = ClaimStatus.CONTESTED, renterStatement = statement.trim().ifEmpty { "I don't accept this claim." }))
        notify(row.claimantHandle, NotificationItem.Kind.SAFETY, "Claim $ref contested", "The claims desk will review both sides.", "s34", ref)
    }

    /** Evidence added to the claim file: a note, and any photos taken for it. */
    suspend fun addEvidence(ref: String, note: String, photos: List<String> = emptyList()) {
        val row = dao.claim(ref) ?: return
        val status = if (row.status == ClaimStatus.MORE_EVIDENCE || row.status == ClaimStatus.OPEN) ClaimStatus.CONTESTED else row.status
        dao.upsertClaim(row.copy(renterEvidence = note.trim().ifEmpty { row.renterEvidence }, renterPhotos = row.renterPhotos + photos, status = status))
    }

    /** Liability accepted, settled at the renter's share; the owner is paid the claim. */
    suspend fun acceptLiability(ref: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED || row.status == ClaimStatus.ACCEPTED) return
        val share = renterShare(row.bookingRef, row.amount)
        val now = AppClock.nowMillis()
        db.withTransaction {
            dao.upsertClaim(row.copy(status = ClaimStatus.ACCEPTED, award = row.amount, decidedAt = now, decidedBy = "Renter"))
            if (share > 0) dao.insertTxn(WalletTxnEntity(handle = row.respondentHandle, label = "Claim $ref settled", amount = -share, createdAt = now))
            creditClaimant(row.claimantHandle, row.amount, "Claim $ref settled")
            notify(row.claimantHandle, NotificationItem.Kind.PAYOUT, "Claim $ref accepted", "${lkr(row.amount)} paid into your wallet", "s35", ref)
        }
    }

    private suspend fun creditClaimant(handle: String, amount: Int, label: String) {
        if (amount <= 0) return
        val available = db.discovery().profileNow(handle)?.walletAvailable ?: 0
        dao.setWalletAvailable(handle, available + amount)
        dao.insertTxn(WalletTxnEntity(handle = handle, label = label, amount = amount, createdAt = AppClock.nowMillis()))
    }

    /**
     * Issue a ruling, as the signed-in claims desk. Refused without a stated
     * reason (FR-19), and the award can never exceed the cap the severity sets.
     */
    suspend fun issueRuling(ref: String, award: Int, rationale: String): Boolean {
        val row = dao.claim(ref) ?: return false
        if (rationale.isBlank() || row.status == ClaimStatus.DECIDED) return false
        val cap = row.itemValue * row.severity.capPercent / 100
        val capped = award.coerceIn(0, minOf(row.amount, cap))
        val now = AppClock.nowMillis()
        val desk = store.person(me).name.takeIf { me.isNotEmpty() } ?: Seed.admin.name
        db.withTransaction {
            dao.upsertClaim(row.copy(status = ClaimStatus.DECIDED, award = capped, rationale = rationale.trim(), decidedAt = now, decidedBy = desk))
            val share = renterShare(row.bookingRef, capped)
            if (share > 0) dao.insertTxn(WalletTxnEntity(handle = row.respondentHandle, label = "Claim $ref ruling", amount = -share, createdAt = now))
            creditClaimant(row.claimantHandle, capped, "Claim $ref ruling")
            notify(row.respondentHandle, NotificationItem.Kind.SAFETY, "Ruling on claim $ref", "Awarded ${lkr(capped)}. You pay ${lkr(share)}. The reason is on the claim.", "s34", ref)
            notify(row.claimantHandle, NotificationItem.Kind.SAFETY, "Ruling on claim $ref", "Awarded ${lkr(capped)}, paid into your wallet.", "s34", ref)
        }
        return true
    }

    /** Ask the renter for more. */
    suspend fun requestMoreEvidence(ref: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED) return
        db.withTransaction {
            dao.setClaimStatus(ref, ClaimStatus.MORE_EVIDENCE)
            notify(row.respondentHandle, NotificationItem.Kind.SAFETY, "More evidence needed · $ref", "The claims desk has asked for anything else you have.", "s34", ref)
        }
    }

    suspend fun escalate(ref: String) {
        val row = dao.claim(ref) ?: return
        if (row.status == ClaimStatus.DECIDED) return
        dao.setClaimStatus(ref, ClaimStatus.ESCALATED)
    }

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000

        const val PAY_CARD = 0
        const val PAY_WALLET = 1
        const val PAY_CASH = 2

        /** Who a booking event in a conversation is from. */
        const val SYSTEM = "system"

        private const val DM = "dm:"

        /** The key of the conversation between two members, the same whichever way round. */
        fun dmKey(a: String, b: String): String = DM + listOf(a, b).sorted().joinToString("|")

        /** The rule [refundFor] applies, in the words the listing and checkout print. */
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
            "@priya_n" to "22 Temple Rd, Kopay",
            "@arun_s" to "41 KKS Rd, Chunnakam",
            "@fathima_r" to "6 Main St, Jaffna Fort",
            "@kasun_w" to "17 Point Pedro Rd, Kaithady",
            "@tharsika_m" to "9 School Ln, Urumpirai",
            "@sanjay_p" to "30 Palaly Rd, Kondavil",
            "@meera_g" to "12 Campus Rd, Thirunelvely",
            "@rohan_ds" to "2 Jetty Rd, Mandaitivu",
        )

        fun pickupAddress(handle: String): String = addresses[handle] ?: "Jaffna"
    }
}
