package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.CaptureGuide
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Phase
import com.gearloop.app.data.db.GearDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writing the sample marketplace, and putting it back.
 *
 * A new install writes [Seed.World] once — members, listings, bookings,
 * conversations — and opens signed out, on the welcome pages. Later launches
 * read what is there. [reset] clears every table and writes the marketplace
 * again; the functional tests start from it, and so does a usability session.
 *
 * The scenarios fast-forward to the middle of a rental or to an open claim.
 * They are not fixtures written into tables — each one runs the same repository
 * calls the screens make, in order.
 */
class DemoData(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val money: MoneyRepository,
    private val evidence: EvidenceRepository,
) {

    enum class Scenario { MID_RENTAL, CLAIM_OPEN }

    /** First launch writes the sample marketplace; every later launch reads it. */
    suspend fun start() {
        if (db.accounts().count() == 0) seed() else session.load()
        store.awaitLoaded()
    }

    suspend fun seed() {
        val w = Seed.World()
        val s = db.seed()
        db.withTransaction {
            s.accounts(w.accounts())
            s.people(w.people())
            s.profiles(w.profiles())
            s.sports(w.sports())
            s.listings(w.listings())
            s.reviews(w.reviews())
            s.trips(w.trips())
            s.tripItems(w.tripItems())
            s.bookings(w.bookings())
            s.txns(w.walletTxns())
            s.notifications(w.notifications())
            s.messages(w.messages())
            s.claims(w.claims())
            s.verification(w.verificationSteps())
            s.badges(w.badges())
            s.matches(w.matches())
            s.proposals(w.proposals())
            s.cycle(w.cycle())
            s.blocks(w.blocks())
            s.tasks(w.tasks())
            s.closet(w.closet())
        }
        session.replace(SessionStore.fresh())
    }

    /** Everything back to the sample marketplace, optionally signed in as [signInAs]. */
    suspend fun reset(signInAs: String? = Seed.ME) {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        seed()
        if (signInAs != null) session.signIn(signInAs)
        store.awaitLoaded()
    }

    suspend fun play(scenario: Scenario) {
        reset()
        val ref = bookTheTorq()
        handOver(ref)
        if (scenario == Scenario.CLAIM_OPEN) returnWithDamage(ref)
    }

    /** Booking the Torq, through the same calls the listing and checkout make. */
    private suspend fun bookTheTorq(): String {
        session.selectListing(Demo.torq.id)
        money.startBooking(Demo.torq.id)
        money.signWaiver(Demo.nirmal.name)
        val result = money.confirmDraft()
        return requireNotNull((result as? MoneyRepository.ConfirmResult.Booked)?.ref) { "the Torq should be bookable on its default dates, got $result" }
    }

    /** Tag checked, seven frames, the pickup mark, both signatures. */
    private suspend fun handOver(ref: String) {
        evidence.select(ref)
        evidence.enterCode(ref, Seed.handoverCode(ref))
        evidence.startCapture(Phase.PICKUP)
        val angles = CaptureGuide.anglesFor(Demo.torq.sport)
        angles.forEachIndexed { i, angle -> evidence.capture(ref, Phase.PICKUP, angle, FRAMES[i % FRAMES.size]) }
        evidence.addMark(ref, Phase.PICKUP, Demo.PICKUP_MARK.area, Demo.PICKUP_MARK.severity, Demo.PICKUP_MARK.note, 0.5f, 0.86f)
        evidence.signAsRenter(ref)
        evidence.ownerSigns(ref)
    }

    /** The return frames, the new ding, and the claim raised from it. */
    private suspend fun returnWithDamage(ref: String) {
        evidence.startCapture(Phase.RETURN)
        val angles = CaptureGuide.anglesFor(Demo.torq.sport)
        angles.forEachIndexed { i, angle -> evidence.capture(ref, Phase.RETURN, angle, FRAMES[i % FRAMES.size]) }
        val markId = evidence.addMark(ref, Phase.RETURN, Demo.RETURN_MARK.area, DamageMark.Severity.FUNCTIONAL, Demo.RETURN_MARK.note, 0.5f, 0.86f)
        evidence.finishReturn(ref)
        evidence.raiseClaim(markId)
    }

    private companion object {
        /** Bundled condition photographs standing in for camera frames when a test fast-forwards. */
        val FRAMES = listOf("torq_2", "torq_5", "cond_3", "cond_1", "torq_4", "torq_4", "cond_2")
    }
}
