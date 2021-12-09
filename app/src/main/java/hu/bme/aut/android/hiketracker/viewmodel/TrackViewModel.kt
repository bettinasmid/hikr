package hu.bme.aut.android.hiketracker.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.PointRepository
import hu.bme.aut.android.hiketracker.utils.TrackLoader
import kotlinx.coroutines.launch

class TrackViewModel : ViewModel() {
    private val repo: PointRepository
    var trackPoints : LiveData<List<Point>>
    var currentPoint: LiveData<Point?>
    private lateinit var loader : TrackLoader

    init{
        val pointDao = TrackerApplication.pointDatabase.pointDao()
        repo = PointRepository(pointDao)
        trackPoints = repo.getAllPoints()
        currentPoint = repo.getCurrent()
    }

    //Make sure to delete outdated points before calling this
    private fun savePoints(points: List<Point>) = viewModelScope.launch{
        repo.insertAll(points)
    }

    fun clearPoints() = viewModelScope.launch {
        repo.deleteAllPoints()
    }

    fun loadTrackFromGpxFile(context : Context, uri: Uri?) = viewModelScope.launch{
        clearPoints()
        loader = TrackLoader()
        val points = loader.loadFile(context, uri)
        if (points.isNotEmpty()){
            savePoints(points)
        }
    }

    fun arePointsAvailable() : Boolean{
        return !(trackPoints.value.isNullOrEmpty())
    }


}