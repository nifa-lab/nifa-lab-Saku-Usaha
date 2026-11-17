package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.ui.viewmodel.SakuUsahaViewModel

@Composable
fun StockScreen(
    viewModel: SakuUsahaViewModel,
    products: List<ProductEntity>,
    onOpenStockProduk: () -> Unit,
    onOpenStockBahan: () -> Unit,
    onOpenStockInventaris: () -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lowStockList by viewModel.lowStockProducts.collectAsState()
    var lowStockTypeFilter by remember { mutableStateOf("SEMUA") } // "SEMUA", "PRODUK", "BAHAN", "INVENTARIS"
    var restockTarget by remember { mutableStateOf<ProductEntity?>(null) }

    val produkCount = remember(products) { products.count { it.stockType == "PRODUK" } }
    val bahanCount = remember(products) { products.count { it.stockType == "BAHAN" } }
    val inventarisCount = remember(products) { products.count { it.stockType == "INVENTARIS" } }

    val filteredLowStock = remember(lowStockList, lowStockTypeFilter) {
        if (lowStockTypeFilter == "SEMUA") lowStockList
        else lowStockList.filter { it.stockType == lowStockTypeFilter }
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // =====================================================================
        // BAGIAN ATAS: Header & 3 Pilihan Lingkaran + Ikon (Produk, Bahan, Inventaris)
        // =====================================================================
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stok",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$produkCount produk • $bahanCount bahan • $inventarisCount inventaris",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onNavigateToCategories,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_stock_categories")
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kategori", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            AiAssistantTopActionButton()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Pilihan Berbentuk Lingkaran dan Ikon: Produk, Bahan, Inventaris
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularStockOptionItem(
                            label = "Produk",
                            countText = "$produkCount item",
                            icon = Icons.Default.Storefront,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = onOpenStockProduk,
                            testTag = "btn_circle_stock_produk",
                            modifier = Modifier.weight(1f)
                        )
                        CircularStockOptionItem(
                            label = "Bahan",
                            countText = "$bahanCount item",
                            icon = Icons.Default.Kitchen,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = onOpenStockBahan,
                            testTag = "btn_circle_stock_bahan",
                            modifier = Modifier.weight(1f)
                        )
                        CircularStockOptionItem(
                            label = "Inventaris",
                            countText = "$inventarisCount item",
                            icon = Icons.Default.HomeRepairService,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = onOpenStockInventaris,
                            testTag = "btn_circle_stock_inventaris",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // BAGIAN BAWAH: Daftar Stok yang Sedang Menipis
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (lowStockList.isNotEmpty()) WarningYellow else MoneyInGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stok yang Sedang Menipis",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StatusBadge(
                    text = "${lowStockList.size} item",
                    containerColor = if (lowStockList.isNotEmpty()) WarningYellowBg else MoneyInGreenBg,
                    contentColor = if (lowStockList.isNotEmpty()) WarningYellow else MoneyInGreen
                )
            }

            if (lowStockList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val filterOptions = listOf(
                    "SEMUA" to "Semua (${lowStockList.size})",
                    "PRODUK" to "Produk (${lowStockList.count { it.stockType == "PRODUK" }})",
                    "BAHAN" to "Bahan (${lowStockList.count { it.stockType == "BAHAN" }})",
                    "INVENTARIS" to "Inventaris (${lowStockList.count { it.stockType == "INVENTARIS" }})"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { (key, title) ->
                        val isSelected = lowStockTypeFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { lowStockTypeFilter = key },
                            label = { Text(title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredLowStock.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MoneyInGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (lowStockList.isEmpty()) "Semua Stok Aman" else "Tidak Ada Stok Menipis di Kategori Ini",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seluruh stok produk, bahan baku, dan inventaris masih di atas batas minimum.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredLowStock, key = { it.id }) { item ->
                val isEmpty = item.stock <= 0
                val typeLabel = stockTypeLabel(item.stockType)
                val typeBg = when (item.stockType) {
                    "BAHAN" -> MaterialTheme.colorScheme.secondaryContainer
                    "INVENTARIS" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val typeFg = when (item.stockType) {
                    "BAHAN" -> MaterialTheme.colorScheme.onSecondaryContainer
                    "INVENTARIS" -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        1.dp,
                        if (isEmpty) MoneyOutRed.copy(alpha = 0.3f) else WarningYellow.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            when (item.stockType) {
                                "BAHAN" -> onOpenStockBahan()
                                "INVENTARIS" -> onOpenStockInventaris()
                                else -> onOpenStockProduk()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(item.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StatusBadge(
                                        text = typeLabel,
                                        containerColor = typeBg,
                                        contentColor = typeFg
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(
                                        text = if (isEmpty) "Habis" else "Menipis",
                                        containerColor = if (isEmpty) MoneyOutRedBg else WarningYellowBg,
                                        contentColor = if (isEmpty) MoneyOutRed else WarningYellow
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sisa: ${item.stock} ${item.unit} • Min: ${item.minStockAlert} ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = { restockTarget = item },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_quick_restock_${item.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restok", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularStockOptionItem(
    label: String,
    countText: String,
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
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.14f),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.45f)),
            modifier = Modifier
                .size(62.dp)
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = countText,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
