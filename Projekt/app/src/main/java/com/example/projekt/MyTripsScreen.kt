package com.example.projekt.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
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
                                userId = uid // Dodaj userId u model
                            )
                        )
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moja putovanja") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (trips.isEmpty()) {
                Text("Nema vaših putovanja.")
            } else {
                trips.forEach { trip ->
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
                                label = { Text("Naziv") },
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
                                label = { Text("Opis") },
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
                                    Text(start?.let { formatter.format(it) } ?: "Početak")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = {
                                    pickDate(end) {
                                        end = it
                                        db.collection("trips").document(trip.id)
                                            .update("endDate", it)
                                    }
                                }) {
                                    Text(end?.let { formatter.format(it) } ?: "Kraj")
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            val otherParticipants = trip.participants.filter { it != uid }

                            if (otherParticipants.isEmpty()) {
                                Text("Niko se još nije prijavio na putovanje.")
                            } else {
                                Text("Pridruženi korisnici:", style = MaterialTheme.typography.titleSmall)
                                otherParticipants.forEach { participantId ->
                                    var username by remember(participantId) { mutableStateOf("Učitavanje...") }

                                    LaunchedEffect(participantId) {
                                        db.collection("users").document(participantId).get()
                                            .addOnSuccessListener { doc ->
                                                username = doc.getString("username") ?: "Nepoznat korisnik"
                                            }
                                            .addOnFailureListener {
                                                username = "Greška pri učitavanju"
                                            }
                                    }

                                    Text("• @$username", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = { tripToDelete = trip },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Obriši putovanje")
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm dialog
    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Potvrda brisanja") },
            text = { Text("Da li ste sigurni da želite obrisati putovanje \"${trip.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    Firebase.firestore.collection("trips").document(trip.id)
                        .delete()
                        .addOnSuccessListener {
                            trips.remove(trip)
                        }
                    tripToDelete = null
                }) {
                    Text("Obriši")
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Otkaži")
                }
            }
        )
    }
}
