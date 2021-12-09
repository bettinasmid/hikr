package hu.bme.aut.android.hiketracker.service

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import hu.bme.aut.android.hiketracker.model.Point
import java.util.*

//Location coordinates treated as cartesian coordinates
//returns a direction vector pointing from this to the other point
//calculating as though cartesian coordinates
operator fun Location.minus(other: Location) : Location {
    val lat = this.latitude
    val lng = this.longitude
    val alt = this.altitude
    val bea = this.bearing
    return Location("").apply {
        latitude = lat - other.latitude
        longitude = lng -  other.longitude
        altitude = alt - other.altitude
        bearing = bea - other.bearing
    }
}

//returns actual point
operator fun Location.plus(other: Location) : Location {
    val lat = this.latitude
    val lng = this.longitude
    val alt = this.altitude
    val bea = this.bearing
    return Location("").apply {
        latitude = lat + other.latitude
        longitude = lng +  other.longitude
        altitude = alt + other.altitude
        bearing = bea + other.bearing
    }
}

operator fun Location.times(other: Location): Location{
    val lat = this.latitude
    val lng = this.longitude
    val alt = this.altitude
    val bea = this.bearing
    return Location("").apply {
        latitude = lat*other.latitude
        longitude = lng*other.longitude
        altitude = alt*other.altitude
        bearing = bea*other.bearing
    }
}

//used for vector normalization
operator fun Location.times(scalar : Float): Location{
    val lat = this.latitude
    val lng = this.longitude
    val alt = this.altitude
    val bea = this.bearing
    return Location("").apply {
        latitude = lat*scalar
        longitude = lng*scalar
        altitude = alt*scalar
        bearing = bea*scalar
    }
}

operator fun Location.div(scalar: Float): Location{
    return this.times(1/scalar)
}

fun Location.normalize() : Location{
    val reciprocalLength = 1/this.length()
    val lat = this.latitude
    val lng = this.longitude
    return Location("").apply{
        latitude = lat*reciprocalLength
        longitude = lng*reciprocalLength
    }
}

fun Location.length(): Double{
    val x = this.latitude.toDouble()
    val y = this.longitude.toDouble()
    return kotlin.math.sqrt(x * x + y * y)
}

fun Location.matches(other: Any?): Boolean{
    val distance = this.distanceTo(other as Location)
    return distance < PositionChecker.LOCATION_EQUALITY_TOLERANCE_METERS
}

fun Location.toVectorString(): String{
    return "( ${this.latitude.to6DigitDoubleString()} ; ${this.longitude.to6DigitDoubleString()} )"
}

fun Double.to6DigitDoubleString(): String{
    return "%.6f".format(Locale.ENGLISH, this)
}

fun Location.toLatLng() : LatLng{
    return LatLng(this.latitude, this.longitude)
}

fun Location.bearingDiffWithin(other: Location?, eps: Float): Boolean{
    if(other != null)
        return (this.bearing - other.bearing) < eps || (this.bearing - other.bearing) > 360 - eps
    return true
}

fun Location.toPoint(): Point {
    return Point(
        latitude = this.latitude,
        longitude = this.longitude,
        elevation = this.altitude,
        bearing = this.bearing.toDouble(),
        isCurrent = 0)
}

fun getZeroLocation():Location{
    return Location("").apply{
        latitude = 0.0
        longitude = 0.0
        altitude = 0.0
        bearing = 0.0f
    }
}

//for calibration purposes
fun calcVariance(locations: List<Location>) : Location{
    var sum = getZeroLocation()
    var sumOfSquared = getZeroLocation()
    for ( l in locations) {
        sum = sum.plus(l)
        sumOfSquared.plus(l*l)
    }
    val mean = sum/locations.size.toFloat()
    val meanOfSquared = sumOfSquared/locations.size.toFloat()
    return meanOfSquared - mean*mean
}

fun interpolate(locations: MutableList<Location>): Location {
    var sum = getZeroLocation()
    for ( l in locations) {
        sum = sum.plus(l)
    }
    return sum/locations.size.toFloat()
}

fun calcVarFromMean(locations: MutableList<Location>): Float{
    val mean = interpolate(locations)
    var sum = 0.0F
    for(l in locations){
        val squaredDistance = l.distanceTo(mean)
        sum += squaredDistance*squaredDistance
    }
    return sum/locations.size.toFloat()
}