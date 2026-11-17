package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.SakuUsahaViewModel

@Composable
fun StockProdukScreen(
    viewModel: SakuUsahaViewModel,
    allProducts: List<ProductEntity>,
    onNavigateToCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    StockCategoryDetailPage(
        viewModel = viewModel,
        allProducts = allProducts,
        stockType = "PRODUK",
        title = "Stok Produk",
        emptyMessage = "Belum ada produk terdaftar. Gunakan menu Tambah Stok di atas untuk menambah produk atau mengisi stok.",
        emptyIcon = Icons.Default.Storefront,
        accentColor = MaterialTheme.colorScheme.primary,
        onNavigateToCategories = onNavigateToCategories,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun StockBahanScreen(
    viewModel: SakuUsahaViewModel,
    allProducts: List<ProductEntity>,
    onNavigateToCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    StockCategoryDetailPage(
        viewModel = viewModel,
        allProducts = allProducts,
        stockType = "BAHAN",
        title = "Stok Bahan",
        emptyMessage = "Belum ada bahan baku terdaftar. Gunakan menu Tambah Stok di atas untuk mencatat bahan baku usaha Anda.",
        emptyIcon = Icons.Default.Kitchen,
        accentColor = MaterialTheme.colorScheme.secondary,
        onNavigateToCategories = onNavigateToCategories,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun StockInventarisScreen(
    viewModel: SakuUsahaViewModel,
    allProducts: List<ProductEntity>,
    onNavigateToCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    StockCategoryDetailPage(
        viewModel = viewModel,
        allProducts = allProducts,
        stockType = "INVENTARIS",
        title = "Stok Inventaris",
        emptyMessage = "Belum ada data inventaris & peralatan. Gunakan menu Tambah Stok di atas untuk mencatat perlengkapan usaha.",
        emptyIcon = Icons.Default.HomeRepairService,
        accentColor = MaterialTheme.colorScheme.tertiary,
        onNavigateToCategories = onNavigateToCategories,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockCategoryDetailPage(
    viewModel: SakuUsahaViewModel,
    allProducts: List<ProductEntity>,
    stockType: String,
    title: String,
    emptyMessage: String,
    emptyIcon: ImageVector,
    accentColor: Color,
    onNavigateToCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val itemsOfType = remember(allProducts, stockType) {
        allProducts.filter { it.stockType == stockType }
    }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("SEMUA") } // "SEMUA", "MENIPIS", "AMAN"

    var showAddStockMenu by remember { mutableStateOf(false) }
    var showCreateNewDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var restockTarget by remember { mutableStateOf<ProductEntity?>(null) }

    val stockCategories by viewModel.categoriesByType("STOK").collectAsState(initial = emptyList())

    val filteredItems = remember(itemsOfType, searchQuery, statusFilter) {
        itemsOfType.filter { prod ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.categoryName.contains(searchQuery, ignoreCase = true)
            }
            val matchesStatus = when (statusFilter) {
                "MENIPIS" -> prod.stock <= prod.minStockAlert
                "AMAN" -> prod.stock > prod.minStockAlert
                else -> true
            }
            matchesSearch && matchesStatus
        }.sortedWith(compareBy<ProductEntity> { it.stock > it.minStockAlert }.thenBy { it.name.lowercase() })
    }

    val safeCount = itemsOfType.count { it.stock > it.minStockAlert }
    val lowCount = itemsOfType.count { it.stock <= it.minStockAlert }

    if (showAddStockMenu) {
        AddStockMenuDialog(
            stockType = stockType,
            itemsOfType = itemsOfType,
            onConfirmRestock = { itemId, qtyToAdd, unitCost ->
                viewModel.restockItem(itemId, qtyToAdd, unitCost)
                showAddStockMenu = false
            },
            onCreateNewItem = { showCreateNewDialog = true },
            onDismiss = { showAddStockMenu = false }
        )
    }

    if (showCreateNewDialog) {
        ProductFormDialog(
            product = null,
            allProducts = allProducts,
            categories = stockCategories.filter { it.name != "Semua" },
            defaultStockType = stockType,
            onSave = { newItem ->
                viewModel.saveProduct(newItem)
                showCreateNewDialog = false
            },
            onDelete = {},
            onDismiss = { showCreateNewDialog = false }
        )
    }

    if (itemToEdit != null) {
        ProductFormDialog(
            product = itemToEdit,
            allProducts = allProducts,
            categories = stockCategories.filter { it.name != "Semua" },
            defaultStockType = stockType,
            onSave = { updated ->
                viewModel.saveProduct(updated)
                itemToEdit = null
            },
            onDelete = { toDel ->
                viewModel.deleteProduct(toDel)
                itemToEdit = null
            },
            onDismiss = { itemToEdit = null }
        )
    }

    if (restockTarget != null) {
        QuickRestockDialog(
            item = restockTarget!!,
            onConfirmRestock = { qtyToAdd, newCost ->
                viewModel.restockItem(restockTarget!!.id, qtyToAdd, newCost)
                restockTarget = null
            },
            onDismiss = { restockTarget = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${itemsOfType.size} item • $safeCount aman • $lowCount menipis",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_stock_${stockType.lowercase()}")
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
                        modifier = Modifier.testTag("btn_categories_stock_${stockType.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Kelola Kategori",
                            tint = accentColor
                        )
                    }
                    AiAssistantTopActionButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddStockMenu = true },
                containerColor = accentColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_stock_${stockType.lowercase()}")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Stok")
            }
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
            // Header Summary + Menu Tambah Stok
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // KPI Row
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
                                Text("Total Item", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${itemsOfType.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MoneyInGreenBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Stok Aman", fontSize = 10.sp, color = MoneyInGreen)
                                Text("$safeCount", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MoneyInGreen)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (lowCount > 0) WarningYellowBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Menipis / Habis", fontSize = 10.sp, color = if (lowCount > 0) WarningYellow else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$lowCount", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (lowCount > 0) WarningYellow else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Menu Tambah Stok (Primary Action Row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAddStockMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("btn_menu_add_stock_${stockType.lowercase()}")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Stok", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { showCreateNewDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("btn_new_item_${stockType.lowercase()}")
                        ) {
                            Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Item Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari ${stockTypeLabel(stockType).lowercase()} atau kategori...") },
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
                            .testTag("input_search_stock_${stockType.lowercase()}")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filterChips = listOf(
                        "SEMUA" to "Semua (${itemsOfType.size})",
                        "MENIPIS" to "Menipis/Habis ($lowCount)",
                        "AMAN" to "Aman ($safeCount)"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filterChips) { (key, label) ->
                            val isSelected = statusFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { statusFilter = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = emptyIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada $title",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { prod ->
                    StockDetailItemCard(
                        item = prod,
                        accentColor = accentColor,
                        onIncrement = { viewModel.adjustStock(prod.id, 1) },
                        onDecrement = { if (prod.stock > 0) viewModel.adjustStock(prod.id, -1) },
                        onQuickRestock = { restockTarget = prod },
                        onClick = { itemToEdit = prod },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StockDetailItemCard(
    item: ProductEntity,
    accentColor: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuickRestock: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowStock = item.stock <= item.minStockAlert
    val isEmpty = item.stock <= 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(item.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.isComposedProduct) {
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(
                                    text = "Racikan",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = "${item.categoryName} • Batas min: ${item.minStockAlert} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    text = if (isEmpty) "Habis" else if (isLowStock) "Menipis" else "Aman",
                    containerColor = if (isEmpty) MoneyOutRedBg else if (isLowStock) WarningYellowBg else MoneyInGreenBg,
                    contentColor = if (isEmpty) MoneyOutRed else if (isLowStock) WarningYellow else MoneyInGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (item.stockType) {
                            "BAHAN" -> "Modal / ${item.unit}"
                            "INVENTARIS" -> "Nilai / ${item.unit}"
                            else -> "HPP / Jual"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (item.stockType == "PRODUK") {
                            "${Formatters.rupiah(item.costPrice)} / ${Formatters.rupiah(item.sellPrice)}"
                        } else {
                            Formatters.rupiah(item.costPrice)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick restock button
                    FilledTonalButton(
                        onClick = onQuickRestock,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_card_restock_${item.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Stok", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Stepper - / qty / +
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            enabled = item.stock > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "${item.stock} ${item.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
