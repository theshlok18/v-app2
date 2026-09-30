package com.samai.assistant.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samai.assistant.ui.components.OrbConfig
import com.samai.assistant.ui.components.OrbState
import com.samai.assistant.ui.components.SAMOrb
import com.samai.assistant.ui.theme.AccentCyan
import com.samai.assistant.ui.theme.SurfaceDark
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class OverlayState { HIDDEN, LISTENING, THINKING, EXECUTING, SPEAKING, SUCCESS, ERROR }

data class OverlayData(
    val state: OverlayState = OverlayState.HIDDEN,
    val message: String = "",
    val progress: Float = 0f
)

@Singleton
class SAMOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private val overlayState = mutableStateOf(OverlayData())

    fun isShowing(): Boolean = overlayView != null

    fun show(state: OverlayState, message: String = "") {
        overlayState.value = OverlayData(state = state, message = message)
        if (overlayView == null) {
            createOverlay()
        }
    }

    fun updateState(state: OverlayState, message: String = "", progress: Float = 0f) {
        overlayState.value = OverlayData(state = state, message = message, progress = progress)
    }

    fun hide() {
        overlayState.value = OverlayData(state = OverlayState.HIDDEN)
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
    }

    fun showResult(success: Boolean, message: String) {
        val state = if (success) OverlayState.SUCCESS else OverlayState.ERROR
        show(state, message)
        // Auto-dismiss after 3 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (overlayState.value.state == state) {
                hide()
            }
        }, 3000)
    }

    private fun createOverlay() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 48
        }

        overlayView = ComposeView(context).apply {
            setContent {
                SAMOverlayContent(
                    data = overlayState.value,
                    onCancel = { hide() }
                )
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (_: Exception) {}
    }
}

@Composable
fun SAMOverlayContent(data: OverlayData, onCancel: () -> Unit) {
    val orbState = when (data.state) {
        OverlayState.LISTENING -> OrbState.LISTENING
        OverlayState.THINKING -> OrbState.THINKING
        OverlayState.EXECUTING -> OrbState.EXECUTING
        OverlayState.SPEAKING -> OrbState.SPEAKING
        OverlayState.SUCCESS -> OrbState.SUCCESS
        OverlayState.ERROR -> OrbState.ERROR
        else -> OrbState.IDLE
    }

    val isExpanded = data.message.isNotEmpty() || data.state == OverlayState.EXECUTING

    AnimatedVisibility(
        visible = data.state != OverlayState.HIDDEN,
        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
        exit = scaleOut(targetScale = 0.8f) + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(if (isExpanded) 24.dp else 50),
            color = SurfaceDark.copy(alpha = 0.92f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mini orb
                SAMOrb(
                    config = OrbConfig(size = 36.dp, auraColor = AccentCyan),
                    state = orbState
                )

                AnimatedContent(targetState = isExpanded, label = "expand") { expanded ->
                    if (expanded) {
                        Column(modifier = Modifier.widthIn(min = 120.dp, max = 240.dp)) {
                            Text(
                                text = data.message.ifEmpty { getStateLabel(data.state) },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (data.state == OverlayState.EXECUTING && data.progress > 0f) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { data.progress },
                                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                    color = AccentCyan,
                                    trackColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = getStateLabel(data.state),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Cancel button
                IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                    Text("×", color = Color.White.copy(alpha = 0.5f), fontSize = MaterialTheme.typography.titleLarge.fontSize)
                }
            }
        }
    }
}

private fun getStateLabel(state: OverlayState): String = when (state) {
    OverlayState.LISTENING -> "Listening..."
    OverlayState.THINKING -> "Thinking..."
    OverlayState.EXECUTING -> "Executing..."
    OverlayState.SPEAKING -> "Speaking..."
    OverlayState.SUCCESS -> "Done"
    OverlayState.ERROR -> "Error"
    else -> ""
}
