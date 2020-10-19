package ai.fuzzylabs.wearablemyfoot

import ai.fuzzylabs.wearablemyfoot.imu.IMUReading
import ai.fuzzylabs.wearablemyfoot.imu.IMUSessionService
import android.content.*
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.launch
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.gauravk.audiovisualizer.visualizer.HiFiVisualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking


private val TAG = MainActivity::class.java.simpleName

/**
 * Main activity
 *
 * Shows running visualisation and metrics. Has controls for starting and stopping the recording
 */
@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    private var state: String = STATE_CONNECTED

    private val cadenceTextView: TextView by lazy { findViewById(R.id.cadenceTextView) }
    private val speedTextView: TextView by lazy { findViewById(R.id.speedTextView) }
    private val distanceTextView: TextView by lazy { findViewById(R.id.distanceTextView) }

    private val recordButton: Button by lazy { findViewById(R.id.recordButton) }
    private val stopButton: Button by lazy { findViewById(R.id.stopButton) }
    private val continueButton: Button by lazy { findViewById(R.id.continueButton) }
    private val pauseButton: Button by lazy { findViewById(R.id.pauseButton) }
    private val visualiser: HiFiVisualizer by lazy { findViewById(R.id.visualiser) }
    private val busyProgressBar: ProgressBar by lazy { findViewById(R.id.busyProgressBar) }

    private var sessionService: IMUSessionService? = null

    private val sessionServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            Log.d(TAG, "IMUSessionService Connected")
            sessionService = (binder as IMUSessionService.LocalBinder).service
            cadenceTextView.text = getString(R.string.value_cadence, sessionService?.getCurrentCadence())
            speedTextView.text = getString(R.string.value_speed, sessionService?.getCurrentSpeed())
            distanceTextView.text = getString(R.string.value_distance, sessionService?.getCurrentDistance())
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sessionService = null
        }
    }

    var bluetoothLeService: BluetoothLeService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        var mBluetoothLeService: BluetoothLeService? = null
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            Log.d("onServiceConnected", "Connected")
            mBluetoothLeService = (binder as BluetoothLeService.LocalBinder).service
            bluetoothLeService = mBluetoothLeService
            connectToDevice()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_powermetre)

        val broadcastFilter = IntentFilter()
        broadcastFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        broadcastFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        broadcastFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_FOUND)
        broadcastFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        broadcastFilter.addAction(BluetoothLeService.EXTRA_DATA)
        registerReceiver(gattUpdateReceiver, broadcastFilter)

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        val bound = this.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, bound.toString())

        val sessionBroadcastFilter = IntentFilter()
        sessionBroadcastFilter.addAction(IMUSessionService.METRICS_UPDATED_ACTION)
        sessionBroadcastFilter.addAction(IMUSessionService.SAVED_ACTION)
        sessionBroadcastFilter.addAction(IMUSessionService.VISUALISATION_UPDATED_ACTION)
        sessionBroadcastFilter.addAction(IMUSessionService.DEBUG_ACTION)
        registerReceiver(imuSessionUpdateReceiver, sessionBroadcastFilter)

        val sessionServiceIntent = Intent(this, IMUSessionService::class.java)
        val sessionBound = bindService(
            sessionServiceIntent,
            sessionServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        Log.d("Session Binding", sessionBound.toString())

        updateView()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothLeService?.disconnectDevice()
    }

    private val imuSessionUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                IMUSessionService.METRICS_UPDATED_ACTION -> {
                    val cadence = intent.getDoubleExtra(IMUSessionService.CADENCE_DOUBLE, 0.0)
                    val speed = intent.getDoubleExtra(IMUSessionService.SPEED_DOUBLE, 0.0)
                    val distance = intent.getDoubleExtra(IMUSessionService.DISTANCE_DOUBLE, 0.0)
                    cadenceTextView.text = getString(R.string.value_cadence, cadence)
                    speedTextView.text = getString(R.string.value_speed, speed)
                    distanceTextView.text = getString(R.string.value_distance, distance)
                }
                IMUSessionService.SAVED_ACTION -> {
                    state = STATE_CONNECTED
                    Toast.makeText(applicationContext, getString(R.string.saved_message), Toast.LENGTH_SHORT).show()
                    updateView()
                }
                IMUSessionService.VISUALISATION_UPDATED_ACTION -> {
                    val bytes = intent.getByteArrayExtra(IMUSessionService.VISUALISATION_BYTEARRAY)
                    if(bytes != null) {
                        if(bytes.size < 1000){
                            val paddedBytes = byteArrayOf(*ByteArray(1000 - bytes.size) {127}, *bytes)
                            visualiser.setRawAudioBytes(paddedBytes)
                        } else {
                            visualiser.setRawAudioBytes(bytes)
                        }
                    }
                }
            }
        }
    }

    // Handles various events fired by the BLE Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    private val gattUpdateReceiver = object : BroadcastReceiver() {

        val TAG = "gattUpdateReceiver"

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    sessionService?.reset()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    finish()
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val imuReading =
                        IMUReading.fromByteArray(intent.getByteArrayExtra("IMU_BYTEARRAY"))
                    if (imuReading != null) {
                        runBlocking {
                            launch(Dispatchers.Default) {
                                sessionService?.addReading(imuReading)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Connect to BLE device if not yet connected
     */
    fun connectToDevice() {
        if(bluetoothLeService?.connectionState == BluetoothLeService.STATE_DISCONNECTED) {
            bluetoothLeService?.connectDevice()
        }
    }

    /**
     * Start recording
     */
    fun onRecord(view: View) {
        sessionService?.record()
        state = STATE_RECORDING
        updateView()
    }

    /**
     * Pause recording
     */
    fun onPause(view: View) {
        sessionService?.pauseRecording()
        state = STATE_PAUSED
        updateView()
    }

    /**
     * Continue recording
     */
    fun onContinue(view: View) {
        sessionService?.record()
        state = STATE_RECORDING
        updateView()
    }

    /**
     * Stop recording and save
     */
    fun onStop(view: View) {
        sessionService?.stopRecording()
        state = STATE_BUSY
        Toast.makeText(applicationContext, getString(R.string.stop_message), Toast.LENGTH_SHORT).show()
        updateView()
    }

    /**
     * Go to sessions screen
     */
    fun onSessionButton(view: View) {
        val intent = Intent(applicationContext, SessionsActivity::class.java)
        startActivity(intent)
    }

    private fun updateView() {
        continueButton.visibility = if(state == STATE_PAUSED) View.VISIBLE else View.INVISIBLE
        pauseButton.visibility = if(state == STATE_RECORDING) View.VISIBLE else View.INVISIBLE
        stopButton.visibility = if(state == STATE_PAUSED) View.VISIBLE else View.INVISIBLE
        recordButton.visibility = if(state == STATE_CONNECTED) View.VISIBLE else View.INVISIBLE
        busyProgressBar.visibility = if(state == STATE_BUSY) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        const val STATE_CONNECTED = "ai.fuzzylabs.insoleandroid.MainActivity.STATE_CONNECTED"
        const val STATE_RECORDING = "ai.fuzzylabs.insoleandroid.MainActivity.STATE_RECORDING"
        const val STATE_PAUSED = "ai.fuzzylabs.insoleandroid.MainActivity.STATE_PAUSED"
        const val STATE_BUSY = "ai.fuzzylabs.insoleandroid.MainActivity.STATE_BUSY"
    }
}