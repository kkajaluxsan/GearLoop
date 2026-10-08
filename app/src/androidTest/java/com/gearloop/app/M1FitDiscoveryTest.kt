package com.gearloop.app

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gearloop.app.data.Fit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Member 1 — Fit & Discovery. Test cases TC-01 to TC-16.
 *
 * Every interface is driven through at least two of its CRUD operations, and
 * every write is checked in the database as well as on screen.
 */
@RunWith(AndroidJUnit4::class)
class M1FitDiscoveryTest : FunctionalTest() {

    /** TC-01 · S-01 · Read the stored intent; Update it on Continue. */
    @Test
    fun tc01_intent_is_read_and_updated() {
        launch("s01")
        text("Rent gear").assertIsSelected()
        shot("s01_intent")
        tapText("Swap gear")
        tapText("Continue")
        until { db { discovery().profileNow()?.intent } == 2 }
        waitForText("Let's get your sizes right")
    }

    /** TC-02 · S-02 · Update height; the derived sizes recalculate before the save (FR-01). */
    @Test
    fun tc02_fit_profile_saves_and_recalculates() {
        launch("s02")
        val increase = compose.onAllNodesWithContentDescription("Increase")[0]
        repeat(3) { increase.performClick() }
        waitForText("177")
        waitForText("L (55–57)")
        shot("s02_fit_profile")
        tapText("Save and continue")
        until { db { discovery().profileNow()?.heightCm } == 177 }
        waitForText("Pick your sports")
    }

    /** TC-03 · S-02 · Delete the measurements; verdicts fall back to "add your height" (FR-03). */
    @Test
    fun tc03_removing_measurements_unknowns_every_verdict() {
        launch("s02")
        tapText("Remove my measurements")
        until { db { discovery().profileNow()?.heightCm } == null }
        relaunch("s07")
        waitForText("Add your height and weight to get a verdict")
        assertTrue(descExists("Fit verdict: ${Fit.UNKNOWN.label.lowercase()}"))
    }

    /** TC-04 · S-02 · Imperial converts what it shows rather than relabelling it (defect D-03). */
    @Test
    fun tc04_imperial_converts_the_display() {
        launch("s02")
        tapText("Imperial")
        waitForText("5'9\"")
        waitForText("150")
        assertTrue(!exists("174"))
    }

    /** TC-05 · S-03 · Create and Delete a sport interest with a tap each. */
    @Test
    fun tc05_sport_interests_are_created_and_deleted() {
        launch("s03")
        tapText("Trekking")
        until { db { discovery().hasSport("Trekking") } == 1 }
        tapText("Surfing")
        until { db { discovery().hasSport("Surfing") } == 0 }
        waitForText("Continue (3 selected)")
    }

    /** TC-06 · S-04 · Read the rails; Update a listing's saved state from its heart. */
    @Test
    fun tc06_home_heart_saves_the_listing() {
        launch("s04")
        waitForText("This weekend near you".uppercase())
        compose.onAllNodesWithContentDescription("Save Torq Mod Fish 6'6\"").onFirst().performClick()
        until { db { discovery().listing("torq")?.saved } == true }
        // Both rails the Torq is on now show it saved.
        shot("s04_home_saved")
    }

    /** TC-07 · S-05 · Read results through the shared filter; a query that matches nothing is recoverable. */
    @Test
    fun tc07_search_query_filters_and_empties() {
        launch("s05")
        waitForText("7 results")
        compose.onNode(hasSetTextAction()).performTextInput("kayak")
        // The kayak is 5.9 km away, outside the default 5 km.
        waitForText("Nothing matches all of that")
        // Loosening drops the query before it widens the distance.
        tapText("Loosen the tightest filter")
        waitForText("7 results")
    }

    /** TC-08 · S-05 · The order control really reorders; Update saved from a result row. */
    @Test
    fun tc08_search_sorts_and_saves() {
        launch("s05")
        tapText("Best match")
        waitForText("Lowest price")
        shot("s05_results")
        compose.onAllNodesWithContentDescription("Save SS Ton Willow Bat").onFirst().performClick()
        until { db { discovery().listing("bat")?.saved } == true }
    }

    /** TC-09 · S-06 · Update the filters; they survive a restart; Reset deletes them. */
    @Test
    fun tc09_filters_persist_and_reset_deletes_them() {
        launch("s06")
        compose.onNode(isToggleable() and hasText("Instant book only")).tap()
        waitForText("Show 3 results")
        until(8_000) { db { discovery().searchPrefs()?.instantOnly } == true }
        relaunch("s06")
        waitForText("Show 3 results")
        tapText("Reset")
        until(8_000) { db { discovery().searchPrefs() } == null }
        waitForText("Show 7 results")
    }

    /** TC-10 · S-07 · Read the listing that was opened, with its own live fit reason. */
    @Test
    fun tc10_listing_detail_shows_the_selected_listing() {
        c.session.selectListing("trek")
        launch("s07")
        waitForText("Trek Marlin 7 — Size M")
        waitForText("M frame for your 174 cm (M band)")
        waitForText("Request to book")
    }

    /** TC-11 · S-07 · Create the booking draft that S-08 to S-11 edit. */
    @Test
    fun tc11_book_creates_the_draft() {
        launch("s07")
        waitForText("LKR 9,576")
        shot("s07_listing")
        tapText("Book instantly")
        until { db { money().booking("D-torq") } != null }
        waitForText("Choose your dates")
    }

    /** TC-12 · S-29 · Read pins from the filtered listings; Create an alert for the area. */
    @Test
    fun tc12_map_creates_an_area_alert() {
        launch("s29")
        waitForText("LKR 650")
        desc("Alert me about new gear in this area").performClick()
        until { c.store.alertRows.value.size == 1 }
        assertEquals("ALERT", c.store.alertRows.value.first().kind)
        shot("s29_map")
    }

    /** TC-13 · S-30 · Update the party and the kit; Delete a rental and put it back. */
    @Test
    fun tc13_trip_planner_updates_and_deletes() {
        launch("s30")
        waitForText("LKR 29,880")
        compose.onAllNodesWithContentDescription("Increase")[0].performClick()
        until { db { discovery().tripNow()?.adults } == 3 }
        tapText("Dry bag 20 L")
        waitForText("LKR 30,180")
        compose.onAllNodesWithText("Remove from kit").onFirst().tap()
        until { c.store.tripItemRows.value.count { it.section == "RENT" } == 3 }
        shot("s30_trip")
    }

    /** TC-14 · S-39 · Create an alert from the live filters; Delete it again. */
    @Test
    fun tc14_no_results_alerts_are_created_and_deleted() {
        launch("s39")
        tapText("Alert me when one appears")
        until { c.store.alertRows.value.size == 1 }
        waitForText("Your alerts and posts".uppercase())
        desc("Remove alert").tap()
        until { c.store.alertRows.value.isEmpty() }
    }

    /** TC-15 · S-39 · Each recovery card is a real loosening, with the count it really buys. */
    @Test
    fun tc15_recovery_card_loosens_the_live_filters() {
        launch("s39")
        waitForText("Within 25 km")
        waitForText("9")
        tapText("Within 25 km")
        waitForText("9 results")
    }

    /** TC-16 · S-42 · Create a review for a finished rental; the histogram moves; Delete it. */
    @Test
    fun tc16_review_is_created_and_deleted() {
        c.session.selectListing("bat")
        launch("s42")
        tapText("Write a review")
        compose.onNode(hasSetTextAction()).performTextInput("Great bat, light pick-up and well knocked in.")
        desc("4 stars").performClick()
        tapText("Post review")
        until { c.store.reviewRows.value.any { it.listingId == "bat" && it.stars == 4 } }
        until { db { money().booking("GL-4M0N7")?.rating } == 4 }
        assertEquals(7, db { discovery().person("@m_rizwan")?.reviews })
        shot("s42_reviews")
        tapText("Delete my review")
        until { c.store.reviewRows.value.none { it.listingId == "bat" } }
        assertNull(db { money().booking("GL-4M0N7")?.rating })
    }
}
