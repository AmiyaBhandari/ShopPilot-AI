package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleItem
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    var selectedProductsWithQty by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var selectedCustomerName by remember { mutableStateOf("") }
    var discountStr by remember { mutableStateOf("0.0") }
    var productSearchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(products, productSearchQuery) {
        if (productSearchQuery.isBlank()) products.take(15)
        else products.filter { it.name.contains(productSearchQuery, ignoreCase = true) || it.category.contains(productSearchQuery, ignoreCase = true) }
    }

    val saleItems = remember(selectedProductsWithQty, products) {
        selectedProductsWithQty.mapNotNull { (prodId, qty) ->
            val prod = products.find { it.id == prodId }
            if (prod != null && qty > 0) {
                SaleItem(
                    productId = prod.id,
                    productName = prod.name,
                    quantity = qty,
                    unitPrice = prod.sellingPrice,
                    costPrice = prod.costPrice,
                    total = qty * prod.sellingPrice
                )
            } else null
        }
    }

    val subtotal = saleItems.sumOf { it.total }
    val totalCost = saleItems.sumOf { it.costPrice * it.quantity }
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val finalTotal = (subtotal - discount).coerceAtLeast(0.0)
    val estimatedProfit = finalTotal - totalCost

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Sale / Billing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select items and payment mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = BrandSecondary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$currency${String.format("%.2f", finalTotal)}",
                        fontWeight = FontWeight.Bold,
                        color = BrandSecondary,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Product search & add
            OutlinedTextField(
                value = productSearchQuery,
                onValueChange = { productSearchQuery = it },
                placeholder = { Text("Search catalog to add items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Product Selector list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredProducts, key = { it.id }) { prod ->
                    val currentQty = selectedProductsWithQty[prod.id] ?: 0.0
                    Surface(
                        color = if (currentQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$currency${prod.sellingPrice} • In stock: ${prod.currentStock.toInt()}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Stepper
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (currentQty > 0) {
                                    IconButton(
                                        onClick = {
                                            val newQty = currentQty - 1.0
                                            selectedProductsWithQty = if (newQty <= 0) {
                                                selectedProductsWithQty - prod.id
                                            } else {
                                                selectedProductsWithQty + (prod.id to newQty)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = StatusCritical)
                                    }
                                    Text(
                                        text = "${currentQty.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        selectedProductsWithQty = selectedProductsWithQty + (prod.id to (currentQty + 1.0))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Payment Mode Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(PaymentMethod.CASH, PaymentMethod.UPI, PaymentMethod.CREDIT).forEach { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = { selectedPaymentMethod = method },
                            label = {
                                Text(
                                    text = when (method) {
                                        PaymentMethod.CASH -> "💵 Cash"
                                        PaymentMethod.UPI -> "📱 UPI"
                                        PaymentMethod.CREDIT -> "📒 Udhaar (Khata)"
                                        else -> method.name
                                    }
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Customer Selector if Udhaar
            if (selectedPaymentMethod == PaymentMethod.CREDIT) {
                var customerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = !customerExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedCustomerName.isNotBlank()) selectedCustomerName else "Select Customer for Khata",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Customer *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (Due: $currency${c.creditBalance})") },
                                onClick = {
                                    selectedCustomerId = c.id
                                    selectedCustomerName = c.name
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Profit & Summary Info
            if (saleItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${saleItems.size} items selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Est. Profit: $currency${String.format("%.1f", estimatedProfit)}",
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Confirm Sale Button
            Button(
                onClick = {
                    if (saleItems.isNotEmpty()) {
                        viewModel.recordSale(
                            items = saleItems,
                            paymentMethod = selectedPaymentMethod,
                            discount = discount,
                            customerId = selectedCustomerId,
                            customerName = selectedCustomerName
                        )
                        onDismiss()
                    }
                },
                enabled = saleItems.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_quick_sale_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Complete Sale ($currency${String.format("%.2f", finalTotal)})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
