package com.carparking.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carparking.app.data.model.Car
import com.carparking.app.data.model.ParkingPhoto
import com.carparking.app.data.model.ParkingRecord

@Database(
    entities = [Car::class, ParkingRecord::class, ParkingPhoto::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun parkingRecordDao(): ParkingRecordDao
    abstract fun parkingPhotoDao(): ParkingPhotoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Migration v1 → v2 : ajout de la table parking_photos */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `parking_photos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `parkingId` INTEGER NOT NULL,
                        `photoPath` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`parkingId`) REFERENCES `parking_records`(`id`)
                            ON DELETE CASCADE
                    )"""
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_parking_photos_parkingId` " +
                    "ON `parking_photos` (`parkingId`)"
                )
                // Migre les photos existantes (photoPath dans parking_records)
                db.execSQL(
                    """INSERT INTO parking_photos (parkingId, photoPath, addedAt)
                       SELECT id, photoPath, parkedAt FROM parking_records
                       WHERE photoPath IS NOT NULL"""
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_parking_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
