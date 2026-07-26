package com.zyrel.appcaster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import com.zyrel.appcaster.webrtc.SignalingServer

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var signalingServer: SignalingServer? = null
    private var targetIp: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", 0) ?: 0
        val resultData = intent?.getParcelableExtra<Intent>("RESULT_DATA")
        targetIp = intent?.getStringExtra("TARGET_IP")

        if (resultCode != 0 && resultData != null) {
            startForeground(1, createNotification())
            
            // 1. Start the WebSocket Server on port 8080
            startSignalingServer()

            // 2. Start capturing the screen
            startProjection(resultCode, resultData)
        }
        return START_NOT_STICKY
    }

    private fun startSignalingServer() {
        signalingServer = SignalingServer(8080).apply {
            onMessageReceived = { message ->
                Log.d("AppCaster", "Received from React: $message")
                // In the next step, we will parse this message and link it to WebRTC
            }
            start() // This boots up the server!
        }
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        Log.d("AppCaster", "App captured. Waiting to stream to $targetIp...")
        // We will link the WebRTC video encoder here next.
    }

    private fun createNotification(): Notification {
        val channelId = "AppCastChannel"
        val channel = NotificationChannel(channelId, "Casting App", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("AppCaster Running")
            .setContentText("Hosting video stream on port 8080...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        signalingServer?.stopServer()
        mediaProjection?.stop()
        super.onDestroy()
    }
}

