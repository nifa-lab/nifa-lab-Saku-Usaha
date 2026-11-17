package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.ai.configureAppCheck
import com.example.ui.components.LocalOpenAiAssistant
import com.example.ui.components.SakuUsahaBottomNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.SakuUsahaTheme
import com.example.ui.viewmodel.SakuUsahaViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SakuUsahaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before rendering UI or creating ViewModels
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val profile by viewModel.businessProfile.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val activeSubScreen by viewModel.activeSubScreen.collectAsState()
            val showRecordModal by viewModel.showRecordModal.collectAsState()
            val recordInitialType by viewModel.recordInitialType.collectAsState()
            val toastMessage by viewModel.toastMessage.collectAsState()

            val transactions by viewModel.allTransactions.collectAsState()
            val lowStockProducts by viewModel.lowStockProducts.collectAsState()
            val allProducts by viewModel.allProducts.collectAsState()
            val activeDebts by viewModel.activeDebts.collectAsState()
            val allDebts by viewModel.allDebts.collectAsState()

            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            // Trigger Toast / Snackbar feedback
            LaunchedEffect(toastMessage) {
                toastMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            // Parse customized brand accent color
            val brandColor = remember(profile?.themeColorHex) {
                try {
                    Color(android.graphics.Color.parseColor(profile?.themeColorHex ?: "#146C43"))
                } catch (_: Exception) {
                    BrandGreenPrimary
                }
            }

            val themeMode = profile?.themeMode ?: "LIGHT"

            SakuUsahaTheme(
                brandColor = brandColor,
                themeMode = themeMode
            ) {
                // If subscreen is active, handle Back key
                BackHandler(enabled = activeSubScreen != null) {
                    viewModel.navigateBack()
                }

                if (profile != null && !profile!!.isOnboarded) {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            viewModel.updateProfile(profile!!.copy(isOnboarded = true))
                        }
                    )
                } else if (profile != null && !profile!!.isLoggedIn) {
                    AuthScreen(
                        onLoginSuccess = { owner, business, type ->
                            viewModel.updateProfile(
                                profile!!.copy(
                                    ownerName = owner,
                                    businessName = business,
                                    businessType = type,
                                    isLoggedIn = true,
                                    isOnboarded = true
                                )
                            )
                        }
                    )
                } else {
                    CompositionLocalProvider(
                        LocalOpenAiAssistant provides { viewModel.navigateToSubScreen("AI_ASSISTANT") }
                    ) {
                        Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (activeSubScreen == null || activeSubScreen == "PRODUCTS") {
                                SakuUsahaBottomNavigationBar(
                                    currentTab = if (activeSubScreen == "PRODUCTS") -1 else currentTab,
                                    onTabSelected = { tabIndex ->
                                        viewModel.selectTab(tabIndex)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeSubScreen) {
                                "REPORTS" -> {
                                    ReportsScreen(
                                        viewModel = viewModel,
                                        profile = profile,
                                        transactions = transactions,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "DEBTS" -> {
                                    DebtsScreen(
                                        viewModel = viewModel,
                                        debts = allDebts,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "APPEARANCE" -> {
                                    AppearanceScreen(
                                        viewModel = viewModel,
                                        profile = profile,
                                        onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") },
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "CATEGORIES" -> {
                                    CategoriesManageScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "TRANSACTION_TYPE_DETAIL" -> {
                                    val selectedType by viewModel.selectedTransactionTypeDetail.collectAsState()
                                    TransactionTypeDetailScreen(
                                        viewModel = viewModel,
                                        transactions = transactions,
                                        typeKey = selectedType,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "PERMISSIONS" -> {
                                    AppPermissionsScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "PRODUCTS" -> {
                                    ProductsScreen(
                                        viewModel = viewModel,
                                        products = allProducts,
                                        onOpenProductList = { viewModel.navigateToSubScreen("PRODUCTS_LIST") },
                                        onOpenHppRecipe = { viewModel.navigateToSubScreen("PRODUCTS_HPP") },
                                        onOpenProductTransactions = { viewModel.navigateToSubScreen("PRODUCTS_TRANSACTIONS") },
                                        onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") },
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "PRODUCTS_LIST" -> {
                                    ProductListScreen(
                                        viewModel = viewModel,
                                        products = allProducts,
                                        onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") },
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "PRODUCTS_HPP" -> {
                                    ProductHppRecipeScreen(
                                        viewModel = viewModel,
                                        products = allProducts,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "PRODUCTS_TRANSACTIONS" -> {
                                    ProductTransactionsScreen(
                                        viewModel = viewModel,
                                        products = allProducts,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "STOCK_PRODUK" -> {
                                    StockProdukScreen(
                                        viewModel = viewModel,
                                        allProducts = allProducts,
                                        onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") },
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "STOCK_BAHAN" -> {
                                    StockBahanScreen(
                                        viewModel = viewModel,
                                        allProducts = allProducts,
                                        onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") },
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "STOCK_INVENTARIS" -> {
                                    StockInventarisScreen(
                                        viewModel = viewModel,
                                        allProducts = allProducts,
                                        onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") },
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                "AI_ASSISTANT" -> {
                                    AiAssistantScreen(
                                        profile = profile,
                                        transactions = transactions,
                                        products = allProducts,
                                        lowStockProducts = lowStockProducts,
                                        activeDebts = activeDebts,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                }
                                else -> {
                                    when (currentTab) {
                                        0 -> HomeScreen(
                                            viewModel = viewModel,
                                            profile = profile,
                                            transactions = transactions,
                                            products = allProducts,
                                            lowStockProducts = lowStockProducts,
                                            activeDebts = activeDebts,
                                            onNavigateToTransactions = { viewModel.selectTab(1) },
                                            onNavigateToProducts = { viewModel.navigateToSubScreen("PRODUCTS") },
                                            onNavigateToStock = { viewModel.selectTab(3) },
                                            onNavigateToDebts = { viewModel.navigateToSubScreen("DEBTS") },
                                            onNavigateToAppearance = { viewModel.navigateToSubScreen("APPEARANCE") }
                                        )
                                        1 -> TransactionsScreen(
                                            viewModel = viewModel,
                                            transactions = transactions
                                        )
                                        3 -> StockScreen(
                                            viewModel = viewModel,
                                            products = allProducts,
                                            onOpenStockProduk = { viewModel.navigateToSubScreen("STOCK_PRODUK") },
                                            onOpenStockBahan = { viewModel.navigateToSubScreen("STOCK_BAHAN") },
                                            onOpenStockInventaris = { viewModel.navigateToSubScreen("STOCK_INVENTARIS") },
                                            onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") }
                                        )
                                        4 -> MoreSettingsScreen(
                                            viewModel = viewModel,
                                            profile = profile,
                                            onNavigateToProducts = { viewModel.navigateToSubScreen("PRODUCTS") },
                                            onNavigateToAiAssistant = { viewModel.navigateToSubScreen("AI_ASSISTANT") },
                                            onNavigateToReports = { viewModel.navigateToSubScreen("REPORTS") },
                                            onNavigateToDebts = { viewModel.navigateToSubScreen("DEBTS") },
                                            onNavigateToAppearance = { viewModel.navigateToSubScreen("APPEARANCE") },
                                            onNavigateToCategories = { viewModel.navigateToSubScreen("CATEGORIES") },
                                            onNavigateToPermissions = { viewModel.navigateToSubScreen("PERMISSIONS") }
                                        )
                                    }
                                }
                            }

                            // Record Transaction Bottom Sheet Modal
                            if (showRecordModal) {
                                RecordTransactionSheet(
                                    viewModel = viewModel,
                                    initialType = recordInitialType,
                                    onDismissRequest = { viewModel.closeRecordModal() },
                                    onNavigateToCategories = {
                                        viewModel.closeRecordModal()
                                        viewModel.navigateToSubScreen("CATEGORIES")
                                    }
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}
