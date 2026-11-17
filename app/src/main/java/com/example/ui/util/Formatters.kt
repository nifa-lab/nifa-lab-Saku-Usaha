package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val idLocale = Locale("id", "ID")
    private val numberFormat = NumberFormat.getNumberInstance(idLocale)

    @Synchronized
    fun rupiah(amount: Long): String {
        return "Rp " + numberFormat.format(amount)
    }

    fun date(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy", idLocale)
        return sdf.format(Date(timestamp))
    }

    fun dateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", idLocale)
        return sdf.format(Date(timestamp))
    }

    fun timeOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", idLocale)
        return sdf.format(Date(timestamp))
    }

    fun dayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE", idLocale)
        return sdf.format(Date(timestamp))
    }

    fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isPastDueDate(dueDate: Long?): Boolean {
        if (dueDate == null) return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dueDate < today
    }

    fun openWhatsAppMessage(context: Context, phone: String?, message: String) {
        val cleanPhone = (phone ?: "").replace("[^0-9]".toRegex(), "")
        val formattedPhone = when {
            cleanPhone.startsWith("0") -> "62" + cleanPhone.substring(1)
            cleanPhone.startsWith("62") -> cleanPhone
            else -> "62$cleanPhone"
        }
        val encodedMessage = Uri.encode(message)
        val url = if (formattedPhone.length > 5) {
            "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to standard share sheet
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Kirim Pengingat").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun shareReportText(context: Context, reportContent: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan SakuUsaha")
            putExtra(Intent.EXTRA_TEXT, reportContent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Bagikan Laporan Keuangan").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

enum class DateFilterPeriod(val label: String) {
    HARI_INI("Hari ini"),
    KEMARIN("Kemarin"),
    TUJUH_HARI("7 hari"),
    TIGA_PULUH_HARI("30 hari"),
    BULAN_INI("Bulan ini"),
    BULAN_LALU("Bulan lalu"),
    TAHUN_INI("Tahun ini"),
    KUSTOM("Kustom");

    fun getTimeRange(customStart: Long = 0, customEnd: Long = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (this) {
            HARI_INI -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            KEMARIN -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            TUJUH_HARI -> {
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                val calStart = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(calStart.timeInMillis, end)
            }
            TIGA_PULUH_HARI -> {
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                val calStart = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(calStart.timeInMillis, end)
            }
            BULAN_INI -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val calEnd = Calendar.getInstance()
                calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH))
                calEnd.set(Calendar.HOUR_OF_DAY, 23)
                calEnd.set(Calendar.MINUTE, 59)
                calEnd.set(Calendar.SECOND, 59)
                calEnd.set(Calendar.MILLISECOND, 999)
                Pair(start, calEnd.timeInMillis)
            }
            BULAN_LALU -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            TAHUN_INI -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val calEnd = Calendar.getInstance()
                calEnd.set(Calendar.DAY_OF_YEAR, calEnd.getActualMaximum(Calendar.DAY_OF_YEAR))
                calEnd.set(Calendar.HOUR_OF_DAY, 23)
                calEnd.set(Calendar.MINUTE, 59)
                calEnd.set(Calendar.SECOND, 59)
                calEnd.set(Calendar.MILLISECOND, 999)
                Pair(start, calEnd.timeInMillis)
            }
            KUSTOM -> {
                if (customStart > 0 && customEnd >= customStart) {
                    Pair(customStart, customEnd)
                } else {
                    TUJUH_HARI.getTimeRange()
                }
            }
        }
    }
}
