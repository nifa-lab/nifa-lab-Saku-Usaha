package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ReportExporter {

    fun generateAndSharePdf(
        context: Context,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        periodLabel: String,
        startTime: Long,
        endTime: Long
    ) {
        try {
            val file = createPdfFile(context, profile, transactions, periodLabel, startTime, endTime)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan - ${profile?.businessName ?: "SakuUsaha"}")
                putExtra(Intent.EXTRA_TEXT, "Berikut terlampir file PDF Laporan Keuangan periode $periodLabel dari ${profile?.businessName ?: "SakuUsaha"}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan PDF", uri)
            }

            val chooser = Intent.createChooser(shareIntent, "Unduh / Bagikan PDF Laporan").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan PDF", uri)
            }

            val resInfoList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateAndShareExcel(
        context: Context,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        periodLabel: String,
        startTime: Long,
        endTime: Long
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanStoreName = (profile?.businessName ?: "SakuUsaha").replace(" ", "_")
            val fileName = "Laporan_${cleanStoreName}_${System.currentTimeMillis()}.csv"
            val file = File(reportsDir, fileName)

            val totalIn = transactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
            val totalOut = transactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }
            val netProfit = totalIn - totalOut
            val margin = if (totalIn > 0) ((netProfit.toFloat() / totalIn.toFloat()) * 100).toInt() else 0

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Write UTF-8 BOM so Excel opens with proper encoding
                    writer.write("\uFEFF")

                    // Header Info
                    writer.write("REKAP LAPORAN KEUANGAN UMKM - SAKUUSAHA\n")
                    writer.write("Nama Usaha,${escapeCsv(profile?.businessName ?: "Toko Suka Maju")}\n")
                    writer.write("Pemilik,${escapeCsv(profile?.ownerName ?: "Budi Santoso")}\n")
                    writer.write("Periode,${escapeCsv("$periodLabel (${Formatters.date(startTime)} - ${Formatters.date(endTime)})")}\n")
                    writer.write("Tanggal Dibuat,${escapeCsv(Formatters.dateTime(System.currentTimeMillis()))}\n\n")

                    // Ringkasan Laba Rugi Table
                    writer.write("RINGKASAN LABA RUGI\n")
                    writer.write("Indikator,Nominal (Rp)\n")
                    writer.write("Total Pemasukan,${totalIn}\n")
                    writer.write("Total Pengeluaran,${totalOut}\n")
                    writer.write("Laba Bersih,${netProfit}\n")
                    writer.write("Margin Keuntungan,${margin}%\n\n")

                    // Daftar Transaksi Table
                    writer.write("RINCIAN TRANSAKSI\n")
                    writer.write("No,Tanggal,Waktu,Jenis,Arah,Kategori,Barang / Produk,Jumlah,Nominal (Rp),Metode Bayar,Pihak,Catatan\n")

                    transactions.forEachIndexed { index, tx ->
                        val date = Formatters.date(tx.timestamp)
                        val time = Formatters.timeOnly(tx.timestamp)
                        val prodName = tx.productName ?: "-"
                        val qty = if (tx.productName != null) tx.quantity.toString() else "-"
                        val contact = tx.contactName ?: "-"
                        val note = tx.notes ?: "-"

                        writer.write(
                            "${index + 1}," +
                                    "${escapeCsv(date)}," +
                                    "${escapeCsv(time)}," +
                                    "${escapeCsv(tx.type)}," +
                                    "${escapeCsv(tx.direction)}," +
                                    "${escapeCsv(tx.category)}," +
                                    "${escapeCsv(prodName)}," +
                                    "${escapeCsv(qty)}," +
                                    "${tx.amount}," +
                                    "${escapeCsv(tx.paymentMethod)}," +
                                    "${escapeCsv(contact)}," +
                                    "${escapeCsv(note)}\n"
                        )
                    }
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Excel/CSV - ${profile?.businessName ?: "SakuUsaha"}")
                putExtra(Intent.EXTRA_TEXT, "Terlampir rekap laporan keuangan format Excel/CSV periode $periodLabel.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan Excel", uri)
            }

            val chooser = Intent.createChooser(shareIntent, "Buka di Excel / Bagikan File CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan Excel", uri)
            }

            val resInfoList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat Excel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun printReport(
        context: Context,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        periodLabel: String,
        startTime: Long,
        endTime: Long
    ) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "Layanan Cetak tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show()
                return
            }

            val pdfFile = createPdfFile(context, profile, transactions, periodLabel, startTime, endTime)

            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder("Laporan_Keuangan.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()

                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(pdfFile).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            val cleanTitle = (profile?.businessName ?: "SakuUsaha").replace("[^a-zA-Z0-9_]".toRegex(), "_")
            printManager.print("Laporan_$cleanTitle", printAdapter, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mencetak: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPdfFile(
        context: Context,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        periodLabel: String,
        startTime: Long,
        endTime: Long
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val cleanStoreName = (profile?.businessName ?: "SakuUsaha").replace(" ", "_")
        val fileName = "Laporan_${cleanStoreName}_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)

        val totalIn = transactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
        val totalOut = transactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }
        val netProfit = totalIn - totalOut
        val margin = if (totalIn > 0) ((netProfit.toFloat() / totalIn.toFloat()) * 100).toInt() else 0

        // A4 page: 595 x 842 points
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val paintText = Paint().apply {
            color = Color.rgb(20, 32, 30) // TextInk
            isAntiAlias = true
        }
        val paintBrand = Paint().apply {
            color = Color.rgb(20, 108, 67) // BrandGreen #146C43
            isAntiAlias = true
        }
        val paintBox = Paint().apply {
            color = Color.rgb(245, 242, 233) // BrandSurfaceCream #F5F2E9
            isAntiAlias = true
        }
        val paintGreen = Paint().apply {
            color = Color.rgb(21, 128, 61) // MoneyInGreen
            isAntiAlias = true
        }
        val paintRed = Paint().apply {
            color = Color.rgb(214, 56, 74) // MoneyOutRed
            isAntiAlias = true
        }
        val paintLine = Paint().apply {
            color = Color.rgb(220, 224, 222)
            strokeWidth = 1f
        }

        // Header Banner Background
        canvas.drawRect(0f, 0f, 595f, 90f, paintBrand)

        // Store Title on Header
        paintText.color = Color.WHITE
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 20f
        canvas.drawText(profile?.businessName ?: "TOKO SUKA MAJU", 30f, 38f, paintText)

        paintText.textSize = 11f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Laporan Rekap Laba Rugi & Rekap Keuangan UMKM", 30f, 56f, paintText)
        canvas.drawText("Pemilik: ${profile?.ownerName ?: "Budi Santoso"} • SakuUsaha v1.2", 30f, 72f, paintText)

        // Right side info on Header
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Periode: $periodLabel", 565f, 42f, paintText)
        canvas.drawText("${Formatters.date(startTime)} - ${Formatters.date(endTime)}", 565f, 58f, paintText)
        paintText.textAlign = Paint.Align.LEFT

        // SECTION 1: Ringkasan Laba Rugi Cards
        var y = 115f
        paintText.color = Color.rgb(20, 32, 30)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 13f
        canvas.drawText("RINGKASAN LABA RUGI", 30f, y, paintText)

        y += 12f
        // Draw 3 Summary Boxes: Pemasukan, Pengeluaran, Laba Bersih
        val boxWidth = 168f
        val boxHeight = 58f

        // Box 1: Pemasukan
        canvas.drawRoundRect(30f, y, 30f + boxWidth, y + boxHeight, 8f, 8f, paintBox)
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL PEMASUKAN", 42f, y + 20f, paintText)
        paintGreen.textSize = 14f
        paintGreen.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Formatters.rupiah(totalIn), 42f, y + 42f, paintGreen)

        // Box 2: Pengeluaran
        val x2 = 30f + boxWidth + 15f
        canvas.drawRoundRect(x2, y, x2 + boxWidth, y + boxHeight, 8f, 8f, paintBox)
        canvas.drawText("TOTAL PENGELUARAN", x2 + 12f, y + 20f, paintText)
        paintRed.textSize = 14f
        paintRed.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Formatters.rupiah(totalOut), x2 + 12f, y + 42f, paintRed)

        // Box 3: Laba Bersih
        val x3 = x2 + boxWidth + 15f
        canvas.drawRoundRect(x3, y, x3 + boxWidth, y + boxHeight, 8f, 8f, paintBox)
        canvas.drawText("LABA BERSIH (MARGIN $margin%)", x3 + 12f, y + 20f, paintText)
        val profitPaint = if (netProfit >= 0) paintGreen else paintRed
        profitPaint.textSize = 14f
        profitPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Formatters.rupiah(netProfit), x3 + 12f, y + 42f, profitPaint)

        // SECTION 2: Tabel Transaksi
        y += boxHeight + 30f
        paintText.color = Color.rgb(20, 32, 30)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 13f
        canvas.drawText("DAFTAR TRANSAKSI", 30f, y, paintText)

        y += 14f
        // Table Header
        paintBox.color = Color.rgb(235, 240, 238)
        canvas.drawRect(30f, y, 565f, y + 22f, paintBox)

        paintText.color = Color.rgb(20, 32, 30)
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NO", 36f, y + 14f, paintText)
        canvas.drawText("TANGGAL", 65f, y + 14f, paintText)
        canvas.drawText("JENIS", 145f, y + 14f, paintText)
        canvas.drawText("DESKRIPSI / BARANG", 205f, y + 14f, paintText)
        canvas.drawText("METODE", 370f, y + 14f, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("NOMINAL (RP)", 555f, y + 14f, paintText)
        paintText.textAlign = Paint.Align.LEFT

        y += 22f

        // Table Rows (print up to 20 recent transactions cleanly on page 1)
        val itemsToPrint = transactions.take(20)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.textSize = 8.5f

        if (itemsToPrint.isEmpty()) {
            paintText.textAlign = Paint.Align.CENTER
            paintText.color = Color.GRAY
            canvas.drawText("Belum ada catatan transaksi pada periode ini", 297f, y + 25f, paintText)
            paintText.textAlign = Paint.Align.LEFT
            paintText.color = Color.rgb(20, 32, 30)
            y += 40f
        } else {
            itemsToPrint.forEachIndexed { i, tx ->
                if (i % 2 == 1) {
                    paintBox.color = Color.rgb(250, 250, 250)
                    canvas.drawRect(30f, y, 565f, y + 18f, paintBox)
                }
                canvas.drawLine(30f, y + 18f, 565f, y + 18f, paintLine)

                val isInc = tx.direction == "MASUK"
                val itemAmountPaint = if (isInc) paintGreen else paintRed
                itemAmountPaint.textSize = 8.5f
                itemAmountPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                canvas.drawText("${i + 1}", 36f, y + 12f, paintText)
                canvas.drawText(Formatters.date(tx.timestamp), 65f, y + 12f, paintText)
                canvas.drawText(tx.type, 145f, y + 12f, paintText)

                val desc = (tx.productName ?: tx.category).let { if (it.length > 28) it.take(26) + ".." else it }
                canvas.drawText(desc, 205f, y + 12f, paintText)
                canvas.drawText(tx.paymentMethod, 370f, y + 12f, paintText)

                itemAmountPaint.textAlign = Paint.Align.RIGHT
                val prefix = if (isInc) "+ " else "- "
                canvas.drawText(prefix + Formatters.rupiah(tx.amount), 555f, y + 12f, itemAmountPaint)
                itemAmountPaint.textAlign = Paint.Align.LEFT

                y += 18f
            }
        }

        // Footer / Signature Area
        y = 780f
        canvas.drawLine(30f, y, 565f, y, paintLine)
        paintText.textSize = 8f
        paintText.color = Color.GRAY
        canvas.drawText("Dokumen ini dicetak otomatis melalui SakuUsaha pada ${Formatters.dateTime(System.currentTimeMillis())}", 30f, y + 14f, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Halaman 1 dari 1", 565f, y + 14f, paintText)
        paintText.textAlign = Paint.Align.LEFT

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return file
    }

    // ==========================================
    // CASH FLOW (ARUS KAS) EXPORT
    // ==========================================

    fun generateAndShareCashFlowPdf(
        context: Context,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        periodLabel: String,
        startTime: Long,
        endTime: Long,
        startingCash: Long
    ) {
        try {
            val file = createCashFlowPdfFile(context, profile, transactions, periodLabel, startTime, endTime, startingCash)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Arus Kas - ${profile?.businessName ?: "SakuUsaha"}")
                putExtra(Intent.EXTRA_TEXT, "Terlampir Laporan Arus Kas periode $periodLabel dari ${profile?.businessName ?: "SakuUsaha"}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan Arus Kas PDF", uri)
            }

            val chooser = Intent.createChooser(shareIntent, "Unduh / Bagikan PDF Arus Kas").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan Arus Kas PDF", uri)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF Arus Kas: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateAndShareCashFlowExcel(
        context: Context,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        periodLabel: String,
        startTime: Long,
        endTime: Long,
        startingCash: Long
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanStoreName = (profile?.businessName ?: "SakuUsaha").replace(" ", "_")
            val fileName = "Laporan_Arus_Kas_${cleanStoreName}_${System.currentTimeMillis()}.csv"
            val file = File(reportsDir, fileName)

            val totalIn = transactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
            val totalOut = transactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }
            val netCash = totalIn - totalOut
            val endingCash = startingCash + netCash

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write("LAPORAN ARUS KAS UMKM - SAKUUSAHA\n")
                    writer.write("Nama Usaha,${escapeCsv(profile?.businessName ?: "Toko Suka Maju")}\n")
                    writer.write("Pemilik,${escapeCsv(profile?.ownerName ?: "Budi Santoso")}\n")
                    writer.write("Periode,${escapeCsv("$periodLabel (${Formatters.date(startTime)} - ${Formatters.date(endTime)})")}\n")
                    writer.write("Tanggal Dibuat,${escapeCsv(Formatters.dateTime(System.currentTimeMillis()))}\n\n")

                    writer.write("RINGKASAN POSISI KAS\n")
                    writer.write("Indikator,Nominal (Rp)\n")
                    writer.write("Saldo Kas Awal Usaha,${startingCash}\n")
                    writer.write("Total Arus Kas Masuk,${totalIn}\n")
                    writer.write("Total Arus Kas Keluar,${totalOut}\n")
                    writer.write("Arus Kas Bersih (Surplus/Defisit),${netCash}\n")
                    writer.write("Saldo Kas Akhir Periode,${endingCash}\n\n")

                    writer.write("RINCIAN MUTASI KAS\n")
                    writer.write("No,Tanggal,Waktu,Arah,Kategori,Metode Bayar,Nominal (Rp),Pihak,Keterangan\n")

                    transactions.forEachIndexed { i, tx ->
                        val date = Formatters.date(tx.timestamp)
                        val time = Formatters.timeOnly(tx.timestamp)
                        val contact = tx.contactName ?: "-"
                        val note = tx.notes ?: (tx.productName ?: "-")
                        writer.write(
                            "${i + 1}," +
                                    "${escapeCsv(date)}," +
                                    "${escapeCsv(time)}," +
                                    "${escapeCsv(tx.direction)}," +
                                    "${escapeCsv(tx.category)}," +
                                    "${escapeCsv(tx.paymentMethod)}," +
                                    "${tx.amount}," +
                                    "${escapeCsv(contact)}," +
                                    "${escapeCsv(note)}\n"
                        )
                    }
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Arus Kas Excel/CSV - ${profile?.businessName ?: "SakuUsaha"}")
                putExtra(Intent.EXTRA_TEXT, "Terlampir rekap laporan arus kas format Excel/CSV periode $periodLabel.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan Excel", uri)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Buka di Excel / Bagikan CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat Excel Arus Kas: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createCashFlowPdfFile(
        context: Context,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        periodLabel: String,
        startTime: Long,
        endTime: Long,
        startingCash: Long
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val cleanStoreName = (profile?.businessName ?: "SakuUsaha").replace(" ", "_")
        val fileName = "Laporan_Arus_Kas_${cleanStoreName}_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)

        val totalIn = transactions.filter { it.direction == "MASUK" }.sumOf { it.amount }
        val totalOut = transactions.filter { it.direction == "KELUAR" }.sumOf { it.amount }
        val netCash = totalIn - totalOut
        val endingCash = startingCash + netCash

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paintText = Paint().apply { isAntiAlias = true; color = Color.rgb(20, 32, 30) }
        val paintBrand = Paint().apply { isAntiAlias = true; color = Color.rgb(20, 108, 67) }
        val paintBox = Paint().apply { isAntiAlias = true; color = Color.rgb(245, 242, 233) }
        val paintGreen = Paint().apply { isAntiAlias = true; color = Color.rgb(21, 128, 61) }
        val paintRed = Paint().apply { isAntiAlias = true; color = Color.rgb(214, 56, 74) }
        val paintLine = Paint().apply { color = Color.rgb(220, 224, 222); strokeWidth = 1f }

        // Header
        canvas.drawRect(0f, 0f, 595f, 90f, paintBrand)
        paintText.color = Color.WHITE
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 20f
        canvas.drawText(profile?.businessName ?: "TOKO SUKA MAJU", 30f, 38f, paintText)

        paintText.textSize = 11f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Laporan Arus Kas & Likuiditas Usaha", 30f, 56f, paintText)
        canvas.drawText("Pemilik: ${profile?.ownerName ?: "Budi Santoso"} • SakuUsaha", 30f, 72f, paintText)

        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Periode: $periodLabel", 565f, 42f, paintText)
        canvas.drawText("${Formatters.date(startTime)} - ${Formatters.date(endTime)}", 565f, 58f, paintText)
        paintText.textAlign = Paint.Align.LEFT

        // Summary Boxes
        var y = 115f
        paintText.color = Color.rgb(20, 32, 30)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 13f
        canvas.drawText("POSISI & ARUS KAS", 30f, y, paintText)

        y += 12f
        val boxW = 170f
        val boxH = 50f

        // Box 1: Saldo Kas Awal
        canvas.drawRoundRect(30f, y, 30f + boxW, y + boxH, 8f, 8f, paintBox)
        paintText.textSize = 9f
        paintText.color = Color.GRAY
        canvas.drawText("Saldo Kas Awal Usaha", 40f, y + 16f, paintText)
        paintText.textSize = 12f
        paintText.color = Color.rgb(20, 32, 30)
        canvas.drawText(Formatters.rupiah(startingCash), 40f, y + 36f, paintText)

        // Box 2: Kas Masuk & Keluar
        val x2 = 30f + boxW + 12f
        canvas.drawRoundRect(x2, y, x2 + boxW, y + boxH, 8f, 8f, paintBox)
        paintText.textSize = 9f
        paintText.color = Color.GRAY
        canvas.drawText("Arus Kas Masuk / Keluar", x2 + 10f, y + 16f, paintText)
        paintGreen.textSize = 10f
        paintGreen.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("+ " + Formatters.rupiah(totalIn), x2 + 10f, y + 36f, paintGreen)
        paintRed.textSize = 10f
        paintRed.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintRed.textAlign = Paint.Align.RIGHT
        canvas.drawText("- " + Formatters.rupiah(totalOut), x2 + boxW - 10f, y + 36f, paintRed)
        paintRed.textAlign = Paint.Align.LEFT

        // Box 3: Saldo Kas Akhir
        val x3 = x2 + boxW + 12f
        canvas.drawRoundRect(x3, y, x3 + boxW, y + boxH, 8f, 8f, paintBox)
        paintText.textSize = 9f
        paintText.color = Color.GRAY
        canvas.drawText("Saldo Kas Akhir Tersedia", x3 + 10f, y + 16f, paintText)
        paintBrand.textSize = 12f
        paintBrand.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Formatters.rupiah(endingCash), x3 + 10f, y + 36f, paintBrand)

        y += 70f
        // Table Header
        paintText.color = Color.rgb(20, 32, 30)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 13f
        canvas.drawText("RINCIAN ARUS KAS", 30f, y, paintText)

        y += 12f
        paintBox.color = Color.rgb(235, 240, 238)
        canvas.drawRect(30f, y, 565f, y + 22f, paintBox)
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("No", 36f, y + 15f, paintText)
        canvas.drawText("Tanggal", 65f, y + 15f, paintText)
        canvas.drawText("Arah", 145f, y + 15f, paintText)
        canvas.drawText("Kategori / Deskripsi", 205f, y + 15f, paintText)
        canvas.drawText("Metode", 370f, y + 15f, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Nominal (Rp)", 555f, y + 15f, paintText)
        paintText.textAlign = Paint.Align.LEFT
        y += 22f

        transactions.take(24).forEachIndexed { i, tx ->
            if (i % 2 == 1) {
                paintBox.color = Color.rgb(250, 250, 250)
                canvas.drawRect(30f, y, 565f, y + 18f, paintBox)
            }
            canvas.drawLine(30f, y + 18f, 565f, y + 18f, paintLine)

            val isInc = tx.direction == "MASUK"
            val itemAmountPaint = if (isInc) paintGreen else paintRed
            itemAmountPaint.textSize = 8.5f
            itemAmountPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("${i + 1}", 36f, y + 12f, paintText)
            canvas.drawText(Formatters.date(tx.timestamp), 65f, y + 12f, paintText)
            canvas.drawText(if (isInc) "Masuk" else "Keluar", 145f, y + 12f, paintText)
            val desc = (tx.productName ?: tx.category).let { if (it.length > 28) it.take(26) + ".." else it }
            canvas.drawText(desc, 205f, y + 12f, paintText)
            canvas.drawText(tx.paymentMethod, 370f, y + 12f, paintText)

            itemAmountPaint.textAlign = Paint.Align.RIGHT
            val prefix = if (isInc) "+ " else "- "
            canvas.drawText(prefix + Formatters.rupiah(tx.amount), 555f, y + 12f, itemAmountPaint)
            itemAmountPaint.textAlign = Paint.Align.LEFT
            y += 18f
        }

        // Footer
        canvas.drawLine(30f, 780f, 565f, 780f, paintLine)
        paintText.textSize = 8f
        paintText.color = Color.GRAY
        canvas.drawText("Dokumen Arus Kas dicetak melalui SakuUsaha pada ${Formatters.dateTime(System.currentTimeMillis())}", 30f, 794f, paintText)

        pdfDocument.finishPage(page)
        FileOutputStream(file).use { fos -> pdfDocument.writeTo(fos) }
        pdfDocument.close()
        return file
    }

    // ==========================================
    // STOCK & INVENTORY VALUATION EXPORT
    // ==========================================

    fun generateAndShareStockPdf(
        context: Context,
        profile: BusinessProfileEntity?,
        products: List<ProductEntity>
    ) {
        try {
            val file = createStockPdfFile(context, profile, products)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Nilai Stok & Aset - ${profile?.businessName ?: "SakuUsaha"}")
                putExtra(Intent.EXTRA_TEXT, "Terlampir Laporan Nilai Stok & Inventaris Toko dari ${profile?.businessName ?: "SakuUsaha"}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan Stok PDF", uri)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Unduh / Bagikan PDF Stok"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF Stok: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateAndShareStockExcel(
        context: Context,
        profile: BusinessProfileEntity?,
        products: List<ProductEntity>
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanStoreName = (profile?.businessName ?: "SakuUsaha").replace(" ", "_")
            val fileName = "Laporan_Stok_Aset_${cleanStoreName}_${System.currentTimeMillis()}.csv"
            val file = File(reportsDir, fileName)

            val totalCostValuation = products.sumOf { it.stock * it.costPrice }
            val totalSellValuation = products.sumOf { it.stock * it.sellPrice }
            val potentialProfit = totalSellValuation - totalCostValuation
            val totalUnits = products.sumOf { it.stock }

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write("LAPORAN VALUASI STOK & ASET INVENTARIS - SAKUUSAHA\n")
                    writer.write("Nama Usaha,${escapeCsv(profile?.businessName ?: "Toko Suka Maju")}\n")
                    writer.write("Pemilik,${escapeCsv(profile?.ownerName ?: "Budi Santoso")}\n")
                    writer.write("Tanggal Dibuat,${escapeCsv(Formatters.dateTime(System.currentTimeMillis()))}\n\n")

                    writer.write("RINGKASAN ASET STOK\n")
                    writer.write("Indikator,Nilai\n")
                    writer.write("Total Nilai Modal Stok (Rp),${totalCostValuation}\n")
                    writer.write("Potensi Omzet Penjualan (Rp),${totalSellValuation}\n")
                    writer.write("Potensi Keuntungan Kotor (Rp),${potentialProfit}\n")
                    writer.write("Total Fisik Unit Barang,${totalUnits} pcs\n")
                    writer.write("Jumlah SKU / Variasi Produk,${products.size} produk\n\n")

                    writer.write("DAFTAR STOK BARANG\n")
                    writer.write("No,Nama Produk,Kategori,Sisa Stok (pcs),Status,Harga Modal (@),Total Nilai Modal (Rp),Harga Jual (@),Potensi Omzet (Rp),Potensi Margin (%)\n")

                    products.forEachIndexed { i, p ->
                        val costTotal = p.stock * p.costPrice
                        val sellTotal = p.stock * p.sellPrice
                        val status = if (p.stock <= 0) "Habis" else if (p.stock <= p.minStockAlert) "Menipis" else "Aman"
                        val margin = if (p.sellPrice > 0) (((p.sellPrice - p.costPrice).toFloat() / p.sellPrice.toFloat()) * 100).toInt() else 0

                        writer.write(
                            "${i + 1}," +
                                    "${escapeCsv(p.name)}," +
                                    "${escapeCsv(p.categoryName)}," +
                                    "${p.stock}," +
                                    "${escapeCsv(status)}," +
                                    "${p.costPrice}," +
                                    "${costTotal}," +
                                    "${p.sellPrice}," +
                                    "${sellTotal}," +
                                    "${margin}%\n"
                        )
                    }
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Stok Excel/CSV - ${profile?.businessName ?: "SakuUsaha"}")
                putExtra(Intent.EXTRA_TEXT, "Terlampir rekap laporan stok barang format Excel/CSV.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("Laporan Excel", uri)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Buka di Excel / Bagikan CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat Excel Stok: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createStockPdfFile(
        context: Context,
        profile: BusinessProfileEntity?,
        products: List<ProductEntity>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val cleanStoreName = (profile?.businessName ?: "SakuUsaha").replace(" ", "_")
        val fileName = "Laporan_Stok_${cleanStoreName}_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)

        val totalCostValuation = products.sumOf { it.stock * it.costPrice }
        val totalSellValuation = products.sumOf { it.stock * it.sellPrice }
        val potentialProfit = totalSellValuation - totalCostValuation
        val totalUnits = products.sumOf { it.stock }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paintText = Paint().apply { isAntiAlias = true; color = Color.rgb(20, 32, 30) }
        val paintBrand = Paint().apply { isAntiAlias = true; color = Color.rgb(20, 108, 67) }
        val paintBox = Paint().apply { isAntiAlias = true; color = Color.rgb(245, 242, 233) }
        val paintGreen = Paint().apply { isAntiAlias = true; color = Color.rgb(21, 128, 61) }
        val paintLine = Paint().apply { color = Color.rgb(220, 224, 222); strokeWidth = 1f }

        // Header
        canvas.drawRect(0f, 0f, 595f, 90f, paintBrand)
        paintText.color = Color.WHITE
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 20f
        canvas.drawText(profile?.businessName ?: "TOKO SUKA MAJU", 30f, 38f, paintText)

        paintText.textSize = 11f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Laporan Valuasi Aset Stok & Inventaris Toko", 30f, 56f, paintText)
        canvas.drawText("Pemilik: ${profile?.ownerName ?: "Budi Santoso"} • SakuUsaha", 30f, 72f, paintText)

        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total: ${products.size} SKU ($totalUnits pcs)", 565f, 42f, paintText)
        canvas.drawText(Formatters.date(System.currentTimeMillis()), 565f, 58f, paintText)
        paintText.textAlign = Paint.Align.LEFT

        // Summary Boxes
        var y = 115f
        paintText.color = Color.rgb(20, 32, 30)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 13f
        canvas.drawText("VALUASI ASET STOK", 30f, y, paintText)

        y += 12f
        val boxW = 170f
        val boxH = 50f

        // Box 1: Total Modal Stok
        canvas.drawRoundRect(30f, y, 30f + boxW, y + boxH, 8f, 8f, paintBox)
        paintText.textSize = 9f
        paintText.color = Color.GRAY
        canvas.drawText("Total Nilai Modal Stok", 40f, y + 16f, paintText)
        paintText.textSize = 12f
        paintText.color = Color.rgb(20, 32, 30)
        canvas.drawText(Formatters.rupiah(totalCostValuation), 40f, y + 36f, paintText)

        // Box 2: Potensi Omzet
        val x2 = 30f + boxW + 12f
        canvas.drawRoundRect(x2, y, x2 + boxW, y + boxH, 8f, 8f, paintBox)
        paintText.textSize = 9f
        paintText.color = Color.GRAY
        canvas.drawText("Potensi Omzet Penjualan", x2 + 10f, y + 16f, paintText)
        paintGreen.textSize = 12f
        paintGreen.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Formatters.rupiah(totalSellValuation), x2 + 10f, y + 36f, paintGreen)

        // Box 3: Potensi Margin Laba
        val x3 = x2 + boxW + 12f
        canvas.drawRoundRect(x3, y, x3 + boxW, y + boxH, 8f, 8f, paintBox)
        paintText.textSize = 9f
        paintText.color = Color.GRAY
        canvas.drawText("Potensi Keuntungan Kotor", x3 + 10f, y + 16f, paintText)
        paintBrand.textSize = 12f
        paintBrand.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(Formatters.rupiah(potentialProfit), x3 + 10f, y + 36f, paintBrand)

        y += 70f
        // Table Header
        paintText.color = Color.rgb(20, 32, 30)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = 13f
        canvas.drawText("DAFTAR STOK BARANG", 30f, y, paintText)

        y += 12f
        paintBox.color = Color.rgb(235, 240, 238)
        canvas.drawRect(30f, y, 565f, y + 22f, paintBox)
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("No", 36f, y + 15f, paintText)
        canvas.drawText("Nama Produk", 65f, y + 15f, paintText)
        canvas.drawText("Kategori", 215f, y + 15f, paintText)
        canvas.drawText("Stok", 320f, y + 15f, paintText)
        canvas.drawText("Harga Modal", 370f, y + 15f, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Nilai Modal (Rp)", 555f, y + 15f, paintText)
        paintText.textAlign = Paint.Align.LEFT
        y += 22f

        products.take(24).forEachIndexed { i, p ->
            if (i % 2 == 1) {
                paintBox.color = Color.rgb(250, 250, 250)
                canvas.drawRect(30f, y, 565f, y + 18f, paintBox)
            }
            canvas.drawLine(30f, y + 18f, 565f, y + 18f, paintLine)

            canvas.drawText("${i + 1}", 36f, y + 12f, paintText)
            val name = if (p.name.length > 24) p.name.take(22) + ".." else p.name
            canvas.drawText(name, 65f, y + 12f, paintText)
            canvas.drawText(p.categoryName, 215f, y + 12f, paintText)
            canvas.drawText("${p.stock} pcs", 320f, y + 12f, paintText)
            canvas.drawText(Formatters.rupiah(p.costPrice), 370f, y + 12f, paintText)

            paintText.textAlign = Paint.Align.RIGHT
            canvas.drawText(Formatters.rupiah(p.stock * p.costPrice), 555f, y + 12f, paintText)
            paintText.textAlign = Paint.Align.LEFT
            y += 18f
        }

        // Footer
        canvas.drawLine(30f, 780f, 565f, 780f, paintLine)
        paintText.textSize = 8f
        paintText.color = Color.GRAY
        canvas.drawText("Dokumen Nilai Stok dicetak melalui SakuUsaha pada ${Formatters.dateTime(System.currentTimeMillis())}", 30f, 794f, paintText)

        pdfDocument.finishPage(page)
        FileOutputStream(file).use { fos -> pdfDocument.writeTo(fos) }
        pdfDocument.close()
        return file
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}

