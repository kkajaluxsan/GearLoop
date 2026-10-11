package com.gearloop.app.data

import androidx.annotation.DrawableRes
import com.gearloop.app.R

/**
 * Full-size photograph → 600 px thumbnail.
 *
 * Every photograph ships twice. The full frame is 1200×900, which is what a
 * hero or a carousel actually needs on a 3× screen; a bitmap that size costs
 * 4.1 MB of heap once decoded, and the same frame is used for 48 dp avatars and
 * 96 dp list thumbnails all over the app. At 600 px the same picture costs
 * about 1 MB, and at the sizes it is drawn there is nothing to see between them.
 *
 * `GearImage` picks the right one from the space it has been given, so no call
 * site has to know this file exists.
 *
 * Generated file — do not hand-edit. Regenerate alongside the packaged imagery.
 */
object Thumbs {

    /** The widest slot a thumbnail is allowed to fill, in pixels. */
    const val MAX_PX = 600

    private val map: Map<Int, Int> = mapOf(
        R.drawable.photo_burton_board to R.drawable.photo_burton_board_thumb,
        R.drawable.photo_climbing_rack to R.drawable.photo_climbing_rack_thumb,
        R.drawable.photo_cond_1 to R.drawable.photo_cond_1_thumb,
        R.drawable.photo_cond_2 to R.drawable.photo_cond_2_thumb,
        R.drawable.photo_cond_3 to R.drawable.photo_cond_3_thumb,
        R.drawable.photo_gear_flatlay to R.drawable.photo_gear_flatlay_thumb,
        R.drawable.photo_helmet to R.drawable.photo_helmet_thumb,
        R.drawable.photo_kayak to R.drawable.photo_kayak_thumb,
        R.drawable.photo_mk_longboard to R.drawable.photo_mk_longboard_thumb,
        R.drawable.photo_mk_bodyboard to R.drawable.photo_mk_bodyboard_thumb,
        R.drawable.photo_mk_fullsuit to R.drawable.photo_mk_fullsuit_thumb,
        R.drawable.photo_mk_isup to R.drawable.photo_mk_isup_thumb,
        R.drawable.photo_mk_sup_paddle to R.drawable.photo_mk_sup_paddle_thumb,
        R.drawable.photo_mk_tandem_kayak to R.drawable.photo_mk_tandem_kayak_thumb,
        R.drawable.photo_mk_kayak_paddle to R.drawable.photo_mk_kayak_paddle_thumb,
        R.drawable.photo_mk_road_bike to R.drawable.photo_mk_road_bike_thumb,
        R.drawable.photo_mk_ebike to R.drawable.photo_mk_ebike_thumb,
        R.drawable.photo_mk_bike_trailer to R.drawable.photo_mk_bike_trailer_thumb,
        R.drawable.photo_mk_hardtail to R.drawable.photo_mk_hardtail_thumb,
        R.drawable.photo_mk_emtb to R.drawable.photo_mk_emtb_thumb,
        R.drawable.photo_mk_backpack_tent to R.drawable.photo_mk_backpack_tent_thumb,
        R.drawable.photo_mk_family_tent to R.drawable.photo_mk_family_tent_thumb,
        R.drawable.photo_mk_sleeping_bag to R.drawable.photo_mk_sleeping_bag_thumb,
        R.drawable.photo_mk_camp_stove to R.drawable.photo_mk_camp_stove_thumb,
        R.drawable.photo_mk_hiking_pack to R.drawable.photo_mk_hiking_pack_thumb,
        R.drawable.photo_mk_trek_poles to R.drawable.photo_mk_trek_poles_thumb,
        R.drawable.photo_mk_hammock to R.drawable.photo_mk_hammock_thumb,
        R.drawable.photo_mk_cricket_kit to R.drawable.photo_mk_cricket_kit_thumb,
        R.drawable.photo_mk_cricket_net to R.drawable.photo_mk_cricket_net_thumb,
        R.drawable.photo_mk_badminton_rackets to R.drawable.photo_mk_badminton_rackets_thumb,
        R.drawable.photo_mk_football_boots to R.drawable.photo_mk_football_boots_thumb,
        R.drawable.photo_mk_snorkel_set to R.drawable.photo_mk_snorkel_set_thumb,
        R.drawable.photo_mk_swim_fins to R.drawable.photo_mk_swim_fins_thumb,
        R.drawable.photo_mk_climb_shoes to R.drawable.photo_mk_climb_shoes_thumb,
        R.drawable.photo_mk_harness to R.drawable.photo_mk_harness_thumb,
        R.drawable.photo_mk_crash_pad to R.drawable.photo_mk_crash_pad_thumb,
        R.drawable.photo_mk_gps_watch to R.drawable.photo_mk_gps_watch_thumb,
        R.drawable.photo_mk_trail_shoes to R.drawable.photo_mk_trail_shoes_thumb,
        R.drawable.photo_mk_kite_kit to R.drawable.photo_mk_kite_kit_thumb,
        R.drawable.photo_my_bat to R.drawable.photo_my_bat_thumb,
        R.drawable.photo_my_helmet to R.drawable.photo_my_helmet_thumb,
        R.drawable.photo_my_kayak to R.drawable.photo_my_kayak_thumb,
        R.drawable.photo_nsp_sup to R.drawable.photo_nsp_sup_thumb,
        R.drawable.photo_quechua_tent to R.drawable.photo_quechua_tent_thumb,
        R.drawable.photo_santacruz to R.drawable.photo_santacruz_thumb,
        R.drawable.photo_sot_kayak to R.drawable.photo_sot_kayak_thumb,
        R.drawable.photo_ss_bat to R.drawable.photo_ss_bat_thumb,
        R.drawable.photo_torq_1 to R.drawable.photo_torq_1_thumb,
        R.drawable.photo_torq_2 to R.drawable.photo_torq_2_thumb,
        R.drawable.photo_torq_3 to R.drawable.photo_torq_3_thumb,
        R.drawable.photo_torq_4 to R.drawable.photo_torq_4_thumb,
        R.drawable.photo_torq_5 to R.drawable.photo_torq_5_thumb,
        R.drawable.photo_trek_marlin to R.drawable.photo_trek_marlin_thumb,
        R.drawable.photo_wetsuit to R.drawable.photo_wetsuit_thumb,
        R.drawable.place_arugam_bay to R.drawable.place_arugam_bay_thumb,
        R.drawable.place_casuarina to R.drawable.place_casuarina_thumb,
        R.drawable.place_jaffna to R.drawable.place_jaffna_thumb,
        R.drawable.sport_badminton to R.drawable.sport_badminton_thumb,
        R.drawable.sport_camping to R.drawable.sport_camping_thumb,
        R.drawable.sport_climbing to R.drawable.sport_climbing_thumb,
        R.drawable.sport_cricket to R.drawable.sport_cricket_thumb,
        R.drawable.sport_cycling to R.drawable.sport_cycling_thumb,
        R.drawable.sport_football to R.drawable.sport_football_thumb,
        R.drawable.sport_kayaking to R.drawable.sport_kayaking_thumb,
        R.drawable.sport_running to R.drawable.sport_running_thumb,
        R.drawable.sport_sup to R.drawable.sport_sup_thumb,
        R.drawable.sport_surfing to R.drawable.sport_surfing_thumb,
        R.drawable.sport_swimming to R.drawable.sport_swimming_thumb,
        R.drawable.sport_trekking to R.drawable.sport_trekking_thumb,
    )

    /** The thumbnail for [res], or [res] itself if it doesn't have one. */
    @DrawableRes
    fun of(@DrawableRes res: Int): Int = map[res] ?: res
}
