package com.gearloop.app.data.repo

import com.gearloop.app.data.Phase
import com.gearloop.app.data.db.SessionDao
import com.gearloop.app.data.db.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This phone's state: who is signed in, and what they have selected — the
 * listing on the detail screen, the booking the handover screens are about,
 * the proposal, the claim, the conversation.
 *
 * Selection happens immediately before navigation — tap a card, open its
 * screen — so it is held in memory and read synchronously, and the next screen
 * draws the right thing on its first frame. Every change is also written to the
 * database, so the selection and the sign-in survive the process being killed,
 * which Android is entitled to do at any time.
 */
class SessionStore(private val dao: SessionDao, private val scope: CoroutineScope) {

    private val state = MutableStateFlow(fresh())
    val session: StateFlow<SessionEntity> = state.asStateFlow()

    private val writes = Mutex()

    /** The signed-in member's handle, or "" when nobody is signed in. */
    val me: StateFlow<String> = state.map { it.currentHandle.orEmpty() }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "")

    /** Read synchronously by the write paths; "" when nobody is signed in. */
    val handle: String get() = state.value.currentHandle.orEmpty()

    val signedIn: Boolean get() = state.value.currentHandle != null

    suspend fun load() {
        dao.session()?.let { state.value = it }
        profile.value = null
    }

    fun update(change: (SessionEntity) -> SessionEntity) {
        val next = change(state.value)
        state.value = next
        scope.launch { writes.withLock { dao.save(next) } }
    }

    /** Replace outright, waiting for the write — used when the database is reseeded. */
    suspend fun replace(next: SessionEntity) {
        state.value = next
        profile.value = null
        writes.withLock { dao.save(next) }
    }

    /**
     * Whose public profile is open: the owner from a listing, or the member
     * themself from their own Profile tab. Held in memory only; if the process
     * dies there, the public profile falls back to the owner of the listing in
     * view.
     */
    private val profile = MutableStateFlow<String?>(null)
    val profileHandle: StateFlow<String?> = profile.asStateFlow()
    fun selectProfile(handle: String) { profile.value = handle }

    /** Sign in as [handle], clearing whatever the previous member had open. */
    suspend fun signIn(handle: String) {
        val next = state.value.copy(
            currentHandle = handle,
            welcomeSeen = true,
            bookingRef = null,
            proposalRef = null,
            claimRef = null,
            draftListingId = null,
            threadId = "",
            capturePhase = Phase.PICKUP,
        )
        state.value = next
        profile.value = null
        writes.withLock { dao.save(next) }
    }

    suspend fun signOut() {
        val next = state.value.copy(currentHandle = null, bookingRef = null, proposalRef = null, claimRef = null, draftListingId = null, threadId = "")
        state.value = next
        profile.value = null
        writes.withLock { dao.save(next) }
    }

    fun markWelcomeSeen() = update { it.copy(welcomeSeen = true) }
    fun setThemeMode(mode: Int) = update { it.copy(themeMode = mode.coerceIn(0, 2)) }

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

    companion object {
        /** A phone nobody has used yet: signed out, welcome pages not seen. */
        fun fresh() = SessionEntity(
            currentHandle = null,
            welcomeSeen = false,
            themeMode = 0,
            listingId = "torq",
            bookingRef = null,
            proposalRef = null,
            claimRef = null,
            threadId = "",
            draftListingId = null,
            capturePhase = Phase.PICKUP,
            bookingSeq = 0,
        )
    }
}
