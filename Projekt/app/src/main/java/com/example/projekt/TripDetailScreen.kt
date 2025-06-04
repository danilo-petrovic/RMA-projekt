package com.example.projekt.screens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(tripId: String, onBack: () -> Unit) {
    val tripState = remember { mutableStateOf<Trip?>(null) }
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val uid = currentUser?.uid
    var showDialog by remember { mutableStateOf(false) }
    var creatorName by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var lastShakeTime = 0L
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val magnitude = sqrt(it.values[0] * it.values[0] +
                            it.values[1] * it.values[1] +
                            it.values[2] * it.values[2])
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
                            creatorName = userDoc.getString("username") ?: "Unknown"
                        }
                }
            }
    }

    val trip = tripState.value

    Column(modifier = Modifier.padding(24.dp)) {
        SharedHeader()

        if (trip == null) {
            Text("Loading...")
        } else {
            Text(trip.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(trip.description, style = MaterialTheme.typography.bodyLarge)

            creatorName?.let {
                Spacer(Modifier.height(8.dp))
                Text("Created by: @$it", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))
            trip.startDate?.let { Text("Beginning: ${formatter.format(it)}") }
            trip.endDate?.let { Text("End: ${formatter.format(it)}") }

            Spacer(Modifier.height(16.dp))
            Text("Location:")

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
                        title = "Location"
                    )
                }
            } else {
                Text("Location unavailable")
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }

    if (showDialog && trip != null && uid != null && !trip.participants.contains(uid)) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Join?") },
            text = { Text("Do you want to join ${trip.name}?") },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        showDialog = false
                        val updatedParticipants = trip.participants.toMutableList().apply { add(uid) }

                        Firebase.firestore.collection("trips").document(trip.id)
                            .update("participants", updatedParticipants)
                            .addOnSuccessListener {
                                Firebase.firestore.collection("users").document(uid).get()
                                    .addOnSuccessListener { userDoc ->
                                        val username = userDoc.getString("username")
                                            ?: currentUser?.displayName
                                            ?: "Unknown"

                                        Firebase.firestore.collection("notifications").add(
                                            mapOf(
                                                "toUserId" to trip.userId,
                                                "message" to "$username has just joined the trip named ${trip.name}",
                                                "timestamp" to Date()
                                            )
                                        )

                                        showNotification(
                                            context,
                                            "$username has just joined the trip named ${trip.name}"
                                        )

                                        Toast.makeText(context, "You have joined!", Toast.LENGTH_SHORT).show()
                                        tripState.value = trip.copy(participants = updatedParticipants)
                                    }
                            }
                    }) {
                        Text("Yes")
                    }

                    Spacer(Modifier.width(8.dp))

                    TextButton(onClick = { showDialog = false }) {
                        Text("No")
                    }
                }
            },
            dismissButton = {}
        )
    }
}
