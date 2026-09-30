package com.samai.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.samai.assistant.navigation.Screen
import com.samai.assistant.ui.components.OrbState
import com.samai.assistant.ui.components.SAMOrb
import com.samai.assistant.ui.theme.*

data class QuickAction(val icon: ImageVector, val label: String, val route: String)

@Composable
fun HomeScreen(navController: NavController) {
    val quickActions = listOf(
        QuickAction(Icons.Filled.Mic, "Voice", Screen.CHAT),
        QuickAction(Icons.Filled.Visibility, "Lens", Screen.DISCOVER),
        QuickAction(Icons.Filled.PhoneAndroid, "Control", Screen.TASKS),
        QuickAction(Icons.Filled.Assignment, "Tasks", Screen.TASKS),
        QuickAction(Icons.Filled.Code, "Code", Screen.CODING),
        QuickAction(Icons.Filled.Storage, "Memory", Screen.MEMORY),
        QuickAction(Icons.Filled.School, "Knowledge", Screen.KNOWLEDGE),
        QuickAction(Icons.Filled.Search, "Search", Screen.DISCOVER),
        QuickAction(Icons.Filled.Public, "Wikipedia", Screen.KNOWLEDGE),
        QuickAction(Icons.Filled.Bolt, "Research", Screen.DISCOVER),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("S.A.M.", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineMedium.fontSize, color = MaterialTheme.colorScheme.primary)
                Text("Smart Autonomous Machine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Online", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                IconButton(onClick = { navController.navigate(Screen.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SAM Orb - central animated element
        Box(contentAlignment = Alignment.Center) {
            SAMOrb(
                config = com.samai.assistant.ui.components.OrbConfig(
                    size = 200.dp,
                    auraColor = AccentCyan,
                    enableParticles = true,
                    enableRings = true,
                    enableGlow = true
                ),
                state = OrbState.IDLE
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "How can I help you?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Voice activation button
        FilledTonalButton(
            onClick = { navController.navigate(Screen.CHAT) },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.height(52.dp)
        ) {
            Icon(Icons.Filled.Mic, null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Tap to Speak", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxWidth().height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickActions) { action ->
                HomeActionChip(action) { navController.navigate(action.route) }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HomeActionChip(action: QuickAction, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.size(64.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(action.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(action.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
