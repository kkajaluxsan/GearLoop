package com.gearloop.app.data

/**
 * Attribution for the photography.
 *
 * Every image is Creative Commons or public domain, sourced through
 * Openverse or Wikimedia Commons and re-cropped and colour-graded for the app. Licences that
 * permit derivative works were the only ones used, because the packaging
 * step crops and re-grades every frame.
 *
 * Generated file — do not hand-edit.
 */
data class PhotoCredit(
    val resourceName: String,
    val title: String,
    val creator: String,
    val licence: String,
    val source: String,
)

val photoCredits: List<PhotoCredit> = listOf(
    PhotoCredit("photo_burton_board", "snowboard", "ffo-sesp", "BY-SA", "flickr"),
    PhotoCredit("photo_climbing_rack", "Carabiners climbing gear", "Unknown", "CC0", "rawpixel"),
    PhotoCredit("photo_cond_1", "Surfboard ding", "goosmurf", "BY", "flickr"),
    PhotoCredit("photo_cond_2", "Ding", "igb", "BY", "flickr"),
    PhotoCredit("photo_cond_3", "Nose Job", "igb", "BY", "flickr"),
    PhotoCredit("photo_gear_flatlay", "Camping Equipment", "wetwebwork", "BY-SA", "flickr"),
    PhotoCredit("photo_helmet", "My new bicycle helmet", "firepile", "BY", "flickr"),
    PhotoCredit("photo_kayak", "Kayaks on Kayak Beach", "Laurel Fan", "BY-SA", "flickr"),
    PhotoCredit("photo_my_bat", "GandM Flare DXM bat-Purist 156g ball", "Marie-Lan Nguyen", "BY", "wikimedia"),
    PhotoCredit("photo_my_helmet", "Abus, Cyclingworld Europe 2024, Meerbusch (P1180004)", "Matti Blume", "BY-SA", "wikimedia"),
    PhotoCredit("photo_my_kayak", "Inflatable kayak at Manning River", "JaredMcKenzie", "CC0", "wikimedia"),
    PhotoCredit("photo_nsp_sup", "Sunset .. Vancouver Island heat wave", "Nick Kenrick..", "BY", "flickr"),
    PhotoCredit("photo_quechua_tent", "RMNP Camping Tent", "Dave Dugdale", "BY-SA", "flickr"),
    PhotoCredit("photo_santacruz", "Rocky Mountain Vertex", "Dusty J", "BY", "flickr"),
    PhotoCredit("photo_sot_kayak", "Orange sit-on-top kayak detail", "23IVAN", "BY-SA", "wikimedia"),
    PhotoCredit("photo_ss_bat", "google cricket bat & ball", "osde8info", "BY-SA", "flickr"),
    PhotoCredit("photo_torq_1", "Finished Board, Deck", "tylerkaraszewski", "BY", "flickr"),
    PhotoCredit("photo_torq_2", "Surfboard fins sand beach sunset", "Unknown", "CC0", "rawpixel"),
    PhotoCredit("photo_torq_3", "barbuto", "istolethetv", "BY", "flickr"),
    PhotoCredit("photo_torq_4", "Surfboard Fin", "mikecogh", "BY-SA", "flickr"),
    PhotoCredit("photo_torq_5", "Laminated Deck, Tail View", "tylerkaraszewski", "BY", "flickr"),
    PhotoCredit("photo_trek_marlin", "2010 Breezer Thunder", "Richard Masoner / Cyclelicious", "BY-SA", "flickr"),
    PhotoCredit("photo_wetsuit", "Bush Man Chemistry", "Chris Hunkeler", "BY-SA", "flickr"),
    PhotoCredit("place_arugam_bay", "BOAT ,SAND, PALMS & CLOUDS.", "ronsaunders47", "BY-SA", "flickr"),
    PhotoCredit("place_casuarina", "Beach", "kansasphoto", "BY", "flickr"),
    PhotoCredit("place_jaffna", "Jaffna, Sri Lanka 6/6", "Dennis S. Hurd", "CC0", "flickr"),
    PhotoCredit("sport_badminton", "Badminton semi final", "RedCraig", "BY-SA", "flickr"),
    PhotoCredit("sport_camping", "Campsite tents", "bibliojojo", "BY", "flickr"),
    PhotoCredit("sport_climbing", "Rock Climbing", "Joshua Tree National Park", "PDM", "flickr"),
    PhotoCredit("sport_cricket", "Lords Cricket Ground - June 2011 - nPowerment", "Gareth1953 All Right Now", "BY", "flickr"),
    PhotoCredit("sport_cycling", "All Japan Cycle Road Race Championships", "musumemiyuki", "BY", "flickr"),
    PhotoCredit("sport_football", "Health and safety madness on football pitch", "markhillary", "BY", "flickr"),
    PhotoCredit("sport_kayaking", "Kayaking river no date", "U.S. Forest Service (source)", "CC0", "rawpixel"),
    PhotoCredit("sport_running", "Trail Running Lago Orta", "Julia Baykova", "BY", "flickr"),
    PhotoCredit("sport_sup", "Stand Up Paddleboard Class", "Northwest Rafting Company", "BY", "flickr"),
    PhotoCredit("sport_surfing", "Surf Wave", "trindade.joao", "BY", "flickr"),
    PhotoCredit("sport_swimming", "Swimming Pool Lanes", "gliak00", "BY-SA", "flickr"),
    PhotoCredit("sport_trekking", "Hiking trail sign on green hill", "Horia Varlan", "BY", "flickr"),
)
