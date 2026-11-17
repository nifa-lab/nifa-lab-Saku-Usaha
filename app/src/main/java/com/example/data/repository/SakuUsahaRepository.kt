package com.example.data.repository

import com.example.data.local.SakuUsahaDao
import com.example.data.local.populateInitialData
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SakuUsahaRepository(private val dao: SakuUsahaDao) {

    val businessProfile: Flow<BusinessProfileEntity?> = dao.getBusinessProfile()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = dao.getRecentTransactions(5)
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = dao.getLowStockProducts()
    val activeDebts: Flow<List<TransactionEntity>> = dao.getActiveDebts()
    val allDebts: Flow<List<TransactionEntity>> = dao.getAllDebtTransactions()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsBetween(start, end)

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        dao.getCategoriesByType(type)

    fun getPaymentsForTransaction(transactionId: Long): Flow<List<DebtPaymentEntity>> =
        dao.getPaymentsForTransaction(transactionId)

    suspend fun saveTransaction(
        type: String, // "JUAL", "BELI", "BIAYA"
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
        notes: String? = null
    ): Long {
        val direction = when (type) {
            "JUAL", "PENJUALAN", "MODAL" -> "MASUK"
            else -> "KELUAR"
        }
        val remainingDebt = if (paymentMethod == "HUTANG") {
            (amount - downPayment).coerceAtLeast(0L)
        } else 0L

        val status = if (paymentMethod == "HUTANG") {
            when {
                remainingDebt == 0L -> "LUNAS"
                downPayment > 0L -> "DICICIL"
                else -> "BELUM_DIBAYAR"
            }
        } else "LUNAS"

        val contactType = when (type) {
            "JUAL", "PENJUALAN" -> "PELANGGAN"
            "MODAL" -> "INVESTOR"
            else -> "SUPPLIER"
        }

        val finalProductName = if (items.isNotEmpty()) {
            items.joinToString(", ") { "${it.product.name} (${it.quantity}x)" }
        } else {
            productName
        }

        val finalQuantity = if (items.isNotEmpty()) {
            items.sumOf { it.quantity }
        } else {
            quantity
        }

        val finalProductId = if (items.isNotEmpty()) {
            items.first().product.id
        } else {
            productId
        }

        val transaction = TransactionEntity(
            type = type,
            direction = direction,
            category = category,
            productId = finalProductId,
            productName = finalProductName,
            quantity = finalQuantity,
            amount = amount,
            paymentMethod = paymentMethod,
            status = status,
            dueDate = dueDate,
            downPayment = downPayment,
            remainingDebt = remainingDebt,
            contactName = contactName,
            contactPhone = contactPhone,
            contactType = contactType,
            timestamp = timestamp,
            receiptFileName = receiptFileName,
            receiptSizeText = receiptSizeText,
            notes = notes
        )

        val txId = dao.insertTransaction(transaction)

        // Automatically update stock inventory and recalculate HPP for restock (BELI)
        if (items.isNotEmpty()) {
            for (item in items) {
                if (type == "JUAL" || type == "PENJUALAN") {
                    dao.adjustStock(item.product.id, -item.quantity)
                    // If product has recipe ingredients, reduce ingredient stock accordingly
                    if (item.product.isComposedProduct && !item.product.recipeJson.isNullOrBlank()) {
                        val ingredients = RecipeHelper.parseIngredients(item.product.recipeJson)
                        for (ing in ingredients) {
                            val qtyNeeded = (ing.quantityNeeded * item.quantity).toInt().coerceAtLeast(1)
                            dao.adjustStock(ing.materialId, -qtyNeeded)
                        }
                    }
                } else if (type == "BELI") {
                    val prod = dao.getProductById(item.product.id)
                    if (prod != null) {
                        val purchaseQty = item.quantity
                        val unitPurchasePrice = item.product.costPrice
                        val oldStock = prod.stock.coerceAtLeast(0)
                        val oldCost = prod.costPrice
                        val newStock = oldStock + purchaseQty
                        val newCost = if (newStock > 0) {
                            ((oldStock * oldCost) + (purchaseQty * unitPurchasePrice)) / newStock
                        } else unitPurchasePrice
                        dao.updateProduct(prod.copy(stock = newStock, costPrice = newCost))
                        recalculateComposedProductsForMaterial(prod.id, newCost)
                    } else {
                        dao.adjustStock(item.product.id, item.quantity)
                    }
                }
            }
        } else if (productId != null) {
            if (type == "JUAL" || type == "PENJUALAN") {
                dao.adjustStock(productId, -quantity)
                val prod = dao.getProductById(productId)
                if (prod != null && prod.isComposedProduct && !prod.recipeJson.isNullOrBlank()) {
                    val ingredients = RecipeHelper.parseIngredients(prod.recipeJson)
                    for (ing in ingredients) {
                        val qtyNeeded = (ing.quantityNeeded * quantity).toInt().coerceAtLeast(1)
                        dao.adjustStock(ing.materialId, -qtyNeeded)
                    }
                }
            } else if (type == "BELI") {
                val prod = dao.getProductById(productId)
                if (prod != null) {
                    val purchaseQty = quantity.coerceAtLeast(1)
                    val unitPurchasePrice = amount / purchaseQty
                    val oldStock = prod.stock.coerceAtLeast(0)
                    val oldCost = prod.costPrice
                    val newStock = oldStock + purchaseQty
                    val newCost = if (newStock > 0) {
                        ((oldStock * oldCost) + (purchaseQty * unitPurchasePrice)) / newStock
                    } else unitPurchasePrice
                    dao.updateProduct(prod.copy(stock = newStock, costPrice = newCost))
                    recalculateComposedProductsForMaterial(prod.id, newCost)
                } else {
                    dao.adjustStock(productId, quantity)
                }
            }
        }

        // If down payment exists, log the initial debt payment
        if (paymentMethod == "HUTANG" && downPayment > 0L) {
            dao.insertDebtPayment(
                DebtPaymentEntity(
                    transactionId = txId,
                    amount = downPayment,
                    paymentMethod = "TUNAI",
                    timestamp = timestamp,
                    notes = "Uang Muka (DP)"
                )
            )
        }

        return txId
    }

    suspend fun payDebt(
        transactionId: Long,
        paymentAmount: Long,
        paymentMethod: String,
        notes: String? = null,
        receiptFileName: String? = null
    ) {
        val tx = dao.getTransactionById(transactionId) ?: return
        val newRemaining = (tx.remainingDebt - paymentAmount).coerceAtLeast(0L)
        val newStatus = if (newRemaining == 0L) "LUNAS" else "DICICIL"

        dao.updateTransaction(
            tx.copy(
                remainingDebt = newRemaining,
                status = newStatus
            )
        )

        dao.insertDebtPayment(
            DebtPaymentEntity(
                transactionId = transactionId,
                amount = paymentAmount,
                paymentMethod = paymentMethod,
                timestamp = System.currentTimeMillis(),
                notes = notes,
                receiptFileName = receiptFileName
            )
        )
    }

    suspend fun settleDebtFull(transactionId: Long, paymentMethod: String) {
        val tx = dao.getTransactionById(transactionId) ?: return
        if (tx.remainingDebt > 0L) {
            payDebt(transactionId, tx.remainingDebt, paymentMethod, "Pelunasan Penuh")
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        // Rollback stock if associated with a product
        if (transaction.productId != null) {
            if (transaction.type == "JUAL" || transaction.type == "PENJUALAN") {
                dao.adjustStock(transaction.productId, transaction.quantity)
            } else if (transaction.type == "BELI") {
                dao.adjustStock(transaction.productId, -transaction.quantity)
            }
        }
        dao.deleteTransaction(transaction)
    }

    suspend fun updateTransactionCategory(transaction: TransactionEntity, newCategory: String) {
        dao.updateTransaction(transaction.copy(category = newCategory))
    }

    // Products
    suspend fun saveProduct(product: ProductEntity) {
        val finalProduct = if (product.isComposedProduct && !product.recipeJson.isNullOrBlank()) {
            val ingredients = RecipeHelper.parseIngredients(product.recipeJson)
            val allProds = dao.getAllProductsDirect()
            val costMap = allProds.associate { it.id to it.costPrice }
            val updatedIngredients = ingredients.map { ing ->
                val latestCost = costMap[ing.materialId] ?: ing.unitCost
                ing.copy(unitCost = latestCost)
            }
            val totalCost = updatedIngredients.sumOf { it.unitCost * it.quantityNeeded }
            val yield = product.yieldQuantity.coerceAtLeast(1)
            val computedHpp = (totalCost / yield).toLong()
            product.copy(
                costPrice = if (computedHpp > 0) computedHpp else product.costPrice,
                recipeJson = RecipeHelper.toJson(updatedIngredients)
            )
        } else {
            product
        }

        val savedId = if (finalProduct.id == 0L) {
            dao.insertProduct(finalProduct)
        } else {
            dao.updateProduct(finalProduct)
            finalProduct.id
        }

        // If this product might be used as an ingredient elsewhere, update composed products
        recalculateComposedProductsForMaterial(savedId, finalProduct.costPrice)
    }

    suspend fun recalculateComposedProductsForMaterial(materialId: Long, newCost: Long) {
        try {
            val allProducts = dao.getAllProductsDirect()
            val materialMap = allProducts.associate { it.id to it.costPrice }.toMutableMap()
            materialMap[materialId] = newCost

            for (prod in allProducts) {
                if (prod.isComposedProduct && !prod.recipeJson.isNullOrBlank()) {
                    val ingredients = RecipeHelper.parseIngredients(prod.recipeJson)
                    if (ingredients.any { it.materialId == materialId }) {
                        val updatedIngredients = ingredients.map { ing ->
                            if (ing.materialId == materialId) ing.copy(unitCost = newCost)
                            else ing.copy(unitCost = materialMap[ing.materialId] ?: ing.unitCost)
                        }
                        val totalCost = updatedIngredients.sumOf { (materialMap[it.materialId] ?: it.unitCost) * it.quantityNeeded }
                        val yield = prod.yieldQuantity.coerceAtLeast(1)
                        val newHpp = (totalCost / yield).toLong()
                        dao.updateProduct(
                            prod.copy(
                                costPrice = newHpp,
                                recipeJson = RecipeHelper.toJson(updatedIngredients)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun recalculateAllComposedHpp() {
        try {
            val allProducts = dao.getAllProductsDirect()
            val materialMap = allProducts.associate { it.id to it.costPrice }

            for (prod in allProducts) {
                if (prod.isComposedProduct && !prod.recipeJson.isNullOrBlank()) {
                    val ingredients = RecipeHelper.parseIngredients(prod.recipeJson)
                    val updatedIngredients = ingredients.map { ing ->
                        val currentCost = materialMap[ing.materialId] ?: ing.unitCost
                        ing.copy(unitCost = currentCost)
                    }
                    val totalCost = updatedIngredients.sumOf { it.unitCost * it.quantityNeeded }
                    val yield = prod.yieldQuantity.coerceAtLeast(1)
                    val newHpp = (totalCost / yield).toLong()
                    dao.updateProduct(
                        prod.copy(
                            costPrice = newHpp,
                            recipeJson = RecipeHelper.toJson(updatedIngredients)
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProduct(product)
    }

    suspend fun adjustProductStock(productId: Long, delta: Int) {
        dao.adjustStock(productId, delta)
    }

    suspend fun restockProduct(productId: Long, quantityToAdd: Int, purchasePricePerUnit: Long? = null) {
        if (quantityToAdd <= 0) return
        val prod = dao.getProductById(productId) ?: return
        val oldStock = prod.stock.coerceAtLeast(0)
        val oldCost = prod.costPrice
        val newStock = oldStock + quantityToAdd
        val newCost = if (purchasePricePerUnit != null && purchasePricePerUnit > 0L && newStock > 0) {
            ((oldStock * oldCost) + (quantityToAdd * purchasePricePerUnit)) / newStock
        } else {
            oldCost
        }
        dao.updateProduct(prod.copy(stock = newStock, costPrice = newCost))
        if (newCost != oldCost) {
            recalculateComposedProductsForMaterial(prod.id, newCost)
        }
    }

    // Categories
    suspend fun addCategory(name: String, type: String, colorHex: String = "#146C43") {
        dao.insertCategory(
            CategoryEntity(
                name = name.trim(),
                type = type,
                colorHex = colorHex,
                isDefault = false
            )
        )
    }

    suspend fun renameCategory(category: CategoryEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val oldName = category.name
        dao.updateCategory(category.copy(name = trimmed))
        if (category.type == "PENJUALAN" || category.type == "BIAYA") {
            dao.updateCategoryInTransactions(oldName, trimmed, if (category.type == "PENJUALAN") "JUAL" else "BIAYA")
        } else if (category.type == "STOK") {
            dao.updateCategoryInProducts(oldName, trimmed)
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        if (category.type == "STOK") {
            // reassign products to "Lainnya" or fallback
            dao.updateCategoryInProducts(category.name, "Lainnya")
        }
        dao.deleteCategory(category)
    }

    // Profile & Appearance
    suspend fun updateBusinessProfile(profile: BusinessProfileEntity) {
        dao.insertOrUpdateProfile(profile)
    }

    suspend fun clearAllData() {
        dao.clearTransactions()
        dao.clearProducts()
        dao.clearDebtPayments()
        dao.clearCategories()

        // Re-insert clean base default categories so the user can immediately record transactions
        val defaultCategories = listOf(
            CategoryEntity(name = "Penjualan Produk", type = "PENJUALAN", colorHex = "#15803D", isDefault = true),
            CategoryEntity(name = "Jasa & Layanan", type = "PENJUALAN", colorHex = "#0D9488", isDefault = true),
            CategoryEntity(name = "Pendapatan Lain", type = "PENJUALAN", colorHex = "#2563EB", isDefault = true),
            CategoryEntity(name = "Setoran Modal Pemilik", type = "MODAL", colorHex = "#059669", isDefault = true),
            CategoryEntity(name = "Pinjaman Modal Usaha", type = "MODAL", colorHex = "#0284C7", isDefault = true),
            CategoryEntity(name = "Tambahan Modal Ekuitas", type = "MODAL", colorHex = "#7C3AED", isDefault = true),
            CategoryEntity(name = "Belanja Stok", type = "BELI", colorHex = "#0284C7", isDefault = true),
            CategoryEntity(name = "Bahan Baku & Kemasan", type = "BELI", colorHex = "#EA580C", isDefault = true),
            CategoryEntity(name = "Listrik & Air", type = "BIAYA", colorHex = "#F59E0B", isDefault = true),
            CategoryEntity(name = "Sewa Tempat", type = "BIAYA", colorHex = "#7C3AED", isDefault = true),
            CategoryEntity(name = "Operasional Toko", type = "BIAYA", colorHex = "#D6384A", isDefault = true),
            CategoryEntity(name = "Biaya Lainnya", type = "BIAYA", colorHex = "#64748B", isDefault = true),
            CategoryEntity(name = "Semua", type = "STOK", colorHex = "#146C43", isDefault = true),
            CategoryEntity(name = "Umum", type = "STOK", colorHex = "#059669", isDefault = true)
        )
        for (cat in defaultCategories) {
            dao.insertCategory(cat)
        }
    }

    suspend fun resetToDemoData() {
        dao.clearTransactions()
        dao.clearProducts()
        dao.clearCategories()
        dao.clearDebtPayments()
        populateInitialData(dao)
    }
}
