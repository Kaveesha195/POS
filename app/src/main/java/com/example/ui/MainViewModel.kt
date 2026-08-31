package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import com.example.data.repository.ReportsData
import com.example.data.repository.SaleWithItems
import com.example.data.repository.BestSellingItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = PosRepository(database)

    // Current Authenticated User (Defaults to initial Admin)
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // State flows from Room
    val settings: StateFlow<ShopSettingsEntity> = repository.settingsFlow
        .map { it ?: ShopSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopSettingsEntity())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSaleItems: StateFlow<List<SaleItemEntity>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockHistory: StateFlow<List<StockHistoryEntity>> = repository.allStockHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart and POS Active State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _posDiscount = MutableStateFlow(0.0)
    val posDiscount: StateFlow<Double> = _posDiscount.asStateFlow()

    private val _posTaxRate = MutableStateFlow(0.0)
    val posTaxRate: StateFlow<Double> = _posTaxRate.asStateFlow()

    // Invoice currently in view for receipt screen
    private val _viewingInvoice = MutableStateFlow<SaleWithItems?>(null)
    val viewingInvoice: StateFlow<SaleWithItems?> = _viewingInvoice.asStateFlow()

    // UI Feedback Banner
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        // Automatically authenticate initial user when database loads
        viewModelScope.launch {
            allUsers.collect { users ->
                if (_currentUser.value == null && users.isNotEmpty()) {
                    _currentUser.value = users.firstOrNull { it.role == "ADMIN" } ?: users.first()
                }
            }
        }
    }

    // AUTHENTICATION
    fun loginWithPin(username: String, pin: String): Boolean {
        val user = allUsers.value.find { it.username.equals(username, ignoreCase = true) && it.pin == pin }
        return if (user != null) {
            _currentUser.value = user
            true
        } else {
            false
        }
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }

    val isAdmin: Boolean
        get() = _currentUser.value?.role == "ADMIN"

    // POS CART LOGIC
    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) {
            viewModelScope.launch { _snackbarMessage.emit("Product is out of stock!") }
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val existing = current[index]
            if (existing.quantity >= product.stock) {
                viewModelScope.launch { _snackbarMessage.emit("Maximum available stock reached (${product.stock})") }
                return
            }
            current[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = current
    }

    fun addByBarcode(barcode: String): Boolean {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return false
        val product = allProducts.value.find { it.barcode == trimmed }
        return if (product != null) {
            addToCart(product)
            true
        } else {
            viewModelScope.launch { _snackbarMessage.emit("No product found with barcode: $trimmed") }
            false
        }
    }

    fun incrementQuantity(productId: Long) {
        val product = allProducts.value.find { it.id == productId } ?: return
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val existing = current[index]
            if (existing.quantity >= product.stock) {
                viewModelScope.launch { _snackbarMessage.emit("Cannot exceed stock limit (${product.stock})") }
                return
            }
            current[index] = existing.copy(quantity = existing.quantity + 1)
            _cartItems.value = current
        }
    }

    fun decrementQuantity(productId: Long) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val existing = current[index]
            if (existing.quantity > 1) {
                current[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    fun removeCartItem(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _posDiscount.value = 0.0
        _selectedCustomer.value = null
    }

    fun setDiscount(amount: Double) {
        _posDiscount.value = amount.coerceAtLeast(0.0)
    }

    fun setTaxRate(percent: Double) {
        _posTaxRate.value = percent.coerceAtLeast(0.0)
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun completeSale(
        paidAmount: Double,
        paymentMethod: String,
        onSuccess: (SaleWithItems) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            viewModelScope.launch { _snackbarMessage.emit("Cart is empty!") }
            return
        }

        val user = _currentUser.value ?: UserEntity(
            username = "cashier",
            fullName = "Cashier",
            pin = "1111",
            role = "CASHIER"
        )
        val customer = _selectedCustomer.value
        val subtotal = items.sumOf { it.subtotal }
        val discount = _posDiscount.value
        val tax = (subtotal - discount).coerceAtLeast(0.0) * (_posTaxRate.value / 100.0)
        val total = (subtotal - discount + tax).coerceAtLeast(0.0)

        if (paidAmount < total) {
            viewModelScope.launch { _snackbarMessage.emit("Paid amount (Rs. $paidAmount) is less than Total (Rs. $total)") }
            return
        }

        viewModelScope.launch {
            try {
                val saleWithItems = repository.completeSale(
                    cartItems = items,
                    customerId = customer?.id,
                    customerName = customer?.name ?: "Walk-in Customer",
                    cashierId = user.id,
                    cashierName = user.fullName,
                    discount = discount,
                    tax = tax,
                    paidAmount = paidAmount,
                    paymentMethod = paymentMethod
                )
                _viewingInvoice.value = saleWithItems
                clearCart()
                _snackbarMessage.emit("Sale completed successfully! Invoice #${saleWithItems.sale.invoiceNumber}")
                onSuccess(saleWithItems)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to complete sale: ${e.localizedMessage}")
            }
        }
    }

    fun setViewingInvoice(sale: SaleEntity) {
        viewModelScope.launch {
            val items = repository.getSaleItems(sale.id)
            _viewingInvoice.value = SaleWithItems(sale, items)
        }
    }

    // PRODUCT MANAGEMENT
    fun saveProduct(
        id: Long = 0,
        name: String,
        barcode: String,
        categoryId: Long,
        categoryName: String,
        buyingPrice: Double,
        sellingPrice: Double,
        stock: Int,
        minStock: Int,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                id = id,
                name = name.trim(),
                barcode = barcode.trim(),
                categoryId = categoryId,
                categoryName = categoryName,
                buyingPrice = buyingPrice,
                sellingPrice = sellingPrice,
                stock = stock,
                minStock = minStock,
                imageUrl = imageUrl
            )
            if (id == 0L) {
                repository.insertProduct(product)
                _snackbarMessage.emit("Product '${product.name}' created")
            } else {
                repository.updateProduct(product)
                _snackbarMessage.emit("Product '${product.name}' updated")
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        if (!isAdmin) {
            viewModelScope.launch { _snackbarMessage.emit("Only Admin can delete products") }
            return
        }
        viewModelScope.launch {
            repository.deleteProduct(product)
            _snackbarMessage.emit("Product '${product.name}' deleted")
        }
    }

    // CATEGORY MANAGEMENT
    fun saveCategory(id: Long = 0, name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val category = CategoryEntity(
                id = id,
                name = name.trim(),
                colorHex = colorHex,
                iconName = iconName
            )
            if (id == 0L) {
                repository.insertCategory(category)
                _snackbarMessage.emit("Category '${category.name}' created")
            } else {
                repository.updateCategory(category)
                _snackbarMessage.emit("Category '${category.name}' updated")
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (!isAdmin) {
            viewModelScope.launch { _snackbarMessage.emit("Only Admin can delete categories") }
            return
        }
        viewModelScope.launch {
            repository.deleteCategory(category)
            _snackbarMessage.emit("Category '${category.name}' deleted")
        }
    }

    // CUSTOMER MANAGEMENT
    fun saveCustomer(
        id: Long = 0,
        name: String,
        phone: String,
        email: String,
        address: String,
        creditBalance: Double = 0.0
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                creditBalance = creditBalance
            )
            if (id == 0L) {
                repository.insertCustomer(customer)
                _snackbarMessage.emit("Customer '${customer.name}' registered")
            } else {
                repository.updateCustomer(customer)
                _snackbarMessage.emit("Customer '${customer.name}' updated")
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _snackbarMessage.emit("Customer '${customer.name}' removed")
        }
    }

    // STOCK ADJUSTMENT
    fun adjustStock(productId: Long, qtyChange: Int, reason: String) {
        val userName = _currentUser.value?.fullName ?: "Admin"
        viewModelScope.launch {
            val success = repository.adjustStock(productId, qtyChange, reason, userName)
            if (success) {
                _snackbarMessage.emit("Stock adjusted by $qtyChange ($reason)")
            } else {
                _snackbarMessage.emit("Failed to adjust stock")
            }
        }
    }

    // EXPENSE MANAGEMENT
    fun saveExpense(
        id: Long = 0,
        title: String,
        category: String,
        amount: Double,
        paymentMethod: String,
        notes: String
    ) {
        val user = _currentUser.value?.fullName ?: "Admin"
        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = id,
                title = title.trim(),
                category = category.trim(),
                amount = amount,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                recordedBy = user
            )
            if (id == 0L) {
                repository.insertExpense(expense)
                _snackbarMessage.emit("Expense '${expense.title}' recorded")
            } else {
                repository.updateExpense(expense)
                _snackbarMessage.emit("Expense '${expense.title}' updated")
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        if (!isAdmin) {
            viewModelScope.launch { _snackbarMessage.emit("Only Admin can delete expenses") }
            return
        }
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _snackbarMessage.emit("Expense deleted")
        }
    }

    // USER MANAGEMENT
    fun saveUser(
        id: Long = 0,
        username: String,
        fullName: String,
        pin: String,
        role: String,
        email: String,
        phone: String
    ) {
        if (!isAdmin) {
            viewModelScope.launch { _snackbarMessage.emit("Only Admin can manage users") }
            return
        }
        viewModelScope.launch {
            val user = UserEntity(
                id = id,
                username = username.trim(),
                fullName = fullName.trim(),
                pin = pin.trim(),
                role = role,
                email = email.trim(),
                phone = phone.trim()
            )
            if (id == 0L) {
                repository.insertUser(user)
                _snackbarMessage.emit("User '${user.fullName}' created")
            } else {
                repository.updateUser(user)
                _snackbarMessage.emit("User '${user.fullName}' updated")
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        if (!isAdmin) {
            viewModelScope.launch { _snackbarMessage.emit("Only Admin can delete users") }
            return
        }
        if (user.id == _currentUser.value?.id) {
            viewModelScope.launch { _snackbarMessage.emit("Cannot delete currently active user!") }
            return
        }
        viewModelScope.launch {
            repository.deleteUser(user)
            _snackbarMessage.emit("User '${user.fullName}' removed")
        }
    }

    // SETTINGS
    fun saveSettings(
        shopName: String,
        phone: String,
        email: String,
        address: String,
        currencySymbol: String,
        defaultTaxRate: Double,
        receiptFooter: String
    ) {
        if (!isAdmin) {
            viewModelScope.launch { _snackbarMessage.emit("Only Admin can update shop settings") }
            return
        }
        viewModelScope.launch {
            val s = ShopSettingsEntity(
                id = 1,
                shopName = shopName.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                currencySymbol = currencySymbol.trim(),
                defaultTaxRate = defaultTaxRate,
                receiptFooter = receiptFooter.trim()
            )
            repository.updateSettings(s)
            _snackbarMessage.emit("Shop settings updated successfully")
        }
    }

    fun resetToSampleData() {
        if (!isAdmin) {
            viewModelScope.launch { _snackbarMessage.emit("Only Admin can reset database") }
            return
        }
        viewModelScope.launch {
            repository.resetDatabase()
            _snackbarMessage.emit("Database reset to sample store data")
        }
    }

    // REPORTS CALCULATION (Real calculated metrics from live database)
    val reportsData: StateFlow<ReportsData> = combine(
        allSales,
        allSaleItems,
        allExpenses,
        allProducts
    ) { sales, items, expenses, products ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val todaySales = sales.filter { it.timestamp >= startOfDay }
        val todayTotal = todaySales.sumOf { it.total }
        val totalRevenue = sales.sumOf { it.total }

        // Total Cost of Goods from sale items
        val totalCostOfGoods = items.sumOf { it.unitBuyingPrice * it.quantity }
        val grossProfit = (totalRevenue - totalCostOfGoods).coerceAtLeast(0.0)
        val totalExpenseAmt = expenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenseAmt

        // Best selling products
        val productSalesMap = items.groupBy { it.productId }
        val bestSelling = productSalesMap.map { (prodId, prodItems) ->
            val name = prodItems.firstOrNull()?.productName ?: "Product #$prodId"
            val qty = prodItems.sumOf { it.quantity }
            val rev = prodItems.sumOf { it.totalPrice }
            BestSellingItem(productId = prodId, productName = name, totalQuantitySold = qty, totalRevenue = rev)
        }.sortedByDescending { it.totalQuantitySold }.take(10)

        // Daily and monthly breakdown
        val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        val dailyMap = sales.groupBy { dayFormat.format(Date(it.timestamp)) }
            .mapValues { entry -> entry.value.sumOf { it.total } }

        val monthlyMap = sales.groupBy { monthFormat.format(Date(it.timestamp)) }
            .mapValues { entry -> entry.value.sumOf { it.total } }

        val lowCount = products.count { it.stock <= it.minStock }
        val stockValuation = products.sumOf { it.buyingPrice * it.stock }
        val stockItems = products.sumOf { it.stock }

        ReportsData(
            todaySalesTotal = todayTotal,
            todaySalesCount = todaySales.size,
            totalRevenue = totalRevenue,
            totalCostOfGoods = totalCostOfGoods,
            grossProfit = grossProfit,
            totalExpenses = totalExpenseAmt,
            netProfit = netProfit,
            lowStockCount = lowCount,
            totalStockValuation = stockValuation,
            totalStockItemsCount = stockItems,
            bestSellingProducts = bestSelling,
            dailyRevenueMap = dailyMap,
            monthlyRevenueMap = monthlyMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsData())
}
