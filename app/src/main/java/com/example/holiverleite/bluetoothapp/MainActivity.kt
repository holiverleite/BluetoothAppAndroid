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
import android.os.Message
import android.util.Log
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    var TAG = "leite"
    var UUID_APP: UUID? = null
    val REQUEST_ENABLE_BT = 1
    var bluetoothAdapter: BluetoothAdapter? = null
    var masterDevice: BluetoothDevice? = null
    var clientDevice: BluetoothDevice? = null
    var devices: ArrayList<BluetoothDevice> = arrayListOf()
    var currentButtonPressed: String = ""
    var mConnectedThread: ConnectedThread? = null
    var mConnectThread: ConnectThread? = null
    var mSecureAcceptThread: AcceptThread? = null
    var mHandler: Handler? = null

    // Device Client receiver
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent?.action.toString()
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    masterDevice = device
                    bluetoothAdapter?.cancelDiscovery()
                    connect(device)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.UUID_APP = UUID.fromString("15316465-aa57-4690-abcf-172afa2f7e0b")

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
            mSecureAcceptThread = AcceptThread()
            mSecureAcceptThread?.start()
//            AcceptThread().start()
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
                    bluetoothAdapter?.cancelDiscovery()

//                    acopanhar o connect connected ????????
//                    mConnectThread = ConnectThread(device)
//                    mConnectThread?.start()
//                    moutput ta closed !!!!!!
                    mConnectedThread?.write("blablabla")
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

    fun connect(device: BluetoothDevice) {

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        mConnectThread = ConnectThread(device)
        mConnectThread?.start()
    }

    fun connected(socket: BluetoothSocket, device: BluetoothDevice) {

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }

        mConnectedThread = ConnectedThread(socket,device)
        mConnectedThread?.start()
    }



    // Server
    inner class AcceptThread() : Thread() {

        val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("11", UUID_APP)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
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
                    if (socket != null) {
                        connected(it,it.remoteDevice)
                        shouldLoop = false
                    }
                }
            }
//            mmServerSocket?.close()
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
    inner class ConnectThread(device: BluetoothDevice) : Thread() {

        val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID_APP)
        }
        val currentDevice = device

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                val inputStream = socket?.inputStream
                val outputStream = socket?.outputStream
                val available = inputStream?.available()



                try {
                    var message = "1"
                    outputStream?.write(message.toByteArray())
//                    outputStream?.flush()
                } catch (e: Exception) {

                } finally {
//                    outputStream?.close()
//                    inputStream?.close()
//                    mmSocket?.close()
//                    socket.close()
                }

                mConnectThread = null

                connected(socket,currentDevice)
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

    // client 22
    inner class ConnectedThread(mmSocket: BluetoothSocket,device: BluetoothDevice?) : Thread() {

        var mmInStream: InputStream = mmSocket.inputStream
        var mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
        private val currentSocket: BluetoothSocket = mmSocket

        override fun run() {
            while (true) {
                try {
                    sleep(1000) // It's necessary to get the message
                    val available = mmInStream.available()
                    val bytes = ByteArray(available)
                    mmInStream.read(bytes, 0, available)
                    var message = String(bytes)

                    if (message != "") {
                        when (message) {
                            "1" -> {
                                device1.setBackgroundColor(Color.GREEN)
                                device1.setText("Device 1 Pareado")
                                return
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
                            else -> {
                                outputMessage.text = message
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.i(TAG,"error")
                }
            }
        }

        fun write(message: String) {
            try {
//                currentSocket.outputStream.write(message.toByteArray())
                mmOutStream?.write(message.toByteArray())
                mmOutStream?.flush()
            } catch(e: java.lang.Exception) {
                Log.e("client", "Cannot send", e)
            } finally {
                mmOutStream?.close()
                currentSocket?.close()
            }
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                currentSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}
