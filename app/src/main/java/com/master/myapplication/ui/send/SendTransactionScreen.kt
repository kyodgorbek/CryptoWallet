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
    var recipientAddress by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

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
                    // Show inputs for Idle, Loading, and Error (with error message)
                    TransactionInputContent(
                        recipientAddress = recipientAddress,
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
    onRecipientChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }

        // Recipient Input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Recipient Address",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
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
                placeholder = { Text("0x...") },
                enabled = !isLoading
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
                placeholder = { Text("0.001") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !isLoading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
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
