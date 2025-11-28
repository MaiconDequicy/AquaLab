package br.iots.aqualab.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.iots.aqualab.data.dao.MedicaoDao
import br.iots.aqualab.model.MedicaoManual

@Database(entities = [MedicaoManual::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicaoDao(): MedicaoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aqualab_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}