package com.master.myapplication.ui.send

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.master.myapplication.data.model.TransactionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SendTransactionViewModel = hiltViewModel()
) {
    val transactionState by viewModel.transactionState.collectAsState()
    val userAddress by viewModel.userAddress.collectAsState()
    var recipientAddress by remember { mutableStateOf("0x742d35Cc6634C0532925a3b844Bc454e4438f44e") } // Default test address
    var amount by remember { mutableStateOf("0.001") } // Default test amount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (val state = transactionState) {
                is TransactionState.Success -> {
                    SuccessContent(
                        txHash = state.result.txHash,
                        onSendAnother = { 
                            recipientAddress = ""
                            amount = ""
                            viewModel.resetTransactionState() 
                        }
                    )
                }
                else -> {
                    TransactionInputContent(
                        recipientAddress = recipientAddress,
                        userAddress = userAddress,
                        onRecipientChange = { 
                            recipientAddress = it
                            viewModel.resetTransactionState()
                        },
                        amount = amount,
                        onAmountChange = { 
                            amount = it
                            viewModel.resetTransactionState()
                        },
                        onSend = { viewModel.sendTransaction(recipientAddress, amount) },
                        isLoading = state is TransactionState.Loading,
                        errorMessage = (state as? TransactionState.Error)?.message
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionInputContent(
    recipientAddress: String,
    userAddress: String,
    onRecipientChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        if (errorMessage != null) {
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Recipient Input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recipient Address",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                // Demo/Helpful shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (userAddress.isNotBlank()) {
                        AssistChip(
                            onClick = { onRecipientChange(userAddress) },
                            label = { Text("My Address", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFEFF6FF),
                                labelColor = Color(0xFF2563EB)
                            ),
                            border = null
                        )
                    }
                    AssistChip(
                        onClick = { onRecipientChange("0x742d35Cc6634C0532925a3b844Bc454e4438f44e") },
                        label = { Text("Demo", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFF3F4F6),
                            labelColor = Color(0xFF4B5563)
                        ),
                        border = null
                    )
                }
            }
            
            OutlinedTextField(
                value = recipientAddress,
                onValueChange = onRecipientChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (errorMessage?.contains("address", true) == true) Color.Red else Color(0xFFE5E7EB),
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedContainerColor = Color(0xFFF9FAFB),
                    focusedContainerColor = Color.White
                ),
                placeholder = { Text("0x...", color = Color.LightGray) },
                trailingIcon = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val data = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (data.isNotBlank()) {
                            onRecipientChange(data)
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color.Gray)
                    }
                },
                enabled = !isLoading,
                singleLine = true
            )
        }

        // Amount Input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
             Text(
                "Amount (ETH)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (errorMessage?.contains("amount", true) == true) Color.Red else Color(0xFFE5E7EB),
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedContainerColor = Color(0xFFF9FAFB),
                    focusedContainerColor = Color.White
                ),
                placeholder = { Text("0.001", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !isLoading,
                trailingIcon = {
                    Text("ETH", color = Color.Gray, modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                 CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Send Transaction", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SuccessContent(
    txHash: String,
    onSendAnother: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)), // Light green
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A), // Green
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Transaction Success!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF14532D) // Dark green
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color(0xFFBBF7D0))
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Transaction Hash:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF14532D)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${txHash.take(10)}...${txHash.takeLast(6)}",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFF374151)
                )
                IconButton(
                    onClick = {
                          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                          val clip = ClipData.newPlainText("Transaction Hash", txHash)
                          clipboard.setPrimaryClip(clip)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy, 
                        contentDescription = "Copy",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                 Text(
                    "View on Etherscan",
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium // Underline usually done with text decoration but skipping for simplicity
                )
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }
    }
}
