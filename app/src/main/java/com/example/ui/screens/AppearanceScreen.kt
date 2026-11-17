package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File
import com.example.R
import com.example.data.model.BusinessProfileEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: SakuUsahaViewModel,
    profile: BusinessProfileEntity?,
    onNavigateToCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var businessName by remember(profile) { mutableStateOf(profile?.businessName ?: "Toko Suka Maju") }
    var ownerName by remember(profile) { mutableStateOf(profile?.ownerName ?: "Budi Santoso") }
    var selectedColorHex by remember(profile) { mutableStateOf(profile?.themeColorHex ?: "#146C43") }
    var selectedThemeMode by remember(profile) { mutableStateOf(profile?.themeMode ?: "LIGHT") }
    var selectedBanner by remember(profile) { mutableStateOf(profile?.bannerPreset ?: "WARUNG") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                val destFile = File(context.filesDir, "custom_banner.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                selectedBanner = "CUSTOM"
                if (profile != null) {
                    viewModel.updateProfile(
                        profile.copy(
                            bannerPreset = "CUSTOM",
                            customBannerUri = destFile.absolutePath
                        )
                    )
                }
                viewModel.showToast("Gambar kartu beranda berhasil diperbarui")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MoneyOutRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Hapus Semua Data?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Peringatan: Tindakan ini akan menghapus permanen seluruh data transaksi, daftar produk stok, dan riwayat utang piutang dari aplikasi.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Data yang telah dihapus tidak dapat dipulihkan kembali. Aplikasi akan bersih seperti baru.",
                        fontWeight = FontWeight.SemiBold,
                        color = MoneyOutRed
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyOutRed),
                    modifier = Modifier.testTag("btn_confirm_reset_all_data")
                ) {
                    Text("Hapus Semua Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atur Tampilan & Usaha", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Business Identity Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Identitas Usaha",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = {
                            businessName = it
                            if (profile != null) {
                                viewModel.updateProfile(profile.copy(businessName = it))
                            }
                        },
                        label = { Text("Nama Toko / Usaha") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_business_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = {
                            ownerName = it
                            if (profile != null) {
                                viewModel.updateProfile(profile.copy(ownerName = it))
                            }
                        },
                        label = { Text("Nama Pemilik") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_owner_name")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Color Picker (6 Colors)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Warna Tema Aplikasi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Pilih aksen warna merek untuk tombol, ikon, dan kartu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val colorOptions = listOf(
                        "#146C43" to "Hijau Merek",
                        "#10B981" to "Hijau Mint",
                        "#2563EB" to "Biru Laut",
                        "#7C3AED" to "Ungu Royal",
                        "#EA580C" to "Oranye Senja",
                        "#DC2626" to "Merah Crimson"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorOptions.forEach { (hex, name) ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColorHex = hex
                                        if (profile != null) {
                                            viewModel.updateProfile(profile.copy(themeColorHex = hex))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Mode: Otomatis, Terang, Gelap
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mode Tampilan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("LIGHT", "Terang", Icons.Default.LightMode),
                            Triple("DARK", "Gelap", Icons.Default.DarkMode),
                            Triple("SYSTEM", "Otomatis", Icons.Default.BrightnessAuto)
                        ).forEach { (modeCode, modeLabel, modeIcon) ->
                            val isSelected = selectedThemeMode == modeCode
                            OutlinedButton(
                                onClick = {
                                    selectedThemeMode = modeCode
                                    if (profile != null) {
                                        viewModel.updateProfile(profile.copy(themeMode = modeCode))
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(modeIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(modeLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header Banner Preset
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gambar Kartu Beranda",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Pilih latar kartu laba di halaman depan atau gunakan foto sendiri",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Warung Modern illustration
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedBanner == "WARUNG") 3.dp else 1.dp,
                                color = if (selectedBanner == "WARUNG") MaterialTheme.colorScheme.primary else Color.LightGray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(85.dp)
                                .clickable {
                                    selectedBanner = "WARUNG"
                                    if (profile != null) {
                                        viewModel.updateProfile(profile.copy(bannerPreset = "WARUNG"))
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = R.drawable.banner_warung_modern),
                                    contentDescription = "Warung",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Warung", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Option 2: Clean Gradient
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedBanner == "CLEAN") 3.dp else 1.dp,
                                color = if (selectedBanner == "CLEAN") MaterialTheme.colorScheme.primary else Color.LightGray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(85.dp)
                                .clickable {
                                    selectedBanner = "CLEAN"
                                    if (profile != null) {
                                        viewModel.updateProfile(profile.copy(bannerPreset = "CLEAN"))
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Warna Polos", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Option 3: Gambar Sendiri (Kustom)
                        val hasCustomImage = profile?.customBannerUri != null && File(profile.customBannerUri).exists()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedBanner == "CUSTOM") 3.dp else 1.dp,
                                color = if (selectedBanner == "CUSTOM") MaterialTheme.colorScheme.primary else Color.LightGray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(85.dp)
                                .clickable {
                                    if (hasCustomImage) {
                                        selectedBanner = "CUSTOM"
                                        if (profile != null) {
                                            viewModel.updateProfile(profile.copy(bannerPreset = "CUSTOM"))
                                        }
                                    } else {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (hasCustomImage) {
                                    AsyncImage(
                                        model = File(profile!!.customBannerUri!!),
                                        contentDescription = "Foto Sendiri",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Foto Sendiri", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Pilih Foto", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Button to upload / replace custom photo
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_upload_custom_banner")
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (profile?.customBannerUri != null && File(profile.customBannerUri).exists()) {
                                "Ganti Gambar Sendiri dari Galeri"
                            } else {
                                "Tambah Gambar Sendiri dari Galeri"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation shortcuts to Manage Categories & Reset
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ListItem(
                        headlineContent = { Text("Kelola Kategori Transaksi & Stok", fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onNavigateToCategories)
                    )

                    Divider(modifier = Modifier.padding(horizontal = 8.dp))

                    ListItem(
                        headlineContent = { Text("Reset & Hapus Semua Data", color = MoneyOutRed, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Hapus permanen semua catatan transaksi, produk, dan utang") },
                        leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MoneyOutRed) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showResetDialog = true }
                            .testTag("menu_reset_all_data")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
