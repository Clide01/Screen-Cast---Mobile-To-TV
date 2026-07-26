package com.zyrel.appcaster.webrtc

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class SignalingServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {

    private val TAG = "SignalingServer"
    private var activeClient: WebSocket? = null

    // Callback to pass messages back to our background service
    var onMessageReceived: ((String) -> Unit)? = null

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        Log.d(TAG, "React Receiver Connected: ${conn?.remoteSocketAddress}")
        activeClient = conn
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        Log.d(TAG, "React Receiver Disconnected")
        if (activeClient == conn) {
            activeClient = null
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        Log.d(TAG, "Message from Receiver: $message")
        message?.let {
            onMessageReceived?.invoke(it)
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e(TAG, "WebSocket Error", ex)
    }

    override fun onStart() {
        Log.d(TAG, "Signaling Server started on port $port. Ready for React app to connect.")
    }

    fun sendMessageToReceiver(message: String) {
        if (activeClient != null && activeClient!!.isOpen) {
            activeClient!!.send(message)
        } else {
            Log.w(TAG, "Cannot send message, React Receiver not connected yet.")
        }
    }

    fun stopServer() {
        stop(1000)
    }
}

