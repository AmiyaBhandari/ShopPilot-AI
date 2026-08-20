package com.example.ui.screens.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.ai.ExtractedInvoice
import com.example.data.ai.ExtractedInvoiceItem
import com.example.ui.MainViewModel
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInvoiceScannerSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val scannedInvoice by viewModel.scannedInvoice.collectAsState()
    val isScanning by viewModel.isScanningInvoice.collectAsState()
    val products by viewModel.products.collectAsState()

    var editableInvoice by remember(scannedInvoice) { mutableStateOf(scannedInvoice) }

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
                        text = "AI Invoice / Bill Scanner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scan distributor invoice with Gemini Vision",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Gemini OCR is analyzing invoice photo...",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Extracting line items, rates, taxes & distributor info",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (editableInvoice == null) {
                // Photo capture / Sample Invoice trigger
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Take a photo of wholesale bill / distributor invoice",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            // Generate sample invoice image for OCR
                            val bmp = createSampleInvoiceBitmap()
                            viewModel.scanInvoicePhoto(bmp)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("scan_sample_invoice_button")
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Wholesale Invoice (Demo Photo)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Review & Map Extracted Invoice
                editableInvoice?.let { inv ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Supplier: ${inv.supplierName}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Invoice No: ${inv.invoiceNumber} • Date: ${inv.invoiceDate}",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Total Amount: ₹${String.format("%.2f", inv.total)} (${inv.items.size} items extracted)",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Verify Extracted Line Items:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        itemsIndexed(inv.items) { index, item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.productName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Surface(
                                            color = if (item.isNewProduct) StatusWarningBg else StatusSuccessBg,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (item.isNewProduct) "New Item" else "Catalog Match",
                                                color = if (item.isNewProduct) StatusWarning else StatusSuccess,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Qty: ${item.quantity.toInt()} ${item.unit} @ ₹${item.unitCost}", fontSize = 12.sp)
                                        Text("Line Total: ₹${String.format("%.2f", item.total)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Confirm Action Button
                    Button(
                        onClick = {
                            viewModel.confirmInvoicePurchase(inv)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_scanned_invoice_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm & Inward Stock (₹${String.format("%.2f", inv.total)})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Creates a synthetic realistic Indian wholesale invoice bitmap to test OCR Multimodal
 */
private fun createSampleInvoiceBitmap(): Bitmap {
    val width = 600
    val height = 800
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(AndroidColor.WHITE)

    val paint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 28f
        isFakeBoldText = true
        isAntiAlias = true
    }

    canvas.drawText("TAX INVOICE / CASH MEMO", 120f, 60f, paint)

    paint.textSize = 20f
    paint.isFakeBoldText = false
    canvas.drawText("ABC TRADERS & DISTRIBUTORS", 50f, 110f, paint)
    canvas.drawText("GSTIN: 07AAAAA0000A1Z5 | Phone: 9820112345", 50f, 140f, paint)
    canvas.drawText("Invoice: ABC/2026/099 | Date: 20-Aug-2026", 50f, 170f, paint)

    paint.strokeWidth = 2f
    canvas.drawLine(50f, 190f, 550f, 190f, paint)

    paint.isFakeBoldText = true
    canvas.drawText("Item Name", 50f, 220f, paint)
    canvas.drawText("Qty", 320f, 220f, paint)
    canvas.drawText("Rate", 400f, 220f, paint)
    canvas.drawText("Total", 480f, 220f, paint)

    canvas.drawLine(50f, 235f, 550f, 235f, paint)

    paint.isFakeBoldText = false
    canvas.drawText("1. Maggi 2-Min Noodles 70g", 50f, 270f, paint)
    canvas.drawText("30", 320f, 270f, paint)
    canvas.drawText("11.50", 400f, 270f, paint)
    canvas.drawText("345.00", 480f, 270f, paint)

    canvas.drawText("2. Parle-G Biscuits 250g", 50f, 320f, paint)
    canvas.drawText("24", 320f, 320f, paint)
    canvas.drawText("24.00", 400f, 320f, paint)
    canvas.drawText("576.00", 480f, 320f, paint)

    canvas.drawText("3. Coca-Cola 500ml", 50f, 370f, paint)
    canvas.drawText("12", 320f, 370f, paint)
    canvas.drawText("32.00", 400f, 370f, paint)
    canvas.drawText("384.00", 480f, 370f, paint)

    canvas.drawLine(50f, 420f, 550f, 420f, paint)

    paint.isFakeBoldText = true
    canvas.drawText("Sub Total:", 320f, 460f, paint)
    canvas.drawText("₹1305.00", 460f, 460f, paint)
    canvas.drawText("GST (5%):", 320f, 500f, paint)
    canvas.drawText("₹65.25", 460f, 500f, paint)
    canvas.drawText("Net Total:", 320f, 550f, paint)
    canvas.drawText("₹1370.25", 460f, 550f, paint)

    return bmp
}
