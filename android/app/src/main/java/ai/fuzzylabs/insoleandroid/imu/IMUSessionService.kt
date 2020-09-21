package ai.fuzzylabs.insoleandroid.imu

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.*
import java.io.*
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

    override fun onBind(p0: Intent?): IBinder? {
        state = CONNECTED_STATE
        return binder
    }

    private var updateWindowMetricsJob: Job? = null

    fun addReading(reading: IMUReading) {
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
        saveToGPX()
        reset()
    }

    fun reset() {
        startTimestamp = null
        session.clear()
    }

    fun getCurrentCadence(): Double {
        return if (session.elements.size > 0) {
            session.elements.last().cadence
        } else {
            0.0
        }
    }

    private fun updateWindowMetrics() {
        session.updateWindowMetrics()

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
            os.close()
        } catch (e: IOException) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w(TAG, "Error writing $file", e)
        }

        Log.d(TAG, file.toString())
    }

    private fun saveToGPX() {
        val filename = "$startTimestamp.gpx"
        val path: File? = applicationContext.getExternalFilesDir(null)
        val file = File(path, filename)

        val elements = session.elements

        val gpx = Xml.newSerializer()

        try {
            val os: OutputStream = FileOutputStream(file)
            os.writer().use {
                gpx.setOutput(it)
                gpx.startDocument("UTF-8", true)

                gpx.setPrefix("", "http://www.topografix.com/GPX/1/1")
                gpx.setPrefix("gpxtpx","http://www.garmin.com/xmlschemas/TrackPointExtension/v1")
                gpx.setPrefix("xsi", "http://www.w3.org/2001/XMLSchema-instance")

                gpx.startTag("http://www.topografix.com/GPX/1/1", "gpx")
                gpx.attribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd")
                gpx.attribute(null, "version", "1.1")
                gpx.attribute(null, "creator", "WearableMyFoot")

                gpx.startTag(null, "trk")
                gpx.startTag(null, "trkseg")

                elements.forEach { element -> element.fillGPX(gpx) }

                gpx.endTag(null, "trkseg")
                gpx.endTag(null, "trk")

                gpx.endTag("http://www.topografix.com/GPX/1/1", "gpx")
                gpx.endDocument()
                gpx.flush()
            }
            os.close()
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