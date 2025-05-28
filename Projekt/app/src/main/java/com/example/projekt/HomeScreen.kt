package com.example.projekt.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase

data class Trip(val name: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateTrip: () -> Unit,
    onTripClick: (Trip) -> Unit,
    onMyTrips: () -> Unit,
    onLogout: () -> Unit
) {
    val trips = remember { mutableStateListOf<Trip>() }
    val uid = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore

    DisposableEffect(uid) {
        val registration: ListenerRegistration = db.collection("trips")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    trips.clear()
                    for (doc in snapshot.documents) {
                        val tripOwnerId = doc.getString("userId")
                        if (tripOwnerId != null && tripOwnerId != uid) {
                            val name = doc.getString("name") ?: continue
                            val desc = doc.getString("description") ?: ""
                            trips.add(Trip(name, desc))
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
            TopAppBar(
                title = { Text("JoinMe") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opcije")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Moja putovanja") },
                            onClick = {
                                menuExpanded = false
                                onMyTrips()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Odjavi se") },
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
                Icon(Icons.Default.Add, contentDescription = "Dodaj putovanje")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (trips.isEmpty()) {
                Text("Nema dostupnih putovanja drugih korisnika.")
            } else {
                trips.forEach { trip ->
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
