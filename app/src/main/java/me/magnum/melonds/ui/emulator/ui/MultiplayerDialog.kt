package me.magnum.melonds.ui.emulator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.magnum.melonds.R
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun MultiplayerDialog(
    defaultNickname: String,
    onHost: (nickname: String, numPlayers: Int, port: Int) -> Unit,
    onJoin: (nickname: String, ip: String, port: Int) -> Unit,
    onDismiss: () -> Unit,
    isConnecting: Boolean = false,
    initialIp: String = "",
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.multiplayer_join), stringResource(R.string.multiplayer_host))

    var nickname by remember { mutableStateOf(defaultNickname) }
    var port by remember { mutableStateOf("7064") }
    var numPlayers by remember { mutableStateOf("2") }
    var serverIp by remember { mutableStateOf(initialIp) }

    Dialog(onDismissRequest = { if (!isConnecting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp
        ) {
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
