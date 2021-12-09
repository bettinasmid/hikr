package hu.bme.aut.android.hiketracker.utils

import android.Manifest
import android.content.Context
import android.os.Environment
import permissions.dispatcher.NeedsPermission
import java.io.*

class ExternalFileWriter(var dirName: String, var fileName: String) {
    //source: https://stackoverflow.com/questions/8330276/write-a-file-in-external-storage-in-android/37150620
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun writeToSDFile(context: Context, text: String, append: Boolean = true) {
        val root: File? = context.getExternalFilesDir(null)
        val dir = File(root!!.absolutePath.toString() + dirName)
        dir.mkdirs()
        val file = File(dir, fileName)
        try {
            val fs = FileOutputStream(file, append)
            val pw = PrintWriter(fs)
            pw.println(text)
            pw.flush()
            pw.close()
            fs.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            throw(e)
        } catch (e: IOException) {
            e.printStackTrace()
            throw(e)
        }
    }

}