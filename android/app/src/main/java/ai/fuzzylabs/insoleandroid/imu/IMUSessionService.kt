package ai.fuzzylabs.insoleandroid.imu

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.Instant

@ExperimentalUnsignedTypes
private val TAG = IMUSessionService::class.java.simpleName

@ExperimentalUnsignedTypes
class IMUSessionService : Service() {

    inner class LocalBinder : Binder() {
        val service: IMUSessionService
            get() = this@IMUSessionService
    }
    private val binder = LocalBinder();

    private var state = DISCONNECTED_STATE
    private val session = IMUSession()
    private var windowCounter = 0
    private val windowStep = 100
    private var startTimestamp: Instant? = null

    var cadence = 0.0

    override fun onBind(p0: Intent?): IBinder? {
        state = CONNECTED_STATE
        return binder
    }

    private var updateWindowMetricsJob: Job? = null

    fun addReading(reading: IMUReading) {


            Log.d(TAG, "reading received")
            if (state == CONNECTED_STATE || state == RECORDING_STATE) {
                session.shiftWindow(reading)

                if (state == RECORDING_STATE) {
                    session.addReading(reading)
                }
                windowCounter++
            }
            if (windowCounter >= windowStep) {
                // CPU intensive task, launch in a coroutine
                if(updateWindowMetricsJob?.isActive == true) {
                    return //wait until the previous job has finished
                }
                updateWindowMetricsJob = GlobalScope.launch {
                    updateWindowMetrics()
                }
            }
    }

    fun record() {
        if (startTimestamp == null) startTimestamp = Instant.now()
        state = RECORDING_STATE
    }

    fun pauseRecording() {
        state = CONNECTED_STATE
    }

    fun stopRecording() {
        state = CONNECTED_STATE
        saveToCSV()
        reset()
    }

    fun reset() {
        startTimestamp = null
        session.clear()
    }

    private fun updateWindowMetrics() {
        cadence = session.getWindowCadence()

        val intent = Intent(METRICS_UPDATED_ACTION)
        sendBroadcast(intent)

        windowCounter = 0
    }

    private fun saveToCSV() {
        val filename = "$startTimestamp.csv"
        val path: File? = applicationContext.getExternalFilesDir(null)
        val file = File(path, filename)

        try {
            val os: OutputStream = FileOutputStream(file)
            os.writer().use {
                it.write("time,aX,aY,aZ,gX,gY,gZ\n")
                session.readings.forEach { reading -> it.write(reading.toCSVRow()) }
            }
        } catch (e: IOException) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w(TAG, "Error writing $file", e)
        }

        Log.d(TAG, file.toString())
    }

    fun getBytes(): ByteArray {
        return session.getVisualisationBytes()
    }

    companion object {
        const val DISCONNECTED_STATE = "ai.fuzzylabs.insoleandroid.imu.DISCONNECTED_STATE"
        const val CONNECTED_STATE = "ai.fuzzylabs.insoleandroid.imu.CONNECTED_STATE"
        const val RECORDING_STATE = "ai.fuzzylabs.insoleandroid.imu.RECORDING_STATE"
        const val METRICS_UPDATED_ACTION = "ai.fuzzylabs.insoleandroud.imu.METRICS_UPDATED_ACTION"
    }
}