package ai.fuzzylabs.insoleandroid

import ai.fuzzylabs.insoleandroid.imu.IMUReading
import ai.fuzzylabs.insoleandroid.imu.IMUSessionService
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.gauravk.audiovisualizer.visualizer.HiFiVisualizer


private val TAG = MainActivity::class.java.simpleName

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    private var state: String = STATE_CONNECTED

    private var cadenceTextView: TextView? = null
    private var recordButton: Button? = null
    private var stopButton: Button? = null
    private var continueButton: Button? = null
    private var pauseButton: Button? = null
    private val visualiser: HiFiVisualizer by lazy { findViewById<HiFiVisualizer>(R.id.visualiser) }

    private var sessionService: IMUSessionService? = null

    private val sessionServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            Log.d(TAG, "IMUSessionService Connected")
            sessionService = (binder as IMUSessionService.LocalBinder).service
            cadenceTextView?.text = getString(R.string.value_cadence, sessionService?.getCurrentCadence())
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
        cadenceTextView = findViewById(R.id.cadenceTextView)
        recordButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        pauseButton = findViewById(R.id.pauseButton)
        continueButton = findViewById(R.id.continueButton)


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
        registerReceiver(imuSessionUpdateReceiver, sessionBroadcastFilter)

        val sessionServiceIntent = Intent(this, IMUSessionService::class.java)
        val sessionBound = bindService(
            sessionServiceIntent,
            sessionServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        Log.d("Session Binding", sessionBound.toString())

        updateView()

        val handler = Handler()
        val testVis = object: Runnable {
            override fun run() {
                val ba = sessionService?.getBytes()
                visualiser.setRawAudioBytes(ba)
                handler.postDelayed(this, 100)
            }
        }
        handler.post(testVis)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothLeService?.disconnectDevice()
    }

    private val imuSessionUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                IMUSessionService.METRICS_UPDATED_ACTION -> {
                    val cadence = sessionService?.getCurrentCadence()
                    Log.d(TAG, "Cadence: $cadence")
                    cadenceTextView?.text = getString(R.string.value_cadence, cadence)
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
                        sessionService?.addReading(imuReading)
                    }
                }
            }
        }
    }

    fun connectToDevice() {
        if(bluetoothLeService?.connectionState == BluetoothLeService.STATE_DISCONNECTED) {
            bluetoothLeService?.connectDevice()
        }
    }

    fun onRecord(view: View) {
        sessionService?.record()
        state = STATE_RECORDING
        updateView()
    }

    fun onPause(view: View) {
        sessionService?.pauseRecording()
        state = STATE_PAUSED
        updateView()
    }

    fun onContinue(view: View) {
        sessionService?.record()
        state = STATE_RECORDING
        updateView()
    }

    fun onStop(view: View) {
        sessionService?.stopRecording()
        state = STATE_CONNECTED
        Toast.makeText(applicationContext, getString(R.string.stop_message), Toast.LENGTH_SHORT).show()
        updateView()
    }

    fun onSessionButton(view: View) {
        val intent = Intent(applicationContext, SessionsActivity::class.java)
        startActivity(intent)
    }

    private fun updateView() {
        continueButton?.visibility = if(state == STATE_PAUSED) View.VISIBLE else View.INVISIBLE
        pauseButton?.visibility = if(state == STATE_RECORDING) View.VISIBLE else View.INVISIBLE
        stopButton?.visibility = if(state == STATE_PAUSED) View.VISIBLE else View.INVISIBLE
        recordButton?.visibility = if(state == STATE_CONNECTED) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        const val STATE_CONNECTED = "ai.fuzzylabs.insoleandroid.MainActivity.STATE_CONNECTED"
        const val STATE_RECORDING = "ai.fuzzylabs.insoleandroid.MainActivity.STATE_RECORDING"
        const val STATE_PAUSED = "ai.fuzzylabs.insoleandroid.MainActivity.STATE_PAUSED"
    }
}