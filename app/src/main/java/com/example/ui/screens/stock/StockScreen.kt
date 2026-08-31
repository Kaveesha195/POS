package com.example.ui.screens.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockHistoryEntity
import com.example.ui.MainViewModel
import com.example.ui.components.StockBadge
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosWarning
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val stockHistory by viewModel.allStockHistory.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inventory Overview, 1: Low Stock Alerts, 2: History Log
    var adjustingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Inventory (${products.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Low Stock (${lowStockProducts.size})", fontWeight = FontWeight.SemiBold)
                        if (lowStockProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PosWarning)
                            )
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Stock Audit Log", fontWeight = FontWeight.SemiBold) }
            )
        }

        when (selectedTab) {
            0 -> InventoryTab(
                products = products,
                onAdjustStock = { adjustingProduct = it }
            )
            1 -> LowStockTab(
                lowStockProducts = lowStockProducts,
                onAdjustStock = { adjustingProduct = it }
            )
            2 -> StockHistoryTab(
                stockHistory = stockHistory
            )
        }
    }

    if (adjustingProduct != null) {
        StockAdjustmentDialog(
            product = adjustingProduct!!,
            onDismiss = { adjustingProduct = null },
            onConfirm = { qtyChange, reason ->
                viewModel.adjustStock(adjustingProduct!!.id, qtyChange, reason)
                adjustingProduct = null
            }
        )
    }
}

@Composable
fun InventoryTab(
    products: List<ProductEntity>,
    onAdjustStock: (ProductEntity) -> Unit
) {
    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No products in inventory", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { it.id }) { product ->
                StockProductCard(
                    product = product,
                    onAdjustStock = { onAdjustStock(product) }
                )
            }
        }
    }
}

@Composable
fun LowStockTab(
    lowStockProducts: List<ProductEntity>,
    onAdjustStock: (ProductEntity) -> Unit
) {
    if (lowStockProducts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = PosSuccess, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("All product stocks are at healthy levels!", fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lowStockProducts, key = { it.id }) { product ->
                StockProductCard(
                    product = product,
                    onAdjustStock = { onAdjustStock(product) },
                    highlightWarning = true
                )
            }
        }
    }
}

@Composable
fun StockProductCard(
    product: ProductEntity,
    onAdjustStock: () -> Unit,
    highlightWarning: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock_item_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlightWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Barcode: ${product.barcode} • Min Alert Level: ${product.minStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                StockBadge(stock = product.stock, minStock = product.minStock)
            }

            Button(
                onClick = onAdjustStock,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("adjust_stock_btn_${product.id}")
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Adjust Stock")
            }
        }
    }
}

@Composable
fun StockHistoryTab(stockHistory: List<StockHistoryEntity>) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    if (stockHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No stock changes recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stockHistory, key = { it.id }) { log ->
                val isPositive = log.changeQty > 0
                val qtyColor = if (isPositive) PosSuccess else MaterialTheme.colorScheme.error

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.productName, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Reason: ${log.reason} • By: ${log.recordedBy}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isPositive) "+${log.changeQty}" else "${log.changeQty}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qtyColor
                                )
                            )
                            Text(
                                text = "Stock: ${log.previousStock} → ${log.newStock}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (qtyChange: Int, reason: String) -> Unit
) {
    var isRestock by remember { mutableStateOf(true) }
    var quantityInput by remember { mutableStateOf("10") }
    var selectedReason by remember { mutableStateOf("Supplier Restock") }
    var customReason by remember { mutableStateOf("") }

    val presetReasons = listOf("Supplier Restock", "Inventory Count Adjustment", "Damaged / Expired Goods", "Customer Return", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stock Adjustment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${product.name}\nCurrent Stock: ${product.stock} units",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Action Type Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isRestock,
                        onClick = { isRestock = true },
                        label = { Text("Stock In (+)") },
                        leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null, tint = PosSuccess) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isRestock,
                        onClick = { isRestock = false },
                        label = { Text("Stock Out (-)") },
                        leadingIcon = { Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Adjustment Quantity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Reason:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetReasons.forEach { reason ->
                        Surface(
                            onClick = { selectedReason = reason },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedReason == reason) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityInput.toIntOrNull() ?: 0
                    if (qty > 0) {
                        val change = if (isRestock) qty else -qty
                        val reason = if (selectedReason == "Other" && customReason.isNotBlank()) customReason else selectedReason
                        onConfirm(change, reason)
                    }
                }
            ) {
                Text("Apply Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
