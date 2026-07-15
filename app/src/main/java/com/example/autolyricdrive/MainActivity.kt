package com.example.autolyricdrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager
    private var overlayView: LyricsOverlayView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)
        overlayView = LyricsOverlayView(this)

        setContent {
            MainScreen(
                permissionManager = permissionManager,
                onStartService = { startRecognitionService() },
                onShowOverlay = { overlayView?.show() },
                onHideOverlay = { overlayView?.hide() }
            )
        }
    }

    private fun startRecognitionService() {
        if (permissionManager.allPermissionsGranted()) {
            val intent = android.content.Intent(this, RecognitionService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}

@Composable
fun MainScreen(
    permissionManager: PermissionManager,
    onStartService: () -> Unit,
    onShowOverlay: () -> Unit,
    onHideOverlay: () -> Unit
) {
    var hasAudio by remember { mutableStateOf(permissionManager.hasAudioPermission()) }
    var hasOverlay by remember { mutableStateOf(permissionManager.hasOverlayPermission()) }
    var isIgnoringBattery by remember { mutableStateOf(permissionManager.isIgnoringBatteryOptimizations()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AutoLyric-Drive Setup", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        PermissionItem("マイク権限", hasAudio)
        PermissionItem("オーバーレイ権限", hasOverlay)
        PermissionItem("バッテリー最適化の除外", isIgnoringBattery)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStartService, enabled = hasAudio && hasOverlay && isIgnoringBattery) {
            Text("サービス開始")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = onShowOverlay) { Text("オーバーレイ表示") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onHideOverlay) { Text("オーバーレイ非表示") }
        }
    }
}

@Composable
fun PermissionItem(name: String, granted: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(name, modifier = Modifier.weight(1f))
        Text(if (granted) "✅ 許可済み" else "❌ 未許可", color = if (granted) androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Red)
    }
}
