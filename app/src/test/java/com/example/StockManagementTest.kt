package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ProductEntity
import com.example.data.model.ProductIngredient
import com.example.data.model.RecipeHelper
import com.example.data.repository.SakuUsahaRepository
import com.example.ui.components.AiAssistantTopActionButton
import com.example.ui.components.LocalOpenAiAssistant
import com.example.ui.components.SakuUsahaTopHeader
import com.example.ui.screens.DashboardMenuChoiceFrame
import com.example.ui.screens.StockScreen
import com.example.ui.theme.SakuUsahaTheme
import com.example.ui.viewmodel.SakuUsahaViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StockManagementTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: SakuUsahaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SakuUsahaRepository(db.dao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `restockProduct updates stock weighted cost and recalculates composed product HPP`() = runBlocking {
        val bahanId = db.dao().insertProduct(
            ProductEntity(
                name = "Gula Pasir",
                categoryName = "Bahan",
                sellPrice = 0L,
                costPrice = 10_000L,
                stock = 2,
                minStockAlert = 5,
                unit = "kg",
                stockType = "BAHAN"
            )
        )

        val product = ProductEntity(
            name = "Es Teh Manis",
            categoryName = "Minuman",
            sellPrice = 5_000L,
            costPrice = 2_000L,
            stock = 10,
            minStockAlert = 5,
            isComposedProduct = true,
            recipeJson = RecipeHelper.toJson(
                listOf(
                    ProductIngredient(
                        materialId = bahanId,
                        materialName = "Gula Pasir",
                        quantityNeeded = 0.2,
                        unit = "kg",
                        unitCost = 10_000L
                    )
                )
            ),
            yieldQuantity = 1,
            unit = "cup",
            stockType = "PRODUK"
        )
        repository.saveProduct(product)

        // Restock 2 kg at Rp 14.000/kg -> weighted avg = (2*10000 + 2*14000)/4 = 12.000
        repository.restockProduct(bahanId, quantityToAdd = 2, purchasePricePerUnit = 14_000L)

        val updatedBahan = db.dao().getProductById(bahanId)!!
        assertEquals(4, updatedBahan.stock)
        assertEquals(12_000L, updatedBahan.costPrice)

        val allProds = repository.allProducts.first()
        val updatedDrink = allProds.first { it.name == "Es Teh Manis" }
        // 0.2 * 12.000 = 2.400
        assertEquals(2_400L, updatedDrink.costPrice)
    }

    @Test
    fun `lowStockProducts detects items across Produk Bahan and Inventaris`() = runBlocking {
        db.dao().insertProduct(
            ProductEntity(
                name = "Kopi Susu",
                categoryName = "Minuman",
                sellPrice = 12_000L,
                costPrice = 5_000L,
                stock = 2,
                minStockAlert = 5,
                stockType = "PRODUK"
            )
        )
        db.dao().insertProduct(
            ProductEntity(
                name = "Biji Kopi",
                categoryName = "Minuman",
                sellPrice = 0L,
                costPrice = 80_000L,
                stock = 1,
                minStockAlert = 3,
                unit = "kg",
                stockType = "BAHAN"
            )
        )
        db.dao().insertProduct(
            ProductEntity(
                name = "Grinder Kopi",
                categoryName = "Lainnya",
                sellPrice = 0L,
                costPrice = 500_000L,
                stock = 1,
                minStockAlert = 1,
                unit = "unit",
                stockType = "INVENTARIS"
            )
        )

        val lowStock = repository.lowStockProducts.first()
        assertEquals(3, lowStock.size)
        assertTrue(lowStock.any { it.stockType == "PRODUK" })
        assertTrue(lowStock.any { it.stockType == "BAHAN" })
        assertTrue(lowStock.any { it.stockType == "INVENTARIS" })
    }

    @Test
    fun `stockHub displays 3 circular options and triggers navigation callbacks`() {
        val viewModel = SakuUsahaViewModel(context.applicationContext as android.app.Application)
        var openedProduk = false
        var openedBahan = false
        var openedInventaris = false

        val sampleProducts = listOf(
            ProductEntity(id = 1, name = "Roti", categoryName = "Makanan", sellPrice = 8000, costPrice = 5000, stock = 2, minStockAlert = 5, stockType = "PRODUK"),
            ProductEntity(id = 2, name = "Tepung", categoryName = "Bahan", sellPrice = 0, costPrice = 12000, stock = 10, minStockAlert = 5, stockType = "BAHAN"),
            ProductEntity(id = 3, name = "Oven", categoryName = "Aset", sellPrice = 0, costPrice = 900000, stock = 1, minStockAlert = 1, stockType = "INVENTARIS")
        )

        composeRule.setContent {
            SakuUsahaTheme {
                StockScreen(
                    viewModel = viewModel,
                    products = sampleProducts,
                    onOpenStockProduk = { openedProduk = true },
                    onOpenStockBahan = { openedBahan = true },
                    onOpenStockInventaris = { openedInventaris = true },
                    onNavigateToCategories = {}
                )
            }
        }

        composeRule.onNodeWithTag("btn_circle_stock_produk").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("btn_circle_stock_bahan").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("btn_circle_stock_inventaris").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Stok yang Sedang Menipis").assertIsDisplayed()

        assertTrue(openedProduk)
        assertTrue(openedBahan)
        assertTrue(openedInventaris)
    }

    @Test
    fun `dashboardMenuChoiceFrame displays transaction choices and circular Produk inside frame`() {
        var openedProducts = false
        var quickType = ""
        composeRule.setContent {
            SakuUsahaTheme {
                DashboardMenuChoiceFrame(
                    productCount = 6,
                    onQuickRecord = { quickType = it },
                    onNavigateToProducts = { openedProducts = true }
                )
            }
        }

        composeRule.onNodeWithTag("frame_menu_pilihan").assertIsDisplayed()
        composeRule.onNodeWithText("Pencatatan Transaksi").assertDoesNotExist()
        composeRule.onNodeWithText("Menu Pilihan").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_quick_penjualan").assertIsDisplayed().performClick()
        assertEquals("PENJUALAN", quickType)
        composeRule.onNodeWithTag("btn_shortcut_products").assertIsDisplayed().performClick()
        assertTrue(openedProducts)
    }

    @Test
    fun `topRightAiAssistantButton triggers LocalOpenAiAssistant callback from header`() {
        var openedAiAssistant = false
        composeRule.setContent {
            SakuUsahaTheme {
                CompositionLocalProvider(
                    LocalOpenAiAssistant provides { openedAiAssistant = true }
                ) {
                    SakuUsahaTopHeader(
                        businessName = "Toko Maju",
                        subtitle = "Modal: Rp 5.000.000",
                        onAppearanceClick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("btn_top_ai_assistant").assertIsDisplayed().performClick()
        assertTrue(openedAiAssistant)
    }
}
