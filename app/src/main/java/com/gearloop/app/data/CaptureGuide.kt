package com.gearloop.app.data

/**
 * The seven angles a condition check asks for, chosen for the kind of gear.
 *
 * A surfboard is checked deck, bottom, nose, tail, both rails and the fin box;
 * asking for a "fin box" on a cricket bat or a tent made the guided capture
 * read as a template. Every kind still has seven angles, so the pickup and
 * return sets pair up one for one, and the listing flow asks for the same seven
 * photographs the handover checks against.
 */
object CaptureGuide {

    /** The outline drawn on the damage-marking screen. */
    enum class Shape { BOARD, HULL, BIKE, ITEM }

    private val boards = setOf("Surfing", "SUP", "Snowboarding")

    fun shapeFor(sport: String): Shape = when (sport) {
        in boards -> Shape.BOARD
        "Kayaking" -> Shape.HULL
        "Cycling" -> Shape.BIKE
        else -> Shape.ITEM
    }

    fun anglesFor(sport: String): List<String> = anglesFor(shapeFor(sport))

    fun anglesFor(shape: Shape): List<String> = when (shape) {
        Shape.BOARD -> listOf("Deck", "Bottom", "Nose", "Tail", "Rail L", "Rail R", "Fin box")
        Shape.HULL -> listOf("Top", "Hull", "Bow", "Stern", "Left side", "Right side", "Cockpit")
        Shape.BIKE -> listOf("Drive side", "Other side", "Frame", "Front wheel", "Rear wheel", "Brakes", "Gears")
        Shape.ITEM -> listOf("Front", "Back", "Left side", "Right side", "Top", "Underside", "Accessories")
    }

    /** What each angle asks for, under the guide. */
    fun framingFor(angle: String): String = when (angle) {
        "Deck", "Bottom", "Top", "Hull", "Drive side", "Other side", "Front", "Back" -> "FULL LENGTH"
        "Rail L", "Rail R", "Left side", "Right side" -> "SIDE ON"
        "Accessories" -> "EVERYTHING INCLUDED"
        else -> "CLOSE UP"
    }

    /** The area a tap on the outline names, from where it fell (fractions of width and height). */
    fun areaAt(shape: Shape, x: Float, y: Float): String = when (shape) {
        Shape.BOARD -> when {
            y < 0.18f -> "Nose"
            y > 0.80f -> "Tail"
            x < 0.25f -> "Rail L"
            x > 0.75f -> "Rail R"
            else -> "Deck"
        }
        Shape.HULL -> when {
            y < 0.18f -> "Bow"
            y > 0.82f -> "Stern"
            x < 0.25f -> "Left side"
            x > 0.75f -> "Right side"
            y in 0.40f..0.60f -> "Cockpit"
            else -> "Top"
        }
        Shape.BIKE -> when {
            y > 0.45f && x < 0.38f -> "Rear wheel"
            y > 0.45f && x > 0.62f -> "Front wheel"
            y > 0.55f -> "Gears"
            x > 0.62f -> "Brakes"
            else -> "Frame"
        }
        Shape.ITEM -> when {
            y < 0.2f -> "Top"
            y > 0.8f -> "Underside"
            x < 0.25f -> "Left side"
            x > 0.75f -> "Right side"
            else -> "Front"
        }
    }
}
