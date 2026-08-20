package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.ai.ExtractedInvoice
import com.example.data.demo.DemoData
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class InsufficientStockException(
    val productName: String,
    val availableStock: Double,
    val requestedQuantity: Double
) : Exception("Insufficient stock for '$productName': requested $requestedQuantity, but only $availableStock available in inventory.")

data class DashboardMetrics(
    val todaySalesTotal: Double = 0.0,
    val todayEstimatedGrossProfit: Double = 0.0,
    val todaySalesCount: Int = 0,
    val todayAvgTransactionValue: Double = 0.0,
    val totalProductsCount: Int = 0,
    val totalStockQuantity: Double = 0.0,
    val inventoryCostValue: Double = 0.0,
    val inventorySellingValue: Double = 0.0,
    val potentialGrossProfit: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val deadStockCount: Int = 0,
    val totalCustomerCreditOutstanding: Double = 0.0,
    val totalSupplierPayable: Double = 0.0
)

enum class RecommendationConfidence(val label: String, val description: String) {
    STRONG("High Confidence", "Frequent, consistent sales over past 30 days"),
    NORMAL("Normal", "Consistent sales history"),
    LIMITED_DATA("Limited Data", "Few sales recorded; recommendation based on buffer"),
    NO_HISTORY("No Sales History", "No recorded sales yet; based strictly on minimum stock threshold")
}

data class PurchaseRecommendation(
    val product: ProductEntity,
    val currentStock: Double,
    val avgDailySales: Double,
    val estimatedDaysRemaining: Double,
    val suggestedReorderQty: Double,
    val reason: String,
    val supplierName: String,
    val confidence: RecommendationConfidence = RecommendationConfidence.NORMAL,
    val isUrgent: Boolean = false
)

data class DeadStockItem(
    val product: ProductEntity,
    val currentStock: Double,
    val daysSinceLastSale: Int,
    val hasEverBeenSold: Boolean,
    val inventoryCost: Double,
    val suggestedAction: String
)

data class BackupPreview(
    val version: Int,
    val exportedAt: Long,
    val shopName: String,
    val productCount: Int,
    val customerCount: Int,
    val supplierCount: Int,
    val saleCount: Int,
    val purchaseCount: Int,
    val rawJson: String
)

sealed class UndoableAction(
    val actionId: String = UUID.randomUUID().toString(),
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    data class Sale(
        val saleId: String,
        val summary: String,
        val totalAmount: Double
    ) : UndoableAction(description = "Sale #$saleId ($summary - ₹${String.format("%.2f", totalAmount)})")

    data class Purchase(
        val purchaseId: String,
        val invoiceNumber: String,
        val supplierName: String,
        val totalCost: Double
    ) : UndoableAction(description = "Inward #$invoiceNumber from $supplierName (₹${String.format("%.2f", totalCost)})")

    data class StockAdjustment(
        val productId: String,
        val productName: String,
        val previousStock: Double,
        val adjustedStock: Double
    ) : UndoableAction(description = "Stock adjustment for $productName (${previousStock.toInt()} → ${adjustedStock.toInt()})")

    data class CustomerPayment(
        val customerId: String,
        val customerName: String,
        val amount: Double,
        val ledgerEntryId: String?
    ) : UndoableAction(description = "Khata payment of ₹${String.format("%.2f", amount)} from $customerName")
}

class ShopRepository(private val db: AppDatabase) {

    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllActiveProducts()
    val allCategories: Flow<List<String>> = db.productDao().getAllCategories()
    val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
    val outOfStockProducts: Flow<List<ProductEntity>> = db.productDao().getOutOfStockProducts()

    val allSales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    val allPurchases: Flow<List<PurchaseEntity>> = db.purchaseDao().getAllPurchases()
    val allStockMovements: Flow<List<StockMovementEntity>> = db.stockMovementDao().getAllMovements()

    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val customersWithCredit: Flow<List<CustomerEntity>> = db.customerDao().getCustomersWithCredit()

    val allSuppliers: Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()
    val priceHistory: Flow<List<PriceHistoryEntity>> = db.priceHistoryDao().getAllPriceHistory()
    val activeInsights: Flow<List<AIInsightEntity>> = db.aiInsightDao().getActiveInsights()
    val shopSettings: Flow<ShopSettingsEntity?> = db.shopSettingsDao().getSettingsFlow()

    // --- Product Operations ---
    suspend fun getProductById(id: String): ProductEntity? = withContext(Dispatchers.IO) {
        db.productDao().getProductById(id)
    }

    fun getProductByIdFlow(id: String): Flow<ProductEntity?> = db.productDao().getProductByIdFlow(id)

    suspend fun saveProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = db.productDao().getProductById(product.id)
            if (existing != null) {
                // Check if price changed -> Record Price History
                if (existing.costPrice != product.costPrice || existing.sellingPrice != product.sellingPrice) {
                    db.priceHistoryDao().insertPriceHistory(
                        PriceHistoryEntity(
                            productId = product.id,
                            productName = product.name,
                            oldCostPrice = existing.costPrice,
                            newCostPrice = product.costPrice,
                            oldSellingPrice = existing.sellingPrice,
                            newSellingPrice = product.sellingPrice,
                            reason = "Product details updated"
                        )
                    )
                }
                // Check if stock changed directly
                if (existing.currentStock != product.currentStock) {
                    val diff = product.currentStock - existing.currentStock
                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            type = StockMovementType.ADJUSTMENT,
                            quantity = diff,
                            previousStock = existing.currentStock,
                            newStock = product.currentStock,
                            reason = "Manual inventory quantity edit"
                        )
                    )
                }
                db.productDao().updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            } else {
                db.productDao().insertProduct(product)
                if (product.currentStock > 0) {
                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            type = StockMovementType.ADJUSTMENT,
                            quantity = product.currentStock,
                            previousStock = 0.0,
                            newStock = product.currentStock,
                            reason = "Initial product setup"
                        )
                    )
                }
            }
        }
    }

    suspend fun adjustStock(
        productId: String,
        quantityDelta: Double,
        type: StockMovementType,
        reason: String
    ): UndoableAction.StockAdjustment? = withContext(Dispatchers.IO) {
        db.withTransaction {
            val product = db.productDao().getProductById(productId) ?: return@withTransaction null
            val oldStock = product.currentStock
            val newStock = (oldStock + quantityDelta).coerceAtLeast(0.0)
            db.productDao().updateStock(productId, newStock)
            db.stockMovementDao().insertMovement(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = type,
                    quantity = quantityDelta,
                    previousStock = oldStock,
                    newStock = newStock,
                    reason = reason
                )
            )
            UndoableAction.StockAdjustment(
                productId = productId,
                productName = product.name,
                previousStock = oldStock,
                adjustedStock = newStock
            )
        }
    }

    suspend fun updateProductPrices(
        productId: String,
        newCostPrice: Double,
        newSellingPrice: Double,
        reason: String = "Price update"
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val product = db.productDao().getProductById(productId) ?: return@withTransaction
            db.priceHistoryDao().insertPriceHistory(
                PriceHistoryEntity(
                    productId = productId,
                    productName = product.name,
                    oldCostPrice = product.costPrice,
                    newCostPrice = newCostPrice,
                    oldSellingPrice = product.sellingPrice,
                    newSellingPrice = newSellingPrice,
                    reason = reason
                )
            )
            db.productDao().updatePrices(productId, newCostPrice, newSellingPrice)
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        db.productDao().updateProduct(product.copy(active = false, updatedAt = System.currentTimeMillis()))
    }

    // --- Transaction-Safe Sale Operations ---
    suspend fun recordSale(
        items: List<SaleItem>,
        paymentMethod: PaymentMethod,
        discount: Double = 0.0,
        customerId: String? = null,
        customerName: String = "",
        notes: String = ""
    ): Pair<SaleEntity, UndoableAction.Sale> = withContext(Dispatchers.IO) {
        db.withTransaction {
            if (items.isEmpty()) {
                throw IllegalArgumentException("Cannot record sale with empty item list.")
            }

            val saleId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            // 1. First Pass: Validate stock availability and resolve true product cost prices
            val resolvedItems = mutableListOf<SaleItem>()
            for (item in items) {
                if (item.productId.isNotBlank()) {
                    val prod = db.productDao().getProductById(item.productId)
                        ?: throw IllegalArgumentException("Product '${item.productName}' with id '${item.productId}' not found.")

                    if (item.quantity > prod.currentStock) {
                        throw InsufficientStockException(
                            productName = prod.name,
                            availableStock = prod.currentStock,
                            requestedQuantity = item.quantity
                        )
                    }

                    val effectiveUnitCost = prod.costPrice
                    val effectiveUnitPrice = if (item.unitPrice > 0) item.unitPrice else prod.sellingPrice
                    resolvedItems.add(
                        item.copy(
                            productName = prod.name,
                            unitPrice = effectiveUnitPrice,
                            costPrice = effectiveUnitCost,
                            total = item.quantity * effectiveUnitPrice
                        )
                    )
                } else {
                    resolvedItems.add(item)
                }
            }

            // 2. Second Pass: Perform atomic stock decrements and stock movements
            var totalSelling = 0.0
            var totalCost = 0.0
            val itemsArray = JSONArray()

            for (item in resolvedItems) {
                val obj = JSONObject().apply {
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("costPrice", item.costPrice)
                    put("total", item.total)
                }
                itemsArray.put(obj)
                totalSelling += item.total
                totalCost += (item.costPrice * item.quantity)

                if (item.productId.isNotBlank()) {
                    val prod = db.productDao().getProductById(item.productId)!!
                    val newStock = (prod.currentStock - item.quantity).coerceAtLeast(0.0)
                    db.productDao().updateStock(prod.id, newStock)
                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            productId = prod.id,
                            productName = prod.name,
                            type = StockMovementType.SALE,
                            quantity = -item.quantity,
                            previousStock = prod.currentStock,
                            newStock = newStock,
                            reason = "Sale #$saleId",
                            referenceId = saleId,
                            createdAt = now
                        )
                    )
                }
            }

            val finalTotal = (totalSelling - discount).coerceAtLeast(0.0)
            val grossProfit = finalTotal - totalCost

            val sale = SaleEntity(
                id = saleId,
                itemsJson = itemsArray.toString(),
                itemCount = resolvedItems.size,
                subtotal = totalSelling,
                discount = discount,
                total = finalTotal,
                estimatedCost = totalCost,
                estimatedGrossProfit = grossProfit,
                paymentMethod = paymentMethod,
                customerId = customerId,
                customerName = customerName,
                notes = notes,
                createdAt = now
            )

            db.saleDao().insertSale(sale)

            // 3. Handle Customer Credit if applicable
            if (paymentMethod == PaymentMethod.CREDIT && !customerId.isNullOrBlank()) {
                val customer = db.customerDao().getCustomerById(customerId)
                if (customer != null) {
                    val newBal = customer.creditBalance + finalTotal
                    db.customerDao().updateCreditBalance(customerId, finalTotal)
                    db.customerDao().insertLedgerEntry(
                        CustomerLedgerEntryEntity(
                            customerId = customerId,
                            type = "SALE_CREDIT",
                            amount = finalTotal,
                            balanceAfter = newBal,
                            referenceId = saleId,
                            note = "Credit Sale (${resolvedItems.size} items)",
                            createdAt = now
                        )
                    )
                }
            }

            val undoAction = UndoableAction.Sale(
                saleId = saleId,
                summary = "${resolvedItems.size} items",
                totalAmount = finalTotal
            )

            Pair(sale, undoAction)
        }
    }

    // --- Transaction-Safe Purchase Operations ---
    suspend fun recordPurchase(
        supplierId: String?,
        supplierName: String,
        invoiceNumber: String,
        items: List<PurchaseItem>,
        discount: Double = 0.0,
        tax: Double = 0.0,
        paymentStatus: PaymentStatus = PaymentStatus.PAID,
        notes: String = "",
        invoiceImageUrl: String? = null
    ): Pair<PurchaseEntity, UndoableAction.Purchase> = withContext(Dispatchers.IO) {
        db.withTransaction {
            val purchaseId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            var subtotal = 0.0
            val itemsArray = JSONArray()

            for (item in items) {
                val obj = JSONObject().apply {
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("quantity", item.quantity)
                    put("unitCost", item.unitCost)
                    put("total", item.total)
                }
                itemsArray.put(obj)
                subtotal += item.total

                if (item.productId.isNotBlank()) {
                    val prod = db.productDao().getProductById(item.productId)
                    if (prod != null) {
                        val oldCost = prod.costPrice
                        val newStock = prod.currentStock + item.quantity
                        db.productDao().updateStock(prod.id, newStock)

                        // Track price history if unit cost changed
                        if (item.unitCost > 0 && item.unitCost != oldCost) {
                            db.priceHistoryDao().insertPriceHistory(
                                PriceHistoryEntity(
                                    productId = prod.id,
                                    productName = prod.name,
                                    oldCostPrice = oldCost,
                                    newCostPrice = item.unitCost,
                                    oldSellingPrice = prod.sellingPrice,
                                    newSellingPrice = prod.sellingPrice,
                                    reason = "Invoice #$invoiceNumber from $supplierName",
                                    createdAt = now
                                )
                            )
                            db.productDao().updatePrices(prod.id, item.unitCost, prod.sellingPrice)
                        }

                        db.stockMovementDao().insertMovement(
                            StockMovementEntity(
                                productId = prod.id,
                                productName = prod.name,
                                type = StockMovementType.PURCHASE,
                                quantity = item.quantity,
                                previousStock = prod.currentStock,
                                newStock = newStock,
                                reason = "Inward Invoice #$invoiceNumber",
                                referenceId = purchaseId,
                                createdAt = now
                            )
                        )
                    }
                }
            }

            val total = (subtotal - discount + tax).coerceAtLeast(0.0)

            val purchase = PurchaseEntity(
                id = purchaseId,
                supplierId = supplierId,
                supplierName = supplierName,
                invoiceNumber = invoiceNumber,
                itemsJson = itemsArray.toString(),
                itemCount = items.size,
                subtotal = subtotal,
                discount = discount,
                tax = tax,
                total = total,
                paymentStatus = paymentStatus,
                notes = notes,
                invoiceImageUrl = invoiceImageUrl,
                createdAt = now
            )

            db.purchaseDao().insertPurchase(purchase)

            // Update supplier outstanding amount if unpaid
            if (paymentStatus != PaymentStatus.PAID && !supplierId.isNullOrBlank()) {
                db.supplierDao().updateOutstandingAmount(supplierId, total)
            }

            val undoAction = UndoableAction.Purchase(
                purchaseId = purchaseId,
                invoiceNumber = invoiceNumber,
                supplierName = supplierName,
                totalCost = total
            )

            Pair(purchase, undoAction)
        }
    }

    // --- Customer & Khata Operations ---
    suspend fun addCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        db.customerDao().insertCustomer(customer)
    }

    fun getCustomerLedger(customerId: String): Flow<List<CustomerLedgerEntryEntity>> {
        return db.customerDao().getLedgerForCustomer(customerId)
    }

    suspend fun recordCustomerPayment(
        customerId: String,
        amount: Double,
        note: String = "Payment received"
    ): UndoableAction.CustomerPayment? = withContext(Dispatchers.IO) {
        db.withTransaction {
            val customer = db.customerDao().getCustomerById(customerId) ?: return@withTransaction null
            val newBal = customer.creditBalance - amount
            val entryId = UUID.randomUUID().toString()
            db.customerDao().updateCreditBalance(customerId, -amount)
            db.customerDao().insertLedgerEntry(
                CustomerLedgerEntryEntity(
                    id = entryId,
                    customerId = customerId,
                    type = "PAYMENT_RECEIVED",
                    amount = amount,
                    balanceAfter = newBal,
                    note = note,
                    createdAt = System.currentTimeMillis()
                )
            )
            UndoableAction.CustomerPayment(
                customerId = customerId,
                customerName = customer.name,
                amount = amount,
                ledgerEntryId = entryId
            )
        }
    }

    // --- Supplier Operations ---
    suspend fun addSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        db.supplierDao().insertSupplier(supplier)
    }

    suspend fun recordSupplierPayment(
        supplierId: String,
        amountPaid: Double
    ) = withContext(Dispatchers.IO) {
        db.supplierDao().updateOutstandingAmount(supplierId, -amountPaid)
    }

    // --- Undo Operations ---
    suspend fun undoAction(action: UndoableAction): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            when (action) {
                is UndoableAction.Sale -> {
                    val sale = db.saleDao().getSaleById(action.saleId) ?: return@withTransaction false
                    // Restore stock for all items in the sale
                    try {
                        val array = JSONArray(sale.itemsJson)
                        for (i in 0 until array.length()) {
                            val itemObj = array.getJSONObject(i)
                            val pId = itemObj.optString("productId")
                            val qty = itemObj.optDouble("quantity", 0.0)
                            if (pId.isNotBlank() && qty > 0) {
                                val prod = db.productDao().getProductById(pId)
                                if (prod != null) {
                                    val restoredStock = prod.currentStock + qty
                                    db.productDao().updateStock(pId, restoredStock)
                                    db.stockMovementDao().insertMovement(
                                        StockMovementEntity(
                                            productId = prod.id,
                                            productName = prod.name,
                                            type = StockMovementType.RETURN_FROM_CUSTOMER,
                                            quantity = qty,
                                            previousStock = prod.currentStock,
                                            newStock = restoredStock,
                                            reason = "Undo Sale #${sale.id}",
                                            referenceId = "UNDO_${sale.id}"
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // If credit sale, deduct customer balance and remove ledger entry
                    if (sale.paymentMethod == PaymentMethod.CREDIT && !sale.customerId.isNullOrBlank()) {
                        db.customerDao().updateCreditBalance(sale.customerId, -sale.total)
                        db.customerDao().deleteLedgerEntriesByReferenceId(sale.id)
                    }

                    // Delete the sale record and original stock movements
                    db.stockMovementDao().deleteMovementsByReferenceId(sale.id)
                    db.saleDao().deleteSaleById(sale.id)
                    true
                }

                is UndoableAction.Purchase -> {
                    val purchase = db.purchaseDao().getPurchaseById(action.purchaseId) ?: return@withTransaction false
                    try {
                        val array = JSONArray(purchase.itemsJson)
                        for (i in 0 until array.length()) {
                            val itemObj = array.getJSONObject(i)
                            val pId = itemObj.optString("productId")
                            val qty = itemObj.optDouble("quantity", 0.0)
                            if (pId.isNotBlank() && qty > 0) {
                                val prod = db.productDao().getProductById(pId)
                                if (prod != null) {
                                    val deductedStock = (prod.currentStock - qty).coerceAtLeast(0.0)
                                    db.productDao().updateStock(pId, deductedStock)
                                    db.stockMovementDao().insertMovement(
                                        StockMovementEntity(
                                            productId = prod.id,
                                            productName = prod.name,
                                            type = StockMovementType.RETURN_TO_SUPPLIER,
                                            quantity = -qty,
                                            previousStock = prod.currentStock,
                                            newStock = deductedStock,
                                            reason = "Undo Purchase #${purchase.invoiceNumber}",
                                            referenceId = "UNDO_${purchase.id}"
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Revert supplier balance if unpaid
                    if (purchase.paymentStatus != PaymentStatus.PAID && !purchase.supplierId.isNullOrBlank()) {
                        db.supplierDao().updateOutstandingAmount(purchase.supplierId, -purchase.total)
                    }

                    db.stockMovementDao().deleteMovementsByReferenceId(purchase.id)
                    db.purchaseDao().deletePurchaseById(purchase.id)
                    true
                }

                is UndoableAction.StockAdjustment -> {
                    val prod = db.productDao().getProductById(action.productId) ?: return@withTransaction false
                    db.productDao().updateStock(prod.id, action.previousStock)
                    db.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            productId = prod.id,
                            productName = prod.name,
                            type = StockMovementType.ADJUSTMENT,
                            quantity = action.previousStock - prod.currentStock,
                            previousStock = prod.currentStock,
                            newStock = action.previousStock,
                            reason = "Undo stock adjustment"
                        )
                    )
                    true
                }

                is UndoableAction.CustomerPayment -> {
                    val customer = db.customerDao().getCustomerById(action.customerId) ?: return@withTransaction false
                    db.customerDao().updateCreditBalance(customer.id, action.amount)
                    if (action.ledgerEntryId != null) {
                        db.customerDao().deleteLedgerEntryById(action.ledgerEntryId)
                    }
                    true
                }
            }
        }
    }

    // --- AI Insights Management ---
    suspend fun dismissInsight(id: String) = withContext(Dispatchers.IO) {
        db.aiInsightDao().dismissInsight(id)
    }

    suspend fun insertInsight(insight: AIInsightEntity) = withContext(Dispatchers.IO) {
        db.aiInsightDao().insertInsight(insight)
    }

    // --- Settings ---
    suspend fun updateSettings(settings: ShopSettingsEntity) = withContext(Dispatchers.IO) {
        db.shopSettingsDao().insertOrUpdate(settings)
    }

    suspend fun getSettings(): ShopSettingsEntity = withContext(Dispatchers.IO) {
        db.shopSettingsDao().getSettings() ?: DemoData.getDemoSettings().copy(setupCompleted = false)
    }

    // --- Demo Data & Data Reset ---
    suspend fun loadDemoShopData() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.clearAllTables()
            db.productDao().insertProducts(DemoData.getDemoProducts())
            db.supplierDao().insertSuppliers(DemoData.getDemoSuppliers())
            db.customerDao().insertCustomers(DemoData.getDemoCustomers())
            db.saleDao().insertSales(DemoData.getDemoSales())
            db.purchaseDao().insertPurchases(DemoData.getDemoPurchases())
            db.stockMovementDao().insertMovements(DemoData.getDemoStockMovements())
            db.priceHistoryDao().insertPriceHistories(DemoData.getDemoPriceHistory())
            db.aiInsightDao().insertInsights(DemoData.getDemoInsights())
            db.shopSettingsDao().insertOrUpdate(DemoData.getDemoSettings())
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.clearAllTables()
            db.shopSettingsDao().insertOrUpdate(
                ShopSettingsEntity(
                    id = "default_shop",
                    shopName = "My Shop",
                    setupCompleted = false
                )
            )
        }
    }

    // --- Comprehensive V2 Backup & Restore ---
    suspend fun exportShopDataJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "ShopPilot AI")

        // Settings
        val settings = db.shopSettingsDao().getSettings()
        if (settings != null) {
            val sObj = JSONObject().apply {
                put("shopName", settings.shopName)
                put("shopType", settings.shopType)
                put("ownerName", settings.ownerName)
                put("phone", settings.phone)
                put("currency", settings.currency)
                put("language", settings.language)
                put("defaultLowStockThreshold", settings.defaultLowStockThreshold)
                put("deadStockDaysThreshold", settings.deadStockDaysThreshold)
            }
            root.put("settings", sObj)
        }

        // Products
        val products = db.productDao().getAllProducts().first()
        val prodArray = JSONArray()
        for (p in products) {
            prodArray.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category)
                    put("brand", p.brand)
                    put("sku", p.sku)
                    put("barcode", p.barcode)
                    put("unit", p.unit)
                    put("costPrice", p.costPrice)
                    put("sellingPrice", p.sellingPrice)
                    put("currentStock", p.currentStock)
                    put("minimumStock", p.minimumStock)
                    put("reorderLevel", p.reorderLevel)
                    put("supplierId", p.supplierId)
                    put("supplierName", p.supplierName)
                    put("active", p.active)
                    put("notes", p.notes)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)
                }
            )
        }
        root.put("products", prodArray)

        // Customers
        val customers = db.customerDao().getAllCustomers().first()
        val custArray = JSONArray()
        for (c in customers) {
            custArray.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("creditBalance", c.creditBalance)
                    put("totalPurchases", c.totalPurchases)
                    put("notes", c.notes)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                }
            )
        }
        root.put("customers", custArray)

        // Suppliers
        val suppliers = db.supplierDao().getAllSuppliers().first()
        val supArray = JSONArray()
        for (s in suppliers) {
            supArray.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("phone", s.phone)
                    put("outstandingAmount", s.outstandingAmount)
                    put("productsSummary", s.productsSummary)
                    put("notes", s.notes)
                    put("createdAt", s.createdAt)
                    put("updatedAt", s.updatedAt)
                }
            )
        }
        root.put("suppliers", supArray)

        // Sales
        val sales = db.saleDao().getAllSales().first()
        val salesArray = JSONArray()
        for (s in sales) {
            salesArray.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("itemsJson", s.itemsJson)
                    put("itemCount", s.itemCount)
                    put("subtotal", s.subtotal)
                    put("discount", s.discount)
                    put("total", s.total)
                    put("estimatedCost", s.estimatedCost)
                    put("estimatedGrossProfit", s.estimatedGrossProfit)
                    put("paymentMethod", s.paymentMethod.name)
                    put("customerId", s.customerId)
                    put("customerName", s.customerName)
                    put("notes", s.notes)
                    put("createdAt", s.createdAt)
                }
            )
        }
        root.put("sales", salesArray)

        // Purchases
        val purchases = db.purchaseDao().getAllPurchases().first()
        val purchArray = JSONArray()
        for (p in purchases) {
            purchArray.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("supplierId", p.supplierId)
                    put("supplierName", p.supplierName)
                    put("invoiceNumber", p.invoiceNumber)
                    put("itemsJson", p.itemsJson)
                    put("itemCount", p.itemCount)
                    put("subtotal", p.subtotal)
                    put("discount", p.discount)
                    put("tax", p.tax)
                    put("total", p.total)
                    put("paymentStatus", p.paymentStatus.name)
                    put("notes", p.notes)
                    put("createdAt", p.createdAt)
                }
            )
        }
        root.put("purchases", purchArray)

        root.toString(2)
    }

    fun parseBackupPreview(jsonString: String): BackupPreview? {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val shopName = root.optJSONObject("settings")?.optString("shopName", "Shop Backup") ?: "Shop Backup"
            val prodCount = root.optJSONArray("products")?.length() ?: 0
            val custCount = root.optJSONArray("customers")?.length() ?: 0
            val supCount = root.optJSONArray("suppliers")?.length() ?: 0
            val saleCount = root.optJSONArray("sales")?.length() ?: 0
            val purchCount = root.optJSONArray("purchases")?.length() ?: 0

            BackupPreview(
                version = version,
                exportedAt = exportedAt,
                shopName = shopName,
                productCount = prodCount,
                customerCount = custCount,
                supplierCount = supCount,
                saleCount = saleCount,
                purchaseCount = purchCount,
                rawJson = jsonString
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun importShopDataJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            try {
                val root = JSONObject(jsonString)
                if (!root.has("products")) return@withTransaction false

                val prodArray = root.optJSONArray("products") ?: JSONArray()
                val importedProducts = mutableListOf<ProductEntity>()
                for (i in 0 until prodArray.length()) {
                    val p = prodArray.getJSONObject(i)
                    importedProducts.add(
                        ProductEntity(
                            id = p.optString("id", UUID.randomUUID().toString()),
                            name = p.optString("name", "Item"),
                            category = p.optString("category", "General"),
                            brand = p.optString("brand", ""),
                            sku = p.optString("sku", ""),
                            barcode = p.optString("barcode", ""),
                            unit = p.optString("unit", "pcs"),
                            costPrice = p.optDouble("costPrice", 0.0),
                            sellingPrice = p.optDouble("sellingPrice", 0.0),
                            currentStock = p.optDouble("currentStock", 0.0),
                            minimumStock = p.optDouble("minimumStock", 5.0),
                            reorderLevel = p.optDouble("reorderLevel", 10.0),
                            supplierId = p.optString("supplierId", null),
                            supplierName = p.optString("supplierName", ""),
                            active = p.optBoolean("active", true),
                            notes = p.optString("notes", ""),
                            createdAt = p.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }

                db.clearAllTables()
                db.productDao().insertProducts(importedProducts)

                // Import customers
                val custArray = root.optJSONArray("customers")
                if (custArray != null) {
                    val importedCustomers = mutableListOf<CustomerEntity>()
                    for (i in 0 until custArray.length()) {
                        val c = custArray.getJSONObject(i)
                        importedCustomers.add(
                            CustomerEntity(
                                id = c.optString("id", UUID.randomUUID().toString()),
                                name = c.optString("name", "Customer"),
                                phone = c.optString("phone", ""),
                                creditBalance = c.optDouble("creditBalance", 0.0),
                                totalPurchases = c.optDouble("totalPurchases", 0.0),
                                notes = c.optString("notes", ""),
                                createdAt = c.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = c.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    db.customerDao().insertCustomers(importedCustomers)
                }

                // Import suppliers
                val supArray = root.optJSONArray("suppliers")
                if (supArray != null) {
                    val importedSuppliers = mutableListOf<SupplierEntity>()
                    for (i in 0 until supArray.length()) {
                        val s = supArray.getJSONObject(i)
                        importedSuppliers.add(
                            SupplierEntity(
                                id = s.optString("id", UUID.randomUUID().toString()),
                                name = s.optString("name", "Supplier"),
                                phone = s.optString("phone", ""),
                                outstandingAmount = s.optDouble("outstandingAmount", 0.0),
                                productsSummary = s.optString("productsSummary", ""),
                                notes = s.optString("notes", ""),
                                createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    db.supplierDao().insertSuppliers(importedSuppliers)
                }

                // Import sales
                val salesArray = root.optJSONArray("sales")
                if (salesArray != null) {
                    val importedSales = mutableListOf<SaleEntity>()
                    for (i in 0 until salesArray.length()) {
                        val s = salesArray.getJSONObject(i)
                        val pMethodStr = s.optString("paymentMethod", "CASH")
                        val pMethod = try { PaymentMethod.valueOf(pMethodStr) } catch (e: Exception) { PaymentMethod.CASH }
                        importedSales.add(
                            SaleEntity(
                                id = s.optString("id", UUID.randomUUID().toString()),
                                itemsJson = s.optString("itemsJson", "[]"),
                                itemCount = s.optInt("itemCount", 1),
                                subtotal = s.optDouble("subtotal", s.optDouble("total", 0.0)),
                                discount = s.optDouble("discount", 0.0),
                                total = s.optDouble("total", 0.0),
                                estimatedCost = s.optDouble("estimatedCost", 0.0),
                                estimatedGrossProfit = s.optDouble("estimatedGrossProfit", 0.0),
                                paymentMethod = pMethod,
                                customerId = if (s.has("customerId") && !s.isNull("customerId")) s.getString("customerId") else null,
                                customerName = s.optString("customerName", ""),
                                notes = s.optString("notes", ""),
                                createdAt = s.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    db.saleDao().insertSales(importedSales)
                }

                // Import purchases
                val purchArray = root.optJSONArray("purchases")
                if (purchArray != null) {
                    val importedPurchases = mutableListOf<PurchaseEntity>()
                    for (i in 0 until purchArray.length()) {
                        val p = purchArray.getJSONObject(i)
                        val pStatStr = p.optString("paymentStatus", "PAID")
                        val pStat = try { PaymentStatus.valueOf(pStatStr) } catch (e: Exception) { PaymentStatus.PAID }
                        importedPurchases.add(
                            PurchaseEntity(
                                id = p.optString("id", UUID.randomUUID().toString()),
                                supplierId = if (p.has("supplierId") && !p.isNull("supplierId")) p.getString("supplierId") else null,
                                supplierName = p.optString("supplierName", "Supplier"),
                                invoiceNumber = p.optString("invoiceNumber", ""),
                                itemsJson = p.optString("itemsJson", "[]"),
                                itemCount = p.optInt("itemCount", 1),
                                subtotal = p.optDouble("subtotal", p.optDouble("total", 0.0)),
                                discount = p.optDouble("discount", 0.0),
                                tax = p.optDouble("tax", 0.0),
                                total = p.optDouble("total", 0.0),
                                paymentStatus = pStat,
                                notes = p.optString("notes", ""),
                                createdAt = p.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    db.purchaseDao().insertPurchases(importedPurchases)
                }

                // Restore settings
                val sObj = root.optJSONObject("settings")
                if (sObj != null) {
                    db.shopSettingsDao().insertOrUpdate(
                        ShopSettingsEntity(
                            id = "default_shop",
                            shopName = sObj.optString("shopName", "My Shop"),
                            shopType = sObj.optString("shopType", "General Store"),
                            ownerName = sObj.optString("ownerName", ""),
                            phone = sObj.optString("phone", ""),
                            currency = sObj.optString("currency", "₹"),
                            language = sObj.optString("language", "Hinglish"),
                            defaultLowStockThreshold = sObj.optDouble("defaultLowStockThreshold", 5.0),
                            deadStockDaysThreshold = sObj.optInt("deadStockDaysThreshold", 30),
                            setupCompleted = true,
                            lastBackupTimestamp = System.currentTimeMillis()
                        )
                    )
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // --- Recommendation & Dead Stock Engine ---
    suspend fun calculatePurchaseRecommendations(): List<PurchaseRecommendation> = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllActiveProducts().first()
        val sales = db.saleDao().getAllSales().first()
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * 86400000L)

        val recentSales = sales.filter { it.createdAt >= thirtyDaysAgo }
        val productSalesCounts = mutableMapOf<String, Double>()
        val productActiveSaleDays = mutableMapOf<String, MutableSet<Long>>()

        for (sale in recentSales) {
            val dayBucket = sale.createdAt / 86400000L
            try {
                val array = JSONArray(sale.itemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pId = obj.optString("productId")
                    val qty = obj.optDouble("quantity", 1.0)
                    productSalesCounts[pId] = (productSalesCounts[pId] ?: 0.0) + qty
                    productActiveSaleDays.getOrPut(pId) { mutableSetOf() }.add(dayBucket)
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        val recommendations = mutableListOf<PurchaseRecommendation>()
        for (prod in products) {
            val totalSold30Days = productSalesCounts[prod.id] ?: 0.0
            val activeDaysCount = productActiveSaleDays[prod.id]?.size ?: 0

            val confidence = when {
                totalSold30Days <= 0.0 -> RecommendationConfidence.NO_HISTORY
                activeDaysCount >= 10 -> RecommendationConfidence.STRONG
                activeDaysCount >= 3 -> RecommendationConfidence.NORMAL
                else -> RecommendationConfidence.LIMITED_DATA
            }

            val avgDailySales = if (totalSold30Days > 0.0) totalSold30Days / 30.0 else 0.0
            val daysRemaining = if (avgDailySales > 0.0) prod.currentStock / avgDailySales else if (prod.currentStock <= 0) 0.0 else 999.0

            val isLowStock = prod.currentStock <= prod.minimumStock
            val isUrgent = prod.currentStock <= 0 || (avgDailySales > 0 && daysRemaining <= 3.0) || (prod.currentStock <= prod.minimumStock * 0.5)

            // Evaluate if reorder is needed
            if (prod.currentStock <= prod.reorderLevel || isLowStock || (avgDailySales > 0 && daysRemaining <= 7.0)) {
                val targetBufferDays = 14.0
                val suggestedQty = when (confidence) {
                    RecommendationConfidence.NO_HISTORY -> {
                        val baseTarget = if (prod.reorderLevel > prod.minimumStock) prod.reorderLevel * 1.5 else prod.minimumStock * 2.0
                        (baseTarget - prod.currentStock).coerceAtLeast(prod.minimumStock)
                    }
                    else -> {
                        val demandBased = (avgDailySales * targetBufferDays) + prod.reorderLevel - prod.currentStock
                        demandBased.coerceAtLeast(prod.minimumStock)
                    }
                }

                val reason = when (confidence) {
                    RecommendationConfidence.NO_HISTORY ->
                        "No sales history yet. Stock (${prod.currentStock.toInt()}) is at or below threshold (${prod.minimumStock.toInt()})."
                    RecommendationConfidence.LIMITED_DATA ->
                        "Limited sales data (${totalSold30Days.toInt()} units in 30d). Recommended minimum buffer of ${suggestedQty.toInt()} units."
                    else ->
                        "Based on ${String.format("%.1f", avgDailySales)} units/day velocity (${activeDaysCount} active sale days in 30d). Estimated ~${daysRemaining.toInt()} days of stock left."
                }

                recommendations.add(
                    PurchaseRecommendation(
                        product = prod,
                        currentStock = prod.currentStock,
                        avgDailySales = avgDailySales,
                        estimatedDaysRemaining = daysRemaining,
                        suggestedReorderQty = kotlin.math.ceil(suggestedQty),
                        reason = reason,
                        supplierName = if (prod.supplierName.isNotBlank()) prod.supplierName else "Default Distributor",
                        confidence = confidence,
                        isUrgent = isUrgent
                    )
                )
            }
        }

        recommendations.sortedWith(compareByDescending<PurchaseRecommendation> { it.isUrgent }.thenBy { it.estimatedDaysRemaining })
    }

    suspend fun calculateDeadStock(thresholdDays: Int = 30): List<DeadStockItem> = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllActiveProducts().first()
        val sales = db.saleDao().getAllSales().first()
        val now = System.currentTimeMillis()
        val thresholdMillis = thresholdDays.toLong() * 86400000L
        val cutoffTime = now - thresholdMillis

        // Build product -> last sold timestamp from all sales
        val productLastSold = mutableMapOf<String, Long>()
        for (sale in sales) {
            try {
                val array = JSONArray(sale.itemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pId = obj.optString("productId")
                    if (pId.isNotBlank()) {
                        val existing = productLastSold[pId] ?: 0L
                        if (sale.createdAt > existing) {
                            productLastSold[pId] = sale.createdAt
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore json error
            }
        }

        val deadList = mutableListOf<DeadStockItem>()
        for (prod in products) {
            if (prod.currentStock > 0) {
                val lastSoldTime = productLastSold[prod.id]
                val referenceTime = lastSoldTime ?: prod.createdAt
                val hasEverBeenSold = lastSoldTime != null

                if (referenceTime < cutoffTime) {
                    val daysInactive = ((now - referenceTime) / 86400000L).toInt().coerceAtLeast(thresholdDays)
                    val costLocked = prod.currentStock * prod.costPrice
                    deadList.add(
                        DeadStockItem(
                            product = prod,
                            currentStock = prod.currentStock,
                            daysSinceLastSale = daysInactive,
                            hasEverBeenSold = hasEverBeenSold,
                            inventoryCost = costLocked,
                            suggestedAction = if (prod.profitMarginPercent > 20) {
                                "Offer 15% discount or bundle with high-velocity items"
                            } else {
                                "Return to distributor or clear at cost"
                            }
                        )
                    )
                }
            }
        }

        deadList.sortedByDescending { it.inventoryCost }
    }
}
