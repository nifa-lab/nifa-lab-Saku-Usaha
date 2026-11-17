package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: SakuUsahaViewModel,
    products: List<ProductEntity>,
    onOpenProductList: () -> Unit,
    onOpenHppRecipe: () -> Unit,
    onOpenProductTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDetailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingReceiptTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val transactions by viewModel.allTransactions.collectAsState()
    val stockCategories by viewModel.categoriesByType("STOK").collectAsState(initial = emptyList())

    if (showAddDialog) {
        ProductFormDialog(
            product = null,
            allProducts = products,
            categories = stockCategories.filter { it.name != "Semua" },
            onSave = { newProd ->
                viewModel.saveProduct(newProd)
                showAddDialog = false
            },
            onDelete = {},
            onDismiss = { showAddDialog = false }
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

    val productItems = remember(products) { products.filter { it.stockType == "PRODUK" } }
    val safeStockCount = productItems.count { it.stock > it.minStockAlert }
    val lowStockCount = productItems.count { it.stock <= it.minStockAlert }
    val totalUnitsSold = transactions.filter { it.type in listOf("PENJUALAN", "JUAL") }.sumOf { it.quantity }
    val recentTransactions = transactions.take(6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Produk", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${productItems.size} produk • $totalUnitsSold pcs terjual",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_products")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToCategories,
                        modifier = Modifier.testTag("btn_products_categories")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Kelola Kategori",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // =========================================================================
            // BAGIAN ATAS: 3 Pilihan Menu Lingkaran + Tombol Persegi Panjang Tambah Produk
            // =========================================================================
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        // 3 Pilihan Berbentuk Lingkaran
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProductOptionItem(
                                label = "Daftar Produk",
                                icon = Icons.Default.Storefront,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = onOpenProductList,
                                testTag = "btn_circle_daftar_produk",
                                modifier = Modifier.weight(1f)
                            )
                            CircularProductOptionItem(
                                label = "HPP & Resep",
                                icon = Icons.Default.Calculate,
                                accentColor = Color(0xFF0D9488),
                                onClick = onOpenHppRecipe,
                                testTag = "btn_circle_hpp_resep",
                                modifier = Modifier.weight(1f)
                            )
                            CircularProductOptionItem(
                                label = "Transaksi",
                                icon = Icons.Default.ReceiptLong,
                                accentColor = MoneyInGreen,
                                onClick = onOpenProductTransactions,
                                testTag = "btn_circle_transaksi_produk",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pilihan Tambah Produk Berbentuk Persegi Panjang
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("btn_add_product_rect")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBox,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tambah Produk",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ringkasan KPI Produk
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Total Produk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${productItems.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MoneyInGreenBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Stok Aman", fontSize = 10.sp, color = MoneyInGreen)
                                    Text("$safeStockCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MoneyInGreen)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (lowStockCount > 0) WarningYellowBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Perlu Restok", fontSize = 10.sp, color = if (lowStockCount > 0) WarningYellow else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$lowStockCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (lowStockCount > 0) WarningYellow else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // RIWAYAT TRANSAKSI TERAKHIR
            // =========================================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Transaksi Terakhir",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onOpenProductTransactions,
                        modifier = Modifier.testTag("btn_products_see_all_tx")
                    ) {
                        Text(
                            text = "Lihat Semua",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                text = "Riwayat penjualan dan pembelian stok terbaru akan tampil di sini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentTransactions) { tx ->
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
private fun CircularProductOptionItem(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.14f),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.45f)),
            modifier = Modifier
                .size(60.dp)
                .minimumInteractiveComponentSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
