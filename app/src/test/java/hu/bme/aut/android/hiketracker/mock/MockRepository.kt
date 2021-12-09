package hu.bme.aut.android.hiketracker.mock

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.Repository

class MockRepository : Repository{
    private var livePointList = MutableLiveData<List<Point>>(emptyList<Point>())
    private var pointList = mutableListOf<Point>()

    override fun getAllPoints(): LiveData<List<Point>> {
        return livePointList
    }

    override suspend fun getAllPointsOnce(): List<Point> {
        TODO("Not yet implemented")
    }

    override fun getCurrent(): LiveData<Point?> {
        return MutableLiveData<Point>(pointList.find{it.isCurrent == 1})
    }

    override suspend fun insertAll(points: List<Point>){
        pointList.addAll(points)
        livePointList.value = points
    }

    override suspend fun insert(point: Point){
        pointList.add(point)
        livePointList.value= pointList
    }

    override suspend fun deleteAllPoints() {
        pointList.clear()
        livePointList.value = pointList
    }

    override suspend fun saveCurrent(point: Point) {
        point.isCurrent = 1
        if ( !pointList.any { it.isCurrent == 1})
            pointList.add(point)
        else {
            pointList.removeIf { it.isCurrent == 1 }
            pointList.add(point)
        }
        livePointList.value = pointList
    }

//    override suspend fun markVisited(point: Point){
//        pointList.find{ p -> p == point}?.visited = true
//        livePointList.value = pointList
//    }

}