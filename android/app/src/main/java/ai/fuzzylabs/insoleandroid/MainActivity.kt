package ai.fuzzylabs.insoleandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT: Int = 1;
    private val PERMISSION_REQUEST_COARSE_LOCATION: Int = 1

    var statusTextView: TextView? = null
    var scanButton: Button? = null
    var connectButton: Button? = null
    var readingCountTextView: TextView? = null
    var lastReadingTextView: TextView? = null

    var csvWriter: CSVWriter? = null
    var readings: MutableList<IMUReading>? = mutableListOf()

    val baseBluetoothUuidPostfix = "0000-1000-8000-00805F9B34FB"

    private fun uuidFromShortCode16(shortCode16: String): UUID? {
        return UUID.fromString("0000$shortCode16-$baseBluetoothUuidPostfix")
    }

    fun getServiceUUID(): UUID? {
        return uuidFromShortCode16("1FFF")
    }

    var bluetoothLeService: BluetoothLeService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        var mBluetoothLeService: BluetoothLeService? = null
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            Log.d("onServiceConnected", "Connected")
            mBluetoothLeService = (binder as BluetoothLeService.LocalBinder).service
            bluetoothLeService = mBluetoothLeService

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_powermetre)
        statusTextView = findViewById(R.id.statusTextView)
        scanButton = findViewById(R.id.scanBtn)
        connectButton = findViewById(R.id.connectBtn)
        readingCountTextView = findViewById(R.id.readingCountTextView)
        lastReadingTextView = findViewById(R.id.lastReadingTextView)

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
        initBluetooth()
        val str = "1FFF"
        val uuid = uuidFromShortCode16(str)

        Log.d("constructor", uuid.toString())

        val broadcastFilter = IntentFilter()
        broadcastFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        broadcastFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        broadcastFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_FOUND)
        broadcastFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        broadcastFilter.addAction(BluetoothLeService.EXTRA_DATA)
        registerReceiver(gattUpdateReceiver, broadcastFilter)

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        val bound = this.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d("ServiceBinding", bound.toString())

        csvWriter = CSVWriter(applicationContext)
    }

    private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private var mScanning = false
    private val handler = Handler()
    private var foundDevice: BluetoothDevice? = null

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("leScanCallback", "Found device")
            Log.d("leScanCallback", result.device.toString())
            setFoundDevice(result.device)
        }
    }

    fun setFoundDevice(device: BluetoothDevice) {
        bluetoothLeScanner.stopScan(leScanCallback)
        foundDevice = device
        val deviceTextView = findViewById<TextView>(R.id.scannedDeviceTextView)
        deviceTextView.text = foundDevice.toString()
        findViewById<Button>(R.id.connectBtn).isEnabled = true
        statusTextView?.text = "Device found"
    }

    // Handles various events fired by the Service.
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
                    statusTextView?.text = "GATT connected"
                    connectButton?.text = "Disconnect"
                    readings = mutableListOf()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    statusTextView?.text = "GATT disconnected"
                    connectButton?.text = "Connect"
                }
                BluetoothLeService.ACTION_GATT_CHARACTERISTIC_FOUND -> {
                    statusTextView?.text = "GATT characteristic found and subscribed to"
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val imuReading = IMUReading.fromByteArray(intent.getByteArrayExtra("IMU_BYTEARRAY"))
                    lastReadingTextView?.text = imuReading.toString()
                    if (imuReading != null) {
                        readings?.add(imuReading)
                        updateReadingCount()
                    }
                }
            }
        }
    }

    fun saveResults(view: View) {
        csvWriter?.saveResults(readings)
        statusTextView?.text = "${readings?.count()} readings saved"
        readings = mutableListOf()
        updateReadingCount()
    }

    fun updateReadingCount() {
        readingCountTextView?.text = "Readings recorded: ${readings?.count()}"
    }

    fun connectToDevice(view: View) {
        if(bluetoothLeService?.connectionState == BluetoothLeService.STATE_DISCONNECTED) {
            bluetoothLeService?.connectDevice(foundDevice)
        } else {
            bluetoothLeService?.disconnectDevice()
        }
    }

    fun scanDevices(view: View) {
        if (!mScanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                mScanning = false
                Log.d("Scan devices", "Stopped")
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(getServiceUUID())).build()
            val filters: MutableList<ScanFilter> = arrayListOf(filter)
            val scanSettings = ScanSettings.Builder().build()
            mScanning = true
            bluetoothLeScanner.startScan(filters, scanSettings, leScanCallback)
            Log.d("Scan devices", "Started")

        } else {
            mScanning = false
            Log.d("Scan devices", "Stopped")
            bluetoothLeScanner.stopScan(leScanCallback)
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
}