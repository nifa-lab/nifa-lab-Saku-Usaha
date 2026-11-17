package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SakuUsahaDao {

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE paymentMethod = 'HUTANG' AND remainingDebt > 0 ORDER BY dueDate ASC, timestamp DESC")
    fun getActiveDebts(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE paymentMethod = 'HUTANG' ORDER BY timestamp DESC")
    fun getAllDebtTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory AND type = :type")
    suspend fun updateCategoryInTransactions(oldCategory: String, newCategory: String, type: String)

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock <= minStockAlert ORDER BY stock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryName = :categoryName ORDER BY name ASC")
    fun getProductsByCategory(categoryName: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getAllProductsDirect(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = stock + :quantityDelta WHERE id = :productId")
    suspend fun adjustStock(productId: Long, quantityDelta: Int)

    @Query("UPDATE products SET categoryName = :newCategory WHERE categoryName = :oldCategory")
    suspend fun updateCategoryInProducts(oldCategory: String, newCategory: String)

    @Query("SELECT COUNT(*) FROM products WHERE categoryName = :categoryName")
    suspend fun getProductCountInCategory(categoryName: String): Int

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY isDefault DESC, name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // --- BUSINESS PROFILE ---
    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getBusinessProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    suspend fun getBusinessProfileDirect(): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: BusinessProfileEntity)

    // --- DEBT PAYMENTS ---
    @Query("SELECT * FROM debt_payments WHERE transactionId = :transactionId ORDER BY timestamp DESC")
    fun getPaymentsForTransaction(transactionId: Long): Flow<List<DebtPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long

    // --- BULK OPERATIONS FOR DEMO/RESET ---
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM debt_payments")
    suspend fun clearDebtPayments()
}
