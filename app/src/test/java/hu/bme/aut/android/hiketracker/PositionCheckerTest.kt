package hu.bme.aut.android.hiketracker

import android.content.Context
import android.location.Location
import android.os.Looper.getMainLooper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.service.PositionChecker
import hu.bme.aut.android.hiketracker.utils.Logger
import hu.bme.aut.android.hiketracker.service.WarningHandler
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.OLDEST_SDK])
class PositionCheckerTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    private val lifecycleOwner : LifecycleOwner = mock(LifecycleOwner::class.java)
//    private val repo = MockRepository()
    private var positionChecker : PositionChecker
    private var spyChecker : PositionChecker
    private var points : MutableList<Point> = mutableListOf()
    private var locations: List<Location>
    private var results : List<PositionChecker.State>
    private val warner: WarningHandler = mock(WarningHandler::class.java)
    private var logger: Logger = mock(Logger::class.java)
    private var scope = CoroutineScope(Dispatchers.Unconfined)
    private var i = 0

    init{
        val lifecycle = LifecycleRegistry(lifecycleOwner)
        `when`(lifecycleOwner.lifecycle).thenReturn(lifecycle)
        points = setupTrackPoints()
        locations = setupTestLocations()
        results = setupResults()
        positionChecker = PositionChecker(context, lifecycleOwner, points, warner, logger)
//        runBlocking{
//            repo.insertAll(points)
//        }
        shadowOf(getMainLooper()).idle()
        spyChecker = spy(positionChecker)
    }

    @Test
    fun test1(){
        assertThat(positionChecker.revealState()).isEqualTo(PositionChecker.State.initial)
        for(i in locations.indices) {
            scope.launch {
                positionChecker.onNewLocation(locations[i])
            }
            assertThat(positionChecker.revealState()).isEqualTo(results[i])
        }
    }

//    @Test
//    fun test1(){
//        shadowOf(getMainLooper()).idle()
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test2(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test3(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test4(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test5(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test6(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test7(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test8(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        verify(warner).warn(context, "Off track!")
//        i++
//    }
//    @Test
//    fun test9(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//    @Test
//    fun test10(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }
//
//    @Test
//    fun test11(){
//        scope.launch{ positionChecker.onNewLocation(locations[i]) }
//        assertThat(positionChecker.peakState()).isEqualTo(results[i])
//        i++
//    }


    fun setupTrackPoints() : MutableList<Point>{
        var points = mutableListOf<Point>()
        points.addAll(listOf(
            Point(47.421690, 19.214975),
            Point(47.42163650168605, 19.215029655254632),
            Point(47.42129898799968, 19.21472265069098),
            Point(47.420865, 19.214436),
            Point(47.420397, 19.215182),
            Point(47.419762, 19.216298),
            Point(47.420579, 19.217312),
            Point(47.421116, 19.217977),
            Point(47.421225, 19.217044),
            Point(47.421181, 19.216508),
            Point(47.421232, 19.215585),
            Point(47.42158766574735, 19.215271180415066)
        ))
        return points
    }

    fun setupTestLocations(): List<Location> {
        var points = mutableListOf<Point>()
        points.addAll(listOf(
            Point(47.42152409449271, 19.215217272712277), //backwards but ok
            Point(47.42165775463311, 19.214981490647908), //ok
            Point(47.42160625983407, 19.214995050037917), //ok
            Point(47.42141533362297, 19.214822257390605), //ok
            Point(47.420975371865815, 19.214419609174406), //ok
 //           Point(47.42143084287234, 19.21482542424098), //started going backwards, but its ok
            Point(47.42162917190034, 19.215039577633803), //going backwards, but its ok
            Point(47.420954343163935, 19.214405180670916), // right direction, near 4th checkpoint
            Point(47.4207755739542, 19.21409694656962), //deferred by 28meters from the track (allowed is 25)
            Point(47.42082516690537, 19.214402376180885), //got back, 5m near 4th checkpoint
            Point(47.419818258761325, 19.21635164401849), //skipped a point, close to 5th checkpoint
            Point(47.41991081551058, 19.21634627959038), //ok
            Point(47.420003372094406, 19.216461614581462) //ok
        ))
        return points.map{ it.toLocation()}
    }

    fun setupResults(): List<PositionChecker.State>{
        return listOf(
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
  //          PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
            PositionChecker.State.lost,
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack,
            PositionChecker.State.onTrack
        )
    }
}