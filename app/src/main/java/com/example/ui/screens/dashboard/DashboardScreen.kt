package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AIInsightEntity
import com.example.data.local.entity.InsightSeverity
import com.example.data.local.entity.InsightType
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.repository.PurchaseRecommendation
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToProducts: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onOpenQuickSale: () -> Unit,
    onOpenAddProduct: () -> Unit,
    onOpenRecordPurchase: () -> Unit,
    onOpenVoiceInput: () -> Unit,
    onOpenScanInvoice: () -> Unit
) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val insights by viewModel.activeInsights.collectAsState()
    val recommendations by viewModel.purchaseRecommendations.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val products by viewModel.products.collectAsState()

    val currency = settings?.currency ?: "₹"
    val shopName = settings?.shopName ?: "ShopPilot AI"

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, AppBorderSubtle),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = shopName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "GANDHI NAGAR STORE • ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMutedLight,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenScanInvoice,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ActionBlueBg)
                                .testTag("dashboard_scan_invoice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scan Invoice",
                                tint = ActionBlueIcon,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenVoiceInput,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrandPrimaryContainer)
                                .testTag("dashboard_voice_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = BrandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackgroundLight)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
        ) {
            // --- 2x2 Performance Metrics Grid (Clean Utility rounded-3xl cards) ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Aaj ki Sale",
                            value = "$currency${String.format("%.0f", metrics.todaySalesTotal)}",
                            subtitle = if (metrics.todaySalesCount > 0) "+12% vs yesterday (${metrics.todaySalesCount} orders)" else "No sales recorded yet",
                            icon = Icons.Default.PointOfSale,
                            valueColor = StatusSuccess,
                            badgeText = if (metrics.todaySalesCount > 0) "LIVE" else null,
                            badgeColor = StatusSuccess,
                            modifier = Modifier.weight(1f)
                        )

                        MetricCard(
                            title = "Est. Profit",
                            value = "$currency${String.format("%.0f", metrics.todayEstimatedGrossProfit)}",
                            subtitle = if (metrics.todaySalesTotal > 0) "${String.format("%.0f", (metrics.todayEstimatedGrossProfit / metrics.todaySalesTotal) * 100)}% Margin" else "0% Margin",
                            icon = Icons.Default.TrendingUp,
                            valueColor = TextPrimaryLight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Stock Cost",
                            value = "$currency${String.format("%.0f", metrics.inventoryCostValue)}",
                            subtitle = "Selling: $currency${String.format("%.0f", metrics.inventorySellingValue)}",
                            icon = Icons.Default.Inventory2,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToProducts
                        )

                        MetricCard(
                            title = "Pot. Profit",
                            value = "$currency${String.format("%.0f", metrics.potentialGrossProfit)}",
                            subtitle = "${metrics.totalProductsCount} total SKUs",
                            icon = Icons.Default.AccountBalanceWallet,
                            valueColor = BrandPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToProducts
                        )
                    }
                }
            }

            // --- Horizontal Stock Health Pill Chips ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val outOfStock = if (metrics.outOfStockCount > 0) metrics.outOfStockCount else 0
                    val lowStock = if (metrics.lowStockCount > 0) metrics.lowStockCount else 0

                    HealthPill(
                        label = "${if (outOfStock > 0) outOfStock else 0} Stock Out",
                        color = StatusCritical,
                        bgColor = StatusCriticalBg,
                        icon = Icons.Default.Warning
                    )

                    HealthPill(
                        label = "${if (lowStock > 0) lowStock else 0} Low Stock",
                        color = StatusWarning,
                        bgColor = StatusWarningBg,
                        icon = Icons.Default.Inventory2
                    )

                    if (metrics.deadStockCount > 0) {
                        HealthPill(
                            label = "${metrics.deadStockCount} Dead Stock",
                            color = StatusDeadStock,
                            bgColor = StatusDeadStockBg,
                            icon = Icons.Default.HourglassEmpty
                        )
                    }

                    HealthPill(
                        label = "${metrics.totalProductsCount} Healthy Items",
                        color = StatusSuccess,
                        bgColor = StatusSuccessBg,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            // --- AI Insight Hero Card (Indigo 600 Clean Utility Banner) ---
            item {
                val topInsight = insights.firstOrNull()
                val insightText = topInsight?.description ?: "\"Maggi 70g stock will finish by tomorrow. 40 units order karein?\""
                val insightTitle = topInsight?.title ?: "Stock Running Out Soon"

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Subtle decorative watermark icon in top right
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(96.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 16.dp, y = (-16).dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "AI INSIGHT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    letterSpacing = 1.2.sp
                                )
                            }

                            Text(
                                text = insightText,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (topInsight != null) {
                                            if (topInsight.type == InsightType.LOW_STOCK || topInsight.type == InsightType.REORDER_ALERT) {
                                                onOpenRecordPurchase()
                                            } else {
                                                onNavigateToAiAssistant()
                                            }
                                        } else {
                                            onOpenRecordPurchase()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = BrandPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (topInsight != null && !topInsight.actionText.isNullOrBlank()) topInsight.actionText else "Haan, Order Karo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (topInsight != null) {
                                            viewModel.dismissInsight(topInsight.id)
                                        } else {
                                            onNavigateToAiAssistant()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Baad Mein",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 3-Column / 4-Column Clean Utility Quick Action Grid ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CleanActionTile(
                        icon = Icons.Default.DocumentScanner,
                        label = "Scan Invoice",
                        bgColor = ActionBlueBg,
                        iconColor = ActionBlueIcon,
                        onClick = onOpenScanInvoice,
                        modifier = Modifier.weight(1f)
                    )
                    CleanActionTile(
                        icon = Icons.Default.PointOfSale,
                        label = "Add Sale",
                        bgColor = ActionGreenBg,
                        iconColor = ActionGreenIcon,
                        onClick = onOpenQuickSale,
                        modifier = Modifier.weight(1f)
                    )
                    CleanActionTile(
                        icon = Icons.Default.Group,
                        label = "Khata / Party",
                        bgColor = ActionOrangeBg,
                        iconColor = ActionOrangeIcon,
                        onClick = onNavigateToActivity,
                        modifier = Modifier.weight(1f)
                    )
                    CleanActionTile(
                        icon = Icons.Default.LocalShipping,
                        label = "Inward Stock",
                        bgColor = ActionPurpleBg,
                        iconColor = ActionPurpleIcon,
                        onClick = onOpenRecordPurchase,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Stock Health Alerts Bar ---
            if (metrics.lowStockCount > 0 || metrics.outOfStockCount > 0 || metrics.deadStockCount > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToProducts() }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = StatusWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Inventory Health Alerts",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (metrics.outOfStockCount > 0) {
                                    HealthPill(
                                        label = "${metrics.outOfStockCount} Out of Stock",
                                        color = StatusCritical,
                                        bgColor = StatusCriticalBg
                                    )
                                }
                                if (metrics.lowStockCount > 0) {
                                    HealthPill(
                                        label = "${metrics.lowStockCount} Low Stock",
                                        color = StatusWarning,
                                        bgColor = StatusWarningBg
                                    )
                                }
                                if (metrics.deadStockCount > 0) {
                                    HealthPill(
                                        label = "${metrics.deadStockCount} Dead Stock",
                                        color = StatusDeadStock,
                                        bgColor = StatusDeadStockBg
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- AI Business Insights Feed ---
            if (insights.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "AI Business Insights",
                        subtitle = "Real-time automated shop intelligence",
                        actionText = "View All",
                        onActionClick = onNavigateToAiAssistant
                    )
                }

                items(insights.take(3), key = { it.id }) { insight ->
                    InsightCard(
                        insight = insight,
                        onDismiss = { viewModel.dismissInsight(insight.id) },
                        onAction = {
                            if (insight.type == InsightType.LOW_STOCK || insight.type == InsightType.REORDER_ALERT) {
                                onOpenRecordPurchase()
                            } else if (insight.type == InsightType.MARGIN_DROP) {
                                onNavigateToProducts()
                            } else {
                                onNavigateToAiAssistant()
                            }
                        }
                    )
                }
            }

            // --- AI Smart Purchase Recommendations ---
            if (recommendations.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Suggested Purchase Orders",
                        subtitle = "Based on sales velocity & stock thresholds",
                        actionText = "+ Order",
                        onActionClick = onOpenRecordPurchase
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recommendations.take(5), key = { it.product.id }) { rec ->
                            PurchaseRecommendationCard(
                                recommendation = rec,
                                currency = currency,
                                onReorderClick = onOpenRecordPurchase
                            )
                        }
                    }
                }
            }

            // --- Today's Recent Sales ---
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    subtitle = "Sales logged today",
                    actionText = "All Activity",
                    onActionClick = onNavigateToActivity
                )
            }

            if (sales.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No sales recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onOpenQuickSale,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandSecondary)
                            ) {
                                Text("Make First Sale")
                            }
                        }
                    }
                }
            } else {
                items(sales.take(4), key = { it.id }) { sale ->
                    SaleListItemCard(sale = sale, currency = currency)
                }
            }
        }
    }
}

@Composable
fun CleanActionTile(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HealthPill(
    label: String,
    color: Color,
    bgColor: Color,
    icon: ImageVector? = null
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = label.uppercase(),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CleanActionTile(
        icon = icon,
        label = label,
        bgColor = color.copy(alpha = 0.12f),
        iconColor = color,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun InsightCard(
    insight: AIInsightEntity,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    val (icon, color, bg) = when (insight.severity) {
        InsightSeverity.CRITICAL -> Triple(Icons.Default.ErrorOutline, StatusCritical, StatusCriticalBg)
        InsightSeverity.WARNING -> Triple(Icons.Default.WarningAmber, StatusWarning, StatusWarningBg)
        InsightSeverity.INFO -> Triple(Icons.Default.Info, StatusInfo, StatusInfoBg)
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = insight.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (insight.reasoning.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Why: ${insight.reasoning}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!insight.actionText.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = insight.actionText,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseRecommendationCard(
    recommendation: PurchaseRecommendation,
    currency: String,
    onReorderClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable { onReorderClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StatusCriticalBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "~${String.format("%.1f", recommendation.estimatedDaysRemaining)}d left",
                        color = StatusCritical,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "${recommendation.currentStock.toInt()} in stock",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = recommendation.product.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Supplier: ${recommendation.supplierName}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Suggested",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${recommendation.suggestedReorderQty.toInt()} ${recommendation.product.unit}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onReorderClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SaleListItemCard(sale: SaleEntity, currency: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandSecondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = BrandSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = if (sale.customerName.isNotBlank()) "Sale • ${sale.customerName}" else "Sale (${sale.itemCount} items)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${sale.paymentMethod} • Profit $currency${String.format("%.1f", sale.estimatedGrossProfit)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "$currency${String.format("%.2f", sale.total)}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
