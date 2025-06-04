package com.example.projekt.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val notifications = remember { mutableStateListOf<Pair<String, Date>>() }
    val uid = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("notifications")
                .whereEqualTo("toUserId", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val tempList = mutableListOf<Pair<String, Date>>()
                    for (doc in snapshot.documents) {
                        val message = doc.getString("message")
                        val timestamp = doc.getDate("timestamp")

                        if (!message.isNullOrBlank() && timestamp != null) {
                            tempList.add(Pair(message, timestamp))
                        }
                    }
                    notifications.clear()
                    notifications.addAll(tempList.sortedByDescending { it.second })
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("There are no notifications")
                }
            } else {
                notifications.forEach { (msg, time) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(msg, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(formatter.format(time), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
