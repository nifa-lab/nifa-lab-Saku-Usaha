package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.DetailedTransactionCard
import com.example.ui.components.PeriodPickerModal
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.ExportOptionsModalSheet
import com.example.ui.components.TransactionDetailModalSheet
import com.example.ui.theme.*
import com.example.ui.util.DateFilterPeriod
import com.example.ui.util.Formatters
import com.example.ui.util.ReportExporter
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeDetailScreen(
    viewModel: SakuUsahaViewModel,
    transactions: List<TransactionEntity>,
    typeKey: String, // "SEMUA", "JUAL", "BELI", "BIAYA"
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile by viewModel.businessProfile.collectAsState()
    var selectedPeriod by remember { mutableStateOf(DateFilterPeriod.BULAN_INI) }
    var showPeriodModal by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedDetailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingReceiptTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEditCategory by remember { mutableStateOf<TransactionEntity?>(null) }

    val (title, headerColor, headerBgColor) = when (typeKey) {
        "MODAL" -> Triple("Riwayat Penambahan Modal", Color(0xFF0D9488), Color(0xFFCCFBF1))
        "PENJUALAN", "JUAL" -> Triple("Riwayat Penjualan", MoneyInGreen, MoneyInGreenBg)
        "PEMASUKAN" -> Triple("Riwayat Semua Pemasukan", MoneyInGreen, MoneyInGreenBg)
        "BELI" -> Triple("Riwayat Pembelian Stok", Color(0xFF0284C7), Color(0xFFE0F2FE))
        "BIAYA" -> Triple("Riwayat Biaya Operasional", MoneyOutRed, MoneyOutRedBg)
        "PENGELUARAN" -> Triple("Riwayat Semua Pengeluaran", MoneyOutRed, MoneyOutRedBg)
        else -> Triple("Semua Transaksi", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    // Filter by date range and type
    val (start, end) = selectedPeriod.getTimeRange()
    val filtered = transactions.filter { tx ->
        val inRange = tx.timestamp in start..end
        val matchesType = when (typeKey) {
            "MODAL" -> tx.type == "MODAL"
            "PENJUALAN", "JUAL" -> tx.type == "PENJUALAN" || tx.type == "JUAL"
            "PEMASUKAN" -> tx.direction == "MASUK" || tx.type == "MODAL" || tx.type == "PENJUALAN" || tx.type == "JUAL"
            "BELI" -> tx.type == "BELI"
            "BIAYA" -> tx.type == "BIAYA"
            "PENGELUARAN" -> tx.direction == "KELUAR" || tx.type == "BELI" || tx.type == "BIAYA"
            else -> true
        }
        val matchesSearch = if (searchQuery.isBlank()) true else {
            (tx.productName ?: "").contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    (tx.contactName ?: "").contains(searchQuery, ignoreCase = true) ||
                    (tx.notes ?: "").contains(searchQuery, ignoreCase = true)
        }
        inRange && matchesType && matchesSearch
    }

    val totalAmount = filtered.sumOf { it.amount }
    val totalCount = filtered.size
    val averageAmount = if (totalCount > 0) totalAmount / totalCount else 0L

    val grouped = filtered.groupBy { Formatters.date(it.timestamp) }

    if (showPeriodModal) {
        PeriodPickerModal(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { selectedPeriod = it },
            onDismissRequest = { showPeriodModal = false }
        )
    }

    if (showExportSheet) {
        ExportOptionsModalSheet(
            onExportPdf = {
                ReportExporter.generateAndSharePdf(
                    context = context,
                    profile = profile,
                    transactions = filtered,
                    periodLabel = "${title} - ${selectedPeriod.label}",
                    startTime = start,
                    endTime = end
                )
                showExportSheet = false
            },
            onExportExcel = {
                ReportExporter.generateAndShareExcel(
                    context = context,
                    profile = profile,
                    transactions = filtered,
                    periodLabel = "${title} - ${selectedPeriod.label}",
                    startTime = start,
                    endTime = end
                )
                showExportSheet = false
            },
            onDirectPrint = {
                ReportExporter.printReport(
                    context = context,
                    profile = profile,
                    transactions = filtered,
                    periodLabel = "${title} - ${selectedPeriod.label}",
                    startTime = start,
                    endTime = end
                )
                showExportSheet = false
            },
            onDismiss = { showExportSheet = false }
        )
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
                    modifier = Modifier.testTag("btn_confirm_delete_detail_tx")
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
        val catType = if (transactionToEditCategory!!.type == "JUAL") "PENJUALAN" else "BIAYA"
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Filter rentang waktu & riwayat lengkap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_detail_history")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showExportSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = headerBgColor,
                            contentColor = headerColor
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("btn_export_detail_type")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            // Section: Pilihan Rentang Waktu dan Tanggal (Prominent Top Controls)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = headerColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pilih Rentang Waktu & Tanggal",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = { showPeriodModal = true }) {
                                Text("Kalender >", fontWeight = FontWeight.Bold, color = headerColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal Chip Row of Date Shortcuts
                        val periods = DateFilterPeriod.values().toList()
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(periods) { p ->
                                val isSelected = selectedPeriod == p
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPeriod = p },
                                    label = { Text(p.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = headerColor,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("detail_period_${p.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Date Range Pill Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = headerBgColor.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, headerColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPeriodModal = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = headerColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${selectedPeriod.label}: ${Formatters.date(start)} - ${Formatters.date(end)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = "Ubah Tanggal",
                                    tint = headerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Summary Metrics Card for this specific type & range
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (typeKey == "JUAL") "Total Pemasukan" else (if (typeKey == "BELI" || typeKey == "BIAYA") "Total Pengeluaran" else "Total Nominal"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.rupiah(totalAmount),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = headerColor
                                )
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(44.dp)
                                .width(1.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Jumlah Transaksi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalCount transaksi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(44.dp)
                                .width(1.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Rata-rata",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.rupiah(averageAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari barang, pelanggan, kategori...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_detail_search")
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Grouped Transactions List
            if (grouped.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Transaksi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tidak ada transaksi pada rentang waktu yang dipilih.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { showPeriodModal = true }) {
                            Text("Ubah Rentang Waktu")
                        }
                    }
                }
            } else {
                grouped.forEach { (dateStr, txList) ->
                    val dayTotal = txList.sumOf { it.amount }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ${Formatters.rupiah(dayTotal)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = headerColor
                            )
                        }
                    }

                    items(txList) { tx ->
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
    }
}
