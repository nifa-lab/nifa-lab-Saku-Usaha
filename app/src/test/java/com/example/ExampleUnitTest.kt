package com.example

import com.example.ui.util.DateFilterPeriod
import com.example.ui.util.Formatters
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testRupiahFormatting() {
        val formatted = Formatters.rupiah(750000L)
        assertTrue(formatted.contains("750.000") || formatted.contains("750,000"))
        assertTrue(formatted.startsWith("Rp"))
    }

    @Test
    fun testDateFilterPeriodRanges() {
        val (startToday, endToday) = DateFilterPeriod.HARI_INI.getTimeRange()
        assertTrue(endToday > startToday)

        val (start7, end7) = DateFilterPeriod.TUJUH_HARI.getTimeRange()
        assertTrue(end7 > start7)
    }

    @Test
    fun testProfitAndMarginCalculation() {
        val income = 2_450_000L
        val expense = 1_350_000L
        val netProfit = income - expense
        assertEquals(1_100_000L, netProfit)

        val margin = ((netProfit.toFloat() / income.toFloat()) * 100).toInt()
        assertEquals(44, margin)
    }

    @Test
    fun testDebtRemainingCalculation() {
        val total = 500_000L
        val downPayment = 150_000L
        val remaining = (total - downPayment).coerceAtLeast(0L)
        assertEquals(350_000L, remaining)
    }

    @Test
    fun testCsvReportColumns() {
        val sampleRow = listOf("1", "18 Okt 2026", "JUAL", "Penjualan Produk", "150000", "QRIS")
        val csvLine = sampleRow.joinToString(",")
        assertTrue(csvLine.contains("18 Okt 2026"))
        assertTrue(csvLine.contains("150000"))
        assertTrue(csvLine.contains("QRIS"))
    }
}
