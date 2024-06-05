package com.example.aquaclean


import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothReceiver(private val onBluetoothStateChanged: (Boolean) -> Unit) : BroadcastReceiver() {

    val intentFilter: IntentFilter
        get() = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> onBluetoothStateChanged(true)
                BluetoothAdapter.STATE_OFF -> onBluetoothStateChanged(false)
            }
        }
    }
}
