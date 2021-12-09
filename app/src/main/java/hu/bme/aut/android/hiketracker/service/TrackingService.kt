package hu.bme.aut.android.hiketracker.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.TAG_TOTAL_DISTANCE
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger
import hu.bme.aut.android.hiketracker.repository.PointRepository
import kotlinx.coroutines.*
import java.util.*

class TrackingService : LifecycleService(), LocationProvider.OnNewLocationAvailable {
    private val NOTIF_FOREGROUND_ID = 8
    private var startTime : Long = 0
    private val MIN_ALLOWABLE_SPEED_MPS: Float = 0.4F
    private val MAX_ALLOWABLE_SPEED_MPS: Float = 4.0F
    private val LOCATION_UPDATE_TOLERANCE_METERS: Float = 10.0F
    private var mode: String = "not set"

    companion object{
        const val MODE_RECORD_TRACK = "record track"
        const val MODE_TRACK_CHECK = "track check"
    }
    enum class State {
        initializing, stable
    }
    private var state = State.initializing

    //utilities
    private lateinit var notificationHandler : NotificationHandler
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val repo = PointRepository(TrackerApplication.pointDatabase.pointDao())

    //communication with other components
    val handler = Handler(Looper.getMainLooper())
    private lateinit var sp : SharedPreferences
    private lateinit var locationProvider: LocationProvider
    private var locationHandler: NewLocationHandler? = null

    //location monitoring variables
    private var lastLocation: Location? = null //for new location validity check
    private var refPositions = mutableListOf<Location>() //3 element buffer for checking stability & smoothing out the jitter in location updates
    private var adjustmentLimit = 0 //increased when predicting location, so that we don't get trapped in adjustments forever
    private var totalDistance : Float = 0.0F
    private val binder : IBinder = TrackingServiceBinder()

    override fun onCreate() {
        logger.log("service:onCreate")
        super.onCreate()
        locationProvider = LocationProvider(applicationContext,this)
        notificationHandler = NotificationHandler(
            NOTIF_FOREGROUND_ID = NOTIF_FOREGROUND_ID,
            NOTIFICATION_CHANNEL_ID = "hike_tracker_notifications",
            NOTIFICATION_CHANNEL_NAME = "Hike Tracker notifications",
            context = this
        )
        sp = getSharedPreferences(TrackerApplication.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        totalDistance = sp.getFloat(TAG_TOTAL_DISTANCE, 0.0f)
    }

    override fun onBind(p0: Intent): IBinder? {
        logger.log("Service:onBind")
        return binder
    }

    inner class TrackingServiceBinder : Binder() {
        fun getService() : TrackingService{
            return this@TrackingService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startTime = System.currentTimeMillis()
        mode = intent!!.getStringExtra("MODE")?:"not set"
        logger.log("Service:onStartCommand: mode = ${mode}")
        val service = this
        if (mode == MODE_TRACK_CHECK ) {
            serviceScope.launch {
                val points = repo.getAllPointsOnce()
                if(points.isNotEmpty())
                    locationHandler = PositionChecker(service as Context, service as LifecycleOwner, points, WarningHandler(service as Context), logger)
                else
                    handler.post{Toast.makeText(service as Context, "error: no points in memory", Toast.LENGTH_SHORT)}
            }
        }
        else if( mode == MODE_RECORD_TRACK)
            locationHandler = PositionRecorder(this, repo)
        else
            logger.log("\t mode is ${mode}")

        startForeground(NOTIF_FOREGROUND_ID, notificationHandler.createNotification("Return to app"))

        return START_STICKY
    }

    override fun onNewLocation(location: Location) {
        val elapsed = System.currentTimeMillis()-startTime
        logger.log("TrackingService: onNewLocation \t ${(elapsed/60000).toInt()}:${elapsed%60000/1000}")
        logger.log("\t(${location.latitude},${location.longitude})\t" +
                "bearing:${location.bearing}\t" +
                "speed:${location.speed}")
        logger.log("\tdistance to last location: ${lastLocation?.distanceTo(location)}")
        try {
            when (state) {
                State.initializing -> {
                    refPositions.add(location)
                    logger.log("state initializing: refpoints size = ${refPositions.size}")
                    if (refPositions.size >= 3) {
                        if (areConsistent(refPositions)) { //before that we ignore everything
                            lastLocation = refPositions[0]
                            state = State.stable
                            proceedWithLocation(location, false)
                        }
                        refPositions.removeAt(0)
                    }
                }
                State.stable -> {
                    if (!locationIsValid(location) ){
                       if(adjustmentLimit < 1) { //even in the case of invalid location updates we predict maximum once in a row
                            proceedWithLocation(predictInsteadOfActual(), true)
                            adjustmentLimit++
                       }
                        else{
                            proceedWithLocation(location, true) //might be because we are too far from the last correct position
                            adjustmentLimit--.coerceAtLeast(0) //give a new chance to prediction
                       }
                    }
                    else {//location is actually valid, no complications
                        adjustmentLimit--.coerceAtLeast(0)
                        proceedWithLocation(location, true)
                    }
                }
            }
        } catch ( e: Exception ){
            logger.log(Log.getStackTraceString(e))
        }
    }

    private fun proceedWithLocation(location: Location, updateRef: Boolean){
        //update & save state
        if(lastLocation != null)
            totalDistance += location.distanceTo(lastLocation)
        lastLocation = location
        if(updateRef){
            refPositions.add(location)
            refPositions.removeAt(0)
        }
        val editor = sp.edit()
        editor.putFloat(TrackerApplication.TAG_TOTAL_DISTANCE, totalDistance)
        editor.apply()
        serviceScope.launch {
            locationHandler?.onNewLocation(location)
            repo.saveCurrent(location.toPoint())
        }
    }

    public fun startLocationMonitoring(){
        logger.log("Service:startLocationMonitoring")
        locationProvider.startLocationMonitoring()
    }

    public fun stopLocationMonitoring(){
        logger.log("Service:stopLocationMonitoring")
        locationProvider.stopLocationMonitoring()
        serviceScope.launch {
            locationHandler?.finish()
        }
        val editor = sp.edit()
        editor.clear()
        editor.apply()
    }

    //Filter out false location updates
    private fun locationIsValid(location: Location): Boolean{
        //Unconditionally validate the starting location
        if(lastLocation == null) return true
        //Strict checks are disabled for now
        if(/*(location.hasSpeed() && location.speed < MAX_ALLOWABLE_SPEED_MPS && location.speed> MIN_ALLOWABLE_SPEED_MPS) &&*/
            location.distanceTo(lastLocation)< LOCATION_UPDATE_TOLERANCE_METERS)
                return true
        return false
    }

    private fun areConsistent(locations : List<Location>): Boolean{
        var log = "\tconsistency (each should be under 15): "
        for(i in 0..locations.size-2){
            val dist = locations[i].distanceTo(locations[i+1])
            log = log + dist + " , "
            if(dist > 15.0) {
                log += "inconsistent"
                logger.log(log)
                return false
            }
        }
        logger.log(log)
        return true
    }

    private fun predictInsteadOfActual(): Location{
        val prediction = interpolate(refPositions)
        return prediction
    }

    override fun onDestroy(){
        logger.log("Service:onDestroy")
        try {
            serviceScope.launch {
                locationHandler?.finish()
            }
            stopLocationMonitoring()
        } catch ( e: Exception){
            logger.log(Log.getStackTraceString(e))
        }
        stopForeground(true)
        super.onDestroy()
    }
}