package com.gearloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The search predicate, checked against the real dataset.
 *
 * This is the one piece of the app where a wrong answer is invisible: a filter
 * that quietly excludes something looks exactly like a filter that works. The
 * cases below are the ones a demonstration actually walks through, plus the two
 * boundary bugs that were live before these tests existed — the LKR 400 helmet
 * falling below the price floor, and the delivery toggle that counted as an
 * active filter while filtering nothing.
 */
class SearchFiltersTest {

    private val all = Demo.allListings

    private fun filters(block: SearchFilters.() -> Unit = {}) = SearchFilters().apply(block)

    @Test
    fun `default filters leave the app showing results`() {
        // S-05 is on the main demo path. Opening it to an empty list would be
        // the worst possible first impression.
        val results = filters().apply(all)
        assertTrue("default filters must not empty the results", results.isNotEmpty())
    }

    @Test
    fun `default filters are reported as inactive`() {
        val f = filters()
        assertEquals(0, f.activeCount)
        assertTrue(f.isDefault)
    }

    @Test
    fun `the widest price range excludes nothing on price`() {
        // The slider ends mean "no limit". PRICE_MIN is 500 and the cheapest
        // item is the LKR 400 helmet, so treating the low end as a real floor
        // silently hid it.
        val results = filters { priceLower = 0f; priceUpper = 1f }.apply(all)
        val cheapest = all.minBy { it.pricePerDay }
        assertTrue(
            "the cheapest item (${cheapest.pricePerDay}) must survive the widest range",
            cheapest in results,
        )
    }

    @Test
    fun `the under two thousand chip keeps only cheaper gear`() {
        // What S-04's "Under LKR 2,000" chip sets.
        val results = filters { priceUpper = 0.34f }.apply(all)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.pricePerDay <= 2_000 })
    }

    @Test
    fun `fits me excludes only the verdicts that mean wrong size`() {
        val results = filters { fitOnly = true }.apply(all)
        assertTrue(results.none { it.fit == Fit.BLOCKED })
        assertTrue(results.none { it.fit == Fit.UNKNOWN })
        assertTrue(results.all { it.fit in setOf(Fit.PERFECT, Fit.GOOD, Fit.WORKABLE) })
    }

    @Test
    fun `distance filter respects the selected band`() {
        SearchFilters.DISTANCES.indices.forEach { i ->
            val km = SearchFilters.DISTANCES[i]
            val results = filters { distanceIndex = i }.apply(all)
            assertTrue(
                "everything shown at $km km must be within $km km",
                results.all { it.distanceKm <= km },
            )
        }
    }

    @Test
    fun `widening the distance never removes a result`() {
        val narrow = filters { distanceIndex = 0 }.apply(all)
        val wide = filters { distanceIndex = SearchFilters.DISTANCES.lastIndex }.apply(all)
        assertTrue(wide.containsAll(narrow))
    }

    @Test
    fun `delivery filter is real and not decorative`() {
        // It counts towards activeCount, so it has to actually do something.
        val results = filters { delivery = true }.apply(all)
        assertTrue("some demo gear must offer delivery", results.isNotEmpty())
        assertTrue(results.all { it.delivery })
        assertTrue("delivery must not match everything", results.size < all.size)
    }

    @Test
    fun `instant book filter is real`() {
        val results = filters { instantOnly = true }.apply(all)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.instantBook })
        assertTrue(results.size < all.size)
    }

    @Test
    fun `query matches title sport and item code, case insensitively`() {
        val torq = Demo.torq
        assertTrue(torq in filters { query = torq.title.take(4).lowercase() }.apply(all))
        assertTrue(torq in filters { query = torq.sport.uppercase() }.apply(all))
        assertTrue(torq in filters { query = torq.code }.apply(all))
    }

    @Test
    fun `a query matching nothing returns an empty list rather than everything`() {
        assertTrue(filters { query = "zzzznotathing" }.apply(all).isEmpty())
    }

    @Test
    fun `grade filter matches the grade letter`() {
        val results = filters { grades.add("Grade A") }.apply(all)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.grade == Grade.A })
    }

    @Test
    fun `reset returns to the default result set`() {
        val f = filters {
            fitOnly = true; instantOnly = true; delivery = true
            distanceIndex = 0; priceLower = 0.4f; priceUpper = 0.5f
            query = "torq"; lengths.add("6'6\""); grades.add("Grade A")
        }
        assertTrue(f.activeCount > 0)
        f.reset()
        assertEquals(0, f.activeCount)
        assertEquals(SearchFilters().apply(all), f.apply(all))
    }

    @Test
    fun `loosen never narrows, and eventually shows everything`() {
        // Drives the "Loosen the tightest filter" action on the empty state. A
        // button that made the list shorter would be worse than no button.
        val f = filters {
            fitOnly = true; instantOnly = true; delivery = true
            distanceIndex = 0; priceLower = 0.4f; priceUpper = 0.5f
            query = "torq"; lengths.add("6'6\""); grades.add("Grade A")
        }
        var previous = f.apply(all).size
        repeat(12) {
            f.loosen()
            val now = f.apply(all).size
            assertTrue("loosening must never reduce the result count", now >= previous)
            previous = now
        }
        assertEquals("loosening to the end must show every listing", all.size, f.apply(all).size)
    }

    @Test
    fun `loosen clears every filter it claims to count`() {
        // If activeCount counts a filter, loosen has to know how to drop it —
        // otherwise the empty state offers a button that stops helping.
        val f = filters {
            fitOnly = true; instantOnly = true; delivery = true
            priceLower = 0.4f; priceUpper = 0.5f
            query = "torq"; lengths.add("6'6\""); grades.add("Grade A")
        }
        repeat(12) { f.loosen() }
        assertFalse(f.fitOnly)
        assertFalse(f.instantOnly)
        assertFalse(f.delivery)
        assertTrue(f.query.isBlank())
        assertTrue(f.lengths.isEmpty())
        assertTrue(f.grades.isEmpty())
    }

    @Test
    fun `an impossible combination empties the list so the empty state is reachable`() {
        val results = filters {
            distanceIndex = 0
            instantOnly = true
            grades.add("Grade C")
            priceUpper = 0.02f
        }.apply(all)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `active count reflects exactly what is switched on`() {
        val f = filters()
        assertEquals(0, f.activeCount)
        f.fitOnly = true
        assertEquals(1, f.activeCount)
        f.query = "surf"
        assertEquals(2, f.activeCount)
        f.grades.add("Grade A")
        assertEquals(3, f.activeCount)
        f.grades.clear()
        f.query = ""
        f.fitOnly = false
        assertEquals(0, f.activeCount)
        assertFalse(f.activeCount > 0)
    }
}
