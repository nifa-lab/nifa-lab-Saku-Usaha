package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.data.model.ProductIngredient
import com.example.data.model.RecipeHelper
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.util.Formatters

enum class StockSortOrder(val label: String) {
    TERLARIS("🔥 Terlaris"),
    OMSET_TERTINGGI("💰 Omset Tertinggi"),
    STOK_BANYAK("📦 Stok Terbanyak"),
    STOK_SEDIKIT("⚠️ Stok Terendah"),
    NAMA_AZ("🔤 Nama A-Z")
}

fun stockTypeLabel(stockType: String): String = when (stockType) {
    "BAHAN" -> "Bahan"
    "INVENTARIS" -> "Inventaris"
    else -> "Produk"
}

// =============================================================================
// KARTU PRODUK STOK DENGAN RINCIAN HPP, MARGIN, DAN PENJUALAN
// =============================================================================
@Composable
fun DetailedStockProductCard(
    product: ProductEntity,
    soldQty: Int,
    soldRevenue: Long,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val marginRp = product.sellPrice - product.costPrice
    val marginPercent = if (product.sellPrice > 0) ((marginRp.toFloat() / product.sellPrice.toFloat()) * 100).toInt() else 0
    val isLowStock = product.stock <= product.minStockAlert

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
                    Text(product.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (product.isComposedProduct) {
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(
                                    text = "Racikan",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = product.categoryName + if (soldQty > 0) " • 🔥 $soldQty terjual (${Formatters.rupiah(soldRevenue)})" else " • Belum terjual",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (soldQty > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    text = if (product.stock <= 0) "Habis" else if (isLowStock) "Menipis" else "Aman",
                    containerColor = if (product.stock <= 0) MoneyOutRedBg else if (isLowStock) WarningYellowBg else MoneyInGreenBg,
                    contentColor = if (product.stock <= 0) MoneyOutRed else if (isLowStock) WarningYellow else MoneyInGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Side-by-side transparent pricing: Harga Jual, HPP Modal, Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Harga Jual", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.rupiah(product.sellPrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("HPP Modal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.rupiah(product.costPrice), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    Text("Margin", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${Formatters.rupiah(marginRp)} (+$marginPercent%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneyInGreen
                    )
                }

                // Quick stock counter buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        enabled = product.stock > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = "${product.stock}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// =============================================================================
// DIALOG TAMBAH STOK CEPAT (RESTOK ITEM TERPILIH)
// =============================================================================
@Composable
fun QuickRestockDialog(
    item: ProductEntity,
    onConfirmRestock: (quantityToAdd: Int, purchasePricePerUnit: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyText by remember { mutableStateOf("5") }
    var costText by remember { mutableStateOf(if (item.costPrice > 0L) item.costPrice.toString() else "") }
    val addQty = qtyText.toIntOrNull() ?: 0
    val newCost = costText.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.emoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Tambah Stok ${stockTypeLabel(item.stockType)}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(item.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Stok Saat Ini", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${item.stock} ${item.unit}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Stok Baru", fontSize = 11.sp, color = MoneyInGreen)
                            Text(
                                text = "${item.stock + addQty.coerceAtLeast(0)} ${item.unit}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MoneyInGreen
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) qtyText = it },
                    label = { Text("Jumlah Tambah Stok (${item.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_restock_qty")
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) costText = it },
                    label = {
                        Text(
                            when (item.stockType) {
                                "INVENTARIS" -> "Nilai / Harga Beli per ${item.unit} (Opsional)"
                                else -> "Harga Beli per ${item.unit} (Update HPP Otomatis)"
                            }
                        )
                    },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_restock_cost")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (addQty > 0) {
                        onConfirmRestock(addQty, newCost)
                    }
                },
                enabled = addQty > 0,
                modifier = Modifier.testTag("btn_confirm_quick_restock")
            ) {
                Text("Simpan Stok")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// =============================================================================
// MENU TAMBAH STOK: PILIH RESTOK ITEM YANG ADA ATAU TAMBAH ITEM BARU
// =============================================================================
@Composable
fun AddStockMenuDialog(
    stockType: String,
    itemsOfType: List<ProductEntity>,
    onConfirmRestock: (itemId: Long, quantityToAdd: Int, purchasePricePerUnit: Long?) -> Unit,
    onCreateNewItem: () -> Unit,
    onDismiss: () -> Unit
) {
    val label = stockTypeLabel(stockType)
    val sortedItems = remember(itemsOfType) {
        itemsOfType.sortedWith(compareBy<ProductEntity> { it.stock > it.minStockAlert }.thenBy { it.stock })
    }
    var selectedItem by remember(sortedItems) { mutableStateOf(sortedItems.firstOrNull()) }
    var qtyText by remember { mutableStateOf("5") }
    var priceText by remember(selectedItem?.id) {
        mutableStateOf(
            selectedItem?.costPrice?.takeIf { it > 0L }?.toString() ?: ""
        )
    }

    val addQty = qtyText.toIntOrNull() ?: 0
    val unitPrice = priceText.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Menu Tambah Stok $label",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Opsi 1: Buat Item Baru
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onCreateNewItem()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_add_new_item_from_menu")
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah Item $label Baru", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Atau Tambah Stok Item yang Sudah Ada:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (sortedItems.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada data $label. Gunakan tombol di atas untuk menambah item baru.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sortedItems.forEach { prod ->
                            val isSelected = selectedItem?.id == prod.id
                            val isLow = prod.stock <= prod.minStockAlert
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedItem = prod
                                        priceText = if (prod.costPrice > 0L) prod.costPrice.toString() else ""
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(prod.emoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = prod.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Stok: ${prod.stock} ${prod.unit}",
                                                fontSize = 11.sp,
                                                color = if (isLow) WarningYellow else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isLow) {
                                        StatusBadge(
                                            text = if (prod.stock <= 0) "Habis" else "Menipis",
                                            containerColor = if (prod.stock <= 0) MoneyOutRedBg else WarningYellowBg,
                                            contentColor = if (prod.stock <= 0) MoneyOutRed else WarningYellow
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedItem != null) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) qtyText = it },
                            label = { Text("Jumlah Tambah (${selectedItem!!.unit})") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_menu_restock_qty")
                        )

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) priceText = it },
                            label = { Text("Harga Beli Satuan (Opsional)") },
                            prefix = { Text("Rp ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_menu_restock_cost")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedItem
                    if (item != null && addQty > 0) {
                        onConfirmRestock(item.id, addQty, unitPrice)
                    }
                },
                enabled = selectedItem != null && addQty > 0,
                modifier = Modifier.testTag("btn_confirm_add_stock_menu")
            ) {
                Text("Tambah Stok")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

// =============================================================================
// FORM INPUT PRODUK / BAHAN / INVENTARIS: DENGAN KOMPOSISI RESEP & KALKULATOR HPP
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: ProductEntity?,
    allProducts: List<ProductEntity>,
    categories: List<CategoryEntity>,
    defaultStockType: String = "PRODUK",
    onSave: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveStockType = product?.stockType ?: defaultStockType
    val isProductType = effectiveStockType == "PRODUK"
    val defaultEmoji = when (effectiveStockType) {
        "BAHAN" -> "🧂"
        "INVENTARIS" -> "🛠️"
        else -> "📦"
    }
    val defaultUnit = when (effectiveStockType) {
        "BAHAN" -> "kg"
        "INVENTARIS" -> "unit"
        else -> "pcs"
    }

    var name by remember { mutableStateOf(product?.name ?: "") }
    var categoryName by remember { mutableStateOf(product?.categoryName ?: (categories.firstOrNull()?.name ?: "Lainnya")) }
    var unit by remember { mutableStateOf(product?.unit ?: defaultUnit) }
    var selectedEmoji by remember { mutableStateOf(product?.emoji ?: defaultEmoji) }

    // Toggle: Barang Jadi vs Produk Racikan Resep (hanya relevan untuk PRODUK)
    var isComposed by remember { mutableStateOf(if (isProductType) (product?.isComposedProduct ?: false) else false) }

    // Retail & Cost fields
    var sellPriceText by remember { mutableStateOf(product?.sellPrice?.toString() ?: "") }
    var standaloneCostPriceText by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var stockText by remember { mutableStateOf(product?.stock?.toString() ?: if (effectiveStockType == "INVENTARIS") "1" else "10") }
    var minStockText by remember { mutableStateOf(product?.minStockAlert?.toString() ?: if (effectiveStockType == "INVENTARIS") "1" else "5") }

    // Recipe Composition fields
    var yieldQuantityText by remember { mutableStateOf(product?.yieldQuantity?.toString() ?: "1") }
    var ingredientsList by remember {
        mutableStateOf(
            if (product != null && product.isComposedProduct) {
                RecipeHelper.parseIngredients(product.recipeJson)
            } else emptyList()
        )
    }

    var showAddIngredientModal by remember { mutableStateOf(false) }

    // Live calculation for composed HPP
    val yieldQty = yieldQuantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val calculatedRecipeTotalCost = ingredientsList.sumOf { (it.quantityNeeded * it.unitCost).toLong() }
    val calculatedUnitHpp = if (yieldQty > 0) calculatedRecipeTotalCost / yieldQty else 0L

    val effectiveHpp = if (isComposed) calculatedUnitHpp else (standaloneCostPriceText.toLongOrNull() ?: 0L)
    val sellPrice = if (isProductType) (sellPriceText.toLongOrNull() ?: 0L) else 0L
    val marginRp = sellPrice - effectiveHpp
    val marginPercent = if (sellPrice > 0) ((marginRp.toFloat() / sellPrice.toFloat()) * 100).toInt() else 0

    val emojis = when (effectiveStockType) {
        "BAHAN" -> listOf("🧂", "🍃", "🥛", "🥚", "🧈", "🍯", "☕", "🍫", "🌾", "🍋", "🧊", "🥤", "📦")
        "INVENTARIS" -> listOf("🛠️", "⚖️", "🪟", "🥣", "🧊", "🖨️", "🪑", "💡", "🔌", "🗄️", "🍳", "📦")
        else -> listOf("📦", "🧋", "🥐", "🥛", "🍜", "☕", "🍚", "🍳", "🍫", "🧼", "🥤", "🍰", "🥩", "🍗", "🥪")
    }

    val typeTitle = stockTypeLabel(effectiveStockType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (product == null) "Tambah $typeTitle Baru" else "Ubah $typeTitle")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Emoji picker row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(emojis) { emo ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (selectedEmoji == emo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedEmoji = emo },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emo, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama $typeTitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Category selector chip row
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Kategori:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = categoryName == cat.name,
                                    onClick = { categoryName = cat.name },
                                    label = { Text(cat.name, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    // Unit selector (pcs, kg, unit, dll)
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan") },
                        placeholder = { Text(defaultUnit) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isProductType) {
                    // Toggle: Tipe Produk
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Produk Racikan / Resep Bahan",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isComposed) "HPP dihitung otomatis dari bahan baku" else "Barang jadi / HPP diinput manual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isComposed,
                                onCheckedChange = { isComposed = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Form Racikan Resep
                    if (isComposed) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Komposisi Bahan Penyusun:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    TextButton(
                                        onClick = { showAddIngredientModal = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Tambah Bahan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (ingredientsList.isEmpty()) {
                                    Text(
                                        text = "Belum ada bahan baku dipilih. Klik 'Tambah Bahan' untuk memilih bahan penyusun.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ingredientsList.forEachIndexed { idx, ing ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(ing.materialName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("${ing.quantityNeeded} ${ing.unit} @ ${Formatters.rupiah(ing.unitCost)} = ${Formatters.rupiah((ing.quantityNeeded * ing.unitCost).toLong())}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        ingredientsList = ingredientsList.filterIndexed { i, _ -> i != idx }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp), tint = MoneyOutRed)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = yieldQuantityText,
                                        onValueChange = { if (it.all { c -> c.isDigit() }) yieldQuantityText = it },
                                        label = { Text("Hasil Porsi / Pcs") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("HPP per $unit Otomatis:", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                        Text(Formatters.rupiah(calculatedUnitHpp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Prices Section (Harga Jual & HPP Berdampingan Transparan)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sellPriceText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) sellPriceText = it },
                            label = { Text("Harga Jual") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        if (!isComposed) {
                            OutlinedTextField(
                                value = standaloneCostPriceText,
                                onValueChange = { if (it.all { c -> c.isDigit() }) standaloneCostPriceText = it },
                                label = { Text("Harga Modal (HPP)") },
                                prefix = { Text("Rp ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("HPP Otomatis", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.rupiah(effectiveHpp), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Berdasarkan resep", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Transparent Margin Live Preview Card
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (marginRp >= 0) MoneyInGreenBg else MoneyOutRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Margin Keuntungan:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "+${Formatters.rupiah(marginRp)} (+$marginPercent%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (marginRp >= 0) MoneyInGreen else MoneyOutRed
                            )
                        }
                    }
                } else {
                    // Cost / Value input for BAHAN & INVENTARIS
                    OutlinedTextField(
                        value = standaloneCostPriceText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) standaloneCostPriceText = it },
                        label = {
                            Text(if (effectiveStockType == "BAHAN") "Harga Beli per $unit (Modal)" else "Nilai / Harga Aset Satuan")
                        },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stock fields
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) stockText = it },
                        label = { Text("Stok Sekarang") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) minStockText = it },
                        label = { Text("Batas Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val sell = if (isProductType) (sellPriceText.toLongOrNull() ?: 0L) else 0L
                        val cost = effectiveHpp
                        val stk = stockText.toIntOrNull() ?: 0
                        val minStk = minStockText.toIntOrNull() ?: 5
                        val yld = yieldQuantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1

                        onSave(
                            ProductEntity(
                                id = product?.id ?: 0L,
                                name = name.trim(),
                                categoryName = categoryName,
                                sellPrice = sell,
                                costPrice = cost,
                                stock = stk,
                                minStockAlert = minStk,
                                emoji = selectedEmoji,
                                isComposedProduct = isComposed,
                                recipeJson = if (isComposed) RecipeHelper.toJson(ingredientsList) else null,
                                yieldQuantity = yld,
                                unit = unit.ifBlank { defaultUnit },
                                stockType = effectiveStockType
                            )
                        )
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            Row {
                if (product != null) {
                    TextButton(
                        onClick = { onDelete(product) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MoneyOutRed)
                    ) {
                        Text("Hapus")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )

    // Modal to pick raw material ingredient (prioritizing BAHAN items)
    if (showAddIngredientModal) {
        val availableMaterials = remember(allProducts, product?.id) {
            allProducts
                .filter { it.id != product?.id && it.stockType != "INVENTARIS" }
                .sortedWith(compareBy<ProductEntity> { it.stockType != "BAHAN" }.thenBy { it.name })
        }
        var selectedMaterial by remember { mutableStateOf<ProductEntity?>(availableMaterials.firstOrNull()) }
        var qtyInputText by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showAddIngredientModal = false },
            title = { Text("Pilih Bahan Baku / Barang") },
            text = {
                Column {
                    Text("Pilih bahan penyusun dari Stok Bahan atau Barang:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (availableMaterials.isEmpty()) {
                        Text("Belum ada bahan baku atau barang terdaftar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            items(availableMaterials) { mat ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedMaterial?.id == mat.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedMaterial = mat }
                                        .padding(vertical = 3.dp, horizontal = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${mat.emoji} ${mat.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(
                                                text = "${stockTypeLabel(mat.stockType)} • Stok ${mat.stock} ${mat.unit}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text("${Formatters.rupiah(mat.costPrice)}/${mat.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = qtyInputText,
                            onValueChange = { qtyInputText = it },
                            label = { Text("Takaran (${selectedMaterial?.unit ?: "satuan"})") },
                            placeholder = { Text("contoh: 1 atau 0.5") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mat = selectedMaterial
                        val q = qtyInputText.toDoubleOrNull() ?: 1.0
                        if (mat != null && q > 0) {
                            ingredientsList = ingredientsList + ProductIngredient(
                                materialId = mat.id,
                                materialName = mat.name,
                                quantityNeeded = q,
                                unit = mat.unit,
                                unitCost = mat.costPrice
                            )
                            showAddIngredientModal = false
                        }
                    },
                    enabled = selectedMaterial != null
                ) {
                    Text("Pilih Bahan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIngredientModal = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
