package com.example.autolyricdrive

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.dir.lyricviewx.LyricViewX

class LyricsOverlayView(private val context: Context) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private val viewModel = LyricsViewModel()

    // Lifecycle/SavedState boilerplate for ComposeView in WindowManager
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = viewModelStore
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun show() {
        if (composeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            120.dpToPx(context), // 歌詞表示用の高さ
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        composeView = ComposeView(context).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                LyricsContent(uiState)
            }
            // Set owners
            setTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner, this@LyricsOverlayView)
            setTag(androidx.lifecycle.viewmodel.R.id.view_tree_view_model_store_owner, this@LyricsOverlayView)
            setTag(androidx.savedstate.R.id.view_tree_saved_state_registry_owner, this@LyricsOverlayView)
        }

        windowManager.addView(composeView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun hide() {
        composeView?.let {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            windowManager.removeView(it)
            composeView = null
        }
    }

    @Composable
    fun LyricsContent(state: LyricsViewModel.LyricsState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is LyricsViewModel.LyricsState.Idle -> {
                    Text("楽曲を再生してください", color = Color.White)
                }
                is LyricsViewModel.LyricsState.Loading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                is LyricsViewModel.LyricsState.Success -> {
                    AndroidView(
                        factory = { ctx ->
                            LyricViewX(ctx).apply {
                                setLabel("歌詞を読み込み中...")
                                loadLyric(state.lrc)
                            }
                        },
                        update = { view ->
                            view.updateTime(state.offsetMs.toLong())
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LyricsViewModel.LyricsState.Error -> {
                    Text(state.message, color = Color.Red)
                }
            }
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
