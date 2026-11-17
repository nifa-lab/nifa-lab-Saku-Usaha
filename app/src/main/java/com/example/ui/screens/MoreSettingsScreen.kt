package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessProfileEntity
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.theme.MoneyOutRed
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    viewModel: SakuUsahaViewModel,
    profile: BusinessProfileEntity?,
    onNavigateToProducts: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSyncInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
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
                    Text("Peringatan: Seluruh catatan transaksi, data produk inventaris, dan riwayat utang piutang akan dihapus permanen dari perangkat.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Data yang dihapus tidak dapat dikembalikan. Aplikasi akan bersih dan kosong seperti baru pertama kali dipasang.",
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
                    modifier = Modifier.testTag("btn_confirm_reset_all_data_settings")
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

    if (showSyncInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSyncInfoDialog = false },
            title = { Text("Penyimpanan & Sinkronisasi") },
            text = {
                Text("Semua data transaksi, produk, dan utang tersimpan aman di memori lokal HP Anda (SQLite/Room). Aplikasi dapat digunakan 100% tanpa koneksi internet (offline-first).")
            },
            confirmButton = {
                TextButton(onClick = { showSyncInfoDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Tentang SakuUsaha") },
            text = {
                Column {
                    Text("SakuUsaha v1.2", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Kelola Keuangan UMKM Lebih Mudah")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Membantu usaha kecil meraih mimpi besar dengan pencatatan laba rugi, stok barang otomatis, utang piutang, dan kustomisasi tampilan.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Title
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengaturan & Lainnya",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    AiAssistantTopActionButton()
                }
            }
        }

        // Profile Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(onClick = onNavigateToAppearance)
                    .testTag("card_profile_settings")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.businessName ?: "Toko Suka Maju",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pemilik: ${profile?.ownerName ?: "Budi Santoso"} • ${profile?.businessType ?: "Toko Kelontong"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ubah profil & tampilan >",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Feature Navigation Group
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // AI Asistan Usaha
                    ListItem(
                        headlineContent = { Text("AI Asistan Usaha", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Konsultasi omset, HPP, dan saran stok pintar") },
                        leadingContent = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToAiAssistant)
                            .testTag("menu_ai_assistant")
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Produk
                    ListItem(
                        headlineContent = { Text("Produk", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Daftar produk, HPP & resep, serta transaksi produk") },
                        leadingContent = {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToProducts)
                            .testTag("menu_produk")
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Laporan Keuangan
                    ListItem(
                        headlineContent = { Text("Laporan Keuangan", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Laba rugi, arus kas, produk terlaris & ekspor") },
                        leadingContent = {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToReports)
                            .testTag("menu_laporan")
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Utang & Piutang
                    ListItem(
                        headlineContent = { Text("Utang & Piutang", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Pantau tagihan supplier & piutang pelanggan") },
                        leadingContent = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToDebts)
                            .testTag("menu_utang")
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Atur Tampilan
                    ListItem(
                        headlineContent = { Text("Tema & Tampilan", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Ubah warna tema, mode gelap/terang, dan banner beranda") },
                        leadingContent = {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToAppearance)
                            .testTag("menu_tampilan")
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Kelola Kategori
                    ListItem(
                        headlineContent = { Text("Kelola Kategori", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Kategori transaksi penjualan, biaya, dan stok") },
                        leadingContent = {
                            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToCategories)
                            .testTag("menu_kategori")
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Izin Akses Ponsel
                    ListItem(
                        headlineContent = { Text("Izin Akses Ponsel", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Kamera nota, notifikasi kas harian, getaran & berkas") },
                        leadingContent = {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToPermissions)
                            .testTag("menu_permissions")
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // System & Info Group
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text("Penyimpanan & Keamanan", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Offline-first SQLite Room database") },
                        leadingContent = {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSyncInfoDialog = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Tentang Aplikasi", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("SakuUsaha v1.2 untuk UMKM Indonesia") },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAboutDialog = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Reset & Hapus Semua Data", color = MoneyOutRed, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Hapus permanen semua transaksi, stok barang, dan catatan utang") },
                        leadingContent = {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MoneyOutRed)
                        },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MoneyOutRed) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showResetDialog = true }
                            .testTag("menu_reset_all_data_settings")
                    )
                }
            }
        }
    }
}
