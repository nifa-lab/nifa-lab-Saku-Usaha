package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.PeriodPickerModal
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SakuUsahaTopHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TransactionDetailModalSheet
import com.example.ui.theme.*
import com.example.ui.util.DateFilterPeriod
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.SakuUsahaViewModel
import coil.compose.AsyncImage
import java.io.File

@Composable
fun HomeScreen(
    viewModel: SakuUsahaViewModel,
    profile: BusinessProfileEntity?,
    transactions: List<TransactionEntity>,
    products: List<ProductEntity>,
    lowStockProducts: List<ProductEntity>,
    activeDebts: List<TransactionEntity>,
    onNavigateToTransactions: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPeriod by viewModel.homeDatePeriod.collectAsState()
    var showPeriodModal by remember { mutableStateOf(false) }
    var selectedDetailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingReceiptTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // Filter transactions for chosen period
    val (start, end) = selectedPeriod.getTimeRange()
    val periodTransactions = transactions.filter { it.timestamp in start..end }

    // Accurate calculation for Modal, Penjualan, Beli, Biaya
    val totalPenjualan = periodTransactions
        .filter { it.type == "PENJUALAN" || it.type == "JUAL" }
        .sumOf { it.amount }
    val totalModal = periodTransactions
        .filter { it.type == "MODAL" }
        .sumOf { it.amount }
    val totalBeli = periodTransactions
        .filter { it.type == "BELI" }
        .sumOf { it.amount }
    val totalBiaya = periodTransactions
        .filter { it.type == "BIAYA" }
        .sumOf { it.amount }

    // Laba Bersih Operasional = Penjualan - (Beli Stok + Biaya Operasional)
    val totalExpense = totalBeli + totalBiaya
    val netProfit = totalPenjualan - totalExpense

    // Total Aliran Kas Masuk (Penjualan + Modal)
    val totalIncome = totalPenjualan + totalModal

    if (showPeriodModal) {
        PeriodPickerModal(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { viewModel.setHomeDatePeriod(it) },
            onDismissRequest = { showPeriodModal = false }
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
                onNavigateToTransactions()
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Store Header
        item {
            SakuUsahaTopHeader(
                businessName = profile?.businessName ?: "Toko Suka Maju",
                subtitle = "Modal: " + Formatters.rupiah(profile?.capitalAmount ?: 5_000_000L),
                onAppearanceClick = onNavigateToAppearance
            )
        }

        // Hero Card: Net Profit Card with customizable background
        item {
            HeroProfitCard(
                netProfit = netProfit,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                period = selectedPeriod,
                bannerPreset = profile?.bannerPreset ?: "WARUNG",
                customBannerUri = profile?.customBannerUri,
                brandColor = MaterialTheme.colorScheme.primary,
                onPeriodClick = { showPeriodModal = true },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Bingkai Persegi Panjang: Pilihan Pencatatan Transaksi + Pilihan Produk berbentuk lingkaran di bawah Penjualan
        item {
            DashboardMenuChoiceFrame(
                productCount = products.count { it.stockType == "PRODUK" },
                onQuickRecord = { type -> viewModel.openRecordModal(type) },
                onNavigateToProducts = onNavigateToProducts,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Warning Alerts (Stok Menipis & Piutang)
        if (lowStockProducts.isNotEmpty() || activeDebts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lowStockProducts.isNotEmpty()) {
                        AlertBanner(
                            icon = Icons.Default.WarningAmber,
                            title = "${lowStockProducts.size} Produk Stok Menipis",
                            description = "Contoh: ${lowStockProducts.first().name} sisa ${lowStockProducts.first().stock} pcs. Segera belanja!",
                            actionText = "Lihat Stok",
                            actionColor = WarningYellow,
                            onClick = onNavigateToStock,
                            modifier = Modifier.testTag("alert_low_stock")
                        )
                    }

                    if (activeDebts.isNotEmpty()) {
                        val pendingCount = activeDebts.size
                        val totalPending = activeDebts.sumOf { it.remainingDebt }
                        AlertBanner(
                            icon = Icons.Default.NotificationsActive,
                            title = "$pendingCount Tagihan / Piutang Aktif",
                            description = "Total tagihan belum selesai: ${Formatters.rupiah(totalPending)}",
                            actionText = "Kelola Utang",
                            actionColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToDebts,
                            modifier = Modifier.testTag("alert_debts")
                        )
                    }
                }
            }
        }

        // 7-Day Sales Trend Bar Chart
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SalesBarChartCard(
                transactions = transactions,
                brandColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Recent Transactions Section Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onNavigateToTransactions,
                    modifier = Modifier.testTag("btn_see_all_transactions")
                ) {
                    Text(
                        text = "Lihat Semua",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Recent Transactions List
        val displayRecent = transactions.take(4)
        if (displayRecent.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum Ada Transaksi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Catat penjualan atau belanja pertama Anda dengan tombol di bawah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(displayRecent) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    onClick = { selectedDetailTransaction = tx },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun HeroProfitCard(
    netProfit: Long,
    totalIncome: Long,
    totalExpense: Long,
    period: DateFilterPeriod,
    bannerPreset: String,
    customBannerUri: String? = null,
    brandColor: Color,
    onPeriodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background: Custom user uploaded image, Warung illustration image or styled gradient
            if (bannerPreset == "CUSTOM" && !customBannerUri.isNullOrEmpty() && File(customBannerUri).exists()) {
                AsyncImage(
                    model = File(customBannerUri),
                    contentDescription = "Gambar Kartu Kustom",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (bannerPreset == "WARUNG") {
                Image(
                    painter = painterResource(id = R.drawable.banner_warung_modern),
                    contentDescription = "Warung SakuUsaha",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BrandDarkGreen, brandColor, BrandDarkGreen)
                            )
                        )
                )
            }

            // Dark readability overlay (around 34% per design spec 2.6)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.70f)
                            )
                        )
                    )
            )

            // Card Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Period Selector Chip & Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Laba Bersih",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onPeriodClick)
                            .testTag("btn_period_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Main Profit Value
                Column {
                    Text(
                        text = Formatters.rupiah(netProfit),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (netProfit >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (netProfit >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (netProfit >= 0) "+12% dibanding periode lalu" else "-5% dibanding periode lalu",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Income & Expense Sub-Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Uang Masuk Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SouthWest,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Uang Masuk",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = Formatters.rupiah(totalIncome),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Uang Keluar Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NorthEast,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Uang Keluar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = Formatters.rupiah(totalExpense),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlertBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionText: String,
    actionColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = actionColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = actionColor
                )
            }
        }
    }
}

@Composable
fun SalesBarChartCard(
    transactions: List<TransactionEntity>,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Penjualan 7 Hari Terakhir",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hari ini berbeda warna",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7 Days Bar chart calculation
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            val daySales = (6 downTo 0).map { dayOffset ->
                val dayStart = now - dayOffset * dayMillis
                val dayName = Formatters.dayName(dayStart)
                val isToday = dayOffset == 0
                val total = transactions
                    .filter { it.direction == "MASUK" && Formatters.isToday(it.timestamp + dayOffset * dayMillis) }
                    .sumOf { it.amount }
                Triple(dayName, total.coerceAtLeast(100_000L), isToday)
            }

            val maxSale = (daySales.maxOfOrNull { it.second } ?: 1_000_000L).coerceAtLeast(100_000L)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                daySales.forEach { (name, amount, isToday) ->
                    val fraction = (amount.toFloat() / maxSale.toFloat()).coerceIn(0.12f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isToday) brandColor else brandColor.copy(alpha = 0.35f)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isToday) brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: TransactionEntity,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val isIncome = transaction.direction == "MASUK"
            val iconBg = if (isIncome) MoneyInGreenBg else MoneyOutRedBg
            val iconColor = if (isIncome) MoneyInGreen else MoneyOutRed

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        "JUAL", "PENJUALAN" -> Icons.Default.TrendingUp
                        "MODAL" -> Icons.Default.Savings
                        "BELI" -> Icons.Default.Inventory2
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.productName ?: transaction.category,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.receiptFileName != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Ada Bukti",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.date(transaction.timestamp) + " • " + transaction.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount and Badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+ " else "- ") + Formatters.rupiah(transaction.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = iconColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(
                    text = if (transaction.paymentMethod == "HUTANG") "Utang" else (if (isIncome) "Masuk" else "Keluar"),
                    containerColor = if (transaction.paymentMethod == "HUTANG") WarningYellowBg else iconBg,
                    contentColor = if (transaction.paymentMethod == "HUTANG") WarningYellow else iconColor
                )
            }
        }
    }
}

@Composable
fun DashboardMenuChoiceFrame(
    productCount: Int,
    onQuickRecord: (String) -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("frame_menu_pilihan")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Baris 1: Pencatatan Transaksi (Penjualan, Modal, Beli Stok, Biaya)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickActionButton(
                    title = "Penjualan",
                    subtitle = "Jual Produk",
                    icon = Icons.Default.TrendingUp,
                    color = MoneyInGreen,
                    onClick = { onQuickRecord("PENJUALAN") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_penjualan")
                )
                QuickActionButton(
                    title = "Modal",
                    subtitle = "Tambah Kas",
                    icon = Icons.Default.Savings,
                    color = Color(0xFF0F766E),
                    onClick = { onQuickRecord("MODAL") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_modal")
                )
                QuickActionButton(
                    title = "Beli Stok",
                    subtitle = "Restok",
                    icon = Icons.Default.ShoppingBag,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onQuickRecord("BELI") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_beli")
                )
                QuickActionButton(
                    title = "Biaya",
                    subtitle = "Beban Toko",
                    icon = Icons.Default.Receipt,
                    color = MoneyOutRed,
                    onClick = { onQuickRecord("BIAYA") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_biaya")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Baris 2: Pilihan Produk berbentuk lingkaran dengan ikon tepat di bawah Penjualan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToProducts)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_shortcut_products")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Produk",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Produk",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$productCount produk",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(3f))
            }
        }
    }
}
