package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talkar.app.ui.viewmodels.TalkingPhotoViewModel

/**
 * Main AR screen for TalkAR.
 * 
 * Now uses the new TalkingPhotoScreen with lip-sync functionality.
 * 
 * Features:
 * - Poster detection with human face filter
 * - Lip-sync video generation with Wav2Lip
 * - Alpha blending for seamless lip overlay
 * - Single poster mode with refresh scan
 * - Tracking loss/recovery handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkARScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: TalkingPhotoViewModel = viewModel {
        TalkingPhotoViewModel(context)
    }
    
    // Use the new TalkingPhotoScreen instead of deprecated TalkARView
    TalkingPhotoScreen(
        modifier = modifier,
        viewModel = viewModel
    )
}
