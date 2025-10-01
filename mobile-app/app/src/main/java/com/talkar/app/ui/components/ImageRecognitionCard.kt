package com.talkar.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talkar.app.data.models.ImageRecognition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageRecognitionCard(
    image: ImageRecognition,
    onGenerateVideo: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDialogue by remember { mutableStateOf(image.dialogues.firstOrNull()) }
    var selectedLanguage by remember { mutableStateOf("en") }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = image.name,
                style = MaterialTheme.typography.headlineSmall
            )
            
            image.description?.let { description ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Language selection
            Text(
                text = "Select Language:",
                style = MaterialTheme.typography.labelMedium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(image.dialogues) { dialogue ->
                    FilterChip(
                        onClick = { 
                            selectedDialogue = dialogue
                            selectedLanguage = dialogue.language
                        },
                        label = { Text(dialogue.language.uppercase()) },
                        selected = selectedDialogue?.id == dialogue.id
                    )
                }
            }
            
            // Dialogue text
            selectedDialogue?.let { dialogue ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dialogue.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Generate video button
            Button(
                onClick = {
                    selectedDialogue?.let { dialogue ->
                        onGenerateVideo(
                            dialogue.text,
                            dialogue.language,
                            dialogue.voiceId
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedDialogue != null
            ) {
                Text("Generate Talking Head")
            }
        }
    }
}

