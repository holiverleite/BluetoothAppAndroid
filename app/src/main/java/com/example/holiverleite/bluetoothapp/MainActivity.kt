package com.example.holiverleite.bluetoothapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.core.os.HandlerCompat.postDelayed
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    var TAG = "leite"
    var UUID_APP: UUID? = null
    val REQUEST_ENABLE_BT = 1
    var bluetoothAdapter: BluetoothAdapter? = null
    var devices: ArrayList<BluetoothDevice> = arrayListOf()
    var currentButtonPressed: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.UUID_APP = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")

        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            this.bluetoothButton.setOnClickListener {
                this.changeBluetoothStatus()
            }
        }

        this.updateBluetoothStatusButton()

        // Device Master Button
        this.device_master.setOnClickListener {
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)

            this.disableOtherRules()
            AcceptThread(null,"").start()
        }

        // Device 1 Button
        this.device1.setOnClickListener {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(this.receiver, filter)
            this.currentButtonPressed = "1"
            this.bluetoothAdapter?.startDiscovery()
        }

        // Start Button
        this.startButton.setOnClickListener {
            for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
                this.devices.add(device)

                Log.i(TAG,device.name)
                if (device.name == "GT-I8552B") {
//                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
//                    ConnectThread(device,"Testeeeeeee").start()
                    AcceptThread(device,"Testeeee").start()
                }
            }
        }
    }

    // Device Client receiver
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent?.action.toString()
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC

//                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    bluetoothAdapter?.cancelDiscovery()
                    ConnectThread(device,currentButtonPressed).start()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        this.updateBluetoothStatusButton()
    }

    // Other Methods
    fun changeBluetoothStatus() {
        if (this.bluetoothAdapter?.isEnabled == true) {
            this.bluetoothAdapter?.disable()
            this.bluetoothButton.setBackgroundColor(Color.RED)
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    fun disableOtherRules() {
        this.startButton.isVisible = true
        this.device_master.isEnabled = false
        this.device1.isEnabled = false
        this.device2.isEnabled = false
        this.device3.isEnabled = false
        this.device4.isEnabled = false
        this.device5.isEnabled = false
        this.device6.isEnabled = false
        this.device7.isEnabled = false
    }

    fun updateBluetoothStatusButton() {
        if (this.bluetoothAdapter?.isEnabled == false) {
            this.bluetoothButton.setBackgroundColor(Color.RED)
        } else {
            this.bluetoothButton.setBackgroundColor(Color.GREEN)
        }
    }

    // Server
    private inner class AcceptThread(device: BluetoothDevice?, message: String) : Thread() {

        val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device?.createRfcommSocketToServiceRecord(UUID_APP)
        }

        val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("11", UUID_APP)
        }
        val currentMessage = message
        var currentDevice = device

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            if (currentMessage != "") {
                bluetoothAdapter?.cancelDiscovery()

                mmSocket?.use { socket ->
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()

                    val outputStream = mmSocket?.outputStream

                    try {
                        outputStream?.write(currentMessage.toByteArray())
                        outputStream?.flush()
                        currentButtonPressed = ""
                    } catch (e: Exception) {
                        Log.e("client", "Sent")
                    } finally {
                        outputStream?.close()
                        mmSocket?.close()
                    }
                }
            } else {
                var shouldLoop = true
                while (shouldLoop) {
                    val socket: BluetoothSocket? = try {
                        mmServerSocket?.accept()
                    } catch (e: IOException) {
                        Log.e(TAG, "Socket's accept() method failed", e)
                        shouldLoop = false
                        null
                    }

                    socket?.also {
                        var inputStream = socket.inputStream

                        try {
                            sleep(1000) // It's necessary to get the message
                            val available = inputStream.available()
                            val bytes = ByteArray(available)
                            inputStream.read(bytes, 0, available)
                            var message = String(bytes)

                            if (message != "") {
                                when (message) {
                                    "1" -> {
                                        device1.setBackgroundColor(Color.GREEN)
                                        device1.setText("Device 1 Pareado")
                                    }
                                    "2" -> {
                                        device2.setBackgroundColor(Color.GREEN)
                                        device2.setText("Device 2 Pareado")
                                    }
                                    "3" -> {
                                        device3.setBackgroundColor(Color.GREEN)
                                        device3.setText("Device 3 Pareado")
                                    }
                                    "4" -> {
                                        device4.setBackgroundColor(Color.GREEN)
                                        device4.setText("Device 4 Pareado")
                                    }
                                    "5" -> {
                                        device5.setBackgroundColor(Color.GREEN)
                                        device5.setText("Device 5 Pareado")
                                    }
                                    "6" -> {
                                        device6.setBackgroundColor(Color.GREEN)
                                        device6.setText("Device 6 Pareado")
                                    }
                                    "7" -> {
                                        device7.setBackgroundColor(Color.GREEN)
                                        device7.setText("Device 7 Pareado")
                                    }
                                }
                                mmServerSocket?.close()
                                shouldLoop = false
                            }
                        } catch (e: Exception) {

                        }
                    }

                    Log.i(TAG,"")
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    // Client
    private inner class ConnectThread(device: BluetoothDevice, message: String) : Thread() {

        val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID_APP)
        }
        val localMessage = message

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                val inputStream = mmSocket?.inputStream
                val outputStream = mmSocket?.outputStream
                val available = inputStream?.available()

                if (available != null && available != 0) {
                    try {
                        val bytes = ByteArray(available)
                        inputStream.read(bytes,0,available)
                        outputMessage.text = String(bytes)
                    } catch (e: Exception) {

                    } finally {
                        inputStream?.close()
                        mmSocket?.close()
                    }


                } else {
                    try {
                        outputStream?.write(localMessage.toByteArray())
                        outputStream?.flush()
                        currentButtonPressed = ""
                    } catch (e: Exception) {
                        Log.e("client", "Sent")
                    } finally {
                        outputStream?.close()
                        mmSocket?.close()
                    }

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
//                manageMyConnectedSocket(socket)
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}
