package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ParsedIntentAction
import com.example.data.local.entity.ProductEntity
import com.example.ui.theme.*

enum class StockStatus(val label: String, val bg: Color, val text: Color) {
    HEALTHY("Healthy", StatusSuccessBg, StatusSuccess),
    LOW("Low Stock", StatusWarningBg, StatusWarning),
    CRITICAL("Critical", StatusCriticalBg, StatusCritical),
    OUT_OF_STOCK("Out of Stock", StatusCriticalBg, StatusCritical),
    DEAD_STOCK("Dead Stock", StatusDeadStockBg, StatusDeadStock)
}

fun getProductStockStatus(product: ProductEntity, deadStockDays: Int = 30): StockStatus {
    val now = System.currentTimeMillis()
    val isDead = product.currentStock > 0 && (now - product.updatedAt) > (deadStockDays.toLong() * 86400000L)
    return when {
        product.currentStock <= 0.0 -> StockStatus.OUT_OF_STOCK
        isDead -> StockStatus.DEAD_STOCK
        product.currentStock <= (product.minimumStock * 0.5) -> StockStatus.CRITICAL
        product.currentStock <= product.minimumStock -> StockStatus.LOW
        else -> StockStatus.HEALTHY
    }
}

@Composable
fun StockStatusBadge(status: StockStatus, modifier: Modifier = Modifier) {
    Surface(
        color = status.bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(status.text)
            )
            Text(
                text = status.label.uppercase(),
                color = status.text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    badgeText: String? = null,
    badgeColor: Color = StatusSuccess,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedLight,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                if (badgeText != null) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtleLight,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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

@Composable
fun ActionConfirmDialog(
    action: ParsedIntentAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Confirm Action",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "AI has proposed the following shop update from your voice/input. Please verify before applying to inventory:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (action) {
                            is ParsedIntentAction.RecordSale -> {
                                Text(
                                    text = "🛒 RECORD SALE",
                                    fontWeight = FontWeight.Bold,
                                    color = BrandSecondary,
                                    fontSize = 12.sp
                                )
                                action.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${item.quantity.toInt()}x ${item.productName}", fontWeight = FontWeight.Medium)
                                        Text(text = "₹${String.format("%.2f", item.total)}", fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Total (${action.paymentMethod})", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "₹${String.format("%.2f", action.items.sumOf { it.total })}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                }
                                if (action.customerName.isNotBlank()) {
                                    Text(
                                        text = "Customer: ${action.customerName}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is ParsedIntentAction.RecordPurchase -> {
                                Text(
                                    text = "📦 RECORD PURCHASE (INWARD)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontSize = 12.sp
                                )
                                Text(text = "Supplier: ${action.supplierName}", fontWeight = FontWeight.Medium)
                                Text(text = "Product: ${action.productName}")
                                Text(text = "Quantity: ${action.quantity} units @ ₹${action.unitCost}")
                                Text(
                                    text = "Total Inward Cost: ₹${String.format("%.2f", action.quantity * action.unitCost)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is ParsedIntentAction.CustomerPayment -> {
                                Text(
                                    text = "💰 CUSTOMER PAYMENT (KHATA)",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess,
                                    fontSize = 12.sp
                                )
                                Text(text = "Customer: ${action.customerName}")
                                Text(
                                    text = "Payment Received: ₹${String.format("%.2f", action.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = StatusSuccess
                                )
                            }
                            is ParsedIntentAction.AdjustStock -> {
                                Text(
                                    text = "⚙️ ADJUST STOCK",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(text = "Product: ${action.productName}")
                                Text(text = "New Stock: ${action.newQuantity}")
                                Text(text = "Reason: ${action.reason}")
                            }
                            else -> {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_action_button")
            ) {
                Text("Confirm & Apply")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
