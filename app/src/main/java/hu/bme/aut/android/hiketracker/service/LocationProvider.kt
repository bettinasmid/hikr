package hu.bme.aut.android.hiketracker.service

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class LocationProvider (context: Context, private val onNewLocationAvailable: OnNewLocationAvailable)  {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            onNewLocationAvailable.onNewLocation(locationResult.lastLocation)
        }
    }

    interface OnNewLocationAvailable {
        fun onNewLocation(location: Location)
    }

    @Throws(SecurityException::class)
    fun startLocationMonitoring() {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback, Looper.myLooper())

    }

    @Throws(SecurityException::class)
    fun stopLocationMonitoring() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}