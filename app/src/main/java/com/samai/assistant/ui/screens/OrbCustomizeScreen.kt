package com.samai.assistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.samai.assistant.ui.components.*
import com.samai.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrbCustomizeScreen(navController: NavController) {
    var selectedType by remember { mutableStateOf(OrbType.CLASSIC) }
    var selectedAura by remember { mutableStateOf("cyan") }
    var selectedSize by remember { mutableStateOf(2f) }
    var particlesOn by remember { mutableStateOf(true) }
    var ringsOn by remember { mutableStateOf(true) }
    var glowOn by remember { mutableStateOf(true) }
    var visualizerOn by remember { mutableStateOf(true) }

    val auraColors = mapOf(
        "blue" to AccentBlue, "cyan" to AccentCyan, "purple" to AccentPurple,
        "green" to SuccessGreen, "red" to ErrorRed, "glow" to AccentGlow
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orb Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live preview
            val previewColor = auraColors[selectedAura] ?: AccentCyan
            SAMOrb(
                config = OrbConfig(
                    type = selectedType,
                    size = (100 + selectedSize * 40).dp,
                    auraColor = previewColor,
                    enableParticles = particlesOn,
                    enableRings = ringsOn,
                    enableGlow = glowOn,
                    enableVoiceVisualizer = visualizerOn
                ),
                state = OrbState.IDLE
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Orb Type
            Text("Orb Type", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbType.entries.forEach { type ->
                    FilterChip(selected = selectedType == type, onClick = { selectedType = type },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Aura
            Text("Aura Color", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                auraColors.forEach { (name, color) ->
                    val selected = selectedAura == name
                    Surface(
                        onClick = { selectedAura = name },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(44.dp),
                        color = color.copy(alpha = if (selected) 1f else 0.5f),
                        border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, Color.White) else null
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Size slider
            Text("Orb Size", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Slider(value = selectedSize, onValueChange = { selectedSize = it }, valueRange = 0f..4f, steps = 3)

            Spacer(modifier = Modifier.height(16.dp))

            // Effects toggles
            Text("Effects", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            EffectToggle("Particles", particlesOn) { particlesOn = it }
            EffectToggle("Rotating Rings", ringsOn) { ringsOn = it }
            EffectToggle("Glow", glowOn) { glowOn = it }
            EffectToggle("Voice Visualizer", visualizerOn) { visualizerOn = it }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Save Configuration")
            }
        }
    }
}

@Composable
private fun EffectToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
