package com.samai.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samai.assistant.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState { IDLE, LISTENING, THINKING, EXECUTING, SPEAKING, SUCCESS, ERROR }
enum class OrbType { CLASSIC, ENERGY, NEON, GALAXY, MINIMAL, CUSTOM }

data class OrbConfig(
    val type: OrbType = OrbType.CLASSIC,
    val size: Dp = 160.dp,
    val auraColor: Color = AccentCyan,
    val enableParticles: Boolean = true,
    val enableRings: Boolean = true,
    val enableGlow: Boolean = true,
    val enableVoiceVisualizer: Boolean = true
)

@Composable
fun SAMOrb(
    modifier: Modifier = Modifier,
    config: OrbConfig = OrbConfig(),
    state: OrbState = OrbState.IDLE,
    voiceAmplitude: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sam_orb")

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(getAnimDuration(state), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breathe"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(getRotationDuration(state), easing = LinearEasing)
        ), label = "rotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ), label = "particles"
    )

    val stateColor = getStateColor(state)
    val effectiveAmplitude = when (state) {
        OrbState.LISTENING -> voiceAmplitude.coerceIn(0.1f, 1f)
        OrbState.SPEAKING -> pulseAlpha
        OrbState.EXECUTING -> breathe
        else -> 0.5f
    }

    Canvas(modifier = modifier.size(config.size)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = size.minDimension * 0.28f
        val scaledRadius = baseRadius * breathe

        // Background glow
        if (config.enableGlow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stateColor.copy(alpha = 0.25f * pulseAlpha),
                        stateColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = baseRadius * 2.2f
                ),
                radius = baseRadius * 2.2f,
                center = Offset(centerX, centerY)
            )
        }

        // Rotating rings
        if (config.enableRings) {
            for (ring in 0 until 3) {
                val ringRadius = scaledRadius * (1.3f + ring * 0.25f)
                val ringRotation = rotation + ring * 120f
                val alpha = (1f - ring * 0.25f) * 0.4f * effectiveAmplitude

                drawContext.canvas.save()
                drawContext.canvas.translate(centerX, centerY)
                drawContext.canvas.rotate(ringRotation)

                drawArc(
                    color = stateColor.copy(alpha = alpha),
                    startAngle = 0f,
                    sweepAngle = 240f - ring * 40f,
                    useCenter = false,
                    topLeft = Offset(-ringRadius, -ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )

                drawContext.canvas.restore()
            }
        }

        // Orb core gradient
        val coreBrush = when (config.type) {
            OrbType.CLASSIC -> Brush.radialGradient(
                colors = listOf(stateColor.copy(alpha = 0.9f), stateColor.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(centerX, centerY), radius = scaledRadius
            )
            OrbType.ENERGY -> Brush.sweepGradient(
                colors = listOf(stateColor, AccentPurple, AccentBlue, stateColor)
            )
            OrbType.NEON -> Brush.radialGradient(
                colors = listOf(stateColor.copy(alpha = 1f), stateColor.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(centerX, centerY), radius = scaledRadius
            )
            OrbType.GALAXY -> Brush.radialGradient(
                colors = listOf(AccentPurple.copy(alpha = 0.8f), stateColor.copy(alpha = 0.5f), Black.copy(alpha = 0.3f)),
                center = Offset(centerX, centerY), radius = scaledRadius
            )
            OrbType.MINIMAL -> Brush.radialGradient(
                colors = listOf(stateColor.copy(alpha = 0.4f), stateColor.copy(alpha = 0.1f)),
                center = Offset(centerX, centerY), radius = scaledRadius
            )
            OrbType.CUSTOM -> Brush.radialGradient(
                colors = listOf(stateColor, config.auraColor.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(centerX, centerY), radius = scaledRadius
            )
        }

        drawCircle(
            brush = coreBrush,
            radius = scaledRadius,
            center = Offset(centerX, centerY)
        )

        // Inner bright core
        drawCircle(
            color = Color.White.copy(alpha = 0.15f * effectiveAmplitude),
            radius = scaledRadius * 0.4f,
            center = Offset(centerX, centerY)
        )

        // Voice visualizer bars
        if (config.enableVoiceVisualizer && (state == OrbState.LISTENING || state == OrbState.SPEAKING)) {
            val bars = 16
            for (i in 0 until bars) {
                val angle = (i * 360f / bars) + rotation
                val barHeight = (0.3f + effectiveAmplitude * 0.7f * abs(sin((i + particlePhase * bars) * 1.2f))) * scaledRadius * 0.5f
                val rad = Math.toRadians(angle.toDouble())
                val startX = centerX + cos(rad.toFloat()) * scaledRadius * 1.15f
                val startY = centerY + sin(rad.toFloat()) * scaledRadius * 1.15f
                val endX = centerX + cos(rad.toFloat()) * (scaledRadius * 1.15f + barHeight)
                val endY = centerY + sin(rad.toFloat()) * (scaledRadius * 1.15f + barHeight)

                drawLine(
                    color = stateColor.copy(alpha = 0.7f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Particles
        if (config.enableParticles) {
            val particleCount = getParticleCount(state)
            for (i in 0 until particleCount) {
                val angle = (i * 360f / particleCount) + rotation * (if (i % 2 == 0) 1f else -0.5f)
                val dist = scaledRadius * (1.5f + sin((particlePhase * 6.28f + i).toFloat()) * 0.3f)
                val rad = Math.toRadians(angle.toDouble())
                val px = centerX + cos(rad.toFloat()) * dist
                val py = centerY + sin(rad.toFloat()) * dist
                val pAlpha = (0.3f + 0.7f * pulseAlpha) * (1f - i.toFloat() / particleCount)

                drawCircle(
                    color = stateColor.copy(alpha = pAlpha * 0.6f),
                    radius = 3f + effectiveAmplitude * 4f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

private fun getAnimDuration(state: OrbState): Int = when (state) {
    OrbState.IDLE -> 3000
    OrbState.LISTENING -> 1500
    OrbState.THINKING -> 800
    OrbState.EXECUTING -> 1000
    OrbState.SPEAKING -> 600
    OrbState.SUCCESS -> 400
    OrbState.ERROR -> 200
}

private fun getRotationDuration(state: OrbState): Int = when (state) {
    OrbState.IDLE -> 20000
    OrbState.LISTENING -> 12000
    OrbState.THINKING -> 4000
    OrbState.EXECUTING -> 6000
    OrbState.SPEAKING -> 8000
    OrbState.SUCCESS -> 15000
    OrbState.ERROR -> 30000
}

private fun getStateColor(state: OrbState): Color = when (state) {
    OrbState.IDLE -> AccentBlue.copy(alpha = 0.7f)
    OrbState.LISTENING -> AccentCyan
    OrbState.THINKING -> AccentPurple
    OrbState.EXECUTING -> AccentGlow
    OrbState.SPEAKING -> AccentBlue
    OrbState.SUCCESS -> SuccessGreen
    OrbState.ERROR -> ErrorRed
}

private fun getParticleCount(state: OrbState): Int = when (state) {
    OrbState.IDLE -> 8
    OrbState.LISTENING -> 14
    OrbState.THINKING -> 20
    OrbState.EXECUTING -> 16
    OrbState.SPEAKING -> 12
    OrbState.SUCCESS -> 24
    OrbState.ERROR -> 6
}

private fun abs(v: Float) = if (v < 0f) -v else v
