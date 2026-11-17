package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.RecipeHelper
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductHppRecipeScreen(
    viewModel: SakuUsahaViewModel,
    products: List<ProductEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var filterMode by remember { mutableStateOf("SEMUA") } // "SEMUA", "RACIKAN", "JADI"
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    val stockCategories by viewModel.categoriesByType("STOK").collectAsState(initial = emptyList())

    val productItems = remember(products) { products.filter { it.stockType == "PRODUK" } }

    val filteredProducts = remember(productItems, filterMode) {
        when (filterMode) {
            "RACIKAN" -> productItems.filter { it.isComposedProduct }
            "JADI" -> productItems.filter { !it.isComposedProduct }
            else -> productItems
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

    val avgMargin = if (productItems.isNotEmpty()) {
        productItems.map { p ->
            if (p.sellPrice > 0) ((p.sellPrice - p.costPrice).toFloat() / p.sellPrice.toFloat() * 100).toInt() else 0
        }.average().toInt()
    } else 0
    val potentialProfit = productItems.sumOf { (it.sellPrice - it.costPrice) * it.stock.coerceAtLeast(0) }
    val composedCount = productItems.count { it.isComposedProduct }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HPP & Resep", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "$composedCount produk racikan • Rata-rata margin +$avgMargin%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_hpp_recipe")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.recalculateAllHpp() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("btn_recalculate_hpp")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hitung Ulang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Transparansi HPP & Margin", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Otomatis terhubung dengan komposisi bahan baku & restok",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Rata-rata Margin", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        Text("+$avgMargin%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Margin per produk", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MoneyInGreenBg,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Potensi Untung", fontSize = 10.sp, color = MoneyInGreen)
                                        Text(Formatters.rupiah(potentialProfit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MoneyInGreen)
                                        Text("Dari stok aktif", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "SEMUA" to "Semua (${productItems.size})",
                            "RACIKAN" to "Produk Racikan ($composedCount)",
                            "JADI" to "Barang Jadi (${productItems.size - composedCount})"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = filterMode == key,
                                onClick = { filterMode = key },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Data HPP",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tambahkan produk atau racikan bahan baku untuk menghitung HPP otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredProducts) { prod ->
                    val marginRp = prod.sellPrice - prod.costPrice
                    val marginPercent = if (prod.sellPrice > 0) ((marginRp.toFloat() / prod.sellPrice.toFloat()) * 100).toInt() else 0
                    val ingredients = if (prod.isComposedProduct) RecipeHelper.parseIngredients(prod.recipeJson) else emptyList()

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { productToEdit = prod }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(prod.emoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            if (prod.isComposedProduct) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                StatusBadge(
                                                    text = "Racikan (${ingredients.size} bahan)",
                                                    containerColor = Color(0xFFCCFBF1),
                                                    contentColor = Color(0xFF0D9488)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${prod.categoryName} • Satuan: ${prod.unit} • Stok: ${prod.stock}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                StatusBadge(
                                    text = "+$marginPercent%",
                                    containerColor = if (marginPercent >= 20) MoneyInGreenBg else WarningYellowBg,
                                    contentColor = if (marginPercent >= 20) MoneyInGreen else WarningYellow
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("HPP (Harga Modal)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.rupiah(prod.costPrice), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column {
                                    Text("Harga Jual Satuan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.rupiah(prod.sellPrice), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Laba Kotor / Margin", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+${Formatters.rupiah(marginRp)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MoneyInGreen)
                                }
                            }

                            if (prod.isComposedProduct && ingredients.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Komposisi Resep (Hasil: ${prod.yieldQuantity} ${prod.unit}):",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ingredients.forEach { ing ->
                                            val subtotal = (ing.quantityNeeded * ing.unitCost).toLong()
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("• ${ing.materialName} (${ing.quantityNeeded} ${ing.unit})", fontSize = 10.sp)
                                                Text(Formatters.rupiah(subtotal), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
}
