package ai.fuzzylabs.wearablemyfoot

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

private val TAG = WelcomeActivity::class.java.simpleName

/**
 * Welcome screen activity
 *
 * Allows scanning for BLE devices and connect
 */
class WelcomeActivity : AppCompatActivity() {

    private var state = NOT_FOUND_STATE

    var scanButton: Button? = null
    var connectButton: Button? = null
    var statusTextView: TextView? = null
    var scanProgressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        scanButton = findViewById(R.id.scanBtn)
        connectButton = findViewById(R.id.connectBtn)
        statusTextView = findViewById(R.id.statusTextView)
        scanProgressBar = findViewById(R.id.scanProgressBar)


        checkPermissions()
        initBluetooth()

        val broadcastFilter = IntentFilter()
        broadcastFilter.addAction(BluetoothLeService.ACTION_DEVICE_NOT_FOUND)
        broadcastFilter.addAction(BluetoothLeService.ACTION_DEVICE_FOUND)
        registerReceiver(scanUpdateReceiver, broadcastFilter)

        val bleServiceIntent = Intent(this, BluetoothLeService::class.java)
        val bound = this.bindService(bleServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "BLE Service Bind: ${bound.toString()}")

        if(!bound) {
            Log.e(TAG, "Cannot proceed, BLE service not bound")
        }

    }

    var bluetoothLeService: BluetoothLeService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        var mBluetoothLeService: BluetoothLeService? = null
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            Log.d("onServiceConnected", "Connected")
            mBluetoothLeService = (binder as BluetoothLeService.LocalBinder).service
            bluetoothLeService = mBluetoothLeService
            startScan()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
            bluetoothLeService = null
        }
    }

    private val scanUpdateReceiver = object : BroadcastReceiver() {

        val TAG: String = WelcomeActivity::class.java.simpleName + ".scanUpdateReceiver"

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_DEVICE_FOUND -> {
                    state = FOUND_STATE
                }
                BluetoothLeService.ACTION_DEVICE_NOT_FOUND -> {
                    state = NOT_FOUND_STATE
                }
            }
            updateView()
        }
    }

    /**
     * Check if the application has required permissions
     */
    private fun checkPermissions() {
        // TODO Check Android version and request permissions accordingly
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("This app needs location access")
            builder.setMessage("Please grant location access so this app can detect peripherals.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener(DialogInterface.OnDismissListener {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
            })
            builder.show()
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("This app needs location access")
            builder.setMessage("Please grant location access so this app can detect peripherals.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener(DialogInterface.OnDismissListener {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_FINE_LOCATION
                )
            })
            builder.show()
        }
    }

    private fun initBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    fun onScanButton(view: View) {
        startScan()
    }

    /**
     * Start scan (if not yet started and BLE service is binded)
     */
    fun startScan() {
        if (bluetoothLeService != null) {
            bluetoothLeService?.scanDevices()
            state = SCANNING_STATE
            updateView()
        }
    }

    /**
     * Connect to a BLE device and go to the Main screen
     */
    @ExperimentalUnsignedTypes
    fun onConnectButton(view: View) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
    }

    /**
     * Go to recorded sessions screen
     */
    fun onSessionButton(view: View) {
        val intent = Intent(applicationContext, SessionsActivity::class.java)
        startActivity(intent)
    }

    /**
     * Update view according to its current state
     */
    fun updateView() {
        scanButton?.isEnabled = (state != SCANNING_STATE)
        connectButton?.isEnabled = (state == FOUND_STATE)
        scanProgressBar?.visibility = if(state == SCANNING_STATE) View.VISIBLE else View.INVISIBLE
        when(state) {
            SCANNING_STATE -> {
                statusTextView?.text = getString(R.string.status_ble_scanning)
            }
            FOUND_STATE -> {
                statusTextView?.text = getString(R.string.status_ble_found, bluetoothLeService?.getDeviceName())
            }
            NOT_FOUND_STATE -> {
                statusTextView?.text = getString(R.string.status_ble_not_found)
            }
        }
    }

    companion object {
        const val SCANNING_STATE = "ai.fuzzylabs.insoleandroid.WelcomeActivity.SCANNING_STATE"
        const val FOUND_STATE = "ai.fuzzylabs.insoleandroid.WelcomeActivity.FOUND_STATE"
        const val NOT_FOUND_STATE = "ai.fuzzylabs.insoleandroid.WelcomeActivity.NOT_FOUND_STATE"

        const val PERMISSION_REQUEST_COARSE_LOCATION: Int = 1
        const val PERMISSION_REQUEST_FINE_LOCATION: Int = 1
        const val REQUEST_ENABLE_BT: Int = 1
    }
}