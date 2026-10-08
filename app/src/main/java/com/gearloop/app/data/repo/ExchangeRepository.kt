package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.DayState
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.Fit
import com.gearloop.app.data.GearItem
import com.gearloop.app.data.Grade
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.NeedsAttention
import com.gearloop.app.data.NotificationItem
import com.gearloop.app.data.Photos
import com.gearloop.app.data.ProposalStatus
import com.gearloop.app.data.SwapOffer
import com.gearloop.app.data.countdown
import com.gearloop.app.data.db.CalendarBlockEntity
import com.gearloop.app.data.db.ClosetItemEntity
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.db.ListingEntity
import com.gearloop.app.data.db.RailEntryEntity
import com.gearloop.app.data.db.ReviewEntity
import com.gearloop.app.data.db.SwapProposalEntity
import com.gearloop.app.data.db.WalletTxnEntity
import com.gearloop.app.data.dateRange
import com.gearloop.app.data.dayMonth
import com.gearloop.app.data.grouped
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
import java.time.temporal.ChronoUnit

/**
 * Member 4 — Exchange & the Supply Side.
 *
 * S-19 Swap feed · S-20 Build proposal · S-21 Fairness · S-22 Proposal detail ·
 * S-23 Cycle preview · S-24 Owner dashboard · S-25 / S-26 Create listing ·
 * S-27 Calendar · S-28 Requests · S-36 Gear closet.
 *
 * The owner console (S-24 to S-28) is Kavitha's: the dataset makes her the host
 * with six listed units, so a request Nirmal sends from the renter's side lands
 * in her inbox here, and accepting it confirms it on his rentals screen. One
 * device, two sides of the market, one database.
 */
class ExchangeRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val money: MoneyRepository,
    private val scope: CoroutineScope,
) {
    private val dao = db.exchange()

    /**
     * Every read-modify-write of a row goes through this, one at a time. Two
     * edits launched together used to read the same row and the second write
     * put back what the first had changed: the cash on a sent proposal (D-14),
     * a listing title typed on S-25 (D-15).
     */
    private val edits = Mutex()

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    // ============================================================ valuations

    /** Fair market values, as S-21's valuation columns derive them. */
    private val fmv = mapOf(
        "burton" to Demo.BURTON_FMV,
        "santacruz" to Demo.SANTACRUZ_FMV,
        "rack" to 29_000,
        "kayak" to 44_000,
        "nsp" to 38_000,
        "trek" to 46_500,
        "helmet" to 6_500,
        "wetsuit" to 9_000,
        "torq" to Demo.BOARD_VALUE,
        "tent" to 18_000,
        "sotkayak" to Demo.AYESHA_KAYAK_FMV,
        "myhelmet" to 7_000,
        "mycamp" to 8_000,
    )

    fun valueOf(item: GearItem): Int = fmv[item.id] ?: (item.deposit * 2)

    /** The arithmetic behind a value, row by row, ending in the value itself. */
    fun valuationRows(item: GearItem): List<Pair<String, String>> = when (item.id) {
        Demo.burton.id -> Demo.burtonValuation
        Demo.santaCruz.id -> Demo.santaCruzValuation
        else -> {
            val value = valueOf(item)
            listOf(
                "New price" to grouped((value / 0.85 / 0.7).toInt() / 100 * 100),
                "Age" to "−30%",
                "Grade ${item.grade.letter}" to if (item.grade == Grade.A) "×1.00" else "×0.85",
                "FMV" to grouped(value),
            )
        }
    }

    // ============================================================ S-19 matches

    val matches: StateFlow<List<Match>> = combine(store.matchRows, store.allItems, store.people, store.proposalRows) { rows, items, people, proposals ->
        val byId = items.associateBy { it.id }
        rows.filter { !it.hidden }.mapNotNull { r ->
            val give = byId[r.giveListingId] ?: return@mapNotNull null
            val get = byId[r.getListingId] ?: return@mapNotNull null
            val person = people[r.counterpartyHandle] ?: return@mapNotNull null
            val open = proposals.firstOrNull { it.matchId == r.id && it.status in OPEN }
            Match(r.id, SwapOffer(give, get, r.giveValue, r.getValue, person, r.threeWay), open?.ref)
        }
    }.hot(emptyList())

    val hiddenCount: StateFlow<Int> = store.matchRows.map { rows -> rows.count { it.hidden } }.hot(0)

    /** S-19 — Update: hide a match ("not for me"). Undoable. */
    suspend fun setMatchHidden(id: String, hidden: Boolean) = dao.setMatchHidden(id, hidden)

    // ============================================================ proposals

    val proposals: StateFlow<List<Proposal>> = combine(store.proposalRows, store.allItems, store.people) { rows, items, people ->
        val byId = items.associateBy { it.id }
        rows.mapNotNull { r ->
            Proposal(
                ref = r.ref,
                matchId = r.matchId,
                give = byId[r.giveListingId] ?: return@mapNotNull null,
                get = byId[r.getListingId] ?: return@mapNotNull null,
                giveValue = r.giveValue,
                getValue = r.getValue,
                counterparty = people[r.counterpartyHandle] ?: return@mapNotNull null,
                boot = r.boot,
                status = r.status,
                round = r.round,
                expiresAt = DemoClock.fromMillis(r.expiresAt),
                updatedAt = DemoClock.fromMillis(r.updatedAt),
            )
        }
    }.hot(emptyList())

    val proposal: StateFlow<Proposal?> = combine(session.session, proposals) { s, list ->
        list.firstOrNull { it.ref == s.proposalRef } ?: list.firstOrNull { it.open }
    }.hot(null)

    val negotiation: StateFlow<List<Message>> = combine(proposal, store.messages) { p, list ->
        if (p == null) emptyList() else list.filter { it.threadId == p.ref }
    }.hot(emptyList())

    /** What the renter could offer: the board they own, and the kit in their closet. */
    fun myGear(): List<GearItem> = store.allItems.value
        // What the renter owns and has listed, the board first. This was the
        // Burton plus Kavitha's helmet and wetsuit, offered as his own.
        .filter { it.owner.handle == Seed.ME && it.status != ListingStatus.DRAFT }
        .sortedBy { if (it.id == Demo.burton.id) 0 else 1 }

    /** What the other side could give: the item matched first, then the rest of their gear. */
    fun theirGear(p: Proposal): List<GearItem> {
        // Only theirs: Dinesh's list used to end with Kavitha's kayak.
        val theirs = store.allItems.value.filter {
            it.owner.handle == p.counterparty.handle && it.id != p.get.id && it.status != ListingStatus.DRAFT
        }
        return (listOf(p.get) + theirs).distinctBy { it.id }.take(3)
    }

    /** S-19 "Propose swap" — Create a draft, or reopen the one already under way. */
    suspend fun proposeFromMatch(matchId: String): String? {
        val match = dao.match(matchId) ?: return null
        val existing = dao.openProposalFor(matchId)
        if (existing != null) {
            session.selectProposal(existing.ref)
            return existing.ref
        }
        val ref = nextProposalRef(dao.proposalRefs().toSet())
        val now = DemoClock.nowMillis()
        dao.upsertProposal(
            SwapProposalEntity(
                ref = ref, matchId = matchId,
                giveListingId = match.giveListingId, getListingId = match.getListingId,
                giveValue = match.giveValue, getValue = match.getValue,
                counterpartyHandle = match.counterpartyHandle,
                boot = 0, status = ProposalStatus.DRAFT, round = 1,
                expiresAt = now + 5 * MoneyRepository.DAY_MS, createdAt = now, updatedAt = now,
            ),
        )
        session.selectProposal(ref)
        return ref
    }

    fun selectProposal(ref: String) = session.selectProposal(ref)

    private suspend fun editProposal(change: (SwapProposalEntity) -> SwapProposalEntity) = edits.withLock {
        val ref = proposal.value?.ref ?: return@withLock
        val row = dao.proposal(ref) ?: return@withLock
        dao.upsertProposal(change(row).copy(updatedAt = DemoClock.nowMillis()))
    }

    /** S-20 — Update what is given. */
    suspend fun setGive(item: GearItem) = editProposal { it.copy(giveListingId = item.id, giveValue = valueOf(item)) }

    /** S-20 — Update what is wanted. */
    suspend fun setGet(item: GearItem) = editProposal { it.copy(getListingId = item.id, getValue = valueOf(item)) }

    /**
     * S-21 "Send proposal" — Update: sent, or sent back as a counter. The cash
     * comes in with the send and is written in the same row update, so the
     * amount on the slider is the amount that goes (D-14).
     */
    suspend fun send(boot: Int? = null) = edits.withLock {
        val p = proposal.value ?: return@withLock
        val row = dao.proposal(p.ref) ?: return@withLock
        val now = DemoClock.nowMillis()
        val round = if (row.status == ProposalStatus.DRAFT) 1 else row.round + 1
        val cash = (boot ?: row.boot).coerceAtLeast(0)
        db.withTransaction {
            dao.upsertProposal(row.copy(boot = cash, status = ProposalStatus.SENT, round = round.coerceAtMost(5), updatedAt = now))
            val plus = if (cash > 0) " + ${lkr(cash)}" else ""
            money.post(row.ref, "My ${p.give.shortTitle}$plus for your ${p.get.shortTitle}.", ChatMessage.Kind.TEXT, fromMe = true)
        }
    }

    /** S-22 "Accept" — Update: agreed, and the cash goes into escrow. */
    suspend fun accept() {
        val p = proposal.value ?: return
        val row = dao.proposal(p.ref) ?: return
        val now = DemoClock.nowMillis()
        db.withTransaction {
            dao.upsertProposal(row.copy(status = ProposalStatus.ACCEPTED, updatedAt = now))
            if (row.boot > 0) db.money().insertTxn(WalletTxnEntity(label = "Escrow · ${row.ref}", amount = -row.boot, createdAt = now))
            money.post(row.ref, "Swap agreed · ${row.ref}", ChatMessage.Kind.SYSTEM, fromMe = false)
            money.notify(
                NotificationItem.Kind.SWAP,
                "Swap agreed with ${p.counterparty.name.substringBefore(' ')}",
                "${p.give.shortTitle} for ${p.get.shortTitle}" + if (row.boot > 0) " · ${lkr(row.boot)} held in escrow" else "",
                "s22",
                row.ref,
            )
        }
    }

    /** S-22 — Update: turned down. */
    suspend fun decline() {
        val ref = proposal.value?.ref ?: return
        dao.setProposalStatus(ref, ProposalStatus.DECLINED, DemoClock.nowMillis())
    }

    /** S-22 — Delete a draft outright, or withdraw one already sent. */
    suspend fun withdraw() {
        val p = proposal.value ?: return
        if (p.status == ProposalStatus.DRAFT) dao.deleteProposal(p.ref)
        else dao.setProposalStatus(p.ref, ProposalStatus.WITHDRAWN, DemoClock.nowMillis())
    }

    private fun nextProposalRef(used: Set<String>): String {
        var n = 2215
        while ("SW-$n" in used) n++
        return "SW-$n"
    }

    // ============================================================ S-23 cycle

    val cycle: StateFlow<Cycle?> = combine(store.cycleRows, store.allItems, store.people) { rows, items, people ->
        val byId = items.associateBy { it.id }
        rows.groupBy { it.cycleId }.entries.firstOrNull()?.let { (id, members) ->
            Cycle(
                id,
                members.sortedBy { it.position }.mapNotNull { m ->
                    val person = people[m.handle] ?: return@mapNotNull null
                    val item = byId[m.givesListingId] ?: return@mapNotNull null
                    CycleMember(person, item, m.givesValue, m.accepted)
                },
            )
        }
    }.hot(null)

    /** S-23 — Update the renter's own seat: accept, decline, or take it back. */
    suspend fun setMyAcceptance(accepted: Boolean?) {
        val c = cycle.value ?: return
        dao.setCycleAcceptance(c.id, Seed.ME, accepted)
    }

    // ============================================================ owner console

    private val ownerItems: StateFlow<List<GearItem>> = store.allItems
        .map { list -> list.filter { it.owner.handle == Seed.OWNER && it.status == ListingStatus.PUBLISHED } }
        .hot(emptyList())

    private val ownerBookings: StateFlow<List<Booking>> = store.bookings
        .map { list -> list.filter { it.owner.handle == Seed.OWNER && it.status != BookingStatus.DRAFT } }
        .hot(emptyList())

    /** The units, in the order the calendar lists them: the boards, then the rest. */
    val units: StateFlow<List<CalendarUnit>> = ownerItems.map { items ->
        val order = listOf("torq", "kayak", "trek", "helmet", "wetsuit")
        items.sortedBy { order.indexOf(it.id).let { i -> if (i < 0) 99 else i } }
            .flatMap { item -> (1..item.units).map { CalendarUnit(item, it) } }
    }.hot(emptyList())

    data class Dashboard(
        val earnings: Int,
        val nextPayout: Int,
        val occupancy: Int,
        val listings: Int,
        val ratingText: String,
    )

    val dashboard: StateFlow<Dashboard> = combine(ownerBookings, units, store.people) { bookings, unitList, people ->
        val worldStart = DemoClock.toMillis(DemoClock.WORLD_START)
        val inApp = bookings.filter {
            DemoClock.toMillis(it.createdAt) >= worldStart &&
                it.status in setOf(BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.RETURNED, BookingStatus.COMPLETED)
        }
        Dashboard(
            earnings = Demo.EARNINGS_MONTH + inApp.sumOf { it.ownerPayout },
            nextPayout = Demo.NEXT_PAYOUT,
            occupancy = occupancyOf(unitList, bookings, DemoClock.today),
            // Listings, not units: the Trek Marlin's two units made this read 6
            // while her public profile (S-37) lists 5.
            listings = unitList.map { it.item.id }.distinct().size,
            ratingText = people[Seed.OWNER]?.ratingText ?: "",
        )
    }.hot(Dashboard(Demo.EARNINGS_MONTH, Demo.NEXT_PAYOUT, 0, 0, ""))

    /** Days held over the next fortnight, as a share of every unit's fourteen days. */
    private fun occupancyOf(unitList: List<CalendarUnit>, bookings: List<Booking>, from: LocalDate): Int {
        if (unitList.isEmpty()) return 0
        val held = unitList.sumOf { u -> unitOccupiedDays(u, bookings, from, 14) }
        return held * 100 / (unitList.size * 14)
    }

    private fun unitOccupiedDays(u: CalendarUnit, bookings: List<Booking>, from: LocalDate, days: Int): Int =
        (0 until days).count { d ->
            val day = from.plusDays(d.toLong())
            bookings.any { b -> b.item.id == u.item.id && b.unit == u.unit && b.status.holdsDates && day in b.span }
        }

    fun occupancyFor(u: CalendarUnit, from: LocalDate): Int = unitOccupiedDays(u, ownerBookings.value, from, 14) * 100 / 14

    /** "Needs you now": the most urgent request, any overdue return, and the open tasks. */
    val attention: StateFlow<List<Attention>> = combine(ownerBookings, store.taskRows) { bookings, tasks ->
        val now = DemoClock.now()
        val rows = mutableListOf<Attention>()
        bookings.filter { it.status == BookingStatus.REQUESTED && it.respondBy != null }
            .minByOrNull { it.respondBy!! }
            ?.let { b ->
                val left = ChronoUnit.MILLIS.between(now, b.respondBy)
                rows += Attention(
                    key = "req-${b.ref}",
                    item = NeedsAttention(NeedsAttention.Severity.URGENT, "Request expires in ${countdown(left)}", "${b.renter.name.substringBefore(' ')} · ${b.item.shortTitle} · ${b.dates}", "Review"),
                    route = "s28",
                    bookingRef = b.ref,
                )
            }
        bookings.filter { it.status == BookingStatus.ACTIVE && it.returnAt.isBefore(now) }.forEach { b ->
            val hours = ChronoUnit.HOURS.between(b.returnAt, now).coerceAtLeast(1)
            rows += Attention(
                key = "late-${b.ref}",
                item = NeedsAttention(NeedsAttention.Severity.URGENT, "Return overdue by $hours h", "${b.unitLabel} · ${b.renter.name}", "Contact renter"),
                route = "s33",
                bookingRef = b.ref,
            )
        }
        tasks.filter { !it.done }.forEach { t ->
            rows += Attention(
                key = "task-${t.id}",
                item = NeedsAttention(t.severity, t.title, t.detail, t.action),
                route = if (t.action == "Block dates") "s27" else "s33",
                taskId = t.id,
            )
        }
        rows
    }.hot(emptyList())

    /** Pickups and returns in the next three days, soonest first. */
    val comingUp: StateFlow<List<ComingUp>> = ownerBookings.map { bookings ->
        val now = DemoClock.now()
        val until = now.plusDays(3)
        val events = mutableListOf<ComingUp>()
        bookings.filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.ACTIVE }.forEach { b ->
            if (b.status == BookingStatus.CONFIRMED && !b.pickupAt.isBefore(now) && b.pickupAt.isBefore(until)) {
                events += ComingUp(b.pickupAt, "Pickup · ${b.renter.name}", b.unitLabel, b.ref)
            }
            if (!b.returnAt.isBefore(now) && b.returnAt.isBefore(until)) {
                events += ComingUp(b.returnAt, "Return · ${b.renter.name}", b.unitLabel, b.ref)
            }
        }
        events.sortedBy { it.at }
    }.hot(emptyList())

    /** S-24 — Update a task to done; "Block dates" also creates the block it names. */
    suspend fun completeTask(taskId: String): String? {
        var message: String? = null
        db.withTransaction {
            if (taskId == "service") {
                val day = LocalDate.of(2026, 8, 12)
                if (dao.blockCount("trek", 2, day.toEpochDay()) == 0) {
                    dao.insertBlock(CalendarBlockEntity(listingId = "trek", unit = 2, day = day.toEpochDay(), reason = "Service"))
                }
                message = "Blocked ${dayMonth(day)} on Trek Marlin #2 for its service"
            }
            dao.setTaskDone(taskId, true)
        }
        return message
    }

    /** S-24 "Write it" — Create the owner's review of a renter, then close the task. */
    suspend fun reviewRenter(taskId: String, renterHandle: String, listingId: String, stars: Int, body: String) {
        db.withTransaction {
            db.discovery().insertReview(
                ReviewEntity(
                    listingId = listingId, subjectHandle = renterHandle, authorHandle = Seed.OWNER,
                    date = dayMonth(DemoClock.today) + " " + DemoClock.today.year, stars = stars.coerceIn(1, 5),
                    body = body.trim(), photos = emptyList(), ownerResponse = null, bookingRef = null, createdAt = DemoClock.nowMillis(),
                ),
            )
            dao.setTaskDone(taskId, true)
        }
    }

    // ============================================================ S-27 calendar

    data class Conflict(val late: Booking, val next: Booking, val freeUnit: Int?)

    /** An overdue return on a unit that goes out again within a day. */
    val conflicts: StateFlow<List<Conflict>> = ownerBookings.map { bookings ->
        val now = DemoClock.now()
        bookings.filter { it.status == BookingStatus.ACTIVE && it.returnAt.isBefore(now) }.mapNotNull { late ->
            val next = bookings.filter {
                it.item.id == late.item.id && it.unit == late.unit && it.ref != late.ref &&
                    it.status == BookingStatus.CONFIRMED && it.pickupAt.isAfter(now.minusHours(1)) && it.pickupAt.isBefore(now.plusDays(1))
            }.minByOrNull { it.pickupAt } ?: return@mapNotNull null
            val free = (1..late.item.units).filter { it != late.unit }.firstOrNull { u ->
                Availability.isRangeFree(next.span, DemoClock.today, store.spansFor(next.item.id, u, next.ref), store.blockedFor(next.item.id, u))
            }
            Conflict(late, next, free)
        }
    }.hot(emptyList())

    /** The fortnight's cells for one unit. Requested days are drawn as pending. */
    fun cellsFor(
        u: CalendarUnit,
        from: LocalDate,
        days: Int = 14,
        bookings: List<Booking> = store.bookings.value,
        blocks: List<CalendarBlockEntity> = store.blockRows.value,
    ): List<DayCell> {
        val held = bookings.filter { it.item.id == u.item.id && it.unit == u.unit && it.status.holdsDates }
        val confirmed = held.filter { it.status != BookingStatus.REQUESTED }.map { it.span }
        val requested = held.filter { it.status == BookingStatus.REQUESTED }.map { it.span }
        val blocked = blocks.filter { it.listingId == u.item.id && it.unit == u.unit }.map { LocalDate.ofEpochDay(it.day) }.toSet()
        return (0 until days).map { d ->
            val day = from.plusDays(d.toLong())
            val state = when {
                requested.any { day in it } && confirmed.none { day in it } -> DayState.IN_RANGE
                else -> Availability.stateOf(day, DemoClock.today, confirmed, blocked).let { if (it == DayState.TODAY) DayState.AVAILABLE else it }
            }
            DayCell(day, state)
        }
    }

    fun blockAt(u: CalendarUnit, day: LocalDate): CalendarBlock? =
        store.blockRows.value.firstOrNull { it.listingId == u.item.id && it.unit == u.unit && it.day == day.toEpochDay() }
            ?.let { CalendarBlock(it.id, it.listingId, it.unit, LocalDate.ofEpochDay(it.day), it.reason) }

    /** S-27 — Create a block on a free day. Refused on a held day. */
    suspend fun block(u: CalendarUnit, day: LocalDate, reason: String = "Owner block"): Long? {
        val held = store.spansNow(u.item.id, u.unit).any { day in it }
        if (held || day.isBefore(DemoClock.today) || dao.blockCount(u.item.id, u.unit, day.toEpochDay()) > 0) return null
        return dao.insertBlock(CalendarBlockEntity(listingId = u.item.id, unit = u.unit, day = day.toEpochDay(), reason = reason))
    }

    /** S-27 — Delete a block. */
    suspend fun unblock(id: Long): CalendarBlockEntity? {
        val row = dao.block(id) ?: return null
        dao.deleteBlock(id)
        return row
    }

    suspend fun restoreBlock(row: CalendarBlockEntity) {
        dao.insertBlock(row.copy(id = 0))
    }

    /** S-27 "Move to unit #2" — Update the booking onto a free unit. */
    suspend fun moveToUnit(ref: String, unit: Int) {
        val row = db.money().booking(ref) ?: return
        db.money().upsertBooking(row.copy(unit = unit, updatedAt = DemoClock.nowMillis()))
    }

    // ============================================================ S-28 requests

    val requests: StateFlow<List<Booking>> = ownerBookings
        .map { list -> list.filter { it.status == BookingStatus.REQUESTED }.sortedBy { it.respondBy } }
        .hot(emptyList())

    val ownerUpcoming: StateFlow<List<Booking>> = ownerBookings
        .map { list -> list.filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.ACTIVE }.sortedBy { it.start } }
        .hot(emptyList())

    val ownerPast: StateFlow<List<Booking>> = ownerBookings
        .map { list -> list.filter { it.status in setOf(BookingStatus.COMPLETED, BookingStatus.DECLINED, BookingStatus.CANCELLED, BookingStatus.RETURNED) }.sortedByDescending { it.start } }
        .hot(emptyList())

    fun trustStats(handle: String) = store.trustStats(handle)

    /** S-28 — Update: accept. The renter is told if the renter is on this device. */
    suspend fun accept(ref: String): Boolean {
        val row = db.money().booking(ref) ?: return false
        if (row.status != BookingStatus.REQUESTED) return false
        val span = Availability.Span(LocalDate.ofEpochDay(row.startDay), LocalDate.ofEpochDay(row.endDay))
        // Other open requests do not stop an acceptance; only confirmed holds and
        // blocks do, because accepting is the moment the dates become held.
        val confirmed = dao.confirmedOn(row.listingId, row.unit, ref)
            .map { Availability.Span(LocalDate.ofEpochDay(it.startDay), LocalDate.ofEpochDay(it.endDay)) }
        val free = Availability.isRangeFree(span, DemoClock.today, confirmed, store.blocksNow(row.listingId, row.unit))
        if (!free) return false
        val now = DemoClock.nowMillis()
        val item = store.item(row.listingId)
        db.withTransaction {
            db.money().upsertBooking(row.copy(status = BookingStatus.CONFIRMED, respondBy = null, updatedAt = now))
            if (row.renterHandle == Seed.ME) {
                money.notify(NotificationItem.Kind.BOOKING, "Kavitha accepted your request", "${item?.title ?: ref} · ${dateRange(span.start, span.end)} · $ref", "s31", ref)
                money.post(Seed.OWNER, "Booking confirmed · $ref", ChatMessage.Kind.SYSTEM, fromMe = false)
                money.post(Seed.OWNER, "Pickup point · ${MoneyRepository.pickupAddress(Seed.OWNER)}", ChatMessage.Kind.LOCATION, fromMe = false)
            }
        }
        return true
    }

    /** S-28 — Update: decline, refunding the renter. */
    suspend fun decline(ref: String) {
        val row = db.money().booking(ref) ?: return
        if (row.status != BookingStatus.REQUESTED) return
        val now = DemoClock.nowMillis()
        val item = store.item(row.listingId)
        db.withTransaction {
            db.money().upsertBooking(row.copy(status = BookingStatus.DECLINED, respondBy = null, updatedAt = now))
            if (row.renterHandle == Seed.ME) {
                db.money().insertTxn(WalletTxnEntity(label = "Refund · $ref declined", amount = row.total, createdAt = now))
                money.notify(NotificationItem.Kind.BOOKING, "Kavitha declined your request", "${item?.title ?: ref} · ${lkr(row.total)} refunded", "s31", ref)
            }
        }
    }

    // ============================================================ S-25 / S-26 listing

    val draftListing: StateFlow<GearItem?> = combine(session.session, store.allItems) { s, items ->
        s.draftListingId?.let { id -> items.firstOrNull { it.id == id } }
    }.hot(null)

    private suspend fun nextListingId(): String {
        var n = store.listingRows.value.size + 1
        while (db.discovery().listing("L-$n") != null) n++
        return "L-$n"
    }

    /**
     * Create a new listing as a draft, owned by whoever started it: Kavitha from
     * the owner dashboard, the renter from their own gear closet.
     */
    suspend fun startListing(owner: String, fromClosetId: Long? = null): String {
        val closet = fromClosetId?.let { dao.closetItem(it) }
        val id = nextListingId()
        val now = DemoClock.nowMillis()
        val sport = closet?.sport ?: "Surfing"
        val title = closet?.title ?: "New surfboard"
        val code = (title.filter(Char::isLetter).take(2).uppercase().ifEmpty { "GL" }) + "-" + (1000 + (id.hashCode() and 0x7fff) % 9000)
        db.discovery().upsertListing(
            ListingEntity(
                id = id,
                title = title,
                shortTitle = closet?.shortTitle ?: title,
                code = closet?.code ?: code,
                sizeLabel = closet?.sizeLabel ?: "—",
                grade = closet?.grade ?: Grade.B,
                pricePerDay = Demo.SUGGESTED_PRICE,
                distanceKm = if (owner == Seed.ME) 0.0 else 2.4,
                photos = closet?.photoKey?.let(::listOf) ?: emptyList(),
                ownerHandle = owner,
                fit = Fit.GOOD,
                sport = sport,
                instantBook = false,
                delivery = false,
                deposit = 25_000,
                saved = false,
                status = ListingStatus.DRAFT,
                units = 1,
                volumeL = null,
                frameBand = null,
                specs = emptyList(),
                included = listOf(closet?.shortTitle ?: "Item as photographed"),
                excluded = emptyList(),
                conditionPhotos = emptyList(),
                conditionNote = "New listing",
                conditionDate = dayMonth(DemoClock.today) + " " + DemoClock.today.year,
                extras = emptyList(),
                discounts = Demo.durationDiscounts.map { it.third },
                waiveDepositWithPlus = false,
                createdAt = now,
                sortOrder = 100 + store.listingRows.value.size,
            ),
        )
        if (closet != null) dao.upsertClosetItem(closet.copy(listingId = id))
        session.selectDraft(id)
        return id
    }

    /** Edit an existing listing through the same two screens (the dashboard's insights). */
    fun editListing(id: String) = session.selectDraft(id)

    private suspend fun editDraft(change: (ListingEntity) -> ListingEntity) = edits.withLock {
        val id = session.draftListingId ?: return@withLock
        val row = db.discovery().listing(id) ?: return@withLock
        db.discovery().upsertListing(change(row))
    }

    /**
     * S-25 — Create a photo in one of the seven slots (the guided angles). The
     * first photo is the item's own, so the hero is the thing being listed.
     */
    suspend fun addPhoto(slot: Int) = edits.withLock {
        val id = session.draftListingId ?: return@withLock
        val row = db.discovery().listing(id) ?: return@withLock
        val key = if (row.photos.isEmpty()) firstPhotoFor(row)
        else Photos.sampleFrame[Demo.captureAngles[slot].label] ?: "cond_1"
        val photos = row.photos.toMutableList()
        if (slot < photos.size) photos[slot] = key else photos.add(key)
        db.discovery().upsertListing(row.copy(photos = photos))
    }

    private suspend fun firstPhotoFor(row: ListingEntity): String =
        dao.closetForListing(row.id)?.photoKey
            ?: if (row.sport == "Surfing") "torq_1" else "sport_" + row.sport.lowercase()

    /** S-25 — Delete a photo. The rest close up behind it. */
    suspend fun removePhoto(slot: Int) = editDraft { row ->
        if (slot >= row.photos.size) row else row.copy(photos = row.photos.toMutableList().also { it.removeAt(slot) })
    }

    /**
     * S-25 — Update the title (the short form is what cards and tags print) and
     * the size printed on the gear tag, together in one write (D-15).
     */
    suspend fun setDetails(title: String, size: String) = editDraft { row ->
        val clean = title.trim().ifEmpty { row.title }
        row.copy(
            title = clean,
            shortTitle = clean.take(28),
            sizeLabel = size.trim().ifEmpty { row.sizeLabel }.uppercase(java.util.Locale.UK),
        )
    }

    /** S-26 — Update the daily rate. */
    suspend fun setPrice(price: Int) = editDraft { it.copy(pricePerDay = price.coerceIn(200, 20_000)) }

    /** S-26 — Update a duration discount. */
    suspend fun setDiscount(index: Int, on: Boolean) = editDraft { row ->
        val d = row.discounts.toMutableList().also { while (it.size < 3) it.add(false) }
        d[index] = on
        row.copy(discounts = d)
    }

    /** S-26 — Update. */
    suspend fun setWaiveDeposit(waive: Boolean) = editDraft { it.copy(waiveDepositWithPlus = waive) }

    /** S-25 "Save draft" — Update: nothing to change, but the timestamp records the save. */
    suspend fun saveDraft() = editDraft { it.copy(createdAt = it.createdAt) }

    /**
     * S-26 — Update: publish. A water-sport listing goes to the front of the
     * "New in surfing" rail, and a closet item is marked as listed.
     */
    suspend fun publish(): String? {
        val id = session.draftListingId ?: return null
        val row = db.discovery().listing(id) ?: return null
        val wasDraft = row.status == ListingStatus.DRAFT
        db.withTransaction {
            db.discovery().upsertListing(row.copy(status = ListingStatus.PUBLISHED))
            if (wasDraft && row.sport in setOf("Surfing", "SUP", "Kayaking")) {
                db.discovery().shiftRail("surf")
                db.discovery().upsertRailEntry(RailEntryEntity("surf", id, 0))
            }
            dao.closetForListing(id)?.let { dao.upsertClosetItem(it.copy(listed = true)) }
        }
        return id
    }

    /** Delete a draft that was never published. */
    suspend fun discardDraft() {
        val id = session.draftListingId ?: return
        val row = db.discovery().listing(id) ?: return
        if (row.status != ListingStatus.DRAFT) return
        db.withTransaction {
            db.discovery().deleteListing(id)
            dao.closetForListing(id)?.let { dao.upsertClosetItem(it.copy(listingId = null)) }
        }
        session.selectDraft(null)
    }

    // ============================================================ S-36 closet

    val closet: StateFlow<List<ClosetItem>> = store.closetRows.map { rows ->
        val now = DemoClock.nowMillis()
        rows.map {
            ClosetItem(it.id, it.title, it.shortTitle, Photos.res(it.photoKey), it.sport, it.sizeLabel, it.grade, it.code, it.listed, it.listingId, (now - it.idleSince) / MoneyRepository.DAY_MS)
        }
    }.hot(emptyList())

    /** S-36 — Create a closet item. */
    suspend fun addClosetItem(title: String, sport: String, sizeLabel: String, grade: Grade): Long {
        val clean = title.trim().ifEmpty { "Untitled gear" }
        return dao.insertClosetItem(
            ClosetItemEntity(
                title = clean,
                shortTitle = clean.take(28),
                photoKey = "sport_" + sport.lowercase().replace(' ', '_'),
                sport = sport,
                sizeLabel = sizeLabel.trim().ifEmpty { "—" }.uppercase(java.util.Locale.UK),
                grade = grade,
                code = (clean.filter(Char::isLetter).take(2).uppercase().ifEmpty { "GL" }) + "-" + (1000 + (clean.hashCode() and 0x7fff) % 9000),
                listed = false,
                listingId = null,
                idleSince = DemoClock.nowMillis(),
                position = dao.maxClosetPosition() + 1,
            ),
        )
    }

    /** S-36 — Update a closet item's details. */
    suspend fun updateClosetItem(id: Long, title: String, sizeLabel: String, grade: Grade) {
        val row = dao.closetItem(id) ?: return
        val clean = title.trim().ifEmpty { row.title }
        dao.upsertClosetItem(row.copy(title = clean, shortTitle = clean.take(28), sizeLabel = sizeLabel.trim().ifEmpty { row.sizeLabel }.uppercase(java.util.Locale.UK), grade = grade))
    }

    /** S-36 — Delete. Returns the row so the toast can offer undo. */
    suspend fun deleteClosetItem(id: Long): ClosetItemEntity? {
        val row = dao.closetItem(id) ?: return null
        dao.deleteClosetItem(id)
        return row
    }

    suspend fun restoreClosetItem(row: ClosetItemEntity) {
        dao.insertClosetItem(row)
    }

    private companion object {
        val OPEN = setOf(ProposalStatus.DRAFT, ProposalStatus.SENT, ProposalStatus.COUNTERED)
    }
}
