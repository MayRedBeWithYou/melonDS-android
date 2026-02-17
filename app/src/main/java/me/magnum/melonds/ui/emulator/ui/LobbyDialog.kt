package me.magnum.melonds.ui.emulator.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.magnum.melonds.MelonEmulator
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.LanPlayer
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun LobbyDialog(
    onLeave: () -> Unit,
    onDismiss: () -> Unit = {},
) {
    var players by remember { mutableStateOf(emptyList<LanPlayer>()) }
    var maxPlayers by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        var previousPlayers = emptyList<LanPlayer>()
        while (true) {
            val (currentPlayers, currentMax) = withContext(Dispatchers.IO) {
                MelonEmulator.processMultiplayerEvents()
                MelonEmulator.getPlayerList().toList() to MelonEmulator.getMaxPlayers()
            }
            maxPlayers = currentMax

            players = currentPlayers
            maxPlayers = currentMax
            previousPlayers = currentPlayers
            delay(100)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.multiplayer_lobby),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.multiplayer_players_count, players.size, maxPlayers),
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )

                val localIp = remember { getLocalIpAddress() }
                Text(
                    text = "IP: $localIp",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Divider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 300.dp)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(players) { player ->
                        PlayerRow(player)
                    }
                }

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLeave,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.multiplayer_leave),
                        color = MaterialTheme.colors.onError
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: LanPlayer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayName = if (player.isLocalPlayer) {
            "${player.name} (you)"
        } else {
            player.name
        }
        Text(
            text = displayName,
            fontWeight = if (player.isLocalPlayer) FontWeight.Bold else FontWeight.Normal
        )

        val statusText = when (player.status) {
            2 -> stringResource(R.string.multiplayer_status_host)
            1 -> stringResource(R.string.multiplayer_status_connected)
            3 -> stringResource(R.string.multiplayer_status_connecting)
            else -> stringResource(R.string.multiplayer_status_disconnected)
        }
        val statusColor = when (player.status) {
            2 -> MaterialTheme.colors.primary
            1 -> MaterialTheme.colors.secondary
            3 -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            else -> MaterialTheme.colors.error
        }
        Text(
            text = statusText,
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getLocalIpAddress(): String {
    try {
        for (intf in NetworkInterface.getNetworkInterfaces()) {
            for (addr in intf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "Unknown"
                }
            }
        }
    } catch (_: Exception) {}
    return "Unknown"
}
