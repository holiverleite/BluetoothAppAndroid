package com.example.holiverleite.bluetoothapp

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (this.bluetoothAdapter?.isEnabled == false) {
            this.bluetoothButton.setBackgroundColor(Color.RED)
        } else {
            this.bluetoothButton.setBackgroundColor(Color.GREEN)
        }
    }

    fun changeBluetoothStatus() {
        if (this.bluetoothAdapter?.isEnabled == true) {
            this.bluetoothAdapter?.disable()
            this.bluetoothButton.setBackgroundColor(Color.RED)
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }
}
