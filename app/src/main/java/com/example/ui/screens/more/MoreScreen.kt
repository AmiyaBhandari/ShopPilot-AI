package com.example.ui.screens.more

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ShopSettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    onNavigateToAssistant: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()

    val currency = settings?.currency ?: "₹"

    var showEditSettingsDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonString by remember { mutableStateOf("") }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Hub & Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shop Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = settings?.shopName ?: "My Shop",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${settings?.shopType ?: "General Store"} • ${settings?.language ?: "Hinglish"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { showEditSettingsDialog = true },
                            modifier = Modifier.testTag("edit_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Reports & Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Business Intelligence Overview",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Products Listed:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${products.size}", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Inventory Cost Locked:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format("%.0f", metrics.inventoryCostValue)}", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Sales Logged:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format("%.0f", sales.sumOf { it.total })}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Gross Profit Logged:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format("%.0f", sales.sumOf { it.estimatedGrossProfit })}", fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Customer Khata Due (Udhaar):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currency${String.format("%.0f", metrics.totalCustomerCreditOutstanding)}", fontWeight = FontWeight.Bold, color = StatusCritical)
                        }
                    }
                }
            }

            // Data Management Section
            item {
                SectionHeader(title = "Local Data & Offline Backup")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ListItem(
                            headlineContent = { Text("Export Shop Backup (JSON)", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Export full products, khata, sales & purchases to JSON") },
                            leadingContent = {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable {
                                viewModel.exportBackup { json ->
                                    exportedJsonString = json
                                    showExportDialog = true
                                }
                            }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("Import Shop Backup (JSON)", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Restore data from previous JSON file or backup") },
                            leadingContent = {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BrandTertiary)
                            },
                            modifier = Modifier.clickable {
                                showImportDialog = true
                            }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("Load Sample Indian Kirana Demo", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Populate 12+ real FMCG products, sales, khata & suppliers") },
                            leadingContent = {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = BrandSecondary)
                            },
                            modifier = Modifier.clickable {
                                viewModel.loadDemoShop()
                            }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("Clear All Store Data", fontWeight = FontWeight.SemiBold, color = StatusCritical) },
                            supportingContent = { Text("Reset local database to start fresh") },
                            leadingContent = {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusCritical)
                            },
                            modifier = Modifier.clickable {
                                showClearDataConfirm = true
                            }
                        )
                    }
                }
            }

            // About & Architecture
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ShopPilot AI • v1.0.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Local-First AI inventory manager built with Room & Gemini Multimodal. Your data is 100% private and stored on-device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Shop Data Backup (JSON)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy or save this JSON string to backup your entire shop data:")
                    OutlinedTextField(
                        value = exportedJsonString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ShopPilot Backup", exportedJsonString)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showToast("Backup JSON copied to clipboard!")
                        showExportDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importJsonText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Shop Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your exported JSON backup string below:")
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importBackup(importJsonText)
                            showImportDialog = false
                        }
                    },
                    enabled = importJsonText.isNotBlank()
                ) {
                    Text("Restore Data")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Data Confirm
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear All Shop Data?", fontWeight = FontWeight.Bold, color = StatusCritical) },
            text = { Text("This will permanently remove all products, sales records, customer ledgers, and purchases from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Text("Yes, Clear Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Settings Dialog
    if (showEditSettingsDialog) {
        var name by remember { mutableStateOf(settings?.shopName ?: "") }
        var type by remember { mutableStateOf(settings?.shopType ?: "General Store") }
        var owner by remember { mutableStateOf(settings?.ownerName ?: "") }
        var phone by remember { mutableStateOf(settings?.phone ?: "") }
        var lang by remember { mutableStateOf(settings?.language ?: "Hinglish") }

        AlertDialog(
            onDismissRequest = { showEditSettingsDialog = false },
            title = { Text("Shop Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Shop Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Shop Type / Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = owner,
                        onValueChange = { owner = it },
                        label = { Text("Owner Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = (settings ?: ShopSettingsEntity()).copy(
                            shopName = name,
                            shopType = type,
                            ownerName = owner,
                            phone = phone,
                            language = lang
                        )
                        viewModel.updateSettings(updated)
                        showEditSettingsDialog = false
                    }
                ) {
                    Text("Save Settings")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
