package com.example.projekt.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.projekt.ui.SharedHeader
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import java.util.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition

@Composable
fun CreateTripScreen(onTripCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }

    fun showDatePicker(onDateSelected: (Date) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(modifier = Modifier.padding(24.dp)) {
        SharedHeader()

        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Column {
            Button(onClick = { showDatePicker { startDate = it } }) {
                Text(startDate?.toString() ?: "Beginning")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { showDatePicker { endDate = it } }) {
                Text(endDate?.toString() ?: "End")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Start Location")

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(45.2671, 19.8335), 6f)
        }

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng -> selectedLatLng = latLng }
        ) {
            selectedLatLng?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Chosen location"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {
                val uid = Firebase.auth.currentUser?.uid
                if (uid != null) {
                    val tripData = hashMapOf(
                        "name" to name,
                        "description" to desc,
                        "startDate" to startDate,
                        "endDate" to endDate,
                        "participants" to listOf(uid),
                        "userId" to uid,
                        "locationLat" to selectedLatLng?.latitude,
                        "locationLng" to selectedLatLng?.longitude
                    )
                    Firebase.firestore.collection("trips")
                        .add(tripData)
                        .addOnSuccessListener { onTripCreated() }
                        .addOnFailureListener { error = "Error: ${it.message}" }
                } else {
                    error = "You are not logged in"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create")
        }
    }
}
