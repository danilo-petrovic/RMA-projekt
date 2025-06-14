package com.example.projekt.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(onBack: () -> Unit) {
    val trips = remember { mutableStateListOf<Trip>() }
    val uid = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    var tripToDelete by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("trips")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    trips.clear()
                    for (doc in snapshot) {
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
                                userId = uid
                            )
                        )
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My trips") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            if (trips.isEmpty()) {
                item {
                    Text("There are no trips.")
                }
            } else {
                items(trips) { trip ->
                    TripCard(trip = trip, uid = uid, db = db, formatter = formatter) {
                        tripToDelete = it
                    }
                }
            }
        }
    }

    // Confirm delete dialog
    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Confirm") },
            text = { Text("Do you want to delete \"${trip.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    Firebase.firestore.collection("trips").document(trip.id)
                        .delete()
                        .addOnSuccessListener {
                            trips.remove(trip)
                        }
                    tripToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun TripCard(
    trip: Trip,
    uid: String?,
    db: com.google.firebase.firestore.FirebaseFirestore,
    formatter: SimpleDateFormat,
    onDelete: (Trip) -> Unit
) {
    var name by remember { mutableStateOf(trip.name) }
    var desc by remember { mutableStateOf(trip.description) }
    var start by remember { mutableStateOf(trip.startDate) }
    var end by remember { mutableStateOf(trip.endDate) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    fun pickDate(current: Date?, onPicked: (Date) -> Unit) {
        calendar.time = current ?: Date()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                onPicked(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    db.collection("trips").document(trip.id)
                        .update("name", it)
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = desc,
                onValueChange = {
                    desc = it
                    db.collection("trips").document(trip.id)
                        .update("description", it)
                },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    pickDate(start) {
                        start = it
                        db.collection("trips").document(trip.id)
                            .update("startDate", it)
                    }
                }) {
                    Text(start?.let { formatter.format(it) } ?: "Beginning")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    pickDate(end) {
                        end = it
                        db.collection("trips").document(trip.id)
                            .update("endDate", it)
                    }
                }) {
                    Text(end?.let { formatter.format(it) } ?: "End")
                }
            }

            Spacer(Modifier.height(8.dp))

            val otherParticipants = trip.participants.filter { it != uid }

            if (otherParticipants.isEmpty()) {
                Text("Nobody joined the trip.")
            } else {
                Text("Users who have joined:", style = MaterialTheme.typography.titleSmall)
                otherParticipants.forEach { participantId ->
                    var username by remember(participantId) { mutableStateOf("Loading...") }

                    LaunchedEffect(participantId) {
                        db.collection("users").document(participantId).get()
                            .addOnSuccessListener { doc ->
                                username = doc.getString("username") ?: "Unknown"
                            }
                            .addOnFailureListener {
                                username = "Loading error"
                            }
                    }

                    Text("• @$username", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onDelete(trip) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete the trip")
            }
        }
    }
}
