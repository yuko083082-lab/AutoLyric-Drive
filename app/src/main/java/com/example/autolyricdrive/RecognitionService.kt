package com.example.autolyricdrive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.acrcloud.utils.ACRCloudClient
import com.acrcloud.utils.ACRCloudConfig
import com.acrcloud.utils.IACRCloudListener
import kotlinx.coroutines.*
import org.json.JSONObject

class RecognitionService : Service(), IACRCloudListener {

    private val TAG = "RecognitionService"
    private val CHANNEL_ID = "AutoLyricDriveChannel"
    private val NOTIFICATION_ID = 1

    private var mClient: ACRCloudClient? = null
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initACRCloud()
    }

    private fun initACRCloud() {
        val config = ACRCloudConfig().apply {
            this.acrcloudListener = this@RecognitionService
            this.context = this@RecognitionService
            this.host = BuildConfig.ACR_HOST
            this.accessKey = BuildConfig.ACR_ACCESS_KEY
            this.accessSecret = BuildConfig.ACR_ACCESS_SECRET
            this.recorderConfig.rate = 8000
            this.recorderConfig.channels = 1
        }
        mClient = ACRCloudClient()
        mClient?.initWithConfig(config)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        val notification = createNotification("楽曲識別待機中...")
        startForeground(NOTIFICATION_ID, notification)

        startRecognitionLoop()

        return START_STICKY
    }

    private fun startRecognitionLoop() {
        serviceScope.launch {
            while (isActive) {
                Log.d(TAG, "Starting recognition...")
                val started = mClient?.startRecognize() ?: false
                if (!started) {
                    Log.e(TAG, "Failed to start recognition (Check microphone permission or SDK state)")
                }
                
                // 10秒待機してから次の識別へ（ACRCloudの標準的な識別時間は数秒）
                delay(10000)
            }
        }
    }

    // IACRCloudListener Implementation
    override fun onResult(result: String?) {
        mClient?.stopRecognize()
        if (result != null) {
            parseResult(result)
        }
    }

    override fun onVolumeChanged(volume: Double) {
        // 必要に応じて実装
    }

    private fun parseResult(result: String) {
        try {
            val json = JSONObject(result)
            val status = json.getJSONObject("status")
            val code = status.getInt("code")

            if (code == 0) {
                val metadata = json.getJSONObject("metadata")
                val music = metadata.getJSONArray("music").getJSONObject(0)
                
                val title = music.getString("title")
                val artist = music.getJSONArray("artists").getJSONObject(0).getString("name")
                val playOffsetMs = music.getInt("play_offset_ms")

                Log.d(TAG, "Detected: $title by $artist (Offset: ${playOffsetMs}ms)")
                
                // MusicDataBus を通じて UI (ViewModel) へ通知
                serviceScope.launch {
                    MusicDataBus.post(MusicDataBus.MusicInfo(title, artist, playOffsetMs))
                }

                updateNotification("再生中: $title - $artist")
            } else {
                val msg = status.getString("msg")
                Log.w(TAG, "Recognition failed: $msg (Code: $code)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse result JSON", e)
        }
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoLyric-Drive")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // 仮のアイコン
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "AutoLyric-Drive Recognition Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        mClient?.release()
        Log.d(TAG, "Service destroyed")
    }
}
