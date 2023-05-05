package com.mgmsdbx.blecentral

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import java.util.*


private fun log(message: String) {
    Log.d("mgm", message)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                Modifier.padding(all = 8.dp)
            ) {
                Text(text = "Scan for Bluetooth LE devices")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    startBleScan()
                }) {
                    Text(text = "Start BLE Scan")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        log("start Bluetooth scan")
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val device = result.device ?: return

            val name: String = result.scanRecord?.deviceName ?: "result.device.name"


            if (name?.contains("Pixel 4") == true) {
                val now = Calendar.getInstance().time
                val id = "$now name=$name address= ${device.name}"
                Log.d("mgm", "\n\nonScanResult id $id\n\n")

                stopBleScan()
                result.device.connectGatt(this@MainActivity, false, bluetoothGattCallback)
            }
        }

        override fun onScanFailed(errorCode: Int) {}
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setReportDelay(0)
        .build()

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    Handler(Looper.getMainLooper()).post {
                        gatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt?.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
//                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here
//
//


                services.forEach { service ->
                    val characteristicsTable = service.characteristics.joinToString(
                        separator = "\n|--",
                        prefix = "|--"
                    ) { it.uuid.toString() }
                    Log.i("printGattTbl", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable")

                    service.characteristics.forEach{
//                        log(it.descriptors.toString())
                        log("read char")

                        this.readCharacteristic(it)

                    }
                }

//                val char1 = gatt
//                    .getService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"))?.getCharacteristic( UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"))


            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {

                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${characteristic.value.toHexString()}")
                        characteristic.descriptors.forEach{
                            log(it.toString())
                        }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }
    }


    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }


    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )

            service.characteristics.forEach{
                log(it.descriptors.toString())

            }
        }
    }
}