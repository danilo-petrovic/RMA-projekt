package com.example.projekt.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.projekt.screens.MyTripsScreen
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
                        popUpTo("login") { inclusive = true } // uklanja login iz backstack-a
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
                    val name = trip.name.encodeURL()
                    val desc = trip.description.encodeURL()
                    navController.navigate("tripDetail/$name/$desc")
                },
                onMyTrips = { navController.navigate("myTrips") },
                onLogout = {
                    Firebase.auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
            HomeScreen(
                onCreateTrip = { navController.navigate("createTrip") },
                onTripClick = { trip ->
                    val name = trip.name.encodeURL()
                    val desc = trip.description.encodeURL()
                    navController.navigate("tripDetail/$name/$desc")
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
            CreateTripScreen(
                onTripCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable("myTrips") {
            MyTripsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            "tripDetail/{name}/{desc}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("desc") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", "UTF-8")
            val desc = URLDecoder.decode(backStackEntry.arguments?.getString("desc") ?: "", "UTF-8")
            TripDetailScreen(name, desc, onBack = { navController.popBackStack() })
        }
    }
}

fun String.encodeURL(): String = java.net.URLEncoder.encode(this, "UTF-8")
