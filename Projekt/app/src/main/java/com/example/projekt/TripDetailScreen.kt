package com.example.projekt.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projekt.ui.SharedHeader

@Composable
fun TripDetailScreen(name: String, desc: String, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        SharedHeader()
        Text(name, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(desc, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Natrag")
        }
    }
}
