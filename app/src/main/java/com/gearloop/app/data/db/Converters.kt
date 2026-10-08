package com.gearloop.app.data.db

import androidx.room.TypeConverter

/**
 * Lists stored as a single column.
 *
 * Every list in this schema is short, owned by its row and never queried into —
 * a listing's photo keys, a booking's ticked checklist items — so a join table
 * would add a query per row for no benefit. The separator is the ASCII unit
 * separator, which no field in the app can contain. Enums need nothing here:
 * Room stores them by name.
 */
class Converters {

    @TypeConverter
    fun stringsToColumn(value: List<String>): String = value.joinToString(SEP)

    @TypeConverter
    fun columnToStrings(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(SEP)

    @TypeConverter
    fun intsToColumn(value: List<Int>): String = value.joinToString(SEP)

    @TypeConverter
    fun columnToInts(value: String): List<Int> =
        if (value.isEmpty()) emptyList() else value.split(SEP).map(String::toInt)

    @TypeConverter
    fun booleansToColumn(value: List<Boolean>): String = value.joinToString(SEP) { if (it) "1" else "0" }

    @TypeConverter
    fun columnToBooleans(value: String): List<Boolean> =
        if (value.isEmpty()) emptyList() else value.split(SEP).map { it == "1" }

    private companion object {
        const val SEP = "\u001F"
    }
}
