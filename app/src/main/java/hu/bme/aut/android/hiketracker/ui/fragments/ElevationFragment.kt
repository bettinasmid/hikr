package hu.bme.aut.android.hiketracker.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.fragment_elevation.*


class ElevationFragment : Fragment() {

    private val trackViewModel: TrackViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_elevation, container, false)
        trackViewModel.trackPoints.observe(viewLifecycleOwner, Observer {
            if(!it.isNullOrEmpty())
                loadElevationData(it)
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chartElevation.setTouchEnabled(true)
        chartElevation.setPinchZoom(true)
        chartElevation.setScaleEnabled(true)
        chartElevation.description.isEnabled = false
        chartElevation.invalidate()
        chartElevation.setDrawGridBackground(false)
        chartElevation.xAxis.position=XAxis.XAxisPosition.BOTTOM
        chartElevation.legend.isEnabled = false
    }

    private fun loadElevationData(points: List<Point>){
        var entries = mutableListOf<Entry>()
        for(i in 0 until (points.size-1)){
            entries.add(Entry(i.toFloat(), points[i].elevation.toFloat()))
            print(points[i].toString() + '\n')
        }
        val dataSet = LineDataSet(entries, "Elevation")

        dataSet.setDrawCircles(false)
        dataSet.setColor(Color.DKGRAY)
        dataSet.setLineWidth(1f)
        dataSet.setValueTextSize(9f)
        dataSet.setDrawFilled(true)
        dataSet.setFormLineWidth(1f)
        dataSet.setFormSize(15f)
        val data = LineData(dataSet)
        chartElevation.data = data
        chartElevation.invalidate()
    }
}