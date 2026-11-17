package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.TransactionDetailModalSheet
import com.example.ui.theme.*
import com.example.ui.util.DateFilterPeriod
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.SakuUsahaViewModel
import java.util.Calendar

private enum class ProductTxRangePreset(val label: String) {
    SEMUA("Semua Waktu"),
    HARI_INI("Hari Ini"),
    TUJUH_HARI("7 Hari"),
    TIGA_PULUH_HARI("30 Hari"),
    BULAN_INI("Bulan Ini"),
    KUSTOM("Rentang Tanggal")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTransactionsScreen(
    viewModel: SakuUsahaViewModel,
    products: List<ProductEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedPreset by remember { mutableStateOf(ProductTxRangePreset.SEMUA) }
    var customStartMillis by remember { mutableLongStateOf(0L) }
    var customEndMillis by remember { mutableLongStateOf(0L) }
    var showDateRangeDialog by remember { mutableStateOf(false) }

    var selectedProductId by remember { mutableStateOf<Long?>(null) } // null = Semua Produk

    var selectedDetailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingReceiptTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val transactions by viewModel.allTransactions.collectAsState()

    // Hitung rentang waktu aktif
    val activeTimeRange = remember(selectedPreset, customStartMillis, customEndMillis) {
        when (selectedPreset) {
            ProductTxRangePreset.SEMUA -> Pair(0L, Long.MAX_VALUE)
            ProductTxRangePreset.HARI_INI -> DateFilterPeriod.HARI_INI.getTimeRange()
            ProductTxRangePreset.TUJUH_HARI -> DateFilterPeriod.TUJUH_HARI.getTimeRange()
            ProductTxRangePreset.TIGA_PULUH_HARI -> DateFilterPeriod.TIGA_PULUH_HARI.getTimeRange()
            ProductTxRangePreset.BULAN_INI -> DateFilterPeriod.BULAN_INI.getTimeRange()
            ProductTxRangePreset.KUSTOM -> {
                if (customStartMillis > 0L && customEndMillis >= customStartMillis) {
                    val calEnd = Calendar.getInstance().apply {
                        timeInMillis = customEndMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    Pair(customStartMillis, calEnd.timeInMillis)
                } else {
                    Pair(0L, Long.MAX_VALUE)
                }
            }
        }
    }

    // Hanya transaksi PENJUALAN produk dalam rentang waktu terpilih
    val salesInRange = remember(transactions, activeTimeRange) {
        transactions.filter { tx ->
            tx.type in listOf("PENJUALAN", "JUAL") &&
                tx.timestamp in activeTimeRange.first..activeTimeRange.second
        }
    }

    val selectedProduct = remember(products, selectedProductId) {
        products.firstOrNull { it.id == selectedProductId }
    }

    // Filter transaksi penjualan berdasarkan pilihan produk
    val filteredSalesTransactions = remember(salesInRange, selectedProduct) {
        if (selectedProduct == null) {
            salesInRange
        } else {
            salesInRange.filter { tx ->
                tx.productId == selectedProduct.id ||
                    (tx.productName?.contains(selectedProduct.name, ignoreCase = true) == true)
            }
        }
    }

    // Hitung total penjualan & unit terjual untuk pilihan produk & rentang waktu aktif
    val summaryStats = remember(filteredSalesTransactions, selectedProduct) {
        if (selectedProduct == null) {
            val totalAmt = filteredSalesTransactions.sumOf { it.amount }
            val totalQty = filteredSalesTransactions.sumOf { tx ->
                if (!tx.productName.isNullOrBlank() && tx.productName.contains(",")) {
                    tx.productName.split(", ").sumOf { part ->
                        Regex("""\((\d+)x\)""").find(part)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    }
                } else {
                    tx.quantity
                }
            }
            Pair(totalAmt, totalQty)
        } else {
            var totalAmt = 0L
            var totalQty = 0
            for (tx in filteredSalesTransactions) {
                if (tx.productId == selectedProduct.id && !tx.productName.orEmpty().contains(",")) {
                    totalQty += tx.quantity
                    totalAmt += tx.amount
                } else if (!tx.productName.isNullOrBlank()) {
                    val parts = tx.productName.split(", ")
                    for (part in parts) {
                        val cleanName = part.replace(Regex("""\s*\(\d+x\)"""), "").trim()
                        if (cleanName.equals(selectedProduct.name, ignoreCase = true)) {
                            val q = Regex("""\((\d+)x\)""").find(part)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                            totalQty += q
                            totalAmt += q * selectedProduct.sellPrice
                        }
                    }
                }
            }
            Pair(totalAmt, totalQty)
        }
    }

    if (showDateRangeDialog) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = if (customStartMillis > 0L) customStartMillis else null,
            initialSelectedEndDateMillis = if (customEndMillis > 0L) customEndMillis else null
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis ?: start
                        if (start != null && end != null) {
                            customStartMillis = start
                            customEndMillis = end
                            selectedPreset = ProductTxRangePreset.KUSTOM
                        }
                        showDateRangeDialog = false
                    }
                ) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangeDialog = false }) {
                    Text("Batal")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Pilih Rentang Waktu",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                modifier = Modifier.heightIn(max = 460.dp)
            )
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
                selectedDetailTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(selectedDetailTransaction!!)
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

    val activeRangeLabel = when (selectedPreset) {
        ProductTxRangePreset.KUSTOM -> {
            if (customStartMillis > 0L) {
                "${Formatters.date(customStartMillis)} – ${Formatters.date(customEndMillis)}"
            } else {
                "Rentang Tanggal"
            }
        }
        else -> selectedPreset.label
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Transaksi Produk", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Penjualan produk • $activeRangeLabel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_product_transactions")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. FILTER RENTANG WAKTU & PILIHAN TRANSAKSI PRODUK
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // Filter Rentang Waktu Transaksi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rentang Waktu Transaksi",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = activeRangeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ProductTxRangePreset.values()) { preset ->
                                val isSelected = selectedPreset == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (preset == ProductTxRangePreset.KUSTOM) {
                                            showDateRangeDialog = true
                                        } else {
                                            selectedPreset = preset
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (preset == ProductTxRangePreset.KUSTOM && isSelected && customStartMillis > 0L) {
                                                "${Formatters.date(customStartMillis)} - ${Formatters.date(customEndMillis)}"
                                            } else {
                                                preset.label
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (preset == ProductTxRangePreset.KUSTOM) {
                                        { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("chip_range_${preset.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pilihan Transaksi Produk
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pilihan Produk",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                val isAll = selectedProductId == null
                                FilterChip(
                                    selected = isAll,
                                    onClick = { selectedProductId = null },
                                    label = { Text("Semua Produk", fontSize = 11.sp, fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("chip_product_all")
                                )
                            }
                            items(products.filter { it.stockType == "PRODUK" }) { prod ->
                                val isSelected = selectedProductId == prod.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedProductId = prod.id },
                                    label = { Text("${prod.emoji} ${prod.name}", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("chip_product_${prod.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ringkasan Penjualan Produk
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MoneyInGreenBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (selectedProduct != null) "Penjualan ${selectedProduct.name}" else "Total Penjualan Produk",
                                        fontSize = 10.sp,
                                        color = MoneyInGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = Formatters.rupiah(summaryStats.first),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyInGreen
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Volume Terjual", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "${summaryStats.second} pcs (${filteredSalesTransactions.size} trx)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. GRAFIK PENJUALAN PRODUK
            item {
                ProductSalesTrendChartCard(
                    salesTransactions = filteredSalesTransactions,
                    selectedProduct = selectedProduct,
                    brandColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
            }

            // 3. GRAFIK PRODUK TERLARIS
            item {
                TopSellingProductsChartCard(
                    salesTransactions = salesInRange,
                    products = products,
                    selectedProductId = selectedProductId,
                    brandColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }

            // 4. RIWAYAT PENJUALAN PRODUK
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Penjualan Produk (${filteredSalesTransactions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedProduct != null) {
                        Text(
                            text = "${selectedProduct.emoji} ${selectedProduct.name}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filteredSalesTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum Ada Penjualan Produk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tidak ada penjualan produk pada filter rentang waktu atau produk yang dipilih.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredSalesTransactions) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        onClick = { selectedDetailTransaction = tx },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductSalesTrendChartCard(
    salesTransactions: List<TransactionEntity>,
    selectedProduct: ProductEntity?,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    // Hitung 7 hari terakhir (atau titik harian dari transaksi penjualan)
    val dailyData = remember(salesTransactions, selectedProduct) {
        val anchorMillis = salesTransactions.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
        (6 downTo 0).map { offset ->
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = anchorMillis
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = dayCal.timeInMillis
            val end = start + 24 * 60 * 60 * 1000L - 1
            val dayTxs = salesTransactions.filter { it.timestamp in start..end }
            val dayTotal = if (selectedProduct == null) {
                dayTxs.sumOf { it.amount }
            } else {
                var sum = 0L
                for (tx in dayTxs) {
                    if (tx.productId == selectedProduct.id && !tx.productName.orEmpty().contains(",")) {
                        sum += tx.amount
                    } else if (!tx.productName.isNullOrBlank()) {
                        for (part in tx.productName.split(", ")) {
                            val cleanName = part.replace(Regex("""\s*\(\d+x\)"""), "").trim()
                            if (cleanName.equals(selectedProduct.name, ignoreCase = true)) {
                                val q = Regex("""\((\d+)x\)""").find(part)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                                sum += q * selectedProduct.sellPrice
                            }
                        }
                    }
                }
                sum
            }
            Triple(Formatters.dayName(start), dayTotal, dayTxs.size)
        }
    }

    val totalTrendSales = dailyData.sumOf { it.second }
    val peakSale = dailyData.maxOfOrNull { it.second } ?: 0L
    val maxScale = peakSale.coerceAtLeast(100_000L)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_grafik_penjualan_produk")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik Penjualan Produk",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedProduct != null) {
                            "Tren penjualan ${selectedProduct.name}: ${Formatters.rupiah(totalTrendSales)}"
                        } else {
                            "Tren 7 hari periode aktif: ${Formatters.rupiah(totalTrendSales)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp, top = 8.dp)
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    val count = dailyData.size.coerceAtLeast(1)
                    val slotWidth = chartWidth / count
                    val barWidth = (slotWidth * 0.44f).coerceAtMost(36.dp.toPx())

                    for (i in 0..2) {
                        val y = chartHeight * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    val linePath = Path()
                    val points = mutableListOf<Offset>()

                    dailyData.forEachIndexed { index, (_, amount, _) ->
                        val ratio = if (amount > 0) (amount.toFloat() / maxScale.toFloat()).coerceIn(0.08f, 1f) else 0.04f
                        val barHeight = chartHeight * ratio
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        val top = chartHeight - barHeight
                        val isLatest = index == dailyData.lastIndex

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (isLatest) {
                                    listOf(brandColor, brandColor.copy(alpha = 0.75f))
                                } else {
                                    listOf(brandColor.copy(alpha = 0.45f), brandColor.copy(alpha = 0.22f))
                                }
                            ),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        val centerPoint = Offset(left + barWidth / 2f, top)
                        points.add(centerPoint)
                        if (index == 0) {
                            linePath.moveTo(centerPoint.x, centerPoint.y)
                        } else {
                            linePath.lineTo(centerPoint.x, centerPoint.y)
                        }
                    }

                    if (points.size > 1) {
                        drawPath(
                            path = linePath,
                            color = MoneyInGreen,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        points.forEachIndexed { idx, pt ->
                            drawCircle(
                                color = if (idx == points.lastIndex) brandColor else MoneyInGreen,
                                radius = if (idx == points.lastIndex) 4.5.dp.toPx() else 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dailyData.forEachIndexed { index, (dayLabel, _, _) ->
                        val isLatest = index == dailyData.lastIndex
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isLatest) brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terakhir: ${Formatters.rupiah(dailyData.lastOrNull()?.second ?: 0L)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = brandColor
                )
                Text(
                    text = "Puncak: ${Formatters.rupiah(peakSale)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MoneyInGreen
                )
            }
        }
    }
}

@Composable
fun TopSellingProductsChartCard(
    salesTransactions: List<TransactionEntity>,
    products: List<ProductEntity>,
    selectedProductId: Long?,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    val topProductStats = remember(salesTransactions, products) {
        val qtyMap = mutableMapOf<String, Int>()
        val revMap = mutableMapOf<String, Long>()
        val idMap = mutableMapOf<String, Long?>()

        for (tx in salesTransactions) {
            if (tx.productId != null && !tx.productName.orEmpty().contains(",")) {
                val matched = products.firstOrNull { it.id == tx.productId }
                val name = matched?.name ?: tx.productName ?: tx.category
                qtyMap[name] = (qtyMap[name] ?: 0) + tx.quantity
                revMap[name] = (revMap[name] ?: 0L) + tx.amount
                idMap[name] = matched?.id ?: tx.productId
            } else if (!tx.productName.isNullOrBlank()) {
                val parts = tx.productName.split(", ")
                for (part in parts) {
                    val qtyMatch = Regex("""\((\d+)x\)""").find(part)
                    val q = qtyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    val cleanName = part.replace(Regex("""\s*\(\d+x\)"""), "").trim()
                    val matched = products.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
                    val rev = if (matched != null) q * matched.sellPrice else tx.amount / parts.size.coerceAtLeast(1)
                    qtyMap[cleanName] = (qtyMap[cleanName] ?: 0) + q
                    revMap[cleanName] = (revMap[cleanName] ?: 0L) + rev
                    idMap[cleanName] = matched?.id
                }
            }
        }

        qtyMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (name, qty) ->
                Triple(name, qty, revMap[name] ?: 0L) to idMap[name]
            }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_grafik_produk_terlaris")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik Produk Terlaris",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Peringkat jumlah terjual & omset pada periode terpilih",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = MoneyInGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (topProductStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada penjualan produk pada rentang waktu ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxQty = (topProductStats.maxOfOrNull { it.first.second } ?: 1).coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topProductStats.forEachIndexed { idx, (triple, prodId) ->
                        val (name, qty, rev) = triple
                        val fraction = (qty.toFloat() / maxQty.toFloat()).coerceIn(0.08f, 1f)
                        val isHighlighted = selectedProductId != null && selectedProductId == prodId

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (idx == 0 || isHighlighted) brandColor
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (idx == 0 || isHighlighted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$name ($qty pcs)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isHighlighted || idx == 0) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isHighlighted) brandColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Formatters.rupiah(rev),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MoneyInGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                isHighlighted -> brandColor
                                                idx == 0 -> brandColor
                                                else -> MoneyInGreen.copy(alpha = (0.85f - idx * 0.12f).coerceAtLeast(0.35f))
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
