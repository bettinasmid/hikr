 package hu.bme.aut.android.hiketracker.service

 import android.content.Context
 import android.location.Location
 import android.os.Handler
 import android.os.Looper
 import android.util.Log
 import android.widget.Toast
 import androidx.lifecycle.*
 import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.debugMode
 import hu.bme.aut.android.hiketracker.utils.Logger
 import hu.bme.aut.android.hiketracker.model.Point
 import java.lang.Math.max

 open class PositionChecker(
     var context: Context,
     lifecycleOwner: LifecycleOwner,
     private val trackPoints: List<Point>,
     private var warner : WarningHandler,
     var logger: Logger
 ) : NewLocationHandler{

     private val MAX_ALLOWABLE_METERS_TO_TRACK: Float = 25.0F
     private val AVG_METERS_BETWEEN_SEGPOINTS : Float  = 10.0F
     private val BEARING_DISCREPANCY_TOLERANCE_DEGREES: Float = 10.0F

     enum class State{
         initial, onTrack, lost
     }
     companion object{
         const val LOCATION_EQUALITY_TOLERANCE_METERS : Float = 12.0F
     }

     private var locationPoints = emptyList<Location>()
     private var extendedLocationPoints = mutableListOf<Location>()
     private var state : State = State.initial
     private val handler = Handler(Looper.getMainLooper())

     init{
         locationPoints = convertTrackPointsToLocation(trackPoints)
         val t = calculateSegmentDivisionFactor(locationPoints)
         for(i in locationPoints.indices-1){
             val segment = calculateSegmentPoints(i,t)
             extendedLocationPoints.addAll(segment)
         }
         extendedLocationPoints.add(locationPoints.last())
         logger.log("PositionChecker: trackPoints size = ${trackPoints.size}, extendedLocationPoints size = ${extendedLocationPoints.size}")
     }

     private fun convertTrackPointsToLocation(points: List<Point>?): List<Location>{
         logger.log("convertTrackPointsToLocation called")
         if(!points.isNullOrEmpty()) {
             val locationPoints = points.map {
                 it.toLocation()
             }
             return locationPoints
         }
         return emptyList()
     }

     private fun calculateSegmentDivisionFactor(points: List<Location>): Int{
         var fullDistance = 0.0F
         for (i in 0..points.size - 2)
             fullDistance += (points[i].distanceTo(points[i + 1]))
         val t =
             max(((fullDistance / points.size.toFloat()) / AVG_METERS_BETWEEN_SEGPOINTS).toInt(),
                 1)
         logger.log("calculateSegmentDivisionFactor called\n\ttotal distance:\t${fullDistance} meters\n\tnumber of trackpoints:\t${points.size}\n\tt:\t$t")
         return t
     }

     override suspend fun onNewLocation(location: Location) {
         if(debugMode)
             handler.post{
                 Toast.makeText(context, "${location.latitude},${location.longitude},\nbearing:${location.bearing}\nspeed:${location.speed}", Toast.LENGTH_SHORT).show()
             }
        try {
            checkPosition(location)
        } catch (e: Exception) {
            logger.log(Log.getStackTraceString(e))
        }
     }


     private fun calculateSegmentPoints(p0idx: Int = 0, t: Int): List<Location>{
         if(p0idx == locationPoints.size-1)
             return emptyList()
         val p0 = locationPoints[p0idx]
         val p1 = locationPoints[p0idx+1]

         val vecToNext = p1-p0
         val dirVector = vecToNext.normalize()
         val step = vecToNext.length()/t
         val segment = mutableListOf<Location>()
         segment.add(p0)
         logger.log("\tcurrentSegment points:")
         for(i in 1 until t){
             val newPoint = p0+dirVector*step.toFloat()*i.toFloat()
             segment.add(newPoint)
             logger.log("\t\tcurrentSegment point $i: ${newPoint.toVectorString()}")
         }
         return segment
     }

     //input: the newly received point
     //decide if user is on track or at least heading back to the track.
     //if not, notify them
     private fun checkPosition(location: Location){
         fun bearingsClose(b1 : Float, b2: Float): Boolean{
             return (b1-b2) < BEARING_DISCREPANCY_TOLERANCE_DEGREES || (b1-b2) > 360 - BEARING_DISCREPANCY_TOLERANCE_DEGREES
         }
         logger.log("checkPosition - state : ${state.name}")
         val hitPoint = extendedLocationPoints.find{loc -> location.distanceTo(loc) < MAX_ALLOWABLE_METERS_TO_TRACK}
         var userCloseToTrack = hitPoint != null
         when(state){
             State.initial -> {
                 if(userCloseToTrack)
                     state = State.onTrack
                 else
                     handler.post {
                         Toast.makeText(context, "Can't find the track?", Toast.LENGTH_SHORT).show()
                     }
                 logger.log("\tuser hasn't started yet")
                 return
             }
             State.lost -> {
                 if(userCloseToTrack) { //found the way back
                     logger.log("\tuser was lost but found the way back to track")
                     state = State.onTrack
                     return
                 }
                 val closestPoint = findClosestPointOnTrack(location)
                 val mindist = closestPoint.distanceTo(location).toDouble()
                 if(!bearingsClose(location.bearing, location.bearingTo(closestPoint)))
                    warner.warn(context, "Off track by ${mindist} meters!")

                 logger.log("\tuser notified, state = lost, distance to track = ${mindist} meters")
                 return
             }
             State.onTrack ->{
                 logger.log("\tuser is close enough to tracksegment: ${userCloseToTrack}")
                 if(!userCloseToTrack){  // user left the track, notify them
                     state = State.lost
                     warner.warn(context, "Off track")
                     logger.log("\tuser notified, state = lost")
                 }
                 else if (debugMode) {
                     val idx = extendedLocationPoints.indexOf(hitPoint)
                     handler.post {
                         Toast.makeText(context,
                             "checkpoint: ${idx}",
                             Toast.LENGTH_SHORT).show()
                     }
                     logger.log("checkpoint: ${idx}")
                 }
             }
         }
     }

     fun findClosestPointOnTrack(location: Location): Location
     {
         var mindistance = 9999.0f
         var closestPoint = extendedLocationPoints[0]
         for(l in extendedLocationPoints){
             if(l.distanceTo(location)<mindistance) {
                 mindistance = l.distanceTo(location)
                 closestPoint = l
             }
         }
         return closestPoint
     }

     override suspend fun finish(){
        //NOP
     }

     //for testing only!
     fun revealState() : State {
         return state
     }
 }