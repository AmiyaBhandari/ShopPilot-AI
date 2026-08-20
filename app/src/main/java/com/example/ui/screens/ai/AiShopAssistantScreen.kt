package com.example.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DeadStockItem
import com.example.data.repository.PurchaseRecommendation
import com.example.ui.ChatMessage
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AssistantTab {
    CHAT,
    PURCHASE_RECOMMENDATIONS,
    DEAD_STOCK,
    MARGIN_ALERTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiShopAssistantScreen(
    viewModel: MainViewModel,
    onOpenVoiceInput: () -> Unit,
    onOpenScanInvoice: () -> Unit,
    onOpenRecordPurchase: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AssistantTab.CHAT) }
    var inputText by remember { mutableStateOf("") }

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val recommendations by viewModel.purchaseRecommendations.collectAsState()
    val deadStockList by viewModel.deadStockList.collectAsState()
    val deadStockThreshold by viewModel.deadStockThreshold.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()

    val currency = settings?.currency ?: "₹"
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickQueries = listOf(
        "Dukan ki summary batao",
        "Kaunsa product reorder karna hai?",
        "Dead stock kitna hai?",
        "Margin kisme kam ho raha hai?",
        "Kal kitna cash aur upi collect hua?",
        "Sabse zyada bikne wala maal kaunsa hai?"
    )

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text("ShopPilot Intelligence", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Gemini AI • Business Advisor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenScanInvoice,
                        modifier = Modifier.testTag("ai_screen_scan_invoice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan Invoice",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Sub-navigation tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == AssistantTab.CHAT,
                    onClick = { selectedTab = AssistantTab.CHAT },
                    text = { Text("AI Chat & Voice", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == AssistantTab.PURCHASE_RECOMMENDATIONS,
                    onClick = { selectedTab = AssistantTab.PURCHASE_RECOMMENDATIONS },
                    text = { Text("Reorder Advice (${recommendations.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == AssistantTab.DEAD_STOCK,
                    onClick = { selectedTab = AssistantTab.DEAD_STOCK },
                    text = { Text("Dead Stock (${deadStockList.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == AssistantTab.MARGIN_ALERTS,
                    onClick = { selectedTab = AssistantTab.MARGIN_ALERTS },
                    text = { Text("Margin Alerts", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                AssistantTab.CHAT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Message list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(chatMessages, key = { it.id }) { msg ->
                                ChatBubbleItem(message = msg)
                            }

                            if (isAiLoading) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Analyzing your shop inventory & sales...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Preset queries
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(quickQueries) { q ->
                                SuggestionChip(
                                    onClick = {
                                        inputText = q
                                        viewModel.askAiAssistant(q)
                                        inputText = ""
                                    },
                                    label = { Text(q, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Input bar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 60.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = onOpenVoiceInput,
                                    modifier = Modifier.testTag("ai_mic_input_button")
                                ) {
                                    Surface(
                                        color = BrandSecondary.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Voice Input",
                                            tint = BrandSecondary,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = { Text("Ask in Hindi / Hinglish / English...") },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_chat_input_field"),
                                    singleLine = true
                                )

                                IconButton(
                                    onClick = {
                                        if (inputText.isNotBlank()) {
                                            viewModel.askAiAssistant(inputText)
                                            inputText = ""
                                        }
                                    },
                                    enabled = inputText.isNotBlank()
                                ) {
                                    Surface(
                                        color = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                AssistantTab.PURCHASE_RECOMMENDATIONS -> {
                    PurchaseRecommendationsTab(
                        recommendations = recommendations,
                        currency = currency,
                        onReorder = onOpenRecordPurchase
                    )
                }
                AssistantTab.DEAD_STOCK -> {
                    DeadStockTab(
                        deadStockList = deadStockList,
                        currentThreshold = deadStockThreshold,
                        onThresholdChange = { viewModel.setDeadStockThreshold(it) },
                        currency = currency
                    )
                }
                AssistantTab.MARGIN_ALERTS -> {
                    MarginAlertsTab(priceHistory = priceHistory, currency = currency)
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            shadowElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BrandSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ShopPilot AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandSecondary
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )

                if (message.reasoning != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Based on: ${message.reasoning}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseRecommendationsTab(
    recommendations: List<PurchaseRecommendation>,
    currency: String,
    onReorder: () -> Unit
) {
    if (recommendations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Inventory is Healthy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No products currently need urgent restocking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Suggested orders derived from 14-day sales velocity:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(recommendations, key = { it.product.id }) { rec ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rec.product.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = rec.confidence.label,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = StatusCriticalBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "~${String.format("%.1f", rec.estimatedDaysRemaining)} days left",
                                        color = StatusCritical,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Stock: ${rec.currentStock.toInt()} ${rec.product.unit}", fontSize = 12.sp)
                            Text("Daily Sales: ~${String.format("%.1f", rec.avgDailySales)}/day", fontSize = 12.sp)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Supplier: ${rec.supplierName} • Suggested: ${rec.suggestedReorderQty.toInt()} units (~$currency${String.format("%.0f", rec.suggestedReorderQty * rec.product.costPrice)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Button(
                            onClick = onReorder,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add to Purchase Order")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeadStockTab(
    deadStockList: List<DeadStockItem>,
    currentThreshold: Int,
    onThresholdChange: (Int) -> Unit,
    currency: String
) {
    val thresholds = listOf(15, 30, 60, 90)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inactive for:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            thresholds.forEach { days ->
                FilterChip(
                    selected = currentThreshold == days,
                    onClick = { onThresholdChange(days) },
                    label = { Text("${days}d", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        if (deadStockList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No dead stock detected for past $currentThreshold days! All products are moving.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val totalCostLocked = deadStockList.sumOf { it.inventoryCost }
                item {
                    Surface(
                        color = StatusDeadStockBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Capital Locked in Inactive Stock", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$currency${String.format("%.2f", totalCostLocked)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = StatusDeadStock
                            )
                            Text("Products with 0 sales in past $currentThreshold+ days.", fontSize = 11.sp)
                        }
                    }
                }

                items(deadStockList, key = { it.product.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = item.product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(text = "${item.daysSinceLastSale}d inactive", fontSize = 11.sp, color = StatusWarning)
                            }

                            Text("Stock: ${item.currentStock.toInt()} • Locked Value: $currency${String.format("%.0f", item.inventoryCost)}")
                            Text(
                                text = "💡 Advice: ${item.suggestedAction}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarginAlertsTab(priceHistory: List<com.example.data.local.entity.PriceHistoryEntity>, currency: String) {
    val increases = remember(priceHistory) {
        priceHistory.filter { it.newCostPrice > it.oldCostPrice && it.oldCostPrice > 0 }
    }

    if (increases.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No margin drops or supplier cost spikes recorded.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(increases, key = { it.id }) { ph ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = ph.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Cost increased: $currency${ph.oldCostPrice} → $currency${ph.newCostPrice} (Selling: $currency${ph.newSellingPrice})",
                            fontSize = 12.sp,
                            color = StatusCritical,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = ph.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
