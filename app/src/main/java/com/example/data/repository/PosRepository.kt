package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
) {
    val subtotal: Double get() = product.sellingPrice * quantity
    val buyingSubtotal: Double get() = product.buyingPrice * quantity
}

data class SaleWithItems(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>
)

data class ReportsData(
    val todaySalesTotal: Double = 0.0,
    val todaySalesCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalCostOfGoods: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val lowStockCount: Int = 0,
    val totalStockValuation: Double = 0.0,
    val totalStockItemsCount: Int = 0,
    val bestSellingProducts: List<BestSellingItem> = emptyList(),
    val dailyRevenueMap: Map<String, Double> = emptyMap(),
    val monthlyRevenueMap: Map<String, Double> = emptyMap()
)

data class BestSellingItem(
    val productId: Long,
    val productName: String,
    val totalQuantitySold: Int,
    val totalRevenue: Double
)

class PosRepository(private val db: AppDatabase) {

    // Settings
    val settingsFlow: Flow<ShopSettingsEntity?> = db.shopSettingsDao().getSettingsFlow()
    suspend fun getSettings(): ShopSettingsEntity = db.shopSettingsDao().getSettings()
        ?: ShopSettingsEntity()
    suspend fun updateSettings(settings: ShopSettingsEntity) =
        db.shopSettingsDao().insertOrUpdate(settings)

    // Users
    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    suspend fun getUserByUsername(username: String): UserEntity? = db.userDao().getUserByUsername(username)
    suspend fun insertUser(user: UserEntity): Long = db.userDao().insertUser(user)
    suspend fun updateUser(user: UserEntity) = db.userDao().updateUser(user)
    suspend fun deleteUser(user: UserEntity) = db.userDao().deleteUser(user)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    suspend fun insertCategory(category: CategoryEntity): Long = db.categoryDao().insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = db.categoryDao().updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = db.categoryDao().deleteCategory(category)

    // Products
    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
    suspend fun getProductById(id: Long): ProductEntity? = db.productDao().getProductById(id)
    suspend fun getProductByBarcode(barcode: String): ProductEntity? = db.productDao().getProductByBarcode(barcode)
    suspend fun insertProduct(product: ProductEntity): Long {
        val id = db.productDao().insertProduct(product)
        if (product.stock > 0) {
            db.stockHistoryDao().insertStockHistory(
                StockHistoryEntity(
                    productId = id,
                    productName = product.name,
                    changeQty = product.stock,
                    previousStock = 0,
                    newStock = product.stock,
                    reason = "INITIAL_STOCK",
                    recordedBy = "Admin"
                )
            )
        }
        return id
    }
    suspend fun updateProduct(product: ProductEntity) = db.productDao().updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = db.productDao().deleteProduct(product)

    // Stock Management
    val allStockHistory: Flow<List<StockHistoryEntity>> = db.stockHistoryDao().getAllStockHistory()
    suspend fun adjustStock(
        productId: Long,
        adjustmentQty: Int,
        reason: String,
        recordedBy: String
    ): Boolean {
        return db.withTransaction {
            val product = db.productDao().getProductById(productId) ?: return@withTransaction false
            val newStock = (product.stock + adjustmentQty).coerceAtLeast(0)
            db.productDao().updateProductStock(productId, newStock)
            db.stockHistoryDao().insertStockHistory(
                StockHistoryEntity(
                    productId = productId,
                    productName = product.name,
                    changeQty = adjustmentQty,
                    previousStock = product.stock,
                    newStock = newStock,
                    reason = reason,
                    recordedBy = recordedBy
                )
            )
            true
        }
    }

    // Customers
    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    suspend fun getCustomerById(id: Long): CustomerEntity? = db.customerDao().getCustomerById(id)
    suspend fun insertCustomer(customer: CustomerEntity): Long = db.customerDao().insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = db.customerDao().updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = db.customerDao().deleteCustomer(customer)

    // Sales & Checkout
    val allSales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    val allSaleItems: Flow<List<SaleItemEntity>> = db.saleItemDao().getAllSaleItems()

    suspend fun getSaleById(saleId: Long): SaleEntity? = db.saleDao().getSaleById(saleId)
    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> = db.saleItemDao().getItemsForSaleSync(saleId)

    suspend fun completeSale(
        cartItems: List<CartItem>,
        customerId: Long?,
        customerName: String,
        cashierId: Long,
        cashierName: String,
        discount: Double,
        tax: Double,
        paidAmount: Double,
        paymentMethod: String
    ): SaleWithItems {
        val subtotal = cartItems.sumOf { it.subtotal }
        val grandTotal = (subtotal - discount + tax).coerceAtLeast(0.0)
        val changeAmount = (paidAmount - grandTotal).coerceAtLeast(0.0)

        val invoiceNumber = "INV-" + SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())

        val sale = SaleEntity(
            invoiceNumber = invoiceNumber,
            customerId = customerId,
            customerName = if (customerName.isBlank()) "Walk-in Customer" else customerName,
            cashierId = cashierId,
            cashierName = cashierName,
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            total = grandTotal,
            paidAmount = paidAmount,
            changeAmount = changeAmount,
            paymentMethod = paymentMethod,
            status = "COMPLETED",
            timestamp = System.currentTimeMillis()
        )

        return db.withTransaction {
            val saleId = db.saleDao().insertSale(sale)
            val insertedSale = sale.copy(id = saleId)

            val saleItems = cartItems.map { cartItem ->
                // Reduce product stock in database
                val currentProduct = db.productDao().getProductById(cartItem.product.id)
                val prevStock = currentProduct?.stock ?: cartItem.product.stock
                val updatedStock = (prevStock - cartItem.quantity).coerceAtLeast(0)

                db.productDao().updateProductStock(cartItem.product.id, updatedStock)

                // Add stock history entry
                db.stockHistoryDao().insertStockHistory(
                    StockHistoryEntity(
                        productId = cartItem.product.id,
                        productName = cartItem.product.name,
                        changeQty = -cartItem.quantity,
                        previousStock = prevStock,
                        newStock = updatedStock,
                        reason = "SALE: $invoiceNumber",
                        recordedBy = cashierName
                    )
                )

                SaleItemEntity(
                    saleId = saleId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    barcode = cartItem.product.barcode,
                    quantity = cartItem.quantity,
                    unitBuyingPrice = cartItem.product.buyingPrice,
                    unitSellingPrice = cartItem.product.sellingPrice,
                    totalPrice = cartItem.subtotal
                )
            }

            db.saleItemDao().insertSaleItems(saleItems)
            SaleWithItems(sale = insertedSale, items = saleItems)
        }
    }

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    suspend fun insertExpense(expense: ExpenseEntity): Long = db.expenseDao().insertExpense(expense)
    suspend fun updateExpense(expense: ExpenseEntity) = db.expenseDao().updateExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = db.expenseDao().deleteExpense(expense)

    // Reset database to default sample data
    suspend fun resetDatabase() {
        db.withTransaction {
            db.clearAllTables()
            seedData()
        }
    }

    suspend fun seedData() {
        // Settings
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

        // Users
        db.userDao().insertUser(
            UserEntity(
                username = "admin",
                fullName = "Gayantha Kulatunga",
                pin = "1234",
                role = "ADMIN",
                email = "admin@gkpos.lk",
                phone = "+94 77 111 2233"
            )
        )
        db.userDao().insertUser(
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
                name = "Dilmah Ceylon Premium Tea 100 Bags",
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

        // Customers
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

        // Expense
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
