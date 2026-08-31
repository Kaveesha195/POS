package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.BestSellingItem
import com.example.ui.MainViewModel
import com.example.ui.components.StatCard
import com.example.ui.theme.PosIndigo
import com.example.ui.theme.PosPurple
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosWarning

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val reportsData by viewModel.reportsData.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol.ifBlank { "Rs." }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Profit & Revenue Summary Matrix
        item {
            Text(
                text = "Financial & Profit Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "TOTAL REVENUE",
                        value = "$currency %.2f".format(reportsData.totalRevenue),
                        subtitle = "Gross Sales",
                        icon = Icons.Default.Payments,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "COST OF GOODS",
                        value = "$currency %.2f".format(reportsData.totalCostOfGoods),
                        subtitle = "Inventory purchase cost",
                        icon = Icons.Default.ShoppingCart,
                        color = PosWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "GROSS PROFIT",
                        value = "$currency %.2f".format(reportsData.grossProfit),
                        subtitle = "Revenue minus COGS",
                        icon = Icons.Default.TrendingUp,
                        color = PosIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "TOTAL EXPENSES",
                        value = "$currency %.2f".format(reportsData.totalExpenses),
                        subtitle = "Operating expenses",
                        icon = Icons.Default.MoneyOff,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Highlighted Net Profit Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reports_net_profit_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reportsData.netProfit >= 0) PosSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET STORE PROFIT",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (reportsData.netProfit >= 0) PosSuccess else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "$currency %.2f".format(reportsData.netProfit),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (reportsData.netProfit >= 0) PosSuccess else MaterialTheme.colorScheme.error
                                )
                            )
                            Text(
                                text = "Realized after deducting COGS and all store expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = if (reportsData.netProfit >= 0) Icons.Default.MonetizationOn else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (reportsData.netProfit >= 0) PosSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        // Inventory Stock Valuation
        item {
            Text(
                text = "Inventory Stock Valuation",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "STOCK ASSET VALUE",
                    value = "$currency %.2f".format(reportsData.totalStockValuation),
                    subtitle = "Based on purchase cost",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = PosPurple,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "TOTAL ITEMS IN STOCK",
                    value = "${reportsData.totalStockItemsCount} Units",
                    subtitle = "${reportsData.lowStockCount} items at low stock",
                    icon = Icons.Default.Inventory,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top 10 Best Selling Products Leaderboard
        item {
            Text(
                text = "Top Best-Selling Products",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (reportsData.bestSellingProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No sales data available yet to rank best-sellers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            itemsIndexed(reportsData.bestSellingProducts) { index, item ->
                BestSellerLeaderboardRow(
                    rank = index + 1,
                    item = item,
                    currency = currency
                )
            }
        }

        // Monthly Breakdown Table
        item {
            Text(
                text = "Monthly Revenue Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (reportsData.monthlyRevenueMap.isEmpty()) {
                        Text("No monthly data recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        reportsData.monthlyRevenueMap.forEach { (month, rev) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(month, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$currency %.2f".format(rev),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BestSellerLeaderboardRow(
    rank: Int,
    item: BestSellingItem,
    currency: String
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFF59E0B) // Gold
        2 -> Color(0xFF94A3B8) // Silver
        3 -> Color(0xFFB45309) // Bronze
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.Bold,
                        color = rankColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${item.totalQuantitySold} Units Sold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "$currency %.2f".format(item.totalRevenue),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
