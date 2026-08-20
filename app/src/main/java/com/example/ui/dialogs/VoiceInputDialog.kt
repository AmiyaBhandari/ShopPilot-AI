package com.example.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var speechText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val sampleVoiceCommands = listOf(
        "Aaj 4 Maggi aur 2 Coke cash me beche",
        "Ramesh ko 2 Aashirvaad Atta udhar diya",
        "Balaji se 50 Parle-G aayi 24 rs me",
        "Ramesh ne 500 rupaye jama kiye",
        "Maggi ka stock kitna bacha hai?",
        "Kaunsa product sabse zyada bik raha hai?",
        "Dead stock kaunsa hai dukan me?"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Voice & Natural Language Assistant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Speak or type in Hindi, Hinglish, or English. ShopPilot AI interprets sales, inward purchases, khata entries, or questions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            // Animated Mic Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(BrandSecondary)
                    .clickable {
                        isListening = !isListening
                        if (isListening && speechText.isBlank()) {
                            speechText = "Aaj 4 Maggi aur 2 Coke cash me beche"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = if (isListening) "Listening... Tap to stop" else "Tap microphone to speak or type below",
                style = MaterialTheme.typography.labelMedium,
                color = BrandSecondary,
                fontWeight = FontWeight.SemiBold
            )

            // Input field
            OutlinedTextField(
                value = speechText,
                onValueChange = { speechText = it },
                label = { Text("What happened in your shop?") },
                placeholder = { Text("e.g. Sold 3 Parle-G for ₹30 cash") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_text_input")
            )

            // Suggested phrases
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Quick Sample Commands (Tap to try):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleVoiceCommands) { phrase ->
                        SuggestionChip(
                            onClick = { speechText = phrase },
                            label = { Text(phrase, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (speechText.isNotBlank()) {
                        viewModel.processVoiceInput(speechText)
                        onDismiss()
                    }
                },
                enabled = speechText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_voice_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Process with ShopPilot AI", fontWeight = FontWeight.Bold)
            }
        }
    }
}
