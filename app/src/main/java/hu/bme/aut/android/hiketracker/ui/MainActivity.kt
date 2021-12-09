package hu.bme.aut.android.hiketracker.ui

import android.Manifest
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger
import hu.bme.aut.android.hiketracker.service.TrackingService
import hu.bme.aut.android.hiketracker.service.TrackingService.Companion.MODE_RECORD_TRACK
import hu.bme.aut.android.hiketracker.service.TrackingService.Companion.MODE_TRACK_CHECK
import hu.bme.aut.android.hiketracker.ui.fragments.MapFragment
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*
import kotlin.system.exitProcess


@RuntimePermissions
class MainActivity : AppCompatActivity() {
    companion object{
        const val PICK_GPX_FILE = 2
        const val STATE_TRACKING_ON = "trackingOn"
        const val STATE_TRACKING_MODE = "trackingMode"
    }
    private lateinit var sp : SharedPreferences

    private val viewModel : TrackViewModel by viewModels()
    private lateinit var mapFragment: MapFragment

    private lateinit var trackingService: TrackingService
    private var serviceIntent: Intent? = null

    private var trackingOn = false
    private var trackingMode : String = "not set"
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as TrackingService.TrackingServiceBinder
            trackingService = binder.getService()
            trackingService.startLocationMonitoring()
            trackingOn = true
            handleButtons()
            isBound = true
            logger.log("MainActivity:onServiceConnected")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
            logger.log("MainActivity:onServiceDisconnected")

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = getSharedPreferences(TrackerApplication.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        logger.log("MainActivity:onCreate - savedInstanceState: ${if(savedInstanceState == null) "NULL" else "not null"}")
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        val fragManager = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val fragments = fragManager?.childFragmentManager?.fragments
        mapFragment = if(fragments?.get(0) is MapFragment) fragments?.get(0) as MapFragment else fragments?.get(1) as MapFragment
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_map, R.id.navigation_elevation
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //restore state
        if(savedInstanceState != null) {
            if (savedInstanceState.getBoolean(STATE_TRACKING_ON)) {
                trackingOn = true
                trackingMode = savedInstanceState.getString(STATE_TRACKING_MODE)?:"not set"

            }
        }
        else { //savedInstanceState might be null despite tracking being on, if the OS killed the service
            trackingOn = sp.getBoolean(STATE_TRACKING_ON, false)
            trackingMode = sp.getString(STATE_TRACKING_MODE, "not set").toString()
            if(trackingOn && trackingMode != "not set") {
                startTrackingWithPermissionCheck()
            } else{
                viewModel.clearPoints()
                mapFragment.unsetCurrentPosition()
            }
        }
        btnOpen.setOnClickListener { openFilePickerDialog() }
        fabStart.setOnClickListener { onFabStartClicked() }
        fabRecord.setOnClickListener{ onFabRecordClicked() }
        btnOff.setOnClickListener{ onBtnOffClicked() }

        handleButtons()
        viewModel.trackPoints.observe(this, Observer {
           if(it.isEmpty())
                handleButtons()
        })
    }


    override fun onStart() {
        super.onStart()
        logger.log("MainActivity:onStart")
        logger.log("\t$STATE_TRACKING_ON : $trackingOn")
        if(trackingOn) {
            logger.log("\tcalling bindService")
            serviceIntent = Intent(this, TrackingService::class.java)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop(){
        super.onStop()
        logger.log("MainActivity:onStop - isBound was ${isBound}")

        if(isBound){
            unbindService(serviceConnection)
            isBound = false
        }
    }


    private fun openFilePickerDialog(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            type = "*/*"
        }
        startActivityForResult(intent, PICK_GPX_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_GPX_FILE && resultCode == RESULT_OK) {
            viewModel.loadTrackFromGpxFile(this, data?.data)
            mapFragment.unsetCurrentPosition()
            fabStart.isVisible = true
        }
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startTracking(){
        //Set trackingOn to true only after service is bound!
        logger.log("MainActivity:startTracking")
        if(trackingMode == MODE_RECORD_TRACK || trackingMode == MODE_TRACK_CHECK) {
            serviceIntent = Intent(this, TrackingService::class.java)
            serviceIntent?.putExtra("MODE", trackingMode)
            startService(serviceIntent)
            bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)
            val editor = sp.edit()
            editor.putBoolean(STATE_TRACKING_ON, true)
            editor.putString(STATE_TRACKING_MODE, trackingMode)
            editor.apply()
        }
        else
            logger.log("ERROR: MODE IS NOT SET")
    }

    private fun stopTracking(){
        logger.log("MainActivity:stopTracking")
        if(trackingOn) {
            trackingOn = false
            //Kill service
            if(isBound) {
                trackingService.stopLocationMonitoring()
                unbindService(serviceConnection)
                isBound = false
            }
            stopService(serviceIntent)
            mapFragment.unsetCurrentPosition()
            handleButtons()
        }
        val editor = sp.edit()
        editor.putBoolean(STATE_TRACKING_ON, false)
        editor.putString(STATE_TRACKING_MODE, "not set")
        editor.apply()
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showDeniedForFineLocation() {
        Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showNeverAskForFineLocation() {
        Toast.makeText(this, "App won't work this way", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putBoolean(STATE_TRACKING_ON, trackingOn)
            putString(STATE_TRACKING_MODE, trackingMode)
        }
        super.onSaveInstanceState(outState)
        logger.log("MainActivity:onSaveInstanceState")
        logger.log("\t$STATE_TRACKING_ON : $trackingOn")
    }

    override fun onDestroy() {
        logger.log("MainActivity:onDestroy")
        super.onDestroy()
    }

    private fun onFabStartClicked(){
        logger.log("fabStartClick")
        if (!trackingOn) {
            if(trackingMode != MODE_TRACK_CHECK){
                trackingMode = MODE_TRACK_CHECK
            }
            if(viewModel.arePointsAvailable())
                startTrackingWithPermissionCheck()
            else
                Toast.makeText(this, "Load a track first!", Toast.LENGTH_LONG).show()
        } else {
            stopTracking()
        }
        //handle buttons only after service is actually connected
    }

    private fun onFabRecordClicked() {
        logger.log("fabRecordClick")
        if (!trackingOn) { //tracking paused OR hasn't been started at all
            if(trackingMode != MODE_RECORD_TRACK){
                //recording is surely freshly started so we don't need previously saved points
                viewModel.clearPoints()
                trackingMode = MODE_RECORD_TRACK
            }
            startTrackingWithPermissionCheck()
        } else {
            stopTracking()
        }
    }

    private fun onBtnOffClicked(){
        stopTracking()
        viewModel.clearPoints()
        finish()
        exitProcess(0)
    }

    private fun handleButtons(){
        logger.log("handleButtons\ttrackingOn = ${trackingOn}")
        when(trackingMode){
            MODE_TRACK_CHECK -> {
                logger.log("\tMODE_TRACK_CHECK")
                fabStart.apply {
                    setImageResource(if (trackingOn) R.drawable.ic_action_stop else R.drawable.ic_play)
                    isVisible = true
                }
                fabRecord.apply {
                    setImageResource(R.drawable.ic_action_record)
                    isVisible = !trackingOn
                }
            }
            MODE_RECORD_TRACK -> {
                logger.log("\tMODE_RECORD_TRACK")
                fabRecord.apply {
                    setImageResource(if (trackingOn) R.drawable.ic_action_stop else R.drawable.ic_action_record)
                    isVisible = true
                }
                fabStart.apply {
                    setImageResource(R.drawable.ic_play)
                    isVisible = !trackingOn
                }
            }
            else -> {
                logger.log("\tMODE UNKNOWN")
                fabStart.apply {
                    setImageResource(R.drawable.ic_play)
                    isVisible = viewModel.arePointsAvailable()
                }
                fabRecord.apply{
                    setImageResource(R.drawable.ic_action_record)
                    isVisible = true
                }
            }
        }
        btnOpen.isVisible = !trackingOn
    }
}