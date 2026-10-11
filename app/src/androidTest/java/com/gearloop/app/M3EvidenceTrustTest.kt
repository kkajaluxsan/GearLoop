package com.gearloop.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gearloop.app.data.BookingStatus
import com.gearloop.app.data.ClaimStatus
import com.gearloop.app.data.DamageMark
import com.gearloop.app.data.Demo
import com.gearloop.app.data.Phase
import com.gearloop.app.data.VerificationStep
import com.gearloop.app.data.repo.DemoData
import com.gearloop.app.data.repo.Seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Member 3 — Evidence & Trust. Test cases TC-36 to TC-50.
 *
 * The handover chain from the hub to settlement, then the identity screens.
 * Preconditions that belong to another member's flow (booking the board) are
 * reached through the same repository calls those screens make.
 */
@RunWith(AndroidJUnit4::class)
class M3EvidenceTrustTest : FunctionalTest() {

    private val ref = Demo.BOOKING_REF
    private fun booking() = db { money().booking(ref) }
    private fun frames(phase: Phase) = db { evidence().captures().first() }.filter { it.bookingRef == ref && it.phase == phase }
    private fun marks() = db { evidence().marksFor(ref) }
    private fun aug(day: Int) = LocalDate.of(2026, 8, day).toEpochDay()
    private val shutter = hasContentDescription("Capture ", substring = true) and hasClickAction()

    /** The board booked and the handshake done: where S-15 starts. */
    private fun handedOver(): String {
        val ref = book()
        runBlocking { c.evidence.enterCode(ref, Seed.handoverCode(ref)) }
        return ref
    }

    /** Seven passing frames for [phase], as the shutter makes them. */
    private fun captureAll(phase: Phase) = shoot(ref, phase)

    /** Mid-rental, the return frames shot and a new functional mark on the tail, returned. */
    private fun returnedWithChange(): Long {
        play(DemoData.Scenario.MID_RENTAL)
        captureAll(Phase.RETURN)
        return runBlocking {
            val id = c.evidence.addMark(ref, Phase.RETURN, Demo.RETURN_MARK.area, DamageMark.Severity.FUNCTIONAL, Demo.RETURN_MARK.note, 0.5f, 0.86f)
            c.evidence.finishReturn(ref)
            id
        }
    }

    /** TC-36 · S-13 · Read the paid booking and its address (FR-15); Update the checklist; the handover opens a day before pickup. */
    @Test
    fun tc36_handover_hub_checklist_and_unlock() {
        book()
        launch("s13")
        waitForText("27 Temple Rd, Nallur")
        tapText("Bring photo ID")
        until { booking()?.checklist == listOf(0) }
        tapText("Charge your phone for the photos")
        until { booking()?.checklist == listOf(0, 2) }
        tapText("Bring photo ID")
        until { booking()?.checklist == listOf(2) }
        shot("s13_handover")
        // Three days before pickup the handover is locked, and says so.
        text("Start handover").assertIsNotEnabled()
        waitForText("Handover opens 24 h before pickup")
        // On the morning of the pickup it opens.
        clockAt(LocalDate.ofEpochDay(booking()!!.startDay).atTime(6, 0))
        relaunch("s13")
        text("Start handover").assertIsEnabled()
        tapText("Start handover")
        waitForText("Scan the tag on the Torq", substring = true)
    }

    /** TC-37 · S-14 · A wrong code is refused with a reason; the right one Updates both handshake times. */
    @Test
    fun tc37_code_fallback_rejects_then_accepts() {
        book()
        launch("s14")
        tapText("Type the code instead")
        waitForText("Type the code")
        val right = Seed.handoverCode(ref)
        val wrong = if (right == "111111") "222222" else "111111"
        compose.onNode(hasSetTextAction()).performTextInput(wrong)
        tapText("Check")
        waitForText("That code doesn't match this booking")
        assertNull(booking()!!.handshakeRenterAt)
        compose.onNode(hasSetTextAction()).performTextReplacement(right)
        tapText("Check")
        until { booking()?.handshakeOwnerAt != null }
        assertNotNull(booking()!!.handshakeRenterAt)
        waitForText("It's the right gear")
    }

    /**
     * TC-38 · S-14 · The tag names the gear: another item's tag is refused by
     * name, and this one's code, however it is typed, records both sides at
     * once. The live scan reads the same text off the QR (TagCodeTest); a test
     * can't hold a tag up to the camera.
     */
    @Test
    fun tc38_scan_records_both_sides() {
        book()
        launch("s14")
        val torq = c.store.allItems.value.first { it.id == "torq" }
        val other = c.store.allItems.value.first { it.id != "torq" && it.code.isNotBlank() }
        tapText("Type the code instead")
        compose.onNode(hasSetTextAction()).performTextInput(other.code)
        tapText("Check")
        waitForText("That tag is ${other.code}", substring = true)
        assertNull(booking()!!.handshakeRenterAt)
        // Lower case and without the dash, it is still the code printed on the tag.
        compose.onNode(hasSetTextAction()).performTextReplacement(torq.code.lowercase().replace("-", ""))
        tapText("Check")
        until { booking()?.handshakeRenterAt != null && booking()?.handshakeOwnerAt != null }
        waitForText("It's the right gear")
        shot("s14_handshake")
        tapText("Check the gear together")
        waitForText("Check it together")
    }

    /**
     * TC-39 · S-15 · Create seven frames; Continue appears only when all seven
     * pass. The frames go in through the call the shutter makes once the camera
     * has a photo, since a test can't point the camera at the board.
     */
    @Test
    fun tc39_seven_angles_are_captured() {
        handedOver()
        launch("s15")
        waitForText("0 / 7")
        // FR-10: the reason for the sequence is given at the moment of the ask.
        waitForText("Anything you record now can never be claimed for later", substring = true)
        assertFalse(exists("Continue"))
        val angles = anglesOf(ref)
        shoot(ref, Phase.PICKUP, angles.dropLast(1))
        waitForText("6 / 7")
        assertFalse(exists("Continue"))
        shoot(ref, Phase.PICKUP, angles)
        waitForText("7 / 7")
        assertTrue(frames(Phase.PICKUP).all { it.ok })
        shot("s15_capture")
        tapText("Continue")
        waitForText("Both of you agree this is the condition")
    }

    /**
     * TC-40 · S-15 · A frame the photo check fails blocks the record (FR-08);
     * Delete it and shoot it again. The failed frame is a real photograph, too
     * dark to see anything in, put through the same steps the shutter's photo
     * takes — saved, measured, recorded with the check's verdict — so it is the
     * check that fails it, not a tap.
     */
    @Test
    fun tc40_failed_frame_is_cleared_and_reshot() {
        handedOver()
        captureAll(Phase.PICKUP)
        runBlocking {
            val file = com.gearloop.app.data.PhotoStore.newFile(app, "frame")
            val dark = android.graphics.Bitmap.createBitmap(640, 480, android.graphics.Bitmap.Config.ARGB_8888)
                .apply { eraseColor(android.graphics.Color.BLACK) }
            java.io.FileOutputStream(file).use { dark.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            val key = requireNotNull(com.gearloop.app.data.PhotoStore.adoptCapture(file))
            val check = com.gearloop.app.data.PhotoStore.check(app, key)
            assertFalse(check.ok)
            c.evidence.capture(ref, Phase.PICKUP, anglesOf(ref).first(), key, check.ok)
        }
        launch("s15")
        until { frames(Phase.PICKUP).count { !it.ok } == 1 }
        waitForText("Not clear enough — retake")
        assertFalse(exists("Continue"))
        tapText("Clear frame")
        until { frames(Phase.PICKUP).size == 6 }
        // The live camera is back for that angle; the reshot frame goes in as the shutter would put it.
        until { compose.onAllNodes(shutter).fetchSemanticsNodes().isNotEmpty() }
        shoot(ref, Phase.PICKUP)
        until { frames(Phase.PICKUP).let { f -> f.size == 7 && f.all { it.ok } } }
        waitForText("Continue")
    }

    /** TC-41 · S-15b · Create a pickup mark, Update its severity, Delete it and Undo. */
    @Test
    fun tc41_damage_mark_create_update_delete() {
        handedOver()
        launch("s15b")
        waitForText("Mark damage")
        tapText("Save this mark")
        until { marks().size == 1 }
        // A pickup record opens on cosmetic: the mark that protects the renter.
        assertEquals(DamageMark.Severity.COSMETIC, marks().single().severity)
        relaunch("s15b")
        tapText("Tail · surface mark")
        waitForText("Edit mark")
        tapText("Functional")
        tapText("Save changes")
        until { marks().singleOrNull()?.severity == DamageMark.Severity.FUNCTIONAL }
        relaunch("s15b")
        tapText("Tail · functional damage")
        c.toaster.undoDisplayMs = 5_000
        tapText("Delete")
        until { marks().isEmpty() }
        tapText("Undo")
        until { marks().size == 1 }
    }

    /** TC-42 · S-16 · Update: both signatures, drawn on the pad, start the rental, and the app moves to S-17 on its own. */
    @Test
    fun tc42_signing_starts_the_rental() {
        handedOver()
        captureAll(Phase.PICKUP)
        launch("s16")
        waitForText("Both of you agree this is the condition")
        sign("Nirmal")
        until { booking()?.renterSignedAt != null }
        sign("Kavitha")
        until(8_000) { booking()?.status == BookingStatus.ACTIVE }
        assertNotNull(booking()!!.ownerSignedAt)
        assertTrue(db { money().messages().first() }.any { it.body == "Condition record signed by both · $ref" })
        waitForText("UNTIL RETURN")
    }

    /** Open [name]'s signature pad on S-16, draw a signature, and sign. */
    private fun sign(name: String) {
        tapText("Sign as $name")
        desc("Signature pad for $name").performTouchInput {
            swipe(centerLeft, center, 250)
            swipe(center, Offset(right * 0.8f, top + height * 0.3f), 250)
        }
        tapText("Sign")
        compose.waitForIdle()
    }

    /** TC-43 · S-17 · Update the return date a day at a time, until the next booking's buffer stops it. */
    @Test
    fun tc43_extend_until_the_calendar_says_no() {
        play(DemoData.Scenario.MID_RENTAL)
        launch("s17")
        waitForText("UNTIL RETURN")
        shot("s17_active")
        // Each extra day is quoted before anything is charged.
        tapText("Extend a day")
        waitForText("Keep it until 11 Aug?")
        tapText("Extend")
        until { booking()?.endDay == aug(11) }
        val charged = booking()!!.total - 9_240
        assertTrue(db { money().walletTxns().first() }.any { it.label.startsWith("Extension · $ref +1 day") && it.amount == -charged })
        tapText("Extend a day")
        waitForText("Keep it until 12 Aug?")
        tapText("Extend")
        until { booking()?.endDay == aug(12) }
        // A 13 Aug return would leave no turnaround day before Dinesh's 14 Aug pickup.
        tapText("Extend a day")
        Thread.sleep(1_000)
        assertEquals(aug(12), booking()!!.endDay)
    }

    /** TC-44 · S-17 · Update the return checklist, ticking and unticking. */
    @Test
    fun tc44_return_checklist_updates() {
        play(DemoData.Scenario.MID_RENTAL)
        launch("s17")
        // The checklist opens unfolded, so its items are there to tick straight away.
        waitForText("Rinse it with fresh water")
        tapText("Rinse it with fresh water")
        until { booking()?.returnChecklist == listOf(0) }
        tapText("Straps and leashes attached")
        until { booking()?.returnChecklist == listOf(0, 2) }
        tapText("Rinse it with fresh water")
        until { booking()?.returnChecklist == listOf(2) }
        waitForText("1 / 4")
    }

    /** TC-45 · S-17 → S-15 → S-15b → S-18 · Create return frames and a new mark; the rental waits on the owner. */
    @Test
    fun tc45_return_with_a_new_mark_waits_for_the_owner() {
        play(DemoData.Scenario.MID_RENTAL)
        launch("s17")
        tapText("Start return")
        waitForText("Check it on return")
        // The seven return frames, through the call the shutter makes.
        shoot(ref, Phase.RETURN)
        waitForText("7 / 7")
        tapText("Mark damage")
        tapText("Save this mark")
        until { marks().any { it.phase == Phase.RETURN } }
        tapText("Continue")
        until { booking()?.status == BookingStatus.RETURNED }
        waitForText("Kavitha is checking 1 change")
        waitForText("CHANGED")
        shot("s18_return")
    }

    /** TC-46 · S-18b → S-18 · Update: "It's the same" settles the rental; then rate the owner on S-18 (Create, Delete). */
    @Test
    fun tc46_same_resolves_and_settles() {
        val markId = returnedWithChange()
        val before = db { discovery().person(Seed.ME) }!!.completedRentals
        // Whether a difference is new is the owner's call.
        signInAs(Seed.OWNER)
        c.evidence.select(ref)
        launch("s18b")
        waitForText("Difference recorded on the tail")
        shot("s18b_diff")
        tapText("It's the same")
        until { booking()?.status == BookingStatus.COMPLETED }
        assertTrue(db { evidence().mark(markId) }!!.resolved)
        assertTrue(db { money().notifications().first() }.any { it.title.startsWith("Rental complete") })
        assertEquals(before + 1, db { discovery().person(Seed.ME) }!!.completedRentals)
        // Settled, S-18 takes the renter's rating: Create a star-only review, then Delete it.
        signInAs(Seed.ME)
        c.evidence.select(ref)
        relaunch("s18")
        desc("5 stars").tap()
        until { booking()?.rating == 5 }
        assertTrue(c.store.reviewRows.value.any { it.bookingRef == ref && it.stars == 5 && it.subjectHandle == Seed.OWNER })
        tapText("Remove my rating")
        until { booking()?.rating == null }
        assertTrue(c.store.reviewRows.value.none { it.bookingRef == ref })
    }

    /** TC-47 · S-18b · Create a claim from the return mark; a cosmetic mark alone is refused. */
    @Test
    fun tc47_raise_claim_from_the_change() {
        val markId = returnedWithChange()
        signInAs(Seed.OWNER)
        c.evidence.select(ref)
        launch("s18b")
        tapText("Raise a claim")
        until { db { money().claim("CL-3081") } != null }
        val claim = db { money().claim("CL-3081") }!!
        assertEquals(ClaimStatus.OPEN, claim.status)
        assertEquals(ref, claim.bookingRef)
        // The estimate is set from the board's value and the rubric, never above the value.
        assertTrue(claim.amount in 1..claim.itemValue)
        waitForText("Claim CL-3081")
        // Raising it twice finds the same claim rather than filing another.
        assertEquals("CL-3081", runBlocking { c.evidence.raiseClaim(markId) })
        // The rubric: cosmetic marks never trigger a claim on their own.
        val cosmetic = runBlocking { c.evidence.addMark(ref, Phase.RETURN, "Deck", DamageMark.Severity.COSMETIC, "Wax scuff", 0.5f, 0.5f) }
        assertNull(runBlocking { c.evidence.raiseClaim(cosmetic) })
        assertEquals(5, db { money().claimRefs().size })
    }

    /** TC-48 · S-32 · Read the score from its parts; Update which earned badges are shown. */
    @Test
    fun tc48_trust_score_and_badges() {
        launch("s32")
        waitForText("742")
        waitForText("58 points to GOLD")
        // The one unverified step is offered from here (hidden before D-12 was fixed).
        waitForText("Continue verification")
        shot("s32_trust")
        compose.onNode(isToggleable() and hasText("10 rentals")).tap()
        until { db { evidence().badges().first() }.first { it.handle == Seed.ME && it.name == "10 rentals" }.featured }
        compose.onNode(isToggleable() and hasText("First loop")).tap()
        until { !db { evidence().badges().first() }.first { it.handle == Seed.ME && it.name == "First loop" }.featured }
        // Unearned badges are not controls at all.
        assertFalse(compose.onAllNodes(isToggleable() and hasText("Gold host")).fetchSemanticsNodes().isNotEmpty())
    }

    /** TC-49 · S-41 → S-32 · Update the address step to done (the score moves); Delete the address, and Undo. */
    @Test
    fun tc49_address_is_verified_then_removed() {
        launch("s41")
        waitForText("Five of six done")
        tapText("Verify now")
        waitForText("Verify your home address")
        compose.onNode(hasSetTextAction()).performTextInput("11 Stanley Rd, Jaffna")
        tapText("Verify")
        until { db { discovery().profileNow(Seed.ME) }?.address == "11 Stanley Rd, Jaffna" }
        assertEquals(VerificationStep.State.DONE, db { evidence().verificationStepsNow(Seed.ME) }.last().state)
        waitForText("All six done")
        // D-12: the address belongs to the home-address step only, not to "Email address" as well.
        assertEquals(1, compose.onAllNodesWithText("Remove this address").fetchSemanticsNodes().size)
        shot("s41_verification")
        relaunch("s32")
        waitForText("772")
        waitForText("28 points to GOLD")
        assertFalse(exists("Continue verification"))
        relaunch("s41")
        c.toaster.undoDisplayMs = 5_000
        tapText("Remove this address")
        until { db { discovery().profileNow(Seed.ME) }?.address == null }
        val steps = db { evidence().verificationStepsNow(Seed.ME) }
        assertEquals(VerificationStep.State.CURRENT, steps.last().state)
        assertEquals(VerificationStep.State.DONE, steps.first().state)
        tapText("Undo")
        until { db { discovery().profileNow(Seed.ME) }?.address == "11 Stanley Rd, Jaffna" }
    }

    /** TC-50 · S-37 · Read the selected listing's owner, never their contact details; Update a listing's saved state. */
    @Test
    fun tc50_public_profile_of_the_listing_owner() {
        c.session.selectListing("nsp")
        launch("s37")
        waitForText(Demo.ayesha.name)
        c.session.selectListing("torq")
        relaunch("s37")
        waitForText(Demo.kavitha.name)
        waitForText("Exact location and contact details are never shown", substring = true)
        assertFalse(exists("27 Temple Rd", substring = true))
        shot("s37_public_profile")
        desc("Save Torq Mod Fish 6'6\"").tap()
        until { db { discovery().isSaved(Seed.ME, "torq") } == 1 }
        tapText("Message Kavitha")
        until { c.session.threadId == Seed.OWNER }
        waitForText("Usually replies in", substring = true)
    }
}
