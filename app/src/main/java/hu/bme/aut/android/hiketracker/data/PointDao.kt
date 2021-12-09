package hu.bme.aut.android.hiketracker.data

import androidx.lifecycle.LiveData
import androidx.room.*
import hu.bme.aut.android.hiketracker.model.Point


@Dao
interface PointDao {
    @Query("SELECT * FROM point WHERE isCurrent = 0")
    fun getAllPoints() : LiveData<List<RoomPoint>>

    @Query("SELECT * FROM point WHERE isCurrent = 0")
    suspend fun getAllPointsOnce() : List<RoomPoint>

    @Query("SELECT * FROM point WHERE isCurrent = 1")
    fun getCurrent() : LiveData<RoomPoint?>

    @Query("SELECT COUNT(*) FROM point WHERE isCurrent = 1")
    suspend fun isCurrentSaved() : Int

    @Query("UPDATE point SET latitude = :lat, longitude = :lon, elevation = :ele, bearing = :bea WHERE isCurrent = 1")
    suspend fun saveCurrent(lat: Double, lon: Double, ele: Double, bea: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertAll(points: List<RoomPoint>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: RoomPoint)

    @Query("DELETE FROM point")
    suspend fun deleteAllPoints()

}