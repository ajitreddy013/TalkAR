package com.talkar.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talkar.app.data.models.AdContent
import com.talkar.app.data.services.AdContentFrontendIntegrationTest
import com.talkar.app.data.services.AdContentGenerationService
import kotlinx.coroutines.launch

/**
 * Test screen for ad content generation frontend integration
 * This screen allows manual testing of the complete workflow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdContentTestScreen(
    modifier: Modifier = Modifier
) {
    val tag = "AdContentTestScreen"
    val coroutineScope = rememberCoroutineScope()
    
    // State for UI elements
    var testStatus by remember { mutableStateOf("Ready to test") }
    var isTesting by remember { mutableStateOf(false) }
    var adContent by remember { mutableStateOf<AdContent?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Test service
    val testService = remember { AdContentFrontendIntegrationTest() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ad Content Integration Test") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Test Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Test Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testStatus,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        testStatus = "Running complete workflow test..."
                        isTesting = true
                        errorMessage = null
                        
                        // Run the complete workflow test
                        testService.testCompleteAdContentWorkflow()
                        
                        // Simulate test completion after delay
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(5000)
                            testStatus = "Test completed! Check logs for details."
                            isTesting = false
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run Full Test")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        testStatus = "Running multiple products test..."
                        isTesting = true
                        errorMessage = null
                        
                        // Run the multiple products test
                        testService.testMultipleProducts()
                        
                        // Simulate test completion after delay
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(8000)
                            testStatus = "Multiple products test completed! Check logs for details."
                            isTesting = false
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Multiple")
                }
            }
            
            // Manual Test Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Manual Test",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var productName by remember { mutableStateOf("Sunrich Water Bottle") }
                    
                    TextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            if (productName.isNotBlank()) {
                                testStatus = "Generating ad content for: $productName"
                                isTesting = true
                                errorMessage = null
                                adContent = null
                                
                                // Generate ad content for the specified product
                                coroutineScope.launch {
                                    try {
                                        val service = AdContentGenerationService.getInstance()
                                        val result = service.generateAdContent(productName)
                                        
                                        if (result.isSuccess) {
                                            val response = result.getOrNull()!!
                                            adContent = AdContent(
                                                script = response.script ?: "No script available",
                                                audioUrl = response.audio_url,
                                                videoUrl = response.video_url,
                                                productName = productName
                                            )
                                            testStatus = "Ad content generated successfully!"
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to generate ad content"
                                            testStatus = "Error: $errorMessage"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Unknown error"
                                        testStatus = "Exception: $errorMessage"
                                        Log.e(tag, "Error generating ad content", e)
                                    } finally {
                                        isTesting = false
                                    }
                                }
                            }
                        },
                        enabled = !isTesting && productName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate Ad Content")
                    }
                }
            }
            
            // Results Section
            if (adContent != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Generated Ad Content",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Product: ${adContent?.productName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Script: ${adContent?.script}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        adContent?.audioUrl?.let {
                            Text(
                                text = "Audio URL: $it",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        adContent?.videoUrl?.let {
                            Text(
                                text = "Video URL: $it",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Error Message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Test Instructions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Click 'Run Full Test' to test the complete workflow\n" +
                                "2. Click 'Test Multiple' to test with different products\n" +
                                "3. Enter a product name and click 'Generate Ad Content' for manual testing\n" +
                                "4. Check Android logs for detailed test output",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}