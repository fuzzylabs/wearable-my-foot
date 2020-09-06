package ai.fuzzylabs.insoleandroid

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import java.util.*

private val TAG = BluetoothLeService::class.java.simpleName

// A service that interacts with the BLE device via the Android BLE API.
class BluetoothLeService : Service() {
    var connectionState = Companion.STATE_DISCONNECTED

    var bluetoothGatt: BluetoothGatt? = null

    val baseBluetoothUuidPostfix = "0000-1000-8000-00805F9B34FB"

    private fun uuidFromShortCode16(shortCode16: String): UUID? {
        return UUID.fromString("0000$shortCode16-$baseBluetoothUuidPostfix")
    }

    fun getServiceUUID(): UUID? {
        return uuidFromShortCode16("1FFF")
    }

    fun getCharacteristicUUID(): UUID? {
        return uuidFromShortCode16("2FFF")
    }

    fun connectDevice(device: BluetoothDevice?) {
        bluetoothGatt = device?.connectGatt(this, false, this.gattCallback)
    }

    fun disconnectDevice() {
        val imuCharacteristic = bluetoothGatt?.getService(getServiceUUID())?.getCharacteristic(getCharacteristicUUID())
        bluetoothGatt?.setCharacteristicNotification(imuCharacteristic, false)
        bluetoothGatt?.disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = Companion.ACTION_GATT_CONNECTED
                    connectionState = Companion.STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    Log.i(TAG, "Connected to GATT server.")
                    Log.i(TAG, "Request higher MTU: " +
                            gatt.requestMtu(Companion.REQUESTED_MTU))
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = Companion.ACTION_GATT_DISCONNECTED
                    connectionState = Companion.STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                }
            }
        }

        // MTU increase needs to be requested to receive characteristics of 24 bytes
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if(mtu == Companion.REQUESTED_MTU) {
                gatt?.discoverServices()
            } else {
                disconnectDevice()
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS ->{
                    val imuCharacteristic = gatt.getService(getServiceUUID())?.getCharacteristic(getCharacteristicUUID())
                    if(imuCharacteristic != null) {
                        gatt.setCharacteristicNotification(imuCharacteristic, true)
                        val descriptor = imuCharacteristic.getDescriptor(uuidFromShortCode16("2902"))?.apply {
                            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        }
                        gatt.writeDescriptor(descriptor)
                        broadcastUpdate(Companion.ACTION_GATT_CHARACTERISTIC_FOUND)
                    } else {
                        disconnectDevice()
                    }
                }
                else -> Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.d(TAG, "Characteristic changed")
            if (characteristic != null) {
                broadcastUpdate(Companion.ACTION_DATA_AVAILABLE, characteristic)
            }
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        intent.putExtra("IMU_BYTEARRAY", characteristic.value)
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    private val mBinder: IBinder = LocalBinder()

    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    companion object {
        const val ACTION_DEVICE_FOUND = "ai.fuzzylabs.insoleandroid.ACTION_DEVICE_FOUND"
        const val ACTION_GATT_CONNECTED = "ai.fuzzylabs.insoleandroid.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "ai.fuzzylabs.insoleandroid.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_CHARACTERISTIC_FOUND = "ai.fuzzylabs.insoleandroid.ACTION_GATT_CHARACTERISTIC_FOUND"
        const val ACTION_DATA_AVAILABLE = "ai.fuzzylabs.insoleandroid.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "ai.fuzzylabs.insoleandroid.EXTRA_DATA"
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTED = 2
        const val REQUESTED_MTU = 3 + 28
    }
}