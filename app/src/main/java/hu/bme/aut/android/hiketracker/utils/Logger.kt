package hu.bme.aut.android.hiketracker.utils

import android.content.Context
import android.widget.Toast
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.debugMode
import hu.bme.aut.android.hiketracker.utils.ExternalFileWriter
import java.io.*
import java.time.LocalDateTime

open class Logger ( val context : Context){
    private var header = "====================================" + LocalDateTime.now().toString() + "===================================="
    private var efw = ExternalFileWriter("/hikrdebug", "hikrrunlog.txt")
    var enabled = true

    init{
        try {
            log(header)
        } catch(e: FileNotFoundException){
            enabled = false
            if(debugMode)
                Toast.makeText(context, "File not found: Logging unavailable", Toast.LENGTH_LONG).show()
        } catch (e: IOException){
            enabled = false
            if(debugMode)
                Toast.makeText(context, "IOException: Logging unavailable", Toast.LENGTH_LONG).show()
        }
    }

    open fun log(m: String){
        if(enabled)
            efw.writeToSDFile(context, m)
    }
}