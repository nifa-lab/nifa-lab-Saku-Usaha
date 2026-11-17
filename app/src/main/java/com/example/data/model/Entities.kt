package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // "JUAL", "BELI", "BIAYA"
    val direction: String, // "MASUK", "KELUAR"
    val category: String,
    val productId: Long? = null,
    val productName: String? = null,
    val quantity: Int = 1,
    val amount: Long,
    val paymentMethod: String, // "TUNAI", "TRANSFER", "QRIS", "E_WALLET", "HUTANG"
    val status: String = "LUNAS", // "LUNAS", "DICICIL", "BELUM_DIBAYAR"
    val dueDate: Long? = null,
    val downPayment: Long = 0L,
    val remainingDebt: Long = 0L,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactType: String? = null, // "PELANGGAN", "SUPPLIER"
    val timestamp: Long = System.currentTimeMillis(),
    val receiptFileName: String? = null,
    val receiptSizeText: String? = null,
    val notes: String? = null
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val categoryName: String,
    val sellPrice: Long,
    val costPrice: Long, // HPP / Modal
    val stock: Int,
    val minStockAlert: Int = 10,
    val emoji: String = "📦",
    val isComposedProduct: Boolean = false,
    val recipeJson: String? = null,
    val yieldQuantity: Int = 1,
    val unit: String = "pcs",
    val stockType: String = "PRODUK" // "PRODUK", "BAHAN", "INVENTARIS"
)

data class ProductIngredient(
    val materialId: Long,
    val materialName: String,
    val quantityNeeded: Double,
    val unit: String = "pcs",
    val unitCost: Long = 0L
)

object RecipeHelper {
    fun parseIngredients(json: String?): List<ProductIngredient> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<ProductIngredient>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProductIngredient(
                        materialId = obj.optLong("materialId"),
                        materialName = obj.optString("materialName"),
                        quantityNeeded = obj.optDouble("quantityNeeded", 1.0),
                        unit = obj.optString("unit", "pcs"),
                        unitCost = obj.optLong("unitCost", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toJson(ingredients: List<ProductIngredient>): String {
        val array = org.json.JSONArray()
        for (item in ingredients) {
            val obj = org.json.JSONObject().apply {
                put("materialId", item.materialId)
                put("materialName", item.materialName)
                put("quantityNeeded", item.quantityNeeded)
                put("unit", item.unit)
                put("unitCost", item.unitCost)
            }
            array.put(obj)
        }
        return array.toString()
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: String, // "PENJUALAN", "BIAYA", "STOK"
    val colorHex: String = "#146C43",
    val isDefault: Boolean = false
)

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey
    val id: Long = 1L,
    val businessName: String = "Toko Suka Maju",
    val ownerName: String = "Budi Santoso",
    val businessType: String = "Toko Kelontong",
    val phone: String = "081234567890",
    val capitalAmount: Long = 5_000_000L,
    val themeColorHex: String = "#146C43",
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val bannerPreset: String = "WARUNG", // "WARUNG", "CLEAN", "CUSTOM"
    val customBannerUri: String? = null,
    val isLoggedIn: Boolean = true,
    val isOnboarded: Boolean = true
)

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transactionId: Long,
    val amount: Long,
    val paymentMethod: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val receiptFileName: String? = null
)

data class CartItem(
    val product: ProductEntity,
    var quantity: Int = 1
)
