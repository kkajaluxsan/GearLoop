package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Availability
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.Pricing
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.DayState
import com.gearloop.app.data.Demo
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
import com.gearloop.app.data.db.ReviewEntity
import com.gearloop.app.data.db.SwapMatchEntity
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
import java.util.Locale

/**
 * Exchange & the Supply Side: the swap feed, proposals and the three-way loop;
 * the owner's dashboard, listings, calendar and requests; the gear closet.
 *
 * The owner screens are the signed-in member's own: a request one member sends
 * lands in the owner's inbox when the owner signs in, and accepting it confirms
 * it on the renter's rentals screen. One database, both sides of the market.
 */
class ExchangeRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val money: MoneyRepository,
    private val scope: CoroutineScope,
) {
    private val dao = db.exchange()
    private val me: String get() = session.handle

    /**
     * Every read-modify-write of a row goes through this, one at a time. Two
     * edits launched together used to read the same row and the second write
     * put back what the first had changed (D-14, D-15).
     */
    private val edits = Mutex()

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    // ============================================================ valuations

    /** Fair market values for the original catalogue, as the valuation columns derive them. */
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

    /** What an item is worth today; for anything else, [Pricing.estimatedValue]. */
    fun valueOf(item: GearItem): Int = fmv[item.id] ?: Pricing.estimatedValue(item)

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
                "Worth now" to grouped(value),
            )
        }
    }

    // ============================================================ the swap feed

    val matches: StateFlow<List<Match>> = combine(store.matchRows, store.allItems, store.people, store.proposalRows) { rows, items, people, proposals ->
        val byId = items.associateBy { it.id }
        rows.filter { !it.hidden }.sortedBy { it.position }.mapNotNull { r ->
            val give = byId[r.giveListingId] ?: return@mapNotNull null
            val get = byId[r.getListingId] ?: return@mapNotNull null
            val person = people[r.counterpartyHandle] ?: return@mapNotNull null
            val open = proposals.firstOrNull { it.matchId == r.id && it.status in OPEN }
            Match(r.id, SwapOffer(give, get, r.giveValue, r.getValue, person, r.threeWay), open?.ref)
        }
    }.hot(emptyList())

    val hiddenCount: StateFlow<Int> = store.matchRows.map { rows -> rows.count { it.hidden } }.hot(0)

    /** Hide a match ("not for me"). Undoable. */
    suspend fun setMatchHidden(id: String, hidden: Boolean) = dao.setMatchHidden(id, hidden)

    /**
     * Find trades for the signed-in member: each piece of their own gear against
     * other members' gear worth between half and twice as much, closest in value
     * first. Matches already found (or hidden) are kept; new ones are added.
     * Returns how many were added.
     */
    suspend fun refreshMatches(): Int = edits.withLock {
        val h = me.ifEmpty { return@withLock 0 }
        val mine = store.allItems.value.filter { it.owner.handle == h && it.status != ListingStatus.DRAFT }
        if (mine.isEmpty()) return@withLock 0
        val existing = dao.matchesOf(h)
        val taken = existing.map { it.giveListingId to it.getListingId }.toSet()
        val others = store.allItems.value.filter { it.owner.handle != h && it.owner.handle != Seed.ADMIN && it.status != ListingStatus.DRAFT }
        val found = mine.flatMap { give ->
            val gv = valueOf(give)
            others.filter { get ->
                val v = valueOf(get)
                v in (gv / 2)..(gv * 2) && get.sport != give.sport && (give.id to get.id) !in taken
            }.map { get -> Triple(give, get, kotlin.math.abs(valueOf(get) - gv)) }
        }.sortedBy { it.third }.distinctBy { it.second.id }.take((6 - existing.count { !it.hidden }).coerceAtLeast(0))
        found.forEachIndexed { i, (give, get, _) ->
            dao.upsertMatch(
                SwapMatchEntity(
                    id = "m-${h.removePrefix("@")}-${give.id}-${get.id}", handle = h,
                    giveListingId = give.id, getListingId = get.id, giveValue = valueOf(give), getValue = valueOf(get),
                    counterpartyHandle = get.owner.handle, threeWay = false, hidden = false, position = existing.size + i,
                ),
            )
        }
        found.size
    }

    // ============================================================ proposals

    /** Every proposal the member is part of, made or received. */
    val proposals: StateFlow<List<Proposal>> = combine(store.proposalRows, store.allItems, store.people, store.me) { rows, items, people, h ->
        val byId = items.associateBy { it.id }
        rows.filter { it.proposerHandle == h || it.counterpartyHandle == h }.mapNotNull { r ->
            Proposal(
                ref = r.ref,
                matchId = r.matchId,
                proposer = people[r.proposerHandle] ?: return@mapNotNull null,
                give = byId[r.giveListingId] ?: return@mapNotNull null,
                get = byId[r.getListingId] ?: return@mapNotNull null,
                giveValue = r.giveValue,
                getValue = r.getValue,
                counterparty = people[r.counterpartyHandle] ?: return@mapNotNull null,
                boot = r.boot,
                status = r.status,
                round = r.round,
                expiresAt = AppClock.fromMillis(r.expiresAt),
                updatedAt = AppClock.fromMillis(r.updatedAt),
            )
        }
    }.hot(emptyList())

    val proposal: StateFlow<Proposal?> = combine(session.session, proposals) { s, list ->
        list.firstOrNull { it.ref == s.proposalRef } ?: list.firstOrNull { it.open }
    }.hot(null)

    val negotiation: StateFlow<List<Message>> = combine(proposal, store.messages) { p, list ->
        if (p == null) emptyList() else list.filter { it.threadId == p.ref }
    }.hot(emptyList())

    /** Whether it is the signed-in member's turn on [p]: theirs to accept, counter or decline. */
    fun myMove(p: Proposal): Boolean = when (p.status) {
        ProposalStatus.SENT -> p.counterparty.handle == me
        ProposalStatus.COUNTERED, ProposalStatus.DRAFT -> p.proposer.handle == me
        else -> false
    }

    /** What the member could offer: their own gear, listed or kept for swaps. */
    fun myGear(): List<GearItem> = store.allItems.value
        .filter { it.owner.handle == me && it.status != ListingStatus.DRAFT }
        .sortedBy { if (it.id == Demo.burton.id) 0 else 1 }

    /** What the other side could give: the item matched first, then the rest of their gear. */
    fun theirGear(p: Proposal): List<GearItem> {
        val theirs = store.allItems.value.filter {
            it.owner.handle == p.counterparty.handle && it.id != p.get.id && it.status != ListingStatus.DRAFT
        }
        return (listOf(p.get) + theirs).distinctBy { it.id }.take(4)
    }

    /** "Propose swap" — Create a draft, or reopen the one already under way. */
    suspend fun proposeFromMatch(matchId: String): String? {
        val match = dao.match(matchId) ?: return null
        val existing = dao.openProposalFor(matchId)
        if (existing != null) {
            session.selectProposal(existing.ref)
            return existing.ref
        }
        val ref = nextProposalRef(dao.proposalRefs().toSet())
        val now = AppClock.nowMillis()
        dao.upsertProposal(
            SwapProposalEntity(
                ref = ref, matchId = matchId, proposerHandle = me,
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
        dao.upsertProposal(change(row).copy(updatedAt = AppClock.nowMillis()))
    }

    suspend fun setGive(item: GearItem) = editProposal { it.copy(giveListingId = item.id, giveValue = valueOf(item)) }

    suspend fun setGet(item: GearItem) = editProposal { it.copy(getListingId = item.id, getValue = valueOf(item)) }

    /**
     * "Send" — the proposer sends the offer (or a new one after a counter); the
     * counterparty sends a counter with the cash they would want. The cash comes
     * in with the send and is written in the same row update (D-14).
     */
    suspend fun send(boot: Int? = null) = edits.withLock {
        val p = proposal.value ?: return@withLock
        val row = dao.proposal(p.ref) ?: return@withLock
        val now = AppClock.nowMillis()
        val cash = (boot ?: row.boot).coerceAtLeast(0)
        val fromProposer = row.proposerHandle == me
        val round = if (row.status == ProposalStatus.DRAFT) 1 else (row.round + 1).coerceAtMost(5)
        val name = store.person(me).name.substringBefore(' ')
        db.withTransaction {
            dao.upsertProposal(
                row.copy(
                    boot = cash,
                    status = if (fromProposer) ProposalStatus.SENT else ProposalStatus.COUNTERED,
                    round = round,
                    updatedAt = now,
                ),
            )
            val plus = if (cash > 0) " + ${lkr(cash)}" else ""
            val text = if (fromProposer) "My ${p.give.shortTitle}$plus for your ${p.get.shortTitle}."
            else "I'd do it for your ${p.give.shortTitle}$plus."
            money.post(row.ref, me, text, ChatMessage.Kind.TEXT)
            val other = if (fromProposer) row.counterpartyHandle else row.proposerHandle
            money.notify(
                other, NotificationItem.Kind.SWAP,
                if (fromProposer) "$name sent you a swap proposal" else "$name countered your swap",
                "${p.give.shortTitle} for ${p.get.shortTitle}$plus · round $round of 5", "s22", row.ref,
            )
        }
    }

    /** "Accept" — agreed, and any cash goes into escrow from the proposer. */
    suspend fun accept() {
        val p = proposal.value ?: return
        val row = dao.proposal(p.ref) ?: return
        // Keep this swap on screen once it is no longer open.
        session.selectProposal(p.ref)
        val now = AppClock.nowMillis()
        val name = store.person(me).name.substringBefore(' ')
        db.withTransaction {
            dao.upsertProposal(row.copy(status = ProposalStatus.ACCEPTED, updatedAt = now))
            if (row.boot > 0) db.money().insertTxn(WalletTxnEntity(handle = row.proposerHandle, label = "Escrow · ${row.ref}", amount = -row.boot, createdAt = now))
            money.post(row.ref, MoneyRepository.SYSTEM, "Swap agreed · ${row.ref}", ChatMessage.Kind.SYSTEM)
            val other = if (row.proposerHandle == me) row.counterpartyHandle else row.proposerHandle
            val summary = "${p.give.shortTitle} for ${p.get.shortTitle}" + if (row.boot > 0) " · ${lkr(row.boot)} held in escrow" else ""
            money.notify(other, NotificationItem.Kind.SWAP, "$name accepted the swap", summary, "s22", row.ref)
            money.notify(me, NotificationItem.Kind.SWAP, "Swap agreed", summary, "s22", row.ref)
            db.evidence().earnBadge(row.proposerHandle, "Swap trader")
            db.evidence().earnBadge(row.counterpartyHandle, "Swap trader")
        }
    }

    /** "Decline" — turned down, and the other side is told. */
    suspend fun decline() {
        val p = proposal.value ?: return
        val row = dao.proposal(p.ref) ?: return
        val now = AppClock.nowMillis()
        dao.setProposalStatus(row.ref, ProposalStatus.DECLINED, now)
        val other = if (row.proposerHandle == me) row.counterpartyHandle else row.proposerHandle
        money.notify(other, NotificationItem.Kind.SWAP, "${store.person(me).name.substringBefore(' ')} declined the swap", "${p.give.shortTitle} for ${p.get.shortTitle}", "s22", row.ref)
    }

    /** Delete a draft outright, or withdraw one already sent. */
    suspend fun withdraw() {
        val p = proposal.value ?: return
        if (p.status == ProposalStatus.DRAFT) dao.deleteProposal(p.ref)
        else {
            dao.setProposalStatus(p.ref, ProposalStatus.WITHDRAWN, AppClock.nowMillis())
            money.notify(p.counterparty.handle, NotificationItem.Kind.SWAP, "${p.proposer.name.substringBefore(' ')} withdrew a swap", "${p.give.shortTitle} for ${p.get.shortTitle}", "s22", p.ref)
        }
    }

    private fun nextProposalRef(used: Set<String>): String {
        var n = 2215
        while ("SW-$n" in used) n++
        return "SW-$n"
    }

    // ============================================================ the three-way loop

    val cycle: StateFlow<Cycle?> = combine(store.cycleRows, store.allItems, store.people, store.me) { rows, items, people, h ->
        val byId = items.associateBy { it.id }
        rows.groupBy { it.cycleId }.entries.firstOrNull { (_, m) -> m.any { it.handle == h } }?.let { (id, members) ->
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

    /** The member's own seat: accept, decline, or take it back. */
    suspend fun setMyAcceptance(accepted: Boolean?) {
        val c = cycle.value ?: return
        dao.setCycleAcceptance(c.id, me, accepted)
        val name = store.person(me).name.substringBefore(' ')
        c.members.filter { it.person.handle != me }.forEach {
            money.notify(
                it.person.handle, NotificationItem.Kind.SWAP,
                when (accepted) {
                    true -> "$name accepted the three-way swap"
                    false -> "$name declined the three-way swap"
                    null -> "$name withdrew their answer on the three-way swap"
                },
                "Loop ${c.id}", "s23", c.id,
            )
        }
    }

    // ============================================================ the owner's console

    private val ownerItems: StateFlow<List<GearItem>> = combine(store.allItems, store.me) { list, h ->
        list.filter { it.owner.handle == h && it.status == ListingStatus.PUBLISHED }
    }.hot(emptyList())

    /** Every listing the member has made, drafts and paused ones included. */
    val myListings: StateFlow<List<GearItem>> = combine(store.allItems, store.me) { list, h ->
        list.filter { it.owner.handle == h }.sortedBy { it.status.ordinal }
    }.hot(emptyList())

    private val ownerBookings: StateFlow<List<Booking>> = combine(store.bookings, store.me) { list, h ->
        list.filter { it.owner.handle == h && it.status != BookingStatus.DRAFT }
    }.hot(emptyList())

    /** The units, in the order the calendar lists them. */
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
        /** Payouts per week, oldest first, as a fraction of the best week. */
        val weekly: List<Float>,
    )

    /** The owner's month, worked out from their payouts and bookings. */
    val dashboard: StateFlow<Dashboard> = combine(ownerBookings, units, store.people, store.txnRows, store.me) { bookings, unitList, people, txns, h ->
        val month = AppClock.today.withDayOfMonth(1)
        val payouts = txns.filter { it.amount > 0 && it.label.startsWith("Payout") }
        val thisMonth = payouts.filter { !AppClock.fromMillis(it.createdAt).toLocalDate().isBefore(month) }.sumOf { it.amount }
        val owed = bookings.filter { it.status in setOf(BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.RETURNED) && it.paymentMethod != MoneyRepository.PAY_CASH }
            .sumOf { it.ownerPayout }
        val weeks = (9 downTo 0).map { w ->
            val from = AppClock.today.minusWeeks(w + 1L)
            val to = AppClock.today.minusWeeks(w.toLong())
            payouts.filter { val d = AppClock.fromMillis(it.createdAt).toLocalDate(); d.isAfter(from) && !d.isAfter(to) }.sumOf { it.amount }
        }
        val best = weeks.maxOrNull()?.takeIf { it > 0 } ?: 1
        Dashboard(
            earnings = thisMonth,
            nextPayout = owed,
            occupancy = occupancyOf(unitList, bookings, AppClock.today),
            listings = unitList.map { it.item.id }.distinct().size,
            ratingText = people[h]?.takeIf { it.reviews > 0 }?.ratingText ?: "New",
            weekly = weeks.map { it.toFloat() / best },
        )
    }.hot(Dashboard(0, 0, 0, 0, "", List(10) { 0f }))

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
        val now = AppClock.now()
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
        val now = AppClock.now()
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

    /** A task to done; "Block dates" also creates the block it names. */
    suspend fun completeTask(taskId: String): String? {
        var message: String? = null
        val task = store.taskRows.value.firstOrNull { it.id == taskId }
        db.withTransaction {
            if (task?.action == "Block dates") {
                val day = dayIn(task.detail) ?: AppClock.today.plusDays(7)
                val listing = store.allItems.value.firstOrNull { it.owner.handle == me && it.units > 1 } ?: store.allItems.value.firstOrNull { it.owner.handle == me }
                if (listing != null) {
                    val unit = listing.units
                    if (dao.blockCount(listing.id, unit, day.toEpochDay()) == 0) {
                        dao.insertBlock(CalendarBlockEntity(listingId = listing.id, unit = unit, day = day.toEpochDay(), reason = "Service"))
                    }
                    message = "Blocked ${dayMonth(day)} on ${if (listing.units > 1) "${listing.shortTitle} #$unit" else listing.shortTitle} for its service"
                }
            }
            dao.setTaskDone(taskId, true)
        }
        return message
    }

    /** The first `12 Aug`-style date in [text], on or after today. */
    private fun dayIn(text: String): LocalDate? {
        val match = Regex("(\\d{1,2}) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)").find(text) ?: return null
        val (d, m) = match.destructured
        val month = java.time.Month.entries.first { it.name.take(3).equals(m, ignoreCase = true) }
        val today = AppClock.today
        val thisYear = runCatching { LocalDate.of(today.year, month, d.toInt()) }.getOrNull() ?: return null
        return if (thisYear.isBefore(today.minusDays(30))) thisYear.plusYears(1) else thisYear
    }

    /** "Write it" — Create the owner's review of a renter, then close the task. */
    suspend fun reviewRenter(taskId: String, renterHandle: String, listingId: String, stars: Int, body: String) {
        db.withTransaction {
            db.discovery().insertReview(
                ReviewEntity(
                    listingId = listingId, subjectHandle = renterHandle, authorHandle = me,
                    date = dayMonth(AppClock.today) + " " + AppClock.today.year, stars = stars.coerceIn(1, 5),
                    body = body.trim(), photos = emptyList(), ownerResponse = null, bookingRef = null, createdAt = AppClock.nowMillis(),
                ),
            )
            dao.setTaskDone(taskId, true)
            money.notify(renterHandle, NotificationItem.Kind.REVIEW, "${store.person(me).name.substringBefore(' ')} reviewed you", "★ $stars — “${body.trim().take(60)}”", "s32", null)
        }
    }

    // ============================================================ the calendar

    data class Conflict(val late: Booking, val next: Booking, val freeUnit: Int?)

    /** An overdue return on a unit that goes out again within a day. */
    val conflicts: StateFlow<List<Conflict>> = ownerBookings.map { bookings ->
        val now = AppClock.now()
        bookings.filter { it.status == BookingStatus.ACTIVE && it.returnAt.isBefore(now) }.mapNotNull { late ->
            val next = bookings.filter {
                it.item.id == late.item.id && it.unit == late.unit && it.ref != late.ref &&
                    it.status == BookingStatus.CONFIRMED && it.pickupAt.isAfter(now.minusHours(1)) && it.pickupAt.isBefore(now.plusDays(1))
            }.minByOrNull { it.pickupAt } ?: return@mapNotNull null
            val free = (1..late.item.units).filter { it != late.unit }.firstOrNull { u ->
                Availability.isRangeFree(next.span, AppClock.today, store.spansFor(next.item.id, u, next.ref), store.blockedFor(next.item.id, u))
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
                else -> Availability.stateOf(day, AppClock.today, confirmed, blocked).let { if (it == DayState.TODAY) DayState.AVAILABLE else it }
            }
            DayCell(day, state)
        }
    }

    fun blockAt(u: CalendarUnit, day: LocalDate): CalendarBlock? =
        store.blockRows.value.firstOrNull { it.listingId == u.item.id && it.unit == u.unit && it.day == day.toEpochDay() }
            ?.let { CalendarBlock(it.id, it.listingId, it.unit, LocalDate.ofEpochDay(it.day), it.reason) }

    /** Create a block on a free day. Refused on a held day. */
    suspend fun block(u: CalendarUnit, day: LocalDate, reason: String = "Owner block"): Long? {
        val held = store.spansNow(u.item.id, u.unit).any { day in it }
        if (held || day.isBefore(AppClock.today) || dao.blockCount(u.item.id, u.unit, day.toEpochDay()) > 0) return null
        return dao.insertBlock(CalendarBlockEntity(listingId = u.item.id, unit = u.unit, day = day.toEpochDay(), reason = reason))
    }

    suspend fun unblock(id: Long): CalendarBlockEntity? {
        val row = dao.block(id) ?: return null
        dao.deleteBlock(id)
        return row
    }

    suspend fun restoreBlock(row: CalendarBlockEntity) {
        dao.insertBlock(row.copy(id = 0))
    }

    /** "Move to unit #2" — the booking onto a free unit. */
    suspend fun moveToUnit(ref: String, unit: Int) {
        val row = db.money().booking(ref) ?: return
        db.money().upsertBooking(row.copy(unit = unit, updatedAt = AppClock.nowMillis()))
    }

    // ============================================================ requests

    val requests: StateFlow<List<Booking>> = ownerBookings
        .map { list -> list.filter { it.status == BookingStatus.REQUESTED }.sortedBy { it.respondBy } }
        .hot(emptyList())

    val ownerUpcoming: StateFlow<List<Booking>> = ownerBookings
        // A return waiting on the owner's check is still the owner's to do.
        .map { list -> list.filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.ACTIVE || it.status == BookingStatus.RETURNED }.sortedBy { it.start } }
        .hot(emptyList())

    val ownerPast: StateFlow<List<Booking>> = ownerBookings
        .map { list -> list.filter { it.status in setOf(BookingStatus.COMPLETED, BookingStatus.DECLINED, BookingStatus.CANCELLED) }.sortedByDescending { it.start } }
        .hot(emptyList())

    fun trustStats(handle: String) = store.trustStats(handle)

    /** Accept a request. The renter is told, and the pickup address released to them. */
    suspend fun accept(ref: String): Boolean {
        val row = db.money().booking(ref) ?: return false
        if (row.status != BookingStatus.REQUESTED) return false
        val span = Availability.Span(LocalDate.ofEpochDay(row.startDay), LocalDate.ofEpochDay(row.endDay))
        // Other open requests do not stop an acceptance; only confirmed holds and
        // blocks do, because accepting is the moment the dates become held.
        val confirmed = dao.confirmedOn(row.listingId, row.unit, ref)
            .map { Availability.Span(LocalDate.ofEpochDay(it.startDay), LocalDate.ofEpochDay(it.endDay)) }
        val free = Availability.isRangeFree(span, AppClock.today, confirmed, store.blocksNow(row.listingId, row.unit))
        if (!free) return false
        val now = AppClock.nowMillis()
        val item = store.item(row.listingId)
        val owner = store.person(row.ownerHandle).name.substringBefore(' ')
        db.withTransaction {
            db.money().upsertBooking(row.copy(status = BookingStatus.CONFIRMED, respondBy = null, updatedAt = now))
            money.notify(row.renterHandle, NotificationItem.Kind.BOOKING, "$owner accepted your request", "${item?.title ?: ref} · ${dateRange(span.start, span.end)} · $ref", "s12", ref)
            money.postDm(row.renterHandle, row.ownerHandle, MoneyRepository.SYSTEM, "Booking confirmed · $ref", ChatMessage.Kind.SYSTEM)
            money.postDm(row.renterHandle, row.ownerHandle, row.ownerHandle, "Pickup point · ${MoneyRepository.pickupAddress(row.ownerHandle)}", ChatMessage.Kind.LOCATION)
        }
        return true
    }

    /** Decline a request, refunding the renter in full. */
    suspend fun decline(ref: String) {
        val row = db.money().booking(ref) ?: return
        if (row.status != BookingStatus.REQUESTED) return
        val now = AppClock.nowMillis()
        val item = store.item(row.listingId)
        val owner = store.person(row.ownerHandle).name.substringBefore(' ')
        db.withTransaction {
            db.money().upsertBooking(row.copy(status = BookingStatus.DECLINED, respondBy = null, updatedAt = now))
            if (row.paymentMethod == MoneyRepository.PAY_WALLET) {
                val available = db.discovery().profileNow(row.renterHandle)?.walletAvailable ?: 0
                db.money().setWalletAvailable(row.renterHandle, available + row.total)
            }
            db.money().insertTxn(WalletTxnEntity(handle = row.renterHandle, label = "Refund · $ref declined", amount = row.total, createdAt = now))
            money.notify(row.renterHandle, NotificationItem.Kind.BOOKING, "$owner declined your request", "${item?.title ?: ref} · ${lkr(row.total)} refunded", "s31", ref)
            money.postDm(row.renterHandle, row.ownerHandle, MoneyRepository.SYSTEM, "Request declined · $ref", ChatMessage.Kind.SYSTEM)
        }
    }

    // ============================================================ listings

    val draftListing: StateFlow<GearItem?> = combine(session.session, store.allItems) { s, items ->
        s.draftListingId?.let { id -> items.firstOrNull { it.id == id } }
    }.hot(null)

    private suspend fun nextListingId(): String {
        var n = store.listingRows.value.size + 1
        while (db.discovery().listing("L-$n") != null) n++
        return "L-$n"
    }

    /** Market rates for one sport: what owners nearby charge a day for it. */
    data class Market(val count: Int, val low: Int, val median: Int, val high: Int, val suggested: Int)

    fun marketFor(sport: String, excluding: String? = null): Market {
        val prices = store.allItems.value
            .filter { it.sport == sport && it.status == ListingStatus.PUBLISHED && it.owner.handle != me && it.id != excluding }
            .map { it.pricePerDay }.sorted()
        if (prices.isEmpty()) return Market(0, 500, 1_000, 2_000, 1_000)
        fun at(q: Double) = prices[((prices.size - 1) * q).toInt()]
        val median = at(0.5)
        return Market(prices.size, prices.first(), median, prices.last(), median / 100 * 100)
    }

    /**
     * Create a new listing as a draft, owned by the signed-in member — from the
     * owner dashboard, or from an item in their gear closet.
     */
    suspend fun startListing(fromClosetId: Long? = null, sport: String? = null): String {
        val closet = fromClosetId?.let { dao.closetItem(it) }
        val id = nextListingId()
        val now = AppClock.nowMillis()
        val kind = closet?.sport ?: sport ?: "Surfing"
        val title = closet?.title ?: ""
        val market = marketFor(kind)
        db.discovery().upsertListing(
            ListingEntity(
                id = id,
                title = title.ifEmpty { "New ${kind.lowercase(Locale.ENGLISH)} listing" },
                shortTitle = closet?.shortTitle ?: title.ifEmpty { "New listing" },
                code = closet?.code ?: freeCode(title.ifEmpty { kind }),
                sizeLabel = closet?.sizeLabel ?: "—",
                grade = closet?.grade ?: Grade.B,
                pricePerDay = market.suggested,
                distanceKm = if (me == Seed.ME) 0.6 else (1.0 + (id.hashCode() and 0x7) * 0.4),
                photos = closet?.photoKey?.takeIf(Photos::isFile)?.let(::listOf) ?: emptyList(),
                ownerHandle = me,
                fit = Fit.GOOD,
                sport = kind,
                instantBook = false,
                delivery = false,
                deposit = (market.suggested * 8 / 1000 * 1000).coerceAtLeast(2_000),
                status = ListingStatus.DRAFT,
                units = 1,
                volumeL = null,
                frameBand = null,
                specs = emptyList(),
                included = listOf(closet?.shortTitle ?: "Item as photographed"),
                excluded = emptyList(),
                conditionPhotos = emptyList(),
                conditionNote = "New listing",
                conditionDate = dayMonth(AppClock.today) + " " + AppClock.today.year,
                extras = emptyList(),
                discounts = Demo.durationDiscounts.map { it.third },
                waiveDepositWithPlus = false,
                description = "",
                createdAt = now,
                sortOrder = 100 + store.listingRows.value.size,
            ),
        )
        if (closet != null) dao.upsertClosetItem(closet.copy(listingId = id))
        session.selectDraft(id)
        return id
    }

    /** An item code nobody else's tag uses: two letters from the title, four digits. */
    private suspend fun freeCode(title: String): String {
        val letters = title.filter(Char::isLetter).take(2).uppercase(Locale.ENGLISH).padEnd(2, 'G')
        var n = 1000 + (title.hashCode() and 0x7fff) % 9000
        while (db.discovery().codeCount("$letters-$n") > 0) n = if (n >= 9999) 1000 else n + 1
        return "$letters-$n"
    }

    /** Edit an existing listing through the same two screens. */
    fun editListing(id: String) = session.selectDraft(id)

    private suspend fun editDraft(change: (ListingEntity) -> ListingEntity) = edits.withLock {
        val id = session.draftListingId ?: return@withLock
        val row = db.discovery().listing(id) ?: return@withLock
        db.discovery().upsertListing(change(row))
    }

    /**
     * Put a photo in one of the seven slots — the angles the handover checks.
     * The first photo is the hero, so it is the thing being listed.
     */
    suspend fun setPhoto(slot: Int, photoKey: String) = editDraft { row ->
        val photos = row.photos.toMutableList()
        if (slot < photos.size) photos[slot] = photoKey else photos.add(photoKey)
        row.copy(photos = photos, conditionPhotos = photos.take(3), conditionDate = dayMonth(AppClock.today) + " " + AppClock.today.year)
    }

    /** Delete a photo. The rest close up behind it. Returns the key removed. */
    suspend fun removePhoto(slot: Int): String? {
        var removed: String? = null
        editDraft { row ->
            if (slot >= row.photos.size) row else {
                val photos = row.photos.toMutableList()
                removed = photos.removeAt(slot)
                row.copy(photos = photos, conditionPhotos = photos.take(3))
            }
        }
        return removed
    }

    /**
     * The title (the short form is what cards and tags print), the size on the
     * tag, the sport, the grade and the description — one write (D-15).
     */
    suspend fun setDetails(title: String, size: String, sport: String? = null, grade: Grade? = null, description: String? = null) = editDraft { row ->
        val clean = title.trim().ifEmpty { row.title }
        row.copy(
            title = clean,
            shortTitle = clean.take(28),
            sizeLabel = size.trim().ifEmpty { row.sizeLabel }.uppercase(Locale.ENGLISH),
            sport = sport ?: row.sport,
            grade = grade ?: row.grade,
            description = description?.trim() ?: row.description,
            fit = if ((sport ?: row.sport) in setOf("Camping", "Trekking", "Badminton", "Football")) Fit.UNKNOWN else Fit.GOOD,
        )
    }

    suspend fun setPrice(price: Int) = editDraft { it.copy(pricePerDay = price.coerceIn(100, 20_000)) }

    suspend fun setDeposit(deposit: Int) = editDraft { it.copy(deposit = deposit.coerceIn(0, 200_000)) }

    suspend fun setDiscount(index: Int, on: Boolean) = editDraft { row ->
        val d = row.discounts.toMutableList().also { while (it.size < 3) it.add(false) }
        d[index] = on
        row.copy(discounts = d)
    }

    suspend fun setWaiveDeposit(waive: Boolean) = editDraft { it.copy(waiveDepositWithPlus = waive) }

    suspend fun setInstantBook(on: Boolean) = editDraft { it.copy(instantBook = on) }

    suspend fun setDelivery(on: Boolean) = editDraft { it.copy(delivery = on) }

    /** "Save draft" — the timestamp records the save. */
    suspend fun saveDraft() = editDraft { it.copy(createdAt = it.createdAt) }

    /** Publish. A closet item is marked as listed, and its swap matches can be found. */
    suspend fun publish(): String? {
        val id = session.draftListingId ?: return null
        val row = db.discovery().listing(id) ?: return null
        db.withTransaction {
            db.discovery().upsertListing(row.copy(status = ListingStatus.PUBLISHED, createdAt = if (row.status == ListingStatus.DRAFT) AppClock.nowMillis() else row.createdAt))
            dao.closetForListing(id)?.let { dao.upsertClosetItem(it.copy(listed = true)) }
        }
        return id
    }

    /** Pause a published listing (out of search, bookings kept), or put it back. */
    suspend fun setPaused(id: String, paused: Boolean) {
        val row = db.discovery().listing(id) ?: return
        if (row.ownerHandle != me || row.status == ListingStatus.DRAFT) return
        db.discovery().setListingStatus(id, if (paused) ListingStatus.PAUSED else ListingStatus.PUBLISHED)
    }

    /**
     * Delete one of the member's listings. Refused while a booking still holds
     * dates on it, because the renter's record would lose its gear.
     */
    suspend fun deleteListing(id: String): Boolean {
        val row = db.discovery().listing(id) ?: return false
        if (row.ownerHandle != me) return false
        val live = store.bookingRows.value.any { it.listingId == id && it.status.holdsDates }
        if (live) return false
        db.withTransaction {
            db.discovery().deleteListing(id)
            dao.closetForListing(id)?.let { dao.upsertClosetItem(it.copy(listingId = null, listed = false)) }
        }
        if (session.draftListingId == id) session.selectDraft(null)
        return true
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

    // ============================================================ the gear closet

    val closet: StateFlow<List<ClosetItem>> = store.closetRows.map { rows ->
        val now = AppClock.nowMillis()
        rows.map {
            ClosetItem(it.id, it.title, it.shortTitle, it.photoKey, it.sport, it.sizeLabel, it.grade, it.code, it.listed, it.listingId, (now - it.idleSince) / MoneyRepository.DAY_MS)
        }
    }.hot(emptyList())

    suspend fun addClosetItem(title: String, sport: String, sizeLabel: String, grade: Grade, photoKey: String? = null): Long {
        val clean = title.trim().ifEmpty { "Untitled gear" }
        return dao.insertClosetItem(
            ClosetItemEntity(
                ownerHandle = me,
                title = clean,
                shortTitle = clean.take(28),
                photoKey = photoKey ?: Photos.forSport(sport),
                sport = sport,
                sizeLabel = sizeLabel.trim().ifEmpty { "—" }.uppercase(Locale.ENGLISH),
                grade = grade,
                code = freeCode(clean),
                listed = false,
                listingId = null,
                idleSince = AppClock.nowMillis(),
                position = dao.maxClosetPosition(me) + 1,
            ),
        )
    }

    suspend fun updateClosetItem(id: Long, title: String, sizeLabel: String, grade: Grade, photoKey: String? = null) {
        val row = dao.closetItem(id) ?: return
        val clean = title.trim().ifEmpty { row.title }
        dao.upsertClosetItem(
            row.copy(
                title = clean, shortTitle = clean.take(28),
                sizeLabel = sizeLabel.trim().ifEmpty { row.sizeLabel }.uppercase(Locale.ENGLISH),
                grade = grade, photoKey = photoKey ?: row.photoKey,
            ),
        )
    }

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
