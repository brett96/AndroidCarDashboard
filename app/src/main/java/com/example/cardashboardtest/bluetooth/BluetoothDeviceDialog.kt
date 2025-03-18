package com.example.cardashboardtest.bluetooth

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.cardashboardtest.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BluetoothDeviceDialog : DialogFragment() {
    private var deviceSelectedListener: ((BluetoothDevice) -> Unit)? = null
    private var devices: List<BluetoothDevice> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            devices.map { it.name ?: "Unknown Device" }
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select OBD Device")
            .setAdapter(adapter) { _, position ->
                deviceSelectedListener?.invoke(devices[position])
                dismiss()
            }
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .create()
    }

    fun setDevices(devices: List<BluetoothDevice>) {
        this.devices = devices
    }

    fun setDeviceSelectedListener(listener: (BluetoothDevice) -> Unit) {
        deviceSelectedListener = listener
    }

    companion object {
        fun newInstance() = BluetoothDeviceDialog()
    }
} 