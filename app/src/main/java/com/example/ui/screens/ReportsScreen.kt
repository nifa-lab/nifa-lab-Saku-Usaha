package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.ExportOptionsModalSheet
import com.example.ui.components.PeriodPickerModal
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.util.DateFilterPeriod
import com.example.ui.util.Formatters
import com.example.ui.util.ReportExporter
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: SakuUsahaViewModel,
    profile: BusinessProfileEntity?,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.reportDatePeriod.collectAsState()
    var showPeriodModal by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var reportTab by remember { mutableIntStateOf(0) } // 0: Laba Rugi, 1: Arus Kas, 2: Stok

    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()

    val (start, end) = selectedPeriod.getTimeRange()
    val periodTransactions = transactions.filter { it.timestamp in start..end }

    // Financial Metrics for Laba Rugi & Arus Kas (Pemasukan: Penjualan & Modal; Pengeluaran: Beli & Biaya)
    val penjualanTx = periodTransactions.filter { it.type == "PENJUALAN" || it.type == "JUAL" }
    val modalTx = periodTransactions.filter { it.type == "MODAL" }
    val totalPenjualan = penjualanTx.sumOf { it.amount }
    val totalModal = modalTx.sumOf { it.amount }
    val totalIncome = totalPenjualan + totalModal

    val beliTx = periodTransactions.filter { it.type == "BELI" }
    val biayaTx = periodTransactions.filter { it.type == "BIAYA" }
    val totalBeli = beliTx.sumOf { it.amount }
    val totalBiaya = biayaTx.sumOf { it.amount }
    val totalExpense = totalBeli + totalBiaya

    // Laba Bersih Usaha = Pendapatan Penjualan - (Beli Stok + Biaya Operasional)
    val netProfit = totalPenjualan - totalExpense
    val profitMargin = if (totalPenjualan > 0) ((netProfit.toFloat() / totalPenjualan.toFloat()) * 100).toInt() else 0

    // Top selling products in selected period (from penjualan)
    val productSales = penjualanTx
        .filter { it.productName != null }
        .groupBy { it.productName!! }
        .map { (name, list) ->
            Triple(name, list.sumOf { it.quantity }, list.sumOf { it.amount })
        }
        .sortedByDescending { it.third }

    // Expenses by category (Beli & Biaya)
    val expenseCategories = (beliTx + biayaTx)
        .groupBy { it.category }
        .map { (cat, list) ->
            Pair(cat, list.sumOf { it.amount })
        }
        .sortedByDescending { it.second }

    // Arus Kas Calculations (Cash Flow)
    val cashPenjualan = penjualanTx.filter { it.paymentMethod != "HUTANG" }.sumOf { it.amount }
    val cashModal = modalTx.filter { it.paymentMethod != "HUTANG" }.sumOf { it.amount }
    val cashInflow = cashPenjualan + cashModal

    val cashBeli = beliTx.filter { it.paymentMethod != "HUTANG" }.sumOf { it.amount }
    val cashBiaya = biayaTx.filter { it.paymentMethod != "HUTANG" }.sumOf { it.amount }
    val cashOutflow = cashBeli + cashBiaya
    val netCashFlow = cashInflow - cashOutflow

    val cashTunaiIn = (penjualanTx + modalTx).filter { it.paymentMethod == "TUNAI" }.sumOf { it.amount }
    val cashTunaiOut = (beliTx + biayaTx).filter { it.paymentMethod == "TUNAI" }.sumOf { it.amount }

    val cashTransferIn = (penjualanTx + modalTx).filter { it.paymentMethod == "TRANSFER" }.sumOf { it.amount }
    val cashTransferOut = (beliTx + biayaTx).filter { it.paymentMethod == "TRANSFER" }.sumOf { it.amount }

    val cashQrisIn = (penjualanTx + modalTx).filter { it.paymentMethod == "QRIS" || it.paymentMethod == "E_WALLET" }.sumOf { it.amount }
    val cashQrisOut = (beliTx + biayaTx).filter { it.paymentMethod == "QRIS" || it.paymentMethod == "E_WALLET" }.sumOf { it.amount }

    val unpaidReceivables = penjualanTx.filter { it.paymentMethod == "HUTANG" }.sumOf { it.remainingDebt }
    val unpaidPayables = (beliTx + biayaTx).filter { it.paymentMethod == "HUTANG" }.sumOf { it.remainingDebt }

    // Stok Calculations
    val totalStockValuationCost = allProducts.sumOf { it.costPrice * it.stock.coerceAtLeast(0) }
    val totalStockValuationSell = allProducts.sumOf { it.sellPrice * it.stock.coerceAtLeast(0) }
    val potentialProfitFromStock = totalStockValuationSell - totalStockValuationCost
    val totalStockUnits = allProducts.sumOf { it.stock.coerceAtLeast(0) }
    val outOfStockCount = allProducts.count { it.stock <= 0 }
    val lowStockCount = allProducts.count { it.stock in 1..it.minStockAlert }

    if (showPeriodModal) {
        PeriodPickerModal(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { viewModel.setReportDatePeriod(it) },
            onDismissRequest = { showPeriodModal = false }
        )
    }

    if (showExportSheet) {
        val tabTitle = when (reportTab) {
            1 -> "Laporan Arus Kas"
            2 -> "Laporan Stok & Inventaris"
            else -> "Laporan Laba Rugi"
        }
        ExportOptionsModalSheet(
            onExportPdf = {
                ReportExporter.generateAndSharePdf(
                    context = context,
                    profile = profile,
                    transactions = periodTransactions,
                    periodLabel = "$tabTitle - ${selectedPeriod.label}",
                    startTime = start,
                    endTime = end
                )
                showExportSheet = false
            },
            onExportExcel = {
                ReportExporter.generateAndShareExcel(
                    context = context,
                    profile = profile,
                    transactions = periodTransactions,
                    periodLabel = "$tabTitle - ${selectedPeriod.label}",
                    startTime = start,
                    endTime = end
                )
                showExportSheet = false
            },
            onDirectPrint = {
                ReportExporter.printReport(
                    context = context,
                    profile = profile,
                    transactions = periodTransactions,
                    periodLabel = "$tabTitle - ${selectedPeriod.label}",
                    startTime = start,
                    endTime = end
                )
                showExportSheet = false
            },
            onDismiss = { showExportSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Keuangan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_reports")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showExportSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("btn_top_export_report")
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
            // Report Sub-tabs (Laba Rugi, Arus Kas, Stok)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TabRow(
                            selectedTabIndex = reportTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .height(44.dp)
                        ) {
                            listOf("Laba Rugi", "Arus Kas", "Stok").forEachIndexed { index, tabTitle ->
                                Tab(
                                    selected = reportTab == index,
                                    onClick = { reportTab = index },
                                    text = { Text(tabTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("tab_report_$index")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Period selector button
                        OutlinedButton(
                            onClick = { showPeriodModal = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_report_period")
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Periode: ${selectedPeriod.label} (${Formatters.date(start)} - ${Formatters.date(end)})")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }
            }

            // =========================================================================
            // TAB 0: LABA RUGI
            // =========================================================================
            if (reportTab == 0) {
                // Summary Indicator Cards
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCardItem(
                            title = "Pendapatan Penjualan (Omset)",
                            amount = totalPenjualan,
                            trendText = "${penjualanTx.size} transaksi penjualan produk",
                            color = MoneyInGreen,
                            icon = Icons.Default.TrendingUp
                        )

                        ReportCardItem(
                            title = "Total Beban & Pengeluaran",
                            amount = totalExpense,
                            trendText = "Beli Stok: ${Formatters.rupiah(totalBeli)} • Biaya: ${Formatters.rupiah(totalBiaya)}",
                            color = MoneyOutRed,
                            icon = Icons.Default.TrendingDown
                        )

                        ReportCardItem(
                            title = "Laba Bersih Operasional (Margin ${profitMargin}%)",
                            amount = netProfit,
                            trendText = if (netProfit >= 0) "Bisnis menghasilkan laba sehat" else "Perlu efisiensi beban & biaya operasional",
                            color = if (netProfit >= 0) MaterialTheme.colorScheme.primary else MoneyOutRed,
                            icon = Icons.Default.AccountBalance
                        )

                        if (totalModal > 0) {
                            ReportCardItem(
                                title = "Suntikan Modal Usaha (Ekuitas)",
                                amount = totalModal,
                                trendText = "${modalTx.size} setoran modal (Pemasukan modal/investor, bukan omset penjualan)",
                                color = Color(0xFF0D9488),
                                icon = Icons.Default.Savings
                            )
                        }
                    }
                }

                // Dedicated Card: 4-Pilar Komposisi Keuangan (Pemasukan vs Pengeluaran)
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Komposisi Keuangan Usaha",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Rincian Pemasukan (Penjualan & Modal) dan Pengeluaran (Beli & Biaya)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Sub-kategori Pemasukan
                            Text(
                                text = "PEMASUKAN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MoneyInGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MoneyInGreenBg,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Penjualan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MoneyInGreen)
                                        Text(Formatters.rupiah(totalPenjualan), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MoneyInGreen)
                                        Text("${penjualanTx.size} transaksi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFCCFBF1),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Modal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D9488))
                                        Text(Formatters.rupiah(totalModal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                        Text("${modalTx.size} transaksi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sub-kategori Pengeluaran
                            Text(
                                text = "PENGELUARAN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MoneyOutRed
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE0F2FE),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Beli Stok", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                                        Text(Formatters.rupiah(totalBeli), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                        Text("${beliTx.size} belanja", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MoneyOutRedBg,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Biaya Ops", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MoneyOutRed)
                                        Text(Formatters.rupiah(totalBiaya), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MoneyOutRed)
                                        Text("${biayaTx.size} beban", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Dedicated Card: Ekspor & Unduh Laporan (PDF, Excel, Print)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExportActionCard(
                        context = context,
                        profile = profile,
                        transactions = periodTransactions,
                        periodLabel = "Laba Rugi - ${selectedPeriod.label}",
                        start = start,
                        end = end
                    )
                }

                // Interactive Bar Comparison (Penjualan, Modal, Beli, Biaya)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Perbandingan 4 Kategori Transaksi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val maxVal = maxOf(totalPenjualan, totalModal, totalBeli, totalBiaya, 100_000L).toFloat()
                            val penjualanFrac = (totalPenjualan.toFloat() / maxVal).coerceIn(0.08f, 1f)
                            val modalFrac = (totalModal.toFloat() / maxVal).coerceIn(0.08f, 1f)
                            val beliFrac = (totalBeli.toFloat() / maxVal).coerceIn(0.08f, 1f)
                            val biayaFrac = (totalBiaya.toFloat() / maxVal).coerceIn(0.08f, 1f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .fillMaxHeight(penjualanFrac)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(MoneyInGreen)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Penjualan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                    Text(Formatters.rupiah(totalPenjualan), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .fillMaxHeight(modalFrac)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(Color(0xFF0D9488))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Modal", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                    Text(Formatters.rupiah(totalModal), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .fillMaxHeight(beliFrac)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(Color(0xFF0284C7))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Beli Stok", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                    Text(Formatters.rupiah(totalBeli), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .fillMaxHeight(biayaFrac)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(MoneyOutRed)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Biaya Ops", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                    Text(Formatters.rupiah(totalBiaya), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                                }
                            }
                        }
                    }
                }

                // Top Selling Products
                if (productSales.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Produk Terlaris",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(productSales.take(5)) { (name, qty, revenue) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Terjual: $qty pcs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = Formatters.rupiah(revenue),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyInGreen
                                    )
                                )
                            }
                        }
                    }
                }

                // Expense Breakdown by Category
                if (expenseCategories.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Rincian Beban & Pengeluaran",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(expenseCategories) { (cat, amt) ->
                        val percentage = if (totalExpense > 0) ((amt.toFloat() / totalExpense.toFloat()) * 100).toInt() else 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${Formatters.rupiah(amt)} ($percentage%)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (percentage.toFloat() / 100f).coerceIn(0f, 1f) },
                                    color = MoneyOutRed,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // TAB 1: ARUS KAS (CASH FLOW)
            // =========================================================================
            else if (reportTab == 1) {
                // Summary Indicator Cards for Cash Flow
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCardItem(
                            title = "Total Kas Masuk (Penjualan & Modal)",
                            amount = cashInflow,
                            trendText = "Penjualan: ${Formatters.rupiah(cashPenjualan)} • Modal: ${Formatters.rupiah(cashModal)}",
                            color = MoneyInGreen,
                            icon = Icons.Default.Payments
                        )

                        ReportCardItem(
                            title = "Total Kas Keluar (Beli Stok & Biaya)",
                            amount = cashOutflow,
                            trendText = "Beli Stok: ${Formatters.rupiah(cashBeli)} • Biaya: ${Formatters.rupiah(cashBiaya)}",
                            color = MoneyOutRed,
                            icon = Icons.Default.MoneyOff
                        )

                        ReportCardItem(
                            title = "Arus Kas Bersih (Net Cash Flow)",
                            amount = netCashFlow,
                            trendText = if (netCashFlow >= 0) "Aliran kas surplus / positif" else "Aliran kas defisit",
                            color = if (netCashFlow >= 0) MaterialTheme.colorScheme.primary else MoneyOutRed,
                            icon = Icons.Default.AccountBalanceWallet
                        )
                    }
                }

                // Dedicated Card: 4-Komponen Aliran Kas Riil
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Komposisi Aliran Kas Riil",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Penerimaan dan pengeluaran kas tunai/transfer/QRIS riil periode ini",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Sub-kategori Kas Masuk
                            Text(
                                text = "KAS MASUK",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MoneyInGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MoneyInGreenBg,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Dari Penjualan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MoneyInGreen)
                                        Text(Formatters.rupiah(cashPenjualan), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MoneyInGreen)
                                        Text("Penerimaan riil", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFCCFBF1),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Dari Modal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D9488))
                                        Text(Formatters.rupiah(cashModal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                        Text("Setoran kas", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sub-kategori Kas Keluar
                            Text(
                                text = "KAS KELUAR",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MoneyOutRed
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE0F2FE),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Beli Stok", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                                        Text(Formatters.rupiah(cashBeli), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                        Text("Pengadaan barang", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MoneyOutRedBg,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Biaya Ops", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MoneyOutRed)
                                        Text(Formatters.rupiah(cashBiaya), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MoneyOutRed)
                                        Text("Beban operasional", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Export Card for Cash Flow
                item {
                    ExportActionCard(
                        context = context,
                        profile = profile,
                        transactions = periodTransactions,
                        periodLabel = "Arus Kas - ${selectedPeriod.label}",
                        start = start,
                        end = end
                    )
                }

                // Rekap Saldo Berdasarkan Metode Pembayaran
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Arus Kas per Metode Pembayaran",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            PaymentMethodCashRow(
                                title = "Kas Tunai",
                                inAmount = cashTunaiIn,
                                outAmount = cashTunaiOut,
                                icon = Icons.Default.AttachMoney
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            PaymentMethodCashRow(
                                title = "Transfer Bank",
                                inAmount = cashTransferIn,
                                outAmount = cashTransferOut,
                                icon = Icons.Default.AccountBalance
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            PaymentMethodCashRow(
                                title = "QRIS & E-Wallet",
                                inAmount = cashQrisIn,
                                outAmount = cashQrisOut,
                                icon = Icons.Default.QrCodeScanner
                            )

                            if (unpaidReceivables > 0 || unpaidPayables > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                PaymentMethodCashRow(
                                    title = "Utang & Piutang Berjalan",
                                    inAmount = unpaidReceivables,
                                    outAmount = unpaidPayables,
                                    icon = Icons.Default.HourglassTop,
                                    labelIn = "Piutang Belum Terima",
                                    labelOut = "Utang Belum Bayar"
                                )
                            }
                        }
                    }
                }

                // Recent Cash Movement Log in Period
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Mutasi Aliran Kas (${periodTransactions.size} Transaksi)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (periodTransactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada transaksi pada periode yang dipilih",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(periodTransactions.sortedByDescending { it.timestamp }) { tx ->
                        val isIncome = tx.direction == "MASUK" || tx.type == "MODAL" || tx.type == "PENJUALAN" || tx.type == "JUAL"
                        val iconVector = when (tx.type) {
                            "MODAL" -> Icons.Default.Savings
                            "PENJUALAN", "JUAL" -> Icons.Default.TrendingUp
                            "BELI" -> Icons.Default.Inventory2
                            else -> Icons.Default.Receipt
                        }
                        val iconBg = when (tx.type) {
                            "MODAL" -> Color(0xFFCCFBF1)
                            "PENJUALAN", "JUAL" -> MoneyInGreenBg
                            "BELI" -> Color(0xFFE0F2FE)
                            else -> MoneyOutRedBg
                        }
                        val iconColor = when (tx.type) {
                            "MODAL" -> Color(0xFF0D9488)
                            "PENJUALAN", "JUAL" -> MoneyInGreen
                            "BELI" -> Color(0xFF0284C7)
                            else -> MoneyOutRed
                        }
                        val typeLabel = when (tx.type) {
                            "MODAL" -> "Modal"
                            "PENJUALAN", "JUAL" -> "Penjualan"
                            "BELI" -> "Beli Stok"
                            else -> "Biaya Ops"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tx.productName ?: tx.category,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            StatusBadge(
                                                text = typeLabel,
                                                containerColor = iconBg,
                                                contentColor = iconColor
                                            )
                                        }
                                        Text(
                                            text = "${Formatters.date(tx.timestamp)} • ${tx.paymentMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${if (isIncome) "+" else "-"} ${Formatters.rupiah(tx.amount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = iconColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // TAB 2: STOK (INVENTORY & VALUATION)
            // =========================================================================
            else if (reportTab == 2) {
                // Summary Cards for Inventory
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCardItem(
                            title = "Total Nilai Modal Aset Stok",
                            amount = totalStockValuationCost,
                            trendText = "$totalStockUnits unit fisik dari ${allProducts.size} produk",
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.Inventory2
                        )

                        ReportCardItem(
                            title = "Estimasi Nilai Jual Stok (Omset Potensial)",
                            amount = totalStockValuationSell,
                            trendText = "Jika semua stok terjual habis",
                            color = MoneyInGreen,
                            icon = Icons.Default.Store
                        )

                        ReportCardItem(
                            title = "Potensi Keuntungan dari Stok",
                            amount = potentialProfitFromStock,
                            trendText = "Selisih harga jual dan modal stok",
                            color = BrandDarkGreen,
                            icon = Icons.Default.Savings
                        )
                    }
                }

                // Inventory Stock Health Status Bar
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Kesehatan Inventaris Stok",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StockStatusCounter(
                                    label = "Stok Aman",
                                    count = allProducts.count { it.stock > it.minStockAlert },
                                    color = MoneyInGreen
                                )
                                StockStatusCounter(
                                    label = "Stok Menipis",
                                    count = lowStockCount,
                                    color = Color(0xFFD97706)
                                )
                                StockStatusCounter(
                                    label = "Stok Habis",
                                    count = outOfStockCount,
                                    color = MoneyOutRed
                                )
                            }
                        }
                    }
                }

                // Critical Stock Warning if any
                if (lowStockProducts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${lowStockProducts.size} Produk Memerlukan Restok Segera!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Stok berada pada atau di bawah batas minimum pengingat.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }
                }

                // Inventory Items Detailed Table / List
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Rincian & Valuasi Stok Produk (${allProducts.size} Produk)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (allProducts.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada produk yang terdaftar di sistem",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(allProducts) { prod ->
                        val isLow = prod.stock in 1..prod.minStockAlert
                        val isOut = prod.stock <= 0
                        val statusText = when {
                            isOut -> "Habis"
                            isLow -> "Menipis"
                            else -> "Aman"
                        }
                        val statusColor = when {
                            isOut -> MoneyOutRed
                            isLow -> Color(0xFFD97706)
                            else -> MoneyInGreen
                        }
                        val statusBg = when {
                            isOut -> MoneyOutRedBg
                            isLow -> Color(0xFFFEF3C7)
                            else -> MoneyInGreenBg
                        }

                        // Count units sold in this period
                        val unitsSoldInPeriod = periodTransactions
                            .filter { it.type == "JUAL" && (it.productId == prod.id || it.productName?.contains(prod.name) == true) }
                            .sumOf { it.quantity }

                        val totalItemCost = prod.costPrice * prod.stock.coerceAtLeast(0)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(prod.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = prod.categoryName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = "$statusText (${prod.stock} unit)",
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Harga Beli: ${Formatters.rupiah(prod.costPrice)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Harga Jual: ${Formatters.rupiah(prod.sellPrice)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Nilai Aset: ${Formatters.rupiah(totalItemCost)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (unitsSoldInPeriod > 0) {
                                            Text(
                                                text = "Terjual Periode Ini: $unitsSoldInPeriod unit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MoneyInGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCardItem(
    title: String,
    amount: Long,
    trendText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Formatters.rupiah(amount),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                )
                Text(
                    text = trendText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExportActionCard(
    context: Context,
    profile: BusinessProfileEntity?,
    transactions: List<TransactionEntity>,
    periodLabel: String,
    start: Long,
    end: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Ekspor & Cetak Laporan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Unduh format resmi PDF, tabel Excel, atau cetak langsung",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button PDF
                Button(
                    onClick = {
                        ReportExporter.generateAndSharePdf(
                            context = context,
                            profile = profile,
                            transactions = transactions,
                            periodLabel = periodLabel,
                            startTime = start,
                            endTime = end
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyOutRed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_pdf")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Button Excel
                Button(
                    onClick = {
                        ReportExporter.generateAndShareExcel(
                            context = context,
                            profile = profile,
                            transactions = transactions,
                            periodLabel = periodLabel,
                            startTime = start,
                            endTime = end
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyInGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_excel")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Button Print
                OutlinedButton(
                    onClick = {
                        ReportExporter.printReport(
                            context = context,
                            profile = profile,
                            transactions = transactions,
                            periodLabel = periodLabel,
                            startTime = start,
                            endTime = end
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_direct_print")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PaymentMethodCashRow(
    title: String,
    inAmount: Long,
    outAmount: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelIn: String = "Masuk",
    labelOut: String = "Keluar"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(labelIn, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    Formatters.rupiah(inAmount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneyInGreen
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(labelOut, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    Formatters.rupiah(outAmount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneyOutRed
                )
            }
        }
    }
}

@Composable
fun StockStatusCounter(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
