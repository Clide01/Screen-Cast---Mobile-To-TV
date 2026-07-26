package com.zyrel.appcaster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zyrel.appcaster.webrtc.DeviceScanner
import com.zyrel.appcaster.webrtc.DiscoveredDevice
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private val deviceScanner = DeviceScanner()
    private val discoveredList = mutableListOf<DiscoveredDevice>()
    private lateinit var adapter: ArrayAdapter<DiscoveredDevice>
    private var selectedTargetIp: String? = null

    private val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && selectedTargetIp != null) {
            val data = result.data ?: return@registerForActivityResult
            
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("RESULT_DATA", data)
                putExtra("TARGET_IP", selectedTargetIp)
            }
            startForegroundService(serviceIntent)
            Toast.makeText(this, "Connecting to $selectedTargetIp...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val listView = findViewById<ListView>(R.id.lv_devices)
        val btnScan = findViewById<Button>(R.id.btn_scan)

        // Custom Adapter to handle our new UI layout
        adapter = object : ArrayAdapter<DiscoveredDevice>(this, R.layout.item_device, discoveredList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_device, parent, false)
                val device = getItem(position)
                
                view.findViewById<TextView>(R.id.tv_device_name).text = device?.type ?: "Unknown Device"
                view.findViewById<TextView>(R.id.tv_device_ip).text = device?.ipAddress ?: ""
                
                return view
            }
        }
        
        listView.adapter = adapter

        btnScan.setOnClickListener {
            scanNetwork()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = discoveredList[position]
            selectedTargetIp = device.ipAddress
            
            val captureIntent = projectionManager.createScreenCaptureIntent()
            startMediaProjection.launch(captureIntent)
        }
    }

    private fun scanNetwork() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)

        if (ipAddress == "0.0.0.0") {
            Toast.makeText(this, "Please connect to Wi-Fi", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Scanning network...", Toast.LENGTH_SHORT).show()
            
            val devices = deviceScanner.scanAllDevices(ipAddress)
            
            discoveredList.clear()
            discoveredList.addAll(devices)
            adapter.notifyDataSetChanged()

            if (devices.isEmpty()) {
                Toast.makeText(this@MainActivity, "No devices found.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

