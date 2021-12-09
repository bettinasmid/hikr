package hu.bme.aut.android.hiketracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase

@Database(
    version = 4,
    exportSchema = false,
    entities = [RoomPoint::class]
)
abstract class PointDatabase :RoomDatabase(){
    abstract  fun pointDao() : PointDao
}

private lateinit var INSTANCE: PointDatabase

fun getDatabase(context: Context): PointDatabase {
    synchronized(PointDatabase::class) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = databaseBuilder(
                context.applicationContext,
                PointDatabase::class.java,
                "point_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
    return INSTANCE
}