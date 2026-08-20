package com.example.data.repository

import android.content.Context
import com.example.data.ai.ExtractedInvoice
import com.example.data.demo.DemoData
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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

data class PurchaseRecommendation(
    val product: ProductEntity,
    val currentStock: Double,
    val avgDailySales: Double,
    val estimatedDaysRemaining: Double,
    val suggestedReorderQty: Double,
    val reason: String,
    val supplierName: String
)

data class DeadStockItem(
    val product: ProductEntity,
    val currentStock: Double,
    val daysSinceLastSale: Int,
    val inventoryCost: Double,
    val suggestedAction: String
)

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
        val existing = db.productDao().getProductById(product.id)
        if (existing != null) {
            // Check if price changed
            if (existing.costPrice != product.costPrice || existing.sellingPrice != product.sellingPrice) {
                db.priceHistoryDao().insertPriceHistory(
                    PriceHistoryEntity(
                        productId = product.id,
                        productName = product.name,
                        oldCostPrice = existing.costPrice,
                        newCostPrice = product.costPrice,
                        oldSellingPrice = existing.sellingPrice,
                        newSellingPrice = product.sellingPrice,
                        reason = "Manual price edit in product details"
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
                        reason = "Direct inventory edit"
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
                        reason = "Initial product stock"
                    )
                )
            }
        }
    }

    suspend fun adjustStock(
        productId: String,
        quantityDelta: Double,
        type: StockMovementType,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val product = db.productDao().getProductById(productId) ?: return@withContext
        val newStock = (product.currentStock + quantityDelta).coerceAtLeast(0.0)
        db.productDao().updateStock(productId, newStock)
        db.stockMovementDao().insertMovement(
            StockMovementEntity(
                productId = productId,
                productName = product.name,
                type = type,
                quantity = quantityDelta,
                previousStock = product.currentStock,
                newStock = newStock,
                reason = reason
            )
        )
    }

    suspend fun updateProductPrices(
        productId: String,
        newCostPrice: Double,
        newSellingPrice: Double,
        reason: String = "Price update"
    ) = withContext(Dispatchers.IO) {
        val product = db.productDao().getProductById(productId) ?: return@withContext
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

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        db.productDao().updateProduct(product.copy(active = false, updatedAt = System.currentTimeMillis()))
    }

    // --- Sale Operations ---
    suspend fun recordSale(
        items: List<SaleItem>,
        paymentMethod: PaymentMethod,
        discount: Double = 0.0,
        customerId: String? = null,
        customerName: String = "",
        notes: String = ""
    ): SaleEntity = withContext(Dispatchers.IO) {
        val saleId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        var totalSelling = 0.0
        var totalCost = 0.0

        val itemsArray = JSONArray()
        for (item in items) {
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

            // Deduct stock if productId exists
            if (item.productId.isNotBlank()) {
                val prod = db.productDao().getProductById(item.productId)
                if (prod != null) {
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
        }

        val finalTotal = (totalSelling - discount).coerceAtLeast(0.0)
        val grossProfit = finalTotal - totalCost

        val sale = SaleEntity(
            id = saleId,
            itemsJson = itemsArray.toString(),
            itemCount = items.size,
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

        // Handle Customer Credit if applicable
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
                        note = "Credit Sale ($items.size items)",
                        createdAt = now
                    )
                )
            }
        }

        sale
    }

    // --- Purchase Operations ---
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
    ): PurchaseEntity = withContext(Dispatchers.IO) {
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

            // Increase product stock & update cost price
            if (item.productId.isNotBlank()) {
                val prod = db.productDao().getProductById(item.productId)
                if (prod != null) {
                    val oldCost = prod.costPrice
                    val newStock = prod.currentStock + item.quantity
                    db.productDao().updateStock(prod.id, newStock)

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
                            reason = "Invoice #$invoiceNumber",
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

        // If unpaid or partial, update supplier outstanding
        if (paymentStatus != PaymentStatus.PAID && !supplierId.isNullOrBlank()) {
            db.supplierDao().updateOutstandingAmount(supplierId, total)
        }

        purchase
    }

    // --- Customer & Ledger ---
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
    ) = withContext(Dispatchers.IO) {
        val customer = db.customerDao().getCustomerById(customerId) ?: return@withContext
        val newBal = customer.creditBalance - amount
        db.customerDao().updateCreditBalance(customerId, -amount)
        db.customerDao().insertLedgerEntry(
            CustomerLedgerEntryEntity(
                customerId = customerId,
                type = "PAYMENT_RECEIVED",
                amount = amount,
                balanceAfter = newBal,
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    // --- Supplier ---
    suspend fun addSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        db.supplierDao().insertSupplier(supplier)
    }

    suspend fun recordSupplierPayment(
        supplierId: String,
        amountPaid: Double
    ) = withContext(Dispatchers.IO) {
        db.supplierDao().updateOutstandingAmount(supplierId, -amountPaid)
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

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        db.shopSettingsDao().insertOrUpdate(
            ShopSettingsEntity(
                id = "default_shop",
                shopName = "My Shop",
                setupCompleted = false
            )
        )
    }

    // --- Backup & Restore (JSON) ---
    suspend fun exportShopDataJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
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
                    put("notes", p.notes)
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
                    put("total", s.total)
                    put("discount", s.discount)
                    put("paymentMethod", s.paymentMethod.name)
                    put("customerName", s.customerName)
                    put("createdAt", s.createdAt)
                }
            )
        }
        root.put("sales", salesArray)

        root.toString(2)
    }

    suspend fun importShopDataJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("products")) return@withContext false

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
                        notes = p.optString("notes", "")
                    )
                )
            }

            db.clearAllTables()
            db.productDao().insertProducts(importedProducts)

            // Import customers if present
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
                            totalPurchases = c.optDouble("totalPurchases", 0.0)
                        )
                    )
                }
                db.customerDao().insertCustomers(importedCustomers)
            }

            // Import suppliers if present
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
                            productsSummary = s.optString("productsSummary", "")
                        )
                    )
                }
                db.supplierDao().insertSuppliers(importedSuppliers)
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

    // --- Calculation Helpers for AI Context & Insights ---
    suspend fun getShopContextForAi(): String = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllActiveProducts().first()
        val sales = db.saleDao().getAllSales().first()
        val lowStock = db.productDao().getLowStockProducts().first()
        val customers = db.customerDao().getCustomersWithCredit().first()
        val suppliers = db.supplierDao().getAllSuppliers().first()

        val root = JSONObject()
        val now = System.currentTimeMillis()
        val todayStart = now - (now % 86400000L)
        val todaySales = sales.filter { it.createdAt >= todayStart }

        root.put("todaySalesTotal", todaySales.sumOf { it.total })
        root.put("todaySalesCount", todaySales.size)
        root.put("todayGrossProfit", todaySales.sumOf { it.estimatedGrossProfit })
        root.put("totalProducts", products.size)
        root.put("lowStockCount", lowStock.size)

        val prodArray = JSONArray()
        for (p in products) {
            prodArray.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category)
                    put("stock", p.currentStock)
                    put("minStock", p.minimumStock)
                    put("costPrice", p.costPrice)
                    put("sellingPrice", p.sellingPrice)
                    put("marginPercent", "%.1f".format(p.profitMarginPercent))
                    put("supplier", p.supplierName)
                }
            )
        }
        root.put("productsCatalog", prodArray)

        val custArray = JSONArray()
        for (c in customers) {
            custArray.put(
                JSONObject().apply {
                    put("name", c.name)
                    put("phone", c.phone)
                    put("creditOwed", c.creditBalance)
                }
            )
        }
        root.put("customersWithCredit", custArray)

        root.toString(2)
    }

    suspend fun calculatePurchaseRecommendations(): List<PurchaseRecommendation> = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllActiveProducts().first()
        val sales = db.saleDao().getAllSales().first()
        val now = System.currentTimeMillis()
        val fourteenDaysAgo = now - (14 * 86400000L)

        val recentSales = sales.filter { it.createdAt >= fourteenDaysAgo }
        val productSalesCounts = mutableMapOf<String, Double>()

        for (sale in recentSales) {
            try {
                val array = JSONArray(sale.itemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pId = obj.optString("productId")
                    val qty = obj.optDouble("quantity", 1.0)
                    productSalesCounts[pId] = (productSalesCounts[pId] ?: 0.0) + qty
                }
            } catch (e: Exception) {
                // ignore json error
            }
        }

        val recommendations = mutableListOf<PurchaseRecommendation>()
        for (prod in products) {
            val totalSold14Days = productSalesCounts[prod.id] ?: (if (prod.currentStock < prod.minimumStock) 3.0 else 0.5)
            val avgDaily = (totalSold14Days / 14.0).coerceAtLeast(0.1)
            val daysRemaining = prod.currentStock / avgDaily

            if (prod.currentStock <= prod.reorderLevel || daysRemaining <= 5.0) {
                val suggestedQty = ((avgDaily * 14) + prod.reorderLevel - prod.currentStock).coerceAtLeast(10.0)
                recommendations.add(
                    PurchaseRecommendation(
                        product = prod,
                        currentStock = prod.currentStock,
                        avgDailySales = avgDaily,
                        estimatedDaysRemaining = daysRemaining,
                        suggestedReorderQty = kotlin.math.ceil(suggestedQty),
                        reason = "Based on ${String.format("%.1f", avgDaily)} daily sales velocity and minimum reorder threshold.",
                        supplierName = if (prod.supplierName.isNotBlank()) prod.supplierName else "Distributor"
                    )
                )
            }
        }

        recommendations.sortedBy { it.estimatedDaysRemaining }
    }

    suspend fun calculateDeadStock(thresholdDays: Int = 30): List<DeadStockItem> = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllActiveProducts().first()
        val sales = db.saleDao().getAllSales().first()
        val now = System.currentTimeMillis()
        val thresholdTime = now - (thresholdDays.toLong() * 86400000L)

        val productLastSold = mutableMapOf<String, Long>()
        for (sale in sales) {
            try {
                val array = JSONArray(sale.itemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pId = obj.optString("productId")
                    val existing = productLastSold[pId] ?: 0L
                    if (sale.createdAt > existing) {
                        productLastSold[pId] = sale.createdAt
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        val deadList = mutableListOf<DeadStockItem>()
        for (prod in products) {
            if (prod.currentStock > 0) {
                val lastSold = productLastSold[prod.id] ?: prod.createdAt
                if (lastSold < thresholdTime) {
                    val daysInactive = ((now - lastSold) / 86400000L).toInt().coerceAtLeast(thresholdDays)
                    val costLocked = prod.currentStock * prod.costPrice
                    deadList.add(
                        DeadStockItem(
                            product = prod,
                            currentStock = prod.currentStock,
                            daysSinceLastSale = daysInactive,
                            inventoryCost = costLocked,
                            suggestedAction = if (prod.profitMarginPercent > 25) "Offer 15% discount or bundle with fast movers" else "Return to distributor or liquidate"
                        )
                    )
                }
            }
        }

        deadList.sortedByDescending { it.inventoryCost }
    }
}
