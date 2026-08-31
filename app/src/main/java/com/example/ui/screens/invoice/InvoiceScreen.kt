package com.example.ui.screens.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceScreen(
    viewModel: MainViewModel,
    onBackToPos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val invoiceWithItems by viewModel.viewingInvoice.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol.ifBlank { "Rs." }

    val sale = invoiceWithItems?.sale
    val items = invoiceWithItems?.items ?: emptyList()
    val dateFormat = SimpleDateFormat("dd-MM-yyyy  hh:mm:ss a", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Receipt Paper Card
        Card(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .testTag("receipt_paper_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Store Header
                Text(
                    text = settings.shopName.ifBlank { "GK POS" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (settings.address.isNotBlank()) {
                    Text(
                        text = settings.address,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (settings.phone.isNotBlank() || settings.email.isNotBlank()) {
                    Text(
                        text = "Tel: ${settings.phone} ${if (settings.email.isNotBlank()) "| ${settings.email}" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                DashedDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Invoice Meta Info
                ReceiptMetaRow(label = "INVOICE #", value = sale?.invoiceNumber ?: "INV-0000")
                ReceiptMetaRow(label = "DATE/TIME", value = if (sale != null) dateFormat.format(Date(sale.timestamp)) else "")
                ReceiptMetaRow(label = "CASHIER", value = sale?.cashierName ?: "Cashier")
                ReceiptMetaRow(label = "CUSTOMER", value = sale?.customerName ?: "Walk-in Customer")

                Spacer(modifier = Modifier.height(12.dp))
                DashedDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Items Table Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("ITEM", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                    Text("QTY", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                    Text("PRICE", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                    Text("TOTAL", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Items List
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.productName,
                            modifier = Modifier.weight(1.8f),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 1
                        )
                        Text(
                            text = "${item.quantity}",
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "%.2f".format(item.unitSellingPrice),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "%.2f".format(item.totalPrice),
                            modifier = Modifier.weight(1.1f),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                DashedDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Total Summary
                ReceiptSummaryRow(label = "Subtotal", value = "$currency %.2f".format(sale?.subtotal ?: 0.0))
                if ((sale?.discount ?: 0.0) > 0) {
                    ReceiptSummaryRow(label = "Discount", value = "- $currency %.2f".format(sale?.discount ?: 0.0))
                }
                if ((sale?.tax ?: 0.0) > 0) {
                    ReceiptSummaryRow(label = "Tax", value = "+ $currency %.2f".format(sale?.tax ?: 0.0))
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface, thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GRAND TOTAL",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = "$currency %.2f".format(sale?.total ?: 0.0),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                ReceiptSummaryRow(label = "Paid Amount (${sale?.paymentMethod ?: "CASH"})", value = "$currency %.2f".format(sale?.paidAmount ?: 0.0))
                ReceiptSummaryRow(label = "Change Returned", value = "$currency %.2f".format(sale?.changeAmount ?: 0.0))

                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Footer Note
                Text(
                    text = settings.receiptFooter.ifBlank { "Thank you for shopping with us! Please visit again." },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBackToPos,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Sale / POS")
            }

            Button(
                onClick = onBackToPos,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("receipt_print_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Receipt")
            }
        }
    }
}

@Composable
fun ReceiptMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace))
    }
}

@Composable
fun ReceiptSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace))
    }
}

@Composable
fun DashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
