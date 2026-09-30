package com.samai.assistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.samai.assistant.navigation.Screen
import com.samai.assistant.ui.components.OrbConfig
import com.samai.assistant.ui.components.OrbState
import com.samai.assistant.ui.components.OrbType
import com.samai.assistant.ui.components.SAMOrb
import com.samai.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAMCoreScreen(navController: NavController) {
    var currentOrbType by remember { mutableStateOf(OrbType.CLASSIC) }
    var currentState by remember { mutableStateOf(OrbState.IDLE) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SAM Core", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large interactive orb
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                SAMOrb(
                    config = OrbConfig(
                        type = currentOrbType,
                        size = 240.dp,
                        auraColor = AccentCyan,
                        enableParticles = true,
                        enableRings = true,
                        enableGlow = true,
                        enableVoiceVisualizer = currentState == OrbState.LISTENING
                    ),
                    state = currentState
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "S.A.M. Core Status: ${currentState.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Smart Autonomous Machine",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // State demo buttons
            Text("Preview States", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OrbState.entries.forEach { state ->
                    FilterChip(
                        selected = currentState == state,
                        onClick = { currentState = state },
                        label = { Text(state.name.take(4), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Orb Types", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OrbType.entries.forEach { type ->
                    FilterChip(
                        selected = currentOrbType == type,
                        onClick = { currentOrbType = type },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(Screen.ORB_CUSTOMIZE) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Palette, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customize Orb")
            }
        }
    }
}

@Composable
fun DiscoverScreen(navController: NavController) {
    val discoverItems = listOf(
        Triple(Icons.Filled.Search, "Google Search", "Search the web with voice"),
        Triple(Icons.Filled.School, "Wikipedia", "Knowledge base explorer"),
        Triple(Icons.Filled.Bolt, "Deep Research", "Multi-source research"),
        Triple(Icons.Filled.Image, "Image Search", "Visual discovery"),
        Triple(Icons.Filled.Link, "Connectors", "External integrations"),
        Triple(Icons.Filled.Code, "Coding Studio", "Build with SAM"),
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Discover", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Explore SAM capabilities", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))

        discoverItems.forEach { (icon, title, subtitle) ->
            Card(
                onClick = { navController.navigate(Screen.KNOWLEDGE) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
