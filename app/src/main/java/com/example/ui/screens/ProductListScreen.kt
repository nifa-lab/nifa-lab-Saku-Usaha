package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: SakuUsahaViewModel,
    products: List<ProductEntity>,
    onNavigateToCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedSortOrder by remember { mutableStateOf(StockSortOrder.TERLARIS) }

    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    val transactions by viewModel.allTransactions.collectAsState()
    val stockCategories by viewModel.categoriesByType("STOK").collectAsState(initial = emptyList())

    val productItems = remember(products) { products.filter { it.stockType == "PRODUK" } }

    val salesStatsMap = remember(transactions, productItems) {
        val qtyMap = mutableMapOf<Long, Int>()
        val revMap = mutableMapOf<Long, Long>()
        val salesTx = transactions.filter { it.type in listOf("PENJUALAN", "JUAL") }

        for (tx in salesTx) {
            if (tx.productId != null && !tx.productName.orEmpty().contains(",")) {
                qtyMap[tx.productId] = (qtyMap[tx.productId] ?: 0) + tx.quantity
                revMap[tx.productId] = (revMap[tx.productId] ?: 0L) + tx.amount
            } else if (!tx.productName.isNullOrBlank()) {
                val parts = tx.productName.split(", ")
                for (part in parts) {
                    val qtyMatch = Regex("""\((\d+)x\)""").find(part)
                    val q = qtyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    val cleanName = part.replace(Regex("""\s*\(\d+x\)"""), "").trim()
                    val matched = productItems.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
                    if (matched != null) {
                        qtyMap[matched.id] = (qtyMap[matched.id] ?: 0) + q
                        revMap[matched.id] = (revMap[matched.id] ?: 0L) + (q * matched.sellPrice)
                    }
                }
            }
        }
        Pair(qtyMap, revMap)
    }

    val filteredAndSortedProducts = remember(productItems, searchQuery, selectedCategory, selectedSortOrder, salesStatsMap) {
        val filtered = productItems.filter { prod ->
            val matchesCategory = selectedCategory == "Semua" || prod.categoryName.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                prod.name.contains(searchQuery, ignoreCase = true) ||
                        prod.categoryName.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }

        when (selectedSortOrder) {
            StockSortOrder.TERLARIS -> filtered.sortedByDescending { salesStatsMap.first[it.id] ?: 0 }
            StockSortOrder.OMSET_TERTINGGI -> filtered.sortedByDescending { salesStatsMap.second[it.id] ?: 0L }
            StockSortOrder.STOK_BANYAK -> filtered.sortedByDescending { it.stock }
            StockSortOrder.STOK_SEDIKIT -> filtered.sortedBy { it.stock }
            StockSortOrder.NAMA_AZ -> filtered.sortedBy { it.name.lowercase() }
        }
    }

    if (productToEdit != null) {
        ProductFormDialog(
            product = productToEdit,
            allProducts = products,
            categories = stockCategories.filter { it.name != "Semua" },
            onSave = { updated ->
                viewModel.saveProduct(updated)
                productToEdit = null
            },
            onDelete = { toDel ->
                viewModel.deleteProduct(toDel)
                productToEdit = null
            },
            onDismiss = { productToEdit = null }
        )
    }

    val safeStockCount = productItems.count { it.stock > it.minStockAlert }
    val lowStockCount = productItems.count { it.stock <= it.minStockAlert }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Daftar Produk", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${productItems.size} produk • $safeStockCount aman • $lowStockCount menipis",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_product_list")
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
                        modifier = Modifier.testTag("btn_product_list_categories")
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
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama produk atau kategori...") },
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
                            .testTag("input_search_product_list")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Urutkan:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StockSortOrder.values()) { order ->
                            val isSelected = selectedSortOrder == order
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSortOrder = order },
                                label = { Text(order.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val chipList = listOf("Semua") + stockCategories.map { it.name }.filter { it != "Semua" }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chipList) { catName ->
                            val isSelected = selectedCategory.equals(catName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = catName },
                                label = { Text(catName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (filteredAndSortedProducts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Produk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tambahkan produk baru melalui tombol Tambah Produk di halaman Produk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredAndSortedProducts) { prod ->
                    val soldQty = salesStatsMap.first[prod.id] ?: 0
                    val soldRevenue = salesStatsMap.second[prod.id] ?: 0L

                    DetailedStockProductCard(
                        product = prod,
                        soldQty = soldQty,
                        soldRevenue = soldRevenue,
                        onIncrement = { viewModel.adjustStock(prod.id, 1) },
                        onDecrement = { if (prod.stock > 0) viewModel.adjustStock(prod.id, -1) },
                        onClick = { productToEdit = prod },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
