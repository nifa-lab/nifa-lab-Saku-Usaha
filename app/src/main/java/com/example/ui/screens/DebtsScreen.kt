package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: SakuUsahaViewModel,
    debts: List<TransactionEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Utang ke Supplier, 1: Piutang Pelanggan
    var transactionToPay by remember { mutableStateOf<TransactionEntity?>(null) }

    // Split into Supplier Debts (Harus Dibayar) and Customer Receivables (Akan Diterima)
    val supplierDebts = debts.filter { it.type == "BELI" || it.contactType == "SUPPLIER" }
    val customerDebts = debts.filter { it.type == "JUAL" || it.contactType == "PELANGGAN" }

    val totalSupplierOwed = supplierDebts.filter { it.status != "LUNAS" }.sumOf { it.remainingDebt }
    val totalCustomerReceivable = customerDebts.filter { it.status != "LUNAS" }.sumOf { it.remainingDebt }

    val displayedList = if (selectedTab == 0) supplierDebts else customerDebts

    if (transactionToPay != null) {
        DebtPaymentModal(
            transaction = transactionToPay!!,
            isReceiving = transactionToPay!!.type == "JUAL",
            onPay = { amount, method, notes ->
                viewModel.payDebt(
                    transactionId = transactionToPay!!.id,
                    paymentAmount = amount,
                    paymentMethod = method,
                    notes = notes
                )
                transactionToPay = null
            },
            onDismiss = { transactionToPay = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utang & Piutang", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openRecordModal(if (selectedTab == 0) "BELI" else "JUAL") },
                        modifier = Modifier.testTag("btn_add_debt")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                    AiAssistantTopActionButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Summary Cards Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Harus Dibayar (Utang Supplier)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Harus Dibayar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.rupiah(totalSupplierOwed),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MoneyOutRed)
                            )
                            Text("${supplierDebts.count { it.status != "LUNAS" }} tagihan supplier", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Akan Diterima (Piutang Pelanggan)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Akan Diterima", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.rupiah(totalCustomerReceivable),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MoneyInGreen)
                            )
                            Text("${customerDebts.count { it.status != "LUNAS" }} piutang pelanggan", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Tab Selector
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .height(44.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Utang Supplier", fontWeight = FontWeight.Bold) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Piutang Pelanggan", fontWeight = FontWeight.Bold) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // List of Debts
            if (displayedList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = MoneyInGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "Tidak Ada Utang Supplier" else "Tidak Ada Piutang Pelanggan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Semua tagihan sudah rapi dan tercatat lunas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedList) { debt ->
                    DebtItemCard(
                        transaction = debt,
                        isReceivable = selectedTab == 1,
                        onPayClick = { transactionToPay = debt },
                        onWhatsAppClick = {
                            val contact = debt.contactName ?: "Pelanggan"
                            val dueStr = debt.dueDate?.let { Formatters.date(it) } ?: "segera"
                            val msg = "Halo Kak $contact, dari ${viewModel.businessProfile.value?.businessName ?: "SakuUsaha"}. Mengingatkan tagihan sebesar ${Formatters.rupiah(debt.remainingDebt)} yang jatuh tempo pada $dueStr. Mohon konfirmasinya ya kak, terima kasih banyak! 🙏"
                            Formatters.openWhatsAppMessage(context, debt.contactPhone, msg)
                        },
                        onQuickSettle = {
                            viewModel.settleDebtFull(debt.id)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DebtItemCard(
    transaction: TransactionEntity,
    isReceivable: Boolean,
    onPayClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onQuickSettle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPastDue = Formatters.isPastDueDate(transaction.dueDate)
    val isSettled = transaction.status == "LUNAS"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSettled) MoneyInGreenBg else WarningYellowBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSettled) Icons.Default.Check else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (isSettled) MoneyInGreen else WarningYellow,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.contactName ?: (if (isReceivable) "Pelanggan" else "Supplier"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transaction.category} • Jatuh Tempo: ${transaction.dueDate?.let { Formatters.date(it) } ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.rupiah(transaction.remainingDebt),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSettled) MoneyInGreen else (if (isPastDue) MoneyOutRed else WarningYellow)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(
                        text = if (isSettled) "Lunas" else (if (isPastDue) "Lewat Tempo" else transaction.status),
                        containerColor = if (isSettled) MoneyInGreenBg else (if (isPastDue) MoneyOutRedBg else WarningYellowBg),
                        contentColor = if (isSettled) MoneyInGreen else (if (isPastDue) MoneyOutRed else WarningYellow)
                    )
                }
            }

            if (!isSettled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp reminder (especially for receivable)
                    OutlinedButton(
                        onClick = onWhatsAppClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ingatkan WA", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Lunas Button
                    TextButton(
                        onClick = onQuickSettle,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Lunas Penuh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Bayar / Terima modal
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReceivable) MoneyInGreen else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(if (isReceivable) "Terima Cicilan" else "Bayar Cicilan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPaymentModal(
    transaction: TransactionEntity,
    isReceiving: Boolean,
    onPay: (Long, String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var isFullPayment by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf(transaction.remainingDebt.toString()) }
    var selectedMethod by remember { mutableStateOf("TUNAI") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (isReceiving) "Terima Pembayaran Piutang" else "Bayar Tagihan Utang",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Pihak: ${transaction.contactName ?: "-"} • Sisa: ${Formatters.rupiah(transaction.remainingDebt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Full vs Partial segment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFullPayment) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable {
                            isFullPayment = true
                            amountText = transaction.remainingDebt.toString()
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Lunas Penuh (${Formatters.rupiah(transaction.remainingDebt)})",
                        color = if (isFullPayment) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isFullPayment) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable {
                            isFullPayment = false
                            amountText = ""
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sebagian (Cicil)",
                        color = if (!isFullPayment) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                label = { Text("Jumlah Pembayaran") },
                prefix = { Text("Rp ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Metode Pembayaran:", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TUNAI" to "Tunai", "TRANSFER" to "Transfer", "QRIS" to "QRIS").forEach { (code, label) ->
                    FilterChip(
                        selected = selectedMethod == code,
                        onClick = { selectedMethod = code },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan / Keterangan (Opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    if (amt > 0L) {
                        onPay(amt.coerceAtMost(transaction.remainingDebt), selectedMethod, notes.ifBlank { null })
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Simpan Pembayaran", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
