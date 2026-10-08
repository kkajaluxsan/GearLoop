package com.gearloop.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList

/**
 * What the discovery screens are actually filtering by.
 *
 * The filter sheet and the results list used to hold separate copies of the same
 * idea, which meant neither could affect the other and the result count was a
 * number typed into the design. One object, shared, is what makes the answer to
 * "does the filter work?" yes.
 *
 * It filters whatever list it is given using fields [GearItem] already carries.
 * Everything here is UI state hoisted to the point where two screens can both
 * see it; since Milestone 03 the app shell also writes it to the database
 * whenever it changes, so the sheet opens as it was left.
 */
class SearchFilters(
    fitOnly: Boolean = false,
    instantOnly: Boolean = false,
    delivery: Boolean = false,
    distanceIndex: Int = DEFAULT_DISTANCE,
    priceLower: Float = 0f,
    priceUpper: Float = 1f,
    query: String = "",
    lengths: List<String> = emptyList(),
    grades: List<String> = emptyList(),
) {
    var fitOnly by mutableStateOf(fitOnly)
    var instantOnly by mutableStateOf(instantOnly)
    var delivery by mutableStateOf(delivery)

    /** Index into [DISTANCES]. */
    var distanceIndex by mutableIntStateOf(distanceIndex)

    /** Both ends of the price range, as a fraction of [PRICE_MIN]..[PRICE_MAX]. */
    var priceLower by mutableFloatStateOf(priceLower)
    var priceUpper by mutableFloatStateOf(priceUpper)

    var query by mutableStateOf(query)

    val lengths = lengths.toMutableStateList()
    val grades = grades.toMutableStateList()

    val maxDistanceKm: Int get() = DISTANCES[distanceIndex.coerceIn(DISTANCES.indices)]
    fun priceAt(fraction: Float): Int =
        (PRICE_MIN + (PRICE_MAX - PRICE_MIN) * fraction).toInt() / 50 * 50

    /** How many filters are doing something. Drives the badge on the search bar. */
    val activeCount: Int
        get() = listOf(
            fitOnly,
            instantOnly,
            delivery,
            distanceIndex != DEFAULT_DISTANCE,
            priceLower > 0f || priceUpper < 1f,
            lengths.isNotEmpty(),
            grades.isNotEmpty(),
            query.isNotBlank(),
        ).count { it }

    val isDefault: Boolean get() = activeCount == 0

    fun apply(items: List<GearItem>): List<GearItem> = items.filter { item ->
        val q = query.trim()
        val matchesQuery = q.isEmpty() ||
            item.title.contains(q, ignoreCase = true) ||
            item.sport.contains(q, ignoreCase = true) ||
            item.code.contains(q, ignoreCase = true)

        // "Fits me" is a safety filter, not a preference one, so it excludes the
        // verdicts that mean the gear is the wrong size rather than merely not ideal.
        val matchesFit = !fitOnly || item.fit in setOf(Fit.PERFECT, Fit.GOOD, Fit.WORKABLE)

        // A slider at its end means "no limit", not "exactly PRICE_MIN". Without
        // this, the widest possible range still hides the LKR 400 helmet.
        val floor = if (priceLower <= 0f) 0 else priceAt(priceLower)
        val ceiling = if (priceUpper >= 1f) Int.MAX_VALUE else priceAt(priceUpper)

        matchesQuery &&
            matchesFit &&
            (!instantOnly || item.instantBook) &&
            (!delivery || item.delivery) &&
            item.distanceKm <= maxDistanceKm &&
            item.pricePerDay >= floor &&
            item.pricePerDay <= ceiling &&
            (lengths.isEmpty() || lengths.any { item.sizeLabel.startsWith(it) }) &&
            (grades.isEmpty() || grades.any { it.endsWith(item.grade.letter) })
    }

    /**
     * Everything the predicate depends on, as one comparable value — what the app
     * shell watches to know when to write the sheet's state to the database.
     */
    val snapshot: List<Any>
        get() = listOf(fitOnly, instantOnly, delivery, distanceIndex, priceLower, priceUpper, query, lengths.toList(), grades.toList())

    /** An independent copy, for asking "what if" without touching the live one (S-39). */
    fun copy(): SearchFilters = SearchFilters(
        fitOnly, instantOnly, delivery, distanceIndex, priceLower, priceUpper, query, lengths.toList(), grades.toList(),
    )

    /** The active filters as a phrase, for an alert or a "wanted" post. */
    fun describe(): String {
        val parts = buildList {
            if (query.isNotBlank()) add("\"${query.trim()}\"")
            if (lengths.isNotEmpty()) add(lengths.joinToString("/"))
            if (grades.isNotEmpty()) add(grades.joinToString("/"))
            if (fitOnly) add("fits me")
            if (instantOnly) add("instant book")
            if (delivery) add("delivered")
            if (priceLower > 0f || priceUpper < 1f) add("${lkr(priceAt(priceLower))}–${grouped(priceAt(priceUpper))} a day")
            add("within $maxDistanceKm km")
        }
        return parts.joinToString(" · ").replaceFirstChar { it.uppercase() }
    }

    /** Put back what the filter sheet held when the app was last open. */
    fun restoreFrom(saved: com.gearloop.app.data.db.SearchPrefsEntity) {
        fitOnly = saved.fitOnly
        instantOnly = saved.instantOnly
        delivery = saved.delivery
        distanceIndex = saved.distanceIndex.coerceIn(DISTANCES.indices)
        priceLower = saved.priceLower
        priceUpper = saved.priceUpper
        query = saved.query
        lengths.clear()
        lengths.addAll(saved.lengths)
        grades.clear()
        grades.addAll(saved.grades)
    }

    fun reset() {
        fitOnly = false
        instantOnly = false
        delivery = false
        distanceIndex = DEFAULT_DISTANCE
        priceLower = 0f
        priceUpper = 1f
        query = ""
        lengths.clear()
        grades.clear()
    }

    /**
     * Drop whichever filter is excluding the most, so the empty state can offer
     * a real way out instead of only apologising.
     *
     * Each call must widen the result set or leave it alone — never narrow it.
     * That rules out falling back to [reset], because the default radius is
     * 5 km and this may already have reached past it to 25.
     */
    fun loosen() {
        when {
            lengths.isNotEmpty() -> lengths.clear()
            grades.isNotEmpty() -> grades.clear()
            priceLower > 0f || priceUpper < 1f -> { priceLower = 0f; priceUpper = 1f }
            fitOnly -> fitOnly = false
            instantOnly -> instantOnly = false
            delivery -> delivery = false
            query.isNotBlank() -> query = ""
            // Last, because reaching further afield changes what the results
            // mean rather than just how many there are.
            distanceIndex < DISTANCES.lastIndex -> distanceIndex = DISTANCES.lastIndex
        }
    }

    companion object {
        const val PRICE_MIN = 500
        const val PRICE_MAX = 5_000
        val DISTANCES = listOf(2, 5, 10, 25)

        /** 5 km — wide enough that the app opens showing results, not an apology. */
        const val DEFAULT_DISTANCE = 1

        val Saver: Saver<SearchFilters, List<Any>> = Saver(
            save = {
                listOf(
                    it.fitOnly, it.instantOnly, it.delivery, it.distanceIndex,
                    it.priceLower, it.priceUpper, it.query,
                    it.lengths.toList(), it.grades.toList(),
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST")
                SearchFilters(
                    fitOnly = it[0] as Boolean,
                    instantOnly = it[1] as Boolean,
                    delivery = it[2] as Boolean,
                    distanceIndex = it[3] as Int,
                    priceLower = it[4] as Float,
                    priceUpper = it[5] as Float,
                    query = it[6] as String,
                    lengths = it[7] as List<String>,
                    grades = it[8] as List<String>,
                )
            },
        )
    }
}

val LocalSearchFilters = staticCompositionLocalOf { SearchFilters() }
