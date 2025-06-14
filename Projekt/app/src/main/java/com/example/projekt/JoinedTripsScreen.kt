package com.example.projekt.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinedTripsScreen(onBack: () -> Unit, onTripClick: (Trip) -> Unit) {
    val trips = remember { mutableStateListOf<Trip>() }
    val uid = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("trips")
                .whereArrayContains("participants", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    trips.clear()
                    for (doc in snapshot) {
                        val tripOwnerId = doc.getString("userId") ?: continue
                        if (tripOwnerId == uid) continue

                        val name = doc.getString("name") ?: continue
                        val desc = doc.getString("description") ?: ""
                        val start = doc.getDate("startDate")
                        val end = doc.getDate("endDate")
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        val lat = doc.getDouble("locationLat")
                        val lng = doc.getDouble("locationLng")

                        trips.add(
                            Trip(
                                id = doc.id,
                                name = name,
                                description = desc,
                                startDate = start,
                                endDate = end,
                                participants = participants,
                                locationLat = lat,
                                locationLng = lng,
                                userId = tripOwnerId
                            )
                        )
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Joined trips") },
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
            if (trips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("There are no joined trips")
                }
            } else {
                LazyColumn {
                    items(trips) { trip ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { onTripClick(trip) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(trip.name, style = MaterialTheme.typography.titleMedium)
                                Text(trip.description, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val updated = trip.participants.filter { it != uid }
                                        db.collection("trips").document(trip.id)
                                            .update("participants", updated)
                                            .addOnSuccessListener {
                                                trips.remove(trip)
                                            }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Leave")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
