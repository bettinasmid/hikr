package hu.bme.aut.android.hiketracker.service

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger


open class WarningHandler(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    private val notifSound =  RingtoneManager.getRingtone(context, notification)

    open fun warn(context: Context, message: String){
        var log =  "\t\t WarningHandler.warn : "

        notifSound.play()
        logger.log(log)
        handler.post{
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

}