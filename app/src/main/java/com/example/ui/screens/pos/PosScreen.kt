package com.example.ui.screens.pos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.CartItem
import com.example.ui.MainViewModel
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StockBadge
import com.example.ui.theme.PosIndigo
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: MainViewModel,
    onSaleCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val discount by viewModel.posDiscount.collectAsStateWithLifecycle()
    val taxRate by viewModel.posTaxRate.collectAsStateWithLifecycle()

    val currency = settings.currencySymbol.ifBlank { "Rs." }

    var searchQuery by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) } // null = All
    var showPaymentModal by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showMobileCartSheet by remember { mutableStateOf(false) }

    // Filter products
    val filteredProducts = remember(products, searchQuery, selectedCategoryId) {
        products.filter { prod ->
            val matchCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchQuery = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery, ignoreCase = true) ||
                    prod.categoryName.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    val subtotal = cartItems.sumOf { it.subtotal }
    val taxAmount = (subtotal - discount).coerceAtLeast(0.0) * (taxRate / 100.0)
    val grandTotal = (subtotal - discount + taxAmount).coerceAtLeast(0.0)
    val totalCartCount = cartItems.sumOf { it.quantity }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            // Tablet / Desktop Split Screen POS Workspace
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Catalog Pane (60%)
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PosSearchBarSection(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        barcodeInput = barcodeInput,
                        onBarcodeChange = { barcodeInput = it },
                        onBarcodeScan = {
                            if (viewModel.addByBarcode(barcodeInput)) {
                                barcodeInput = ""
                            }
                        }
                    )

                    PosCategoryChipsSection(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelect = { selectedCategoryId = it }
                    )

                    PosProductGridSection(
                        products = filteredProducts,
                        currency = currency,
                        onAddToCart = { viewModel.addToCart(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Right Cart & Checkout Pane (40%)
                Card(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .testTag("pos_cart_panel"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    CartPaneContent(
                        cartItems = cartItems,
                        selectedCustomer = selectedCustomer,
                        currency = currency,
                        subtotal = subtotal,
                        discount = discount,
                        taxRate = taxRate,
                        taxAmount = taxAmount,
                        grandTotal = grandTotal,
                        onIncrement = { viewModel.incrementQuantity(it) },
                        onDecrement = { viewModel.decrementQuantity(it) },
                        onRemove = { viewModel.removeCartItem(it) },
                        onClearCart = { viewModel.clearCart() },
                        onSelectCustomerClick = { showCustomerPicker = true },
                        onDiscountChange = { viewModel.setDiscount(it) },
                        onTaxRateChange = { viewModel.setTaxRate(it) },
                        onProceedToPay = {
                            if (cartItems.isNotEmpty()) showPaymentModal = true
                        }
                    )
                }
            }
        } else {
            // Mobile Responsive POS Layout with Floating Cart Bar & Bottom Sheet
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PosSearchBarSection(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    barcodeInput = barcodeInput,
                    onBarcodeChange = { barcodeInput = it },
                    onBarcodeScan = {
                        if (viewModel.addByBarcode(barcodeInput)) {
                            barcodeInput = ""
                        }
                    }
                )

                PosCategoryChipsSection(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelect = { selectedCategoryId = it }
                )

                PosProductGridSection(
                    products = filteredProducts,
                    currency = currency,
                    onAddToCart = { viewModel.addToCart(it) },
                    modifier = Modifier.weight(1f),
                    columns = 2
                )

                // Mobile Bottom Cart Summary Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_mobile_cart_bar"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$totalCartCount Items in Cart",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$currency %.2f".format(grandTotal),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Button(
                            onClick = { showMobileCartSheet = true },
                            enabled = cartItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pos_view_cart_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VIEW CART", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Mobile Modal Bottom Sheet for Cart
            if (showMobileCartSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMobileCartSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Box(modifier = Modifier.fillMaxHeight(0.85f)) {
                        CartPaneContent(
                            cartItems = cartItems,
                            selectedCustomer = selectedCustomer,
                            currency = currency,
                            subtotal = subtotal,
                            discount = discount,
                            taxRate = taxRate,
                            taxAmount = taxAmount,
                            grandTotal = grandTotal,
                            onIncrement = { viewModel.incrementQuantity(it) },
                            onDecrement = { viewModel.decrementQuantity(it) },
                            onRemove = { viewModel.removeCartItem(it) },
                            onClearCart = {
                                viewModel.clearCart()
                                showMobileCartSheet = false
                            },
                            onSelectCustomerClick = { showCustomerPicker = true },
                            onDiscountChange = { viewModel.setDiscount(it) },
                            onTaxRateChange = { viewModel.setTaxRate(it) },
                            onProceedToPay = {
                                showMobileCartSheet = false
                                showPaymentModal = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Customer Selection Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            selectedCustomer = selectedCustomer,
            onCustomerSelected = {
                viewModel.selectCustomer(it)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false }
        )
    }

    // Payment & Change Modal Dialog
    if (showPaymentModal) {
        PaymentDialog(
            currency = currency,
            grandTotal = grandTotal,
            onDismiss = { showPaymentModal = false },
            onConfirmPayment = { paidAmount, paymentMethod ->
                showPaymentModal = false
                viewModel.completeSale(paidAmount, paymentMethod) {
                    onSaleCompleted()
                }
            }
        )
    }
}

@Composable
fun PosSearchBarSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    barcodeInput: String,
    onBarcodeChange: (String) -> Unit,
    onBarcodeScan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Text Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search product name, code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("pos_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Barcode Quick Scan Input
        OutlinedTextField(
            value = barcodeInput,
            onValueChange = onBarcodeChange,
            placeholder = { Text("Barcode") },
            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode") },
            trailingIcon = {
                IconButton(onClick = onBarcodeScan, modifier = Modifier.testTag("pos_barcode_scan_btn")) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Add by barcode", tint = MaterialTheme.colorScheme.primary)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onBarcodeScan() }),
            singleLine = true,
            modifier = Modifier
                .width(160.dp)
                .testTag("pos_barcode_input"),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun PosCategoryChipsSection(
    categories: List<com.example.data.local.entity.CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Products") },
                leadingIcon = {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.testTag("pos_cat_chip_all")
            )
        }

        items(categories) { cat ->
            FilterChip(
                selected = selectedCategoryId == cat.id,
                onClick = { onCategorySelect(cat.id) },
                label = { Text(cat.name) },
                modifier = Modifier.testTag("pos_cat_chip_${cat.id}")
            )
        }
    }
}

@Composable
fun PosProductGridSection(
    products: List<ProductEntity>,
    currency: String,
    onAddToCart: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No products found matching filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
                .fillMaxSize()
                .testTag("pos_product_grid")
        ) {
            items(products, key = { it.id }) { product ->
                ProductGridCard(
                    product = product,
                    currency = currency,
                    onClick = { onAddToCart(product) }
                )
            }
        }
    }
}

@Composable
fun ProductGridCard(
    product: ProductEntity,
    currency: String,
    onClick: () -> Unit
) {
    val isOutOfStock = product.stock <= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) { onClick() }
            .testTag("pos_product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                StockBadge(stock = product.stock, minStock = product.minStock)
                Text(
                    text = product.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Code: ${product.barcode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currency %.2f".format(product.sellingPrice),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                FilledIconButton(
                    onClick = onClick,
                    enabled = !isOutOfStock,
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add to Cart", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun CartPaneContent(
    cartItems: List<CartItem>,
    selectedCustomer: CustomerEntity?,
    currency: String,
    subtotal: Double,
    discount: Double,
    taxRate: Double,
    taxAmount: Double,
    grandTotal: Double,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomerClick: () -> Unit,
    onDiscountChange: (Double) -> Unit,
    onTaxRateChange: (Double) -> Unit,
    onProceedToPay: () -> Unit
) {
    var discountInput by remember(discount) { mutableStateOf(if (discount > 0) discount.toString() else "") }
    var taxRateInput by remember(taxRate) { mutableStateOf(if (taxRate > 0) taxRate.toString() else "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cart Header & Customer Picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Cart (${cartItems.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (cartItems.isNotEmpty()) {
                IconButton(onClick = onClearCart, modifier = Modifier.testTag("pos_clear_cart_button")) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Cart", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Customer selection bar
        Surface(
            onClick = onSelectCustomerClick,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pos_select_customer_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedCustomer?.name ?: "Walk-in Customer (Select)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Cart Items List
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.RemoveShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cart is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Select products from the left to add",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        currency = currency,
                        onIncrement = { onIncrement(item.product.id) },
                        onDecrement = { onDecrement(item.product.id) },
                        onRemove = { onRemove(item.product.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Discounts & Tax Inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = discountInput,
                onValueChange = {
                    discountInput = it
                    onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                },
                label = { Text("Discount ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_discount_input"),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = taxRateInput,
                onValueChange = {
                    taxRateInput = it
                    onTaxRateChange(it.toDoubleOrNull() ?: 0.0)
                },
                label = { Text("Tax (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_tax_input"),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pricing Summary Lines
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SummaryLine(label = "Subtotal", value = "$currency %.2f".format(subtotal))
            if (discount > 0) {
                SummaryLine(label = "Discount", value = "- $currency %.2f".format(discount), valueColor = PosWarning)
            }
            if (taxAmount > 0) {
                SummaryLine(label = "Tax ($taxRate%)", value = "+ $currency %.2f".format(taxAmount))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL DUE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$currency %.2f".format(grandTotal),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pay Button
        Button(
            onClick = onProceedToPay,
            enabled = cartItems.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("pos_proceed_pay_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Payments, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PAY $currency %.2f".format(grandTotal),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    currency: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = "$currency %.2f each".format(item.product.sellingPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            QuantityStepper(
                quantity = item.quantity,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                testTagPrefix = "cart_qty_${item.product.id}"
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currency %.2f".format(item.subtotal),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun SummaryLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

@Composable
fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    onCustomerSelected: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = customers.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Customer", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search customer name, phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Surface(
                            onClick = { onCustomerSelected(null) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedCustomer == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Walk-in Customer (Default)",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(filtered) { c ->
                        Surface(
                            onClick = { onCustomerSelected(c) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedCustomer?.id == c.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(c.name, fontWeight = FontWeight.Bold)
                                if (c.phone.isNotBlank()) {
                                    Text(c.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    currency: String,
    grandTotal: Double,
    onDismiss: () -> Unit,
    onConfirmPayment: (paidAmount: Double, paymentMethod: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CASH") } // CASH, CARD, BANK
    var paidAmountInput by remember { mutableStateOf("%.2f".format(grandTotal)) }

    val paidAmount = paidAmountInput.toDoubleOrNull() ?: 0.0
    val changeAmount = (paidAmount - grandTotal).coerceAtLeast(0.0)
    val isAmountSufficient = paidAmount >= grandTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = PosSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checkout & Payment", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Total to pay banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL PAYABLE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$currency %.2f".format(grandTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Payment Method Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodTab(
                        title = "Cash",
                        icon = Icons.Default.Money,
                        isSelected = selectedMethod == "CASH",
                        onClick = { selectedMethod = "CASH" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodTab(
                        title = "Card",
                        icon = Icons.Default.CreditCard,
                        isSelected = selectedMethod == "CARD",
                        onClick = { selectedMethod = "CARD" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodTab(
                        title = "Bank",
                        icon = Icons.Default.AccountBalance,
                        isSelected = selectedMethod == "BANK",
                        onClick = { selectedMethod = "BANK" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Paid Amount Field
                OutlinedTextField(
                    value = paidAmountInput,
                    onValueChange = { paidAmountInput = it },
                    label = { Text("Tendered / Paid Amount ($currency)") },
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_payment_paid_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Cash Shortcut Buttons
                if (selectedMethod == "CASH") {
                    Text(
                        text = "Quick Cash:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickAmountButton(label = "Exact", onClick = { paidAmountInput = "%.2f".format(grandTotal) }, modifier = Modifier.weight(1f))
                        QuickAmountButton(label = "+500", onClick = {
                            val cur = paidAmountInput.toDoubleOrNull() ?: grandTotal
                            paidAmountInput = "%.2f".format(cur + 500.0)
                        }, modifier = Modifier.weight(1f))
                        QuickAmountButton(label = "+1000", onClick = {
                            val cur = paidAmountInput.toDoubleOrNull() ?: grandTotal
                            paidAmountInput = "%.2f".format(cur + 1000.0)
                        }, modifier = Modifier.weight(1f))
                        QuickAmountButton(label = "+5000", onClick = {
                            val cur = paidAmountInput.toDoubleOrNull() ?: grandTotal
                            paidAmountInput = "%.2f".format(cur + 5000.0)
                        }, modifier = Modifier.weight(1f))
                    }

                    // Change Calculation Banner
                    Surface(
                        color = if (isAmountSufficient) PosSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAmountSufficient) "Change to Return:" else "Remaining Amount Due:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAmountSufficient) PosSuccess else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isAmountSufficient) "$currency %.2f".format(changeAmount) else "$currency %.2f".format(grandTotal - paidAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isAmountSufficient) PosSuccess else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPayment(paidAmount, selectedMethod) },
                enabled = isAmountSufficient,
                colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("pos_payment_complete_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("COMPLETE SALE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PaymentMethodTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun QuickAmountButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        modifier = modifier
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
    }
}
