package com.gearloop.app.data

/**
 * The wider marketplace: members and listings beyond the original catalogue in
 * [Demo], so search, filters, the map and the price slider have real variety to
 * work over — every one of the twelve sports, from a LKR 250 pair of trekking
 * poles to a LKR 6,500 electric mountain bike.
 *
 * Areas are Jaffna neighbourhoods and each distance agrees with its area, the
 * same rule [Demo] follows (D-31).
 */
object Marketplace {

    // ---------------------------------------------------------------- owners

    val priya = Person("Priya Nadarajah", "@priya_n", "Kopay", TrustTier.SILVER, 4.8, 18, "~1 h", 0, "Nov 2024")
    val arun = Person("Arun Sivakumar", "@arun_s", "Chunnakam", TrustTier.GOLD, 4.9, 31, "~30 m", 0, "Jun 2024")
    val fathima = Person("Fathima Rizna", "@fathima_r", "Jaffna Fort", TrustTier.SILVER, 4.6, 9, "~2 h", 0, "Apr 2025")
    val kasun = Person("Kasun Wijeratne", "@kasun_w", "Kaithady", TrustTier.BRONZE, 4.4, 5, "~4 h", 0, "Feb 2026")
    val tharsika = Person("Tharsika Mahendran", "@tharsika_m", "Urumpirai", TrustTier.SILVER, 4.7, 15, "~1 h", 0, "Sep 2024")
    val sanjay = Person("Sanjay Pillai", "@sanjay_p", "Kondavil", TrustTier.GOLD, 4.9, 27, "~20 m", 0, "Mar 2024")
    val meera = Person("Meera Ganesh", "@meera_g", "Thirunelvely", TrustTier.SILVER, 4.8, 11, "~2 h", 0, "Jul 2025")
    val rohan = Person("Rohan de Silva", "@rohan_ds", "Mandaitivu", TrustTier.BRONZE, 4.5, 4, "~6 h", 0, "May 2026")

    val owners = listOf(priya, arun, fathima, kasun, tharsika, sanjay, meera, rohan)

    // ---------------------------------------------------------------- listings

    /** What a listing says beyond its card: the detail page and the condition record. */
    data class Detail(
        val specs: List<SpecRow>,
        val included: List<String>,
        val description: String,
        val conditionNote: String,
        val conditionDaysAgo: Int,
        val excluded: List<String> = emptyList(),
        val units: Int = 1,
    )

    data class Entry(val item: GearItem, val detail: Detail)

    private fun spec(label: String, value: String, fit: Boolean = false) = SpecRow(label, value, fit)

    private fun entry(
        id: String,
        title: String,
        short: String,
        code: String,
        size: String,
        grade: Grade,
        price: Int,
        km: Double,
        photo: String,
        owner: Person,
        fit: Fit,
        sport: String,
        deposit: Int,
        detail: Detail,
        instant: Boolean = false,
        delivery: Boolean = false,
        volumeL: Int? = null,
        frameBand: String? = null,
    ) = Entry(
        GearItem(
            id = id, title = title, shortTitle = short, code = code, sizeLabel = size, grade = grade,
            pricePerDay = price, distanceKm = km, photos = listOf(photo), owner = owner, fit = fit,
            sport = sport, instantBook = instant, delivery = delivery, deposit = deposit,
            volumeL = volumeL, frameBand = frameBand, description = detail.description,
        ),
        detail,
    )

    val entries: List<Entry> = listOf(
        // ---------------------------------------------------------- surfing
        entry(
            "longboard", "Classic 9'0\" Noserider Longboard", "9'0\" Noserider Longboard", "LB-9010", "9'0\"", Grade.B,
            3_500, 4.1, "mk_longboard", sanjay, Fit.GOOD, "Surfing", 28_000,
            Detail(
                listOf(spec("Length", "9'0\" · 274 cm", true), spec("Volume", "72 L", true), spec("Fins", "Single fin box"), spec("Year", "2022")),
                listOf("Board", "Single fin", "Leash", "Board sock"),
                "A stable, easy-paddling longboard for small days at Casuarina. Ideal if you're stepping up from a foamie.",
                "Used 60 days · rails resealed last month", 21,
            ),
            volumeL = 72, instant = true,
        ),
        entry(
            "foamie", "7'0\" Soft-Top Beginner Surfboard", "7'0\" Soft-Top Beginner", "SF-7002", "7'0\"", Grade.A,
            1_600, 6.4, "sport_surfing", priya, Fit.GOOD, "Surfing", 12_000,
            Detail(
                listOf(spec("Length", "7'0\" · 213 cm", true), spec("Volume", "56 L", true), spec("Construction", "Soft top · EPS"), spec("Fins", "Thruster")),
                listOf("Board", "Fins ×3", "Leash"),
                "Forgiving and buoyant. The board most people learn on, and very hard to hurt yourself with.",
                "Used 14 days · no dings", 9,
            ),
            volumeL = 56, instant = true, delivery = true,
        ),
        entry(
            "shortboard", "5'10\" Performance Shortboard", "5'10\" Shortboard", "SB-5104", "5'10\"", Grade.B,
            3_000, 10.4, "sport_surfing", arun, Fit.GOOD, "Surfing", 30_000,
            Detail(
                listOf(spec("Length", "5'10\" · 178 cm", true), spec("Volume", "30 L", true), spec("Fins", "FCS II thruster"), spec("Year", "2024")),
                listOf("Board", "Fins ×3", "Leash", "Traction pad"),
                "For confident surfers on punchy days. Low volume, so check the fit verdict before you book.",
                "Used 40 days · two repaired dings", 12,
            ),
            volumeL = 30,
        ),
        entry(
            "bodyboard", "42\" Bodyboard with Swim Fins", "42\" Bodyboard + fins", "BB-4205", "42\"", Grade.B,
            700, 1.3, "mk_bodyboard", fathima, Fit.GOOD, "Surfing", 4_000,
            Detail(
                listOf(spec("Length", "42\" · 107 cm", true), spec("Core", "PE"), spec("Fins", "EU 40–43")),
                listOf("Bodyboard", "Wrist leash", "Swim fins"),
                "Good fun in the shore break. Fins fit most adult feet.",
                "Used 20 days · deck grip worn but sound", 15,
            ),
            instant = true,
        ),
        entry(
            "fullsuit", "3/2 mm Full Wetsuit — L", "3/2 mm Full Wetsuit L", "WS-3216", "L 3/2MM", Grade.A,
            1_100, 10.2, "mk_fullsuit", arun, Fit.WORKABLE, "Surfing", 7_000,
            Detail(
                listOf(spec("Size", "L", true), spec("Thickness", "3/2 mm"), spec("Zip", "Chest")),
                listOf("Wetsuit", "Hanger"),
                "Warm enough for the windy months. Rinsed and dried after every rental.",
                "Used 18 days · seams checked", 6,
            ),
        ),
        entry(
            "kitekit", "Kitesurf Kit — 9 m Kite, Twin-tip, Harness", "Kitesurf kit · 9 m", "KT-0909", "9M", Grade.B,
            5_500, 4.0, "mk_kite_kit", sanjay, Fit.UNKNOWN, "Surfing", 60_000,
            Detail(
                listOf(spec("Kite", "9 m²"), spec("Board", "136 cm twin-tip"), spec("Harness", "Waist, M"), spec("Lines", "24 m")),
                listOf("Kite", "Bar and lines", "Twin-tip board", "Harness", "Pump"),
                "For riders with lessons behind them. Sanjay asks for proof of an IKO level 2 or a quick session together first.",
                "Used 25 days · lines replaced", 10, excluded = listOf("Lessons (ask Sanjay)"),
            ),
        ),
        // ---------------------------------------------------------- SUP
        entry(
            "isup", "Inflatable SUP 11'0\" Touring Package", "Inflatable SUP 11'0\"", "IS-1108", "11'0\"", Grade.A,
            2_800, 7.4, "mk_isup", rohan, Fit.GOOD, "SUP", 20_000,
            Detail(
                listOf(spec("Length", "11'0\" · 335 cm", true), spec("Width", "32\""), spec("Max load", "140 kg")),
                listOf("Board", "3-piece paddle", "Pump", "Leash", "Backpack"),
                "Tracks straight on the lagoon and packs into a backpack for the bus to Mandaitivu.",
                "Used 11 days · no leaks", 5,
            ),
            instant = true, delivery = true,
        ),
        entry(
            "suppaddle", "Carbon SUP Paddle, Adjustable", "Carbon SUP paddle", "SP-2210", "170–210",
            Grade.A, 450, 3.1, "mk_sup_paddle", meera, Fit.UNKNOWN, "SUP", 3_500,
            Detail(
                listOf(spec("Length", "170–210 cm"), spec("Shaft", "Carbon"), spec("Weight", "680 g")),
                listOf("Paddle", "Blade cover"),
                "Half the weight of the alloy paddles that come with most boards. Your shoulders will notice.",
                "Used 9 days · blade edge taped", 13,
            ),
            instant = true,
        ),
        // ---------------------------------------------------------- kayaking
        entry(
            "tandemkayak", "Tandem Sit-on-Top Kayak", "Tandem sit-on-top kayak", "TK-2003", "12'6\"", Grade.B,
            3_800, 7.6, "mk_tandem_kayak", rohan, Fit.GOOD, "Kayaking", 35_000,
            Detail(
                listOf(spec("Length", "12'6\" · 381 cm", true), spec("Seats", "2 + child"), spec("Capacity", "230 kg")),
                listOf("Kayak", "Paddles ×2", "Buoyancy aids ×2", "Dry bag"),
                "Room for two adults and a child. Rohan can drop it at the Mandaitivu slipway.",
                "Used 22 days · hull checked", 8,
            ),
            delivery = true,
        ),
        entry(
            "kayakpaddle", "Kayak Paddle 220 cm, Feathered", "Kayak paddle 220 cm", "KP-2207", "220CM", Grade.B,
            350, 8.0, "mk_kayak_paddle", kasun, Fit.UNKNOWN, "Kayaking", 2_500,
            Detail(
                listOf(spec("Length", "220 cm"), spec("Shaft", "Fibreglass, 2-piece"), spec("Blades", "Asymmetric")),
                listOf("Paddle", "Drip rings"),
                "A spare or an upgrade for any sit-on-top.",
                "Used 30 days · ferrule tight", 20,
            ),
        ),
        entry(
            "buoyancy", "Adult Buoyancy Aid, 50 N", "Adult buoyancy aid", "BA-5011", "M/L", Grade.A,
            300, 1.1, "sport_kayaking", fathima, Fit.WORKABLE, "Kayaking", 2_000,
            Detail(
                listOf(spec("Size", "M/L · chest 96–112 cm", true), spec("Rating", "50 N ISO 12402-5")),
                listOf("Buoyancy aid"),
                "Bright orange, with a whistle on the shoulder. Wear one every time, whatever the water looks like.",
                "Inspected this month · buckles replaced", 3,
            ),
            instant = true, delivery = true,
        ),
        // ---------------------------------------------------------- cycling
        entry(
            "roadbike", "Giant Contend 2 Road Bike — L", "Giant Contend 2 — L", "GC-5602", "L 56", Grade.A,
            2_600, 4.2, "mk_road_bike", sanjay, Fit.GOOD, "Cycling", 26_000,
            Detail(
                listOf(spec("Frame", "L · 56 cm", true), spec("Groupset", "Shimano Claris 2×8"), spec("Weight", "10.2 kg"), spec("Year", "2024")),
                listOf("Bike", "Lights", "Lock", "Bottle cage"),
                "Quick on the coast road to Point Pedro. Sized for riders around 178–185 cm.",
                "Serviced this month · new tyres", 4, units = 2,
            ),
            frameBand = "L",
        ),
        entry(
            "ebike", "City E-Bike, 80 km Range — M", "City e-bike — M", "EB-8012", "M 52", Grade.A,
            4_500, 3.0, "mk_ebike", meera, Fit.GOOD, "Cycling", 45_000,
            Detail(
                listOf(spec("Frame", "M · step-through", true), spec("Motor", "250 W hub"), spec("Range", "Up to 80 km"), spec("Battery", "36 V 13 Ah")),
                listOf("E-bike", "Charger", "Lock", "Pannier rack"),
                "Charges overnight from a normal socket. Makes the hill up to Kopay disappear.",
                "Battery health 94% · serviced last month", 12,
            ),
            instant = true, delivery = true, frameBand = "M",
        ),
        entry(
            "emtb", "Electric Mountain Bike, Full Suspension — L", "Electric MTB — L", "EM-6503", "L 29", Grade.B,
            6_500, 10.6, "mk_emtb", arun, Fit.GOOD, "Cycling", 80_000,
            Detail(
                listOf(spec("Frame", "L · 29\"", true), spec("Motor", "Mid-drive, 85 Nm"), spec("Travel", "150 mm"), spec("Battery", "625 Wh")),
                listOf("Bike", "Charger", "Helmet", "Repair kit"),
                "The most capable bike on GearLoop. Helmet included, and Arun goes through the controls at pickup.",
                "Serviced 2 weeks ago", 14,
            ),
            frameBand = "L",
        ),
        entry(
            "hardtail", "Hardtail Mountain Bike 27.5\" — S", "Hardtail MTB — S", "HT-2754", "S 27.5", Grade.B,
            1_700, 8.3, "mk_hardtail", kasun, Fit.GOOD, "Cycling", 16_000,
            Detail(
                listOf(spec("Frame", "S · 27.5\"", true), spec("Fork", "100 mm"), spec("Gears", "1×10")),
                listOf("Bike", "Lock", "Pump"),
                "Sized for riders roughly 155–165 cm. Good for the farm tracks around Kaithady.",
                "Chain and pads new", 17,
            ),
            frameBand = "S",
        ),
        entry(
            "kidsbike", "Kids' Bike 20\" with Stabilisers", "Kids' bike 20\"", "KB-2019", "20\"", Grade.B,
            600, 5.6, "sport_cycling", tharsika, Fit.UNKNOWN, "Cycling", 5_000,
            Detail(
                listOf(spec("Wheels", "20\""), spec("Age", "6–9 years"), spec("Brakes", "V-brakes")),
                listOf("Bike", "Stabilisers", "Kids' helmet"),
                "For riders aged about six to nine. Stabilisers come off with one spanner, included.",
                "Checked last week", 7,
            ),
            instant = true,
        ),
        entry(
            "biketrailer", "Child Bike Trailer, 2 Seats", "Child bike trailer", "BT-0220", "2 SEAT", Grade.A,
            900, 6.6, "mk_bike_trailer", priya, Fit.UNKNOWN, "Cycling", 9_000,
            Detail(
                listOf(spec("Seats", "2 children"), spec("Max load", "40 kg"), spec("Hitch", "Universal axle")),
                listOf("Trailer", "Hitch", "Rain cover"),
                "Hitches to almost any adult bike. Five-point harnesses and a rain cover for the monsoon.",
                "Used 8 times · straps washed", 10,
            ),
            delivery = true,
        ),
        // ---------------------------------------------------------- camping
        entry(
            "packtent", "Ultralight 2-Person Backpacking Tent", "2P backpacking tent", "PT-2014", "2P", Grade.A,
            1_000, 5.7, "mk_backpack_tent", tharsika, Fit.UNKNOWN, "Camping", 8_000,
            Detail(
                listOf(spec("Sleeps", "2"), spec("Weight", "1.6 kg"), spec("Season", "3-season")),
                listOf("Tent", "Poles", "Pegs", "Footprint"),
                "Light enough to carry all day. Pitches in under ten minutes.",
                "Used 12 nights · seams sealed", 11,
            ),
            instant = true,
        ),
        entry(
            "familytent", "6-Person Family Tent with Porch", "6P family tent", "FT-6021", "6P", Grade.B,
            1_500, 3.3, "mk_family_tent", thusaraOwner(), Fit.UNKNOWN, "Camping", 12_000,
            Detail(
                listOf(spec("Sleeps", "6"), spec("Rooms", "2 + porch"), spec("Weight", "14 kg")),
                listOf("Tent", "Poles", "Pegs", "Mallet"),
                "Two bedrooms and a porch to cook under. Needs a car to get it anywhere.",
                "Used 9 trips · one pole replaced", 19,
            ),
        ),
        entry(
            "sleepbag", "Sleeping Bag, Comfort 5 °C", "Sleeping bag 5 °C", "SL-0518", "REG", Grade.A,
            500, 3.0, "mk_sleeping_bag", meera, Fit.UNKNOWN, "Camping", 4_000,
            Detail(
                listOf(spec("Comfort", "5 °C"), spec("Fill", "Synthetic"), spec("Length", "Regular, to 185 cm")),
                listOf("Sleeping bag", "Stuff sack", "Liner"),
                "Washed after every rental and comes with its own clean liner.",
                "Washed after last rental", 2,
            ),
            instant = true,
        ),
        entry(
            "campstove", "Camping Stove and Cook Set", "Camping stove + cook set", "CS-1124", "1–2P", Grade.B,
            450, 10.3, "mk_camp_stove", arun, Fit.UNKNOWN, "Camping", 3_500,
            Detail(
                listOf(spec("Burner", "Gas, 3 kW"), spec("Pots", "1.2 L + 0.8 L"), spec("Fuel", "1 canister included")),
                listOf("Stove", "Pots", "Windscreen", "Gas canister"),
                "Boils a litre in about four minutes. One full canister included.",
                "Burner cleaned", 9,
            ),
        ),
        entry(
            "hammock", "Camping Hammock with Mosquito Net", "Camping hammock + net", "HM-0310", "1P", Grade.A,
            400, 1.4, "mk_hammock", fathima, Fit.UNKNOWN, "Camping", 3_000,
            Detail(
                listOf(spec("Load", "150 kg"), spec("Net", "Built-in"), spec("Straps", "Tree-friendly, 3 m")),
                listOf("Hammock", "Straps", "Carabiners"),
                "Sets up between two palms in five minutes. The net matters more than you'd think.",
                "Used 6 nights", 8,
            ),
            instant = true, delivery = true,
        ),
        entry(
            "coolbox", "40 L Cool Box, Keeps Ice 3 Days", "40 L cool box", "CB-4012", "40L", Grade.B,
            350, 1.6, "sport_camping", dineshOwner(), Fit.UNKNOWN, "Camping", 2_500,
            Detail(
                listOf(spec("Capacity", "40 L"), spec("Ice retention", "Up to 3 days"), spec("Wheels", "Yes")),
                listOf("Cool box", "Ice packs ×4"),
                "Fish, drinks or a whole weekend of food. Rolls on two wheels.",
                "Cleaned and dried", 4,
            ),
        ),
        // ---------------------------------------------------------- trekking
        entry(
            "trekpack", "60 L Trekking Backpack, Adjustable Back", "60 L trekking pack", "TP-6015", "60L", Grade.B,
            650, 8.2, "mk_hiking_pack", kasun, Fit.WORKABLE, "Trekking", 5_000,
            Detail(
                listOf(spec("Capacity", "60 L + 10"), spec("Back length", "Adjustable 44–52 cm", true), spec("Rain cover", "Built-in")),
                listOf("Backpack", "Rain cover"),
                "Fits most torsos once the back length is set. Kasun shows you how at pickup.",
                "Used 14 trips · buckles checked", 13,
            ),
        ),
        entry(
            "trekpoles", "Trekking Poles, Carbon (Pair)", "Trekking poles · pair", "TR-0207", "PAIR", Grade.A,
            250, 3.4, "mk_trek_poles", thusaraOwner(), Fit.UNKNOWN, "Trekking", 2_000,
            Detail(
                listOf(spec("Length", "100–135 cm"), spec("Locks", "Flick-lock"), spec("Tips", "Carbide + rubber caps")),
                listOf("Poles ×2", "Rubber tips", "Baskets"),
                "Save your knees on the way down. Rubber caps for road and rock.",
                "Tips replaced", 5,
            ),
            instant = true,
        ),
        entry(
            "headlamp", "Rechargeable Headlamp, 450 lm", "Headlamp 450 lm", "HL-4509", "450LM", Grade.A,
            300, 6.5, "sport_trekking", priya, Fit.UNKNOWN, "Trekking", 1_500,
            Detail(
                listOf(spec("Output", "450 lumens"), spec("Battery", "USB-C, 8 h on medium"), spec("Red light", "Yes")),
                listOf("Headlamp", "USB-C cable"),
                "For early starts and late finishes. Charged before every handover.",
                "Fully charged", 1,
            ),
            instant = true, delivery = true,
        ),
        // ---------------------------------------------------------- cricket
        entry(
            "cricketkit", "Full Cricket Kit Bag — Pads, Gloves, Helmet", "Full cricket kit bag", "CK-1206", "ADULT", Grade.B,
            1_200, 4.3, "mk_cricket_kit", sanjay, Fit.WORKABLE, "Cricket", 9_000,
            Detail(
                listOf(spec("Pads", "Adult, right-handed", true), spec("Gloves", "Adult"), spec("Helmet", "M · 56–58 cm", true)),
                listOf("Pads", "Gloves", "Helmet", "Thigh guard", "Kit bag"),
                "Everything except the bat and the box. Helmet grille checked before every rental.",
                "Helmet foam replaced", 16, excluded = listOf("Bat (rent separately)", "Box"),
            ),
        ),
        entry(
            "cricketnet", "Portable Cricket Practice Net", "Cricket practice net", "CN-1830", "3×3M", Grade.B,
            1_800, 6.8, "mk_cricket_net", priya, Fit.UNKNOWN, "Cricket", 14_000,
            Detail(
                listOf(spec("Size", "3 m × 3 m × 3 m"), spec("Frame", "Steel, pop-up"), spec("Setup", "About 15 minutes")),
                listOf("Net", "Frame", "Pegs", "Carry bag"),
                "A full batting net for the back garden or the school ground. Priya can deliver within Kopay.",
                "Used 10 times", 18,
            ),
            delivery = true,
        ),
        // ---------------------------------------------------------- badminton
        entry(
            "badmintonset", "Badminton Rackets ×4 and Shuttles", "Badminton set · 4 rackets", "BD-0412", "4 RACKET", Grade.B,
            400, 3.4, "mk_badminton_rackets", thusaraOwner(), Fit.UNKNOWN, "Badminton", 3_000,
            Detail(
                listOf(spec("Rackets", "4 × graphite"), spec("Shuttles", "12 × nylon"), spec("Grip", "G4")),
                listOf("Rackets ×4", "Shuttles ×12", "Bag"),
                "Enough for doubles. Strings checked, grips replaced in the last month.",
                "Grips replaced", 6,
            ),
            instant = true,
        ),
        entry(
            "badmintonnet", "Portable Badminton Net and Posts", "Portable badminton net", "BN-0315", "6.1M", Grade.B,
            600, 1.2, "sport_badminton", fathima, Fit.UNKNOWN, "Badminton", 4_000,
            Detail(
                listOf(spec("Width", "6.1 m, full court"), spec("Height", "Adjustable to 1.55 m"), spec("Setup", "About 5 minutes")),
                listOf("Net", "Posts", "Carry bag"),
                "Turns any garden or beach into a court. No pegs needed on sand.",
                "Net checked for tears", 7,
            ),
            instant = true, delivery = true,
        ),
        // ---------------------------------------------------------- football
        entry(
            "footgoals", "Portable Football Goals 3 m (Pair)", "Portable goals · pair", "FG-3011", "3M PAIR", Grade.B,
            1_500, 1.7, "sport_football", dineshOwner(), Fit.UNKNOWN, "Football", 10_000,
            Detail(
                listOf(spec("Size", "3 m × 2 m"), spec("Frame", "Folding steel"), spec("Pegs", "8 included")),
                listOf("Goals ×2", "Nets", "Pegs", "Carry bags"),
                "Folds flat into the boot of a car. Twenty minutes to set up both.",
                "Nets repaired", 10,
            ),
        ),
        entry(
            "footboots", "Football Boots, Firm Ground — EU 42", "Football boots · EU 42", "FB-4204", "EU 42", Grade.B,
            350, 8.4, "mk_football_boots", kasun, Fit.PERFECT, "Football", 2_500,
            Detail(
                listOf(spec("Size", "EU 42 · UK 8", true), spec("Studs", "Firm ground")),
                listOf("Boots", "Boot bag"),
                "Cleaned and disinfected between rentals.",
                "Cleaned", 3,
            ),
            instant = true,
        ),
        // ---------------------------------------------------------- swimming
        entry(
            "snorkelset", "Snorkel Set — Mask, Snorkel and Fins", "Snorkel set + fins", "SN-0413", "M/L", Grade.A,
            450, 1.3, "mk_snorkel_set", fathima, Fit.WORKABLE, "Swimming", 3_000,
            Detail(
                listOf(spec("Mask", "Tempered glass, adult"), spec("Fins", "EU 41–44", true), spec("Snorkel", "Dry top")),
                listOf("Mask", "Snorkel", "Fins", "Mesh bag"),
                "Sterilised after every rental. Good for the reef at Kayts.",
                "Sterilised", 2,
            ),
            instant = true, delivery = true,
        ),
        entry(
            "swimfins", "Training Fins and Kickboard", "Training fins + kickboard", "SW-0215", "EU 40–42", Grade.B,
            300, 5.5, "mk_swim_fins", tharsika, Fit.GOOD, "Swimming", 2_000,
            Detail(
                listOf(spec("Fins", "Short blade, EU 40–42", true), spec("Kickboard", "EVA foam")),
                listOf("Fins", "Kickboard", "Pull buoy"),
                "Pool training set. Rinsed in fresh water after every use.",
                "Rinsed", 4,
            ),
            instant = true,
        ),
        // ---------------------------------------------------------- climbing
        entry(
            "climbshoes", "Climbing Shoes — EU 41", "Climbing shoes · EU 41", "CL-4108", "EU 41", Grade.B,
            500, 10.1, "mk_climb_shoes", arun, Fit.GOOD, "Climbing", 4_000,
            Detail(
                listOf(spec("Size", "EU 41", true), spec("Fit", "Neutral, all-day"), spec("Closure", "Velcro")),
                listOf("Shoes", "Chalk bag"),
                "Comfortable rather than aggressive. A good first pair to try before you buy.",
                "Resoled 3 months ago", 22,
            ),
        ),
        entry(
            "harness", "Climbing Harness and Belay Device", "Harness + belay device", "HB-2209", "M", Grade.A,
            600, 1.8, "mk_harness", dineshOwner(), Fit.WORKABLE, "Climbing", 6_000,
            Detail(
                listOf(spec("Harness", "M · waist 76–86 cm", true), spec("Belay", "Assisted-braking"), spec("Carabiner", "Screwgate HMS")),
                listOf("Harness", "Belay device", "Screwgate"),
                "Inspected before every rental and retired on schedule. The dates are on the tag.",
                "Inspected", 1,
            ),
            instant = true,
        ),
        entry(
            "crashpad", "Bouldering Crash Pad, Folding", "Bouldering crash pad", "CP-1205", "120×90", Grade.B,
            900, 4.4, "mk_crash_pad", sanjay, Fit.UNKNOWN, "Climbing", 7_000,
            Detail(
                listOf(spec("Size", "120 × 90 × 10 cm"), spec("Foam", "Dual density"), spec("Straps", "Backpack")),
                listOf("Crash pad"),
                "Folds into a backpack. Covers most landings on the sandstone at Keerimalai.",
                "Foam firm, cover patched", 15,
            ),
        ),
        // ---------------------------------------------------------- running
        entry(
            "gpswatch", "GPS Running Watch with Heart Rate", "GPS running watch", "GW-5503", "ONE SIZE", Grade.A,
            550, 6.7, "mk_gps_watch", priya, Fit.UNKNOWN, "Running", 6_000,
            Detail(
                listOf(spec("GPS", "Multi-band"), spec("Battery", "Up to 14 days"), spec("Heart rate", "Wrist")),
                listOf("Watch", "Charging cable"),
                "Try one before you buy. Data is wiped at return in front of you.",
                "Reset to factory", 1,
            ),
            instant = true, delivery = true,
        ),
        entry(
            "hydrovest", "Running Hydration Vest, 5 L", "Hydration vest 5 L", "HV-0506", "S/M", Grade.A,
            300, 3.0, "sport_running", meera, Fit.WORKABLE, "Running", 2_000,
            Detail(
                listOf(spec("Size", "S/M · chest 80–96 cm", true), spec("Capacity", "5 L"), spec("Flasks", "2 × 500 ml")),
                listOf("Vest", "Soft flasks ×2"),
                "For long runs in the heat. Flasks washed and dried.",
                "Washed", 3,
            ),
            instant = true,
        ),
        entry(
            "trailshoes", "Trail Running Shoes — EU 43", "Trail shoes · EU 43", "TS-4302", "EU 43", Grade.B,
            400, 7.9, "mk_trail_shoes", kasun, Fit.GOOD, "Running", 3_000,
            Detail(
                listOf(spec("Size", "EU 43 · UK 9", true), spec("Drop", "6 mm"), spec("Lugs", "5 mm")),
                listOf("Shoes"),
                "Grippy on wet laterite. Cleaned and deodorised after every rental.",
                "Cleaned", 5,
            ),
        ),
    )

    val listings: List<GearItem> get() = entries.map { it.item }

    /*
     * Some of the wider marketplace belongs to members of the original catalogue,
     * so their profiles show more than one or two things. Resolved by function so
     * the people in [Demo] are initialised before this object reads them.
     */
    private fun thusaraOwner() = Demo.thusara
    private fun dineshOwner() = Demo.dinesh
}
