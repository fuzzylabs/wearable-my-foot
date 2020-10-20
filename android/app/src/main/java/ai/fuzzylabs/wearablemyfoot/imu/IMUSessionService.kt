package ai.fuzzylabs.wearablemyfoot.imu

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.time.Instant

@ExperimentalUnsignedTypes
private val TAG = IMUSessionService::class.java.simpleName

/**
 * Service that handles [IMUSession]
 *
 * Handles session state (connection, recording and saving)
 */
@ExperimentalUnsignedTypes
class IMUSessionService : Service() {

    private val mutex = Mutex()

    inner class LocalBinder : Binder() {
        val service: IMUSessionService
            get() = this@IMUSessionService
    }
    private val binder = LocalBinder();

    private var state = DISCONNECTED_STATE
    private val session = IMUSession()
    private var windowCounter = 0
    private val windowStep = 100
    private var visualisationUpdateCounter = 0
    private var visualisationUpdateStep = 10
    private var startTimestamp: Instant? = null

    override fun onBind(p0: Intent?): IBinder? {
        state = CONNECTED_STATE
        return binder
    }

    private var updateWindowMetricsJob: Job? = null

    /**
     * Add reading to [IMUSession]
     *

     * window, and/or calculate running metrics from the window
     */
    suspend fun addReading(reading: IMUReading) {
        if (state == CONNECTED_STATE || state == RECORDING_STATE) {
            mutex.withLock {
                session.shiftWindow(reading, state == RECORDING_STATE)
            }
            windowCounter++
            visualisationUpdateCounter++
        }
        if (visualisationUpdateCounter >= visualisationUpdateStep) {
            mutex.withLock {
                updateVisialisation()
            }
        }

        if (windowCounter >= windowStep) {
            if(updateWindowMetricsJob?.isActive == true) {
                return //wait until the previous job has finished
            }
            // Potentially CPU intensive task, launch in a coroutine
            updateWindowMetricsJob = GlobalScope.launch {
                mutex.withLock {
                    updateWindowMetrics()
                }
            }
        }
    }

    /**
     * Start or continue session recording
     */
    fun record() {
        if (startTimestamp == null) startTimestamp = Instant.now()
        state = RECORDING_STATE
    }

    /**
     * Pause session recording
     */
    fun pauseRecording() {
        state = CONNECTED_STATE
    }

    /**
     * Stop recording and save results
     */
    fun stopRecording() {
        exportSession()
    }

    /**
     * Export session to CSV and GPX
     */
    private fun exportSession() {
        GlobalScope.launch {
            state = EXPORTING_STATE
            saveToCSV()
            saveToGPX()
            reset()
            val intent = Intent(SAVED_ACTION)
            sendBroadcast(intent)
            state = CONNECTED_STATE
        }
    }

    /**
     * Reset the current [IMUSession]
     */
    fun reset() {
        startTimestamp = null
        session.clear()
    }

    /**
     * Get current value of cadence
     *
     * @return Returns the last calculated cadence estimate (or 0.0, if none is recorded)
     */
    fun getCurrentCadence(): Double = session.currentElement.cadence

    /**
     * Get current speed in kmh
     */
    fun getCurrentSpeed(): Double = session.currentElement.speed * MPS_TO_KMPH

    /**
     * Get current distance run in meters
     */
    fun getCurrentDistance(): Double = session.currentElement.distance

    private fun updateWindowMetrics() {
        session.updateWindowMetrics(windowCounter, state == RECORDING_STATE)

        val intent = Intent(METRICS_UPDATED_ACTION)
        intent.putExtra(CADENCE_DOUBLE, getCurrentCadence())
        intent.putExtra(SPEED_DOUBLE, getCurrentSpeed())
        intent.putExtra(DISTANCE_DOUBLE, getCurrentDistance())
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
                it.write("time,aX,aY,aZ,gX,gY,gZ,pc0,pc1,pc2\n")
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

//        startTimestamp?.let { session.recalculateElements(it) }
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

    private fun updateVisialisation() {
        val intent = Intent(VISUALISATION_UPDATED_ACTION)
        intent.putExtra(VISUALISATION_BYTEARRAY, getBytes())
        sendBroadcast(intent)
        visualisationUpdateCounter = 0
    }

    /**
     * Get bytes for visualisation
     *
     * @return byte array representation of the current window
     */
    fun getBytes(): ByteArray {
        return session.getVisualisationBytes()
    }

    /**
     * @property[DISCONNECTED_STATE] Service is disconnected
     * @property[CONNECTED_STATE] Services is connected
     * @property[EXPORTING_STATE] The current session is being exported
     * @property[RECORDING_STATE] The current session is being recorded
     * @property[METRICS_UPDATED_ACTION] Metrics update is available
     * @property[VISUALISATION_UPDATED_ACTION] ByteArray for visualisation is updated
     * @property[SAVED_ACTION] A session has been saved
     */
    companion object {
        const val MPS_TO_KMPH = 3.6
        const val DISCONNECTED_STATE = "ai.fuzzylabs.insoleandroid.imu.DISCONNECTED_STATE"
        const val CONNECTED_STATE = "ai.fuzzylabs.insoleandroid.imu.CONNECTED_STATE"
        const val EXPORTING_STATE = "ai.fuzzylabs.insoleandroid.imu.EXPORTING_STATE"
        const val RECORDING_STATE = "ai.fuzzylabs.insoleandroid.imu.RECORDING_STATE"
        const val METRICS_UPDATED_ACTION = "ai.fuzzylabs.insoleandroud.imu.METRICS_UPDATED_ACTION"
        const val VISUALISATION_UPDATED_ACTION = "ai.fuzzylabs.insoleandroid.imu.VISUALISATION_UPDATED_ACTION"
        const val SAVED_ACTION = "ai.fuzzylabs.insoleandroud.imu.SAVED_ACTION"
        const val VISUALISATION_BYTEARRAY = "ai.fuzzylabs.insoleandroud.imu.VISUALISATION_BYTEARRAY"
        const val DEBUG_ACTION = "ai.fuzzylabs.insoleandroud.imu.DEBUG_ACTION"
        const val DEBUG_STRING = "ai.fuzzylabs.insoleandroud.imu.DEBUG_STRING"
        const val CADENCE_DOUBLE = "ai.fuzzylabs.insoleandroud.imu.CADENCE_DOUBLE"
        const val SPEED_DOUBLE = "ai.fuzzylabs.insoleandroud.imu.SPEED_DOUBLE"
        const val DISTANCE_DOUBLE = "ai.fuzzylabs.insoleandroud.imu.DISTANCE_DOUBLE"
    }
}