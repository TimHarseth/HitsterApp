package com.example.hitsterapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun HitsterNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.HOME) {
        composable(Screen.HOME) {
            androidx.compose.material3.Text("Home — coming soon")
        }
        composable(
            route = Screen.PLAYER,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) {
            androidx.compose.material3.Text("Player — coming soon")
        }
    }
}
