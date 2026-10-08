package com.gearloop.app.data.repo

import com.gearloop.app.data.Phase
import com.gearloop.app.data.db.SessionDao
import com.gearloop.app.data.db.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What the user has selected: the listing on S-07, the booking the handover
 * screens are about, the proposal on S-22, the claim on S-34.
 *
 * Selection happens immediately before navigation — tap a card, open S-07 — so
 * it is held in memory and read synchronously, and the next screen draws the
 * right listing on its first frame. Every change is also written to the
 * database, so the selection survives the process being killed in the middle of
 * a flow, which Android is entitled to do at any time.
 */
class SessionStore(private val dao: SessionDao, private val scope: CoroutineScope) {

    private val state = MutableStateFlow(Seed.session(0L))
    val session: StateFlow<SessionEntity> = state.asStateFlow()

    private val writes = Mutex()

    suspend fun load() {
        dao.session()?.let { state.value = it }
    }

    fun update(change: (SessionEntity) -> SessionEntity) {
        val next = change(state.value)
        state.value = next
        scope.launch { writes.withLock { dao.save(next) } }
    }

    /** Replace outright, waiting for the write — used when the demo is reset. */
    suspend fun replace(next: SessionEntity) {
        state.value = next
        profile.value = null
        writes.withLock { dao.save(next) }
    }

    /**
     * Whose public profile S-37 shows: the owner from a listing, or the renter
     * themself from their own Profile tab, which used to open someone else's
     * (D-28, DV-34).
     *
     * Held in memory only, so the schema stays at version 1. It is chosen the
     * moment before S-37 opens; if the process dies there, S-37 falls back to
     * the owner of the listing in view, which is what it always showed.
     */
    private val profile = MutableStateFlow<String?>(null)
    val profileHandle: StateFlow<String?> = profile.asStateFlow()
    fun selectProfile(handle: String) { profile.value = handle }

    val listingId: String get() = state.value.listingId
    val bookingRef: String? get() = state.value.bookingRef
    val proposalRef: String? get() = state.value.proposalRef
    val claimRef: String? get() = state.value.claimRef
    val threadId: String get() = state.value.threadId
    val draftListingId: String? get() = state.value.draftListingId
    val capturePhase: Phase get() = state.value.capturePhase

    fun selectListing(id: String) = update { it.copy(listingId = id) }
    fun selectBooking(ref: String?) = update { it.copy(bookingRef = ref) }
    fun selectProposal(ref: String?) = update { it.copy(proposalRef = ref) }
    fun selectClaim(ref: String?) = update { it.copy(claimRef = ref) }
    fun selectThread(id: String) = update { it.copy(threadId = id) }
    fun selectDraft(id: String?) = update { it.copy(draftListingId = id) }
    fun selectCapturePhase(phase: Phase) = update { it.copy(capturePhase = phase) }

    /** The next booking-reference index, advanced atomically with the write. */
    fun takeBookingSeq(): Int {
        val n = state.value.bookingSeq
        update { it.copy(bookingSeq = n + 1) }
        return n
    }
}
