package hu.bme.aut.android.hiketracker.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import hu.bme.aut.android.hiketracker.service.to6DigitDoubleString
import io.ticofab.androidgpxparser.parser.domain.TrackPoint

class Point(
    val latitude : Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val bearing: Double = 0.0,
    var isCurrent: Int = 0,
    val id: Long = -1
){
    override fun toString(): String {
        return "point oid:${id} : ${latitude} ; ${longitude} ; ${elevation}"
    }

    fun toLocation(): Location {
        return Location("").apply {
            latitude = this@Point.latitude
            longitude = this@Point.longitude
            altitude = this@Point.elevation
        }
    }

    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    fun toGpxString(): String{
        return "<trkpt lat=\"${latitude}\" lon=\"${longitude}\">\n" +
                "\t<ele>${elevation}</ele>\n" +
                "</trkpt>"
    }
}