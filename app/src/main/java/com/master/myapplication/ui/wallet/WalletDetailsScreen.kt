package com.master.myapplication.ui.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.master.myapplication.data.model.WalletInfo
import com.master.myapplication.data.model.WalletState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onSendTransaction: () -> Unit,
    onLogout: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val walletState by viewModel.walletState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB) // Light gray background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadWalletInfo(isRefreshing = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            when (val state = walletState) {
                is WalletState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF3B82F6)
                    )
                }
                is WalletState.Success -> {
                    WalletContent(
                        walletInfo = state.walletInfo,
                        onCopyAddress = {
                           // Use helper in ViewModel or Composable
                        },
                        onSendTransaction = onSendTransaction,
                        onLogoutRequest = { showLogoutDialog = true },
                        onSwitchNetwork = { chainId -> viewModel.switchNetwork(chainId) }
                    )
                }
                is WalletState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Button(
                        onClick = { viewModel.loadWalletInfo() },
                        modifier = Modifier.align(Alignment.Center).padding(top = 48.dp)
                    ) { Text("Retry") }
                }
            }
        }
    }
}

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WalletContent(
    walletInfo: WalletInfo,
    onCopyAddress: () -> Unit,
    onSendTransaction: () -> Unit,
    onLogoutRequest: () -> Unit,
    onSwitchNetwork: (Long) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Wallet Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // EVM Badge
                Surface(
                    color = Color(0xFFDBEAFE),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "EVM",
                        color = Color(0xFF1E40AF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = walletInfo.address,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )

                Divider(
                    color = Color(0xFFE5E7EB),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Network",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${walletInfo.network} - ${walletInfo.chainId}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Network Switcher Button
                    TextButton(
                        onClick = {
                            val targetChainId = if (walletInfo.chainId == 11155111L) 1L else 11155111L
                            onSwitchNetwork(targetChainId)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF3B82F6))
                    ) {
                        Text("Switch")
                    }
                }

                Divider(
                    color = Color(0xFFE5E7EB),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = walletInfo.balance,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ETH",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        // Actions
        ActionItem(
            text = "Copy Address",
            icon = Icons.Default.ContentCopy,
            onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Wallet Address", walletInfo.address)
                  clipboard.setPrimaryClip(clip)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSendTransaction,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Transaction", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        ActionItem(
            text = "Logout",
            icon = Icons.Default.ExitToApp,
            textColor = Color(0xFFDC2626),
            iconColor = Color(0xFFDC2626),
            onClick = onLogoutRequest
        )
    }
}

@Composable
fun ActionItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    textColor: Color = Color.Black,
    iconColor: Color = Color.Gray
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
