package com.talkar.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talkar.app.ar.video.models.TalkingPhotoState
import com.talkar.app.ui.components.ArSceneViewComposable
import com.talkar.app.ui.components.LipSyncVideoOverlay
import com.talkar.app.ui.viewmodels.TalkingPhotoViewModel

// AR HUD colour tokens
private val ArCyan    = Color(0xFF00E5FF)
private val ArGreen   = Color(0xFF69F0AE)
private val ArAmber   = Color(0xFFFFD740)
private val ArRed     = Color(0xFFFF5252)
private val HudBg     = Color(0xBB000000)   // 73% black glass

@Composable
fun TalkingPhotoScreen(
    modifier: Modifier = Modifier,
    viewModel: TalkingPhotoViewModel = viewModel()
) {
    val detectedPosterName  by viewModel.detectedPosterName.collectAsState()
    val sessionRecreateTrigger by viewModel.sessionRecreateTrigger.collectAsState()
    val state               by viewModel.state.collectAsState()
    val error               by viewModel.error.collectAsState()
    val lastArtifactStatus  by viewModel.lastArtifactStatus.collectAsState()
    val loadedPosterCount   by viewModel.loadedPosterCount.collectAsState()
    val videoPath           by viewModel.videoPath.collectAsState()
    val lipCoordinates      by viewModel.lipCoordinates.collectAsState()
    val posterScreenBounds  by viewModel.posterScreenBounds.collectAsState()
    val lipScreenBounds     by viewModel.lipScreenBounds.collectAsState()
    val softResetTrigger    by viewModel.softResetTrigger.collectAsState()

    val isTracking       by viewModel.isTracking.collectAsState()
    val isScanning = state == TalkingPhotoState.TRACKING_READY
    val isPlaying  = state == TalkingPhotoState.PLAYING

    Box(modifier = modifier.fillMaxSize()) {

        // ── 1. Camera / AR layer ─────────────────────────────────────────
        ArSceneViewComposable(
            modifier = Modifier.fillMaxSize(),
            onPreviewReady          = { viewModel.onPreviewReady() },
            onPreviewModeChanged    = { mode -> viewModel.onPreviewModeChanged(mode) },
            onTrackingReady         = { viewModel.onTrackingReady() },
            onPosterSourceChanged   = { source -> viewModel.onPosterSourceChanged(source) },
            onPosterDatabaseReady   = { count, source -> viewModel.onPosterDatabaseReady(count, source) },
            onPosterDetected        = { id, name, anchor -> viewModel.onPosterDetected(id, name, anchor) },
            onPosterLost            = { id -> viewModel.onPosterLost(id) },
            onTrackingUpdate        = { data -> viewModel.onTrackingUpdate(data) },
            onError                 = { msg -> viewModel.onError(msg) },
            sessionRecreateTrigger  = sessionRecreateTrigger,
            softResetTrigger        = softResetTrigger
        )

        // ── 2. Lip-sync video overlay ────────────────────────────────────
        if (isPlaying && !videoPath.isNullOrBlank() && posterScreenBounds != null && isTracking) {
            LipSyncVideoOverlay(
                videoPath          = videoPath,
                posterScreenBounds = posterScreenBounds,
                lipCoordinates     = lipCoordinates,
                lipScreenBounds    = lipScreenBounds,
                onVideoComplete    = { viewModel.onVideoComplete() },
                modifier           = Modifier.fillMaxSize()
            )
        }

        // ── 3. Scanner reticle (pulsing corners while scanning) ──────────
        if (isScanning) {
            ScannerReticle(modifier = Modifier.fillMaxSize())
        }

        // ── 4 & 5. Top overlay: centered title + poster name, reset button top-right ─
        val haptic = LocalHapticFeedback.current
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            // Centered: app name + detected poster name stacked
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TalkAR",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                detectedPosterName?.let { name ->
                    Row(
                        modifier = Modifier
                            .background(HudBg, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ArGreen, CircleShape)
                        )
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Top-right: reset button only
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.fullResetAndReload()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .background(HudBg, CircleShape)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Scan again",
                    tint = ArRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── 6. Status pill (bottom centre) ───────────────────────────────
        val status = buildStatus(
            state              = state,
            loadedPosterCount  = loadedPosterCount,
            detectedPosterName = detectedPosterName,
            videoPath          = videoPath,
            lastArtifactStatus = lastArtifactStatus,
            errorMessage       = error?.message
        )
        StatusPill(
            text        = status.text,
            isLoading   = status.isLoading,
            accentColor = status.color,
            modifier    = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerReticle(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "scanner").animateFloat(
        initialValue = 0.45f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val color = ArCyan.copy(alpha = pulse)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val rectW = w * 0.74f
        val rectH = h * 0.44f
        val l = (w - rectW) / 2f
        val t = (h - rectH) / 2f
        val r = l + rectW
        val b = t + rectH
        val arm = 52.dp.toPx()
        val sw = 3.5.dp.toPx()

        fun seg(a: Offset, b2: Offset) = drawLine(color, a, b2, sw, StrokeCap.Round)

        // top-left
        seg(Offset(l, t + arm), Offset(l, t)); seg(Offset(l, t), Offset(l + arm, t))
        // top-right
        seg(Offset(r - arm, t), Offset(r, t)); seg(Offset(r, t), Offset(r, t + arm))
        // bottom-left
        seg(Offset(l, b - arm), Offset(l, b)); seg(Offset(l, b), Offset(l + arm, b))
        // bottom-right
        seg(Offset(r - arm, b), Offset(r, b)); seg(Offset(r, b), Offset(r, b - arm))
    }
}

@Composable
private fun StatusPill(
    text: String,
    isLoading: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(HudBg, RoundedCornerShape(50.dp))
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(13.dp),
                color       = accentColor,
                strokeWidth = 2.dp
            )
        } else {
            Box(Modifier.size(8.dp).background(accentColor, CircleShape))
        }
        Text(
            text       = text,
            color      = Color.White,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status helpers
// ─────────────────────────────────────────────────────────────────────────────

private data class StatusInfo(val text: String, val isLoading: Boolean, val color: Color)

private fun buildStatus(
    state: TalkingPhotoState,
    loadedPosterCount: Int,
    detectedPosterName: String?,
    videoPath: String?,
    lastArtifactStatus: String,
    errorMessage: String?
): StatusInfo = when {
    !errorMessage.isNullOrBlank() ->
        StatusInfo(friendlyError(errorMessage), false, ArRed)

    state == TalkingPhotoState.PLAYING ->
        StatusInfo("♪  Lip-sync playing", false, ArGreen)

    state == TalkingPhotoState.READY ->
        StatusInfo("Ready — starting playback", false, ArGreen)

    state == TalkingPhotoState.POSTER_DETECTED && detectedPosterName != null ->
        StatusInfo("Preparing…", true, ArCyan)

    lastArtifactStatus == "FETCHING" || state == TalkingPhotoState.FETCHING_ARTIFACT ->
        StatusInfo("Fetching lip-sync…", true, ArCyan)

    lastArtifactStatus.startsWith("WAITING_") || state == TalkingPhotoState.ARTIFACT_WAITING ->
        StatusInfo("Generating lip-sync (may take a minute)…", true, ArAmber)

    state == TalkingPhotoState.TRACKING_READY ->
        if (loadedPosterCount > 0)
            StatusInfo("Scanning · $loadedPosterCount poster${if (loadedPosterCount == 1) "" else "s"} loaded", false, ArCyan)
        else
            StatusInfo("Scanning for poster…", false, ArCyan)

    state == TalkingPhotoState.POSTERS_LOADING ->
        StatusInfo("Downloading poster data…", true, Color.White)

    else -> StatusInfo("Starting camera…", true, Color.White)
}

private fun friendlyError(msg: String) = when (msg) {
    "NO_DEFAULT_SCRIPT"  -> "No script for this poster"
    "NO_FACE_IN_POSTER"  -> "No face detected"
    "PROVIDER_FAILED"    -> "Lip-sync provider failed"
    "NETWORK_UNAVAILABLE"-> "Backend unreachable"
    "TRACKING_UNSTABLE"  -> "Hold camera steady"
    "ARTIFACT_NOT_READY" -> "Still generating…"
    else                 -> msg
}
