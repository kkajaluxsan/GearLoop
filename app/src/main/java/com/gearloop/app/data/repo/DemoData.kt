package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.DemoClock
import com.gearloop.app.data.Phase
import com.gearloop.app.data.db.GearDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeding, resetting, and the two prepared scenarios.
 *
 * "Reset the demo" on the cover now means what it says: the database is cleared
 * and seeded again, and the clock goes back to 09:14 on the 5th. That is what a
 * usability session starts from, so every participant meets the same app.
 *
 * The scenarios fast-forward to the middle of a rental or to an open claim.
 * They are not fixtures written into tables — each one runs the same repository
 * calls the screens make, in order, so reaching S-34 this way exercises exactly
 * the code a participant would.
 */
class DemoData(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    private val money: MoneyRepository,
    private val evidence: EvidenceRepository,
) {

    enum class Scenario(val title: String, val blurb: String) {
        MID_RENTAL(
            "Mid-rental",
            "GL-7K3M9 booked, the board checked together and signed for — S-17 is live.",
        ),
        CLAIM_OPEN(
            "After return, claim open",
            "The board is back with a new tail ding, and Kavitha has raised claim CL-3081.",
        ),
    }

    /** First launch seeds; every later launch picks the clock up where it was. */
    suspend fun start() {
        if (db.discovery().profileNow() == null) {
            seed()
        } else {
            session.load()
            DemoClock.anchorRealMillis = session.session.value.clockAnchor
        }
        store.awaitLoaded()
    }

    suspend fun seed() {
        val anchor = DemoClock.restart()
        val s = db.seed()
        db.withTransaction {
            s.people(Seed.people())
            s.profile(Seed.profile())
            s.sports(Seed.sports())
            s.listings(Seed.listings())
            s.rails(Seed.rails())
            s.reviews(Seed.reviews())
            s.trip(Seed.trip())
            s.tripItems(Seed.tripItems())
            s.bookings(Seed.bookings())
            s.txns(Seed.walletTxns())
            s.notifications(Seed.notifications())
            s.messages(Seed.messages())
            s.claims(Seed.claims())
            s.verification(Seed.verificationSteps())
            s.badges(Seed.badges())
            s.matches(Seed.matches())
            s.proposals(Seed.proposals())
            s.cycle(Seed.cycle())
            s.blocks(Seed.blocks())
            s.tasks(Seed.tasks())
            s.closet(Seed.closet())
        }
        session.replace(Seed.session(anchor))
    }

    /** Everything back to how it was designed. */
    suspend fun reset() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        seed()
        store.awaitLoaded()
    }

    suspend fun play(scenario: Scenario) {
        reset()
        val ref = bookTheTorq()
        handOver(ref)
        if (scenario == Scenario.CLAIM_OPEN) returnWithDamage(ref)
    }

    /** Flow A up to S-12, through the same calls S-07 to S-11 make. */
    private suspend fun bookTheTorq(): String {
        session.selectListing(Demo.torq.id)
        money.startBooking(Demo.torq.id)
        money.signWaiver(Demo.nirmal.name)
        return requireNotNull(money.confirmDraft()) { "the Torq should be bookable on the seeded dates" }
    }

    /** S-13 to S-16: handshake, seven angles, the pickup mark, both signatures. */
    private suspend fun handOver(ref: String) {
        evidence.select(ref)
        evidence.enterCode(ref, Seed.handoverCode(ref))
        evidence.startCapture(Phase.PICKUP)
        evidence.angles.forEach { evidence.capture(ref, Phase.PICKUP, it) }
        evidence.addMark(ref, Phase.PICKUP, Demo.PICKUP_MARK.area, Demo.PICKUP_MARK.severity, Demo.PICKUP_MARK.note, 0.5f, 0.86f)
        evidence.signAsRenter(ref)
        evidence.ownerSigns(ref)
    }

    /** S-17 to S-18b: return frames, the new ding, and the claim raised from it. */
    private suspend fun returnWithDamage(ref: String) {
        evidence.startCapture(Phase.RETURN)
        evidence.angles.forEach { evidence.capture(ref, Phase.RETURN, it) }
        val markId = evidence.addMark(ref, Phase.RETURN, Demo.RETURN_MARK.area, DamageMark.Severity.FUNCTIONAL, Demo.RETURN_MARK.note, 0.5f, 0.86f)
        evidence.finishReturn(ref)
        evidence.raiseClaim(markId)
    }
}
