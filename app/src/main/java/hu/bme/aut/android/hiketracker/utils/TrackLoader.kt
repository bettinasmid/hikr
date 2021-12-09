package hu.bme.aut.android.hiketracker.utils

import android.content.Context
import android.location.Location
import android.net.Uri
import android.widget.Toast
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.service.calcVarFromMean
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.lang.Math.pow


class TrackLoader{
    private val parser = GPXParser()

    fun loadFile(context: Context, path: Uri?) : MutableList<Point> {
        var parsedGpx : Gpx? = null
        try {
            if(path != null) {
                val instr = context.contentResolver.openInputStream(path)
                parsedGpx = parser.parse(instr)
            } else throw IOException("Cannot open uri: path is null.")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        val points = mutableListOf<Point>()
        if (parsedGpx == null) {
            Toast.makeText(context, "Chosen file is not a GPX file.",Toast.LENGTH_LONG).show()
        } else {
            for (trk in parsedGpx.tracks) {
                var i = 0
                for (trkseg in trk.trackSegments) {
                    for (trkpoint in trkseg.trackPoints) {
                        points.add(trkpoint.toModelPoint())
                        i++
                    }
                }
            }
            //already on background thread via Room
        }
        return points
    }

    private fun TrackPoint.toModelPoint(): Point{
        return Point(
            latitude = this.latitude,
            longitude = this.longitude,
            elevation = this.elevation
        )
    }

}