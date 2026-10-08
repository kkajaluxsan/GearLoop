package com.gearloop.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's one database: SQLite on the device, through Room.
 *
 * Version 1 is the Milestone 03 schema. It is exported to `app/schemas/` on every
 * build, which is both the record a future migration is written against and the
 * source of the data model in the report.
 */
@Database(
    entities = [
        PersonEntity::class,
        ProfileEntity::class,
        SportInterestEntity::class,
        SearchPrefsEntity::class,
        ListingEntity::class,
        RailEntryEntity::class,
        ReviewEntity::class,
        TripEntity::class,
        TripItemEntity::class,
        SearchAlertEntity::class,
        BookingEntity::class,
        WalletTxnEntity::class,
        NotificationEntity::class,
        MessageEntity::class,
        ClaimEntity::class,
        CaptureEntity::class,
        DamageMarkEntity::class,
        VerificationStepEntity::class,
        BadgeEntity::class,
        SwapMatchEntity::class,
        SwapProposalEntity::class,
        CycleMemberEntity::class,
        CalendarBlockEntity::class,
        OwnerTaskEntity::class,
        ClosetItemEntity::class,
        SessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GearDatabase : RoomDatabase() {
    abstract fun discovery(): DiscoveryDao
    abstract fun money(): MoneyDao
    abstract fun evidence(): EvidenceDao
    abstract fun exchange(): ExchangeDao
    abstract fun session(): SessionDao
    abstract fun seed(): SeedDao

    companion object {
        const val NAME = "gearloop.db"

        fun build(context: Context): GearDatabase =
            Room.databaseBuilder(context.applicationContext, GearDatabase::class.java, NAME).build()

        /** A database that lives only in memory, for the instrumented tests. */
        fun inMemory(context: Context): GearDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, GearDatabase::class.java).build()
    }
}
