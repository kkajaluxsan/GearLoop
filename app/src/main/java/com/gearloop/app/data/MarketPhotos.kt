package com.gearloop.app.data

import com.gearloop.app.R

/**
 * Photographs for the wider marketplace — the listings in [Marketplace].
 * Kept beside [Photos] so the two key tables read as one. Every one is from
 * Wikimedia Commons under a licence that permits the crop and grade applied;
 * the credits are in [photoCredits].
 */
object MarketPhotos {
    val byKey: Map<String, Int> = mapOf(
        "mk_longboard" to R.drawable.photo_mk_longboard,
        "mk_bodyboard" to R.drawable.photo_mk_bodyboard,
        "mk_fullsuit" to R.drawable.photo_mk_fullsuit,
        "mk_isup" to R.drawable.photo_mk_isup,
        "mk_sup_paddle" to R.drawable.photo_mk_sup_paddle,
        "mk_tandem_kayak" to R.drawable.photo_mk_tandem_kayak,
        "mk_kayak_paddle" to R.drawable.photo_mk_kayak_paddle,
        "mk_road_bike" to R.drawable.photo_mk_road_bike,
        "mk_ebike" to R.drawable.photo_mk_ebike,
        "mk_bike_trailer" to R.drawable.photo_mk_bike_trailer,
        "mk_hardtail" to R.drawable.photo_mk_hardtail,
        "mk_emtb" to R.drawable.photo_mk_emtb,
        "mk_backpack_tent" to R.drawable.photo_mk_backpack_tent,
        "mk_family_tent" to R.drawable.photo_mk_family_tent,
        "mk_sleeping_bag" to R.drawable.photo_mk_sleeping_bag,
        "mk_camp_stove" to R.drawable.photo_mk_camp_stove,
        "mk_hiking_pack" to R.drawable.photo_mk_hiking_pack,
        "mk_trek_poles" to R.drawable.photo_mk_trek_poles,
        "mk_hammock" to R.drawable.photo_mk_hammock,
        "mk_cricket_kit" to R.drawable.photo_mk_cricket_kit,
        "mk_cricket_net" to R.drawable.photo_mk_cricket_net,
        "mk_badminton_rackets" to R.drawable.photo_mk_badminton_rackets,
        "mk_football_boots" to R.drawable.photo_mk_football_boots,
        "mk_snorkel_set" to R.drawable.photo_mk_snorkel_set,
        "mk_swim_fins" to R.drawable.photo_mk_swim_fins,
        "mk_climb_shoes" to R.drawable.photo_mk_climb_shoes,
        "mk_harness" to R.drawable.photo_mk_harness,
        "mk_crash_pad" to R.drawable.photo_mk_crash_pad,
        "mk_gps_watch" to R.drawable.photo_mk_gps_watch,
        "mk_trail_shoes" to R.drawable.photo_mk_trail_shoes,
        "mk_kite_kit" to R.drawable.photo_mk_kite_kit,
    )
}
