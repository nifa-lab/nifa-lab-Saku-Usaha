package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.ui.theme.*
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.SakuUsahaViewModel
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionSheet(
    viewModel: SakuUsahaViewModel,
    initialType: String,
    onDismissRequest: () -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Main Tab: PEMASUKAN vs PENGELUARAN
    var mainTab by remember {
        mutableStateOf(
            if (initialType in listOf("BELI", "BIAYA", "PENGELUARAN")) "PENGELUARAN" else "PEMASUKAN"
        )
    }

    // Sub-Kategori:
    // Under PEMASUKAN: PENJUALAN vs MODAL
    // Under PENGELUARAN: BELI vs BIAYA
    var subCategory by remember {
        mutableStateOf(
            when (initialType) {
                "MODAL" -> "MODAL"
                "BELI" -> "BELI"
                "BIAYA" -> "BIAYA"
                else -> if (mainTab == "PENGELUARAN") "BELI" else "PENJUALAN"
            }
        )
    }

    var amountText by remember { mutableStateOf("") }

    // Multi-product Cart for Penjualan / Beli
    var selectedCartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var showProductSearchDialog by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("TUNAI") } // TUNAI, TRANSFER, QRIS, E_WALLET, HUTANG
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var downPaymentText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Receipt upload state
    var attachedReceiptName by remember { mutableStateOf<String?>(null) }
    var attachedReceiptSize by remember { mutableStateOf<String?>(null) }
    var attachedReceiptPath by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val products by viewModel.allProducts.collectAsState()

    val categoryType = when (subCategory) {
        "PENJUALAN" -> "PENJUALAN"
        "MODAL" -> "MODAL"
        "BELI" -> "BELI"
        else -> "BIAYA"
    }
    val categories by viewModel.categoriesByType(categoryType).collectAsState(initial = emptyList())

    // When subCategory changes, reset products/amount if needed
    LaunchedEffect(subCategory) {
        if (subCategory == "MODAL" || subCategory == "BIAYA") {
            selectedCartItems = emptyList()
        }
        selectedCategory = ""
    }

    // Helper function to safely process picked file/image
    fun processPickedUri(uri: Uri?) {
        if (uri == null) return
        try {
            var fileName = "nota_${System.currentTimeMillis() % 10000}"
            var fileSize = 0L
            val contentResolver = context.contentResolver

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { fileName = it }
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val extension = if (fileName.contains(".")) fileName.substringAfterLast(".") else "jpg"
            val localReceiptFile = File(context.filesDir, "bukti_${System.currentTimeMillis()}.$extension")

            contentResolver.openInputStream(uri)?.use { input ->
                localReceiptFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize <= 0L) {
                fileSize = localReceiptFile.length()
            }

            val sizeText = if (fileSize > 1024 * 1024) {
                String.format("%.1f MB", fileSize.toFloat() / (1024 * 1024))
            } else {
                "${(fileSize / 1024).coerceAtLeast(1)} KB"
            }

            attachedReceiptName = fileName
            attachedReceiptSize = sizeText
            attachedReceiptPath = localReceiptFile.absolutePath
            errorMessage = null
            viewModel.showToast("Bukti transaksi berhasil diunggah ($sizeText)")
        } catch (e: Exception) {
            errorMessage = "Gagal memproses berkas: ${e.localizedMessage ?: "Format tidak didukung"}"
        }
    }

    // Helper function to process camera photo
    fun processCameraBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        try {
            val localFile = File(context.filesDir, "nota_kamera_${System.currentTimeMillis()}.jpg")
            localFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val sizeKb = (localFile.length() / 1024).coerceAtLeast(1)
            attachedReceiptName = localFile.name
            attachedReceiptSize = "$sizeKb KB"
            attachedReceiptPath = localFile.absolutePath
            errorMessage = null
            viewModel.showToast("Foto nota berhasil diambil ($sizeKb KB)")
        } catch (e: Exception) {
            errorMessage = "Gagal menyimpan foto dari kamera"
        }
    }

    // Media & Camera Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        processPickedUri(uri)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        processPickedUri(uri)
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        processCameraBitmap(bitmap)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraPreviewLauncher.launch(null)
        } else {
            errorMessage = "Izin kamera diperlukan untuk memotret bukti transaksi"
        }
    }

    // Calculate dynamic total for cart items
    val cartTotal = if (subCategory == "PENJUALAN") {
        selectedCartItems.sumOf { it.product.sellPrice * it.quantity }
    } else if (subCategory == "BELI") {
        selectedCartItems.sumOf { it.product.costPrice * it.quantity }
    } else 0L

    LaunchedEffect(selectedCartItems) {
        if (selectedCartItems.isNotEmpty()) {
            amountText = cartTotal.toString()
        }
    }

    // Product search modal dialog to select more products
    if (showProductSearchDialog) {
        AlertDialog(
            onDismissRequest = { showProductSearchDialog = false },
            title = {
                Text(
                    text = if (subCategory == "PENJUALAN") "Pilih Produk Penjualan" else "Pilih Produk untuk Restok",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val selectableItems = remember(products, subCategory) {
                    if (subCategory == "PENJUALAN") {
                        products.filter { it.stockType == "PRODUK" }
                    } else {
                        products
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectableItems.isEmpty()) {
                        Text(
                            text = if (subCategory == "PENJUALAN")
                                "Belum ada produk terjual terdaftar. Tambahkan produk di menu Produk atau Stok."
                            else
                                "Belum ada item stok terdaftar. Tambahkan item di menu Stok terlebih dahulu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        selectableItems.forEach { prod ->
                            val existing = selectedCartItems.find { it.product.id == prod.id }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (existing != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        if (existing != null) {
                                            selectedCartItems = selectedCartItems.map {
                                                if (it.product.id == prod.id) it.copy(quantity = it.quantity + 1) else it
                                            }
                                        } else {
                                            selectedCartItems = selectedCartItems + CartItem(product = prod, quantity = 1)
                                        }
                                        if (subCategory == "PENJUALAN" && selectedCategory.isBlank()) {
                                            selectedCategory = "Penjualan Produk"
                                        } else if (subCategory == "BELI" && selectedCategory.isBlank()) {
                                            selectedCategory = "Belanja Stok"
                                        }
                                        showProductSearchDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${prod.emoji} ${prod.name}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (subCategory == "PENJUALAN")
                                                "Harga Jual: ${Formatters.rupiah(prod.sellPrice)} • Sisa Stok: ${prod.stock}"
                                            else
                                                "Harga Beli: ${Formatters.rupiah(prod.costPrice)} • Stok Sekarang: ${prod.stock}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (existing != null) {
                                        Text(
                                            text = "${existing.quantity}x dipilih",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Tambah",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductSearchDialog = false }) {
                    Text("Selesai")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Catat Transaksi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pilih kategori pemasukan atau pengeluaran usaha",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // PILIHAN UTAMA: PEMASUKAN vs PENGELUARAN (TAB UTAMA)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // TAB PEMASUKAN
                    val isPemasukanSelected = mainTab == "PEMASUKAN"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPemasukanSelected) MoneyInGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                mainTab = "PEMASUKAN"
                                if (subCategory !in listOf("PENJUALAN", "MODAL")) {
                                    subCategory = "PENJUALAN"
                                }
                                errorMessage = null
                            }
                            .testTag("tab_main_pemasukan")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isPemasukanSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pemasukan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPemasukanSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // TAB PENGELUARAN
                    val isPengeluaranSelected = mainTab == "PENGELUARAN"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPengeluaranSelected) MoneyOutRed else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                mainTab = "PENGELUARAN"
                                if (subCategory !in listOf("BELI", "BIAYA")) {
                                    subCategory = "BELI"
                                }
                                errorMessage = null
                            }
                            .testTag("tab_main_pengeluaran")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isPengeluaranSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pengeluaran",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPengeluaranSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // =========================================================================
            // SUB-KATEGORI SESUAI PILIHAN UTAMA
            // Pemasukan: Penjualan vs Modal
            // Pengeluaran: Beli vs Biaya
            // =========================================================================
            Text(
                text = "Sub-Kategori ${if (mainTab == "PEMASUKAN") "Pemasukan" else "Pengeluaran"}:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mainTab == "PEMASUKAN") {
                    // 1. PENJUALAN
                    val isPenjualan = subCategory == "PENJUALAN"
                    OutlinedButton(
                        onClick = {
                            subCategory = "PENJUALAN"
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (isPenjualan) 2.dp else 1.dp,
                            color = if (isPenjualan) MoneyInGreen else MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isPenjualan) MoneyInGreenBg else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtab_penjualan")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (isPenjualan) MoneyInGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Penjualan",
                            fontWeight = if (isPenjualan) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPenjualan) MoneyInGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 2. MODAL
                    val isModal = subCategory == "MODAL"
                    OutlinedButton(
                        onClick = {
                            subCategory = "MODAL"
                            selectedCartItems = emptyList()
                            amountText = ""
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (isModal) 2.dp else 1.dp,
                            color = if (isModal) Color(0xFF0D9488) else MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isModal) Color(0xFFCCFBF1) else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtab_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = if (isModal) Color(0xFF0F766E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Modal",
                            fontWeight = if (isModal) FontWeight.Bold else FontWeight.Normal,
                            color = if (isModal) Color(0xFF0F766E) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // 1. BELI (Stok / Inventaris)
                    val isBeli = subCategory == "BELI"
                    OutlinedButton(
                        onClick = {
                            subCategory = "BELI"
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (isBeli) 2.dp else 1.dp,
                            color = if (isBeli) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isBeli) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtab_beli")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = if (isBeli) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Beli (Stok)",
                            fontWeight = if (isBeli) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBeli) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 2. BIAYA (Operasional)
                    val isBiaya = subCategory == "BIAYA"
                    OutlinedButton(
                        onClick = {
                            subCategory = "BIAYA"
                            selectedCartItems = emptyList()
                            amountText = ""
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (isBiaya) 2.dp else 1.dp,
                            color = if (isBiaya) MoneyOutRed else MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isBiaya) MoneyOutRedBg else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtab_biaya")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (isBiaya) MoneyOutRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Biaya (Beban)",
                            fontWeight = if (isBiaya) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBiaya) MoneyOutRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // FORM INPUT KHUSUS SESUAI SUB-KATEGORI
            // =========================================================================

            // 1. KASUS SUB-KATEGORI: MODAL (Pemasukan Uang Modal)
            if (subCategory == "MODAL") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDFA),
                    border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Penambahan Modal Usaha (Kas Masuk)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF115E59)
                            )
                            Text(
                                text = "Mencatat setoran modal awal, suntikan dana pemilik, atau pinjaman modal. Transaksi ini menambah saldo kas usaha tanpa mengurangi persediaan barang.",
                                fontSize = 11.sp,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. KASUS SUB-KATEGORI: PENJUALAN (Wajib Pilih Produk)
            else if (subCategory == "PENJUALAN") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pilih Produk Penjualan (Wajib)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCartItems.isEmpty() && errorMessage != null) MoneyOutRed else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MoneyInGreenBg
                        ) {
                            Text(
                                text = "Bisa > 1 Produk",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneyInGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (products.isNotEmpty()) {
                        TextButton(
                            onClick = { showProductSearchDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Produk Lainnya", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick horizontal chip picker to add products
                if (products.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(products) { prod ->
                            val isSelected = selectedCartItems.any { it.product.id == prod.id }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val existing = selectedCartItems.find { it.product.id == prod.id }
                                    if (existing != null) {
                                        selectedCartItems = selectedCartItems.filter { it.product.id != prod.id }
                                    } else {
                                        selectedCartItems = selectedCartItems + CartItem(product = prod, quantity = 1)
                                        if (selectedCategory.isBlank()) selectedCategory = "Penjualan Produk"
                                        errorMessage = null
                                    }
                                },
                                label = {
                                    Text("${prod.emoji} ${prod.name}")
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        val qty = selectedCartItems.find { it.product.id == prod.id }?.quantity ?: 1
                                        Text("($qty)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("chip_product_${prod.id}")
                            )
                        }
                    }
                }

                // If cart items are selected: show interactive multi-item list with quantity steppers
                if (selectedCartItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Keranjang Penjualan (${selectedCartItems.sumOf { it.quantity }} item)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = Formatters.rupiah(cartTotal),
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyInGreen,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            selectedCartItems.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${item.product.emoji} ${item.product.name}",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${Formatters.rupiah(item.product.sellPrice)} x ${item.quantity} = ${Formatters.rupiah(item.product.sellPrice * item.quantity)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (item.quantity > 1) {
                                                        selectedCartItems = selectedCartItems.map {
                                                            if (it.product.id == item.product.id) it.copy(quantity = it.quantity - 1) else it
                                                        }
                                                    } else {
                                                        selectedCartItems = selectedCartItems.filter { it.product.id != item.product.id }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                            }

                                            Text(
                                                text = "${item.quantity}",
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    selectedCartItems = selectedCartItems.map {
                                                        if (it.product.id == item.product.id) it.copy(quantity = it.quantity + 1) else it
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            IconButton(
                                                onClick = {
                                                    selectedCartItems = selectedCartItems.filter { it.product.id != item.product.id }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Hapus",
                                                    tint = MoneyOutRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { showProductSearchDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Tambah Produk Lainnya", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = MoneyInGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pilih satu atau lebih produk di atas untuk penjualan toko.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. KASUS SUB-KATEGORI: BELI (Belanja Stok & Bahan)
            else if (subCategory == "BELI") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pembelian Stok & Inventaris Produk",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Catat belanja stok dagangan atau bahan baku. Anda dapat memilih produk untuk menambah stok secara otomatis saat disimpan.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Produk untuk Ditambah Stoknya (Opsional)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (products.isNotEmpty()) {
                        TextButton(
                            onClick = { showProductSearchDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cari Produk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (selectedCartItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Produk yang akan direstok:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            selectedCartItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.product.emoji} ${item.product.name} (+${item.quantity} unit)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(
                                        onClick = { selectedCartItems = selectedCartItems.filter { it.product.id != item.product.id } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Batal", tint = MoneyOutRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. KASUS SUB-KATEGORI: BIAYA (Beban Operasional Usaha)
            else if (subCategory == "BIAYA") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MoneyOutRedBg,
                    border = BorderStroke(1.dp, MoneyOutRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MoneyOutRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pengeluaran Beban & Biaya Operasional",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MoneyOutRed
                            )
                            Text(
                                text = "Catat biaya rutin usaha seperti listrik, air, sewa tempat, gaji karyawan, perlengkapan toko, atau biaya lainnya.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // =========================================================================
            // NOMINAL UANG (AMOUNT INPUT)
            // =========================================================================
            Text(
                text = when (subCategory) {
                    "MODAL" -> "Nominal Tambahan Modal (Rp)"
                    "BELI" -> "Nominal Belanja Stok (Rp)"
                    "BIAYA" -> "Nominal Biaya Operasional (Rp)"
                    else -> "Total Nominal Penjualan (Rp)"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        amountText = newValue
                    }
                },
                placeholder = { Text("0") },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_amount")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // KATEGORI TRANSAKSI
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kategori Spesifik",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onNavigateToCategories,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("btn_kelola_kategori")
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kelola Kategori", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (categories.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_cat_${cat.name}")
                        )
                    }
                }
            } else {
                Text(
                    text = "Kategori akan otomatis terisi sesuai sub-kategori terpilih",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // METODE PEMBAYARAN
            // =========================================================================
            Text(
                text = "Metode Pembayaran",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val availableMethods = if (subCategory == "MODAL") {
                    listOf(
                        Triple("TRANSFER", "Transfer Bank", Icons.Default.AccountBalance),
                        Triple("TUNAI", "Kas Tunai", Icons.Default.AttachMoney),
                        Triple("E_WALLET", "E-Wallet", Icons.Default.AccountBalanceWallet)
                    )
                } else {
                    listOf(
                        Triple("TUNAI", "Tunai", Icons.Default.AttachMoney),
                        Triple("TRANSFER", "Transfer Bank", Icons.Default.AccountBalance),
                        Triple("QRIS", "QRIS", Icons.Default.QrCodeScanner),
                        Triple("E_WALLET", "E-Wallet", Icons.Default.AccountBalanceWallet),
                        Triple("HUTANG", if (mainTab == "PEMASUKAN") "Piutang (Tempo)" else "Utang (Tempo)", Icons.Default.HourglassTop)
                    )
                }

                items(availableMethods) { (code, label, icon) ->
                    val isSelected = selectedPaymentMethod == code
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPaymentMethod = code },
                        leadingIcon = {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("chip_pay_$code")
                    )
                }
            }

            // =========================================================================
            // FORM KONTAK / PIHAK TERKAIT (PELANGGAN, SUPPLIER, ATAU PENYETOR MODAL)
            // =========================================================================
            val showContactFields = selectedPaymentMethod == "HUTANG" || subCategory in listOf("PENJUALAN", "BELI", "MODAL")
            if (showContactFields) {
                Spacer(modifier = Modifier.height(14.dp))
                val contactLabel = when (subCategory) {
                    "MODAL" -> "Nama Penyetor Modal / Sumber Dana (Opsional)"
                    "BELI" -> if (selectedPaymentMethod == "HUTANG") "Nama Supplier / Pemasok (Wajib untuk Tempo)" else "Nama Supplier / Toko (Opsional)"
                    "PENJUALAN" -> if (selectedPaymentMethod == "HUTANG") "Nama Pelanggan (Wajib untuk Tempo)" else "Nama Pelanggan (Opsional)"
                    else -> "Nama Pihak / Penerima (Opsional)"
                }

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text(contactLabel) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_name")
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Nomor Telepon / WhatsApp (Opsional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_phone")
                )

                if (selectedPaymentMethod == "HUTANG") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = downPaymentText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) downPaymentText = it },
                        label = { Text("Uang Muka / DP Dibayar di Awal (Rp)") },
                        prefix = { Text("Rp ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_down_payment")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // UNGGAH BUKTI TRANSAKSI (FOTO NOTA, STRUK, KUITANSI, BUKTI TRANSFER)
            // =========================================================================
            Text(
                text = "Bukti Transaksi (Nota / Struk / Bukti Transfer)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (attachedReceiptName == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Kamera
                    OutlinedButton(
                        onClick = {
                            val isCameraGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (isCameraGranted) {
                                cameraPreviewLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_attach_camera")
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kamera", fontSize = 12.sp, maxLines = 1)
                    }

                    // 2. Galeri Foto
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_attach_photo")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galeri", fontSize = 12.sp, maxLines = 1)
                    }

                    // 3. Dokumen / PDF
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_attach_file")
                    ) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Berkas", fontSize = 12.sp, maxLines = 1)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
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
                            if (attachedReceiptPath != null && File(attachedReceiptPath).exists()) {
                                AsyncImage(
                                    model = File(attachedReceiptPath),
                                    contentDescription = "Bukti",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachedReceiptName!!,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${attachedReceiptSize ?: "Tersimpan"} • Siap Disimpan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoneyInGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                attachedReceiptName = null
                                attachedReceiptSize = null
                                attachedReceiptPath = null
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus Bukti", tint = MoneyOutRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Catatan
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan / Keterangan (Opsional)") },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_notes")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MoneyOutRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MoneyOutRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(color = MoneyOutRed, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // TOMBOL AKSI SIMPAN
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action Helper
                fun executeSave(closeSheetOnSuccess: Boolean) {
                    // Validations
                    if (subCategory == "PENJUALAN" && selectedCartItems.isEmpty()) {
                        errorMessage = "Wajib memilih minimal satu produk untuk transaksi penjualan"
                        return
                    }

                    val amt = if (subCategory == "PENJUALAN" && selectedCartItems.isNotEmpty()) {
                        selectedCartItems.sumOf { it.product.sellPrice * it.quantity }
                    } else if (subCategory == "BELI" && selectedCartItems.isNotEmpty() && amountText.isBlank()) {
                        selectedCartItems.sumOf { it.product.costPrice * it.quantity }
                    } else {
                        amountText.toLongOrNull() ?: 0L
                    }

                    if (amt <= 0L) {
                        errorMessage = when (subCategory) {
                            "MODAL" -> "Masukkan nominal modal yang disetorkan"
                            "BELI" -> "Masukkan nominal belanja stok"
                            "BIAYA" -> "Masukkan nominal biaya operasional"
                            else -> "Nominal transaksi tidak boleh kosong"
                        }
                        return
                    }

                    if (selectedPaymentMethod == "HUTANG" && contactName.isBlank()) {
                        errorMessage = if (mainTab == "PEMASUKAN")
                            "Nama pelanggan wajib diisi untuk transaksi piutang (tempo)"
                        else
                            "Nama supplier wajib diisi untuk transaksi utang (tempo)"
                        return
                    }

                    val dp = downPaymentText.toLongOrNull() ?: 0L
                    val dueDate = if (selectedPaymentMethod == "HUTANG") {
                        System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
                    } else null

                    val defaultCategoryForSub = when (subCategory) {
                        "PENJUALAN" -> "Penjualan Produk"
                        "MODAL" -> "Setoran Modal Pemilik"
                        "BELI" -> "Belanja Stok"
                        else -> "Operasional Toko"
                    }

                    val finalCategory = selectedCategory.ifBlank { defaultCategoryForSub }

                    // Product naming
                    val finalProdName = when {
                        selectedCartItems.isNotEmpty() -> selectedCartItems.joinToString(", ") { "${it.product.name} (${it.quantity}x)" }
                        subCategory == "MODAL" -> "Penambahan Modal Usaha"
                        subCategory == "BIAYA" -> finalCategory
                        else -> null
                    }

                    viewModel.saveTransaction(
                        type = subCategory,
                        category = finalCategory,
                        amount = amt,
                        paymentMethod = selectedPaymentMethod,
                        productId = selectedCartItems.firstOrNull()?.product?.id,
                        productName = finalProdName,
                        quantity = if (selectedCartItems.isNotEmpty()) selectedCartItems.sumOf { it.quantity } else 1,
                        items = selectedCartItems,
                        contactName = contactName.ifBlank { null },
                        contactPhone = contactPhone.ifBlank { null },
                        dueDate = dueDate,
                        downPayment = dp,
                        receiptFileName = attachedReceiptName,
                        receiptSizeText = attachedReceiptSize,
                        notes = notes.ifBlank { null },
                        onSuccess = {
                            if (closeSheetOnSuccess) {
                                onDismissRequest()
                            } else {
                                amountText = ""
                                selectedCartItems = emptyList()
                                notes = ""
                                attachedReceiptName = null
                                attachedReceiptSize = null
                                attachedReceiptPath = null
                                errorMessage = null
                            }
                        }
                    )
                }

                // Catat Lagi button
                OutlinedButton(
                    onClick = { executeSave(false) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_save_and_more")
                ) {
                    Text("Catat Lagi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Simpan button
                val saveButtonColor = if (mainTab == "PEMASUKAN") MoneyInGreen else MoneyOutRed
                Button(
                    onClick = { executeSave(true) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = saveButtonColor),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("btn_save_transaction")
                ) {
                    Text(
                        text = "Simpan ${if (mainTab == "PEMASUKAN") "Pemasukan" else "Pengeluaran"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
