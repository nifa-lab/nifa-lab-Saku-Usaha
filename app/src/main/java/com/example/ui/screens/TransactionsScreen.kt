package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.DetailedTransactionCard
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TransactionDetailModalSheet
import com.example.ui.theme.*
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: SakuUsahaViewModel,
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDetailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingReceiptTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEditCategory by remember { mutableStateOf<TransactionEntity?>(null) }

    // Totals for top selection cards
    val allCount = transactions.size
    val penjualanList = transactions.filter { it.type == "PENJUALAN" || it.type == "JUAL" }
    val modalList = transactions.filter { it.type == "MODAL" }
    val beliList = transactions.filter { it.type == "BELI" }
    val biayaList = transactions.filter { it.type == "BIAYA" }

    val totalPenjualan = penjualanList.sumOf { it.amount }
    val totalModal = modalList.sumOf { it.amount }
    val totalPemasukan = totalPenjualan + totalModal
    val totalBeli = beliList.sumOf { it.amount }
    val totalBiaya = biayaList.sumOf { it.amount }
    val totalPengeluaran = totalBeli + totalBiaya

    // Search filter for recent transactions list
    val displayedRecent = transactions.filter { tx ->
        if (searchQuery.isBlank()) true else {
            (tx.productName ?: "").contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    (tx.contactName ?: "").contains(searchQuery, ignoreCase = true) ||
                    (tx.notes ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    if (selectedDetailTransaction != null) {
        TransactionDetailModalSheet(
            transaction = selectedDetailTransaction!!,
            onDismissRequest = { selectedDetailTransaction = null },
            onViewReceipt = {
                viewingReceiptTransaction = selectedDetailTransaction
                selectedDetailTransaction = null
            },
            onEditCategory = {
                transactionToEditCategory = selectedDetailTransaction
                selectedDetailTransaction = null
            },
            onDelete = {
                transactionToDelete = selectedDetailTransaction
                selectedDetailTransaction = null
            }
        )
    }

    if (viewingReceiptTransaction != null) {
        ReceiptDialog(
            fileName = viewingReceiptTransaction!!.receiptFileName ?: "bukti.jpg",
            sizeText = viewingReceiptTransaction!!.receiptSizeText,
            transactionTitle = viewingReceiptTransaction!!.productName ?: viewingReceiptTransaction!!.category,
            amount = viewingReceiptTransaction!!.amount,
            onDismissRequest = { viewingReceiptTransaction = null }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Hapus Transaksi?") },
            text = { Text("Transaksi sebesar ${Formatters.rupiah(transactionToDelete!!.amount)} akan dihapus. Perubahan stok akan dikembalikan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transactionToDelete!!)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyOutRed),
                    modifier = Modifier.testTag("btn_confirm_delete_recent_tx")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (transactionToEditCategory != null) {
        val catType = when (transactionToEditCategory!!.type) {
            "MODAL" -> "MODAL"
            "PENJUALAN", "JUAL" -> "PENJUALAN"
            "BELI" -> "BELI"
            else -> "BIAYA"
        }
        val availableCategories by viewModel.categoriesByType(catType).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { transactionToEditCategory = null },
            title = { Text("Ubah Kategori Transaksi") },
            text = {
                Column {
                    Text("Pilih kategori baru:")
                    Spacer(modifier = Modifier.height(10.dp))
                    availableCategories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (transactionToEditCategory!!.category == cat.name) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTransactionCategory(transactionToEditCategory!!, cat.name)
                                    transactionToEditCategory = null
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Text(cat.name, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { transactionToEditCategory = null }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Header
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Riwayat Transaksi",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pilih jenis transaksi untuk melihat rincian rentang waktu & tanggal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AiAssistantTopActionButton()
                }
            }
        }

        // BAGIAN ATAS: Kotak-Kotak Kategori Transaksi (Pemasukan: Penjualan & Modal, Pengeluaran: Beli & Biaya)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Semua Transaksi Overview
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openTransactionTypeDetail("SEMUA") }
                        .testTag("box_type_semua")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Semua Riwayat Transaksi",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$allCount transaksi tercatat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Buka",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: PEMASUKAN (Penjualan & Modal)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MoneyInGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PEMASUKAN",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MoneyInGreen
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TransactionTypeCardBox(
                        title = "Penjualan",
                        subtitle = "${penjualanList.size} transaksi",
                        amountText = "+ " + Formatters.rupiah(totalPenjualan),
                        accentColor = MoneyInGreen,
                        bgColor = MoneyInGreenBg,
                        icon = Icons.Default.TrendingUp,
                        onClick = { viewModel.openTransactionTypeDetail("PENJUALAN") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("box_type_jual")
                    )

                    TransactionTypeCardBox(
                        title = "Modal Usaha",
                        subtitle = "${modalList.size} setoran modal",
                        amountText = "+ " + Formatters.rupiah(totalModal),
                        accentColor = Color(0xFF0D9488),
                        bgColor = Color(0xFFCCFBF1),
                        icon = Icons.Default.Savings,
                        onClick = { viewModel.openTransactionTypeDetail("MODAL") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("box_type_modal")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: PENGELUARAN (Beli Stok & Biaya)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MoneyOutRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PENGELUARAN",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MoneyOutRed
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TransactionTypeCardBox(
                        title = "Beli Stok",
                        subtitle = "${beliList.size} pengadaan",
                        amountText = "- " + Formatters.rupiah(totalBeli),
                        accentColor = Color(0xFF0284C7),
                        bgColor = Color(0xFFE0F2FE),
                        icon = Icons.Default.Inventory2,
                        onClick = { viewModel.openTransactionTypeDetail("BELI") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("box_type_beli")
                    )

                    TransactionTypeCardBox(
                        title = "Biaya Operasional",
                        subtitle = "${biayaList.size} pengeluaran",
                        amountText = "- " + Formatters.rupiah(totalBiaya),
                        accentColor = MoneyOutRed,
                        bgColor = MoneyOutRedBg,
                        icon = Icons.Default.Receipt,
                        onClick = { viewModel.openTransactionTypeDetail("BIAYA") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("box_type_biaya")
                    )
                }
            }
        }

        // BAGIAN BAWAH: Transaksi Terbaru
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaksi Terbaru",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${displayedRecent.size} transaksi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari barang, kategori, pihak...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_recent_transactions")
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // List of recent transactions
        if (displayedRecent.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tidak Ada Transaksi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Belum ada transaksi yang sesuai kata kunci pencarian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedRecent) { tx ->
                DetailedTransactionCard(
                    transaction = tx,
                    onClick = { selectedDetailTransaction = tx },
                    onViewReceipt = { viewingReceiptTransaction = tx },
                    onEditCategory = { transactionToEditCategory = tx },
                    onDelete = { transactionToDelete = tx },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionTypeCardBox(
    title: String,
    subtitle: String,
    amountText: String,
    accentColor: Color,
    bgColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Buka",
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
