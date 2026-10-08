package com.gearloop.app.data

import androidx.annotation.DrawableRes
import com.gearloop.app.R

/**
 * Photographs as the database stores them: a stable key, not a resource id.
 *
 * `R.drawable` integers are assigned at build time and can change between two
 * builds of the same source, so a row written by one APK and read by the next
 * would point at a different picture. A key is a name, and this table is the one
 * place that turns it back into a resource — which also keeps every reference
 * explicit, so resource shrinking can see that the image is used.
 */
object Photos {

    private val byKey: Map<String, Int> = mapOf(
        "torq_1" to R.drawable.photo_torq_1,
        "torq_2" to R.drawable.photo_torq_2,
        "torq_3" to R.drawable.photo_torq_3,
        "torq_4" to R.drawable.photo_torq_4,
        "torq_5" to R.drawable.photo_torq_5,
        "cond_1" to R.drawable.photo_cond_1,
        "cond_2" to R.drawable.photo_cond_2,
        "cond_3" to R.drawable.photo_cond_3,
        "trek_marlin" to R.drawable.photo_trek_marlin,
        "nsp_sup" to R.drawable.photo_nsp_sup,
        "quechua_tent" to R.drawable.photo_quechua_tent,
        "ss_bat" to R.drawable.photo_ss_bat,
        "wetsuit" to R.drawable.photo_wetsuit,
        "helmet" to R.drawable.photo_helmet,
        "kayak" to R.drawable.photo_kayak,
        "climbing_rack" to R.drawable.photo_climbing_rack,
        "burton_board" to R.drawable.photo_burton_board,
        "santacruz" to R.drawable.photo_santacruz,
        "gear_flatlay" to R.drawable.photo_gear_flatlay,
        "my_helmet" to R.drawable.photo_my_helmet,
        "my_bat" to R.drawable.photo_my_bat,
        "my_kayak" to R.drawable.photo_my_kayak,
        "sot_kayak" to R.drawable.photo_sot_kayak,
        "sport_surfing" to R.drawable.sport_surfing,
        "sport_cycling" to R.drawable.sport_cycling,
        "sport_sup" to R.drawable.sport_sup,
        "sport_cricket" to R.drawable.sport_cricket,
        "sport_trekking" to R.drawable.sport_trekking,
        "sport_camping" to R.drawable.sport_camping,
        "sport_swimming" to R.drawable.sport_swimming,
        "sport_running" to R.drawable.sport_running,
        "sport_climbing" to R.drawable.sport_climbing,
        "sport_kayaking" to R.drawable.sport_kayaking,
        "sport_badminton" to R.drawable.sport_badminton,
        "sport_football" to R.drawable.sport_football,
    )

    private val byRes: Map<Int, String> = byKey.entries.associate { (k, v) -> v to k }

    /** What a key draws. An unknown key falls back to the neutral flat-lay. */
    @DrawableRes
    fun res(key: String): Int = byKey[key] ?: R.drawable.photo_gear_flatlay

    /** The key for a resource, for seeding the database from [Demo]. */
    fun key(@DrawableRes res: Int): String =
        requireNotNull(byRes[res]) { "photo resource $res has no key in Photos" }

    fun resList(keys: List<String>): List<Int> = keys.map(::res)
    fun keyList(res: List<Int>): List<String> = res.map(::key)

    /** A sport's tile photograph — the stand-in for a new listing with no photos yet. */
    @DrawableRes
    fun forSport(sport: String): Int = res("sport_" + sport.lowercase().replace(' ', '_'))

    /**
     * The frame a guided capture records for each angle.
     *
     * The capture screen is a working record — timestamp, angle, both parties,
     * validation — but it does not open the camera in this release, so each
     * angle records the matching condition photograph from the dataset. The keys
     * are the ones S-15 and S-18 always showed, so the evidence still lines up.
     */
    val sampleFrame: Map<String, String> = mapOf(
        "Deck" to "torq_2",
        "Bottom" to "torq_5",
        "Nose" to "cond_3",
        "Tail" to "cond_1",
        "Rail L" to "torq_4",
        "Rail R" to "torq_4",
        "Fin box" to "cond_2",
    )
}
