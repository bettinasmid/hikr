package hu.bme.aut.android.hiketracker.ui.fragments

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.SHARED_PREFERENCES_NAME
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.TAG_TOTAL_DISTANCE
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.service.TrackingService
import hu.bme.aut.android.hiketracker.service.toLatLng
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.fragment_map.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.util.*


@RuntimePermissions
class MapFragment : Fragment()/*, TrackingService.OnPositionChangedListener*/{
    private val trackViewModel: TrackViewModel by activityViewModels()
    private lateinit var mMap: GoogleMap
    private lateinit var sp : SharedPreferences
    private var currentZoom : Float = 14.0f
    private var currentPosition: LatLng? = null

    val callback = OnMapReadyCallback { googleMap ->
        onMapReady(googleMap)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onMapReady(map: GoogleMap){
        logger.log("onMapReady")
        mMap = map
        mMap.mapType = MAP_TYPE_TERRAIN
        mMap.setOnCameraMoveListener {
            currentPosition = mMap.cameraPosition.target
            currentZoom = mMap.cameraPosition.zoom
        }
        mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener)
        try {
            mMap.isMyLocationEnabled = true
            
        }catch (ex: SecurityException){
            Toast.makeText(context, "Location access denied", Toast.LENGTH_SHORT).show()
        }
        trackViewModel.trackPoints.observe(viewLifecycleOwner, Observer { points ->
            if (points.isNotEmpty()) {
                drawPolyline(points)
            }
        })
        trackViewModel.currentPoint.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                onPositionChanged(it)
            }
        })
        swSatellite.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked)
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            else
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.log("MapFragment:onViewCreated")
        sp = requireContext().getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        tvDistance.text = "Distance"
    }

    private fun drawPolyline(points: List<Point>) {
        logger.log("MapFragment: drawPolyline")
        logger.log("\tcurrentPosition is null: ${currentPosition == null}")
        val mapPoints = points.map{ it -> it.toLatLng()}
        logger.log("\tmapPoints size: ${mapPoints.size}")

        mMap.clear()
        mMap.addPolyline(
            PolylineOptions().clickable(true).color(Color.BLUE).addAll(
                mapPoints
            )
        )
        mMap.addMarker(MarkerOptions().apply {
            position(mapPoints[0])
            title(getString(R.string.starting_point_label))
        })
        //In case of track check mode, show an overview of the track
        if(currentPosition == null)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapPoints[0], currentZoom))
    }

    //source: https://www.zoftino.com/android-show-current-location-on-map-example
    private val onMyLocationButtonClickListener = OnMyLocationButtonClickListener {
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f))
        false
    }

//    override fun onPositionChanged(location: Location) {
    fun onPositionChanged(point: Point) {
        logger.log("MapFragment: onPositionChanged")
        if(currentPosition == null)
            currentZoom = 14.0f
        currentPosition = point.toLatLng()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(currentPosition,
            currentZoom,
            0.0f,
            point.bearing.toFloat())))
        val distance = sp.getFloat(TAG_TOTAL_DISTANCE,0.0F) / 1000
        this.tvDistance.text = getString(R.string.distance_label, "%.1f".format(Locale.ENGLISH, distance))
    }

    //Null out currentPosition when tracking is stopped, so focus is not taken away
    //from the track starting point when a new track is loaded
    fun unsetCurrentPosition(){
        currentPosition = null
    }

}