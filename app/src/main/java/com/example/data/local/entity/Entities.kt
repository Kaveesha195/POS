package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "GK POS Retail",
    val phone: String = "+94 77 123 4567",
    val email: String = "support@gkpos.lk",
    val address: String = "45 Galle Road, Colombo 03",
    val currencySymbol: String = "Rs.",
    val defaultTaxRate: Double = 0.0,
    val receiptFooter: String = "Thank you for shopping with us! Please come again."
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val pin: String,
    val role: String, // "ADMIN" or "CASHIER"
    val email: String = "",
    val phone: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "#3B82F6"
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String,
    val categoryId: Long = 0,
    val categoryName: String = "General",
    val buyingPrice: Double,
    val sellingPrice: Double,
    val stock: Int,
    val minStock: Int = 5,
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val creditBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val cashierId: Long,
    val cashierName: String,
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val paidAmount: Double,
    val changeAmount: Double,
    val paymentMethod: String, // "CASH", "CARD", "BANK"
    val status: String = "COMPLETED",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val unitBuyingPrice: Double,
    val unitSellingPrice: Double,
    val totalPrice: Double
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val paymentMethod: String = "CASH",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val recordedBy: String = "Admin"
)

@Entity(tableName = "stock_history")
data class StockHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val changeQty: Int,
    val previousStock: Int,
    val newStock: Int,
    val reason: String, // "SALE", "RESTOCK", "ADJUSTMENT", "DAMAGE"
    val timestamp: Long = System.currentTimeMillis(),
    val recordedBy: String = "System"
)
