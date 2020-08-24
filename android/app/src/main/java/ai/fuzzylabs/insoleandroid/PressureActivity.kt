package ai.fuzzylabs.insoleandroid

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import ai.fuzzylabs.insoleandroid.ui.main.SectionsPagerAdapter
import ai.fuzzylabs.insoleandroid.viewmodel.PressureViewModel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class PressureActivity : AppCompatActivity() {
    val h = object : Handler() {
        override fun handleMessage(msg: android.os.Message) {
            when (msg.what) {
                RECEIVE_MESSAGE
                -> {
                    val readBuf = msg.obj as ByteArray
                    val strIncom = String(readBuf, 0, msg.arg1)
                    sb.append(strIncom)
                    val endOfLineIndex = sb.indexOf("\n")
                    if (endOfLineIndex > 0) {
                        pressureViewModel.update(sb.substring(0, endOfLineIndex))
                        sb.delete(0, sb.length)
                    }
                    Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                }
            }
        }
    }

    internal val RECEIVE_MESSAGE = 1
    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private val sb = StringBuilder()

    private lateinit var pressureViewModel: PressureViewModel

    private var mConnectedThread: ConnectedThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pressure)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        btAdapter = BluetoothAdapter.getDefaultAdapter()
        checkBTState()

        pressureViewModel = ViewModelProviders.of(this).get(PressureViewModel::class.java)
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                val m = device.javaClass.getMethod(
                    "createInsecureRfcommSocketToServiceRecord",
                    *arrayOf<Class<*>>(UUID::class.java)
                )
                return m.invoke(device, MY_UUID) as BluetoothSocket
            } catch (e: Exception) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e)
            }

        }
        return device.createRfcommSocketToServiceRecord(MY_UUID)
    }

    public override fun onResume() {
        super.onResume()

        Log.d(TAG, "...onResume - try connect...")

        val device = btAdapter!!.getRemoteDevice(address)

        try {
            btSocket = createBluetoothSocket(device)
        } catch (e: IOException) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.message + ".")
        }

        // Discovery is resource intensive
        btAdapter!!.cancelDiscovery()

        Log.d(TAG, "...Connecting...")
        try {
            btSocket!!.connect()
            Log.d(TAG, "....Connection ok...")
        } catch (e: IOException) {
            try {
                btSocket!!.close()
            } catch (e2: IOException) {
                errorExit(
                    "Fatal Error",
                    "In onResume() and unable to close socket during connection failure" + e2.message + "."
                )
            }

        }

        Log.d(TAG, "...Create Socket...")

        mConnectedThread = ConnectedThread(btSocket!!)
        mConnectedThread!!.start()
    }

    public override fun onPause() {
        super.onPause()

        Log.d(TAG, "...In onPause()...")

        try {
            btSocket!!.close()
        } catch (e2: IOException) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.message + ".")
        }

    }

    private fun checkBTState() {
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support")
        } else {
            if (btAdapter!!.isEnabled) {
                Log.d(TAG, "...Bluetooth on...")
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1)
            }
        }
    }

    private fun errorExit(title: String, message: String) {
        Toast.makeText(baseContext, "$title - $message", Toast.LENGTH_LONG).show()
        finish()
    }

    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            val buffer = ByteArray(256)
            var bytes: Int

            while (true) {
                try {
                    bytes = mmInStream!!.read(buffer)
                    h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    break
                }

            }
        }

        // write data to device
        fun write(message: String) {
            Log.d(TAG, "...Data to send: $message...")
            val msgBuffer = message.toByteArray()
            try {
                mmOutStream!!.write(msgBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "...Error data send: " + e.message + "...")
            }

        }
    }

    companion object {
        private val TAG = "smart-insole"
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val address = "A4:CF:12:43:54:CA" // target device
    }
}