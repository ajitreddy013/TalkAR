package com.talkar.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkar.app.performance.PerformanceMonitor

/**
 * Performance Monitoring Overlay
 * Displays real-time FPS, memory, and performance metrics
 */
@Composable
fun PerformanceOverlay(
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpanded: () -> Unit = {}
) {
    val fps by performanceMonitor.fps.collectAsState()
    val memoryMB by performanceMonitor.memoryUsageMB.collectAsState()
    val performanceStatus by performanceMonitor.performanceStatus.collectAsState()
    
    // Update memory every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            performanceMonitor.updateMemoryUsage()
            kotlinx.coroutines.delay(2000)
        }
    }
    
    Card(
        modifier = modifier
            .clickable { onToggleExpanded() }
            .width(if (isExpanded) 280.dp else 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (performanceStatus) {
                PerformanceMonitor.PerformanceStatus.EXCELLENT -> Color(0xFF1B5E20).copy(alpha = 0.9f)
                PerformanceMonitor.PerformanceStatus.GOOD -> Color(0xFF2E7D32).copy(alpha = 0.9f)
                PerformanceMonitor.PerformanceStatus.FAIR -> Color(0xFFF57C00).copy(alpha = 0.9f)
                PerformanceMonitor.PerformanceStatus.POOR -> Color(0xFFC62828).copy(alpha = 0.9f)
            }
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            label = "expand_collapse"
        ) { expanded ->
            if (expanded) {
                ExpandedPerformanceView(performanceMonitor)
            } else {
                CompactPerformanceView(fps, memoryMB)
            }
        }
    }
}

@Composable
private fun CompactPerformanceView(
    fps: Float,
    memoryMB: Float
) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // FPS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "FPS",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${fps.toInt()} FPS",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Memory
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Memory",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${memoryMB.toInt()}MB",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ExpandedPerformanceView(
    performanceMonitor: PerformanceMonitor
) {
    val metrics = remember { performanceMonitor.getMetrics() }
    
    Column(
        modifier = Modifier.padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance",
                color = Color.White,
                fontSize = 14.sp,
                style = MaterialTheme.typography.titleSmall
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Collapse",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Divider(
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Metrics
        MetricRow("FPS", "${metrics.fps.toInt()}", target = "≥30", isGood = metrics.fps >= 30f)
        MetricRow("Memory", "${metrics.memoryMB.toInt()}MB", target = "<500MB", isGood = metrics.memoryMB < 500f)
        MetricRow("Frame Time", "${metrics.averageFrameTimeMs.toInt()}ms", target = "<33ms", isGood = metrics.averageFrameTimeMs < 33f)
        
        if (metrics.videoLoadTimeMs > 0) {
            MetricRow("Video Load", "${metrics.videoLoadTimeMs}ms", target = "<2s", isGood = metrics.videoLoadTimeMs < 2000L)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Device info
        Text(
            text = "${metrics.deviceInfo.manufacturer} ${metrics.deviceInfo.model}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    target: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium
            )
            Icon(
                imageVector = if (isGood) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = if (isGood) "Good" else "Warning",
                tint = if (isGood) Color(0xFF81C784) else Color(0xFFFFB74D),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
