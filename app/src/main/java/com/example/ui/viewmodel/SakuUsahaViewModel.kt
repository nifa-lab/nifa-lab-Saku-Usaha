package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.SakuUsahaRepository
import com.example.ui.util.DateFilterPeriod
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SakuUsahaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SakuUsahaRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = SakuUsahaRepository(database.dao())
    }

    // App Navigation and Screen State
    private val _currentTab = MutableStateFlow(0) // 0: Beranda, 1: Transaksi, 2: Catat (+), 3: Stok, 4: Lainnya
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _activeSubScreen = MutableStateFlow<String?>(null) // null = Root Tabs, "REPORTS", "DEBTS", "APPEARANCE", "CATEGORIES", "ONBOARDING", "LOGIN", "REGISTER"
    val activeSubScreen: StateFlow<String?> = _activeSubScreen.asStateFlow()

    // Transaction Recording Modal State
    private val _showRecordModal = MutableStateFlow(false)
    val showRecordModal: StateFlow<Boolean> = _showRecordModal.asStateFlow()

    private val _recordInitialType = MutableStateFlow("JUAL") // "JUAL", "BELI", "BIAYA"
    val recordInitialType: StateFlow<String> = _recordInitialType.asStateFlow()

    // Date Filter Period for Home & Transactions
    private val _homeDatePeriod = MutableStateFlow(DateFilterPeriod.HARI_INI)
    val homeDatePeriod: StateFlow<DateFilterPeriod> = _homeDatePeriod.asStateFlow()

    private val _transDatePeriod = MutableStateFlow(DateFilterPeriod.BULAN_INI)
    val transDatePeriod: StateFlow<DateFilterPeriod> = _transDatePeriod.asStateFlow()

    private val _reportDatePeriod = MutableStateFlow(DateFilterPeriod.TIGA_PULUH_HARI)
    val reportDatePeriod: StateFlow<DateFilterPeriod> = _reportDatePeriod.asStateFlow()

    private val _selectedTransactionTypeDetail = MutableStateFlow("SEMUA")
    val selectedTransactionTypeDetail: StateFlow<String> = _selectedTransactionTypeDetail.asStateFlow()

    // Toast notification
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Data Streams from Repository
    val businessProfile: StateFlow<BusinessProfileEntity?> = repository.businessProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<TransactionEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDebts: StateFlow<List<TransactionEntity>> = repository.activeDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun categoriesByType(type: String): Flow<List<CategoryEntity>> =
        repository.getCategoriesByType(type)

    // Navigation Methods
    fun selectTab(index: Int) {
        if (index == 2) {
            // Center + button: open record transaction modal
            openRecordModal("JUAL")
        } else {
            _currentTab.value = index
            _activeSubScreen.value = null
        }
    }

    fun navigateToSubScreen(screenName: String) {
        _activeSubScreen.value = screenName
    }

    fun openTransactionTypeDetail(type: String) {
        _selectedTransactionTypeDetail.value = type
        _activeSubScreen.value = "TRANSACTION_TYPE_DETAIL"
    }

    fun navigateBack() {
        _activeSubScreen.value = when (_activeSubScreen.value) {
            "PRODUCTS_LIST", "PRODUCTS_HPP", "PRODUCTS_TRANSACTIONS" -> "PRODUCTS"
            else -> null
        }
    }

    fun openRecordModal(initialType: String = "JUAL") {
        _recordInitialType.value = initialType
        _showRecordModal.value = true
    }

    fun closeRecordModal() {
        _showRecordModal.value = false
    }

    fun setHomeDatePeriod(period: DateFilterPeriod) {
        _homeDatePeriod.value = period
    }

    fun setTransDatePeriod(period: DateFilterPeriod) {
        _transDatePeriod.value = period
    }

    fun setReportDatePeriod(period: DateFilterPeriod) {
        _reportDatePeriod.value = period
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // Business Actions
    fun saveTransaction(
        type: String,
        category: String,
        amount: Long,
        paymentMethod: String,
        productId: Long? = null,
        productName: String? = null,
        quantity: Int = 1,
        items: List<CartItem> = emptyList(),
        contactName: String? = null,
        contactPhone: String? = null,
        dueDate: Long? = null,
        downPayment: Long = 0L,
        timestamp: Long = System.currentTimeMillis(),
        receiptFileName: String? = null,
        receiptSizeText: String? = null,
        notes: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveTransaction(
                type = type,
                category = category,
                amount = amount,
                paymentMethod = paymentMethod,
                productId = productId,
                productName = productName,
                quantity = quantity,
                items = items,
                contactName = contactName,
                contactPhone = contactPhone,
                dueDate = dueDate,
                downPayment = downPayment,
                timestamp = timestamp,
                receiptFileName = receiptFileName,
                receiptSizeText = receiptSizeText,
                notes = notes
            )
            showToast("Transaksi tersimpan")
            onSuccess()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            showToast("Transaksi berhasil dihapus")
        }
    }

    fun updateTransactionCategory(transaction: TransactionEntity, newCategory: String) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transaction, newCategory)
            showToast("Kategori diperbarui")
        }
    }

    fun payDebt(
        transactionId: Long,
        paymentAmount: Long,
        paymentMethod: String,
        notes: String? = null,
        receiptFileName: String? = null
    ) {
        viewModelScope.launch {
            repository.payDebt(
                transactionId = transactionId,
                paymentAmount = paymentAmount,
                paymentMethod = paymentMethod,
                notes = notes,
                receiptFileName = receiptFileName
            )
            showToast("Pembayaran berhasil dicatat")
        }
    }

    fun settleDebtFull(transactionId: Long, paymentMethod: String = "TUNAI") {
        viewModelScope.launch {
            repository.settleDebtFull(transactionId, paymentMethod)
            showToast("Utang telah dilunasi")
        }
    }

    fun adjustStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, delta)
        }
    }

    fun restockItem(productId: Long, quantityToAdd: Int, purchasePricePerUnit: Long? = null) {
        viewModelScope.launch {
            repository.restockProduct(productId, quantityToAdd, purchasePricePerUnit)
            showToast("Stok berhasil ditambahkan (+$quantityToAdd)")
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showToast("Produk disimpan")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showToast("Produk dihapus")
        }
    }

    fun recalculateAllHpp() {
        viewModelScope.launch {
            repository.recalculateAllComposedHpp()
            showToast("HPP produk berhasil dihitung ulang")
        }
    }

    fun addCategory(name: String, type: String, colorHex: String) {
        viewModelScope.launch {
            repository.addCategory(name, type, colorHex)
            showToast("Kategori ditambahkan")
        }
    }

    fun renameCategory(category: CategoryEntity, newName: String) {
        viewModelScope.launch {
            repository.renameCategory(category, newName)
            showToast("Nama kategori diperbarui")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showToast("Kategori '${category.name}' berhasil dihapus")
        }
    }

    fun updateProfile(profile: BusinessProfileEntity) {
        viewModelScope.launch {
            repository.updateBusinessProfile(profile)
            showToast("Pengaturan disimpan")
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetToDemoData()
            showToast("Data awal berhasil dipulihkan")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showToast("Semua data transaksi, stok, dan utang telah berhasil dihapus bersih")
        }
    }
}
