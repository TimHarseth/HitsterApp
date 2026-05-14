package com.example.hitsterapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hitsterapp.ui.home.HomeScreen
import com.example.hitsterapp.ui.player.PlayerScreen

@Composable
fun HitsterNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.HOME) {
        composable(Screen.HOME) {
            HomeScreen(
                onPlaylistSelected = { playlistId ->
                    navController.navigate(Screen.player(playlistId))
                }
            )
        }
        composable(
            route = Screen.PLAYER,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) {
            PlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
