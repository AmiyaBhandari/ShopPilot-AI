package com.example.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActivityTab {
    SALES,
    PURCHASES,
    CUSTOMER_KHATA,
    SUPPLIERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: MainViewModel,
    onOpenQuickSale: () -> Unit,
    onOpenRecordPurchase: () -> Unit,
    onOpenVoiceInput: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ActivityTab.SALES) }

    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currency = settings?.currency ?: "₹"

    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerPaymentDialog by remember { mutableStateOf<CustomerEntity?>(null) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity & Ledger", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenVoiceInput) {
                        Surface(
                            color = BrandSecondary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = BrandSecondary,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        ActivityTab.SALES -> onOpenQuickSale()
                        ActivityTab.PURCHASES -> onOpenRecordPurchase()
                        ActivityTab.CUSTOMER_KHATA -> showAddCustomerDialog = true
                        ActivityTab.SUPPLIERS -> showAddSupplierDialog = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = when (selectedTab) {
                            ActivityTab.SALES -> "+ Record Sale"
                            ActivityTab.PURCHASES -> "+ Inward Stock"
                            ActivityTab.CUSTOMER_KHATA -> "+ Add Customer"
                            ActivityTab.SUPPLIERS -> "+ Add Supplier"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("activity_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Activity Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == ActivityTab.SALES,
                    onClick = { selectedTab = ActivityTab.SALES },
                    text = { Text("Sales (${sales.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == ActivityTab.PURCHASES,
                    onClick = { selectedTab = ActivityTab.PURCHASES },
                    text = { Text("Inward (${purchases.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == ActivityTab.CUSTOMER_KHATA,
                    onClick = { selectedTab = ActivityTab.CUSTOMER_KHATA },
                    text = { Text("Khata (${customers.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == ActivityTab.SUPPLIERS,
                    onClick = { selectedTab = ActivityTab.SUPPLIERS },
                    text = { Text("Suppliers (${suppliers.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                ActivityTab.SALES -> {
                    SalesListTab(sales = sales, currency = currency, onNewSale = onOpenQuickSale)
                }
                ActivityTab.PURCHASES -> {
                    PurchasesListTab(purchases = purchases, currency = currency, onNewPurchase = onOpenRecordPurchase)
                }
                ActivityTab.CUSTOMER_KHATA -> {
                    CustomerKhataTab(
                        customers = customers,
                        currency = currency,
                        onViewLedger = { selectedCustomerForLedger = it },
                        onRecordPayment = { showCustomerPaymentDialog = it }
                    )
                }
                ActivityTab.SUPPLIERS -> {
                    SuppliersListTab(suppliers = suppliers, currency = currency, onAddSupplier = { showAddSupplierDialog = true })
                }
            }
        }
    }

    // Customer Ledger Sheet
    selectedCustomerForLedger?.let { customer ->
        CustomerLedgerModal(
            customer = customer,
            currency = currency,
            viewModel = viewModel,
            onDismiss = { selectedCustomerForLedger = null },
            onRecordPayment = {
                showCustomerPaymentDialog = customer
            }
        )
    }

    // Customer Payment Dialog
    showCustomerPaymentDialog?.let { customer ->
        CustomerPaymentDialog(
            customer = customer,
            onDismiss = { showCustomerPaymentDialog = null },
            onConfirmPayment = { amount, note ->
                viewModel.recordCustomerPayment(customer.id, amount, note)
                showCustomerPaymentDialog = null
            }
        )
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, initialBalance, notes ->
                viewModel.addCustomer(
                    CustomerEntity(
                        name = name,
                        phone = phone,
                        creditBalance = initialBalance,
                        notes = notes
                    )
                )
                showAddCustomerDialog = false
            }
        )
    }

    // Add Supplier Dialog
    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onSave = { name, phone, summary, notes ->
                viewModel.addSupplier(
                    SupplierEntity(
                        name = name,
                        phone = phone,
                        productsSummary = summary,
                        notes = notes
                    )
                )
                showAddSupplierDialog = false
            }
        )
    }
}

@Composable
fun SalesListTab(sales: List<SaleEntity>, currency: String, onNewSale: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    if (sales.isEmpty()) {
        EmptyActivityState(
            title = "No sales recorded yet",
            subtitle = "Record sales using Quick Sale or Voice command.",
            buttonText = "Make First Sale",
            onAction = onNewSale
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sales, key = { it.id }) { sale ->
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
                            Column {
                                Text(
                                    text = if (sale.customerName.isNotBlank()) sale.customerName else "Walk-in Customer",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = dateFormat.format(Date(sale.createdAt)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currency${String.format("%.2f", sale.total)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Profit: $currency${String.format("%.1f", sale.estimatedGrossProfit)}",
                                    fontSize = 11.sp,
                                    color = StatusSuccess,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (sale.paymentMethod == PaymentMethod.CREDIT) StatusWarningBg else BrandPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = sale.paymentMethod.name,
                                    color = if (sale.paymentMethod == PaymentMethod.CREDIT) StatusWarning else BrandPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "${sale.itemCount} items",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchasesListTab(purchases: List<PurchaseEntity>, currency: String, onNewPurchase: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (purchases.isEmpty()) {
        EmptyActivityState(
            title = "No purchase invoices recorded",
            subtitle = "Log incoming goods from suppliers or scan distributor invoices.",
            buttonText = "Inward Stock",
            onAction = onNewPurchase
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(purchases, key = { it.id }) { purchase ->
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
                            Column {
                                Text(
                                    text = purchase.supplierName.ifBlank { "Supplier Invoice" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Inv: ${purchase.invoiceNumber.ifBlank { "N/A" }} • ${dateFormat.format(Date(purchase.createdAt))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currency${String.format("%.2f", purchase.total)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Surface(
                                    color = if (purchase.paymentStatus == PaymentStatus.PAID) StatusSuccessBg else StatusWarningBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = purchase.paymentStatus.name,
                                        color = if (purchase.paymentStatus == PaymentStatus.PAID) StatusSuccess else StatusWarning,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (purchase.notes.isNotBlank()) {
                            Text(
                                text = purchase.notes,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerKhataTab(
    customers: List<CustomerEntity>,
    currency: String,
    onViewLedger: (CustomerEntity) -> Unit,
    onRecordPayment: (CustomerEntity) -> Unit
) {
    val totalCredit = customers.sumOf { it.creditBalance }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Header Card
        Surface(
            color = BrandSecondary.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Khata / Credit Owed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currency${String.format("%.2f", totalCredit)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandSecondary
                    )
                }

                Surface(
                    color = BrandSecondary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${customers.count { it.creditBalance > 0 }} with dues",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (customers.isEmpty()) {
            EmptyActivityState(
                title = "No customers in Khata",
                subtitle = "Keep track of credit sales and payments digitally.",
                buttonText = "Add Customer",
                onAction = {}
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(customers, key = { it.id }) { customer ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewLedger(customer) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (customer.phone.isNotBlank()) {
                                    Text(
                                        text = customer.phone,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currency${String.format("%.2f", customer.creditBalance)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (customer.creditBalance > 0) StatusCritical else StatusSuccess
                                )
                                Text(
                                    text = if (customer.creditBalance > 0) "DUE / UDHAAR" else "NO DUES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customer.creditBalance > 0) StatusCritical else StatusSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuppliersListTab(suppliers: List<SupplierEntity>, currency: String, onAddSupplier: () -> Unit) {
    if (suppliers.isEmpty()) {
        EmptyActivityState(
            title = "No suppliers listed",
            subtitle = "Maintain supplier contacts and payable balances.",
            buttonText = "Add Supplier",
            onAction = onAddSupplier
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(suppliers, key = { it.id }) { supplier ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            Column {
                                Text(
                                    text = supplier.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (supplier.phone.isNotBlank()) {
                                    Text(
                                        text = supplier.phone,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Payable: $currency${String.format("%.0f", supplier.outstandingAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (supplier.outstandingAmount > 0) StatusWarning else StatusSuccess
                                )
                            }
                        }

                        if (supplier.productsSummary.isNotBlank()) {
                            Text(
                                text = "Supplies: ${supplier.productsSummary}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyActivityState(
    title: String,
    subtitle: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(buttonText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerModal(
    customer: CustomerEntity,
    currency: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onRecordPayment: () -> Unit
) {
    val ledger by viewModel.repository.getCustomerLedger(customer.id).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Khata Balance: $currency${String.format("%.2f", customer.creditBalance)}",
                        color = if (customer.creditBalance > 0) StatusCritical else StatusSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRecordPayment,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text("Collect Payment")
                }
            }

            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (ledger.isEmpty()) {
                Text(
                    text = "No ledger entries recorded yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ledger.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${entry.type} • ${entry.note}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${dateFormat.format(Date(entry.createdAt))} • Bal: $currency${String.format("%.0f", entry.balanceAfter)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (entry.type == "PAYMENT_RECEIVED") "-$currency${entry.amount}" else "+$currency${entry.amount}",
                            fontWeight = FontWeight.Bold,
                            color = if (entry.type == "PAYMENT_RECEIVED") StatusSuccess else StatusCritical
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerPaymentDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(customer.creditBalance.toString()) }
    var note by remember { mutableStateOf("Payment Received (Cash / UPI)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Record Payment from ${customer.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Outstanding Due: ₹${String.format("%.2f", customer.creditBalance)}",
                    fontWeight = FontWeight.SemiBold,
                    color = StatusCritical
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount Received (₹)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Mode") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirmPayment(amt, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, initialCredit: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var initialCreditStr by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer to Khata", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialCreditStr,
                    onValueChange = { initialCreditStr = it },
                    label = { Text("Existing Credit Balance (₹)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Address / Remarks") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val initCredit = initialCreditStr.toDoubleOrNull() ?: 0.0
                        onSave(name, phone, initCredit, notes)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Customer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, summary: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Distributor / Supplier", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier Name *") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Products Supplied (e.g. Biscuits, Milk)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Payment Terms / Notes") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, phone, summary, notes)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Supplier")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}
