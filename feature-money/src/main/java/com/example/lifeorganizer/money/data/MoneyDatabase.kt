package com.example.lifeorganizer.money.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MoneyCategory::class, MoneyTransaction::class, Recurring::class, PayeeRule::class],
    version = 1,
    exportSchema = true
)
abstract class MoneyDatabase : RoomDatabase() {
    abstract fun moneyDao(): MoneyDao

    companion object {
        @Volatile
        private var INSTANCE: MoneyDatabase? = null

        fun getDatabase(context: Context): MoneyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, MoneyDatabase::class.java, "money_database")
                    .build().also { INSTANCE = it }
            }

        /** For tests. */
        fun inMemory(context: Context): MoneyDatabase =
            Room.inMemoryDatabaseBuilder(context, MoneyDatabase::class.java).build()
    }
}
