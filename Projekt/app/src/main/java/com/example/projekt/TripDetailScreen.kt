package com.example.projekt.screens

import android.app.AlertDialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.projekt.notifications.showNotification
import com.example.projekt.ui.SharedHeader
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

@Composable
fun TripDetailScreen(tripId: String, onBack: () -> Unit) {
    val tripState = remember { mutableStateOf<Trip?>(null) }
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val uid = currentUser?.uid
    var showDialog by remember { mutableStateOf(false) }
    var creatorName by remember { mutableStateOf<String?>(null) }

    // Sensor shake detection
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var lastShakeTime = 0L
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z)
                    if (magnitude > 15) {
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTime > 1000) {
                            lastShakeTime = now
                            showDialog = true
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(tripId) {
        Firebase.firestore.collection("trips").document(tripId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: ""
                val desc = doc.getString("description") ?: ""
                val start = doc.getDate("startDate")
                val end = doc.getDate("endDate")
                val lat = doc.getDouble("locationLat")
                val lng = doc.getDouble("locationLng")
                val participants = doc.get("participants") as? List<String> ?: emptyList()
                val userId = doc.getString("userId") ?: ""

                tripState.value = Trip(
                    id = tripId,
                    name = name,
                    description = desc,
                    startDate = start,
                    endDate = end,
                    locationLat = lat,
                    locationLng = lng,
                    participants = participants,
                    userId = userId
                )

                if (userId.isNotEmpty()) {
                    Firebase.firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            creatorName = userDoc.getString("username") ?: "Nepoznat korisnik"
                        }
                }
            }
    }

    val trip = tripState.value

    Column(modifier = Modifier.padding(24.dp)) {
        SharedHeader()

        if (trip == null) {
            Text("Učitavanje...")
        } else {
            Text(trip.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(trip.description, style = MaterialTheme.typography.bodyLarge)

            creatorName?.let {
                Spacer(Modifier.height(8.dp))
                Text("Kreirao: @$it", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))
            trip.startDate?.let {
                Text("Početak: ${formatter.format(it)}")
            }
            trip.endDate?.let {
                Text("Kraj: ${formatter.format(it)}")
            }

            Spacer(Modifier.height(16.dp))
            Text("Lokacija:")

            if (trip.locationLat != null && trip.locationLng != null) {
                val latLng = LatLng(trip.locationLat, trip.locationLng)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(latLng, 10f)
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = latLng),
                        title = "Lokacija"
                    )
                }
            }
            else {
                Text("Lokacija nije dostupna.")
            }

            Spacer(Modifier.height(16.dp))

            if (uid != null && trip.participants.contains(uid) && uid != trip.userId) {
                Button(onClick = {
                    val updatedParticipants = trip.participants.toMutableList()
                    updatedParticipants.remove(uid)

                    Firebase.firestore.collection("trips").document(trip.id)
                        .update("participants", updatedParticipants)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Odjavljeni ste s putovanja.", Toast.LENGTH_SHORT).show()
                            tripState.value = trip.copy(participants = updatedParticipants)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }) {
                    Text("Odustani od putovanja")
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Natrag")
            }
        }
    }

    // Dialog za potreseni uređaj
    if (showDialog && trip != null && uid != null && !trip.participants.contains(uid)) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Pridruži se putovanju?") },
            text = { Text("Želite li se pridružiti putovanju ${trip.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    val updatedParticipants = trip.participants.toMutableList().apply { add(uid) }
                    Firebase.firestore.collection("trips").document(trip.id)
                        .update("participants", updatedParticipants)
                        .addOnSuccessListener {
                            Firebase.firestore.collection("notifications").add(
                                mapOf(
                                    "toUserId" to trip.userId,
                                    "message" to "${currentUser.displayName ?: "Nepoznat korisnik"} se prijavio na putovanje ${trip.name}",
                                    "timestamp" to Date()
                                )
                            )

                            showNotification(
                                context,
                                "${currentUser.displayName ?: "Nepoznat korisnik"} se pridružio na putovanje ${trip.name}"
                            )
                            Toast.makeText(context, "Pridruženi ste!", Toast.LENGTH_SHORT).show()
                            tripState.value = trip.copy(participants = updatedParticipants)
                        }
                }) {
                    Text("Da")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Ne")
                }
            }
        )
    }
}