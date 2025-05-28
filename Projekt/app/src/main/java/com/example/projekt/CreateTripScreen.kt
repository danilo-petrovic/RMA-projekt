package com.example.projekt.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projekt.ui.SharedHeader
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun CreateTripScreen(onTripCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(24.dp)) {
        SharedHeader()

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Naziv putovanja") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Opis") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

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
                        "userId" to uid
                    )
                    Firebase.firestore.collection("trips")
                        .add(tripData)
                        .addOnSuccessListener {
                            onTripCreated()  // više ne šaljemo Trip objekt
                        }
                        .addOnFailureListener {
                            error = "Greška pri spremanju: ${it.message}"
                        }
                } else {
                    error = "Niste prijavljeni"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kreiraj")
        }
    }
}
