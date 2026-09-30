package com.samai.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samai.assistant.ui.theme.*
import com.samai.assistant.voice.VoiceState

@Composable
fun VoiceOrb(orbSize: Dp = 120.dp, voiceState: VoiceState = VoiceState.IDLE) {
    val transition = rememberInfiniteTransition(label = "voice_orb")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val activeColor = when (voiceState) {
        VoiceState.LISTENING -> AccentCyan
        VoiceState.PROCESSING -> AccentPurple
        VoiceState.SPEAKING -> AccentBlue
        VoiceState.ERROR -> ErrorRed
        else -> AccentBlue.copy(alpha = 0.5f)
    }

    Canvas(modifier = Modifier.size(orbSize)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val baseRadius = this.size.minDimension * 0.3f

        // Animated rings
        for (i in 1..3) {
            val ringPulse = (pulse + i * 0.33f) % 1f
            val radius = baseRadius + ringPulse * baseRadius * 0.8f
            val alpha = (1f - ringPulse) * 0.4f
            drawCircle(
                color = activeColor.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }

        // Core circle
        drawCircle(
            color = activeColor.copy(alpha = 0.8f),
            radius = baseRadius * 0.5f,
            center = center
        )

        // Outer glow
        drawCircle(
            color = activeColor.copy(alpha = 0.1f),
            radius = baseRadius,
            center = center
        )
    }
}
