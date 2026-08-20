package com.example.ui.screens.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class ProductFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK,
    DEAD_STOCK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: MainViewModel,
    onOpenVoiceInput: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val deadStockList by viewModel.deadStockList.collectAsState()

    val currency = settings?.currency ?: "₹"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedFilter by remember { mutableStateOf(ProductFilter.ALL) }

    var selectedProductForDetail by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showStockAdjustDialog by remember { mutableStateOf<ProductEntity?>(null) }

    // Filter Logic
    val filteredProducts = remember(products, searchQuery, selectedCategory, selectedFilter, deadStockList) {
        val deadIds = deadStockList.map { it.product.id }.toSet()
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                ProductFilter.ALL -> true
                ProductFilter.LOW_STOCK -> p.currentStock <= p.minimumStock && p.currentStock > 0
                ProductFilter.OUT_OF_STOCK -> p.currentStock <= 0
                ProductFilter.DEAD_STOCK -> deadIds.contains(p.id)
            }

            matchesQuery && matchesCat && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inventory (${products.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenVoiceInput,
                        modifier = Modifier.testTag("products_voice_input_button")
                    ) {
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
                    productToEdit = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Product", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("add_product_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search products, SKU, barcodes...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_search_input")
            )

            // Stock Health Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == ProductFilter.ALL,
                        onClick = { selectedFilter = ProductFilter.ALL },
                        label = { Text("All (${products.size})") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    val lowCount = products.count { it.currentStock <= it.minimumStock && it.currentStock > 0 }
                    FilterChip(
                        selected = selectedFilter == ProductFilter.LOW_STOCK,
                        onClick = { selectedFilter = ProductFilter.LOW_STOCK },
                        label = { Text("Low Stock ($lowCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusWarningBg,
                            selectedLabelColor = StatusWarning
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    val outCount = products.count { it.currentStock <= 0 }
                    FilterChip(
                        selected = selectedFilter == ProductFilter.OUT_OF_STOCK,
                        onClick = { selectedFilter = ProductFilter.OUT_OF_STOCK },
                        label = { Text("Out of Stock ($outCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusCriticalBg,
                            selectedLabelColor = StatusCritical
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    val deadCount = deadStockList.size
                    FilterChip(
                        selected = selectedFilter == ProductFilter.DEAD_STOCK,
                        onClick = { selectedFilter = ProductFilter.DEAD_STOCK },
                        label = { Text("Dead Stock ($deadCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusDeadStockBg,
                            selectedLabelColor = StatusDeadStock
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Categories Filter Chips
            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        SuggestionChip(
                            onClick = { selectedCategory = "All" },
                            label = { Text("All Categories") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (selectedCategory == "All") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(categories) { cat ->
                        SuggestionChip(
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (selectedCategory == cat) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Product List
            if (filteredProducts.isEmpty()) {
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
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try adjusting your search query or filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            currency = currency,
                            onClick = { selectedProductForDetail = product },
                            onAdjustStock = { showStockAdjustDialog = product }
                        )
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    selectedProductForDetail?.let { prod ->
        ProductDetailModal(
            product = prod,
            currency = currency,
            viewModel = viewModel,
            onDismiss = { selectedProductForDetail = null },
            onEdit = {
                productToEdit = prod
                selectedProductForDetail = null
                showAddEditDialog = true
            },
            onAdjustStock = {
                showStockAdjustDialog = prod
            }
        )
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            initialProduct = productToEdit,
            onDismiss = {
                showAddEditDialog = false
                productToEdit = null
            },
            onSave = { updatedProd ->
                viewModel.saveProduct(updatedProd)
                showAddEditDialog = false
                productToEdit = null
            }
        )
    }

    // Stock Adjust Dialog
    showStockAdjustDialog?.let { prod ->
        StockAdjustModalDialog(
            product = prod,
            onDismiss = { showStockAdjustDialog = null },
            onApply = { delta, type, reason ->
                viewModel.adjustStock(prod.id, delta, type, reason)
                showStockAdjustDialog = null
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    currency: String,
    onClick: () -> Unit,
    onAdjustStock: () -> Unit
) {
    val stockStatus = getProductStockStatus(product)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.category}${if (product.brand.isNotBlank()) " • ${product.brand}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StockStatusBadge(status = stockStatus)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Qty
                Column {
                    Text(
                        text = "Current Stock",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${product.currentStock.toInt()} ${product.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (product.currentStock <= product.minimumStock) StatusCritical else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Prices
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Cost / Sell",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currency${product.costPrice.toInt()} / $currency${product.sellingPrice.toInt()}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Margin %
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Margin",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", product.profitMarginPercent)}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (product.profitMarginPercent >= 20) StatusSuccess else StatusWarning
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailModal(
    product: ProductEntity,
    currency: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit
) {
    val movements by viewModel.repository.allStockMovements.collectAsState(initial = emptyList())
    val productMovements = remember(movements, product.id) {
        movements.filter { it.productId == product.id }.take(10)
    }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${product.category} • ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StockStatusBadge(status = getProductStockStatus(product))
            }

            // Quick Stats Grid
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Stock", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${product.currentStock} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column {
                            Text("Min / Reorder", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${product.minimumStock.toInt()} / ${product.reorderLevel.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Margin %", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format("%.1f", product.profitMarginPercent)}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StatusSuccess)
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Unit Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format("%.2f", product.costPrice)}", fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Selling Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format("%.2f", product.sellingPrice)}", fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Stock Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format("%.0f", product.inventoryCostValue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAdjustStock,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust Stock", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit / Prices", fontSize = 12.sp)
                }
            }

            // Recent Movement Audit Log
            Text(
                text = "Recent Stock Movements",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )

            if (productMovements.isEmpty()) {
                Text(
                    text = "No stock movement logs recorded yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                productMovements.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${m.type.name} • ${m.reason}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Stock: ${m.previousStock} → ${m.newStock}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (m.quantity > 0) "+${m.quantity}" else "${m.quantity}",
                            fontWeight = FontWeight.Bold,
                            color = if (m.quantity > 0) StatusSuccess else StatusCritical
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    initialProduct: ProductEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "General") }
    var brand by remember { mutableStateOf(initialProduct?.brand ?: "") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var costPriceStr by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "0.0") }
    var sellingPriceStr by remember { mutableStateOf(initialProduct?.sellingPrice?.toString() ?: "0.0") }
    var currentStockStr by remember { mutableStateOf(initialProduct?.currentStock?.toString() ?: "10.0") }
    var minStockStr by remember { mutableStateOf(initialProduct?.minimumStock?.toString() ?: "5.0") }
    var reorderLevelStr by remember { mutableStateOf(initialProduct?.reorderLevel?.toString() ?: "15.0") }
    var supplierName by remember { mutableStateOf(initialProduct?.supplierName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct == null) "Add New Product" else "Edit Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg/pkt)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Cost Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Selling Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStockStr,
                        onValueChange = { currentStockStr = it },
                        label = { Text("Current Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it },
                        label = { Text("Min Alert Level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier / Distributor Name") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                        val sell = sellingPriceStr.toDoubleOrNull() ?: 0.0
                        val stock = currentStockStr.toDoubleOrNull() ?: 0.0
                        val minStock = minStockStr.toDoubleOrNull() ?: 5.0
                        val reorder = reorderLevelStr.toDoubleOrNull() ?: 15.0

                        val updated = (initialProduct ?: ProductEntity(name = name)).copy(
                            name = name,
                            category = category,
                            brand = brand,
                            unit = unit,
                            costPrice = cost,
                            sellingPrice = sell,
                            currentStock = stock,
                            minimumStock = minStock,
                            reorderLevel = reorder,
                            supplierName = supplierName,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updated)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Product")
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
fun StockAdjustModalDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onApply: (delta: Double, type: StockMovementType, reason: String) -> Unit
) {
    var adjustMode by remember { mutableStateOf("ADD") } // "ADD", "REDUCE", "SET"
    var quantityStr by remember { mutableStateOf("5") }
    var reason by remember { mutableStateOf("Stock Count Adjustment") }

    val reasons = listOf(
        "Direct Inventory Audit",
        "Damaged / Broken / Leaked",
        "Expired / Spoiled",
        "Customer Return",
        "Returned to Supplier",
        "Personal Shop Use"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adjust Stock: ${product.name}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Current Stock: ${product.currentStock} ${product.unit}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Mode Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = adjustMode == "ADD",
                        onClick = { adjustMode = "ADD" },
                        label = { Text("+ Add Stock") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = adjustMode == "REDUCE",
                        onClick = { adjustMode = "REDUCE" },
                        label = { Text("- Reduce") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Adjustment") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull() ?: 0.0
                    val delta = if (adjustMode == "ADD") qty else -qty
                    val type = if (adjustMode == "ADD") StockMovementType.ADJUSTMENT else StockMovementType.DAMAGE_LOSS
                    onApply(delta, type, reason)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Update Stock")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}
