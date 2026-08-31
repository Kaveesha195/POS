package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ShopSettingsEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ExpenseEntity::class,
        StockHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopSettingsDao(): ShopSettingsDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun stockHistoryDao(): StockHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gk_pos_database"
                )
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
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            // Initial Settings
            db.shopSettingsDao().insertOrUpdate(
                ShopSettingsEntity(
                    id = 1,
                    shopName = "GK POS Retail Mart",
                    phone = "+94 11 234 5678",
                    email = "contact@gkpos.lk",
                    address = "No. 120, Galle Road, Colombo 03, Sri Lanka",
                    currencySymbol = "Rs.",
                    defaultTaxRate = 0.0,
                    receiptFooter = "Thank you for shopping at GK POS! Come back soon."
                )
            )

            // Initial Users
            val adminId = db.userDao().insertUser(
                UserEntity(
                    username = "admin",
                    fullName = "Gayantha Kulatunga",
                    pin = "1234",
                    role = "ADMIN",
                    email = "admin@gkpos.lk",
                    phone = "+94 77 111 2233"
                )
            )
            val cashierId = db.userDao().insertUser(
                UserEntity(
                    username = "cashier",
                    fullName = "Kasun Perera",
                    pin = "1111",
                    role = "CASHIER",
                    email = "kasun@gkpos.lk",
                    phone = "+94 71 444 5566"
                )
            )

            // Categories
            val catBeverages = db.categoryDao().insertCategory(
                CategoryEntity(name = "Beverages", iconName = "local_cafe", colorHex = "#3B82F6")
            )
            val catSnacks = db.categoryDao().insertCategory(
                CategoryEntity(name = "Snacks & Biscuits", iconName = "fastfood", colorHex = "#F59E0B")
            )
            val catDairy = db.categoryDao().insertCategory(
                CategoryEntity(name = "Dairy Products", iconName = "egg", colorHex = "#10B981")
            )
            val catBakery = db.categoryDao().insertCategory(
                CategoryEntity(name = "Bakery & Bread", iconName = "bakery_dining", colorHex = "#8B5CF6")
            )
            val catHousehold = db.categoryDao().insertCategory(
                CategoryEntity(name = "Household & Cleaning", iconName = "cleaning_services", colorHex = "#EC4899")
            )
            val catPersonal = db.categoryDao().insertCategory(
                CategoryEntity(name = "Personal Care", iconName = "soap", colorHex = "#06B6D4")
            )

            // Products
            val products = listOf(
                ProductEntity(
                    name = "Highland Fresh Milk 1L",
                    barcode = "479201100101",
                    categoryId = catDairy,
                    categoryName = "Dairy Products",
                    buyingPrice = 420.0,
                    sellingPrice = 520.0,
                    stock = 35,
                    minStock = 8
                ),
                ProductEntity(
                    name = "Munchee Super Cream Cracker 500g",
                    barcode = "479100200202",
                    categoryId = catSnacks,
                    categoryName = "Snacks & Biscuits",
                    buyingPrice = 310.0,
                    sellingPrice = 380.0,
                    stock = 48,
                    minStock = 10
                ),
                ProductEntity(
                    name = "Elephant House Ginger Beer 400ml",
                    barcode = "479100300303",
                    categoryId = catBeverages,
                    categoryName = "Beverages",
                    buyingPrice = 140.0,
                    sellingPrice = 180.0,
                    stock = 60,
                    minStock = 12
                ),
                ProductEntity(
                    name = "Dilmah Ceylon Premium Tea 100 Tea Bags",
                    barcode = "479100400404",
                    categoryId = catBeverages,
                    categoryName = "Beverages",
                    buyingPrice = 850.0,
                    sellingPrice = 1100.0,
                    stock = 22,
                    minStock = 5
                ),
                ProductEntity(
                    name = "Anchor Full Cream Milk Powder 400g",
                    barcode = "479100500505",
                    categoryId = catDairy,
                    categoryName = "Dairy Products",
                    buyingPrice = 1020.0,
                    sellingPrice = 1250.0,
                    stock = 4, // Low stock warning!
                    minStock = 8
                ),
                ProductEntity(
                    name = "Prima Special White Sliced Bread 450g",
                    barcode = "479100600606",
                    categoryId = catBakery,
                    categoryName = "Bakery & Bread",
                    buyingPrice = 160.0,
                    sellingPrice = 200.0,
                    stock = 25,
                    minStock = 6
                ),
                ProductEntity(
                    name = "Sunlight Soap Bar 110g",
                    barcode = "479100700707",
                    categoryId = catHousehold,
                    categoryName = "Household & Cleaning",
                    buyingPrice = 110.0,
                    sellingPrice = 150.0,
                    stock = 50,
                    minStock = 10
                ),
                ProductEntity(
                    name = "Signal Strong Teeth Toothpaste 160g",
                    barcode = "479100800808",
                    categoryId = catPersonal,
                    categoryName = "Personal Care",
                    buyingPrice = 270.0,
                    sellingPrice = 340.0,
                    stock = 3, // Low stock warning!
                    minStock = 6
                ),
                ProductEntity(
                    name = "MD Mixed Fruit Jam 300g",
                    barcode = "479100900909",
                    categoryId = catSnacks,
                    categoryName = "Snacks & Biscuits",
                    buyingPrice = 390.0,
                    sellingPrice = 480.0,
                    stock = 18,
                    minStock = 5
                ),
                ProductEntity(
                    name = "Kotmale Processed Cheese 200g",
                    barcode = "479101000010",
                    categoryId = catDairy,
                    categoryName = "Dairy Products",
                    buyingPrice = 640.0,
                    sellingPrice = 790.0,
                    stock = 15,
                    minStock = 5
                )
            )

            products.forEach { prod ->
                val pId = db.productDao().insertProduct(prod)
                db.stockHistoryDao().insertStockHistory(
                    StockHistoryEntity(
                        productId = pId,
                        productName = prod.name,
                        changeQty = prod.stock,
                        previousStock = 0,
                        newStock = prod.stock,
                        reason = "INITIAL_STOCK",
                        recordedBy = "Admin"
                    )
                )
            }

            // Default Customers
            db.customerDao().insertCustomer(
                CustomerEntity(
                    name = "Saman Kumara",
                    phone = "+94 77 333 4444",
                    email = "saman@gmail.com",
                    address = "12/A Flower Road, Colombo 07",
                    creditBalance = 0.0
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    name = "Nimali Fernando",
                    phone = "+94 71 888 9999",
                    email = "nimali.f@yahoo.com",
                    address = "48 Temple Trees Ave, Nugegoda",
                    creditBalance = 250.0
                )
            )

            // Seed an initial expense
            db.expenseDao().insertExpense(
                ExpenseEntity(
                    title = "Shop Electricity Bill",
                    category = "Utilities",
                    amount = 8500.0,
                    paymentMethod = "BANK",
                    notes = "CEB Electricity bill payment for current cycle",
                    recordedBy = "Admin"
                )
            )
        }
    }
}
