package com.gearloop.app

import com.gearloop.app.data.repo.Seed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
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
        until { db { discovery().profileNow(Seed.ME)?.intent } == 2 }
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
        until { db { discovery().profileNow(Seed.ME)?.heightCm } == 177 }
        waitForText("Pick your sports")
    }

    /** TC-03 · S-02 · Delete the measurements; verdicts fall back to "add your height" (FR-03). */
    @Test
    fun tc03_removing_measurements_unknowns_every_verdict() {
        launch("s02")
        tapText("Remove my measurements")
        until { db { discovery().profileNow(Seed.ME)?.heightCm } == null }
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
        until { db { discovery().hasSport(Seed.ME, "Trekking") } == 1 }
        tapText("Surfing")
        until { db { discovery().hasSport(Seed.ME, "Surfing") } == 0 }
        waitForText("(3 selected)", substring = true)
    }

    /** TC-06 · S-04 · Read the rails; Update a listing's saved state from its heart. */
    @Test
    fun tc06_home_heart_saves_the_listing() {
        launch("s04")
        waitForText("Free near you".uppercase())
        // The first heart on the page, whichever listing the rails put first.
        val heart = compose.onAllNodesWithContentDescription("Save ", substring = true).onFirst()
        val label = heart.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first()
        val item = c.store.allItems.value.first { "Save ${it.shortTitle}" == label }
        heart.performClick()
        until { db { discovery().isSaved(Seed.ME, item.id) } == 1 }
        shot("s04_home_saved")
    }

    /** TC-07 · S-05 · Read results through the shared filter; a query that matches nothing is recoverable. */
    @Test
    fun tc07_search_query_filters_and_empties() {
        launch("s05")
        val everything = resultCount()
        waitForText(resultsLabel(everything))
        compose.onNode(hasSetTextAction()).performTextInput("kayak")
        waitForText(resultsLabel(resultCount { it.query = "kayak" }))
        // A query that matches nothing offers a way back rather than a blank page.
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("zqxwv")
        waitForText("Nothing matches all of that")
        // Loosening drops the query before anything else.
        tapText("Loosen the tightest filter")
        waitForText(resultsLabel(everything))
    }

    /** TC-08 · S-05 · The order control really reorders; Update saved from a result row. */
    @Test
    fun tc08_search_sorts_and_saves() {
        launch("s05")
        // All three orders are chips on show; picking one selects it.
        text("Best match").assertIsSelected()
        tapText("Lowest price")
        text("Lowest price").assertIsSelected()
        shot("s05_results")
        // The first heart in the reordered list, whichever listing is cheapest.
        val heart = compose.onAllNodesWithContentDescription("Save ", substring = true).onFirst()
        val label = heart.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first()
        val item = c.store.allItems.value.first { "Save ${it.shortTitle}" == label }
        heart.performClick()
        until { db { discovery().isSaved(Seed.ME, item.id) } == 1 }
    }

    /** TC-09 · S-06 · Update the filters; they survive a restart; Reset deletes them. */
    @Test
    fun tc09_filters_persist_and_reset_deletes_them() {
        launch("s06")
        val all = resultCount()
        val instant = resultCount { it.instantOnly = true }
        fun show(n: Int) = if (n == 1) "Show 1 result" else "Show $n results"
        compose.onNode(isToggleable() and hasText("Instant book only")).tap()
        waitForText(show(instant))
        until(8_000) { db { discovery().searchPrefs(Seed.ME)?.instantOnly } == true }
        relaunch("s06")
        waitForText(show(instant))
        tapText("Reset")
        until(8_000) { db { discovery().searchPrefs(Seed.ME) } == null }
        waitForText(show(all))
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
        until { db { money().booking("D-torq${Seed.ME}") } != null }
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
        until { c.discovery.trip.value != null }
        val adults = c.discovery.trip.value!!.adults
        val renting = c.store.tripItemRows.value.count { it.section == "RENT" }
        waitForText(com.gearloop.app.data.lkr(c.discovery.trip.value!!.total))
        compose.onAllNodesWithContentDescription("Increase")[1].performClick() // [0] is the trip's length
        until { db { discovery().tripNow(Seed.ME)?.adults } == adults + 1 }
        until { c.discovery.trip.value?.adults == adults + 1 }
        val before = c.discovery.trip.value!!.total
        tapText("Dry bag 20 L")
        waitForText(com.gearloop.app.data.lkr(before + 300))
        compose.onAllNodesWithText("Remove from kit").onFirst().tap()
        until { c.store.tripItemRows.value.count { it.section == "RENT" } == renting - 1 }
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
        val wider = resultCount { it.distanceIndex = com.gearloop.app.data.SearchFilters.DISTANCES.lastIndex }
        waitForText("$wider")
        tapText("Within 25 km")
        waitForText(resultsLabel(wider))
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
