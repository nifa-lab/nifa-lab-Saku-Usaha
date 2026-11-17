package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        ProductEntity::class,
        CategoryEntity::class,
        BusinessProfileEntity::class,
        DebtPaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SakuUsahaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE business_profile ADD COLUMN customBannerUri TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN isComposedProduct INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN recipeJson TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE products ADD COLUMN yieldQuantity INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT 'pcs'")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN stockType TEXT NOT NULL DEFAULT 'PRODUK'")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saku_usaha.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.dao())
                }
            }
        }
    }
}

suspend fun populateInitialData(dao: SakuUsahaDao) {
    // 1. Initial Business Profile
    dao.insertOrUpdateProfile(
        BusinessProfileEntity(
            id = 1L,
            businessName = "Toko Suka Maju",
            ownerName = "Budi Santoso",
            businessType = "Toko Kelontong",
            phone = "081234567890",
            capitalAmount = 5_000_000L,
            themeColorHex = "#146C43",
            themeMode = "LIGHT",
            bannerPreset = "WARUNG",
            isLoggedIn = true,
            isOnboarded = true
        )
    )

    // 2. Default Categories
    val defaultCategories = listOf(
        // Pemasukan - Penjualan
        CategoryEntity(name = "Penjualan Produk", type = "PENJUALAN", colorHex = "#15803D", isDefault = true),
        CategoryEntity(name = "Jasa & Layanan", type = "PENJUALAN", colorHex = "#0D9488", isDefault = true),
        CategoryEntity(name = "Pendapatan Lain", type = "PENJUALAN", colorHex = "#2563EB", isDefault = true),
        // Pemasukan - Modal
        CategoryEntity(name = "Setoran Modal Pemilik", type = "MODAL", colorHex = "#059669", isDefault = true),
        CategoryEntity(name = "Pinjaman Modal Usaha", type = "MODAL", colorHex = "#0284C7", isDefault = true),
        CategoryEntity(name = "Tambahan Modal Ekuitas", type = "MODAL", colorHex = "#7C3AED", isDefault = true),
        // Pengeluaran - Beli (Stok & Bahan)
        CategoryEntity(name = "Belanja Stok", type = "BELI", colorHex = "#0284C7", isDefault = true),
        CategoryEntity(name = "Bahan Baku & Kemasan", type = "BELI", colorHex = "#EA580C", isDefault = true),
        CategoryEntity(name = "Inventaris Usaha", type = "BELI", colorHex = "#4F46E5", isDefault = true),
        // Pengeluaran - Biaya (Operasional)
        CategoryEntity(name = "Listrik & Air", type = "BIAYA", colorHex = "#F59E0B", isDefault = true),
        CategoryEntity(name = "Sewa Tempat", type = "BIAYA", colorHex = "#7C3AED", isDefault = true),
        CategoryEntity(name = "Gaji Karyawan", type = "BIAYA", colorHex = "#9333EA", isDefault = true),
        CategoryEntity(name = "Operasional Toko", type = "BIAYA", colorHex = "#D6384A", isDefault = true),
        CategoryEntity(name = "Biaya Lainnya", type = "BIAYA", colorHex = "#64748B", isDefault = true),
        // Stok
        CategoryEntity(name = "Semua", type = "STOK", colorHex = "#146C43", isDefault = true),
        CategoryEntity(name = "Makanan", type = "STOK", colorHex = "#D97706", isDefault = true),
        CategoryEntity(name = "Minuman", type = "STOK", colorHex = "#0284C7", isDefault = true),
        CategoryEntity(name = "Sembako", type = "STOK", colorHex = "#059669", isDefault = true),
        CategoryEntity(name = "Lainnya", type = "STOK", colorHex = "#6B7280", isDefault = true)
    )
    for (cat in defaultCategories) {
        dao.insertCategory(cat)
    }

    // 3. Initial Products (Matching Storyboard: Es Teh Manis, Roti Cokelat, Susu UHT, Indomie, etc.)
    val p1 = dao.insertProduct(
        ProductEntity(
            name = "Es Teh Manis",
            categoryName = "Minuman",
            sellPrice = 5_000L,
            costPrice = 2_000L,
            stock = 25,
            minStockAlert = 30, // Trigger low stock!
            emoji = "🧋"
        )
    )
    val p2 = dao.insertProduct(
        ProductEntity(
            name = "Roti Cokelat",
            categoryName = "Makanan",
            sellPrice = 8_000L,
            costPrice = 5_000L,
            stock = 40,
            minStockAlert = 15,
            emoji = "🥐"
        )
    )
    val p3 = dao.insertProduct(
        ProductEntity(
            name = "Susu UHT",
            categoryName = "Minuman",
            sellPrice = 7_500L,
            costPrice = 5_500L,
            stock = 15,
            minStockAlert = 20, // Low stock alert!
            emoji = "🥛"
        )
    )
    val p4 = dao.insertProduct(
        ProductEntity(
            name = "Indomie Goreng",
            categoryName = "Makanan",
            sellPrice = 4_000L,
            costPrice = 2_800L,
            stock = 60,
            minStockAlert = 25,
            emoji = "🍜"
        )
    )
    val p5 = dao.insertProduct(
        ProductEntity(
            name = "Minyak Goreng 1L",
            categoryName = "Sembako",
            sellPrice = 18_000L,
            costPrice = 15_500L,
            stock = 22,
            minStockAlert = 10,
            emoji = "🍾"
        )
    )
    val p6 = dao.insertProduct(
        ProductEntity(
            name = "Beras Ramos 5kg",
            categoryName = "Sembako",
            sellPrice = 72_000L,
            costPrice = 64_000L,
            stock = 12,
            minStockAlert = 5,
            emoji = "🌾",
            stockType = "PRODUK"
        )
    )

    // 3b. Initial Bahan Baku
    dao.insertProduct(
        ProductEntity(
            name = "Gula Pasir 1kg",
            categoryName = "Sembako",
            sellPrice = 0L,
            costPrice = 16_500L,
            stock = 3,
            minStockAlert = 5,
            emoji = "🧂",
            unit = "kg",
            stockType = "BAHAN"
        )
    )
    dao.insertProduct(
        ProductEntity(
            name = "Daun Teh Tubruk",
            categoryName = "Minuman",
            sellPrice = 0L,
            costPrice = 12_000L,
            stock = 8,
            minStockAlert = 4,
            emoji = "🍃",
            unit = "pack",
            stockType = "BAHAN"
        )
    )
    dao.insertProduct(
        ProductEntity(
            name = "Cup & Tutup 16oz",
            categoryName = "Lainnya",
            sellPrice = 0L,
            costPrice = 450L,
            stock = 18,
            minStockAlert = 25,
            emoji = "🥤",
            unit = "pcs",
            stockType = "BAHAN"
        )
    )

    // 3c. Initial Inventaris Usaha
    dao.insertProduct(
        ProductEntity(
            name = "Etalase Kaca Warung",
            categoryName = "Lainnya",
            sellPrice = 0L,
            costPrice = 1_250_000L,
            stock = 2,
            minStockAlert = 1,
            emoji = "🪟",
            unit = "unit",
            stockType = "INVENTARIS"
        )
    )
    dao.insertProduct(
        ProductEntity(
            name = "Timbangan Digital",
            categoryName = "Lainnya",
            sellPrice = 0L,
            costPrice = 185_000L,
            stock = 1,
            minStockAlert = 1,
            emoji = "⚖️",
            unit = "unit",
            stockType = "INVENTARIS"
        )
    )
    dao.insertProduct(
        ProductEntity(
            name = "Blender Minuman",
            categoryName = "Lainnya",
            sellPrice = 0L,
            costPrice = 420_000L,
            stock = 2,
            minStockAlert = 1,
            emoji = "🥣",
            unit = "unit",
            stockType = "INVENTARIS"
        )
    )

    // 4. Initial Realistic Transactions
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L

    // Today's transactions
    dao.insertTransaction(
        TransactionEntity(
            type = "JUAL",
            direction = "MASUK",
            category = "Penjualan Produk",
            productId = p1,
            productName = "Es Teh Manis",
            quantity = 30,
            amount = 150_000L,
            paymentMethod = "QRIS",
            status = "LUNAS",
            timestamp = now - 30 * 60 * 1000L,
            receiptFileName = "nota_qris_001.jpg",
            receiptSizeText = "142 KB",
            notes = "Penjualan rombongan makan siang"
        )
    )
    dao.insertTransaction(
        TransactionEntity(
            type = "BELI",
            direction = "KELUAR",
            category = "Belanja Stok",
            productId = p4,
            productName = "Indomie Goreng",
            quantity = 40,
            amount = 250_000L,
            paymentMethod = "TRANSFER",
            status = "LUNAS",
            timestamp = now - 2 * 60 * 60 * 1000L,
            receiptFileName = "struk_grosir_08.jpg",
            receiptSizeText = "315 KB",
            notes = "Restok dari Agen Surya"
        )
    )
    dao.insertTransaction(
        TransactionEntity(
            type = "BIAYA",
            direction = "KELUAR",
            category = "Listrik & Air",
            amount = 100_000L,
            paymentMethod = "E_WALLET",
            status = "LUNAS",
            timestamp = now - 4 * 60 * 60 * 1000L,
            receiptFileName = "token_pln.png",
            receiptSizeText = "98 KB",
            notes = "Beli token listrik bulanan"
        )
    )
    dao.insertTransaction(
        TransactionEntity(
            type = "JUAL",
            direction = "MASUK",
            category = "Penjualan Produk",
            productId = p2,
            productName = "Roti Cokelat",
            quantity = 50,
            amount = 400_000L,
            paymentMethod = "TUNAI",
            status = "LUNAS",
            timestamp = now - 6 * 60 * 60 * 1000L,
            notes = "Langganan sarapan kantor Bu Santi"
        )
    )
    dao.insertTransaction(
        TransactionEntity(
            type = "JUAL",
            direction = "MASUK",
            category = "Penjualan Produk",
            productId = p3,
            productName = "Susu UHT",
            quantity = 25,
            amount = 187_500L,
            paymentMethod = "TUNAI",
            status = "LUNAS",
            timestamp = now - 7 * 60 * 60 * 1000L,
            notes = "Pagi hari"
        )
    )

    // Yesterday transactions
    dao.insertTransaction(
        TransactionEntity(
            type = "JUAL",
            direction = "MASUK",
            category = "Penjualan Produk",
            amount = 650_000L,
            paymentMethod = "TUNAI",
            status = "LUNAS",
            timestamp = now - dayMillis,
            notes = "Penjualan sembako harian"
        )
    )
    dao.insertTransaction(
        TransactionEntity(
            type = "BIAYA",
            direction = "KELUAR",
            category = "Bahan Baku",
            amount = 180_000L,
            paymentMethod = "TUNAI",
            status = "LUNAS",
            timestamp = now - dayMillis - 2 * 60 * 60 * 1000L,
            notes = "Beli gula pasir & daun teh di pasar"
        )
    )

    // Past 7 days data for chart realism
    val amountsIn = listOf(580_000L, 720_000L, 490_000L, 850_000L, 690_000L)
    val amountsOut = listOf(210_000L, 340_000L, 150_000L, 420_000L, 200_000L)
    for (i in 2..6) {
        val pastTime = now - i * dayMillis
        dao.insertTransaction(
            TransactionEntity(
                type = "JUAL",
                direction = "MASUK",
                category = "Penjualan Produk",
                amount = amountsIn[i - 2],
                paymentMethod = "TUNAI",
                status = "LUNAS",
                timestamp = pastTime
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                type = "BIAYA",
                direction = "KELUAR",
                category = "Operasional Lain",
                amount = amountsOut[i - 2],
                paymentMethod = "TUNAI",
                status = "LUNAS",
                timestamp = pastTime - 3600000L
            )
        )
    }

    // 5. Initial Debt / Accounts Payable & Receivable (matching Storyboard screen 18)
    // Tagihan Supplier ABC: Rp 500.000, Jatuh tempo 20 Okt 2026
    val dueSupplier = now + 2 * dayMillis
    dao.insertTransaction(
        TransactionEntity(
            type = "BELI",
            direction = "KELUAR",
            category = "Belanja Stok",
            amount = 500_000L,
            paymentMethod = "HUTANG",
            status = "BELUM_DIBAYAR",
            dueDate = dueSupplier,
            downPayment = 0L,
            remainingDebt = 500_000L,
            contactName = "Supplier ABC",
            contactPhone = "081298765432",
            contactType = "SUPPLIER",
            timestamp = now - dayMillis,
            notes = "Tagihan kardus & kemasan snack"
        )
    )

    // Piutang Pelanggan: Toko Maju / Bu Dewi: Rp 750.000, Jatuh tempo 25 Okt 2026
    val dueCustomer = now + 7 * dayMillis
    dao.insertTransaction(
        TransactionEntity(
            type = "JUAL",
            direction = "MASUK",
            category = "Penjualan Produk",
            amount = 750_000L,
            paymentMethod = "HUTANG",
            status = "BELUM_DIBAYAR",
            dueDate = dueCustomer,
            downPayment = 0L,
            remainingDebt = 750_000L,
            contactName = "Toko Maju (Bu Dewi)",
            contactPhone = "081345678901",
            contactType = "PELANGGAN",
            timestamp = now - 2 * dayMillis,
            notes = "Pesanan konsumsi arisan warga"
        )
    )
}
