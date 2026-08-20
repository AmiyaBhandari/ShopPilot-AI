package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PaymentStatus
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseItem
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPurchaseDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"

    var supplierName by remember { mutableStateOf("") }
    var selectedSupplierId by remember { mutableStateOf<String?>(null) }
    var invoiceNumber by remember { mutableStateOf("INV-${System.currentTimeMillis() % 10000}") }
    var selectedProductId by remember { mutableStateOf("") }
    var selectedProductName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("20") }
    var unitCostStr by remember { mutableStateOf("0.0") }
    var paymentStatus by remember { mutableStateOf(PaymentStatus.PAID) }
    var notes by remember { mutableStateOf("") }

    var supplierDropdownExpanded by remember { mutableStateOf(false) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

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
            Text(
                text = "Inward Stock / Record Purchase",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Supplier Selector
            ExposedDropdownMenuBox(
                expanded = supplierDropdownExpanded,
                onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = if (supplierName.isNotBlank()) supplierName else "Select Supplier / Distributor",
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Name *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = supplierDropdownExpanded,
                    onDismissRequest = { supplierDropdownExpanded = false }
                ) {
                    suppliers.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name) },
                            onClick = {
                                selectedSupplierId = s.id
                                supplierName = s.name
                                supplierDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = invoiceNumber,
                onValueChange = { invoiceNumber = it },
                label = { Text("Invoice / Bill Number") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Product Selector
            ExposedDropdownMenuBox(
                expanded = productDropdownExpanded,
                onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = if (selectedProductName.isNotBlank()) selectedProductName else "Select Product from Inventory",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Product to Inward *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = productDropdownExpanded,
                    onDismissRequest = { productDropdownExpanded = false }
                ) {
                    products.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.name} (Stock: ${p.currentStock.toInt()})") },
                            onClick = {
                                selectedProductId = p.id
                                selectedProductName = p.name
                                unitCostStr = p.costPrice.toString()
                                productDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Added") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = unitCostStr,
                    onValueChange = { unitCostStr = it },
                    label = { Text("Unit Cost ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // Payment Status
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Payment Status to Supplier",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentStatus.values().forEach { status ->
                        FilterChip(
                            selected = paymentStatus == status,
                            onClick = { paymentStatus = status },
                            label = { Text(status.name) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val qty = quantityStr.toDoubleOrNull() ?: 0.0
            val cost = unitCostStr.toDoubleOrNull() ?: 0.0
            val totalCost = qty * cost

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Inward Amount:", fontWeight = FontWeight.Medium)
                    Text(
                        text = "$currency${String.format("%.2f", totalCost)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            }

            Button(
                onClick = {
                    if (selectedProductId.isNotBlank() && qty > 0) {
                        val purchaseItem = PurchaseItem(
                            productId = selectedProductId,
                            productName = selectedProductName,
                            quantity = qty,
                            unitCost = cost,
                            total = totalCost
                        )
                        viewModel.recordPurchase(
                            supplierId = selectedSupplierId,
                            supplierName = supplierName.ifBlank { "Distributor" },
                            invoiceNumber = invoiceNumber,
                            items = listOf(purchaseItem),
                            paymentStatus = paymentStatus,
                            notes = notes
                        )
                        onDismiss()
                    }
                },
                enabled = selectedProductId.isNotBlank() && qty > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_record_purchase_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Inward Purchase", fontWeight = FontWeight.Bold)
            }
        }
    }
}
