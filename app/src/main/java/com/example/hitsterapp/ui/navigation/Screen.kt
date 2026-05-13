package com.example.hitsterapp.ui.navigation

object Screen {
    const val HOME = "home"
    const val PLAYER = "player/{playlistId}"

    fun player(playlistId: String) = "player/$playlistId"
}
