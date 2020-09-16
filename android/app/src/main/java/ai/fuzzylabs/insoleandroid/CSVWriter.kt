package ai.fuzzylabs.insoleandroid

import ai.fuzzylabs.insoleandroid.imu.IMUReading
import android.content.Context
import android.util.Log
import java.io.*
import java.time.Instant


private val TAG = MainActivity::class.java.simpleName

class CSVWriter(val context: Context) {

    fun saveResults(iter: Iterable<IMUReading>?) {
        val filename = "${Instant.now()}.csv"
        val path: File? = context.getExternalFilesDir(null)
        val file = File(path, filename)

        try {
            val os: OutputStream = FileOutputStream(file)
            os.writer().use {
                it.write("time,aX,aY,aZ,gX,gY,gZ\n")
                iter?.forEach { reading -> it.write(reading.toCSVRow()) }
            }
        } catch (e: IOException) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing $file", e)
        }

        Log.d(TAG, file.toString())

    }
}