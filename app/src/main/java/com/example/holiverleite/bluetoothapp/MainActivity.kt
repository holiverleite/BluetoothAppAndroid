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
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    var TAG = "a"
    val REQUEST_ENABLE_BT = 1
    var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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


        }

        // Device 1 Button
        this.device1.setOnClickListener {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(this.receiver, filter)

            this.bluetoothAdapter?.startDiscovery()
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

    fun updateBluetoothStatusButton() {
        if (this.bluetoothAdapter?.isEnabled == false) {
            this.bluetoothButton.setBackgroundColor(Color.RED)
        } else {
            this.bluetoothButton.setBackgroundColor(Color.GREEN)
        }
    }

    
}
