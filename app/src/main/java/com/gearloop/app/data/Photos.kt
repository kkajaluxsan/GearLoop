package com.gearloop.app.data

import androidx.annotation.DrawableRes
import com.gearloop.app.R

/**
 * Photographs as the database stores them: a stable key, not a resource id.
 *
 * Two kinds of key exist. A bundled photograph is a short name such as
 * `torq_1`, and this table is the one place that turns it back into a
 * resource — `R.drawable` integers can change between two builds of the same
 * source, so a row written by one APK and read by the next would otherwise
 * point at a different picture. A picture taken or chosen on this phone is
 * `file:photos/<name>.jpg`, relative to the app's private files, and is read
 * by [PhotoStore].
 */
object Photos {

    /** What a listing with no photograph of its own shows. */
    const val FALLBACK = "gear_flatlay"

    /** Prefix of a key that names a file in the app's private storage. */
    const val FILE_PREFIX = "file:"

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
    ) + MarketPhotos.byKey

    /** True for a picture taken or chosen on this phone. */
    fun isFile(key: String): Boolean = key.startsWith(FILE_PREFIX)

    /** True for a key this build can draw from its own resources. */
    fun isBundled(key: String): Boolean = key in byKey

    /** What a bundled key draws. An unknown key falls back to the neutral flat-lay. */
    @DrawableRes
    fun res(key: String): Int = byKey[key] ?: R.drawable.photo_gear_flatlay

    /** A sport's tile photograph — the stand-in for a listing with no photos yet. */
    fun forSport(sport: String): String =
        ("sport_" + sport.lowercase().replace(' ', '_')).takeIf { it in byKey } ?: FALLBACK
}
