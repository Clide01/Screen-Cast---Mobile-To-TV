package com.zyrel.appcaster.webrtc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class DiscoveredDevice(val ipAddress: String, val type: String)

class DeviceScanner {

    suspend fun scanAllDevices(localIp: String): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val subnet = localIp.substringBeforeLast(".")
        val discoveredDevices = mutableListOf<DiscoveredDevice>()

        // Common ports to detect if a device is online
        val portsToScan = listOf(
            5173, // Your React TV Receiver
            80,   // Standard Routers / Smart Devices
            443,  // Secure Web Devices
            8080  // Common Development Port
        )

        val jobs = (1..254).map { host ->
            async {
                val testIp = "$subnet.$host"
                var isAlive = false
                var deviceName = "Network Device"

                for (port in portsToScan) {
                    if (isPortOpen(testIp, port, 150)) {
                        isAlive = true
                        if (port == 5173) {
                            deviceName = "AppCaster Receiver (React)"
                        }
                        break // Stop checking ports once we know it's alive
                    }
                }

                if (isAlive) {
                    DiscoveredDevice(ipAddress = testIp, type = deviceName)
                } else null
            }
        }

        jobs.awaitAll().filterNotNull().toCollection(discoveredDevices)
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

