package com.example.projekt.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.projekt.screens.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.URLDecoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLogin = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegister = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onCreateTrip = { navController.navigate("createTrip") },
                onTripClick = { trip ->
                    val tripId = trip.id.encodeURL()
                    navController.navigate("tripDetail/$tripId")
                },
                onMyTrips = { navController.navigate("myTrips") },
                onLogout = {
                    Firebase.auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("createTrip") {
            CreateTripScreen(onTripCreated = {
                navController.popBackStack()
            })
        }

        composable("myTrips") {
            MyTripsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            "tripDetail/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = URLDecoder.decode(
                backStackEntry.arguments?.getString("tripId") ?: "",
                "UTF-8"
            )
            TripDetailScreen(tripId = tripId, onBack = { navController.popBackStack() })
        }
    }
}

fun String.encodeURL(): String = java.net.URLEncoder.encode(this, "UTF-8")
