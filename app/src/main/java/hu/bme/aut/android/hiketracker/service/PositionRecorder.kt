package hu.bme.aut.android.hiketracker.service

import android.content.Context
import android.location.Location
import androidx.lifecycle.LifecycleOwner
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.PointRepository
import hu.bme.aut.android.hiketracker.repository.Repository
import hu.bme.aut.android.hiketracker.utils.TrackWriter
import java.time.LocalDateTime
import java.time.ZoneOffset

class PositionRecorder(val context: Context, val repo: Repository) : NewLocationHandler {
    private var points : MutableList<Point> = repo.getAllPoints().value?.toMutableList() ?: mutableListOf<Point>()
    private var counter = 0
    private val date = LocalDateTime.now()
    private var trackName : String = "recorded" + date.year.toString() + date.month.toString() + date.dayOfMonth.toString() + date.toEpochSecond(
        ZoneOffset.MAX).toString()
    private val trackWriter = TrackWriter(context, trackName)

    override suspend fun onNewLocation(location: Location) {
        logger.log("PositionRecorder:onNewLocation")
        val point = location.toPoint()
        points.add(point)
        repo.insert(point)
        if(counter == 5) {
            trackWriter.writeTrackToFile(context, points)
            counter = 0
        }
        counter++
    }

    override suspend fun finish() {
        trackWriter.writeTrackToFile(context, points)
    }
}