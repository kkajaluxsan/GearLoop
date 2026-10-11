package com.gearloop.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ChatMessage
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Protection
import com.gearloop.app.data.repo.DemoData
import com.gearloop.app.data.repo.Seed
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Member 2 — Money & Commitment. Test cases TC-17 to TC-35.
 *
 * The rent flow from dates to confirmation, then the account screens that
 * money and disputes move through afterwards.
 */
@RunWith(AndroidJUnit4::class)
class M2MoneyCommitmentTest : FunctionalTest() {

    private val draftRef = "D-torq${Seed.ME}"
    private fun aug(day: Int) = LocalDate.of(2026, 8, day).toEpochDay()
    private fun draft() = db { money().booking(draftRef) }

    /** TC-17 · S-08 · Update the draft's dates with a first and a last tap. */
    @Test
    fun tc17_dates_are_updated_by_two_taps() {
        launch("s08")
        waitForText("Confirm 8 – 10 Aug · 3 days")
        desc("18 August, available").tap()
        desc("20 August, available").tap()
        until { draft()?.let { it.startDay == aug(18) && it.endDay == aug(20) } == true }
        waitForText("Confirm 18 – 20 Aug · 3 days")
        shot("s08_dates")
    }

    /** TC-18 · S-08 · Booked, buffer and blocked days are drawn from real rows and ignore taps. */
    @Test
    fun tc18_unavailable_days_ignore_taps() {
        launch("s08")
        waitForText("Confirm 8 – 10 Aug · 3 days")
        // GL-3D9X2 holds 14–16 Aug, its buffer is the 17th, and the owner blocked 24–25 Aug.
        assertTrue(descExists("15 August, booked"))
        assertTrue(descExists("17 August, turnaround day, not bookable"))
        assertTrue(descExists("24 August, blocked by the owner"))
        assertTrue(descExists("4 August, past"))
        desc("15 August, booked").tap()
        desc("24 August, blocked by the owner").tap()
        val row = draft()!!
        assertEquals(aug(8), row.startDay)
        assertEquals(aug(10), row.endDay)
    }

    /** TC-19 · S-09 · Update the plan; each plan states what is held and the most you could lose (FR-04, FR-05). */
    @Test
    fun tc19_protection_plan_is_updated() {
        launch("s09")
        waitForText("Continue with Plus")
        assertTrue(exists("HELD, NOT CHARGED"))
        assertTrue(exists("MOST YOU COULD LOSE"))
        text("Pro").tap()
        until { draft()?.protection == Protection.PRO }
        waitForText("Continue with Pro")
        compose.onNode(isSelectable() and hasText("Pro")).assertIsSelected()
        // Only Basic holds the deposit, and the draft's hold follows the plan.
        text("Basic").tap()
        until { draft()?.protection == Protection.BASIC }
        assertEquals(c.store.item("torq")!!.deposit, draft()!!.depositHold)
        shot("s09_protection")
    }

    /** TC-20 · S-10 · The gate says what is missing (FR-14); reading, ticking and signing Update the draft. */
    @Test
    fun tc20_waiver_gate_then_signature_is_stored() {
        launch("s10")
        waitForText("To continue: read to the end (0%) · tick 3 more · type your name")
        text("I accept and continue").assertIsNotEnabled()
        shot("s10_waiver_gate")
        compose.onNode(hasScrollAction() and hasAnyChild(hasText("GearLoop Rental Waiver", substring = true)))
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        waitForText("Read to the end")
        repeat(3) { compose.onAllNodes(isToggleable())[it].tap() }
        compose.onNode(hasSetTextAction()).apply { runCatching { performScrollTo() } }.performTextInput(Demo.nirmal.name)
        text("I accept and continue").assertIsEnabled()
        tapText("I accept and continue")
        until { draft()?.waiverName == Demo.nirmal.name }
        assertNotNull(draft()!!.waiverAt)
    }

    /** TC-21 · S-11 · Read the bill: the designed basket comes to LKR 9,240 and every line explains itself (FR-20). */
    @Test
    fun tc21_bill_totals_and_explains_itself() {
        launch("s11")
        waitForText("LKR 9,240")
        assertEquals(9_240, draft()!!.total)
        tapText("What is each line?")
        waitForText("10% of the rental and extras after discounts", substring = true)
        waitForText("FIRSTLOOP takes LKR 831 off your first booking.")
        shot("s11_review_pay")
    }

    /** TC-22 · S-11 · Update the extras and the payment method; the total is re-quoted from the rule. */
    @Test
    fun tc22_extras_and_payment_method_update_the_draft() {
        launch("s11")
        waitForText("LKR 9,240")
        compose.onNode(isToggleable() and hasText("Rash guard", substring = true)).tap()
        until { draft()?.includeExtras == false }
        waitForText("LKR 8,745")
        assertEquals(8_745, draft()!!.total)
        text("Cash at pickup").tap()
        until { draft()?.paymentMethod == com.gearloop.app.data.repo.MoneyRepository.PAY_CASH }
    }

    /** TC-23 · S-11 · Create the booking: one transaction writes the booking, payment, notification and messages. */
    @Test
    fun tc23_confirm_creates_the_booking_and_its_payment() {
        runBlocking { c.money.startBooking("torq"); c.money.signWaiver(Demo.nirmal.name) }
        launch("s11")
        tapText("Confirm and pay")
        waitForText("You're booked")
        val ref = Demo.BOOKING_REF
        until { db { money().booking(ref) }?.status == BookingStatus.CONFIRMED }
        assertEquals(9_240, db { money().booking(ref) }!!.total)
        assertNull(draft())
        assertTrue(db { money().walletTxns().first() }.any { it.label.startsWith("Booking $ref") && it.amount == -9_240 })
        assertTrue(db { money().notifications().first() }.any { it.handle == Seed.ME && it.ref == ref && it.title == "Kavitha's ${Demo.torq.shortTitle} is booked" && it.unread })
        assertTrue(db { money().notifications().first() }.any { it.handle == Seed.OWNER && it.ref == ref && it.unread })
        val thread = db { money().messages().first() }.filter { it.threadId == com.gearloop.app.data.repo.MoneyRepository.dmKey(Seed.ME, Seed.OWNER) }
        assertTrue(thread.any { it.kind == ChatMessage.Kind.SYSTEM && it.body == "Booking confirmed · $ref" })
        // FR-15: the address is released only once payment completes.
        assertTrue(thread.any { it.kind == ChatMessage.Kind.LOCATION && it.body == "Pickup point · 27 Temple Rd, Nallur" })
        shot("s12_confirmed")
    }

    /** TC-24 · S-11 · Delete the draft; nothing has been charged. */
    @Test
    fun tc24_discarding_deletes_the_draft() {
        launch("s11")
        until { draft() != null }
        tapText("Discard this booking")
        waitForText("Discard this booking?")
        tapText("Discard")
        until { draft() == null }
        assertTrue(db { money().walletTxns().first() }.none { it.label.contains(draftRef) })
    }

    /** TC-25 · S-12 · Update a confirmed booking to cancelled; the refund follows the stated rule. */
    @Test
    fun tc25_cancel_refunds_by_the_rule() {
        val ref = book()
        launch("s12")
        waitForText("You're booked")
        tapText("Cancel booking")
        waitForText("Cancel $ref?")
        // Booked a moment ago, so inside the free 24 hours.
        waitForText("You'll get LKR 9,240 of LKR 9,240 back.", substring = true)
        tapText("Cancel it")
        until { db { money().booking(ref) }?.status == BookingStatus.CANCELLED }
        assertTrue(db { money().walletTxns().first() }.any { it.label == "Refund · $ref cancelled" && it.amount == 9_240 })
        waitForText("Booking cancelled")
    }

    /** TC-26 · S-31 · Read the three tabs; Update a past rental out of history, and undo it. */
    @Test
    fun tc26_rentals_remove_from_history_and_undo() {
        launch("s31")
        waitForText("Upcoming (2)")
        tapText("Past")
        waitForText("RATE IT") // status badges are set in uppercase
        c.toaster.undoDisplayMs = 5_000
        compose.onAllNodesWithText("Remove from history")[0].tap()
        until { db { money().booking("GL-5X8T1") }?.hiddenFromHistory == true }
        shot("s31_rentals_past")
        tapText("Undo")
        until { db { money().booking("GL-5X8T1") }?.hiddenFromHistory == false }
    }

    /** TC-27 · S-11 → S-31 · Create a request for gear that is not instant-book, then withdraw it. */
    @Test
    fun tc27_request_is_sent_then_withdrawn() {
        runBlocking { c.money.startBooking("trek"); c.money.signWaiver(Demo.nirmal.name) }
        launch("s11")
        tapText("Send request")
        waitForText("Request sent")
        val ref = Demo.BOOKING_REF
        until { db { money().booking(ref) }?.status == BookingStatus.REQUESTED }
        relaunch("s31")
        tapText("Upcoming (3)")
        tapText("Withdraw request")
        waitForText("Withdraw this request?")
        tapText("Withdraw")
        until { db { money().booking(ref) }?.status == BookingStatus.CANCELLED }
        waitForText("Upcoming (2)")
    }

    /** TC-28 · S-33 · Read the thread; Create messages by typing and by attaching the booking reference. */
    @Test
    fun tc28_messages_are_sent() {
        c.money.openThread(Seed.OWNER)
        launch("s33")
        waitForText("Perfect, thanks. I'll book it for the weekend.")
        val body = "Can I pick it up at 8 instead?"
        compose.onNode(hasSetTextAction()).performTextInput(body)
        desc("Send").tap()
        until { db { money().messages().first() }.any { it.body == body && it.fromHandle == Seed.ME && it.threadId == com.gearloop.app.data.repo.MoneyRepository.dmKey(Seed.ME, Seed.OWNER) } }
        waitForText(body)
        desc("Attach the booking reference").tap()
        until { db { money().messages().first() }.any { it.body.startsWith("Re: GL-") } }
        shot("s33_thread")
    }

    /** TC-29 · S-33 · Delete one of your own messages with a long press; Undo restores it. Theirs can't be deleted. */
    @Test
    fun tc29_own_message_is_deleted_and_restored() {
        c.money.openThread(Seed.OWNER)
        launch("s33")
        val mine = "Perfect, thanks. I'll book it for the weekend."
        waitForText(mine)
        text("Hi Nirmal", substring = true).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
        c.toaster.undoDisplayMs = 5_000
        text(mine).apply { runCatching { performScrollTo() } }.performTouchInput { longClick() }
        waitForText("Delete this message?")
        tapText("Delete")
        until { db { money().messages().first() }.none { it.body == mine } }
        tapText("Undo")
        until { db { money().messages().first() }.any { it.body == mine } }
    }

    /** TC-30 · S-35 · Create a withdrawal that empties the balance, then a top-up. */
    @Test
    fun tc30_wallet_withdraw_and_top_up() {
        launch("s35")
        waitForText("LKR 6,480")
        tapText("Withdraw")
        tapText("Send to my bank")
        until { db { discovery().profileNow(Seed.ME) }?.walletAvailable == 0 }
        assertTrue(db { money().walletTxns().first() }.any { it.label == "Withdrawal to your bank" && it.amount == -6_480 })
        text("Withdraw").assertIsNotEnabled()
        tapText("Top up")
        waitForText("Top up your wallet")
        tapText("10,000")
        tapText("Top up LKR 10,000")
        until { db { discovery().profileNow(Seed.ME) }?.walletAvailable == 10_000 }
        assertTrue(db { money().walletTxns().first() }.any { it.label == "Top up from Visa ••••4471" && it.amount == 10_000 })
        waitForText("LKR 10,000")
        shot("s35_wallet")
    }

    /** TC-31 · S-38 · Update: mark all read; Delete a notification and undo it. */
    @Test
    fun tc31_notifications_marked_read_and_dismissed() {
        val ref = book() // adds a second unread notification
        launch("s38")
        waitForText(db { money().notifications().first() }.first { it.handle == Seed.ME && it.ref == ref }.title)
        assertEquals(2, db { money().notifications().first() }.count { it.handle == Seed.ME && it.unread })
        shot("s38_notifications")
        tapText("Mark all read")
        until { db { money().notifications().first() }.none { it.handle == Seed.ME && it.unread } }
        c.toaster.undoDisplayMs = 5_000
        desc("Dismiss Dinesh countered your swap").tap()
        until { db { money().notifications().first() }.none { it.title == "Dinesh countered your swap" } }
        tapText("Undo")
        until { db { money().notifications().first() }.any { it.title == "Dinesh countered your swap" } }
    }

    /** TC-32 · S-34 · Update a claim: contest it in your own words, then add evidence. */
    @Test
    fun tc32_claim_is_contested_with_evidence() {
        play(DemoData.Scenario.CLAIM_OPEN)
        launch("s34")
        waitForText("Claim CL-3081")
        shot("s34_claim")
        tapText("Contest this")
        waitForText("Why don't you agree?")
        val statement = "The ding was under the wax at pickup. See the tail frame."
        compose.onNode(hasSetTextAction()).performTextInput(statement)
        tapText("Contest")
        until { db { money().claim("CL-3081") }?.status == ClaimStatus.CONTESTED }
        assertEquals(statement, db { money().claim("CL-3081") }!!.renterStatement)
        tapText("Add my evidence")
        waitForText("Add evidence")
        val note = "Receipt for the wax job on 7 Aug, before the rental."
        compose.onNode(hasSetTextAction()).performTextInput(note)
        tapText("Add to claim")
        until { db { money().claim("CL-3081") }?.renterEvidence == note }
    }

    /** TC-33 · S-34 · Update: accept liability; the renter pays only what their plan caps (FR-05). */
    @Test
    fun tc33_accepting_liability_charges_the_capped_share() {
        play(DemoData.Scenario.CLAIM_OPEN)
        launch("s34")
        waitForText("Claim CL-3081")
        tapText("Accept liability")
        waitForText("Accept this claim?")
        tapText("Accept")
        until { db { money().claim("CL-3081") }?.status == ClaimStatus.ACCEPTED }
        val amount = db { money().claim("CL-3081") }!!.amount
        val share = minOf(amount, Protection.PLUS.excess!!)
        assertTrue(db { money().walletTxns().first() }.any { it.label == "Claim CL-3081 settled" && it.amount == -share })
        waitForText("You accepted this claim")
    }

    /** TC-34 · S-40 → S-34 · A ruling is refused without a reason (FR-19); issued with one, the renter reads it. */
    @Test
    fun tc34_ruling_needs_a_reason_and_reaches_the_renter() {
        play(DemoData.Scenario.CLAIM_OPEN)
        openClaimOnDesk()
        text("Issue ruling").assertIsNotEnabled()
        val reason = "The tail ding appears on the return frame only. Functional damage, capped by the rubric."
        compose.onNode(hasSetTextAction()).performTextInput(reason)
        text("Issue ruling").assertIsEnabled()
        shot("s40_admin")
        tapText("Issue ruling")
        until { db { money().claim("CL-3081") }?.status == ClaimStatus.DECIDED }
        val claim = db { money().claim("CL-3081") }!!
        assertEquals(reason, claim.rationale)
        assertTrue(claim.award!! <= claim.amount)
        assertTrue(db { money().notifications().first() }.any { it.title == "Ruling on claim CL-3081" })
        signInAs(Seed.ME)
        c.money.selectClaim("CL-3081")
        relaunch("s34")
        waitForText(reason)
    }

    /** TC-35 · S-40 · Update: ask for more evidence (the renter is told), then escalate. */
    @Test
    fun tc35_more_evidence_then_escalation() {
        play(DemoData.Scenario.CLAIM_OPEN)
        openClaimOnDesk()
        tapText("Request more evidence")
        until { db { money().claim("CL-3081") }?.status == ClaimStatus.MORE_EVIDENCE }
        assertTrue(db { money().notifications().first() }.any { it.title == "More evidence needed · CL-3081" })
        tapText("Escalate")
        until { db { money().claim("CL-3081") }?.status == ClaimStatus.ESCALATED }
        signInAs(Seed.ME)
        c.money.selectClaim("CL-3081")
        relaunch("s34")
        waitForText("With a senior adjudicator")
    }

    /** S-40 opens on the queue on a phone; open CL-3081 from it. */
    private fun openClaimOnDesk() {
        signInAs(Seed.ADMIN) // the claims desk is staff-only
        launch("s40")
        waitForText("CL-3081")
        tapText("CL-3081")
        waitForText("CL-3081 ·", substring = true)
    }

    private fun <T> runBlocking(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
}
