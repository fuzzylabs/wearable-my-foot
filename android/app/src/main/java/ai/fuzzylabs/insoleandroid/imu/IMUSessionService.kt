package ai.fuzzylabs.insoleandroid.imu

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
    private val handler = Handler()

    var cadence = 0.0

    override fun onBind(p0: Intent?): IBinder? {
        state = CONNECTED_STATE
        return binder
    }

    fun addReading(reading: IMUReading) {
        GlobalScope.launch {
            Log.d(TAG, "reading received")
            if (state == CONNECTED_STATE || state == RECORDING_STATE) {
                session.shiftWindow(reading)
                Log.d(TAG, "Window shifter")

                if (state == RECORDING_STATE) {
                    Log.d(TAG, "Recording added")
                    session.addReading(reading)
                }
                windowCounter++
                Log.d(TAG, "Window counter: $windowCounter")
            }
            if (windowCounter >= windowStep) updateWindowMetrics()
        }
    }

    private fun updateWindowMetrics() {
        cadence = session.getWindowCadence()

        val intent = Intent(METRICS_UPDATED_ACTION)
        sendBroadcast(intent)

        windowCounter = 0
    }

    companion object {
        const val DISCONNECTED_STATE = "ai.fuzzylabs.insoleandroid.imu.DISCONNECTED_STATE"
        const val CONNECTED_STATE = "ai.fuzzylabs.insoleandroid.imu.CONNECTED_STATE"
        const val RECORDING_STATE = "ai.fuzzylabs.insoleandroid.imu.RECORDING_STATE"
        const val METRICS_UPDATED_ACTION = "ai.fuzzylabs.insoleandroud.imu.METRICS_UPDATED_ACTION"
    }
}