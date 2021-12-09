package hu.bme.aut.android.hiketracker.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger
import hu.bme.aut.android.hiketracker.data.PointDao
import hu.bme.aut.android.hiketracker.data.RoomPoint
import hu.bme.aut.android.hiketracker.model.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PointRepository(private val pointDao: PointDao): Repository{

    override fun getAllPoints(): LiveData<List<Point>> {
        return pointDao.getAllPoints().map{
            roomPoints -> roomPoints.map{
                roomPoint -> roomPoint.toDomainModel()}
        }
    }

    override suspend fun getAllPointsOnce(): List<Point>{
        return pointDao.getAllPointsOnce().map{
                roomPoint -> roomPoint.toDomainModel()
        }
    }

    override fun getCurrent(): LiveData<Point?> {
        val res = pointDao.getCurrent()
        if(res.value != null)
            return MutableLiveData<Point>()
        return pointDao.getCurrent().map{ roomPoint -> roomPoint?.toDomainModel()}
    }

    override suspend fun saveCurrent(point: Point) {
        point.isCurrent = 1
        if ( pointDao.isCurrentSaved() == 0)
            pointDao.insert(point.toRoomModel())
        else
            pointDao.saveCurrent(point.latitude, point.longitude, point.elevation, point.bearing)
    }

    override suspend fun insertAll(points: List<Point>){
        pointDao.insertAll(points.map{point -> point.toRoomModel() })
    }

    override suspend fun insert(point: Point){
        pointDao.insert(point.toRoomModel())
    }

    override suspend fun deleteAllPoints() {
        pointDao.deleteAllPoints()
    }

    private fun RoomPoint.toDomainModel(): Point{
        return Point(
            id = id,
            latitude = latitude,
            longitude = longitude,
            elevation = elevation,
            bearing = bearing,
            isCurrent = isCurrent
        )
    }

    private fun Point.toRoomModel(): RoomPoint{
        return RoomPoint(
            latitude = latitude,
            longitude = longitude,
            elevation = elevation,
            bearing = bearing,
            isCurrent = isCurrent
        )
    }
}