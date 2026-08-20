package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.*
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Calendar

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user", "assistant"
    val text: String,
    val reasoning: String? = null,
    val proposedAction: ParsedIntentAction? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class UiNotification(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ShopRepository(db)
    val aiService = GeminiAiService()

    // --- State Streams ---
    val products = repository.allProducts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val categories = repository.allCategories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val sales = repository.allSales.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val purchases = repository.allPurchases.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val customers = repository.allCustomers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val suppliers = repository.allSuppliers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val priceHistory = repository.priceHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeInsights = repository.activeInsights.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val settings = repository.shopSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // --- Dynamic Metrics Flow ---
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        products,
        sales,
        customers,
        suppliers
    ) { prodList, salesList, custList, supList ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis

        val todaySales = salesList.filter { it.createdAt >= startOfToday }
        val todayTotal = todaySales.sumOf { it.total }
        val todayProfit = todaySales.sumOf { it.estimatedGrossProfit }
        val todayCount = todaySales.size
        val todayAvg = if (todayCount > 0) todayTotal / todayCount else 0.0

        val totalStock = prodList.sumOf { it.currentStock }
        val costVal = prodList.sumOf { it.inventoryCostValue }
        val sellVal = prodList.sumOf { it.inventorySellingValue }
        val potentialProfit = prodList.sumOf { it.potentialGrossProfit }

        val lowStock = prodList.count { it.currentStock <= it.minimumStock && it.currentStock > 0 }
        val outOfStock = prodList.count { it.currentStock <= 0 }
        val deadStock = prodList.count { it.currentStock > 0 && (now - it.updatedAt) > (30L * 86400000L) }

        val custCredit = custList.sumOf { it.creditBalance }
        val supPayable = supList.sumOf { it.outstandingAmount }

        DashboardMetrics(
            todaySalesTotal = todayTotal,
            todayEstimatedGrossProfit = todayProfit,
            todaySalesCount = todayCount,
            todayAvgTransactionValue = todayAvg,
            totalProductsCount = prodList.size,
            totalStockQuantity = totalStock,
            inventoryCostValue = costVal,
            inventorySellingValue = sellVal,
            potentialGrossProfit = potentialProfit,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock,
            deadStockCount = deadStock,
            totalCustomerCreditOutstanding = custCredit,
            totalSupplierPayable = supPayable
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardMetrics()
    )

    // --- Recommendations & Dead Stock State ---
    private val _purchaseRecommendations = MutableStateFlow<List<PurchaseRecommendation>>(emptyList())
    val purchaseRecommendations: StateFlow<List<PurchaseRecommendation>> = _purchaseRecommendations.asStateFlow()

    private val _deadStockList = MutableStateFlow<List<DeadStockItem>>(emptyList())
    val deadStockList: StateFlow<List<DeadStockItem>> = _deadStockList.asStateFlow()

    // --- AI Chat State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "assistant",
                text = "Namaste! Main ShopPilot AI hoon. Aap apni dukan ka stock, sales, kharidari ya koi bhi report mujhse pooch sakte hain ya bol kar sale/purchase record kar sakte hain."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- Active Scanned Invoice ---
    private val _scannedInvoice = MutableStateFlow<ExtractedInvoice?>(null)
    val scannedInvoice: StateFlow<ExtractedInvoice?> = _scannedInvoice.asStateFlow()

    private val _isScanningInvoice = MutableStateFlow(false)
    val isScanningInvoice: StateFlow<Boolean> = _isScanningInvoice.asStateFlow()

    // --- Proposed Action Confirmation State ---
    private val _proposedAction = MutableStateFlow<ParsedIntentAction?>(null)
    val proposedAction: StateFlow<ParsedIntentAction?> = _proposedAction.asStateFlow()

    // --- UI Notifications ---
    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    init {
        refreshIntelligence()
    }

    fun clearNotification() {
        _notification.value = null
    }

    fun showToast(msg: String, isError: Boolean = false) {
        _notification.value = UiNotification(msg, isError)
    }

    fun refreshIntelligence() {
        viewModelScope.launch {
            try {
                _purchaseRecommendations.value = repository.calculatePurchaseRecommendations()
                _deadStockList.value = repository.calculateDeadStock()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- AI Assistant Query ---
    fun askAiAssistant(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = query)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val contextJson = repository.getShopContextForAi()
                val lang = settings.value?.language ?: "Hinglish"
                val response = aiService.askAssistant(query, contextJson, lang)

                val assistantMsg = ChatMessage(
                    sender = "assistant",
                    text = response
                )
                _chatMessages.value = _chatMessages.value + assistantMsg
            } catch (e: Exception) {
                val fallback = generateOfflineShopResponse(query)
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "assistant",
                    text = fallback.first,
                    reasoning = fallback.second
                )
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // --- Voice Intent Parsing ---
    fun processVoiceInput(speechText: String) {
        if (speechText.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = "🎤 \"$speechText\"")
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val catalogJson = repository.getShopContextForAi()
                val action = aiService.parseVoiceIntent(speechText, catalogJson)

                when (action) {
                    is ParsedIntentAction.QueryResponse -> {
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            sender = "assistant",
                            text = action.answerText,
                            reasoning = action.dataReasoning
                        )
                    }
                    is ParsedIntentAction.RecordSale,
                    is ParsedIntentAction.RecordPurchase,
                    is ParsedIntentAction.AdjustStock,
                    is ParsedIntentAction.CustomerPayment -> {
                        // Prompt user to confirm before modifying data
                        _proposedAction.value = action
                    }
                    is ParsedIntentAction.Unknown -> {
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            sender = "assistant",
                            text = action.message
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback heuristic intent
                handleOfflineVoiceIntent(speechText)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun dismissProposedAction() {
        _proposedAction.value = null
    }

    fun executeProposedAction(action: ParsedIntentAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    is ParsedIntentAction.RecordSale -> {
                        // Match item productIds if possible
                        val catalog = products.value
                        val resolvedItems = action.items.map { item ->
                            val match = catalog.find { it.name.contains(item.productName, ignoreCase = true) || item.productName.contains(it.name, ignoreCase = true) }
                            if (match != null) {
                                item.copy(
                                    productId = match.id,
                                    productName = match.name,
                                    unitPrice = if (item.unitPrice > 0) item.unitPrice else match.sellingPrice,
                                    costPrice = match.costPrice,
                                    total = item.quantity * (if (item.unitPrice > 0) item.unitPrice else match.sellingPrice)
                                )
                            } else item
                        }

                        var custId: String? = null
                        if (action.customerName.isNotBlank()) {
                            val cust = customers.value.find { it.name.contains(action.customerName, ignoreCase = true) }
                            custId = cust?.id
                        }

                        repository.recordSale(
                            items = resolvedItems,
                            paymentMethod = action.paymentMethod,
                            customerId = custId,
                            customerName = action.customerName,
                            notes = "Recorded via Voice: ${action.rawSpeech}"
                        )
                        showToast("Sale recorded successfully via voice!")
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            sender = "assistant",
                            text = "✅ Sale of ₹${String.format("%.2f", resolvedItems.sumOf { it.total })} (${action.paymentMethod}) record ho gayi hai aur stock update kar diya gaya hai."
                        )
                    }
                    is ParsedIntentAction.RecordPurchase -> {
                        val prod = products.value.find { it.name.contains(action.productName, ignoreCase = true) }
                        val pId = prod?.id ?: ""
                        val pName = prod?.name ?: action.productName
                        val unitCost = if (action.unitCost > 0) action.unitCost else (prod?.costPrice ?: 0.0)

                        val purchaseItem = PurchaseItem(
                            productId = pId,
                            productName = pName,
                            quantity = action.quantity,
                            unitCost = unitCost,
                            total = action.quantity * unitCost
                        )
                        repository.recordPurchase(
                            supplierId = prod?.supplierId,
                            supplierName = action.supplierName,
                            invoiceNumber = "VOICE-${System.currentTimeMillis() % 10000}",
                            items = listOf(purchaseItem),
                            notes = "Voice purchase: ${action.rawSpeech}"
                        )
                        showToast("Purchase recorded successfully!")
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            sender = "assistant",
                            text = "✅ Purchase of ${action.quantity} units for '$pName' record kar di gayi hai."
                        )
                    }
                    is ParsedIntentAction.CustomerPayment -> {
                        val cust = customers.value.find { it.name.contains(action.customerName, ignoreCase = true) }
                        if (cust != null) {
                            repository.recordCustomerPayment(cust.id, action.amount, "Voice Payment Received")
                            showToast("Received ₹${action.amount} from ${cust.name}")
                            _chatMessages.value = _chatMessages.value + ChatMessage(
                                sender = "assistant",
                                text = "✅ ${cust.name} se ₹${action.amount} ki payment jama ho gayi hai. Naya balance: ₹${cust.creditBalance - action.amount}."
                            )
                        } else {
                            showToast("Customer '${action.customerName}' not found in Khata.", true)
                        }
                    }
                    else -> {}
                }
                refreshIntelligence()
            } catch (e: Exception) {
                showToast("Action failed: ${e.message}", true)
            } finally {
                _proposedAction.value = null
            }
        }
    }

    // --- Invoice Scanning ---
    fun scanInvoicePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanningInvoice.value = true
            try {
                val catalogJson = repository.getShopContextForAi()
                val extracted = aiService.extractInvoiceFromImage(bitmap, catalogJson)

                // Match with catalog
                val matchedItems = extracted.items.map { item ->
                    val match = products.value.find { it.name.contains(item.productName, ignoreCase = true) || item.productName.contains(it.name, ignoreCase = true) }
                    if (match != null) {
                        item.copy(
                            matchedProductId = match.id,
                            isNewProduct = false,
                            confidence = "Matched: ${match.name}"
                        )
                    } else {
                        item.copy(isNewProduct = true)
                    }
                }

                _scannedInvoice.value = extracted.copy(items = matchedItems)
            } catch (e: Exception) {
                showToast("Could not scan invoice: ${e.message}. Please try a clearer photo or enter manually.", true)
            } finally {
                _isScanningInvoice.value = false
            }
        }
    }

    fun dismissScannedInvoice() {
        _scannedInvoice.value = null
    }

    fun confirmInvoicePurchase(invoice: ExtractedInvoice) {
        viewModelScope.launch {
            try {
                // For items marked as new, create products
                val finalItems = mutableListOf<PurchaseItem>()
                for (item in invoice.items) {
                    var productId = item.matchedProductId
                    if (productId.isNullOrBlank() && item.isNewProduct) {
                        val newProd = ProductEntity(
                            name = item.productName,
                            unit = item.unit,
                            costPrice = item.unitCost,
                            sellingPrice = (item.unitCost * 1.25).coerceAtLeast(item.unitCost + 5.0),
                            currentStock = 0.0,
                            supplierName = invoice.supplierName
                        )
                        repository.saveProduct(newProd)
                        productId = newProd.id
                    }

                    finalItems.add(
                        PurchaseItem(
                            productId = productId ?: "",
                            productName = item.productName,
                            quantity = item.quantity,
                            unitCost = item.unitCost,
                            total = item.total
                        )
                    )
                }

                repository.recordPurchase(
                    supplierId = null,
                    supplierName = invoice.supplierName,
                    invoiceNumber = invoice.invoiceNumber,
                    items = finalItems,
                    discount = invoice.discount,
                    tax = invoice.tax,
                    notes = invoice.notes
                )

                _scannedInvoice.value = null
                showToast("Invoice processed & inventory updated successfully!")
                refreshIntelligence()
            } catch (e: Exception) {
                showToast("Failed to process invoice: ${e.message}", true)
            }
        }
    }

    // --- Fallback Offline Assistant & Voice Parser ---
    private fun generateOfflineShopResponse(query: String): Pair<String, String> {
        val q = query.lowercase()
        val metrics = dashboardMetrics.value
        val prodList = products.value
        val lowList = prodList.filter { it.currentStock <= it.minimumStock }

        return when {
            q.contains("stock") || q.contains("maal") -> {
                val lowNames = lowList.take(4).joinToString(", ") { "${it.name} (${it.currentStock} ${it.unit})" }
                Pair(
                    "Aapke paas kul ${prodList.size} products hain. ${lowList.size} products kam stock pe hain: $lowNames.",
                    "Local inventory calculation"
                )
            }
            q.contains("sale") || q.contains("bikri") || q.contains("bik") -> {
                Pair(
                    "Aaj ki kul bikri ₹${String.format("%.2f", metrics.todaySalesTotal)} (${metrics.todaySalesCount} orders) hui hai. Anumanit labh ₹${String.format("%.2f", metrics.todayEstimatedGrossProfit)} hai.",
                    "Local sales ledger calculation"
                )
            }
            q.contains("margin") || q.contains("profit") || q.contains("labh") -> {
                val high = prodList.maxByOrNull { it.profitMarginPercent }
                val low = prodList.filter { it.sellingPrice > 0 }.minByOrNull { it.profitMarginPercent }
                Pair(
                    "Sabse zyada margin: ${high?.name} (${String.format("%.1f", high?.profitMarginPercent)}%). Sabse kam margin: ${low?.name} (${String.format("%.1f", low?.profitMarginPercent)}%).",
                    "Local product margin calculation"
                )
            }
            q.contains("kharid") || q.contains("purchase") || q.contains("reorder") -> {
                val recs = _purchaseRecommendations.value.take(3)
                if (recs.isNotEmpty()) {
                    val str = recs.mapIndexed { idx, r -> "${idx + 1}. ${r.product.name} — stock ${r.currentStock}, reorder ~${r.suggestedReorderQty.toInt()} units" }.joinToString("\n")
                    Pair("Ye products khareedne ki salah hai:\n$str", "Sales velocity and reorder thresholds")
                } else {
                    Pair("Abhi koi product urgent reorder level pe nahi hai.", "Stock is healthy")
                }
            }
            else -> {
                Pair(
                    "Dukan Status: Aaj ki sales ₹${String.format("%.2f", metrics.todaySalesTotal)}, kul stock value ₹${String.format("%.2f", metrics.inventorySellingValue)}, aur ${metrics.lowStockCount} items low stock mein hain.",
                    "Offline Local Store Summary"
                )
            }
        }
    }

    private fun handleOfflineVoiceIntent(speech: String) {
        val s = speech.lowercase()
        // Simple offline sale detection
        if (s.contains("bech") || s.contains("bika") || s.contains("sold") || s.contains("diya")) {
            val matched = products.value.find { s.contains(it.name.lowercase().take(5)) }
            if (matched != null) {
                val saleItem = SaleItem(
                    productId = matched.id,
                    productName = matched.name,
                    quantity = 1.0,
                    unitPrice = matched.sellingPrice,
                    costPrice = matched.costPrice,
                    total = matched.sellingPrice
                )
                _proposedAction.value = ParsedIntentAction.RecordSale(
                    items = listOf(saleItem),
                    paymentMethod = if (s.contains("upi")) PaymentMethod.UPI else PaymentMethod.CASH,
                    rawSpeech = speech
                )
                return
            }
        }

        val fallback = generateOfflineShopResponse(speech)
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = "assistant",
            text = fallback.first,
            reasoning = fallback.second
        )
    }

    // --- Demo & Data Management ---
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
            refreshIntelligence()
            showToast("Product '${product.name}' saved!")
        }
    }

    fun adjustStock(productId: String, delta: Double, type: StockMovementType, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, delta, type, reason)
            refreshIntelligence()
            showToast("Stock updated!")
        }
    }

    fun recordCustomerPayment(customerId: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, note)
            showToast("Recorded ₹$amount payment!")
        }
    }

    fun addCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.addCustomer(customer)
            showToast("Customer '${customer.name}' added to Khata!")
        }
    }

    fun addSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.addSupplier(supplier)
            showToast("Supplier '${supplier.name}' added!")
        }
    }

    fun updateSettings(settings: ShopSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            showToast("Settings updated!")
        }
    }

    fun dismissInsight(insightId: String) {
        viewModelScope.launch {
            repository.dismissInsight(insightId)
        }
    }

    fun recordSale(
        items: List<SaleItem>,
        paymentMethod: PaymentMethod,
        discount: Double,
        customerId: String?,
        customerName: String
    ) {
        viewModelScope.launch {
            repository.recordSale(items, paymentMethod, discount, customerId, customerName)
            refreshIntelligence()
            showToast("Sale recorded!")
        }
    }

    fun recordPurchase(
        supplierId: String?,
        supplierName: String,
        invoiceNumber: String,
        items: List<PurchaseItem>,
        paymentStatus: PaymentStatus = PaymentStatus.PAID,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.recordPurchase(
                supplierId = supplierId,
                supplierName = supplierName,
                invoiceNumber = invoiceNumber,
                items = items,
                paymentStatus = paymentStatus,
                notes = notes
            )
            refreshIntelligence()
            showToast("Inward recorded and stock updated!")
        }
    }

    fun loadDemoShop() {
        viewModelScope.launch {
            repository.loadDemoShopData()
            refreshIntelligence()
            showToast("Demo shop loaded with realistic general store data!")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            refreshIntelligence()
            showToast("All shop data cleared.")
        }
    }

    fun exportBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportShopDataJson()
            withContext(Dispatchers.Main) {
                onExportReady(json)
            }
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            val success = repository.importShopDataJson(jsonString)
            if (success) {
                refreshIntelligence()
                showToast("Shop data imported successfully!")
            } else {
                showToast("Invalid backup JSON format.", true)
            }
        }
    }
}
