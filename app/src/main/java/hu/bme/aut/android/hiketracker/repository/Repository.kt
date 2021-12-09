package hu.bme.aut.android.hiketracker.repository

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import hu.bme.aut.android.hiketracker.model.Point

interface Repository {
    fun getAllPoints(): LiveData<List<Point>>
    suspend fun getAllPointsOnce(): List<Point>
    fun getCurrent(): LiveData<Point?>
    suspend fun insertAll(points: List<Point>)
    suspend fun insert(point: Point)
    suspend fun deleteAllPoints()
    suspend fun saveCurrent(point: Point)
}