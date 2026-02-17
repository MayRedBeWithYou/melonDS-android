package me.magnum.melonds.domain.model.emulator

sealed class EmulatorEvent {
    data class RumbleStart(val duration: Int) : EmulatorEvent()
    data object RumbleStop : EmulatorEvent()
    data object Stop : EmulatorEvent()
    data class PlayerJoined(val name: String) : EmulatorEvent()
    data class PlayerLeft(val name: String) : EmulatorEvent()
    data object ConnectionLost : EmulatorEvent()
}