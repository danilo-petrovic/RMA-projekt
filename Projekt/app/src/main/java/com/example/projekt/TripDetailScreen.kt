package com.example.projekt.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projekt.ui.SharedHeader
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.maps.CameraUpdateFactory

@Composable
fun TripDetailScreen(tripId: String, onBack: () -> Unit) {
    val tripState = remember { mutableStateOf<Trip?>(null) }
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

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

                tripState.value = Trip(
                    id = tripId,
                    name = name,
                    description = desc,
                    startDate = start,
                    endDate = end,
                    locationLat = lat,
                    locationLng = lng,
                    participants = participants
                )
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
                val position = LatLng(trip.locationLat, trip.locationLng)
                val cameraPositionState = rememberCameraPositionState()
                LaunchedEffect(position) {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(position, 10f)
                    )
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = position),
                        title = "Lokacija"
                    )
                }
            } else {
                Text("Lokacija nije dostupna.")
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Natrag")
            }
        }
    }
}
