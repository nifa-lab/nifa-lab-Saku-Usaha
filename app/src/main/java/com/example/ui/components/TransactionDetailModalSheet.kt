package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*
import com.example.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailModalSheet(
    transaction: TransactionEntity,
    onDismissRequest: () -> Unit,
    onViewReceipt: () -> Unit,
    onEditCategory: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isIncome = transaction.direction == "MASUK"
    val accentColor = when (transaction.type) {
        "MODAL" -> Color(0xFF0D9488)
        "PENJUALAN", "JUAL" -> MoneyInGreen
        "BELI" -> Color(0xFF0284C7)
        else -> MoneyOutRed
    }
    val accentBg = when (transaction.type) {
        "MODAL" -> Color(0xFFCCFBF1)
        "PENJUALAN", "JUAL" -> MoneyInGreenBg
        "BELI" -> Color(0xFFE0F2FE)
        else -> MoneyOutRedBg
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (transaction.type) {
                                "MODAL" -> Icons.Default.Savings
                                "PENJUALAN", "JUAL" -> Icons.Default.TrendingUp
                                "BELI" -> Icons.Default.Inventory2
                                else -> Icons.Default.Receipt
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Rincian Transaksi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No. Ref: #TX-${transaction.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Amount Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isIncome) "Total Uang Masuk" else "Total Uang Keluar",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (if (isIncome) "+ " else "- ") + Formatters.rupiah(transaction.amount),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            text = when (transaction.type) {
                                "MODAL" -> "Pemasukan: Modal Usaha"
                                "PENJUALAN", "JUAL" -> "Pemasukan: Penjualan"
                                "BELI" -> "Pengeluaran: Beli Stok"
                                else -> "Pengeluaran: Biaya Operasional"
                            },
                            containerColor = Color.White.copy(alpha = 0.85f),
                            contentColor = accentColor
                        )
                        StatusBadge(
                            text = if (transaction.paymentMethod == "HUTANG") {
                                "Utang (${transaction.status})"
                            } else {
                                "Lunas • ${transaction.paymentMethod}"
                            },
                            containerColor = if (transaction.paymentMethod == "HUTANG") WarningYellowBg else Color.White.copy(alpha = 0.85f),
                            contentColor = if (transaction.paymentMethod == "HUTANG") WarningYellow else accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Info List Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    DetailRowItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "Waktu Transaksi",
                        value = "${Formatters.date(transaction.timestamp)} • ${Formatters.timeOnly(transaction.timestamp)}"
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    DetailRowItem(
                        icon = Icons.Default.Category,
                        label = "Kategori",
                        value = transaction.category
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    DetailRowItem(
                        icon = Icons.Default.Payment,
                        label = "Metode Pembayaran",
                        value = when (transaction.paymentMethod) {
                            "TUNAI" -> "Tunai / Cash"
                            "QRIS" -> "QRIS Dinamis / Statis"
                            "TRANSFER" -> "Transfer Bank"
                            "E_WALLET" -> "Dompet Digital (GoPay/OVO/Dana)"
                            "HUTANG" -> "Utang / Piutang Tempo"
                            else -> transaction.paymentMethod
                        }
                    )
                }
            }

            // Products Breakdown (Multiple or Single Products)
            if (!transaction.productName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daftar Barang / Produk",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Split if multiple products e.g. "Es Teh Manis (2x), Roti Cokelat (1x)"
                        val items = transaction.productName.split(", ")
                        items.forEachIndexed { index, itemStr ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. $itemStr",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Contact Info (Pelanggan / Supplier)
            if (!transaction.contactName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (transaction.type) {
                                    "PENJUALAN", "JUAL" -> "Data Pelanggan"
                                    "MODAL" -> "Sumber Modal / Investor"
                                    else -> "Data Supplier / Pihak"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = transaction.contactName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!transaction.contactPhone.isNullOrBlank()) {
                            Text(
                                text = "No. Telp: ${transaction.contactPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val phone = transaction.contactPhone.replace("[^0-9]".toRegex(), "")
                                    val formattedPhone = if (phone.startsWith("0")) "62" + phone.substring(1) else phone
                                    val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/$formattedPhone?text=${Uri.encode("Halo ${transaction.contactName}, terima kasih atas transaksi di Toko SakuUsaha!")}")
                                    }
                                    try {
                                        context.startActivity(sendIntent)
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat WhatsApp", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Debt Detail if Utang
            if (transaction.paymentMethod == "HUTANG") {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WarningYellowBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rincian Utang / Tagihan", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = WarningYellow)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Uang Muka (DP):", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.rupiah(transaction.downPayment), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sisa Tagihan:", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.rupiah(transaction.remainingDebt), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MoneyOutRed))
                        }
                        if (transaction.dueDate != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Jatuh Tempo:", style = MaterialTheme.typography.bodySmall)
                                Text(Formatters.date(transaction.dueDate), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Notes
            if (!transaction.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Catatan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transaction.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Receipt Attachment
            if (!transaction.receiptFileName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = transaction.receiptFileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = transaction.receiptSizeText ?: "Bukti tersimpan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onViewReceipt,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Buka Bukti", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share receipt button
                Button(
                    onClick = {
                        shareTransactionReceipt(context, transaction)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_share_receipt")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan Struk / Kuitansi (WhatsApp)", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditCategory,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ubah Kategori", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MoneyOutRed),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_delete_from_detail")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DetailRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun shareTransactionReceipt(context: Context, tx: TransactionEntity) {
    val title = when (tx.type) {
        "MODAL" -> "BUKTI PENAMBAHAN MODAL"
        "PENJUALAN", "JUAL" -> "STRUK PENJUALAN"
        "BELI" -> "BUKTI PEMBELIAN STOK"
        else -> "BUKTI BIAYA OPERASIONAL"
    }
    val message = buildString {
        appendLine("================================")
        appendLine("       SAKU USAHA - $title")
        appendLine("================================")
        appendLine("No. Ref   : #TX-${tx.id}")
        appendLine("Tanggal   : ${Formatters.date(tx.timestamp)} ${Formatters.timeOnly(tx.timestamp)}")
        appendLine("Kategori  : ${tx.category}")
        appendLine("Status    : ${tx.status} (${tx.paymentMethod})")
        if (!tx.productName.isNullOrBlank()) {
            appendLine("--------------------------------")
            appendLine("Barang    : ${tx.productName}")
        }
        if (!tx.contactName.isNullOrBlank()) {
            appendLine("Pihak     : ${tx.contactName}")
        }
        appendLine("--------------------------------")
        appendLine("TOTAL     : ${Formatters.rupiah(tx.amount)}")
        if (tx.paymentMethod == "HUTANG") {
            appendLine("Sisa Utang: ${Formatters.rupiah(tx.remainingDebt)}")
        }
        if (!tx.notes.isNullOrBlank()) {
            appendLine("Catatan   : ${tx.notes}")
        }
        appendLine("================================")
        appendLine("Terima kasih atas kepercayaannya!")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$title #TX-${tx.id}")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Struk Transaksi"))
}
