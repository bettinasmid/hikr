package hu.bme.aut.android.hiketracker.utils

import android.content.Context
import android.widget.Toast
import hu.bme.aut.android.hiketracker.model.Point
import java.io.FileNotFoundException
import java.io.IOException

class TrackWriter (context: Context, trackName: String){

    private val gpxHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"bk_hikr\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensions/v3/GpxExtensionsv3.xsd http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n" +
            "\t<metadata>\n" +
            "\t\t<name>${trackName}</name>\n" +
            "\t\t<desc>${trackName}</desc>\n" +
            "\t</metadata>\n" +
            "<trk>\n" +
            "  <name>${trackName}</name>\n" +
            "<trkseg>"
    private val gpxClosure = "</trkseg>\n" +
            "</trk>\n" +
            "</gpx>"

    private var efw = ExternalFileWriter("/recordings", "${trackName}.gpx")

    fun writeTrackToFile(context: Context, points: List<Point>){
        try{
            var buf = StringBuffer()
            buf.append(gpxHeader)
            for (p in points)
                buf.append(p.toGpxString())
            buf.append(gpxClosure)
            efw.writeToSDFile(context, buf.toString(), append = false)
        } catch(e: FileNotFoundException){
            Toast.makeText(context, "File not found: Recording unavailable", Toast.LENGTH_LONG).show()
        } catch (e: IOException){
            Toast.makeText(context, "IOException: Recording unavailable", Toast.LENGTH_LONG).show()
        }
    }
}