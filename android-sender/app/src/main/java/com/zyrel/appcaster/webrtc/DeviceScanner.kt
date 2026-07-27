package com.zyrel.appcaster.webrtc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class DiscoveredDevice(val ipAddress: String, val type: String)

class DeviceScanner {

    suspend fun scanAllDevices(localIp: String): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val subnet = localIp.substringBeforeLast(".")
        val discoveredDevices = mutableListOf<DiscoveredDevice>()

        val jobs = (1..254).map { host ->
            async {
                val testIp = "$subnet.$host"
                try {
                    val inetAddress = InetAddress.getByName(testIp)
                    
                    // 1. Standard Ping: Send an ICMP request to see if the device exists
                    if (inetAddress.isReachable(300)) {
                        // The device replied! Now check if our React app is running on it
                        if (isPortOpen(testIp, 5173, 200)) {
                            DiscoveredDevice(testIp, "AppCaster Receiver (React)")
                        } else {
                            DiscoveredDevice(testIp, "Network Device (Alive)")
                        }
                    } else {
                        // 2. Fallback: Some modern TVs/Phones block pings for security.
                        // We will forcefully check the React port and standard web port just in case.
                        if (isPortOpen(testIp, 5173, 200)) {
                            DiscoveredDevice(testIp, "AppCaster Receiver (React)")
                        } else if (isPortOpen(testIp, 80, 200)) {
                            DiscoveredDevice(testIp, "Router / Smart TV")
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Wait for all 254 IP addresses to finish scanning and filter out the nulls
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

