package com.gearloop.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.Demo
import com.gearloop.app.data.ListingStatus
import com.gearloop.app.data.ProposalStatus
import com.gearloop.app.data.repo.Seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Member 4 — Exchange & the Supply Side. Test cases TC-51 to TC-68.
 *
 * Swaps from the match feed to the three-way loop, then the owner's console
 * (Kavitha's account on the same device, DV-01) and the renter's gear closet.
 */
@RunWith(AndroidJUnit4::class)
class M4ExchangeSupplyTest : FunctionalTest() {

    private fun proposal(ref: String) = db { exchange().proposal(ref) }
    private fun listing(id: String) = db { discovery().listing(id) }
    private fun drafts() = db { discovery().listings().first() }.filter { it.status == ListingStatus.DRAFT }
    private fun closet() = db { exchange().closet().first() }
    private fun aug(day: Int) = LocalDate.of(2026, 8, day).toEpochDay()
    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasContentDescription(label))

    /** TC-51 · S-19 · Read the matches; Update one to hidden, and undo; My swaps lists the open proposal. */
    @Test
    fun tc51_match_is_hidden_and_restored() {
        launch("s19")
        waitForText("We found 5 trades using your Burton Custom 158.")
        shot("s19_matches")
        c.toaster.undoDisplayMs = 5_000
        compose.onAllNodesWithText("Not for me")[0].tap() // the Santa Cruz, Dinesh
        until { db { exchange().match("m1") }?.hidden == true }
        waitForText("We found 4 trades using your Burton Custom 158.")
        tapText("Undo")
        until { db { exchange().match("m1") }?.hidden == false }
        tapText("My swaps (1)")
        waitForText("SW-2214 · DINESH · ROUND 2")
    }

    /** TC-52 · S-19 → S-20 · Create a draft proposal from a match. */
    @Test
    fun tc52_propose_creates_a_draft() {
        launch("s19")
        // m1 already has SW-2214 and says "Continue proposal", so the first "Propose swap" is m2.
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Propose swap"))
        compose.onAllNodesWithText("Propose swap")[0].tap()
        until { proposal("SW-2215") != null }
        val p = proposal("SW-2215")!!
        assertEquals(ProposalStatus.DRAFT, p.status)
        assertEquals("m2", p.matchId)
        waitForText("Check the balance")
    }

    /** TC-53 · S-20 · Update what is given and what is wanted; each side carries its valuation. */
    @Test
    fun tc53_proposal_sides_are_updated() {
        runBlocking { c.exchange.proposeFromMatch("m2") }
        launch("s20")
        waitForText("Check the balance")
        // His own helmet: this was Kavitha's Giro, offered as his (D-30).
        tapText(Demo.myHelmet.shortTitle)
        until { proposal("SW-2215")?.giveListingId == Demo.myHelmet.id }
        assertEquals(c.exchange.valueOf(c.store.item(Demo.myHelmet.id)!!), proposal("SW-2215")!!.giveValue)
        shot("s20_build")
        tapText(Demo.burton.shortTitle)
        until { proposal("SW-2215")?.giveListingId == "burton" }
    }

    /** TC-54 · S-21 · Update the cash with the slider (by its accessibility action), then Send. */
    @Test
    fun tc54_boot_is_set_and_proposal_sent() {
        runBlocking { c.exchange.proposeFromMatch("m3") } // Burton 42,000 for Kavitha's kayak 44,000
        launch("s21")
        waitForText("Is this a fair trade?")
        desc("Cash you add").performSemanticsAction(SemanticsActions.SetProgress) { it(0.08f) }
        // max is 25,000 here, so 8 % is 2,000 after the 500-rupee rounding.
        waitForText("You pay Kavitha LKR 2,000")
        shot("s21_fairness")
        tapText("Send proposal")
        until { proposal("SW-2215")?.status == ProposalStatus.SENT }
        assertEquals(2_000, proposal("SW-2215")!!.boot)
        assertTrue(db { money().messages().first() }.any { it.threadId == "SW-2215" && it.body.contains("+ LKR 2,000") })
        waitForText("Waiting for Kavitha.")
    }

    /** TC-55 · S-22 · Update the countered proposal to accepted; the cash goes into escrow. */
    @Test
    fun tc55_accepting_puts_the_boot_in_escrow() {
        launch("s22")
        waitForText("Dinesh has answered. Your move.")
        shot("s22_proposal")
        tapText("Accept")
        until { proposal(Demo.SWAP_REF)?.status == ProposalStatus.ACCEPTED }
        assertTrue(db { money().walletTxns().first() }.any { it.label == "Escrow · ${Demo.SWAP_REF}" && it.amount == -Demo.BOOT_AMOUNT })
        assertTrue(db { money().notifications().first() }.any { it.title == "Swap agreed with Dinesh" })
        waitForText("Agreed. Arrange the handover.")
    }

    /** TC-56 · S-22 · Delete a draft; Update a sent proposal to withdrawn. */
    @Test
    fun tc56_draft_deleted_and_sent_withdrawn() {
        runBlocking { c.exchange.proposeFromMatch("m2") }
        launch("s22")
        tapText("Delete draft")
        waitForText("Delete this draft?")
        tapText("Delete")
        until { proposal("SW-2215") == null }
        runBlocking { c.exchange.proposeFromMatch("m3") }
        until { c.exchange.proposal.value?.ref == "SW-2215" }
        runBlocking { c.exchange.send() }
        relaunch("s22")
        tapText("Withdraw")
        waitForText("Withdraw this proposal?")
        tapText("Withdraw proposal")
        until { proposal("SW-2215")?.status == ProposalStatus.WITHDRAWN }
    }

    /** TC-57 · S-23 · The outcome is one sentence (FR-13); Update my seat: accept, take back, decline, undo. */
    @Test
    fun tc57_cycle_seat_is_updated() {
        launch("s23")
        waitForText("You give your Burton Custom 158 to Dinesh and get Ayesha's ${Demo.ayeshaKayak.shortTitle}. No cash changes hands.")
        shot("s23_cycle")
        fun mine() = db { exchange().cycleMembers().first() }.first { it.handle == Seed.ME }.accepted
        tapText("Accept my part")
        until { mine() == true }
        tapText("Take my acceptance back")
        until { mine() == null }
        tapText("Decline")
        until { mine() == false }
        waitForText("SOMEONE DECLINED · THE LOOP IS OFF")
        tapText("Undo my decline")
        until { mine() == null }
    }

    /** TC-58 · S-24 · Read the derived dashboard; Update the service task, which Creates its calendar block. */
    @Test
    fun tc58_block_dates_task_creates_the_block() {
        launch("s24")
        waitForText("Return overdue by 6 h")
        waitForText("Service due in 3 days")
        shot("s24_dashboard")
        tapText("Block dates")
        until { db { exchange().blocksFor("trek", 2) }.any { it.day == aug(12) } }
        until { db { exchange().tasks().first() }.first { it.id == "service" }.done }
        assertFalse(exists("Service due in 3 days"))
    }

    /** TC-59 · S-24 · Create the owner's review of a renter; the task closes. */
    @Test
    fun tc59_owner_reviews_the_renter() {
        launch("s24")
        tapText("Write it")
        waitForText("Review Mohamed")
        field("Your review").performTextInput("Returned the bike clean and on time. Would rent to Mohamed again.")
        tapText("Post review")
        until { c.store.reviewRows.value.any { it.subjectHandle == "@m_rizwan" && it.authorHandle == Seed.OWNER } }
        until { db { exchange().tasks().first() }.first { it.id == "review" }.done }
    }

    /** TC-60 · S-24 → S-25 → S-26 · Create a listing, Update its title, size and photos, publish it into search. */
    @Test
    fun tc60_new_listing_is_published_into_search() {
        launch("s24")
        tapText("Add a listing")
        waitForText("New listing")
        val id = drafts().single().id
        field("Title").performTextReplacement("Firewire Seaside 5'6\"")
        field("Size on the tag").performTextReplacement("5'6\"")
        desc("Deck photo slot").tap()
        until { listing(id)?.photos?.size == 1 }
        shot("s25_photos")
        tapText("Continue")
        until { listing(id)?.title == "Firewire Seaside 5'6\"" }
        assertEquals("5'6\"", listing(id)!!.sizeLabel)
        tapText("Publish listing")
        until { listing(id)?.status == ListingStatus.PUBLISHED }
        assertEquals(id, db { discovery().rails().first() }.filter { it.rail == "surf" }.minByOrNull { it.position }!!.listingId)
        // Search had seven results before; the new board is the eighth.
        relaunch("s05")
        waitForText("8 results")
    }

    /** TC-61 · S-25 · Delete a draft that was never published; Delete a photo from a slot. */
    @Test
    fun tc61_draft_photo_removed_and_draft_deleted() {
        launch("s25")
        until { drafts().size == 1 }
        val id = drafts().single().id
        desc("Deck photo slot").tap()
        desc("Bottom photo slot").tap()
        until { listing(id)?.photos?.size == 2 }
        desc("Bottom photo slot").tap()
        waitForText("Bottom photo")
        tapText("Remove")
        until { listing(id)?.photos?.size == 1 }
        tapText("Delete this draft")
        until { listing(id) == null }
    }

    /** TC-62 · S-24 → S-26 · Update a live listing's price from the dashboard insight. */
    @Test
    fun tc62_kayak_price_is_edited() {
        val before = listing("kayak")!!.pricePerDay
        launch("s24")
        tapText("Review pricing")
        waitForText(Demo.kayak.shortTitle)
        shot("s26_pricing")
        desc("Decrease").tap()
        desc("Decrease").tap()
        tapText("Save changes")
        until { listing("kayak")?.pricePerDay == before - 200 }
        assertEquals(ListingStatus.PUBLISHED, listing("kayak")!!.status)
    }

    /** TC-63 · S-27 · Create a block on a free day and Delete it; booked days are not offered. */
    @Test
    fun tc63_calendar_block_and_unblock() {
        launch("s27")
        tapText("Block dates")
        val torq = Demo.torq.shortTitle
        desc("$torq, 7 Aug, available").tap()
        until { db { exchange().blocksFor("torq", 1) }.any { it.day == aug(7) } }
        until { descExists("$torq, 7 Aug, blocked") }
        desc("$torq, 7 Aug, blocked").tap()
        until { db { exchange().blocksFor("torq", 1) }.none { it.day == aug(7) } }
        // Dinesh holds 14–16 Aug; the booked cell carries no action at all.
        assertTrue(descExists("$torq, 15 Aug, booked"))
        assertFalse(compose.onAllNodes(hasContentDescription("$torq, 15 Aug, booked") and androidx.compose.ui.test.hasClickAction())
            .fetchSemanticsNodes().isNotEmpty())
        shot("s27_calendar")
    }

    /** TC-64 · S-27 · The overdue-return conflict is read from real bookings; Update the next booking onto unit #2. */
    @Test
    fun tc64_conflict_is_resolved_by_moving_units() {
        launch("s27")
        waitForText("overdue", substring = true)
        tapText("Move to unit #2")
        until { db { money().booking("GL-4C2J8") }?.unit == 2 }
        until { !exists("Move to unit #2") }
    }

    /** TC-65 · S-28 · Update one request to confirmed and another to declined. */
    @Test
    fun tc65_requests_are_accepted_and_declined() {
        launch("s28")
        waitForText("Requests (3)")
        shot("s28_requests")
        compose.onAllNodesWithText("Accept")[0].tap() // GL-2K8R5, the soonest to expire
        until { db { money().booking("GL-2K8R5") }?.status == BookingStatus.CONFIRMED }
        waitForText("Requests (2)")
        compose.onAllNodesWithText("Decline")[0].tap() // now GL-7Q3M1
        waitForText("Decline this request?")
        tapText("Decline request")
        until { db { money().booking("GL-7Q3M1") }?.status == BookingStatus.DECLINED }
        waitForText("Requests (1)")
    }

    /** TC-66 · S-11 → S-28 → S-31 · A renter's request reaches the owner, and the acceptance reaches the renter. */
    @Test
    fun tc66_request_round_trip_between_accounts() {
        val ref = book("trek") // not instant-book: a request
        runBlocking { c.exchange.decline("GL-2K8R5"); c.exchange.decline("GL-7Q3M1") }
        launch("s28")
        waitForText("Requests (2)")
        compose.onAllNodesWithText("Accept")[0].tap()
        until { db { money().booking(ref) }?.status == BookingStatus.CONFIRMED }
        assertTrue(db { money().notifications().first() }.any { it.title == "Kavitha accepted your request" && it.ref == ref })
        relaunch("s31")
        tapText("Upcoming (3)")
        waitForText("PICKUP IN 3 DAYS") // status badges are set in uppercase
    }

    /** TC-67 · S-36 · Create, Update and Delete (with Undo) an item in the gear closet. */
    @Test
    fun tc67_closet_item_crud() {
        launch("s36")
        desc("Add gear").tap()
        waitForText("Add to your closet")
        field("What is it?").performTextInput("Rip Curl 4/3 wetsuit")
        field("Size").performTextInput("m")
        tapText("Add")
        until { closet().any { it.title == "Rip Curl 4/3 wetsuit" } }
        assertEquals("M", closet().first { it.title == "Rip Curl 4/3 wetsuit" }.sizeLabel)
        shot("s36_closet")
        tapText("Rip Curl 4/3 wetsuit")
        field("Size").performTextReplacement("L")
        tapText("Save")
        until { closet().first { it.title == "Rip Curl 4/3 wetsuit" }.sizeLabel == "L" }
        c.toaster.undoDisplayMs = 5_000
        tapText("Rip Curl 4/3 wetsuit")
        tapText("Remove")
        until { closet().none { it.title == "Rip Curl 4/3 wetsuit" } }
        tapText("Undo")
        until { closet().any { it.title == "Rip Curl 4/3 wetsuit" } }
    }

    /** TC-68 · S-36 → S-25 → S-26 · An idle closet item is listed: the listing is the renter's, the item is marked listed. */
    @Test
    fun tc68_idle_item_is_listed_from_the_closet() {
        launch("s36")
        waitForText("Your kayak hasn't moved in 94 days")
        tapText("List it")
        until { drafts().any { it.ownerHandle == Seed.ME } }
        val id = drafts().first { it.ownerHandle == Seed.ME }.id
        waitForText("YOUR CLOSET")
        tapText("Continue")
        tapText("Publish listing")
        until { listing(id)?.status == ListingStatus.PUBLISHED }
        until { closet().first { it.listingId == id }.listed }
        waitForText("Gear closet")
        assertNull(drafts().firstOrNull { it.id == id })
    }
}
