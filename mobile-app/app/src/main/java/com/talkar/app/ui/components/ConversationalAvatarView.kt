package com.talkar.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Conversational avatar view that displays AI responses with typing animation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationalAvatarView(
    responseText: String,
    isTyping: Boolean,
    avatarName: String = "TalkAR Assistant",
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var displayText by remember { mutableStateOf("") }
    var currentIndex by remember { mutableStateOf(0) }
    
    // Typing animation effect
    LaunchedEffect(responseText, isTyping) {
        if (isTyping) {
            displayText = ""
            currentIndex = 0
        } else {
            // Simulate typing animation
            displayText = ""
            currentIndex = 0
            while (currentIndex < responseText.length) {
                displayText += responseText[currentIndex]
                currentIndex++
                delay(30) // Adjust typing speed
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Avatar header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawCircle(color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.VolumeUp,
                        contentDescription = "Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = avatarName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "TalkAR Assistant",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Typing indicator
                if (isTyping) {
                    TypingIndicator()
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Response text
            Text(
                text = if (isTyping) displayText else responseText,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Audio controls
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { /* TODO: Play audio */ }
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.VolumeUp,
                        contentDescription = "Play audio"
                    )
                }
            }
        }
    }
}

/**
 * Typing indicator with animated dots
 */
@Composable
fun TypingIndicator() {
    val coroutineScope = rememberCoroutineScope()
    var dotCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            dotCount = (dotCount + 1) % 4
            delay(500)
        }
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(8.dp)
            ) {
                drawCircle(
                    color = if (index < dotCount) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            if (index < 2) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}