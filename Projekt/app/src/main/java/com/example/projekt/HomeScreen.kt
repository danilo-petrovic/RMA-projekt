package com.example.projekt.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import java.util.*

data class Trip(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val participants: List<String> = emptyList(),
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val userId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateTrip: () -> Unit,
    onTripClick: (Trip) -> Unit,
    onMyTrips: () -> Unit,
    onJoinedTrips: () -> Unit,
    onLogout: () -> Unit,
    onNotifications: () -> Unit
) {
    val trips = remember { mutableStateListOf<Trip>() }
    var searchQuery by remember { mutableStateOf("") }
    val uid = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore

    LaunchedEffect(uid) {
        // Notification listener can stay if you still use it elsewhere
        if (uid != null) {
            db.collection("notifications")
                .whereEqualTo("toUserId", uid)
                .addSnapshotListener { _, _ -> }
        }
    }

    DisposableEffect(uid) {
        val registration: ListenerRegistration = db.collection("trips")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    trips.clear()
                    for (doc in snapshot.documents) {
                        val tripOwnerId = doc.getString("userId")
                        val participants = doc.get("participants") as? List<String> ?: emptyList()

                        if (tripOwnerId != null && tripOwnerId != uid && !participants.contains(uid)) {
                            val name = doc.getString("name") ?: continue
                            val desc = doc.getString("description") ?: ""
                            val start = doc.getDate("startDate")
                            val end = doc.getDate("endDate")
                            val lat = doc.getDouble("locationLat")
                            val lng = doc.getDouble("locationLng")

                            if (start == null || start.after(Date())) {
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
            }

        onDispose {
            registration.remove()
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("JoinMe") },
                actions = {
                    IconButton(onClick = onNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }

                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        offset = DpOffset(x = 0.dp, y = 0.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("My trips") },
                            onClick = {
                                menuExpanded = false
                                onMyTrips()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Joined trips") },
                            onClick = {
                                menuExpanded = false
                                onJoinedTrips()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log out") },
                            onClick = {
                                menuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTrip) {
                Icon(Icons.Default.Add, contentDescription = "Add trip")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            val filteredTrips = trips.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }

            if (filteredTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trips")
                }
            } else {
                filteredTrips.forEach { trip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onTripClick(trip) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(trip.name, style = MaterialTheme.typography.titleMedium)
                            Text(trip.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
