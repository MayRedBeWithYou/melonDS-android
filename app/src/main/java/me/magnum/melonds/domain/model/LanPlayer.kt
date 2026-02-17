package me.magnum.melonds.domain.model

data class LanPlayer(
    val id: Int,
    val name: String,
    val status: Int,
    val isLocalPlayer: Boolean,
)
