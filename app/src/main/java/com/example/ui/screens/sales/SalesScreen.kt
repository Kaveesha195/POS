package com.example.ui.screens.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SaleEntity
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.PosIndigo
import com.example.ui.theme.PosSuccess
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesScreen(
    viewModel: MainViewModel,
    onViewSale: (SaleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.allSales.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol.ifBlank { "Rs." }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentFilter by remember { mutableStateOf<String?>(null) } // null = All, "CASH", "CARD", "BANK"

    val filteredSales = remember(sales, searchQuery, selectedPaymentFilter) {
        sales.filter { sale ->
            val matchPayment = selectedPaymentFilter == null || sale.paymentMethod.equals(selectedPaymentFilter, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    sale.customerName.contains(searchQuery, ignoreCase = true) ||
                    sale.cashierName.contains(searchQuery, ignoreCase = true)
            matchPayment && matchQuery
        }
    }

    val totalSum = filteredSales.sumOf { it.total }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search invoice #, customer name, cashier...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Payment Method Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedPaymentFilter == null,
                    onClick = { selectedPaymentFilter = null },
                    label = { Text("All Payments (${sales.size})") }
                )
            }
            item {
                FilterChip(
                    selected = selectedPaymentFilter == "CASH",
                    onClick = { selectedPaymentFilter = "CASH" },
                    label = { Text("Cash") }
                )
            }
            item {
                FilterChip(
                    selected = selectedPaymentFilter == "CARD",
                    onClick = { selectedPaymentFilter = "CARD" },
                    label = { Text("Card") }
                )
            }
            item {
                FilterChip(
                    selected = selectedPaymentFilter == "BANK",
                    onClick = { selectedPaymentFilter = "BANK" },
                    label = { Text("Bank") }
                )
            }
        }

        // Summary Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredSales.size} Invoices",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Total: $currency %.2f".format(totalSum),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Sales List
        if (filteredSales.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sales records found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSales, key = { it.id }) { sale ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewSale(sale) }
                            .testTag("sale_card_${sale.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    text = sale.invoiceNumber,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Customer: ${sale.customerName} • Cashier: ${sale.cashierName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(Date(sale.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currency %.2f".format(sale.total),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(
                                    text = sale.paymentMethod,
                                    color = when (sale.paymentMethod) {
                                        "CASH" -> PosSuccess
                                        "CARD" -> MaterialTheme.colorScheme.primary
                                        else -> PosIndigo
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
