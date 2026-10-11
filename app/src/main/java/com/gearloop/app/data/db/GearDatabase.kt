package com.gearloop.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's one database: SQLite on the device, through Room.
 *
 * Version 1 was the Milestone 03 schema: one renter and one owner on the phone.
 * Version 2 adds accounts and gives every member their own rows, so it cannot
 * be migrated into from version 1 without inventing an owner for each row; an
 * older database is dropped and the sample marketplace written again. Each
 * version is exported to `app/schemas/` on every build.
 */
@Database(
    entities = [
        AccountEntity::class,
        PersonEntity::class,
        ProfileEntity::class,
        SportInterestEntity::class,
        SearchPrefsEntity::class,
        ListingEntity::class,
        SavedListingEntity::class,
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
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GearDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun discovery(): DiscoveryDao
    abstract fun money(): MoneyDao
    abstract fun evidence(): EvidenceDao
    abstract fun exchange(): ExchangeDao
    abstract fun session(): SessionDao
    abstract fun seed(): SeedDao

    companion object {
        const val NAME = "gearloop.db"

        fun build(context: Context): GearDatabase =
            Room.databaseBuilder(context.applicationContext, GearDatabase::class.java, NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /** A database that lives only in memory, for the instrumented tests. */
        fun inMemory(context: Context): GearDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, GearDatabase::class.java).build()
    }
}
