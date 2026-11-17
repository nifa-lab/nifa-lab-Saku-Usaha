package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.theme.MoneyInGreen
import com.example.ui.theme.MoneyOutRed
import com.example.ui.viewmodel.SakuUsahaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionsScreen(
    viewModel: SakuUsahaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State for live permission checking
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Permission Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] ?: hasCameraPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
    }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Izin Akses Ponsel",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_permissions")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    AiAssistantTopActionButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Header Info Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Privasi & Izin Aplikasi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SakuUsaha memerlukan izin fitur ponsel untuk mendukung pencatatan nota fisik, pengingat kas harian, dan ekspor berkas laporan keuangan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Action: Request All Permissions if any missing
            val needsRuntimePermissions = !hasCameraPermission || !hasNotificationPermission
            if (needsRuntimePermissions) {
                item {
                    Button(
                        onClick = {
                            val permissionsToRequest = mutableListOf<String>()
                            if (!hasCameraPermission) permissionsToRequest.add(Manifest.permission.CAMERA)
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (permissionsToRequest.isNotEmpty()) {
                                multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_request_all_permissions")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Izinkan Semua Fitur Ponsel Sekaligus", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 1. Kamera Ponsel
            item {
                PermissionItemCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Kamera Ponsel",
                    description = "Mengambil foto struk, kuitansi belanja, nota transfer bank, dan foto produk langsung lewat kamera.",
                    isGranted = hasCameraPermission,
                    onRequestPermission = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    },
                    testTagKey = "camera"
                )
            }

            // 2. Notifikasi Ponsel
            item {
                PermissionItemCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notifikasi & Pengingat",
                    description = "Menampilkan pengingat harian tutup buku toko di malam hari dan peringatan jatuh tempo piutang pelanggan.",
                    isGranted = hasNotificationPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    testTagKey = "notification"
                )
            }

            // 3. Berkas & Penyimpanan
            item {
                PermissionItemCard(
                    icon = Icons.Default.FolderShared,
                    title = "Penyimpanan & Ekspor Dokumen",
                    description = "Menyimpan dan membagikan laporan keuangan format PDF dan Excel/CSV ke WhatsApp atau memori ponsel.",
                    isGranted = true, // Scoped storage & FileProvider enabled
                    onRequestPermission = null,
                    grantedLabel = "Aktif (FileProvider)",
                    testTagKey = "storage"
                )
            }

            // 4. Getaran Ponsel (Vibration)
            item {
                PermissionItemCard(
                    icon = Icons.Default.Vibration,
                    title = "Getaran Ponsel (Haptic Feedback)",
                    description = "Umpan balik sentuhan dan getar saat menyimpan transaksi, menghapus item, atau mencetak kuitansi.",
                    isGranted = true,
                    onRequestPermission = null,
                    grantedLabel = "Aktif",
                    testTagKey = "vibration"
                )
            }

            // 5. Jaringan Ponsel & Internet
            item {
                PermissionItemCard(
                    icon = Icons.Default.Wifi,
                    title = "Akses Internet & Jaringan",
                    description = "Dukungan berbagi nota atau laporan kas langsung ke pelanggan via aplikasi WhatsApp & cetak nirkabel.",
                    isGranted = true,
                    onRequestPermission = null,
                    grantedLabel = "Aktif",
                    testTagKey = "network"
                )
            }

            // Open Phone System Settings Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { openAppSettings() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_phone_settings")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Pengaturan Izin di Ponsel")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Anda juga dapat mengubah atau mencabut izin aplikasi kapan saja melalui Menu Pengaturan > Aplikasi di ponsel Android Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: (() -> Unit)?,
    grantedLabel: String = "Diizinkan",
    testTagKey: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) MoneyInGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) MoneyInGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isGranted) MoneyInGreen.copy(alpha = 0.15f) else Color(0xFFFFECE5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isGranted) MoneyInGreen else MoneyOutRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGranted) grantedLabel else "Belum Aktif",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isGranted) MoneyInGreen else MoneyOutRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (!isGranted && onRequestPermission != null) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("btn_request_$testTagKey")
                ) {
                    Text("Berikan Izin")
                }
            }
        }
    }
}
