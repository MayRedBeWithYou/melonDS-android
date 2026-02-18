package me.magnum.melonds.ui.emulator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun MultiplayerDialog(
    defaultNickname: String,
    onHost: (nickname: String, numPlayers: Int, port: Int) -> Unit,
    onJoin: (nickname: String, ip: String, port: Int) -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit,
    isConnecting: Boolean = false,
    initialIp: String = "",
) {
    var isInLobby by remember { mutableStateOf(MelonEmulator.getType() != 0) }
    var players by remember { mutableStateOf(emptyList<LanPlayer>()) }
    var maxPlayers by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val mpType = MelonEmulator.getType()
            isInLobby = mpType != 0
            if (isInLobby) {
                val (currentPlayers, currentMax) = withContext(Dispatchers.IO) {
                    MelonEmulator.processMultiplayerEvents()
                    MelonEmulator.getPlayerList().toList() to MelonEmulator.getMaxPlayers()
                }
                players = currentPlayers
                maxPlayers = currentMax
            }
            delay(100)
        }
    }

    Dialog(onDismissRequest = { if (!isConnecting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp
        ) {
            if (isInLobby) {
                LobbyContent(
                    players = players,
                    maxPlayers = maxPlayers,
                    onLeave = onLeave
                )
            } else {
                MultiplayerSetupContent(
                    defaultNickname = defaultNickname,
                    initialIp = initialIp,
                    isConnecting = isConnecting,
                    onHost = onHost,
                    onJoin = onJoin,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun MultiplayerSetupContent(
    defaultNickname: String,
    initialIp: String,
    isConnecting: Boolean,
    onHost: (String, Int, Int) -> Unit,
    onJoin: (String, String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.multiplayer_join), stringResource(R.string.multiplayer_host))

    var nickname by remember { mutableStateOf(defaultNickname) }
    var port by remember { mutableStateOf("7064") }
    var numPlayers by remember { mutableStateOf("2") }
    var serverIp by remember { mutableStateOf(initialIp) }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { if (!isConnecting) selectedTab = index },
                    text = { Text(title) },
                    enabled = !isConnecting
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text(stringResource(R.string.multiplayer_nickname)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnecting
            )

            if (selectedTab == 1) { // Host
                OutlinedTextField(
                    value = numPlayers,
                    onValueChange = { numPlayers = it },
                    label = { Text(stringResource(R.string.multiplayer_num_players)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnecting
                )

                val localIp = remember { getLocalIpAddress() }
                Text(
                    text = "IP: $localIp",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                )
            } else { // Join
                OutlinedTextField(
                    value = serverIp,
                    onValueChange = { serverIp = it },
                    label = { Text(stringResource(R.string.multiplayer_ip)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnecting
                )
            }

            val isPortValid = port.toIntOrNull()?.let { it in 1..65535 } ?: false
            val isNumPlayersValid = numPlayers.toIntOrNull()?.let { it in 2..16 } ?: false
            val isIpValid = if (selectedTab == 0) serverIp.isNotBlank() else true
            val isNicknameValid = nickname.isNotBlank()
            val isInputValid = isPortValid && isNicknameValid && isIpValid && (selectedTab == 0 || isNumPlayersValid)

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text(stringResource(R.string.multiplayer_port)) },
                modifier = Modifier.fillMaxWidth(),
                isError = !isPortValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isConnecting
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isConnecting
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    enabled = isInputValid && !isConnecting,
                    onClick = {
                        val p = port.toIntOrNull() ?: 7064
                        if (selectedTab == 1) {
                            val n = numPlayers.toIntOrNull() ?: 2
                            onHost(nickname, n, p)
                        } else {
                            onJoin(nickname, serverIp.trim(), p)
                        }
                    }
                ) {
                    Text(stringResource(if (selectedTab == 1) R.string.multiplayer_host else R.string.multiplayer_join))
                }
            }
        }
    }
}

@Composable
private fun LobbyContent(
    players: List<LanPlayer>,
    maxPlayers: Int,
    onLeave: () -> Unit,
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
