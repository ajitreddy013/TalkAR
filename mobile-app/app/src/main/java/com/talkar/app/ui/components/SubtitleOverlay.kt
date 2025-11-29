package com.talkar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Subtitle/Closed Caption Overlay for displaying script text
 */
@Composable
fun SubtitleOverlay(
    script: String?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isVisible || script.isNullOrEmpty()) {
        return
    }
    
    // Auto-hide subtitle after 5 seconds of inactivity
    var showSubtitle by remember { mutableStateOf(true) }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(script) {
        showSubtitle = true
        lastUpdateTime = System.currentTimeMillis()
    }
    
    // Auto-hide logic
    LaunchedEffect(showSubtitle, lastUpdateTime) {
        if (showSubtitle) {
            kotlinx.coroutines.delay(5000) // 5 seconds
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime >= 5000) {
                showSubtitle = false
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(if (showSubtitle) 1f else 0f),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = script ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            )
        }
    }
}